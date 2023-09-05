package com.tutk.IOTC;

import androidx.annotation.Keep;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
@Keep
public class St_DeviceLoginInput {
    public int        authenticationType;
    public String     authKey; //only allow '0'~'9' & 'A'~'Z' & 'a'~'z'
}
