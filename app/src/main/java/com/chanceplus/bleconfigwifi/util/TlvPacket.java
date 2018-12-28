package com.chanceplus.bleconfigwifi.util;

public class TlvPacket {
    public byte[] content;
    public int length;
    public int PACKETMAXLENGTH = 15;

    public TlvPacket(int length){
        content = new byte[length];
        this.length = 0;
    }

    public void clearBufferList() {
        content = new byte[PACKETMAXLENGTH];
        this.length = 0;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength(){
        return this.length;
    }
}
