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
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import zhongjing.dcyy.com.R;
import zhongjing.dcyy.com.app.MyApplication;
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
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HIDE_VOLUME:
                    volume_ll.setVisibility(View.GONE);
                    break;
                case HIDE_BRIGHT:
                    bright_ll.setVisibility(View.GONE);
                    break;
                case CONNECT_ERROR:
                    Toast.makeText(PlayActivity.this,R.string.line_error,Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private Chronometer timer;


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

        /*IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");*/

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
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

    }

    //初始化音量和亮度
    private void initVolumeAndBright() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);//flag 1 显示系统调节控件 0 不显示

        window = this.getWindow();
        lp = window.getAttributes();
    }

    //初始化监听器
    private void initListener() {
        detector = new GestureDetector(this,new MyGestureListener());
    }

    private void initEvent() {
        mediaPlayer=new IjkMediaPlayer();

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

        //mediaPlayer准备工作
        mediaPlayer.setOnPreparedListener(this);
        //MediaPlayer完成
        mediaPlayer.setOnCompletionListener(this);

        //截图时声音
        soundPool= new SoundPool(1,AudioManager.STREAM_SYSTEM,5);
        soundPool.load(this,R.raw.shutter,1);

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
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {

    }

    private Socket socket;
    //发送socket(连接-断开-连接-断开)
    public void sendSocket(final char msg){
        cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.11.123", 2005);
                    if(socket==null){
                        mHandler.sendEmptyMessage(CONNECT_ERROR);
                        return;
                    }
                    DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
                    writer.write(Character.toString(msg).getBytes());
                    writer.flush();
                    socket.shutdownOutput();
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //发送socket2(长连接)
    public void sendSocket2(final char msg){
        singleThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if(MyApplication.socket==null){
                        mHandler.sendEmptyMessage(CONNECT_ERROR);
                        return;
                    }
                    DataOutputStream writer = new DataOutputStream(MyApplication.socket.getOutputStream());
                    writer.write(Character.toString(msg).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean unlock = true;
    private boolean isRecoding = true;
    public void click(View view){
        switch (view.getId()){
            case R.id.activity_play_land_btn_unlock:            //横屏锁屏键
                unlock = !unlock;
                if(unlock){
                    unLock_iv.setImageResource(R.drawable.land_btn_unlock);
                }else{
                    unLock_iv.setImageResource(R.drawable.land_btn_lock);
                    hide();
                }
                break;
            case R.id.activity_play_btn_back:                  //竖屏返回键
                finish();
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
                soundPool.play(1,1,1,0,0,1);
                break;
            case R.id.activity_play_btn_vcr:                   //录像键
                if(isRecoding){
                    startRecordVideo();
                }else{
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

    private static final String SAVE_PIC_PATH= Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡
    public static final String SAVE_REAL_PATH = SAVE_PIC_PATH+ "/zhongjingVideo";//保存的确切位置
    private void recordVideo (){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File filePath = new File(SAVE_REAL_PATH);
                if (!filePath.exists()) {
                    filePath.mkdirs();
                }
                File VideoPath = new File(filePath, System.currentTimeMillis()+".mp4");
                mediaPlayer.lm_rtspRecordVideo(URL_RTSP, VideoPath.getAbsolutePath());
            }
        }).start();
    }

    //开始录制视频
    private void startRecordVideo(){
        timer.setVisibility(View.VISIBLE);
        timer.setBase(SystemClock.elapsedRealtime());//计时器清零
        int hour = (int) ((SystemClock.elapsedRealtime() - timer.getBase()) / 1000 / 60);
        timer.setFormat("0"+String.valueOf(hour)+":%s");
        timer.start();
        mediaPlayer.lm_rtspSetRecodeStop(false);
        recordVideo();
    }

    //停止录制视频
    private void stopRecordVideo(){
        mediaPlayer.lm_rtspSetRecodeStop(true);
        timer.stop();
        timer.setVisibility(View.GONE);
    }

    //截图
    private void screenShot(Bitmap bitmap) {
        String picName = System.currentTimeMillis()+".JPEG";
        PicUtils.saveBitmap(bitmap,picName);
        File file = new File(PicUtils.SAVE_REAL_PATH,picName);
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), picName, null);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
            sendBroadcast(intent);
            Toast.makeText(this,"保存成功",Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    //手动更改音量和亮度
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                break;
            case MotionEvent.ACTION_MOVE:
                if(!unlock){
                    break;
                }
                float offsetY = startY - event.getY();
                float offsetPercent = offsetY / windowH;
                if(offsetPercent==0.0){
                    break;
                }
                if (event.getX() < windowW / 2) {
                    int offsetVolume = (int) (offsetPercent * maxVolume);
                    int finalVolume = startVolume + offsetVolume;
                    //更新音量
                    updateVolume(finalVolume);
                } else {
                    int offsetBright = (int) (offsetPercent * 255);
                    int finalBrightness = saveBright + offsetBright;
                    changeAppBrightness(finalBrightness);
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    //更新音量
    private void updateVolume(int volume) {
        volume_ll.setVisibility(View.VISIBLE);
        float currentVolume = (float)volume/maxVolume*100;
        volume_pb.setProgress((int)currentVolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        mHandler.removeMessages(HIDE_VOLUME);
        mHandler.sendEmptyMessageDelayed(HIDE_VOLUME, 3*1000);
    }

    //改变系统亮度
    private int saveBright;
    public void changeAppBrightness(int brightness) {
        saveBright = brightness;
        bright_ll.setVisibility(View.VISIBLE);
        bright_pb.setProgress(brightness);
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness)/255f;
        }
        window.setAttributes(lp);
        mHandler.removeMessages(HIDE_BRIGHT);
        mHandler.sendEmptyMessageDelayed(HIDE_BRIGHT, 3*1000);
    }

    @Override
    public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
        holder.bindToMediaPlayer(mediaPlayer);
        //开启异步准备
        mediaPlayer.prepareAsync();
    }

    @Override
    public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {

    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {    //单击
            if(unlock){
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
        if(isHide){
            show();                                //显示菜单栏
            unLock_iv.setVisibility(View.VISIBLE); //显示锁屏键
        }else{
            hide();                                 //隐藏菜单栏
            unLock_iv.setVisibility(View.INVISIBLE);//隐藏锁屏键
        }
    }

    //显示菜单栏
    private void show() {
        topLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
        rightLayout.setVisibility(View.VISIBLE);
        isHide = false;
    }

    //隐藏菜单栏
    private void hide() {
        topLayout.setVisibility(View.INVISIBLE);
        bottomLayout.setVisibility(View.INVISIBLE);
        rightLayout.setVisibility(View.INVISIBLE);
        isHide = true;
    }

}
