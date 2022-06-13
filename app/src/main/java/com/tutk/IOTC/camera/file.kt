package com.tutk.IOTC.camera

import android.content.Context
import android.net.Uri
import com.tutk.IOTC.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description: 文件下载、上传  文件目录
 */
/**
 * 设备端文件目录路径
 */
enum class FileDirectory(val file: String) {
    RECORD_LOG("/mnt/config/record/log.txt"),
    WIFI_DEBUG("/mnt/config/wifi_debug.cgi"),
    WIFI("/tmp/wifi.txt"),
    STATUS("/mnt/config/get_status.cgi"),
    SERVER("/mnt/config/server_info.cgi"),
    PROFILE("/etc/profile"),
    RECORD_VIDEO_DIRECTORY("/tmp/mnt/sdcard/video/mp4_list.info"),
}

private const val REC_MAX_BUF_SIZE = 1024
private const val SEND_MAX_BUF_SIZE = 2048
private const val DOWNLOAD_WAIT_TIME = 15000
private const val SEND_WAIT_TIME = 30000

/**发送文件状态*/
enum class SendFileStatus {
    RDSENDER_STATE_START,
    RDSENDER_STATE_STOP,
    RDSENDER_STATE_SENDING,
}

/**
 * 文件发送状态
 * @param status 状态
 * @param total 文件总大小
 * @param sendTotal 已发送大小
 * @param progress 发送进度 百分比
 */
internal data class SendFileInfo(
    val status: SendFileStatus,
    val total: Int,
    val sendTotal: Int,
    val progress: Int
)

/**文件发送*/
internal class SendFileJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {

    private val TAG = "SendFileJob"


    private val SESSION_MODE_LAN = 2

    private val recBuf = ByteArray(REC_MAX_BUF_SIZE)
    private val sendBuf = ByteArray(SEND_MAX_BUF_SIZE)

    private var mFis: InputStream? = null

    private var mContext: WeakReference<Context?>? = null

    private var filePath: String? = null
    private var fileUri: Uri? = null

    private var runJob: Job? = null

    private fun isActive() = runJob?.isActive == true

    fun start(file: String?, context: Context?) {

        if (isRunning) {
            d("SendFileJob is Running,not rerun")
            return
        }
        filePath = file
        mContext = WeakReference(context)
        if (filePath.isNullOrEmpty()) {
            d("SendFileJob is error because file is NULL")
            return
        }
        var fileSize = 0
        isRunning = true
        var nTotalSendNum = 0
        val mRecvRDTCommand = ByteArray(28)
        val mSendRDTCommand = ByteArray(28)
        runJob = GlobalScope.launch(Dispatchers.Main) {

            flow {

                while (isRunning && isActive()
                    && ((avChannel?.SID ?: -1) < 0
                            || (avChannel?.mAvIndex ?: -1) < 0
                            || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    delay(100)
                }

                val status = St_RDT_Status()
                var nRead = 0
                val RDT_ID = RDTAPIs.RDT_Create(avChannel?.SID ?: -1, SEND_WAIT_TIME, 3)

                if (RDT_ID >= 0) {
                    var runninttimes = 0
                    while (isRunning && isActive()) {
                        runninttimes++
                        if (runninttimes > 3) {
                            isRunning = false
                        }

                        if ((avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex ?: -1) >= 0) {
                            nRead = RDTAPIs.RDT_Read(
                                RDT_ID,
                                recBuf,
                                REC_MAX_BUF_SIZE,
                                SEND_WAIT_TIME
                            )
                            if (nRead > 0) {
                                System.arraycopy(recBuf, 0, mRecvRDTCommand, 0, 28)

                                var type = mRecvRDTCommand[0]

                                if (type != RDCTRLDEFs.RDT_COMMAND_FILE_NAME) {
                                    d("type != RDCTRLDEFs.RDT_COMMAND_FILE_NAME [$type]")
                                    break
                                }
                                val _filePath = filePath
                                if (_filePath.isNullOrEmpty()) {
                                    break
                                }
                                try {
                                    mFis = if (mContext?.get() != null) {
                                        mContext?.get()?.assets?.open(_filePath)
                                    } else {
                                        FileInputStream(_filePath)
                                    }
                                    val result = mFis?.use { fis ->
                                        fileSize = fis.available()

                                        val bFileSize = fileSize.toString().toByteArray()
                                        System.arraycopy(
                                            bFileSize,
                                            0,
                                            mSendRDTCommand,
                                            1,
                                            bFileSize.size
                                        )
                                        mSendRDTCommand[0] = RDCTRLDEFs.RDT_COMMAND_FILE_SIZE

                                        nRead = RDTAPIs.RDT_Write(RDT_ID, mSendRDTCommand, 28)

                                        if (nRead < 0) {
                                            return@use -1
                                        }
                                        nRead = RDTAPIs.RDT_Read(
                                            RDT_ID,
                                            recBuf,
                                            REC_MAX_BUF_SIZE,
                                            SEND_WAIT_TIME
                                        )

                                        if (nRead < 0) {
                                            return@use -1
                                        }

                                        System.arraycopy(recBuf, 0, mRecvRDTCommand, 0, 28)
                                        type = mRecvRDTCommand[0]

                                        if (type != RDCTRLDEFs.RDT_COMMAND_FILE_START) {
                                            return@use -1
                                        }
                                        emit(
                                            SendFileInfo(
                                                SendFileStatus.RDSENDER_STATE_START,
                                                fileSize,
                                                nTotalSendNum,
                                                ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                                            )
                                        )

                                        do {
                                            nRead = mFis?.read(sendBuf) ?: 0
                                            if (nRead < 0) {
                                                d("The file sent successfully!")
                                                break
                                            }

                                            nRead = RDTAPIs.RDT_Write(RDT_ID, sendBuf, nRead)
                                            if (nRead < 0) {
                                                break
                                            }
                                            if (RDTAPIs.RDT_Status_Check(
                                                    RDT_ID,
                                                    status
                                                ) == RDTAPIs.RDT_ER_NoERROR
                                            ) {
                                                nTotalSendNum += nRead
                                                emit(
                                                    SendFileInfo(
                                                        SendFileStatus.RDSENDER_STATE_SENDING,
                                                        fileSize,
                                                        nTotalSendNum,
                                                        ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                                                    )
                                                )
                                                if (status.BufSizeInSendQueue > 1024000) {
                                                    delay(1000)
                                                }
                                            } else {
                                                break
                                            }
                                        } while (isRunning && isActive())

                                        1
                                    }
                                    if (result != -1) {
                                        break
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (nRead == RDTAPIs.RDT_ER_TIMEOUT) {
                                d("IOTCRDTApis.RDT_ER_TIMEOUT")
                                break
                            } else if (nRead < 0) {
                                d("nRead < 0")
                                break
                            }

                        } else {
                            delay(32)
                        }
                    }
                }
                isRunning = false
                emit(
                    SendFileInfo(
                        SendFileStatus.RDSENDER_STATE_STOP,
                        fileSize,
                        nTotalSendNum,
                        ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                    )
                )
                RDTAPIs.RDT_Destroy(RDT_ID)
            }.flowOn(Dispatchers.IO)
                .collect {
                    iavChannelStatus?.onAVChannelSendFileStatus(
                        it.status,
                        it.total,
                        it.sendTotal,
                        it.progress
                    )
                }

        }
    }

    fun start(uri: Uri?, context: Context?) {
        if (isRunning) {
            d("SendFileJob is Running,not rerun")
            return
        }
        fileUri = uri
        mContext = WeakReference(context)
        if (fileUri == null) {
            d("SendFileJob is error because file is NULL")
            return
        }
        var fileSize = 0
        isRunning = true
        var nTotalSendNum = 0
        val mRecvRDTCommand = ByteArray(28)
        val mSendRDTCommand = ByteArray(28)
        runJob = GlobalScope.launch(Dispatchers.Main) {

            flow {

                while (isRunning && isActive()
                    && ((avChannel?.SID ?: -1) < 0
                            || (avChannel?.mAvIndex ?: -1) < 0
                            || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    delay(100)
                }

                val status = St_RDT_Status()
                var nRead = 0
                val RDT_ID = RDTAPIs.RDT_Create(avChannel?.SID ?: -1, SEND_WAIT_TIME, 3)

                if (RDT_ID >= 0) {
                    var runninttimes = 0
                    while (isRunning && isActive()) {
                        runninttimes++
                        if (runninttimes > 3) {
                            isRunning = false
                        }

                        if ((avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex ?: -1) >= 0) {
                            nRead = RDTAPIs.RDT_Read(
                                RDT_ID,
                                recBuf,
                                REC_MAX_BUF_SIZE,
                                SEND_WAIT_TIME
                            )
                            if (nRead > 0) {
                                System.arraycopy(recBuf, 0, mRecvRDTCommand, 0, 28)

                                var type = mRecvRDTCommand[0]

                                if (type != RDCTRLDEFs.RDT_COMMAND_FILE_NAME) {
                                    d("type != RDCTRLDEFs.RDT_COMMAND_FILE_NAME [$type]")
                                    break
                                }
                                val _filePath = fileUri ?: break
                                try {
                                    mFis = if (mContext?.get() != null) {
                                        mContext?.get()?.contentResolver?.openInputStream(_filePath)
                                    } else null
                                    val result = mFis?.use { fis ->
                                        fileSize = fis.available()

                                        val bFileSize = fileSize.toString().toByteArray()
                                        System.arraycopy(
                                            bFileSize,
                                            0,
                                            mSendRDTCommand,
                                            1,
                                            bFileSize.size
                                        )
                                        mSendRDTCommand[0] = RDCTRLDEFs.RDT_COMMAND_FILE_SIZE

                                        nRead = RDTAPIs.RDT_Write(RDT_ID, mSendRDTCommand, 28)

                                        if (nRead < 0) {
                                            return@use -1
                                        }
                                        nRead = RDTAPIs.RDT_Read(
                                            RDT_ID,
                                            recBuf,
                                            REC_MAX_BUF_SIZE,
                                            SEND_WAIT_TIME
                                        )

                                        if (nRead < 0) {
                                            return@use -1
                                        }

                                        System.arraycopy(recBuf, 0, mRecvRDTCommand, 0, 28)
                                        type = mRecvRDTCommand[0]

                                        if (type != RDCTRLDEFs.RDT_COMMAND_FILE_START) {
                                            return@use -1
                                        }
                                        emit(
                                            SendFileInfo(
                                                SendFileStatus.RDSENDER_STATE_START,
                                                fileSize,
                                                nTotalSendNum,
                                                ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                                            )
                                        )

                                        do {
                                            nRead = mFis?.read(sendBuf) ?: 0
                                            if (nRead < 0) {
                                                d("The file sent successfully!")
                                                break
                                            }

                                            nRead = RDTAPIs.RDT_Write(RDT_ID, sendBuf, nRead)
                                            if (nRead < 0) {
                                                break
                                            }
                                            if (RDTAPIs.RDT_Status_Check(
                                                    RDT_ID,
                                                    status
                                                ) == RDTAPIs.RDT_ER_NoERROR
                                            ) {
                                                nTotalSendNum += nRead
                                                emit(
                                                    SendFileInfo(
                                                        SendFileStatus.RDSENDER_STATE_SENDING,
                                                        fileSize,
                                                        nTotalSendNum,
                                                        ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                                                    )
                                                )
                                                if (status.BufSizeInSendQueue > 1024000) {
                                                    delay(1000)
                                                }
                                            } else {
                                                break
                                            }
                                        } while (isRunning && isActive())

                                        1
                                    }
                                    if (result != -1) {
                                        break
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (nRead == RDTAPIs.RDT_ER_TIMEOUT) {
                                d("IOTCRDTApis.RDT_ER_TIMEOUT")
                                break
                            } else if (nRead < 0) {
                                d("nRead < 0")
                                break
                            }

                        } else {
                            delay(32)
                        }
                    }
                }
                isRunning = false
                emit(
                    SendFileInfo(
                        SendFileStatus.RDSENDER_STATE_STOP,
                        fileSize,
                        nTotalSendNum,
                        ((nTotalSendNum * 1.0 / fileSize) * 100).toInt()
                    )
                )
                RDTAPIs.RDT_Destroy(RDT_ID)
            }.flowOn(Dispatchers.IO)
                .collect {
                    iavChannelStatus?.onAVChannelSendFileStatus(
                        it.status,
                        it.total,
                        it.sendTotal,
                        it.progress
                    )
                }

        }
    }

    fun stop() {
        isRunning = false
    }


    private fun d(msg: String) {
        Liotc.d(TAG, "$msg   uid[${avChannel?.uid}]")
    }

    private fun i(msg: String) {
        Liotc.i(TAG, "$msg   uid[${avChannel?.uid}]")
    }

    private fun e(msg: String) {
        Liotc.e(TAG, "$msg   uid[${avChannel?.uid}]")
    }
}

/**文件下载状态*/
enum class DownLoadFileStatus {
    DOWNLOAD_STATE_START,
    DOWNLOAD_STATE_CLOSED,
    DOWNLOAD_STATE_SENDING,
    DOWNLOAD_STATE_CANCEL,
    DOWNLOAD_STATE_FINISH,

    //已经有一个再下载了，不能同时开启多个下载
    DOWNLOAD_STATE_HAD_DOWNLOAD,
    DOWNLOAD_STATE_ERROR
}

internal data class DownLoadInfo(
    val status: DownLoadFileStatus,
    val total: Long,
    val downloadTotal: Long,
    val progress: Int
)

/**文件下载*/
internal class DownFileJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "DownFileJob"

    private var runJob: Job? = null

    private var srcFile: String? = null
    private var dstFile: String? = null
    private var dstUri: Uri? = null

    private var mContext: WeakReference<Context>? = null


    private val STRUCT_SIZE = 128
    private val RECVBUF_SIZE = 1024

    private fun isActive() = runJob?.isActive == true

    /**
     * @param srcFile 设备对应的待下载的文件
     * @param dstFile 下载好的文件保存的路径
     */
    fun start(srcFile: String?, dstFile: String?) {
        if (isRunning) {
            d("is Running,not rerun")
            iavChannelStatus?.onAVChanneldownloadFileStatus(
                DownLoadFileStatus.DOWNLOAD_STATE_HAD_DOWNLOAD,
                0,
                0,
                0
            )
            return
        }

        if (srcFile.isNullOrEmpty() || dstFile.isNullOrEmpty()) {
            d("srcFile is NULL or dstFile is NULL srcFile=[$srcFile],dstFile=[$dstFile]")
            iavChannelStatus?.onAVChanneldownloadFileStatus(
                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                0,
                0,
                0
            )
            return
        }
        isRunning = true
        this.dstFile = dstFile
        this.srcFile = srcFile
        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                val recvBuf = ByteArray(RECVBUF_SIZE)
                val recvRDTCommand = ByteArray(STRUCT_SIZE)
                val sendRDTCommand = ByteArray(STRUCT_SIZE)

                while (isRunning && isActive()
                    && ((avChannel?.SID ?: -1) < 0
                            || (avChannel?.mAvIndex ?: -1) < 0
                            || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    delay(100)
                }

                var rdt_id = -1
                var total = 0L
                var readTotal = 0L
                if (isRunning && isActive()) {
                    val stRdtStatus = St_RDT_Status()
                    val sid = avChannel?.SID ?: -1
                    d("sid[$sid],[${avChannel?.SID}],srcFile[$srcFile],dstFile[$dstFile]")

                    if (sid >= 0) {
                        rdt_id = RDTAPIs.RDT_Create(sid, DOWNLOAD_WAIT_TIME, 3)
                        d("RDT_ID=[$rdt_id]")

                        while (isRunning && isActive() && !srcFile.isNullOrEmpty() && !dstFile.isNullOrEmpty() && rdt_id >= 0) {

                            if ((avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex ?: -1) >= 0) {
                                val srcFileArray = srcFile.toByteArray()
                                var cmd = RDCTRLDEFs.RDT_COMMAND_FILE_NAME
                                sendRDTCommand[0] = cmd
                                val size =
                                    if (srcFileArray.size < STRUCT_SIZE) srcFileArray.size else STRUCT_SIZE
                                System.arraycopy(srcFileArray, 0, sendRDTCommand, 1, size)
                                var result = RDTAPIs.RDT_Write(rdt_id, sendRDTCommand, STRUCT_SIZE)
                                d("RDT_Write result[$result]")
                                if (result < 0 || !isRunning || !isActive()) {
                                    d("RDT_Write break [$isRunning],[$isActive]")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                result = RDTAPIs.RDT_Read(
                                    rdt_id,
                                    recvBuf,
                                    RECVBUF_SIZE,
                                    DOWNLOAD_WAIT_TIME
                                )
                                System.arraycopy(recvBuf, 0, recvRDTCommand, 0, STRUCT_SIZE)
                                d("RDT_Read result[$result]")
                                if (result < 0 || !isRunning || !isActive()) {
                                    d("RDT_Read break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                cmd = recvRDTCommand[0]
                                d("recvRDTCommand[0] cmd[$cmd]")
                                if (cmd != RDCTRLDEFs.RDT_COMMAND_FILE_SIZE) {
                                    d("recvRDTCommand[0] break")
                                    break
                                }

                                val sizeArray = ByteArray(8)
                                System.arraycopy(recvRDTCommand, 1, sizeArray, 0, 8)
                                d("total read str [${sizeArray.getString()}]")
                                d("total read str2Int [${(sizeArray.getString()).toInt()}]")
                                total = (sizeArray.getString()).toInt().toLong()
                                d("total read[$total]")

                                val startArray = RDCTRLDEFs.parseContent(
                                    RDCTRLDEFs.RDT_COMMAND_FILE_START,
                                    "Start".toByteArray()
                                )
                                result = RDTAPIs.RDT_Write(rdt_id, startArray, STRUCT_SIZE)
                                d("start result [$result]")

                                if (result < 0 || !isRunning || !isActive()) {
                                    d("start break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                if (RDTAPIs.RDT_Status_Check(
                                        rdt_id,
                                        stRdtStatus
                                    ) < 0 || !isRunning || !isActive()
                                ) {
                                    d("RDT_Status_Check break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                try {

                                    FileOutputStream(this@DownFileJob.dstFile).use { fos ->
                                        if (isActive()) {
                                            emit(
                                                DownLoadInfo(
                                                    DownLoadFileStatus.DOWNLOAD_STATE_START,
                                                    total,
                                                    0,
                                                    0
                                                )
                                            )
                                        }
                                        do {
                                            val laveSize = total - readTotal

                                            val readSize =
                                                if (laveSize > RECVBUF_SIZE) RECVBUF_SIZE else laveSize

                                            result = RDTAPIs.RDT_Read(
                                                rdt_id, recvBuf, readSize.toInt(),
                                                DOWNLOAD_WAIT_TIME
                                            )
                                            d("start write src result[$result]")
                                            if (result < 0 && result != RDTAPIs.RDT_ER_TIMEOUT) {
                                                break
                                            }

                                            readTotal += result

                                            fos.write(recvBuf, 0, result)
                                            if (isActive()) {
                                                emit(
                                                    DownLoadInfo(
                                                        DownLoadFileStatus.DOWNLOAD_STATE_SENDING,
                                                        total,
                                                        readTotal,
                                                        (readTotal * 1.0 / total * 100).toInt()
                                                    )
                                                )
                                            }

                                        } while (isRunning && isActive() && readTotal < total)
                                        if (isActive()) {
                                            if (isRunning) {
                                                emit(
                                                    DownLoadInfo(
                                                        DownLoadFileStatus.DOWNLOAD_STATE_FINISH,
                                                        total,
                                                        readTotal,
                                                        (readTotal * 1.0 / total * 100).toInt()
                                                    )
                                                )
                                            } else {
                                                emit(
                                                    DownLoadInfo(
                                                        DownLoadFileStatus.DOWNLOAD_STATE_CANCEL,
                                                        total,
                                                        readTotal,
                                                        (readTotal * 1.0 / total * 100).toInt()
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                val stopArray = RDCTRLDEFs.parseContent(
                                    RDCTRLDEFs.RDT_COMMAND_FILE_STOP,
                                    "Stop".toByteArray()
                                )
                                d("send stop")
                                RDTAPIs.RDT_Write(rdt_id, stopArray, STRUCT_SIZE)
                                break
                            } else {
                                isRunning = false
                                emit(
                                    DownLoadInfo(
                                        DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                        0,
                                        0,
                                        0
                                    )
                                )
                            }

                        }
                    } else {
                        emit(DownLoadInfo(DownLoadFileStatus.DOWNLOAD_STATE_ERROR, 0, 0, 0))
                    }
                } else {
                    if (isActive()) {
                        emit(
                            DownLoadInfo(
                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                total,
                                readTotal,
                                0
                            )
                        )
                    }
                }

                if (rdt_id >= 0) {
                    d("send destory")
                    RDTAPIs.RDT_Destroy(rdt_id)
                }

                if (isActive()) {
                    emit(
                        DownLoadInfo(
                            DownLoadFileStatus.DOWNLOAD_STATE_CLOSED,
                            total,
                            readTotal,
                            if (total > 0) ((readTotal * 1.0 / total * 100).toInt()) else 0
                        )
                    )
                }
                this@DownFileJob.srcFile = null
                this@DownFileJob.dstFile = null
            }.flowOn(Dispatchers.IO)
                .collect {
                    d("collect iavChannelStatus[${iavChannelStatus == null}]")
                    iavChannelStatus?.onAVChanneldownloadFileStatus(
                        it.status,
                        it.total.toInt(),
                        it.downloadTotal.toInt(),
                        it.progress
                    )
                }
        }

    }

    fun start(context: Context?, srcFile: String?, dstUri: Uri?) {
        if (isRunning) {
            d("is Running,not rerun")
            iavChannelStatus?.onAVChanneldownloadFileStatus(
                DownLoadFileStatus.DOWNLOAD_STATE_HAD_DOWNLOAD,
                0,
                0,
                0
            )
            return
        }
        if (srcFile.isNullOrEmpty() || dstUri == null || context == null) {
            d("srcFile is NULL or dstFile is NULL srcFile=[$srcFile],dstUri=[$dstUri]")
            iavChannelStatus?.onAVChanneldownloadFileStatus(
                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                0,
                0,
                0
            )
            return
        }
        this.dstUri = dstUri
        this.srcFile = srcFile
        mContext = WeakReference(context)
        isRunning = true
        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                val recvBuf = ByteArray(RECVBUF_SIZE)
                val recvRDTCommand = ByteArray(STRUCT_SIZE)
                val sendRDTCommand = ByteArray(STRUCT_SIZE)
                while (isRunning && isActive() && ((avChannel?.SID
                        ?: -1) < 0 || (avChannel?.mAvIndex ?: -1) < 0)
                ) {
                    delay(100)
                }
                var rdt_id = -1
                var total = 0L
                var readTotal = 0L
                if (isRunning && isActive()) {
                    val stRdtStatus = St_RDT_Status()
                    val sid = avChannel?.SID ?: -1
                    d("sid[$sid],[${avChannel?.SID}],srcFile[$srcFile],dstUri[$dstUri]")

                    if (sid >= 0) {
                        rdt_id = RDTAPIs.RDT_Create(sid, DOWNLOAD_WAIT_TIME, 3)
                        d("RDT_ID=[$rdt_id]")

                        while (isRunning && isActive() && !srcFile.isNullOrEmpty() && this@DownFileJob.dstUri != null && rdt_id >= 0) {

                            if ((avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex ?: -1) >= 0) {
                                val srcFileArray = srcFile.toByteArray()
                                var cmd = RDCTRLDEFs.RDT_COMMAND_FILE_NAME
                                sendRDTCommand[0] = cmd
                                val size =
                                    if (srcFileArray.size < STRUCT_SIZE) srcFileArray.size else STRUCT_SIZE
                                System.arraycopy(srcFileArray, 0, sendRDTCommand, 1, size)
                                var result = RDTAPIs.RDT_Write(rdt_id, sendRDTCommand, STRUCT_SIZE)
                                d("RDT_Write result[$result]")
                                if (result < 0 || !isRunning || !isActive()) {
                                    d("RDT_Write break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                result = RDTAPIs.RDT_Read(
                                    rdt_id,
                                    recvBuf,
                                    RECVBUF_SIZE,
                                    DOWNLOAD_WAIT_TIME
                                )

                                System.arraycopy(recvBuf, 0, recvRDTCommand, 0, STRUCT_SIZE)
                                d("RDT_Read result[$result]")
                                if (result < 0 || !isRunning || !isActive()) {
                                    d("RDT_Read break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                cmd = recvRDTCommand[0]
                                d("recvRDTCommand[0] cmd[$cmd]")
                                if (cmd != RDCTRLDEFs.RDT_COMMAND_FILE_SIZE) {
                                    d("recvRDTCommand[0] break")
                                    break
                                }

                                val sizeArray = ByteArray(8)
                                System.arraycopy(recvRDTCommand, 1, sizeArray, 0, 8)
                                total = (sizeArray.getString()).toLong()
                                d("total read[$total]")

                                val startArray = RDCTRLDEFs.parseContent(
                                    RDCTRLDEFs.RDT_COMMAND_FILE_START,
                                    "Start".toByteArray()
                                )
                                result = RDTAPIs.RDT_Write(rdt_id, startArray, STRUCT_SIZE)
                                d("start result [$result]")

                                if (result < 0 || !isRunning || !isActive()) {
                                    d("start break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                if (RDTAPIs.RDT_Status_Check(
                                        rdt_id,
                                        stRdtStatus
                                    ) < 0 || !isRunning || !isActive()
                                ) {
                                    d("RDT_Status_Check break")
                                    if (isActive()) {
                                        emit(
                                            DownLoadInfo(
                                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                                0,
                                                0,
                                                0
                                            )
                                        )
                                    }
                                    break
                                }

                                try {
                                    this@DownFileJob.dstUri?.let { uri ->
                                        mContext?.get()?.contentResolver?.openOutputStream(uri)
                                            ?.use { fos ->
                                                if (isActive()) {
                                                    emit(
                                                        DownLoadInfo(
                                                            DownLoadFileStatus.DOWNLOAD_STATE_START,
                                                            total,
                                                            0,
                                                            0
                                                        )
                                                    )
                                                }
                                                do {
                                                    val laveSize = total - readTotal

                                                    val readSize =
                                                        if (laveSize > RECVBUF_SIZE) RECVBUF_SIZE else laveSize

                                                    result = RDTAPIs.RDT_Read(
                                                        rdt_id, recvBuf, readSize.toInt(),
                                                        DOWNLOAD_WAIT_TIME
                                                    )
                                                    d("start write src result[$result]")
                                                    if (result < 0 && result != RDTAPIs.RDT_ER_TIMEOUT) {
                                                        break
                                                    }

                                                    readTotal += result

                                                    fos.write(recvBuf, 0, result)
                                                    if (isActive()) {
                                                        emit(
                                                            DownLoadInfo(
                                                                DownLoadFileStatus.DOWNLOAD_STATE_SENDING,
                                                                total,
                                                                readTotal,
                                                                (readTotal * 1.0 / total * 100).toInt()
                                                            )
                                                        )
                                                    }

                                                } while (isRunning && isActive() && readTotal < total)
                                                if (isActive()) {
                                                    if (isRunning) {
                                                        emit(
                                                            DownLoadInfo(
                                                                DownLoadFileStatus.DOWNLOAD_STATE_FINISH,
                                                                total,
                                                                readTotal,
                                                                (readTotal * 1.0 / total * 100).toInt()
                                                            )
                                                        )
                                                    } else {
                                                        emit(
                                                            DownLoadInfo(
                                                                DownLoadFileStatus.DOWNLOAD_STATE_CANCEL,
                                                                total,
                                                                readTotal,
                                                                (readTotal * 1.0 / total * 100).toInt()
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            } else {
                                isRunning = false
                                emit(
                                    DownLoadInfo(
                                        DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                        0,
                                        0,
                                        0
                                    )
                                )
                            }

                        }
                    } else {
                        emit(DownLoadInfo(DownLoadFileStatus.DOWNLOAD_STATE_ERROR, 0, 0, 0))
                    }
                } else {
                    if (isActive()) {
                        emit(
                            DownLoadInfo(
                                DownLoadFileStatus.DOWNLOAD_STATE_ERROR,
                                total,
                                readTotal,
                                0
                            )
                        )
                    }
                }

                if (rdt_id >= 0) {
                    d("RDT_Destroy [$rdt_id]")
                    RDTAPIs.RDT_Destroy(rdt_id)
                }

                if (isActive()) {
                    emit(
                        DownLoadInfo(
                            DownLoadFileStatus.DOWNLOAD_STATE_CLOSED,
                            total,
                            readTotal,
                            if (total > 0) ((readTotal * 1.0 / total * 100).toInt()) else 0
                        )
                    )
                }
                this@DownFileJob.srcFile = null
                this@DownFileJob.dstUri = null

            }.flowOn(Dispatchers.IO)
                .collect {

                    iavChannelStatus?.onAVChanneldownloadFileStatus(
                        it.status,
                        it.total.toInt(),
                        it.downloadTotal.toInt(),
                        it.progress
                    )
                }
        }
    }


    fun stop() {
        isRunning = false
    }

    private fun d(msg: String) {
        Liotc.d(TAG, "$msg   uid[${avChannel?.uid}]")
    }

    private fun i(msg: String) {
        Liotc.i(TAG, "$msg   uid[${avChannel?.uid}]")
    }

    private fun e(msg: String) {
        Liotc.e(TAG, "$msg   uid[${avChannel?.uid}]")
    }
}

fun ByteArray.getString(): String {
    val sb = StringBuilder()
    run outSide@{
        forEach {
            if (it.toInt() == 0) {
                return@outSide
            }
            sb.append(it.toInt().toChar())
        }
    }
    return sb.toString() ?: ""
}

fun ByteArray.getUtfString(): String {
    val sb = StringBuilder()
    var size = 0
    run outside@{
        this.forEachIndexed { index, byte ->
            if (byte.toInt() == 0x0) {
                size = index
                return@outside
            }
            sb.append(byte.toInt().toChar())
        }
    }
    if (size == 0) {
        return String(this)
    }
    return try {
        val value = String(this, 0, size, Charsets.UTF_8)
        value.nonValidXMLCharacters()
    } catch (e: Exception) {
        e.printStackTrace()
        sb.toString()
    }
}

fun ByteArray.getUtfString2(): String {
    val sb = StringBuilder()
    var size = 0
    run outside@{
        this.forEachIndexed { index, byte ->
            if (byte.toInt() == 0x0) {
                size = index
                return@outside
            }
            sb.append(byte.toInt().toChar())
        }
    }
    if (size == 0) {
        return ""
    }
    return try {
        val value = String(this, 0, size, Charsets.UTF_8)
        value.nonValidXMLCharacters()
    } catch (e: Exception) {
        e.printStackTrace()
        sb.toString()
    }
}

/**去除无效字符串*/
fun String?.nonValidXMLCharacters(): String {
    if (this.isNullOrEmpty()) return ""
    val sb = StringBuilder()
    this.forEach { current ->
        // here; it should not happen.
        if (current.toInt() == 0x9 || current.toInt() == 0xA || current.toInt() == 0xD
            || current.toInt() in 0x20..0xD7FF
            || current.toInt() in 0xE000..0xFFFD
            || current.toInt() in 0x10000..0x10FFFF
        ) {
            sb.append(current)
        }
    }
    return sb.toString()
}
