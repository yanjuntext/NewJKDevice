package com.tutk.IOTC;

import androidx.annotation.Keep;

@Keep
public class st_SearchDeviceInfo {
	public byte[] UID= null;
	public byte[] IP = null;
	public int port=0;
	public byte[] DeviceName= null;
	public byte Reserved;
	public int nNew=0;
}
