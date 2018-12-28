package com.chanceplus.bleconfigwifi.util;

public class MessagePacket {
    public byte cmd;
    public byte org_id;
    public byte length;
    public byte no;
    public byte[] payload;
    public byte crc;
    public byte[] packageInfo;

    public void buildPacket() {
        packageInfo = new byte[payload.length + 5];
        packageInfo[0] = this.cmd;
        packageInfo[1] = this.org_id;
        packageInfo[2] = this.length;
        packageInfo[3] = this.no;

        int i = 1;
        for (byte byteItem : payload){
            packageInfo[3+i] = byteItem;
            i++;
        }

        packageInfo[3+i] = this.caculateCRC(this.payload);
    }

    private byte caculateCRC(byte[] payload){
        int intCRC = 0;
        byte[] byteCRC = new byte[4];
        for (byte byteItem:payload) {
            intCRC += MessagePacket.byteToInt(byteItem);
        }

        if (0xFF < intCRC){
            intCRC = (intCRC >> 8) + (intCRC & 0xFF);
            intCRC += (intCRC >> 8);
        }

        byteCRC = MessagePacket.intToByteArray(intCRC);

        return byteCRC[0];
    }

    public static byte intToByte(int x) {
        return (byte) (x & 0xFF);
    }
    public static int byteToInt(byte b) {
        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
        return b & 0xFF;
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) (a & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 24) & 0xFF)
        };
    }
}
