package com.tutk.webtrc;

import android.content.Context;
import android.media.AudioRecord;

import com.ingenic.api.AudioFrame;
import com.ingenic.api.Frequency;
import com.ingenic.api.OnTSUpdateListener;
import com.ingenic.api.RecodeAudioDataListener;

/**
 * @Author: wangyj
 * @CreateDate: 2022/3/1
 * @Description: 双向语音
 */
public class MyAudioPlayer implements a.OnRecordVolumeListener {
    private boolean enableSystemAEC;
    private boolean mHasInit;
    private a mPlayer;
    private OnAudioPlayerVolumeChangeListener listener;



    private MyAudioPlayer() {
        this.enableSystemAEC = true;
    }

    public static MyAudioPlayer getInstance() {
        return MyAudioPlayerHolder.instance;
    }

    public MyAudioPlayer init(Context var1, RecodeAudioDataListener var2) {
        return this.init(var1, var2, Frequency.PCM_16K);
    }

    public MyAudioPlayer init(Context var1, RecodeAudioDataListener var2, Frequency var3) {
        return this.init(var1, var2, var3, var3.value() / 10);
    }

    public synchronized MyAudioPlayer init(Context var1, RecodeAudioDataListener var2, Frequency var3, int var4) {
        if (this.mHasInit) {
            this.stop();
        }

        if (var1 == null) {
            throw new RuntimeException("context can not be null");
        } else {
            this.mHasInit = true;
            this.mPlayer = new a(var1.getApplicationContext(), var2, var3, var4);
            this.mPlayer.setOnRecordVolumeListener(this);
            return this;
        }
    }

    public void start(boolean var1, boolean var2) {
        if (!this.mHasInit) {
            throw new RuntimeException("must init before start");
        } else {
            this.mPlayer.a(var1, var2);
        }
    }

    public boolean putPlayData(byte[] var1, int var2) {
        return this.mPlayer != null ? this.mPlayer.a(var1, var2) : false;
    }

    public boolean putPlayData(AudioFrame var1) {
        return this.mPlayer != null ? this.mPlayer.a(var1) : false;
    }

    public void setPtsUpdateListener(OnTSUpdateListener var1) {
        if (this.mPlayer != null) {
            this.mPlayer.a(var1);
        }

    }

    public byte[] getFilterData() {
        return this.mPlayer != null ? this.mPlayer.i() : null;
    }

    public void stop() {
        if (this.mPlayer != null) {
            this.mPlayer.a();
        }

    }

    public void release() {
        this.mHasInit = false;
        if (this.mPlayer != null) {
            this.mPlayer.b();
        }

    }

    public void soundOn() {
        if (this.mPlayer != null) {
            this.mPlayer.c();
        }

    }

    public void soundOff() {
        if (this.mPlayer != null) {
            this.mPlayer.d();
        }

    }

    public boolean isSoundOn() {
        return this.mPlayer != null ? this.mPlayer.e() : false;
    }

    public void resumeAudioRecord() {
        if (this.mPlayer != null) {
            this.mPlayer.f();
        }

    }

    public void pauseAudioRecord() {
        if (this.mPlayer != null) {
            this.mPlayer.g();
        }

    }

    public boolean isAudioRecord() {
        return this.mPlayer != null ? this.mPlayer.h() : false;
    }

    public MyAudioPlayer disableSystemAEC() {
        this.enableSystemAEC = false;
        return this;
    }

    public MyAudioPlayer enableSystemAEC() {
        this.enableSystemAEC = true;
        return this;
    }

    @Override
    public void onVolume(double volume) {
        try {
            if(listener != null){
                listener.onVolumeChange(volume);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static class MyAudioPlayerHolder {
        private static MyAudioPlayer instance = new MyAudioPlayer();

        private MyAudioPlayerHolder() {
        }
    }

    public AudioRecord getAudioRecord(){
        if(mPlayer != null){
            return mPlayer.getAudioRecord();
        }
        return null;
    }

    public void setOnAudioPlayerVolumeChangeListener(OnAudioPlayerVolumeChangeListener listener){
        this.listener = listener;
    }

    public interface OnAudioPlayerVolumeChangeListener{
        void onVolumeChange(double volume);
    }
}
