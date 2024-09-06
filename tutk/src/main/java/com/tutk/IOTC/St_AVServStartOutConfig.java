/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AVServStartOutConfig                                                    *
 *                                                                            *
 * Author: Roger                                                              *
 *                                                                            *
 * Date: 2018/05/28                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

import androidx.annotation.Keep;

import java.util.Arrays;

//base on the struct _AVServStartOutConfig in AVAPIs.h
@Keep
public class St_AVServStartOutConfig
{
    public int resend;
    public int two_way_streaming;
    public int auth_type; //0:account & password  1:identity & token
    public byte[] account_or_identity = new byte[256];

    @Override
    public String toString() {
        return "St_AVServStartOutConfig{" +
                "resend=" + resend +
                ", two_way_streaming=" + two_way_streaming +
                ", auth_type=" + auth_type +
                ", account_or_identity=" + Arrays.toString(account_or_identity) +
                '}';
    }
}