package com.tutk.IOTC.listener

import android.net.Uri
import com.tutk.IOTC.camera.DownLoadFileStatus

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description:
 */

interface IVDownloadFileCallback {
    /**
     * @param status 下载状态
     * @param total 文件总大小
     * @param downloadTotal 已下载大小
     * @param progress 下载进度
     */
    @Deprecated("please user with disFilePath and disUri method")
    fun onAvChannelDownloadFileStatus(
        status: DownLoadFileStatus,
        total: Int,
        downloadTotal: Int,
        progress: Int
    )

    fun onAvChannelDownloadFileStatus(
        status: DownLoadFileStatus,
        total: Int,
        downloadTotal: Int,
        progress: Int,
        dstFilePath: String?,
        dstUri: Uri?
    )
}