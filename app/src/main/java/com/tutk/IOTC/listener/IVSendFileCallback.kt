package com.tutk.IOTC.listener

import com.tutk.IOTC.camera.SendFileStatus

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description: 文件发送
 */
fun interface IVSendFileCallback {
    /**
     * @param status 发送状态
     * @param total 文件总大小
     * @param progress 发送进度
     */
    fun onAvChannelSendFileStatus(status:SendFileStatus,total:Int,sendTotal:Int,progress:Int)
}