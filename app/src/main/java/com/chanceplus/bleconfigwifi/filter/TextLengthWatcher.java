package com.chanceplus.bleconfigwifi.filter;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

import com.chanceplus.bleconfigwifi.util.Tools;

public class TextLengthWatcher implements TextWatcher {
    private int maxLength;
    private EditText beWatched;

    public TextLengthWatcher(int maxLength, EditText beWatched) {
        this.maxLength = maxLength;
        this.beWatched = beWatched;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String inputMsg = s.toString().trim();
        if (!TextUtils.isEmpty(inputMsg)) {
            String limitMsg = Tools.getLimitInput(inputMsg, maxLength);
            if (!TextUtils.isEmpty(limitMsg)) {
                if (!limitMsg.equals(inputMsg)) {
                    beWatched.setText(limitMsg);
                    // 设置光标位置
                    beWatched.setSelection(limitMsg.length());
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
