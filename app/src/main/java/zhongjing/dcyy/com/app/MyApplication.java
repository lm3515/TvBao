package zhongjing.dcyy.com.app;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.Bugly;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by Dcyy on 2017/1/10.
 */

public class MyApplication extends Application {
    private static MyApplication sInstance;

//    public static Socket socket;
    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new LoggerInterceptor("TAG"))
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();

        OkHttpUtils.initClient(okHttpClient);

        //Bugly
        Bugly.init(getApplicationContext(), "523a62318c", false);

/*        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.11.123", 2005);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();*/

    }

    public static Context getAppContext() {
        return sInstance;
    }

}
