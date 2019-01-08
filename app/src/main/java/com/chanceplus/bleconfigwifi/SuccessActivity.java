package com.chanceplus.bleconfigwifi;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class SuccessActivity extends AppCompatActivity {

    public static final String KEY_IP = "key_ip";
    public static final String KEY_MAC = "key_mac";
    private TextView text_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
        text_info = (TextView)findViewById(R.id.text_info);

        String ip = getIntent().getStringExtra(KEY_IP);
        String mac = getIntent().getStringExtra(KEY_MAC);
        text_info.setText("ip :" + ip + " mac:" + mac);
    }
}
