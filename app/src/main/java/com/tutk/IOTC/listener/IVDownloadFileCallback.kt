package com.tutk.IOTC.listener

import com.tutk.IOTC.camera.DownLoadFileStatus

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description:
 */
fun interface IVDownloadFileCallback {
    fun onAvChannelDownloadFileStatus(
        status: DownLoadFileStatus,
        total: Int,
        downloadTotal: Int,
        progress: Int
    )
}