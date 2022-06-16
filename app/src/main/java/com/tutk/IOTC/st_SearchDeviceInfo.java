package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class st_SearchDeviceInfo {
    public byte[] UID= null;
    public byte[] IP = null;
    public int port=0;
    public byte[] DeviceName= null;
    public byte Reserved;
    public int nNew=0;
}
