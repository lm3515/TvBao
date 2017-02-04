package zhongjing.dcyy.com.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import zhongjing.dcyy.com.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    //播放界面按钮
    public void clickPlay(View view){
        Intent playIntent = new Intent(this,PlayActivity.class);
        startActivity(playIntent);
    }

    //缓存视频界面按钮
    public void clickCache(View view){
        Intent cacheIntent = new Intent(this,CacheActivity.class);
        startActivity(cacheIntent);
    }

    //设置界面按钮
    public void clickSettings(View view){
        Intent settingsIntent = new Intent(this,SettingsActivity.class);
        startActivity(settingsIntent);
    }
}
