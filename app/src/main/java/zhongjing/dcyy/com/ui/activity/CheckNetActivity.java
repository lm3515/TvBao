package zhongjing.dcyy.com.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import zhongjing.dcyy.com.R;
import zhongjing.dcyy.com.utils.BackPressedUtil;
import zhongjing.dcyy.com.utils.SPUtils;
import zhongjing.dcyy.com.utils.WifiManagerUtils;

/**
 * Created by Administrator on 2017/3/18 0018.
 */

public class CheckNetActivity extends BaseActivity implements View.OnClickListener {
    private static final String wifissid = "ZJBox";
    private static final String wifipsd = "12345678";
    private TextView mWifiName;
    private Button mConn;
    private Button mGo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);
        registerBroadcast();
        SPUtils.putFirstGoIN(this, false);//表示app已经初始化了
        initView();

        //大事
        setSpecificWifi(new WifiManagerUtils(this), wifissid, wifipsd);//手机打开后自动连接指定的WiFi。
    }

    private void setSpecificWifi(WifiManagerUtils wifiManagerUtils, String ssid,
                                 String psw) {
//        WIFI_STATE_DISABLED     0       正在关闭
//        WIFI_STATE_DISABLED     1        已经关闭
//        WIFI_STATE_ENABLING     2       正在打开
//        WIFI_STATE_ENABLED     3        已经打开
//        WIFI_STATE_UNKNOWN     4         未知
        if (wifiManagerUtils.checkWifiState() == 1) {
            wifiManagerUtils.openWifiEnabled();
        } else if (wifiManagerUtils.checkWifiState() == 3) {
            getBaseWifiInfo(wifiManagerUtils, ssid, psw);
        }
    }

    private void getBaseWifiInfo(WifiManagerUtils wifiManagerUtils, String ssid, String psw) {
        WifiInfo wifiInfo = wifiManagerUtils.getNetWorkId();
        if (TextUtils.isEmpty(wifiInfo.getSSID())) {
            //当前没有任何的wifi连接，判断扫描出的所有wifi中是否包含有想要的目标
            List<ScanResult> scanResults = wifiManagerUtils.getScanResult();
            for (ScanResult sr : scanResults) {
                if (sr.SSID.contains(ssid)) {
                    connectSpecificWifi(wifiManagerUtils, sr.SSID, psw);
                    break;
                }
            }
        } else {
            if (!wifiInfo.getSSID().contains(ssid)) {
                //当前连接的wifi不是我们想要的目标
                wifiManagerUtils.disconnectWifi(wifiInfo.getNetworkId());
                connectSpecificWifi(wifiManagerUtils, ssid, psw);
            } else {
                setWifiName(wifiManagerUtils);
            }
        }
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        registerReceiver(mReceiver, filter);
    }

    private void connectSpecificWifi(WifiManagerUtils managerUtils, String ssid, String psw) {
        boolean result = managerUtils.addNetWork(managerUtils.
                createWifiInfo(ssid, psw, 3));
        if (result) {

        } else {
            mWifiName.setText(R.string.checkwifi);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {

                }

                if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    getBaseWifiInfo(new WifiManagerUtils(CheckNetActivity.this), wifissid, wifipsd);
                }
            }

            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {

                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    // 当前WIFI名称
                    mWifiName.setText(wifiInfo.getSSID().equals("0x") ? getString(R.string.none) : wifiInfo.getSSID().replace("\"", ""));
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void setWifiName(WifiManagerUtils wifiManagerUtils) {
//        WIFI_STATE_DISABLED     0       正在关闭
//        WIFI_STATE_DISABLED     1        已经关闭
//        WIFI_STATE_ENABLING     2       正在打开
//        WIFI_STATE_ENABLED     3        已经打开
//        WIFI_STATE_UNKNOWN     4         未知
        switch (wifiManagerUtils.checkWifiState()) {
            case 1:
                mWifiName.setText(R.string.wifi_not_open);
                break;
            case 3:
                WifiInfo wifiInfo = wifiManagerUtils.getNetWorkId();
                if (TextUtils.isEmpty(wifiInfo.getSSID())) {
                    //当前没有任何的wifi连接，判断扫描出的所有wifi中是否包含有想要的目标
                    mWifiName.setText(R.string.wifi_notconnected);
                } else {
                    mWifiName.setText(wifiInfo.getSSID().replace("\"", ""));
                }
        }
    }

    private void initView() {
        mWifiName = (TextView) findViewById(R.id.wifiName);
        mConn = (Button) findViewById(R.id.wifi);
        mGo = (Button) findViewById(R.id.go);
        mConn.setOnClickListener(this);
        mGo.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.wifi) {//============跳转到WIFI设置界面
            Intent intent = new Intent();
            intent.setAction("android.net.wifi.PICK_WIFI_NETWORK");
            startActivityForResult(intent, 1);
        } else {
            startActivity(new Intent(this, PlayActivity.class));
            overridePendingTransition(R.anim.slide_up_in,
                    R.anim.slide_down_out);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        WifiManagerUtils wifiManagerUtils = new WifiManagerUtils(this);
        WifiInfo wifiInfo = wifiManagerUtils.getNetWorkId();
        mWifiName.setText(wifiInfo.getSSID().equals("0x") ? getString(R.string.none) : wifiInfo.getSSID().replace("\"", ""));
    }

    @Override
    public void onBackPressed() {
        BackPressedUtil.getInstance().showQuitTips(this, R.string.quit_setting);
    }
}
