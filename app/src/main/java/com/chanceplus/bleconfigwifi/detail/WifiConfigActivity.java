package com.chanceplus.bleconfigwifi.detail;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chanceplus.bleconfigwifi.R;
import com.chanceplus.bleconfigwifi.adapter.ResultAdapter;
import com.chanceplus.bleconfigwifi.util.ConfigInfo;
import com.chanceplus.bleconfigwifi.util.MessageEncode;
import com.chanceplus.bleconfigwifi.util.MessagePacket;
import com.chanceplus.bleconfigwifi.util.TlvBox;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WifiConfigActivity extends AppCompatActivity{

    //启动activity时带入的数据的key值
    public static final String KEY_DATA = "key_data";

    public static final int TYPE_NO_PASSWD = 0x11;
    public static final int TYPE_WEP = 0x12;
    public static final int TYPE_WPA = 0x13;

    //连接wifi按钮
    private Button btnConf;
    private BleDevice bleDevice;
    private Context context;
    private MessageEncode messageEncode;
    private TextView text_total;
    private Spinner mySpinner;
    //ssid下拉列表
    private List<String> listSsid = new ArrayList<String>();
    private List<String> lstMac = new ArrayList<String>();
    private ArrayAdapter<String> adapterSsid;
    private EditText editPsw;
    private ListView listView;
    //开始配置标志
    private boolean isStart = false;
    private String psw = null;
    private ResultAdapter adapter = null;
    private boolean isThreadDisable = false;
    private HashMap<String, String> hashmap = new HashMap<String, String>();
    private HashMap<String, Integer> hashmapSecurity = new HashMap<String, Integer>();
    private Handler handler = new Handler();

    public enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING,  WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);
        initView();
        initData();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if(wifiManager.isWifiEnabled())
            {
                int position = 0;
                listSsid.clear();
                hashmapSecurity.clear();
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for(ScanResult r : scanResults){
                    listSsid.add(r.SSID);
                    int sec = TYPE_WPA;
                    if (r.capabilities.contains("WPA")
                            || r.capabilities.contains("wpa")) {
                        sec = TYPE_WPA;
                    } else if (r.capabilities.contains("WEP")
                            || r.capabilities.contains("wep")) {
                        sec = TYPE_WEP;
                    } else {
                        sec = TYPE_NO_PASSWD;
                    }
                    hashmapSecurity.put(r.SSID, sec);
                }
                adapterSsid.notifyDataSetChanged();
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssidString=null;
                if(wifiInfo != null){
                    ssidString=wifiInfo.getSSID();
                    int version = getAndroidSDKVersion();
                    if(version > 16 && ssidString.startsWith("\"") && ssidString.endsWith("\"")){
                        ssidString = ssidString.substring(1, ssidString.length() - 1);
                    }
                    for(;position < listSsid.size(); position++){
                        if(ssidString == null || ssidString.endsWith(listSsid.get(position))){
                            break;
                        }
                    }
                }
                mySpinner.setSelection(position);
                if(position == 0){
                    displayToast(ssidString==null?"null":ssidString);
                }
            }
            else if(isWifiApEnabled())
            {
                int position = 0;
                listSsid.clear();
                hashmapSecurity.clear();
                WifiConfiguration conf = getWifiApConfiguration();
                String ssidString=null;
                if(conf != null){
                    ssidString = conf.SSID;
                    listSsid.add(ssidString);
                    adapterSsid.notifyDataSetChanged();
                    for(;position < listSsid.size(); position++){
                        if(ssidString == null || ssidString.endsWith(listSsid.get(position))){
                            break;
                        }
                    }
                }
                mySpinner.setSelection(position);
                if(position == 0){
                    displayToast(ssidString==null?"null":ssidString);
                }
            }
            else
            {
                displayToast("网络不可用，请检查网络!");
            }
            adapter = new ResultAdapter(this, android.R.layout.simple_expandable_list_item_1, lstMac);
            listView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConfig();
    }

    //初始化页面
    private void initView(){
        btnConf = (Button)findViewById(R.id.btn_conf);
        btnConf.setOnClickListener(onButtonConfClick);
        //textSsid = (TextView)findViewById(R.id.text_ssid);
        text_total = (TextView) findViewById(R.id.text_total);
        mySpinner = (Spinner)findViewById(R.id.text_ssid);//下拉框
        adapterSsid = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, listSsid);
        adapterSsid.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapterSsid);
        mySpinner.setEnabled(true);

        editPsw = (EditText) findViewById(R.id.text_psw);
        listView = (ListView) findViewById(R.id.listView1);
        mySpinner.setOnItemSelectedListener(spinnerSelectListener);
        context = this;
    }

    /**
     * 配置网络按钮事件监听器
     */
    private View.OnClickListener onButtonConfClick = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            if(isStart){
                stopConfig();
                return;
            }
			String ssid = mySpinner.getSelectedItem().toString();
			if(ssid.length() == 0){
				displayToast("请先连接WIFI网络!");
				return;
			}
            psw = editPsw.getText().toString();
            lstMac.clear();
            adapter.notifyDataSetChanged();
            isStart = true;
            isThreadDisable = false;
            setEditable(false);
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//            udphelper = new UdpHelper(wifiManager);
//            tReceived = new Thread(udphelper);
//            tReceived.start();
            dataPrepare();
            new Thread(new ConfigReqThread()).start();
            //text_total.setText(String.format("%d connected.", lstMac.size()));
            btnConf.setText(getText(R.string.btn_stop_conf));
        }
    };

    private Spinner.OnItemSelectedListener spinnerSelectListener
            = new Spinner.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> arg0, View arg1,
        int arg2, long arg3) {
            String ssid = mySpinner.getSelectedItem().toString();
            String psw = hashmap.get(ssid);
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifiManager.isWifiEnabled() && psw != null){
                String ssidFormat = "\"%s\"";
                String ssidf = String.format(ssidFormat, ssid);
                int nId = -1;
                for(WifiConfiguration wifiConf : wifiManager.getConfiguredNetworks()){
                    if(wifiConf.SSID.equals(ssidf)){
                        nId = wifiConf.networkId;
                        break;
                    }
                }
                String ssidString=null;
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if(wifiInfo != null){
                    ssidString=wifiInfo.getSSID();
                    int version = getAndroidSDKVersion();
                    if(version > 16 && ssidString.startsWith("\"") && ssidString.endsWith("\"")){
                        ssidString = ssidString.substring(1, ssidString.length() - 1);
                    }
                }
                if(nId == -1){
                    WifiConfiguration wifiConfig = createWifiInfo(ssid, psw,
                            hashmapSecurity.get(ssid) == null ? TYPE_WPA : hashmapSecurity.get(ssid).intValue());
                    nId = wifiManager.addNetwork(wifiConfig);
                    wifiManager.saveConfiguration();
                }
                if(nId>0 && !ssid.equals(ssidString)){
                    wifiManager.enableNetwork(nId, true);
                }
            }
            editPsw.setText(psw);
            arg0.setVisibility(View.VISIBLE);
        }

        public void onNothingSelected(AdapterView<?> arg0) {
            editPsw.setText("");
            arg0.setVisibility(View.VISIBLE);
        }
    };

    /**
     * 停止配置
     */
    private void stopConfig(){
        if(isStart){
            isStart = false;
            btnConf.setEnabled(false);
        }
    }

    /**
     * 显示提示信息
     * @param str
     */
    public void displayToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置按钮状态
     * @param value
     */
    private void setEditable(boolean value) {
        if (value) {
            editPsw.setCursorVisible(true);
            editPsw.setFocusable(true);
            editPsw.setFocusableInTouchMode(true);
            editPsw.requestFocus();
        } else {
            editPsw.setCursorVisible(false);
            editPsw.setFocusable(false);
            editPsw.setFocusableInTouchMode(false);
            editPsw.clearFocus();
        }
    }

    /**
     * 获取android sdk
     * @return
     */
    private int getAndroidSDKVersion() {
        int version = 0;
        try {
            version = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
        } catch (NumberFormatException e) {
            Log.e(e.toString(), e.getMessage());
        }
        return version;
    }

    public WifiConfiguration createWifiInfo(String SSID, String password, int type) {

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        // 分为三种情况：1没有密码2用wep加密3用wpa加密
        if (type == TYPE_NO_PASSWD) {// WIFICIPHER_NOPASS
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;

        } else if (type == TYPE_WEP) {  //  WIFICIPHER_WEP
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WPA) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    /**判断热点开启状态*/
    public boolean isWifiApEnabled() {
        return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
    }

    private WIFI_AP_STATE getWifiApState(){
        int tmp;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            tmp = ((Integer) method.invoke(wifiManager));
            // Fix for Android 4
            if (tmp > 10) {
                tmp = tmp - 10;
            }
            return WIFI_AP_STATE.class.getEnumConstants()[tmp];
        } catch (Exception e) {
            e.printStackTrace();
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
        }
    }

    private WifiConfiguration getWifiApConfiguration(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration tmp = ((WifiConfiguration) method.invoke(wifiManager));

            return tmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void dataSend() {


    }

    void stateRead(){
//            BleManager.getInstance().read(
//                    bleDevice,
//                    UUID_SERVICE_WIFT,
//                    UUID_CHARARCTERISTIC_READ,
//                    new BleReadCallback() {
//                        @Override
//                        public void onReadSuccess(final byte[] data) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    addText(txt_connet_state, HexUtil.formatHexString(data, true));
//                                    Log.d(getClass().getSimpleName(),HexUtil.formatHexString(data, true));
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onReadFailure(final BleException exception) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    addText(txt_connet_state, exception.toString());
//                                    Log.d(getClass().getSimpleName(),exception.toString());
//                                }
//                            });
//                        }
//                    });

    }

    /**
     * 传输内容准备
     */
    private void dataPrepare() {
        messageEncode = new MessageEncode();
        byte apSSID = 1;
        byte apPASSWD = 2;
        byte apBSSID = 3;
        String ssid = mySpinner.getSelectedItem().toString();
        messageEncode.putValue(apSSID,ssid);
        messageEncode.putValue(apPASSWD, psw);
        //message编码
        messageEncode.encode();
    }

    //获取当前activity数据
    private void initData() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();
    }

    class ConfigReqThread implements Runnable {
        public void run() {
            WifiManager wifiManager = null;
            try {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if(wifiManager.isWifiEnabled() || isWifiApEnabled())
                {
                    for (MessagePacket messagePacket:messageEncode.getMessagePacketList()){
                        BleManager.getInstance().write(
                                bleDevice,
                                ConfigInfo.UUID_SERVICE_WIFT,
                                ConfigInfo.UUID_CHARARCTERISTIC_WRITE,
                                messagePacket.packageInfo,
                                new BleWriteCallback() {
                                    @Override
                                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                            Log.d(getClass().getSimpleName(),"write success, current: " + current
                                                        + " total: " + total
                                                        + " justWrite: " + HexUtil.formatHexString(justWrite, true));
                                            Toast.makeText(context, "配置报文下发成功", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onWriteFailure(final BleException exception) {
                                                Log.d(getClass().getSimpleName(),exception.toString());
                                                Toast.makeText(context, exception.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        Thread.sleep(10);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally{
                handler.post(confPost);
            }
        }
    }

    private Runnable confPost = new Runnable(){
        @Override
        public void run() {
            isStart=false;
            isThreadDisable = true;
            btnConf.setEnabled(true);
            setEditable(true);
            btnConf.setText(getText(R.string.btn_conf));
            if(adapter != null){
                adapter.notifyDataSetChanged();
            }
        }

    };
}
