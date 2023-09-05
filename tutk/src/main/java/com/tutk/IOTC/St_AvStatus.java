package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class St_AvStatus {
    public int AvVersion;
    public int RoundTripTime;
    public int LostRate;
    public int BandWidth;
    public int MinRTT;
    public int LastBw;
    public int LastRtt;
    public int LastCwnd;
    public int InFlight;
}
