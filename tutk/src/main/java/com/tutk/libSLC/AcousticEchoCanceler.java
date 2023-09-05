package com.tutk.libSLC;

import android.util.Log;

import androidx.annotation.Keep;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @Author: wangyj
 * @CreateDate: 2022/12/5
 * @Description:
 */
@Keep
public class AcousticEchoCanceler {
    private static int b;
    private static int c;
    private Queue<byte[]> a = new LinkedList();
    private int d = 64;
    private boolean e = false;

    public AcousticEchoCanceler() {
    }

    private native boolean nativeInit(int var1, int var2, int var3, int var4);

    private native void nativeTerminate();

    private native void nativeProcessSpeakerFrame(byte[] var1, int var2, int var3);

    private native int nativeProcessMicFrame(byte[] var1, int var2, int var3);

    public static synchronized void setDelaySize(int var0, int var1) {
        b = var0;
        c = var1;
    }

    static {
        System.loadLibrary("SLCAec");
    }

    public void Capture(byte[] var1, int var2) {
        if (this.e) {
            if (this.a.size() > 100) {
                this.a.clear();
            }

            var2 /= this.d;
            int var3 = 0;

            while(var3 < var2) {
                int var4;
                byte[] var5 = Arrays.copyOfRange(var1, var3 * (var4 = this.d), ++var3 * var4);
                this.a.offer(var5);
            }

            Log.i("tocoAudio", " CaptureQueue.size():" + this.a.size());
        }
    }

    public synchronized boolean Play(byte[] var1, int var2) {
        if (!this.e) {
            return true;
        } else {
            var2 /= this.d;
            boolean var3 = true;

            for(int var4 = 0; var4 < var2; ++var4) {
                AcousticEchoCanceler var10000 = this;
                Object var5 = null;

                byte[] var6;
                try {
                    var6 = (byte[])var10000.a.remove();
                } catch (NoSuchElementException var7) {
                    var6 = (byte[])var5;
                }

                if (var6 == null) {
                    var6 = new byte[this.d];
                    var3 = false;
                }

                this.nativeProcessSpeakerFrame(var6, 0, var6.length);
                int var8;
                this.nativeProcessMicFrame(var1, var4 * (var8 = this.d), var8);
            }

            return var3;
        }
    }

    public synchronized void close() {
        this.nativeTerminate();
        this.a.clear();
        this.e = false;
    }

    public synchronized void Open(int var1, int var2) {
        this.d = var1 / 8000 * 64;
        this.nativeInit(var1, var2, b, c);
        this.e = true;
    }
}
