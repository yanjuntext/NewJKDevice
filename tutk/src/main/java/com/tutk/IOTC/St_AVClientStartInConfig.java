/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AVClientStartInConfig                                                    *
 *                                                                            *
 * Author: Roger                                                              *
 *                                                                            *
 * Date: 2018/05/28                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

//base on the struct AVClientStartInConfig in AVAPIs.h'

import androidx.annotation.Keep;

@Keep
public class St_AVClientStartInConfig
{
    public int iotc_session_id;
    public int iotc_channel_id;
    public int timeout_sec;
    public String account_or_identity= null;
    public String password_or_token= null;
    public int resend;
    public int security_mode; //0:simple 1:DTLS
    public int auth_type; //0:account & password  1:identity & token
    public int sync_recv_data;
    public String dtls_cipher_suites = null;
}

