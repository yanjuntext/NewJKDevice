package com.tutk.IOTC.camera

import android.os.Bundle
import com.tutk.IOTC.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description:IO 命令
 */

class RecvInfo(val type: Int, val data: ByteArray?)


/**连接设备*/
class StartJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "StartJob"
    private var runJob: Job? = null

    private var mSID = -1

    private var isStoped = false

    fun setSid(sid: Int) {
        mSID = sid
    }

    fun start() {
        if (runJob?.isActive == true) {
            d(TAG, "StartJob is Running,not rerun")
            return
        }
        isRunning = true
        isStoped = false
        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                while (runJob?.isActive == true) {
                    var connectingIndex = 0
                    var offlineIndex = 0
                    while (isRunning && runJob?.isActive == true) {
                        if (mSID == IOTC_CONNECT_ING) {
                            connectingIndex++
                            d(TAG, "StartJob mSID[$mSID],connectingIndex = [${connectingIndex}]")
                            if (connectingIndex > 50) {
                                emit(Camera.CONNECTION_STATE_CONNECT_FAILED)
//                                break
                                connectingIndex = 0
                            }
                            delay(1000L)
                            continue
                        }

                        if (mSID < 0) {
                            d(TAG, "StartJob mSID[$mSID],[${offlineIndex}]")
                            offlineIndex++
                            if (offlineIndex > 20) {
                                emit(Camera.CONNECTION_STATE_CONNECT_FAILED)
//                                break
                                offlineIndex = 0
                            }
                            delay(1000L)
                            continue
                        }

                        val nServType = intArrayOf(-1)
                        val mReSend = IntArray(1)

                        val result = avChannel?.let { avChannel ->
                            val avIndex = AVAPIs.avClientStart2(
                                mSID,
                                avChannel.mViewAcc,
                                avChannel.mViewPwd,
                                30,
                                nServType,
                                avChannel.mChannel,
                                mReSend
                            )
                            val servType = nServType[0].toLong()

                            d(TAG, "avIndex=[$avIndex],servType=[$servType]")

                            if (avIndex == AVAPIs.AV_ER_NOT_INITIALIZED) {
                                d(TAG, "avIndex==-20019,AV module has not been initialized")
                                return@let -1
                            }
                            if (avIndex >= 0) {
                                avChannel.mAvIndex = avIndex
                                avChannel.mServiceType = servType
                                emit(Camera.CONNECTION_STATE_CONNECTED)


                                if ((!(isSupport(servType, 262144)
                                            && isSupport(servType, 1048576)
                                            && isSupport(servType, 2097152)
                                            && isSupport(servType, 4194304))
                                            ) && Camera.IS_CHECK
                                ) {
                                    //设备类型不支持
                                    emit(Camera.CONNECTION_STATE_CLIENT_NOSUPPORT)
                                    isStoped = true
                                    return@let -1
                                }
                                return@let -1
                            } else if (avIndex == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT
                                || avIndex == AVAPIs.AV_ER_TIMEOUT
                            ) {
                                emit(Camera.CONNECTION_STATE_TIMEOUT)
                            } else if (avIndex == AVAPIs.AV_ER_WRONG_VIEWACCorPWD) {
                                emit(Camera.CONNECTION_STATE_WRONG_PASSWORD)
                                return@let -1
                            } else {
                                return@let -1
                            }
                            1
                        }
                        if (result == -1) {
                            break
                        }
                    }
                    isRunning = false

                    if (isStoped) {
                        d(TAG, "avClientExit mSID=[$mSID],channel[${avChannel?.mChannel}]")
                        avChannel?.let { avChannel ->
                            if (mSID >= 0) {
                                AVAPIs.avClientExit(mSID, avChannel.mChannel)
                            }
                        }
                        break
                    }
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    iavChannelStatus?.onAVChannelStatus(avChannel?.mChannel ?: 0, it)
                }
        }
    }

    fun stop() {
        isRunning = false
        isStoped = true
    }


    private fun isSupport(servType: Long, value: Long) = servType and value == value

    private fun d(tag: String, msg: String) {
        Liotc.d(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun i(tag: String, msg: String) {
        Liotc.i(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun e(tag: String, msg: String) {
        Liotc.e(tag, "$msg   uid[${avChannel?.uid}]")
    }

}

/**接收数据线程*/
class RecvIOJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "RecvJob"
    private var runJob: Job? = null
    private var mSID = -1

    fun setSid(sid: Int) {
        mSID = sid
    }

    private fun isActive() = runJob?.isActive == true

    fun start() {
        if (runJob?.isActive == true) {
            e(TAG, "runJob is Running ")
            return
        }

        isRunning = true

        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                while (isRunning && isActive()
                    && ((avChannel?.SID ?: -1) < 0
                            || (avChannel?.mAvIndex ?: -1) < 0
                            || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    delay(1000L)
                }

//                while (isRunning && (mSID < 0 || (avChannel?.mAvIndex
//                        ?: -1) < 0) && runJob?.isActive == true
//                ) {
//                    delay(1000L)
//                }
                while (isRunning && runJob?.isActive == true) {
                    avChannel?.let { avChannel ->
                        if (mSID >= 0 && (avChannel.mAvIndex >= 0)) {
                            val ioCtrlType = IntArray(1)
                            val ioCtrlBuf = ByteArray(1024)

                            val nRet = AVAPIs.avRecvIOCtrl(
                                avChannel.mAvIndex,
                                ioCtrlType,
                                ioCtrlBuf,
                                ioCtrlBuf.size,
                                0
                            )
                            d(TAG, "AVAPIs.avRecvIOCtrl nRet=[$nRet]")
                            if (nRet >= 0) {
                                val data = ByteArray(nRet)
                                System.arraycopy(ioCtrlBuf, 0, data, 0, nRet)
                                d(
                                    TAG,
                                    "tutkio AVAPIs.avRecvIOCtrl  avio recv size=[$nRet],onAVChannelRecv emit -----avRecvIOCtrl(${avChannel.mChannel},0x${ioCtrlType[0].toHexString()},${data.getHex()})"
                                )

                                if (ioCtrlType[0] == AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_RESP) {
                                    //音频类型
                                    val channel = Packet.byteArrayToInt_Little(data, 0)
                                    val format = Packet.byteArrayToInt_Little(data, 4)
                                    d(TAG, "channel[$channel],audioCodec[$format]")
                                    if (avChannel.mChannel == channel) {
                                        avChannel.mAudioCodec = format
                                    }
                                } else if (ioCtrlType[0] == AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_RESP) {
                                    d(TAG, "IOTYPE_USER_IPCAM_GETSTREAMCTRL_RESP")
                                }
                                d("Monitor","onAVChannelRecv emit -----")
//                                emit(RecvInfo(ioCtrlType[0], data))
//                                iavChannelStatus?.onAVChannelRecv(avChannel.mChannel, ioCtrlType[0], data)
                                emit(RecvInfo(ioCtrlType[0],data))
//                                emit(Bundle().apply {
//                                    putInt("type",ioCtrlType[0])
//                                    putByteArray("data",data)
//                                })
                                d("Monitor","onAVChannelRecv emit +++++")
                            } else {
                                delay(100)
                            }
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)
                .catch {
                    d("Monitor","onAVChannelRecv emit ----- error=${it.message}")
                }
                .collect {
                    d("Monitor","onAVChannelRecv emit iavChannelStatus=${iavChannelStatus == null}  -${it is RecvInfo}")
                    if(it is RecvInfo){
                        iavChannelStatus?.onAVChannelRecv(avChannel?.mChannel ?: -1, it.type, it.data)
                    }
                }
        }
    }

    fun stop() {
        isRunning = false
        runJob?.cancel()
        runJob = null
    }


    private fun d(tag: String, msg: String) {
        Liotc.d(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun i(tag: String, msg: String) {
        Liotc.i(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun e(tag: String, msg: String) {
        Liotc.e(tag, "$msg   uid[${avChannel?.uid}]")
    }
}

/**发送数据线程*/
class SendIOJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "SendIOJob"
    private var runJob: Job? = null

    private var mSID = -1

    private var isStoped = false

    fun setSid(sid: Int) {
        mSID = sid
    }

    fun start() {
        if (runJob?.isActive == true) {
            d(TAG, "SendIOJob is Running,not rerun")
            return
        }
        isRunning = true
        isStoped = false
        runJob = GlobalScope.launch(Dispatchers.IO) {
            while (runJob?.isActive == true) {
                while (isRunning && runJob?.isActive == true
                    && (mSID < 0 || (avChannel?.mAvIndex ?: -1) < 0)
                ) {
                    delay(1000L)
                }

                if (isRunning && runJob?.isActive == true && mSID >= 0 && (avChannel?.mAvIndex
                        ?: -1) >= 0
                ) {
                    val avIndex = (avChannel?.mAvIndex ?: -1)
                    d(TAG, "avSendIOCtrl avIndex[$avIndex]")
                    AVAPIs.avSendIOCtrl(
                        avIndex,
                        AVAPIs.IOTYPE_INNER_SND_DATA_DELAY,
                        Packet.intToByteArray_Little(0),
                        4
                    )

                }

                while (isRunning && runJob?.isActive == true) {
                    val avIndex = avChannel?.mAvIndex ?: -1
                    if (mSID >= 0 && avIndex >= 0 && (avChannel?.IOCtrlQueue?.isEmpty() == false
                                || avChannel?.IOCtrlFastQueue?.isEmpty() == false)
                    ) {
                        val commonSize = avChannel?.IOCtrlQueue?.getQueueSize() ?: 0
                        val fastSize = avChannel?.IOCtrlFastQueue?.getQueueSize() ?: 0

                        if (commonSize > 0) {
                            avChannel?.IOCtrlQueue?.Dequeue()?.let { data ->
                                if (isRunning && runJob?.isActive == true) {
                                    val ret = AVAPIs.avSendIOCtrl(
                                        avIndex,
                                        data.IOCtrlType,
                                        data.IOCtrlBuf,
                                        data.IOCtrlBuf?.size ?: 0
                                    )
                                    if (ret >= 0) {
                                        d(
                                            TAG,
                                            "tutkio avSendIOCtrl avio send (${avIndex},0x${data.IOCtrlType.toHexString()},${data.IOCtrlBuf?.getHex()})"
                                        )
                                    } else {
                                        d(
                                            TAG,
                                            "tutkio avSendIOCtrl failed : [$ret],0x${data.IOCtrlType.toHexString()}"
                                        )
                                    }
                                }
                            }
                        } else if (fastSize > 0) {
                            avChannel?.IOCtrlFastQueue?.Dequeue()?.let { data ->
                                if (isRunning && runJob?.isActive == true) {
                                    val ret = AVAPIs.avSendIOCtrl(
                                        avIndex,
                                        data.IOCtrlType,
                                        data.IOCtrlBuf,
                                        data.IOCtrlBuf?.size ?: 0
                                    )
                                    if (ret >= 0) {
                                        d(
                                            TAG,
                                            "tutkio avSendIOCtrl fastIO  avio send (${avIndex},0x${data.IOCtrlType.toHexString()},${data.IOCtrlBuf?.getHex()})"
                                        )
                                    } else {
                                        d(
                                            TAG,
                                            "tutkio avSendIOCtrl fastIO failed : [$ret],0x${data.IOCtrlType.toHexString()}"
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        delay(20L)
                    }
                }

                if (isStoped) {
                    d(TAG, "avSendIOCtrlExit avIndex[${avChannel?.mAvIndex}]")
                    avChannel?.let { avChannel ->
                        AVAPIs.avSendIOCtrlExit(avChannel.mAvIndex)
                    }
                    break
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        isStoped = true
    }


    private fun d(tag: String, msg: String) {
        Liotc.d(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun i(tag: String, msg: String) {
        Liotc.i(tag, "$msg   uid[${avChannel?.uid}]")
    }

    private fun e(tag: String, msg: String) {
        Liotc.e(tag, "$msg   uid[${avChannel?.uid}]")
    }
}