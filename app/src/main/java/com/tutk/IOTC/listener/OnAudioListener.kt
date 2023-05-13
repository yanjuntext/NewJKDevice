package com.tutk.IOTC.listener

/**
 * @Author: wangyj
 * @CreateDate: 2023/5/13
 * @Description:
 */
interface OnAudioListener {
    fun onListenerStatus(status: Boolean)
    fun onTalkStatus(status: Boolean)
}