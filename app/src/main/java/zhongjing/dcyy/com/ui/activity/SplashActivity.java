package zhongjing.dcyy.com.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import zhongjing.dcyy.com.R;

public class SplashActivity extends AppCompatActivity {
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (savedInstanceState == null) {
            init();
            Log.d("测试", "第一次进入");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("测试", "start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("测试", "RESUME");
    }

    private void init() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("测试", "跳转");
                Intent playIntent = new Intent(SplashActivity.this, PlayActivity.class);
                startActivity(playIntent);
                overridePendingTransition(R.anim.slide_up_in,
                        R.anim.slide_down_out);
            }
        }, 1500);
    }


    //播放界面按钮
    public void clickPlay(View view) {
        Intent playIntent = new Intent(this, PlayActivity.class);
        startActivity(playIntent);
    }

    //缓存视频界面按钮
    public void clickCache(View view) {
        Intent cacheIntent = new Intent(this, CacheActivity.class);
        startActivity(cacheIntent);
    }

    //设置界面按钮
    public void clickSettings(View view) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("测试", "pause");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("测试", "存起来");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("测试", "onStop");
    }
}
