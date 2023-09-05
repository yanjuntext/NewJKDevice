package com.tutk.IOTC.listener

import com.tutk.IOTC.status.RecordStatus

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/17
 * @Description:
 */
interface IAVChannelRecordStatus {
    fun onAVChannelRecordStatus(status: RecordStatus, file: String?, time: Int)
}