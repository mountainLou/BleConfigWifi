package com.chanceplus.bleconfigwifi.util;

import android.util.Log;

import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;

public class MessageEncode {
    private ArrayList<MessagePacket> messagePacketList;
    private TlvBox tlv;

    public MessageEncode(){
        messagePacketList = new ArrayList<MessagePacket>();
        tlv = new TlvBox();
    }

    public ArrayList<MessagePacket> getMessagePacketList(){
        return messagePacketList;
    }

    /**
     * 增加数据
     * @param type
     * @param value
     */
    public void putValue(byte type ,String value){
        tlv.putStringValue(MessagePacket.byteToInt(type),value);
    }

    /**
     * 返回tlv内容
     * @return
     */
    public TlvBox getTlv(){
        return this.tlv;
    }

    /**
     * 组包分片
     */
    public void encode(){
        // 获取tlv内容
        tlv.serialize();
        int i = 0;
        for (byte[] curBytes:tlv.getTlvList()) {
            MessagePacket pMessagePacket = new MessagePacket();
            pMessagePacket.cmd = Integer.valueOf("0A", 16).byteValue();
            pMessagePacket.org_id = Integer.valueOf("CC", 16).byteValue();
            pMessagePacket.length = MessagePacket.intToByte(tlv.getTotalValuesLength());
            pMessagePacket.no = MessagePacket.intToByte(i+1);
            pMessagePacket.payload = curBytes;
            pMessagePacket.buildPacket();
            messagePacketList.add(pMessagePacket);
            Log.d(getClass().getSimpleName(),"current packet: " + HexUtil.formatHexString(pMessagePacket.packageInfo, true));
            i++;
        }
    }
}
