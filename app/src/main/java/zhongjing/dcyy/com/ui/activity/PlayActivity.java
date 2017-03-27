package zhongjing.dcyy.com.ui.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import zhongjing.dcyy.com.R;
import zhongjing.dcyy.com.utils.PicUtils;
import zhongjing.dcyy.com.widget.widget.media.IRenderView;
import zhongjing.dcyy.com.widget.widget.media.TextureRenderView;

public class PlayActivity extends BaseActivity implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener, IRenderView.IRenderCallback {

    private static final int HIDE_VOLUME = 100;
    private static final int HIDE_BRIGHT = 200;
    private static final int CONNECT_ERROR = 300;
    private static final String URL_RTSP = "rtsp://192.168.11.123/1/h264major";

    private RelativeLayout topLayout;
    private RelativeLayout bottomLayout;
    private RelativeLayout rightLayout;
    private ImageView unLock_iv;
    private AudioManager audioManager;
    private Window window;
    private WindowManager.LayoutParams lp;
    private GestureDetector detector;
    private SoundPool soundPool;

    private int windowH;
    private int windowW;
    private float startY;
    private int startVolume;
    private int maxVolume;
    private boolean isHide = false;
    private IjkMediaPlayer mediaPlayer;
    private TextureRenderView surfaceView;

    static {
        System.loadLibrary("ijkffmpeg");
        System.loadLibrary("ijkplayer");
        System.loadLibrary("ijksdl");
    }

    private FrameLayout volumeOrBright;
    private ProgressBar volume_pb;
    private ProgressBar bright_pb;
    private LinearLayout volume_ll;
    private LinearLayout bright_ll;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_VOLUME:
                    volume_ll.setVisibility(View.GONE);
                    break;
                case HIDE_BRIGHT:
                    bright_ll.setVisibility(View.GONE);
                    break;
                case CONNECT_ERROR:
                    Toast.makeText(PlayActivity.this, R.string.line_error, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private Chronometer timer;
    private int mBottomY;
    private Animation mShowAnim;
    private Animation mHideAnim;
    private View mLoading;
    private Socket mSocket;
    private boolean playerIsPrepared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initWindow();

        initView();
        initVolumeAndBright();
        initListener();
        initEvent();
        initAnimation();

        initSocket();//初始化socket
        initReConnectedTask();//初始化重连SOCKET任务
    }

    private void initReConnectedTask() {
        cachedThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SystemClock.sleep(15 * 1000);
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("测试", "重新连接关闭socket时出问题了" + "====SOCKET=========" + e.toString());
                    }
                    initSocket();//在此与socket服务器建立连接
                    Log.d("测试", "重新连接" + "====SOCKET====" + mSocket.hashCode());
                }
            }
        });
    }

    private void initSocket() {
        cachedThreadPool.submit(new Runnable() {
            @Override
            public void run() {
//                BufferedReader reader = null;
                try {
                    mSocket = new Socket("192.168.11.123", 2005);
//                    reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

//                    String str = null;
//                    if ((str = reader.readLine()) != null) {
//                        Log.d("测试","连接Server返回消息" + str);
//                    }
                    Log.d("测试", "创建新的" + "====SOCKET====" + mSocket.hashCode());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("测试", "创建SOCKET出错===" + e.toString());
                }
            }
        });
    }


    private void initAnimation() {
        mShowAnim = AnimationUtils.loadAnimation(this, R.anim.show_anim);
        mHideAnim = AnimationUtils.loadAnimation(this, R.anim.hide_anim);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.d("测试", "socket为空");
        }
        cachedThreadPool.shutdown();
    }

    private void initView() {

        surfaceView = (TextureRenderView) findViewById(R.id.activity_play_sv);
        topLayout = (RelativeLayout) findViewById(R.id.activity_play_land_rl_top);  //横屏顶部菜单栏
        bottomLayout = (RelativeLayout) findViewById(R.id.activity_play_land_rl_bottom);//横屏底部菜单栏
        rightLayout = (RelativeLayout) findViewById(R.id.activity_play_land_rl_right);//横屏右侧菜单栏
        unLock_iv = (ImageView) findViewById(R.id.activity_play_land_btn_unlock);//横屏锁屏键
        timer = (Chronometer) findViewById(R.id.activity_play_timer);//计时器

        volumeOrBright = (FrameLayout) findViewById(R.id.activity_play_fl_volume_or_bright);//音量或亮度容器
        volume_ll = (LinearLayout) findViewById(R.id.activity_play_volume);//音量容器
        volume_pb = (ProgressBar) findViewById(R.id.volume_pb); //音量pb
        bright_ll = (LinearLayout) findViewById(R.id.activity_play_bright);//亮度容器
        bright_pb = (ProgressBar) findViewById(R.id.bright_pb); //亮度pb

        mLoading = findViewById(R.id.loading);
        mLoading.setVisibility(View.VISIBLE);
    }

    //初始化音量和亮度
    private void initVolumeAndBright() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);//flag 1 显示系统调节控件 0 不显示

        volume_pb.setProgress(volume / maxVolume * 100);

        window = this.getWindow();
        lp = window.getAttributes();
        try {
            saveBright = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            float currentBright = (float) saveBright / 255 * 100;
            bright_pb.setProgress((int) currentBright);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    //初始化监听器
    private void initListener() {
        detector = new GestureDetector(this, new MyGestureListener());
    }

    private void initEvent() {
        mediaPlayer = new IjkMediaPlayer();
        surfaceView.addRenderCallback(this);

        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 16);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 50000);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
        // Param for living
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);

        try {
            mediaPlayer.setDataSource(URL_RTSP);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //mediaPlayer准备工作-------回调,onPrepared
        mediaPlayer.setOnPreparedListener(this);
        //MediaPlayer完成---------回调,onCompletion
        mediaPlayer.setOnCompletionListener(this);

        //截图时声音
        soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        soundPool.load(this, R.raw.shutter, 1);

    }

    //获取屏幕宽高
    private void initWindow() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        windowW = point.x;
        windowH = point.y;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        iMediaPlayer.start();
        playerIsPrepared = true;
        Log.d("测试", "onPrepared===准备完毕");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoading.setVisibility(View.GONE);
            }
        }, 800);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerIsPrepared) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        Log.d("测试", "onCompletion");
    }


    //发送socket(连接-断开-连接-断开)
    public void sendSocket(final char msg) {
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket("192.168.11.123", 2005);
                    if (mSocket == null) {
                        mHandler.sendEmptyMessage(CONNECT_ERROR);
                        return;
                    }
                    DataOutputStream writer = new DataOutputStream(mSocket.getOutputStream());
                    writer.write(Character.toString(msg).getBytes());
                    writer.flush();
                    mSocket.shutdownOutput();
                    mSocket.close();
                    mSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //发送socket2(长连接)
    public void sendSocket2(final char msg) {
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                DataOutputStream writer = null;
                try {
                    Log.d("测试", "点击发送信息" + msg + "====SOCKET====" + mSocket.hashCode());
                    if (mSocket == null) {
                        mHandler.sendEmptyMessage(CONNECT_ERROR);
                        return;
                    }
                    writer = new DataOutputStream(mSocket.getOutputStream());
                    writer.write(Character.toString(msg).getBytes());
                    writer.flush();
                } catch (IOException e) {
                    Log.d("测试", "点击发送信息出错" + e.toString() + "====SOCKET====" + mSocket.hashCode());
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    Log.d("测试", "点击发送信息出错" + e.toString());
                }
            }
        });
    }

    private boolean unlock = true;
    private boolean isRecoding = true;

    public void click(View view) {
        switch (view.getId()) {
            case R.id.activity_play_land_btn_unlock:            //横屏锁屏键
                unlock = !unlock;
                if (unlock) {
                    unLock_iv.setImageResource(R.drawable.land_btn_unlock);
                } else {
                    unLock_iv.setImageResource(R.drawable.land_btn_lock);
                    hide();
                }
                break;
            case R.id.activity_play_btn_back:                  //竖屏返回键
                finish();
                if (!isRecoding) {
                    stopRecordVideo();
                }
                break;
            case R.id.activity_play_btn_fullscreen:            //竖屏全屏键
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case R.id.activity_play_land_btn_back:              //横屏返回键
            case R.id.activity_play_land_btn_fullscreen:        //横屏全屏键
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case R.id.activity_play_btn_camera:                //截图键
                Bitmap bitmap = surfaceView.getBitmap();
                screenShot(bitmap);
                soundPool.play(1, 1, 1, 0, 0, 1);
                break;
            case R.id.activity_play_btn_vcr:                   //录像键
                if (isRecoding) {
                    startRecordVideo();
                } else {
                    stopRecordVideo();
                }
                isRecoding = !isRecoding;
                break;
            case R.id.activity_play_btn_ok:                    //ok键
                sendSocket2('g');  //103
                break;
            case R.id.activity_play_btn_up:                    //上键
                sendSocket2('e'); //101
                break;
            case R.id.activity_play_btn_down:                   //下键
                sendSocket2('f');  //102
                break;
            case R.id.activity_play_btn_left:                   //左键
                sendSocket2('h'); //104
                break;
            case R.id.activity_play_btn_right:                  //右键
                sendSocket2('i'); //105
                break;
            case R.id.activity_play_btn_menu:                  //菜单键
                sendSocket2('d'); //100
                break;
            case R.id.activity_play_btn_closemenu:             //关闭菜单键
                sendSocket2('j'); //106
                break;

        }
    }

    private static final String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡
    public static final String SAVE_REAL_PATH = SAVE_PIC_PATH + "/zhongjingVideo";//保存的确切位置

    private void recordVideo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File filePath = new File(SAVE_REAL_PATH);
                if (!filePath.exists()) {
                    filePath.mkdirs();
                }
                File VideoPath = new File(filePath, System.currentTimeMillis() + ".mp4");
                mediaPlayer.lm_rtspRecordVideo(URL_RTSP, VideoPath.getAbsolutePath());
            }
        }).start();
    }

    //开始录制视频
    private void startRecordVideo() {
        timer.setVisibility(View.VISIBLE);
        timer.startAnimation(mShowAnim);
        timer.setBase(SystemClock.elapsedRealtime());//计时器清零
        int hour = (int) ((SystemClock.elapsedRealtime() - timer.getBase()) / 1000 / 60);
        timer.setFormat("0" + String.valueOf(hour) + ":%s");
        timer.start();
        mediaPlayer.lm_rtspSetRecodeStop(false);
        recordVideo();
    }

    //停止录制视频
    private void stopRecordVideo() {
        mediaPlayer.lm_rtspSetRecodeStop(true);
        timer.stop();
        timer.setVisibility(View.GONE);
        timer.startAnimation(mHideAnim);
    }

    //截图
    private void screenShot(Bitmap bitmap) {
        String picName = System.currentTimeMillis() + ".JPEG";
        PicUtils.saveBitmap(bitmap, picName);
        File file = new File(PicUtils.SAVE_REAL_PATH, picName);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
        sendBroadcast(intent);
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();

    }

    //手动更改音量和亮度
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mBottomY = surfaceView.getBottom();//播放器界面Y坐标作为滑动距离

                break;
            case MotionEvent.ACTION_MOVE:
                if (!unlock || startY > mBottomY + 20) {
                    break;
                }
                float offsetY = startY - event.getY();//位移量

                if (event.getX() > windowW / 2) {
                    final double FLING_MIN_DISTANCE = 20;
                    if (Math.abs(offsetY) > FLING_MIN_DISTANCE) {
                        float offsetPercent = offsetY / mBottomY;
                        int offsetVolume = (int) (offsetPercent * maxVolume);//增加或减少的音量
                        int finalVolume = Math.min(Math.max(startVolume + offsetVolume, 0), maxVolume);
                        //更新音量
                        updateVolume(finalVolume);
                    }
                } else {
                    //调节亮度
                    final double FLING_MIN_DISTANCE = 0.5;
                    final double FLING_MIN_VELOCITY = 60;
                    if (offsetY > FLING_MIN_DISTANCE && Math.abs(offsetY) > FLING_MIN_VELOCITY) {
                        setBrightness(10);
                    }
                    if (offsetY < FLING_MIN_DISTANCE && Math.abs(offsetY) > FLING_MIN_VELOCITY) {
                        setBrightness(-10);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    //更新亮度
    public void setBrightness(float brightness) {
        Log.d("测试", "brightness=========" + brightness);
        mHandler.sendEmptyMessage(HIDE_VOLUME);
        bright_ll.setVisibility(View.VISIBLE);
        WindowManager.LayoutParams lp = getWindow().getAttributes();

        if (lp.screenBrightness == -1) {
            lp.screenBrightness = (float) 0.5;
        }

        lp.screenBrightness = lp.screenBrightness + brightness / 255.0f;

        if (lp.screenBrightness > 1) {
            lp.screenBrightness = 1;
        } else if (lp.screenBrightness < 0) {
            lp.screenBrightness = 0;
        }
        getWindow().setAttributes(lp);

        bright_pb.setProgress((int) (lp.screenBrightness * 255));
        mHandler.removeMessages(HIDE_BRIGHT);
        mHandler.sendEmptyMessageDelayed(HIDE_BRIGHT, 1000);
    }

    //更新音量
    private void updateVolume(int volume) {
        mHandler.sendEmptyMessage(HIDE_BRIGHT);
        volume_ll.setVisibility(View.VISIBLE);
//        volume_ll.startAnimation(mShowAnim);
        float currentVolume = (float) volume / maxVolume * 100;
        volume_pb.setProgress((int) currentVolume);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        mHandler.removeMessages(HIDE_VOLUME);
        mHandler.sendEmptyMessageDelayed(HIDE_VOLUME, 1000);
    }

    //改变系统亮度
    private int saveBright;


    @Override
    public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
        holder.bindToMediaPlayer(mediaPlayer);
        //开启异步准备
        try {
            Log.d("测试", "onSurfaceCreated");
            mediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {

    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {    //单击
            if (unlock) {
                showOrHide();
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {             //双击
            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {                //长按
            super.onLongPress(e);
        }

    }

    //显示或隐藏菜单栏
    private void showOrHide() {
        if (isHide) {
            show();                                //显示菜单栏
            unLock_iv.setVisibility(View.VISIBLE); //显示锁屏键
            unLock_iv.startAnimation(mShowAnim);
        } else {
            hide();                                 //隐藏菜单栏
            unLock_iv.setVisibility(View.INVISIBLE);//隐藏锁屏键
            unLock_iv.startAnimation(mHideAnim);
        }
    }

    //显示菜单栏
    private void show() {
        topLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
        rightLayout.setVisibility(View.VISIBLE);

        topLayout.startAnimation(mShowAnim);
        bottomLayout.startAnimation(mShowAnim);
        rightLayout.startAnimation(mShowAnim);
        isHide = false;
    }

    //隐藏菜单栏
    private void hide() {
        topLayout.setVisibility(View.INVISIBLE);
        bottomLayout.setVisibility(View.INVISIBLE);
        rightLayout.setVisibility(View.INVISIBLE);

        topLayout.startAnimation(mHideAnim);
        bottomLayout.startAnimation(mHideAnim);
        rightLayout.startAnimation(mHideAnim);
        isHide = true;
    }


}
