package com.chanceplus.bleconfigwifi.util;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class Tools {
    public static boolean macString2byte(String macString, byte[] bytes){
        boolean ret = false;
        if (bytes.length != 6){
            ret = false;
            return ret;
        }

        String[] macStrings = macString.split(":");
        if (macStrings.length != 6){
            ret = false;
            return ret;
        }

        for (int i = 0; i < 6; i++) {
            try {
                Integer subMacIntegar =  Integer.valueOf(macStrings[i],16);
                bytes[i] = MessagePacket.intToByte(subMacIntegar.intValue());
            }
            catch (NumberFormatException e){
                Log.d("%s",e.getMessage());
                ret = false;
                return ret;
            }
        }

        ret = true;
        return ret;
    }

    public static String getLimitInput(String inputStr, int limitLength) {
        int orignLen = inputStr.length();
        int resultLen = 0;
        String temp = null;
        for (int i = 0; i < orignLen; i++) {
            temp = inputStr.substring(i, i + 1);
            try {// 3 bytes to indicate chinese word,1 byte to indicate english
                // word ,in utf-8 encode
                if (temp.getBytes("utf-8").length == 3) {
                    resultLen += 2;
                } else {
                    resultLen++;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (resultLen > limitLength) {
                return inputStr.substring(0, i);
            }
        }
        return inputStr;
    }

    public static int getInputLength(String inputStr) {
        int orignLen = inputStr.length();
        int resultLen = 0;
        String temp = null;
        for (int i = 0; i < orignLen; i++) {
            temp = inputStr.substring(i, i + 1);
            try {// 3 bytes to indicate chinese word,1 byte to indicate english
                // word ,in utf-8 encode
                if (temp.getBytes("utf-8").length == 3) {
                    resultLen += 2;
                } else {
                    resultLen++;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return resultLen;
    }
}
