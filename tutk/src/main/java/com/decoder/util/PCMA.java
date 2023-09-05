package com.decoder.util;

/**
 * @Author: wangyj
 * @CreateDate: 2021/10/8
 * @Description:
 */
public class PCMA extends G711Base{
    //G711 转PCM
    public static void alaw2linear(byte alaw[], short lin[], int frames) {
        int i;
        for (i = 0; i < frames; i++)
            lin[i] = a2s[alaw[i] & 0xff];
    }

    public static byte[] alaw2linear(byte alaw[], int frames) {
        int i;
        byte[] retArr = new byte[frames * 2];
        short linTmp;
        int retArrPos = 0;

        for (i = 0; i < frames; i++) {
            //lin[i] = a2s[alaw[i] & 0xff];
            linTmp = a2s[alaw[i] & 0xff];
            retArrPos = i * 2;
            retArr[retArrPos] = (byte) (linTmp & 0xff);
            retArr[retArrPos + 1] = (byte) ((linTmp >> 8) & 0xff);
        }

        return retArr;
    }

    public static void alaw2linear(byte alaw[], short lin[], int frames, int mu) {
        int i;
        for (i = 0; i < frames; i++)
            lin[i] = a2s[alaw[i / mu] & 0xff];
    }

    //PCM 转G711
    public static void linear2alaw(short lin[], int offset, byte alaw[], int frames) {
        int i;
        for (i = 0; i < frames; i++)
            alaw[i] = s2a[lin[i + offset] & 0xffff];
    }
}
