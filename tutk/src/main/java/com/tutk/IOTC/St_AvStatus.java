/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AvStatus                                                         *
 *                                                                            *
 * Author: C.T                                                                *
 *                                                                            *
 * Date: 2018/10/08                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

import androidx.annotation.Keep;

//base on the struct st_AvStatus in AVAPIs.h
@Keep
public class St_AvStatus
{
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
