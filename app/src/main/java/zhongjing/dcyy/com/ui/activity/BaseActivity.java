package zhongjing.dcyy.com.ui.activity;

import android.support.v7.app.AppCompatActivity;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;

/**
 * Created by Dcyy on 2017/1/12.
 */

public class BaseActivity extends AppCompatActivity {

    protected ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
    protected ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    protected void OkHttpUtils_get(String url, final int type){
        OkHttpUtils
                .get()
                .url(url)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        onError_get(call,e,id,type);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        onResponse_get(response,id,type);

                    }
                });
    }

    protected void onResponse_get(String response, int id, int type){};

    protected void onError_get(Call call, Exception e, int id,int type){};

}
