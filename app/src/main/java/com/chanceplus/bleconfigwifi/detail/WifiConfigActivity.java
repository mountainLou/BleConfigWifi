package com.chanceplus.bleconfigwifi.detail;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.chanceplus.bleconfigwifi.R;
import com.chanceplus.bleconfigwifi.SuccessActivity;
import com.chanceplus.bleconfigwifi.comm.Observer;
import com.chanceplus.bleconfigwifi.comm.ObserverManager;
import com.chanceplus.bleconfigwifi.filter.TextLengthWatcher;
import com.chanceplus.bleconfigwifi.util.ConfigInfo;
import com.chanceplus.bleconfigwifi.util.MessageEncode;
import com.chanceplus.bleconfigwifi.util.MessagePacket;
import com.chanceplus.bleconfigwifi.util.Tools;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.wrbug.editspinner.EditSpinner;
import com.wrbug.editspinner.SimpleAdapter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import idea.analyzesystem.android.edittext.mac.MacView;

public class WifiConfigActivity extends AppCompatActivity implements Observer {

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
    private EditSpinner mySpinner;
    //ssid下拉列表
    private List<String> listSsid = new ArrayList<String>();
//    private ArrayAdapter<String> adapterSsid;
    private SimpleAdapter simpAdapter;
    private EditText editPsw;
    private MacView editBssid;
    //自定义数据下发
    private EditText editCustomData;
    private Button btnSendCustomData;

    //开始配置标志
    private boolean isStart = false;
    private String psw = null;
    private String ssid = null;
    private String bssid = null;
    private String customData = null;
    private boolean isThreadDisable = false;
    private HashMap<String, String> hashmap = new HashMap<String, String>();
    private HashMap<String, Integer> hashmapSecurity = new HashMap<String, Integer>();
    private Handler handler = new Handler();
    private UdpHelper udphelper;
    private Thread tReceived;
    private String mac = "";
    private String ip = "";

    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            displayToast("蓝牙设备断开连接！");
            finish();
        }
    }

    public enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING,  WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);
        initView();
        initData();

        ObserverManager.getInstance().addObserver(this);
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
                    if(r.SSID.isEmpty()){
                        continue;
                    }
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
                //listSsid.add("空");
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssidString=null;
                if(wifiInfo != null){
                    ssidString=wifiInfo.getSSID();
                    int version = getAndroidSDKVersion();
                    if(version > 16 && ssidString.startsWith("\"") && ssidString.endsWith("\"")){
                        ssidString = ssidString.substring(1, ssidString.length() - 1);
                    }
                }
                mySpinner.setText(ssidString);
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
                }
                mySpinner.setText(ssidString);
            }
            else
            {
                displayToast("网络不可用，请检查网络!");
            }

            simpAdapter = new SimpleAdapter(this,listSsid);
            mySpinner.setAdapter(simpAdapter);
            mySpinner.setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(confPost);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConfig();
    }

    //初始化页面
    private void initView(){
        context = this;

        btnConf = (Button)findViewById(R.id.btn_conf);
        btnConf.setOnClickListener(onButtonConfClick);

        mySpinner = (EditSpinner) findViewById(R.id.text_ssid);
        mySpinner.setOnItemClickListener(spinnerSelectListener);
        mySpinner.setMaxLength(ConfigInfo.MAX_LENGHT_SSID);

        editPsw = (EditText) findViewById(R.id.text_psw);
        editPsw.addTextChangedListener(
                new TextLengthWatcher(ConfigInfo.MAX_LENGHT_PWD,editPsw)
        );

        editBssid = (MacView) findViewById(R.id.macView);
        editCustomData = (EditText) findViewById(R.id.text_custom);
        editCustomData.addTextChangedListener(
                new TextLengthWatcher(ConfigInfo.MAX_LENGHT_CUSTOMDATA,editCustomData)
        );
        btnSendCustomData = (Button) findViewById(R.id.btn_sendcustomdata);
        btnSendCustomData.setOnClickListener(onButtonSendCustomDataClick);
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
			ssid = mySpinner.getText();
//            if (ssid == "空"){
//                ssid = "";
//            }
            psw = editPsw.getText().toString();
            bssid = editBssid.getMacAddress();
            if(!dataPrepare()){
                return;
            }
            isStart = true;//开始配置
            isThreadDisable = false;
            setEditable(false);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            udphelper = new UdpHelper(wifiManager);
            tReceived = new Thread(udphelper);
            tReceived.start();
            new Thread(new ConfigReqThread(ConfigInfo.UUID_CHARARCTERISTIC_WRITE)).start();
        }
    };

    private View.OnClickListener onButtonSendCustomDataClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            customData = editCustomData.getText().toString();
            if(!customDataPrepare()){
                return;
            }
            setEditable(false);
            new Thread(new ConfigReqThread(ConfigInfo.UUID_CHARARCTERISTIC_CUSTOM_WRITE)).start();
        }
    };

    private AdapterView.OnItemClickListener spinnerSelectListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String ssid = mySpinner.getText();
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
            btnConf.setEnabled(true);
            btnConf.setText(getText(R.string.btn_conf));
            btnSendCustomData.setEnabled(true);
        } else {
            editPsw.setCursorVisible(false);
            editPsw.setFocusable(false);
            editPsw.setFocusableInTouchMode(false);
            editPsw.clearFocus();
            btnConf.setEnabled(false);
            btnConf.setText(getText(R.string.btn_stop_conf));
            btnSendCustomData.setEnabled(false);
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


    /**
     * 传输内容准备
     */
    private boolean dataPrepare() {
        messageEncode = new MessageEncode();
        byte apSSID = 1;
        byte apPASSWD = 2;
        byte apBSSID = 3;

        if((bssid.isEmpty()&&ssid.isEmpty())
            ||psw.isEmpty()){
            displayToast("无效的配置组合");
            return false;
        }

        Collator collator = Collator.getInstance(Locale.CHINA);
        if (!ssid.isEmpty()){
            if(Tools.getInputLength(ssid) > ConfigInfo.MAX_LENGHT_SSID){
                displayToast("ssid 超出有效长度");
                return false;
            }
            messageEncode.putStringValue(apSSID,ssid);
        }
        if (!psw.isEmpty()){
            if(Tools.getInputLength(psw) > ConfigInfo.MAX_LENGHT_PWD){
                displayToast("password 超出有效长度");
                return false;
            }
            messageEncode.putStringValue(apPASSWD, psw);
        }
        if (!bssid.isEmpty()){
            byte[] bssidBytes = new byte[6];
            if(Tools.macString2byte(bssid, bssidBytes))
            {
                if(bssidBytes.length != 6)
                {
                    displayToast("bssid 长度异常");
                    return false;
                }
                messageEncode.putBytesValue(apBSSID,bssidBytes);
            }
        }

        //message编码
        messageEncode.encode();
        return true;
    }

    /**
     * 传输内容准备
     */
    private boolean customDataPrepare() {
        messageEncode = new MessageEncode();
        byte customDataID = 4;
        if(Tools.getInputLength(customData) > ConfigInfo.MAX_LENGHT_CUSTOMDATA){
            displayToast("customData 超出有效长度");
            return false;
        }
        messageEncode.putStringValue(customDataID,customData);
        //message编码
        messageEncode.encode();
        return true;
    }

    //获取当前activity数据
    private void initData() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null)
            finish();
    }

    class ConfigReqThread implements Runnable {
        public String charaType = null;

        public ConfigReqThread(String charaType) {
            this.charaType = charaType;
        }

        public void run() {
            try {
                for (MessagePacket messagePacket:messageEncode.getMessagePacketList()){
                    BleManager.getInstance().write(
                            bleDevice,
                            ConfigInfo.UUID_SERVICE_WIFT,
                            charaType,
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

                    Thread.sleep(50);
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

            setEditable(true);
            btnConf.setText(getText(R.string.btn_conf));
        }

    };

    private Runnable notifyPost = new Runnable(){
        @Override
        public void run() {
            displayToast("网络连接成功");
            Intent intent = new Intent(getApplicationContext(), SuccessActivity.class);
            intent.putExtra(SuccessActivity.KEY_IP, ip);
            intent.putExtra(SuccessActivity.KEY_MAC, mac);
            startActivity(intent);
        }
    };

    class UdpHelper implements Runnable {

        private WifiManager.MulticastLock lock;
        InetAddress mInetAddress;
        public UdpHelper(WifiManager manager) {
            this.lock= manager.createMulticastLock("UDPwifi");
        }
        public void StartListen()  {
            // UDP服务器监听的端口
            Integer port = 65534;
            // 接收的字节大小，客户端发送的数据不能超过这个大小
            byte[] message = new byte[100];
            try {
                // 建立Socket连接
                DatagramSocket datagramSocket = new DatagramSocket(port);
                datagramSocket.setBroadcast(true);
                datagramSocket.setSoTimeout(5000);
                DatagramPacket datagramPacket = new DatagramPacket(message,
                        message.length);
                try {
                    while (!isThreadDisable) {
                        // 准备接收数据
                        Log.d("UDP Demo", "准备接受");
                        this.lock.acquire();
                        try{
                            datagramSocket.receive(datagramPacket);
                            String strMsg="";
                            int count = datagramPacket.getLength();
                            for(int i=0;i<count;i++){
                                mac += String.format("%02x", datagramPacket.getData()[i]);
                            }
                            ip = datagramPacket.getAddress().getHostAddress().toString();
                            strMsg = mac + ";";
                            Log.d("UDP Demo", datagramPacket.getAddress()
                                    .getHostAddress().toString()
                                    + ":" +strMsg );
                            handler.post(notifyPost);
                        }
                        catch(SocketTimeoutException ex){
                            Log.d("UDP Demo", "UDP Receive Timeout.");
                        }
                        this.lock.release();
                    }
                } catch (IOException e) {//IOException
                    e.printStackTrace();
                }
                datagramSocket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            finally{
                if(!isThreadDisable){
                    handler.post(confPost);
                }
            }
        }
        @Override
        public void run() {
            StartListen();
        }
    }
}
