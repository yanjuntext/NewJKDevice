package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class St_AVServStartInConfig {
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
}
