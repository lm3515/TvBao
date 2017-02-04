package zhongjing.dcyy.com.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import zhongjing.dcyy.com.R;
import zhongjing.dcyy.com.utils.PkgUtils;

public class SplashBeforeActivity extends AppCompatActivity {

    private TextView versionName;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_before);
        initView();
        initEvent();
    }

    private void initView() {
        versionName = (TextView) findViewById(R.id.activity_splash_before_tv_version_name);
    }

    private void initEvent() {
        versionName.setText("V "+ PkgUtils.getVersionName(this));
        gotoSplashActivity();
    }

    // 去splash界面
    public void gotoSplashActivity() {
        // 延时启动
        handler.postDelayed(new Runnable() {
            public void run() {
                Intent intent =
                        new Intent(SplashBeforeActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, 2000);

    }
}
