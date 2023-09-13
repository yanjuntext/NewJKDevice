/******************************************************************************
 *                                                                            *
 * Copyright (c) 2011 by TUTK Co.LTD. All Rights Reserved.                    *
 *                                                                            *
 *                                                                            *
 * Class: St_AVServStartInConfig                                                    *
 *                                                                            *
 * Author: Roger                                                              *
 *                                                                            *
 * Date: 2018/05/28                                                           *
 *                                                                            *
 ******************************************************************************/

package com.tutk.IOTC;

import androidx.annotation.Keep;

//base on the struct _AVServStartInConfig in AVAPIs.h
@Keep
public class St_AVServStartInConfig
{
    public int iotc_session_id;
    public int iotc_channel_id;
    public int timeout_sec;
    public int server_type;
    public int resend;
    public int security_mode; // 0:simple 1:DTLS
    public AVAPIs.avPasswordAuthFn password_auth = null;
    public AVAPIs.avTokenAuthFn token_auth = null;
    public AVAPIs.avTokenRequestFn token_request = null;
    public AVAPIs.avTokenDeleteFn token_delete = null;
    public AVAPIs.avIdentityArrayRequestFn identity_array_request = null;
    public AVAPIs.avAbilityRequestFn ability_request = null;
    public AVAPIs.avChangePasswordRequestFn change_password_request = null;
    public AVAPIs.avJsonCtrlRequestFn json_request = null;
    public String dtls_cipher_suites = null;
}
