package com.chanceplus.bleconfigwifi.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import android.util.SparseArray;

public class TlvBox {

    private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    private SparseArray<byte[]> mObjects;
    private int mTotalValues = 0;
    public static final int PACKETMAXLENGTH = 15;
    private ArrayList<byte[]> TlvList = new ArrayList<byte[]>();
    //临时缓存
    TlvPacket bufferPacket = new TlvPacket(PACKETMAXLENGTH);

    public TlvBox() {
        mObjects = new SparseArray<byte[]>();
    }
    public int getTotalValuesLength(){
        return this.mTotalValues;
    }
    public ArrayList<byte[]> getTlvList(){return this.TlvList;}

    public void serialize() {
        int i = 0;
        if (mObjects.size() == 0){return;}
        int key = mObjects.keyAt(i);
        byte[] curObject = mObjects.get(key);
        bufferPacket.clearBufferList();
        while(curObject!=null) {
            // 获取缓存长度
            int offset = bufferPacket.getLength();
            if ((curObject.length + offset + 2) <= PACKETMAXLENGTH) {
                // 将当前数据放进缓存
                byte type   = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(key).get(0);
                byte length = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(curObject.length).get(0);
                bufferPacket.content[offset] = type;
                offset += 1;
                bufferPacket.content[offset] = length;
                offset += 1;
                System.arraycopy(curObject, 0, bufferPacket.content, offset, curObject.length);
                offset += curObject.length;
                bufferPacket.setLength(offset);
                //取下一个元素
                i++;
                if (i < mObjects.size()){
                    key = mObjects.keyAt(i);
                    curObject = mObjects.get(key);
                }
                else{
                    curObject = null;
                    key = mObjects.keyAt(i);
                    //处理缓存剩余的内容
                    byte[] newTlv =  new byte[bufferPacket.getLength()];
                    System.arraycopy(bufferPacket.content, 0, newTlv, 0, bufferPacket.getLength());
                    TlvList.add(newTlv);
                    //清空缓存
                    bufferPacket.clearBufferList();
                }
            }
            else{
                //截取一段放入当前buffer，剩下的放入mObjects
                int cacheLength = PACKETMAXLENGTH - offset - 2;
                if (cacheLength <= 0){
                    //剩下的空间不够存放一条数据，取出缓存内容创建
                    byte[] newTlv =  new byte[bufferPacket.getLength()];
                    System.arraycopy(bufferPacket.content, 0, newTlv, 0, bufferPacket.getLength());
                    TlvList.add(newTlv);
                    //清空缓存
                    bufferPacket.clearBufferList();
                }
                else{
                    byte type   = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(key).get(0);
                    byte length = MessagePacket.intToByte(cacheLength);
                    bufferPacket.content[offset] = type;
                    offset += 1;
                    bufferPacket.content[offset] = length;
                    offset += 1;
                    System.arraycopy(curObject, 0, bufferPacket.content, offset, cacheLength);
                    TlvList.add(bufferPacket.content);
                    bufferPacket.clearBufferList();
                    //讲剩下的内容放入mObjects
                    int remainLength = curObject.length - cacheLength;
                    byte[] newBytes = new byte[remainLength];
                    System.arraycopy(curObject, cacheLength, newBytes, 0, remainLength);
                    curObject = newBytes;
                }
            }
        }
    }

    public void putByteValue(int type,byte value) {
        byte[] bytes = new byte[1];
        bytes[0] = value;
        putBytesValue(type, bytes);
    }

    public void putShortValue(int type,short value) {
        byte[] bytes = ByteBuffer.allocate(2).order(DEFAULT_BYTE_ORDER).putShort(value).array();
        putBytesValue(type, bytes);
    }

    public void putIntValue(int type,int value) {
        byte[] bytes = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putInt(value).array();
        putBytesValue(type, bytes);
    }

    public void putLongValue(int type,long value) {
        byte[] bytes = ByteBuffer.allocate(8).order(DEFAULT_BYTE_ORDER).putLong(value).array();
        putBytesValue(type, bytes);
    }

    public void putFloatValue(int type,float value) {
        byte[] bytes = ByteBuffer.allocate(4).order(DEFAULT_BYTE_ORDER).putFloat(value).array();
        putBytesValue(type, bytes);
    }

    public void putDoubleValue(int type,double value) {
        byte[] bytes = ByteBuffer.allocate(8).order(DEFAULT_BYTE_ORDER).putDouble(value).array();
        putBytesValue(type, bytes);
    }

    public void putStringValue(int type,String value) {
        putBytesValue(type,value.getBytes());
    }


    public void putBytesValue(int type,byte[] value) {
        mObjects.put(type, value);
        mTotalValues += value.length;
    }
}