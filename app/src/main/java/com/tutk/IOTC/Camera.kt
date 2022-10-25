package com.tutk.IOTC

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tutk.IOTC.camera.*
import com.tutk.IOTC.listener.*
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.RecordStatus
import com.tutk.IOTC.status.VoiceType
import com.tutk.bean.TSupportStream
import com.tutk.bean.TTimeZone
import com.tutk.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
const val IOTC_CONNECT_ING = 9999

open class Camera(val uid: String, var psw: String, var viewAccount: String = "admin") :
    IAVChannelListener {

    private val TAG = "IOTCamera"

    companion object {

        private var LICENSE_KEY =
            "AQAAAJcKNzG79jf0NVVLoZosyh0i7HZI145TDmu1i1i3wPJpFTzRiwx9TjiFmZqrFKW2X3brFrwHlnlFfxMJUdsAN5icYfMtNTRIGWOCHbf4E8src24NXxEz2r4Ti6tL4Kp2T4fzfWc5uLuqr0UG6ufEDWchSHBJMA+viECzPXMzAzH/al2k4sCDEVbd7wKHOF/3tQIXPuQA6ac6ZA=="

        const val AUDIORECORD_PLAYBACKMODE = 1
        const val AUDIORECORD_LIVEMODE = 2

        var PORTRAIT_ORIENTATION = 1
        var LANDS_ORIENTATION = 2

        private var mCameraCount = 0
        private var mAtoCameraCount = AtomicInteger(0)
        private var mDefaultMaxCameraLimit = 8

        const val DEFAULT_AV_CHANNEL = 0
        const val DEFAULT_FRAMECOUNT = 30
        const val CONNECTION_STATE_NONE = 0
        const val CONNECTION_STATE_CONNECTING = 1
        const val CONNECTION_STATE_CONNECTED = 2
        const val CONNECTION_STATE_DISCONNECTED = 3
        const val CONNECTION_STATE_UNKNOWN_DEVICE = 4
        const val CONNECTION_STATE_WRONG_PASSWORD = 5
        const val CONNECTION_STATE_TIMEOUT = 6
        const val CONNECTION_STATE_UNSUPPORTED = 7
        const val CONNECTION_STATE_CONNECT_FAILED = 8
        const val CONNECTION_STATE_CLIENT_NOSUPPORT = 9
        const val CONNECTION_STATE_DEV_SLEEP = 10

        const val EXTRA_EVENT_FRAME_INFO = 11
        const val EXTRA_EVENT_AUDIO_SUPPORT = 13
        const val EXTRA_EVENT_RDSENDER = 14
        const val EXTRA_EVENT_LOCAL_RECORD = 15
        const val EXTRA_EVENT_HAD_CLEAN_VIDEOBUF = 16

        //开始本地录像
        const val EXTRA_EVENT_LOCAL_START_RECORD = 17

        //关闭本地录像
        const val EXTRA_EVENT_LOCAL_STOP_RECORD = 18


        const val EXTRA_EVENT_LOCAL_RECORD_READY = 1


        const val CONNECTION_STATE_CLIENT_S_DEV_ISSLEEP = 0x9500
        const val CONNECTION_STATE_CLIENT_S_ROUSEING = 0x9501
        const val CONNECTION_STATE_CLIENT_S_WAKEUP_RUNNING = 0x9502
        const val CONNECTION_STATE_CLIENT_S_DEV_ISWILLSLEEP_CANOT_ROUSE = 0x9503
        const val CONNECTION_STATE_CLIENT_S_DEV_ISSLEEP_CANOT_ROUSE = 0x9504
        const val CONNECTION_STATE_CLIENT_S_DEV_OTHER_STATUS = 0x9505
        const val CONNECTION_STATE_CLIENT_S_DEV_OFFLINE = 0x9506

        const val WAKEUP_RESULT_CLIENT_S_DEV_ISSLEEP = 0x9600
        const val WAKEUP_RESULT_CLIENT_S_ROUSEING = 0x9601
        const val WAKEUP_RESULT_CLIENT_S_WAKEUP_RUNNING = 0x9602
        const val WAKEUP_RESULT_CLIENT_S_DEV_ISWILLSLEEP_CANOT_ROUSE = 0x9603
        const val WAKEUP_RESULT_CLIENT_S_DEV_ISSLEEP_CANOT_ROUSE = 0x9604
        const val WAKEUP_RESULT_CLIENT_S_DEV_OTHER_STATUS = 0x9605
        const val WAKEUP_RESULT_CLIENT_S_DEV_OFFLINE = 0x9606
        const val WAKEUP_RESULT_CLIENT_S_WAKEUP_EXIT = 0x9607
        const val WAKEUP_RESULT_CLIENT_S_WAKEUP_20TIMES_UPDATESTATUS = 0x9608
        var WAKEUP_CONNECT_MODE = 3

        const val RECONNECT_TIMES = 3


        const val MAX_CONNECTED_TIMEOUT_OWL = 180 //3min = 180s

        internal var IS_CHECK = true

        /**
         * tutk 库的key
         * 如果不使用默认的key,请在@link{}之前调用
         */
        fun setTutkKey(key: String) {
            LICENSE_KEY = key
        }

        /**
         * 初始计
         */
        @Synchronized
        fun init(): Int {
            var ret = 0
            val isInited = mAtoCameraCount.get()
            Liotc.i("Camera", "init cameraCount=[$mCameraCount],isInited=[$isInited]")
            if (isInited == 0) {
                val port: Int = (10000 + System.currentTimeMillis() % 10000).toInt()
                val ley_ret = TUTKGlobalAPIs.TUTK_SDK_Set_License_Key(LICENSE_KEY)
                Liotc.i("Camera", "init ley_ret=[$ley_ret]")

                IOTCAPIs.IOTC_Setup_Session_Alive_Timeout(10)
                ret = IOTCAPIs.IOTC_Initialize2(port)
                IOTCAPIs.IOTC_WakeUp_Setup_Auto_WakeUp(1)

                Liotc.i("Camera", "init IOTC_Initialize2() returns=[$ret]")

                if (ret < 0) {
                    return ret
                }
                ret = RDTAPIs.RDT_Initialize()
                Liotc.i("Camera", "init RDT_Initialize() returns=[$ret]")

                ret = AVAPIs.avInitialize(mDefaultMaxCameraLimit * 16)

                if (ret < 0) {
                    return ret
                }
                mAtoCameraCount.set(1)
            }
            return ret
        }

        /**
         * 释放资源
         */
        @Synchronized
        fun uninit(): Int {
            var ret = 0
            val isInit = mAtoCameraCount.get()
            Liotc.i("Camera", "uninit isInit=[$isInit]")
            if (isInit == 1) {
                //说明初始化成功过
                RDTAPIs.RDT_DeInitialize()

                ret = AVAPIs.avDeInitialize()
                Liotc.i("Camera", "uninit AVAPIs.avDeInitialize() resule=[$ret]")

                ret = IOTCAPIs.IOTC_DeInitialize()
                Liotc.i("Camera", "uninit IOTCAPIs.IOTC_DeInitialize() resule=[$ret]")
                mAtoCameraCount.set(0)
            }
            return ret
        }

        private val mLanSearchLock = ReentrantLock()

        /**
         * 局域网搜索UID
         * @Description:进行局域网搜索设备时，请先进行初始化，调用 {@link #init()}
         * @param time 搜索时间 毫秒
         * */
        fun SearchLAN(time: Int = 2500): Array<st_LanSearchInfo?>? {
            if (mAtoCameraCount.get() == 0) {
                //未进行初始化
                throw RuntimeException("Camera no init,please init camera")
            }
            mLanSearchLock.lock()
            val num = IntArray(1)
            var result: Array<st_LanSearchInfo?>? = null
            result = IOTCAPIs.IOTC_Lan_Search(num, time)
            mLanSearchLock.unlock()
            return result
        }

        /**
         * Camera 最大同时运行个数
         */
        fun setMaxCameraLimit(limit: Int) {
            mDefaultMaxCameraLimit = limit
        }

        internal fun setCheck(check:Boolean){
            IS_CHECK = check
        }
    }

    private val mAVChannels = mutableListOf<AVChannel>()

    //通道监听
    private val mOnSessionChannelCallbacks: MutableList<OnSessionChannelCallback> = mutableListOf()

    //IO命令监听
    private val mOnIOCallbacks: MutableList<OnIOCallback> = mutableListOf()

    //帧命令监听
    private val mOnFrameCallbacks: MutableList<OnFrameCallback> = mutableListOf()

    //其他命令监听
    private val mOnExtraCallbacks: MutableList<OnExtraCallback> = mutableListOf()

    //硬解码接口
    private var mIVOTCListener: IVRegisterIOTCListener? = null

    private var mIAVChannelRecordStatus: MutableList<IAVChannelRecordStatus> = mutableListOf()

    //IOTCAPIs.IOTC_Get_SessionID()  session id
    private var nGSID = -1
    private var mSID = -1
    private var mSessionMode = -1

    private var mSendFileContext: WeakReference<Context?>? = null
    private var mSendFile: String? = null
    private var mSendFileUri: Uri? = null
    private var mAvSendFileChannel: Int? = null

    private var mDownloadFileContext: WeakReference<Context?>? = null
    private var mDownloadDstFile: String? = null
    private var mDownloadDstUri: Uri? = null
    private var mDownloadSrcFile: String? = null
    private var mAvDownloadFileChannel: Int? = null


    private var mSendFileStatusCallbacks: MutableList<IVSendFileCallback?> = mutableListOf()

    private var mDownloadFileStatusCallbacks: MutableList<IVDownloadFileCallback?> = mutableListOf()

    private val mSupportStreamList = mutableListOf<TSupportStream>()
    private var mTTimeZone: TTimeZone? = null

    /**
     * 重连间隔  ms
     * if[mReconnectTime] is 0,means not to reconnect automatically
     */
    private var mReconnectTime = 10000L

    fun setReconnectTime(reconnectTime:Long){
        mReconnectTime = reconnectTime
    }

    /*----------------------------------通道命令------------------------------------------*/
    fun registerSessionChannelCallback(callback: OnSessionChannelCallback?) {
        callback ?: return
        val iterator = mOnSessionChannelCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mOnSessionChannelCallbacks.add(callback)
    }

    fun unregisterSessionChannelCallback(callback: OnSessionChannelCallback?) {
        callback ?: return
        mOnSessionChannelCallbacks.remove(callback)
    }

    fun clearSessionChannelCallback() {
        mOnSessionChannelCallbacks.clear()
    }

    fun getSessionMode():Int{
        return mSessionMode
    }

    /*----------------------------------IO命令------------------------------------------*/
    fun registerIOCallback(callback: OnIOCallback?) {
        callback ?: return
        val iterator = mOnIOCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mOnIOCallbacks.add(callback)
    }

    fun unregisterIOCallback(callback: OnIOCallback?) {
        callback ?: return
        mOnIOCallbacks.remove(callback)
    }

    fun clearIOCallback() {
        mOnIOCallbacks.clear()
    }

    /*----------------------------------帧命令------------------------------------------*/
    fun registerFrameCallback(callback: OnFrameCallback?) {
        callback ?: return
        val iterator = mOnFrameCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mOnFrameCallbacks.add(callback)
    }

    fun unregisterFrameCallback(callback: OnFrameCallback?) {
        callback ?: return
        mOnFrameCallbacks.remove(callback)
    }

    fun clearFrameCallback() {
        mOnFrameCallbacks.clear()
    }

    /*----------------------------------其他命令------------------------------------------*/
    fun registerExtraCallback(callback: OnExtraCallback?) {
        callback ?: return
        val iterator = mOnExtraCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mOnExtraCallbacks.add(callback)
    }

    fun unregisterExtraCallback(callback: OnExtraCallback?) {
        callback ?: return
        mOnExtraCallbacks.remove(callback)
    }

    fun clearExtraCallback() {
        mOnExtraCallbacks.clear()
    }


    fun registerIVOTCListener(listener: IVRegisterIOTCListener?) {
        mIVOTCListener = listener
    }

    fun registerAVChannelRecordStatus(listener: IAVChannelRecordStatus?) {
        listener ?: return
        val iterator = mIAVChannelRecordStatus.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == listener) {
                return
            }
        }
        mIAVChannelRecordStatus.add(listener)
    }

    fun unRegisterAVChannelRecordStatus(listener: IAVChannelRecordStatus?) {
        listener ?: return
        mIAVChannelRecordStatus.remove(listener)
    }

    fun registerIVSendFileCallback(callback: IVSendFileCallback?) {
        callback ?: return
        val iterator = mSendFileStatusCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mSendFileStatusCallbacks.add(callback)
    }

    fun unRegisterIVSendFileCallback(callback: IVSendFileCallback?) {
        mSendFileStatusCallbacks.remove(callback)
    }

    fun registerIVDownloadFileCallback(callback: IVDownloadFileCallback?) {
        callback ?: return
        val iterator = mDownloadFileStatusCallbacks.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == callback) {
                return
            }
        }
        mDownloadFileStatusCallbacks.add(callback)
    }

    fun unRegisterIVDownloadFileCallback(callback: IVDownloadFileCallback?) {
        mDownloadFileStatusCallbacks.remove(callback)
    }

    fun isSessionConnected() = mSID >= 0

    fun isChannelConnected(avChannel: Int): Boolean {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                return (mSID >= 0) && next.mAvIndex >= 0
            }
        }
        return false
    }

    /**
     * 发送正常数据
     */
    fun sendIOCtrl(avChannel: Int, type: Int, data: ByteArray?) {
        data ?: return
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                next.IOCtrlQueue?.Enqueue(type, data)
                break
            }
        }
    }

    /**
     * 发送控制命令、如云台转到等
     * 这种命令发送频率快
     */
    fun sendFastIOCtrl(avChannel: Int, type: Int, data: ByteArray?) {
        data ?: return
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                if ((next.IOCtrlFastQueue?.getQueueSize() ?: 0) < AVIOCTRL_FAST_MAX_SEND_SIZE) {
                    next.IOCtrlFastQueue?.Enqueue(type, data)
                }
                return
            }
        }
    }

    //发送控制命令、如云台转到等
    fun clearFastIoQueue(avChannel: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                next.IOCtrlFastQueue?.removeAll()
                break
            }
        }
    }

    /**
     * 连接
     */
    private fun _connect() {
        startConnectJob()
    }

    fun disconnect() {
        val iterator = mAVChannels.iterator()
        Liotc.d("disconnect", "disconnect size[${mAVChannels.size}]")
        while (iterator.hasNext()) {
            val next = iterator.next()
            next.stop()
            if (next.mAvIndex >= 0) {
                AVAPIs.avClientStop(next.mAvIndex)
            }
        }
        mAVChannels.clear()
        stopConnectJob()


        mSendFileUri = null
        mSendFile = null
        mAvSendFileChannel = null
        mSendFileContext = null

        mSupportStreamList.clear()
        if (mSID >= 0) {
            IOTCAPIs.IOTC_Session_Close(mSID)
        }
        mSID = -1

    }

    fun connect(channel: Int = DEFAULT_AV_CHANNEL, account: String = "admin") {
        _connect()
        start(channel, account, psw)
        getSupportStream(channel)
        getAudioCodec(channel)
        getTimeZone(channel, must = true)
    }

    fun reconnect(channel: Int = DEFAULT_AV_CHANNEL, account: String = "admin") {
        Liotc.d("startConnectJob","reconnect")
        disconnect()
        connect(channel, account)
    }


    /**广播设备在线状态*/
    private fun broadCameraSessionStatus(status: Int) {
        val iterator = mOnSessionChannelCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveSessionInfo(this, status)
        }
    }

    /**广播设备在线状态  channel*/
    private fun broadCameraChannelInfo(channel: Int, status: Int) {
        val iterator = mOnSessionChannelCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveChannelInfo(this, channel, status)
        }
    }

    /**广播设备接收到的数据*/
    private fun broadCameraReceiverIOCtrlData(channel: Int, type: Int, data: ByteArray?) {
        when (type) {
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_RESP -> {
                //发送文件命令
                if (mAvSendFileChannel == channel) {
                    startSendFile()
                }
            }
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_RESP -> {
                mSupportStreamList.clear()
                if (data == null || data.size < 4 || channel != DEFAULT_AV_CHANNEL) return
                if (channel == DEFAULT_AV_CHANNEL && isMultiStreamSupport(channel)) {
                    var num = data.littleInt(0)
                    val offset = 4
                    val eachSize = 8
                    Liotc.d("parseSupportStream", "num[$num] 1")
                    if ((data.size - offset) / eachSize != num) {
                        num = (data.size - offset) / eachSize
                    }
                    Liotc.d("parseSupportStream", "num[$num] 2")
                    (0 until num).forEach { index ->
                        var start = index * eachSize + offset
                        val tSupportStream = TSupportStream(
                            data.littleShort(start).toInt(),
                            data.littleShort(start + 2).toInt()
                        )
                        Liotc.d(
                            "parseSupportStream",
                            "supportStream[$tSupportStream],account[$viewAccount],psw[$psw],[$uid]"
                        )
                        mSupportStreamList.add(tSupportStream)
                        start(tSupportStream.channel, viewAccount, psw)
                    }
                }

            }
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DOWNLOAD_FILE_RESP -> {
                //下载文件
                if (mAvDownloadFileChannel == channel) {
                    startDownloadFile()
                }
            }
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_RESP -> {
                //获取时区
                data.parseTimeZone()?.let { tTimeZone ->
                    mTTimeZone = tTimeZone
                }
            }
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIMEZONE_RESP -> {
                //设置设备时区
                data.parseTimeZone()?.let { tTimeZone ->
                    mTTimeZone = tTimeZone
                }
            }
        }

        val iterator = mOnIOCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveIOCtrlData(this, channel, type, data)
        }
    }


    private fun broadCameraReceiveExtraInfo(
        channel: Int,
        type: Int,
        recvFrame: Int,
        dispFrame: Int
    ) {
        val iterator = mOnExtraCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveExtraInfo(this, channel, type, recvFrame, dispFrame)
        }
    }

    private fun broadCameraReceiverFrameData(channel: Int, bitmap: Bitmap?) {
        val iterator = mOnFrameCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveFrameData(this, channel, bitmap)
        }
    }

    private fun broadCameraReceiverFrameData(channel: Int, bitmap: Bitmap?,time:Long) {
        val iterator = mOnFrameCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveFrameData(this, channel, bitmap,time)
        }
    }


    private fun broadCameraReceiverFrameInfo(
        channel: Int,
        bitRate: Int,
        franRate: Int,
        onlineNumber: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    ) {
        val iterator = mOnFrameCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next().receiveFrameInfo(
                this,
                channel,
                bitRate.toLong(),
                franRate,
                onlineNumber,
                frameCount,
                incompleteFrameCount
            )
        }
    }


    private var connectJob: Job? = null
    private var connecting = false

    private fun isActive() = connectJob?.isActive == true

    //开启连接
    private fun startConnectJob() {
        connecting = true
        var first = true
        if (connectJob?.isActive == true) {
            d("startConnectJob", "connectJob isActive")
            return
        }
        connectJob = GlobalScope.launch(Dispatchers.Main) {

            flow {
                delay(500L)
                val stSInfoEx = St_SInfoEx()
                var ret = -1
                var isReconnect = false
                while (connecting && connectJob?.isActive == true) {
                    ret = -1
                    if (first) {
                        while (mSID < 0 && connectJob?.isActive == true) {
                            if (isActive() && !isReconnect) {
                                emit(CONNECTION_STATE_CONNECTING)
                            }
                            mSID = IOTC_CONNECT_ING
                            setAvChannelSid(mSID)

                            nGSID = IOTCAPIs.IOTC_Get_SessionID()

                            d(
                                "startConnectJob",
                                "===ThreadConnectDev retonline ing[$nGSID],devid[$uid]"
                            )
                            if (nGSID >= 0) {
                                mSID = IOTCAPIs.IOTC_Connect_ByUID_Parallel(uid, nGSID)
                                setAvChannelSid(mSID)
                                d(
                                    "startConnectJob",
                                    "===ThreadConnectDev retonline id[$mSID],devid[$uid]"
                                )
                            }

                            when {
                                mSID >= 0 -> {
                                    if (isActive()) {
                                        emit(CONNECTION_STATE_CONNECTED)
                                    }
                                    d(
                                        "startConnectJob",
                                        "===ThreadConnectDev connect ok msid[$mSID],devid[$uid],[${isActive()}]"
                                    )
                                }
                                mSID == IOTCAPIs.IOTC_ER_CONNECT_IS_CALLING -> {

                                    delay(1000L)
                                }
                                (mSID == IOTCAPIs.IOTC_ER_UNKNOWN_DEVICE) || (mSID == IOTCAPIs.IOTC_ER_UNLICENSE)
                                        || (mSID == IOTCAPIs.IOTC_ER_CAN_NOT_FIND_DEVICE) -> {
                                    if (isActive()) {
                                        emit(CONNECTION_STATE_UNKNOWN_DEVICE)
                                    }
                                    break
                                }
                                mSID == IOTCAPIs.IOTC_ER_TIMEOUT -> {
                                    break
                                }
                                mSID == IOTCAPIs.IOTC_ER_DEVICE_NOT_SECURE_MODE || mSID == IOTCAPIs.IOTC_ER_DEVICE_SECURE_MODE -> {
                                    if (isActive()) {
                                        emit(CONNECTION_STATE_UNSUPPORTED)
                                    }
                                    break
                                }
                                else -> {
                                    if (isActive()) {
                                        emit(CONNECTION_STATE_CONNECT_FAILED)
                                    }
                                    break
                                }
                            }
                        }
                    }
                    first = false

                    if (mSID >= 0 && mSID != IOTC_CONNECT_ING) {
                        d("startConnectJob", "start check dev status 111")
                        delay(30000L)
                        d("startConnectJob", "start check dev status 2222")
                        if (!connecting || connectJob?.isActive != true) {
                            d("startConnectJob", "start check dev status stop 111")
                            break
                        }
                        if (mSID >= 0 && mSID != IOTC_CONNECT_ING) {
                            ret = IOTCAPIs.IOTC_Session_Check_Ex(mSID, stSInfoEx)
                            d(
                                "startConnectJob",
                                "ThreadCheckDevStatus status[$ret],[$uid]"
                            )

                            if (ret >= 0) {
                                if (mSessionMode != stSInfoEx.Mode.toInt()) {
                                    mSessionMode = stSInfoEx.Mode.toInt()
                                } else {
//                                        if (connecttimes++ > ConnectDev_50Times) {
//                                            break
//                                        } else {
//                                    delay(3000)
//                                        }
                                }
                            } else if (ret == IOTCAPIs.IOTC_ER_REMOTE_TIMEOUT_DISCONNECT
                                || ret == IOTCAPIs.IOTC_ER_TIMEOUT
                            ) {
                                if (isActive()) {
                                    emit(CONNECTION_STATE_TIMEOUT)
                                }
                                mSID = -1
                                setAvChannelSid(mSID)
                            } else {
                                if (isActive()) {
                                    emit(CONNECTION_STATE_CONNECT_FAILED)
                                }
                                mSID = -1
                                setAvChannelSid(mSID)
                            }
                        }
                    }

                    if(ret < 0 && mReconnectTime >= 1000 && connecting && connectJob?.isActive == true){
                        Liotc.d("startConnectJob","reconnect [$mReconnectTime],[$ret]")
                        delay(mReconnectTime)
                        if(connecting && connectJob?.isActive == true){
                            emit(IOTC_CONNECT_ING)
                        }
                        break
                    }
                }

                //关闭
//                if (nGSID >= 0) {
//                    IOTCAPIs.IOTC_Connect_Stop_BySID(nGSID)
//                }
                d("startConnectJob", "stopThread startConnectJob uid[$uid]")
                if (isActive()) {
                    emit(-1)
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    when(it){
                        IOTC_CONNECT_ING->{
                            reconnect(DEFAULT_AV_CHANNEL)
                        }
                        -1->{
                            d("startConnectJob", "disconnect")
                        }
                        else->{
                            broadCameraSessionStatus(it)
                        }
                    }
                    d("startConnectJob", "emir [$it]")
                }

            d("startConnectJob", "===ThreadConnectDev exit===")
        }
    }

    private fun stopConnectJob() {
        connecting = false
        val s = System.currentTimeMillis()
        if (nGSID >= 0) {
            IOTCAPIs.IOTC_Connect_Stop_BySID(nGSID)
        }
        d("startConnectJob", "stopConnectJob time[${System.currentTimeMillis() - s}]")
        connectJob?.cancel()
    }

    private fun setAvChannelSid(sid: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            next.setSid(sid)
        }
    }

    /**
     *开启 接收IO/发送IO的线程
     *
     **/
    fun start(avChannel: Int, viewAccount: String, viewPasswd: String) {

        var channel: AVChannel? = null
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                channel = next
                break
            }
        }

        if (channel == null) {
            channel = AVChannel(avChannel, viewAccount, viewPasswd, uid, this)
            channel.setSid(mSID)
            mAVChannels.add(channel)
        }
        channel.start()
    }

    fun stop(avChannel: Int) {
        val iterator = mAVChannels.iterator()
        var channel: AVChannel? = null
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.mChannel == avChannel) {
                next.stop()
                channel = next
                if (next.mAvIndex >= 0) {
                    AVAPIs.avClientStop(next.mAvIndex)
                }

                break
            }
        }
        mAVChannels.remove(channel)
    }

    internal fun setPlayMode(channel: Int, playMode: PlayMode = PlayMode.PLAY_LIVE) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.playMode = playMode
                break
            }
        }
    }

    internal fun setVoiceType(channel: Int, voiceType: VoiceType = VoiceType.ONE_WAY_VOICE) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.setVoiceType(voiceType)
                break
            }
        }
    }

    /**开启视频直播*/
    @Synchronized
    internal fun startShow(context: Context?, channel: Int,ratation:Int = 0,withYuv:Boolean = false) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.startShow(context,ratation,withYuv)
                break
            }
        }
    }

    @Synchronized
    internal fun stopShow(channel: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.stopShow()
                break
            }
        }
    }

    //设置音频播放
    @Synchronized
    internal fun setAudioTrackStatus(context: Context?, channel: Int, status: Boolean) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.setAudioTrackStatus(context, status,LocalRecordHelper.recording)
                break
            }
        }
    }

    //设置音频录制(发送音频)
    @Synchronized
    internal fun setAudioRecordStatus(context: Context?, channel: Int, status: Boolean) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.setAudioRecordStatus(context, status)
                break
            }
        }
    }

    //释放音频资源
    internal fun releaseAudio(channel: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {
                avChannel.releaseAudio()
                break
            }
        }
    }

    //开启录像
    internal fun startRecord(
        context: Context?,
        channel: Int,
        file: String?,
        width: Int,
        height: Int,
        onResultCallback: OnResultCallback<RecordStatus>? = null
    ) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val avChannel = iterator.next()
            if (avChannel.mChannel == channel) {

                LocalRecordHelper.startRecord(
                    context,
                    file,
                    width,
                    height,
                    avChannel,
                    onResultCallback
                )
                return
            }
        }
        onResultCallback?.onResult(RecordStatus.VIDEO_CODEC_NULL)
    }

    //关闭录像
    internal fun stopRecord() {
        Liotc.d("stopRecord", "stopRecord")
        LocalRecordHelper.stopRecord()

    }

    //发送音频文件
    fun sendFile(
        context: Context?,
        channel: Int,
        name: String?,
        alias: String?,
        tracks: Int,
        file: String?,
        date:Long = System.currentTimeMillis()/1000
    ) {
        if (name.isNullOrEmpty() || alias.isNullOrEmpty() || file.isNullOrEmpty() || mSendFileUri != null || !mSendFile.isNullOrEmpty()) {
            onAVChannelSendFileStatus(SendFileStatus.RDSENDER_STATE_STOP, 0, 0, 0)
            return
        }
        //通知设备开始发送音频文件

        sendSendFileOrder(channel, name, alias, tracks, date)
        mAvSendFileChannel = channel
        mSendFile = file
        mSendFileUri = null
        mSendFileContext = WeakReference(context)
    }

    fun sendFileUri(
        context: Context?,
        channel: Int,
        name: String?,
        alias: String?,
        tracks: Int,
        file: Uri?,
        date:Long = System.currentTimeMillis()/1000
    ) {
        if (name.isNullOrEmpty() || alias.isNullOrEmpty() || file == null || mSendFileUri != null || !mSendFile.isNullOrEmpty()) {
            onAVChannelSendFileStatus(SendFileStatus.RDSENDER_STATE_STOP, 0, 0, 0)
            return
        }
        //通知设备开始发送音频文件
        sendSendFileOrder(channel, name, alias, tracks, date)
        mAvSendFileChannel = channel
        mSendFileUri = file
        mSendFile = null
        mSendFileContext = WeakReference(context)
    }

    private fun startSendFile() {
        d(TAG, "开始发送音频文件 channel[$mAVChannels]")
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val channel = iterator.next()
            if (channel.mAvIndex == mAvSendFileChannel) {
                if (!mSendFile.isNullOrEmpty()) {
                    d(TAG, "开始发送音频文件 channel[$mAVChannels],file[$mSendFile]")
                    channel.startSendFile(mSendFileContext?.get(), mSendFile)
                } else if (mSendFileUri != null) {
                    d(TAG, "开始发送音频文件 channel[$mAVChannels],uri[$mSendFileUri]")
                    channel.startSendFile(mSendFileContext?.get(), mSendFileUri)
                }
                break
            }
        }
    }

    //停止发送文件
    fun stopSendFile(channelIndex: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val channel = iterator.next()
            if (channel.mAvIndex == channelIndex) {
                channel.releaseSendFile()
                mSendFileContext = null
                mSendFile = null
                mSendFileUri = null
                mAvSendFileChannel = null
                break
            }
        }
    }

    //下载文件
    fun downFile(context: Context?, channelIndex: Int, srcFile: FileDirectory, dstFile: String?) {
        downFile(context, channelIndex, srcFile.file, dstFile)
    }

    fun downFile(context: Context?, channelIndex: Int, srcFile: String?, dstFile: String?) {
        if (srcFile.isNullOrEmpty() || dstFile.isNullOrEmpty() || !mDownloadDstFile.isNullOrEmpty() || mDownloadDstUri != null || !mDownloadSrcFile.isNullOrEmpty()) {
            onAVChanneldownloadFileStatus(DownLoadFileStatus.DOWNLOAD_STATE_ERROR, 0, 0, 0)
            return
        }
        mDownloadDstFile = dstFile
        mDownloadFileContext = WeakReference(context)
        mDownloadSrcFile = srcFile
        mAvDownloadFileChannel = channelIndex
        mDownloadDstUri = null
        sendDownloadFileOrder(channelIndex)
    }

    //下载文件
    fun downFile(context: Context?, channelIndex: Int, srcFile: FileDirectory, dstUri: Uri?) {
        downFile(context, channelIndex, srcFile.file, dstUri)
    }

    fun downFile(context: Context?, channelIndex: Int, srcFile: String?, dstUri: Uri?) {
        if (srcFile.isNullOrEmpty() || dstUri == null || !mDownloadDstFile.isNullOrEmpty() || mDownloadDstUri != null || !mDownloadSrcFile.isNullOrEmpty()) {
            onAVChanneldownloadFileStatus(DownLoadFileStatus.DOWNLOAD_STATE_ERROR, 0, 0, 0)
            return
        }
        mDownloadDstUri = dstUri
        mDownloadFileContext = WeakReference(context)
        mDownloadSrcFile = srcFile
        mAvDownloadFileChannel = channelIndex
        mDownloadDstFile = null
        sendDownloadFileOrder(channelIndex)
    }

    //下载文件
    private fun startDownloadFile() {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val channel = iterator.next()
            if (channel.mAvIndex == mAvDownloadFileChannel) {
                if (!mDownloadDstFile.isNullOrEmpty()) {
                    d(
                        TAG,
                        "开始下载文件 channel[$mAVChannels],src[${mDownloadSrcFile}],dst[$mDownloadDstFile]"
                    )
                    channel.startDownloadFile(mDownloadSrcFile, mDownloadDstFile)
                } else if (mDownloadDstUri != null) {
                    d(
                        TAG,
                        "开始下载文件 channel[$mAVChannels],src[${mDownloadSrcFile}],uri[$mDownloadDstUri]"
                    )
                    channel.startDownloadFile(
                        mDownloadFileContext?.get(),
                        mDownloadSrcFile,
                        mDownloadDstUri
                    )
                }
                break
            }
        }
    }

    fun stopDownloadFile(channelIndex: Int) {
        val iterator = mAVChannels.iterator()
        while (iterator.hasNext()) {
            val channel = iterator.next()
            if (channel.mAvIndex == channelIndex) {
                channel.releaseDownloadFile()
                mDownloadFileContext = null
                mDownloadSrcFile = null
                mDownloadDstUri = null
                mDownloadDstFile = null
                mAvDownloadFileChannel = null
                break
            }
        }
    }

    internal fun getAvChannel(avChannel: Int): AVChannel? {
        return mAVChannels.singleOrNull { it.mChannel == avChannel }
    }

    internal fun isMultiStreamSupport(channel: Int = 0) =
        mAVChannels.singleOrNull { it.mChannel == channel }?.supportMultiStreamMode() ?: false

    fun supportTimeZone() = mTTimeZone?.supportTimeZone ?: false
    fun getTimeZoneGmtDiff() = mTTimeZone?.gmtDiff


    private fun d(tag: String, msg: String) {
        Liotc.d(tag, "$msg   uid[$uid]")
    }

    private fun i(tag: String, msg: String) {
        Liotc.i(tag, "$msg   uid[$uid]")
    }

    private fun e(tag: String, msg: String) {
        Liotc.e(tag, "$msg   uid[$uid]")
    }


    override fun onAVChannelStatus(channel: Int, status: Int) {
        broadCameraChannelInfo(channel, status)
    }

    override fun onAVChannelRecv(channel: Int, type: Int, data: ByteArray?) {
        broadCameraReceiverIOCtrlData(channel, type, data)
    }

    override fun onAVChannelReceiveExtraInfo(
        channel: Int,
        type: Int,
        recvFrame: Int,
        dispFrame: Int
    ) {
        broadCameraReceiveExtraInfo(channel, type, recvFrame, dispFrame)
    }

    override fun onAVChannelReceiverFrameData(channel: Int, bitmap: Bitmap?) {
        broadCameraReceiverFrameData(channel, bitmap)
    }

    override fun onAVChannelReceiverFrameData(channel: Int, bitmap: Bitmap?, time: Long) {
        broadCameraReceiverFrameData(channel, bitmap,time)
    }

    override fun onAVChannelReceiverFrameInfo(
        channel: Int,
        bitRate: Int,
        franRate: Int,
        onlineNumber: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    ) {
        broadCameraReceiverFrameInfo(
            channel,
            bitRate,
            franRate,
            onlineNumber,
            frameCount,
            incompleteFrameCount
        )
    }

    override fun onAVChannelRecordStatus(status: RecordStatus, file: String?, time: Int) {
        val iterator = mIAVChannelRecordStatus.iterator()
        while (iterator.hasNext()) {
            iterator.next().onAVChannelRecordStatus(status, file, time)
        }
    }

    override fun onAVChannelSendFileStatus(
        status: SendFileStatus,
        total: Int,
        sendTotal: Int,
        progress: Int
    ) {
        val iterator = mSendFileStatusCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next()?.onAvChannelSendFileStatus(status, total, sendTotal, progress)
        }
    }

    override fun onAVChanneldownloadFileStatus(
        status: DownLoadFileStatus,
        total: Int,
        downloadTotal: Int,
        progress: Int
    ) {
        d(TAG, "onAVChanneldownloadFileStatus[${mDownloadFileStatusCallbacks.size}]")
        val iterator = mDownloadFileStatusCallbacks.iterator()
        while (iterator.hasNext()) {
            iterator.next()?.onAvChannelDownloadFileStatus(status, total, downloadTotal, progress)
        }
        if (status == DownLoadFileStatus.DOWNLOAD_STATE_FINISH
            || status == DownLoadFileStatus.DOWNLOAD_STATE_CANCEL
            || status == DownLoadFileStatus.DOWNLOAD_STATE_CLOSED
            || status == DownLoadFileStatus.DOWNLOAD_STATE_ERROR
        ) {
            mDownloadDstFile = null
            mDownloadDstUri = null
            mDownloadSrcFile = null
            mAvDownloadFileChannel = null
            mDownloadFileContext = null
        }
    }
}