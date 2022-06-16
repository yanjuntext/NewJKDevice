package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class St_AVClientStartInConfig {
    public int iotc_session_id;
    public int iotc_channel_id;
    public int timeout_sec;
    public String account_or_identity= null;
    public String password_or_token= null;
    public int resend;
    public int security_mode; //0:simple 1:DTLS
    public int auth_type; //0:account & password  1:identity & token
}
