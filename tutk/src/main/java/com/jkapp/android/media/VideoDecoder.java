package com.jkapp.android.media;

import android.content.Context;

import java.nio.ByteBuffer;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/28
 * @Description: H264/H265视频解码
 */
public class VideoDecoder {

    public static final int COLOR_FORMAT_YUV420 = 0;
    public static final int COLOR_FORMAT_RGB565LE = 1;
    public static final int COLOR_FORMAT_BGR32 = 2;

    public VideoDecoder(int colorFormat, int avCodeId, Context cont,int checkYCount) {
        if (cont == null) {
            return;
        }
        nativeInit(colorFormat, avCodeId, cont,checkYCount);
    }

    public void unfinalize() {
        nativeDestroy();
    }

    public int cdata;

    /**
     * @param colorFormat
     * @param avCodeId    0=H264  1=H265
     */
    private native void nativeInit(int colorFormat, int avCodeId, Context context,int checkYCount);

    private native void nativeDestroy();

    public native int consumeNalUnitsFromDirectBuffer(ByteBuffer nalUnits, int numBytes, long packetPTS);

    public native boolean isFrameReady();

    public native int getWidth();

    public native int getHeight();

    public native int getOutputByteSize();

    public native long decodeFrameToDirectBuffer(ByteBuffer buffer);
    public native int decode(ByteBuffer nalUnits, int numBytes, long packetPTS, ByteBuffer outBuffer);
    public native int decodeNew(ByteBuffer nalUnits, int numBytes, long packetPTS, ByteBuffer outBuffer);
    public native int decodeWithYUV(ByteBuffer nalUnits, int numBytes, long packetPTS, ByteBuffer outBuffer,byte[] yuvBuffer);

    public native void releaseCache();
//    //mutile coder
//    private native int nativeInitCreateCoderV2(int colorFormat, Context context); //多设备
//    private native void nativeReleaseCoderV2(int codeIndex);
//    public native void consumeNalUnitsFromDirectBufferV2(int coderIndex, ByteBuffer nalUnits, int numBytes, long packetPTS);
//    public native boolean isFrameReadyV2(int coderIndex );
//    public native int getWidthV2(int coderIndex );
//    public native int getHeightV2(int coderIndex );
//    public native int getOutputByteSizeV2(int coderIndex );
//    public native long decodeFrameToDirectBufferV2(int coderIndex, ByteBuffer buffer);


    static {
        try {
            System.loadLibrary("newvideodecoder");
        } catch (UnsatisfiedLinkError ule) {
            System.out.println("videodecoder error," + ule.getMessage());
        }
    }
}
