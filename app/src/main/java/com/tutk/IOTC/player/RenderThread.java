package com.tutk.IOTC.player;

import android.view.SurfaceHolder;

/**
 * @Author: wangyj
 * @CreateDate: 2023/3/10
 * @Description:
 */
public class RenderThread extends Thread {
    private SurfaceHolder holder;

    private Object mWaitObjectForStopThread = new Object();
    public boolean mIsRunningThread = false;

    public RenderThread(SurfaceHolder holder) {
        this.holder = holder;
    }

    public void stopThread() {
        mIsRunningThread = false;
        try {
            mWaitObjectForStopThread.notify();
        } catch (Exception e) {

        }
    }

    @Override
    public void run() {
        try {
            synchronized (mWaitObjectForStopThread) {
                mWaitObjectForStopThread.wait(33);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
