package com.encoder.util;

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/16
 * @Description:
 */
public class EncSpeex {
    public static native int InitEncoder(int quality);
    public static native int UninitEncoder();
    public static native int Encode(short[] in, int in_size, byte[] out);

    static {

        try {
            System.loadLibrary("SpeexAndroid");
        }
        catch (UnsatisfiedLinkError ule){
            System.out.println("loadLibrary(SpeexAndroid),"+ule.getMessage());
        }
    }
}
