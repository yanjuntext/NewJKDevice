package com.tutk.IOTC;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
public class St_RDT_Status {
    public short Timeout;  //Keepalive timeout for how many seconds
    public short TimeoutThreshold;  //when timeout reach this value will break RDT connection
    public int BufSizeInSendQueue; //byte
    public int BufSizeInRecvQueue; //byte
}
