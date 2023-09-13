package com.tutk.IOTC;

import androidx.annotation.Keep;

@Keep
public class St_IOTCConnectInput{
    public int        authenticationType;
    public String     authKey; //only allow '0'~'9' & 'A'~'Z' & 'a'~'z'
    public int        timeout;
}