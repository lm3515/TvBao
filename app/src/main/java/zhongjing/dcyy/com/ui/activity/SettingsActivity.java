package zhongjing.dcyy.com.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import okhttp3.Call;
import zhongjing.dcyy.com.R;

public class SettingsActivity extends BaseActivity {

    private static final int REQUEST_NAME = 100;
    private static final int REQUEST_PSW = 200;
    private static final int REQUEST_RESET = 300;
    private AlertDialog dialogName;
    private AlertDialog dialogPsw;
    private AlertDialog dialogReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void click(View view){
        switch (view.getId()){
            case R.id.activity_settings_back:            //返回键
                finish();
                break;
            case R.id.activity_settings_changeWiFiName:  //更改WiFi名称
                View nameInflate = View.inflate(this, R.layout.name_dialog, null);

                AlertDialog.Builder builderName = new AlertDialog.Builder(this);
                dialogName = builderName.setView(nameInflate).create();
                dialogName.show();

                final EditText name_et = (EditText) nameInflate.findViewById(R.id.name_dialog_et);
                Button name_bt_cancel = (Button) nameInflate.findViewById(R.id.name_dialog_cancel);
                Button name_bt_sure = (Button) nameInflate.findViewById(R.id.name_dialog_sure);
                name_bt_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogName.dismiss();
                    }
                });

                name_bt_sure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String wifiName = name_et.getText().toString();
                        if(TextUtils.isEmpty(wifiName)){
                            Toast.makeText(SettingsActivity.this,R.string.wifi_name,Toast.LENGTH_SHORT).show();
                            dialogName.dismiss();
                        }else{
                            OkHttpUtils_get("http://192.168.11.123/api/setssid?ssid="+wifiName,REQUEST_NAME);
                            dialogName.dismiss();
                        }

                    }

                });

                break;
            case R.id.activity_settings_changeWiFiPsw:   //更改WiFi密码
                View pswInflate = View.inflate(this, R.layout.psw_dialog, null);

                AlertDialog.Builder builderPsw = new AlertDialog.Builder(this);
                dialogPsw = builderPsw.setView(pswInflate).create();
                dialogPsw.show();

                final EditText psw_et = (EditText) pswInflate.findViewById(R.id.psw_dialog_et);
                Button psw_bt_cancel = (Button) pswInflate.findViewById(R.id.psw_dialog_cancel);
                Button psw_bt_sure = (Button) pswInflate.findViewById(R.id.psw_dialog_sure);
                psw_bt_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogPsw.dismiss();
                    }
                });

                psw_bt_sure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String wifiPsw = psw_et.getText().toString();
                        if(TextUtils.isEmpty(wifiPsw)){
                            Toast.makeText(SettingsActivity.this,R.string.wifi_psw,Toast.LENGTH_SHORT).show();
                            dialogPsw.dismiss();
                        }else{
                            OkHttpUtils_get("http://192.168.11.123/api/setpasswd?pswd="+wifiPsw,REQUEST_PSW);
                            dialogPsw.dismiss();
                        }
                    }
                });
                break;
            case R.id.activity_settings_reset:           //恢复出厂设置
                View resetInflate = View.inflate(this, R.layout.reset_dialog, null);

                AlertDialog.Builder builderReset = new AlertDialog.Builder(this);
                dialogReset = builderReset.setView(resetInflate).create();
                dialogReset.show();

                Button reset_bt_cancel = (Button) resetInflate.findViewById(R.id.reset_dialog_cancel);
                Button reset_bt_sure = (Button) resetInflate.findViewById(R.id.reset_dialog_sure);
                reset_bt_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogReset.dismiss();
                    }
                });

                reset_bt_sure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OkHttpUtils_get("http://192.168.11.123/api/reconfig",REQUEST_RESET);
                        dialogReset.dismiss();
                    }
                });
                break;
        }
    }

    private String parseJson(String wifiNameResult){
        String code = wifiNameResult.substring(127,128);
        return code;

    }

    @Override
    protected void onError_get(Call call, Exception e, int id, int type) {
        switch (type){
            case REQUEST_NAME:
            case REQUEST_PSW:
            case REQUEST_RESET:
                Toast.makeText(SettingsActivity.this,R.string.set_error,Toast.LENGTH_SHORT).show();
                break;

        }
    }

    @Override
    protected void onResponse_get(String response, int id, int type) {
            String code = parseJson(response);
            switch (type){
                case REQUEST_NAME:
                    if(code.equals("0")){
                        Toast.makeText(SettingsActivity.this,R.string.set_name_success,Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingsActivity.this,R.string.set_name_fail,Toast.LENGTH_SHORT).show();
                    }
                    break;
                case REQUEST_PSW:
                    if(code.equals("0")){
                        Toast.makeText(SettingsActivity.this,R.string.set_psw_success,Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingsActivity.this,R.string.set_psw_fail,Toast.LENGTH_SHORT).show();
                    }
                    break;
                case REQUEST_RESET:
                    if(code.equals("0")){
                        Toast.makeText(SettingsActivity.this,R.string.reset_success,Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingsActivity.this,R.string.reset_fail,Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
    }

}
