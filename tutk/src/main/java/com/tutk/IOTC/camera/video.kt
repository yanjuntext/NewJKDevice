package com.tutk.IOTC.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.util.Log
import com.decoder.util.DecMpeg4
import com.jkapp.android.media.VideoDecoder
import com.qxt.yuv420.LibyuvUtils
import com.tutk.IOTC.*
import com.tutk.IOTC.listener.OnResultCallback
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.RecordStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description: 视频处理类相关
 */


private fun Int.dp() = 10

/**视频数据*/
data class RecvVideoInfo(
    val eventType: Int,
    val recvFrame: Int = 0,
    val dispFrame: Int = 0,
    val bitRate: Int = 0,
    val frameRate: Int = 0,
    val onlineNm: Int = 0,
    val frameCount: Int = 0,
    val incompleteFrameCount: Int = 0,
    val bitmap: Bitmap? = null,
    val deviceTime: Long = 0L,

    )

/**视频数据接收*/
class RecvVideoJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {

    private val TAG = "RecvVideoJob"
    private var runJob: Job? = null

    private var mSID = -1


    private var mNoFramIndex = 0

    private var isFirstIFrame = true

    private var isFirstRecording = false

    private var isChangeVideoQuality = false


    fun setSid(sid: Int) {
        mSID = sid
    }

    private fun isActive() = runJob?.isActive == true

    private fun getAvIndex() = avChannel?.mAvIndex ?: -1

    private fun getFrameInfo(online: Int, frameCount: Int, nIncompleteFrmCount: Int) =
        RecvVideoInfo(
            -1, bitRate = ((avChannel?.audioBPS ?: 0) + (avChannel?.videoBPS ?: 0)) * 8 / 1024,
            frameRate = avChannel?.videoFPS ?: 0,
            onlineNm = online, frameCount = frameCount, incompleteFrameCount = nIncompleteFrmCount
        )

    private fun getFrameBitmapInfo(bmp: Bitmap?, deviceTime: Long) =
        RecvVideoInfo(-2, bitmap = bmp, deviceTime = deviceTime)

    private fun getFrameTimeInfo(deviceTime: Long) =
        RecvVideoInfo(-3, deviceTime = deviceTime)

    private fun requestIFrame() {
        if (isRunning && isActive() && getAvIndex() >= 0) {
            d(TAG, "发送511命令")
            avChannel?.IOCtrlQueue?.Enqueue(
                getAvIndex(),
                511,
                Packet.intToByteArray_Little(0)
            )
        }
    }

    fun start() {
        isRunning = true
        if (isActive()) {
            d(TAG, "RecvVideoJob is Running,not rerun")
            return
        }
        isChangeVideoQuality = false
        val time = 0L
        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                avChannel?.refreshSid()
                while (isRunning && isActive && (mSID < 0 || getAvIndex() < 0 || mSID == IOTC_CONNECT_ING)) {
                    avChannel?.refreshSid()
                    delay(100)
                }

                isFirstIFrame = true
                avChannel?.videoBPS = 0
                val buf = ByteArray(MAX_BUF_SIZE)
                val pFrmInfoBuf = ByteArray(24)
                val pFrmNo = IntArray(1)
                var nCodecId = 0
                var nReadSize = 0
                var nFrmCount = 0
                var nIncompleteFrmCount = 0
                var nOnlineNumber = 0
                var nPrevFrmNo: Long = 0xFFFFFFFL
                var nLastTimeStamp = System.currentTimeMillis()
                var nFlow_total_frame_count = 0
                var nFlow_total_expected_frame_size = 0
                var nFlow_total_actual_frame_size = 0

                val outBufSize = IntArray(1)
                val outFrmSize = IntArray(1)
                val outFrmInfoBufSize = IntArray(1)

                d(TAG, "start: RecvVideoJob start")
                if (mSID >= 0 && getAvIndex() >= 0) {
                    d(TAG, "start: RecvVideoJob clean buf 1")
//                    AVAPIs.avClientCleanBuf(getAvIndex())
                    d(TAG, "start: RecvVideoJob clean buf 2")
//                    AVAPIs.avClientCleanLocalBuf(getAvIndex())
//                    AVAPIs.avClientCleanLocalVideoBuf(getAvIndex())
                }
                d(TAG, "start: RecvVideoJob clean remove all 1")

//                avChannel?.VideoFrameQueue?.removeAll()
                d(TAG, "start: RecvVideoJob clean remove all 2")

                mNoFramIndex = 0

                if (isRunning && isActive && getAvIndex() >= 0) {
                    d(TAG, "发送511命令")
                    avChannel?.IOCtrlQueue?.Enqueue(
                        getAvIndex(),
                        511,
                        Packet.intToByteArray_Little(0)
                    )
                }
                var lastIFrameSize = -1
                //发送过511命令之后再通知上层清楚命令，优先保证视频获取
                emit(RecvVideoInfo(Camera.EXTRA_EVENT_HAD_CLEAN_VIDEOBUF))

                var lastIFrame: ByteArray? = null
                var time = 0L
                while (isRunning) {
                    avChannel?.refreshSid()
                    if (mSID >= 0 && getAvIndex() >= 0) {
//                        if (isChangeVideoQuality) {
//                            Liotc.d("AvChannel", "changeQualityStopDecoderVideo: remove all frame ")
//                            avChannel?.VideoFrameQueue?.removeAll()
//                            avChannel?.IOCtrlQueue?.Enqueue(
//                                getAvIndex(),
//                                511,
//                                Packet.intToByteArray_Little(0)
//                            )
//
//                            isChangeVideoQuality = false
//                            continue
//                        }
                        if (System.currentTimeMillis() - nLastTimeStamp > 1000L) {
                            nLastTimeStamp = System.currentTimeMillis()
                            emit(
                                getFrameInfo(
                                    online = nOnlineNumber,
                                    frameCount = nFrmCount,
                                    nIncompleteFrmCount
                                )
                            )
                            avChannel?.videoFPS = 0
                            avChannel?.videoBPS = 0
                        }

                        nReadSize = AVAPIs.avRecvFrameData2(
                            getAvIndex(), buf, buf.size, outBufSize, outFrmSize, pFrmInfoBuf,
                            pFrmInfoBuf.size, outFrmInfoBufSize, pFrmNo
                        )
                        d(TAG, "amera video data nReadSize=[$nReadSize],running[$isRunning]")
                        d(
                            TAG,
                            "AVAPIs.AV_ER_INCOMPLETE_FRAME========== nReadSize=[$nReadSize],running[$isRunning]"
                        )

                        d(TAG, "AVAPIs.AV_ER_INCOMPLETE_FRAME=" + pFrmInfoBuf.getHex())

                        var isFirstRecordingIFrame = false;
                        when {
                            nReadSize >= 0 -> {
                                avChannel?.videoBPS = (avChannel?.videoBPS ?: 0) + outBufSize[0]
                                nFrmCount++

                                val framData = ByteArray(nReadSize)
                                System.arraycopy(buf, 0, framData, 0, nReadSize)

                                val fram = AVFrame(
                                    pFrmNo[0].toLong(),
                                    AVFrame.FRM_STATE_COMPLETE,
                                    pFrmInfoBuf,
                                    framData,
                                    nReadSize,
                                    avChannel?.playMode?.value ?: PlayMode.PLAY_LIVE.value
                                )
                                emit(getFrameTimeInfo(fram.deviceCurrentTime))
                                d(TAG, "---codeId=${fram.codec_id.toInt()}")
                                avChannel?.codeId = fram.codec_id.toInt()
                                d(
                                    TAG,
                                    "AVAPIs.AV_ER_INCOMPLETE_FRAME========== nReadSize=[$nReadSize],running[$isRunning]  i frame[,IFrame[${fram.isIFrame()}]"
                                )
                                nCodecId = fram.codec_id.toInt()
//                                d(TAG, "camera video data nReadSize[$nReadSize],setSize[]")

//                            if (avChannel?.recording == true && fram.isIFrame() && LocalRecordHelper.recording) {

//                                if(fram.isIFrame() || nCodecId == AVFrame.MEDIA_CODEC_VIDEO_MJPEG){
//
//                                    lastIFrame = ByteArray(fram.frmSize)
//                                    System.arraycopy(fram.frmData,0,lastIFrame,0,fram.frmSize)
//                                }
                                if (!fram.isIFrame() && LocalRecordHelper.recording && !isFirstRecording && nCodecId != AVFrame.MEDIA_CODEC_VIDEO_MJPEG) {
                                    isFirstRecording = true
//                                    requestIFrame()
                                }
                                if (nCodecId == AVFrame.MEDIA_CODEC_VIDEO_MJPEG && !isFirstRecording && LocalRecordHelper.recording) {
                                    isFirstIFrame = true
                                }
                                if (LocalRecordHelper.recording) {
                                    d(
                                        TAG,
                                        "***LocalRecordHelper recording iFrame=---${fram.isIFrame()}"
                                    )
                                }

//                                if(LocalRecordHelper.recording){
//                                    if(lastIFrame != null){
//                                        d(TAG,"***-------LocalRecordHelper recording iFrame")
//                                        LocalRecordHelper.setParseBuffer(lastIFrame)
//                                        LocalRecordHelper.canRecording = true
//                                        val fData = ByteArray(lastIFrame.size)
//                                        System.arraycopy(lastIFrame,0,fData,0,lastIFrame.size)
//                                        LocalRecordHelper.recordVideoFrame(fData,fData.size,true)
//                                        lastIFrame = null
//                                    }else{
//                                        val fData = ByteArray(fram.frmSize)
//                                        System.arraycopy(fram.frmData,0,fData,0,fram.frmSize)
//                                        d(TAG,"***-------LocalRecordHelper recording iFrame recordVideoFrame")
//                                        LocalRecordHelper.recordVideoFrame(fData,fData.size,fram.isIFrame())
//                                    }
//                                }

//
                                if (LocalRecordHelper.recording) {
//                                    if ((fram.isIFrame() || nCodecId == AVFrame.MEDIA_CODEC_VIDEO_MJPEG) ) {
                                    if(time == 0L){
                                        time = System.currentTimeMillis()
                                    }
                                    if(fram.isIFrame()){
                                        val cT = System.currentTimeMillis()
                                        d(TAG,"recrddddddddd time==${(cT - time)/1000}")
                                        time = cT
                                    }
                                    d(TAG, "333LocalRecordHelper recording iFrame")
                                    LocalRecordHelper.setParseBuffer(fram.frmData)
                                    LocalRecordHelper.canRecording = true
//                                    }
                                    val fData = ByteArray(fram.frmSize)
                                    System.arraycopy(fram.frmData, 0, fData, 0, fram.frmSize)
                                    d(TAG, "333LocalRecordHelper recording iFrame recordVideoFrame")
                                    LocalRecordHelper.recordVideoFrame(
                                        fData,
                                        fData.size,
                                        fram.isIFrame()
                                    )
                                }

                                if (!LocalRecordHelper.recording) {
                                    isFirstRecording = false
                                    time = 0L
                                }


                                nOnlineNumber = fram.onlineNum.toInt()
                                d(TAG, "codeId=$nCodecId")

                                when (nCodecId) {
                                    AVFrame.MEDIA_CODEC_VIDEO_H264,
                                    AVFrame.MEDIA_CODEC_VIDEO_H265 -> {
                                        if (fram.isIFrame()) {
                                            mNoFramIndex = -1
                                        }
                                        if (fram.isIFrame() || pFrmNo[0].toLong() == nPrevFrmNo + 1) {
                                            nPrevFrmNo = pFrmNo[0].toLong()

                                            avChannel?.videoFPS = (avChannel?.videoFPS ?: 0) + 1
                                            avChannel?.VideoFrameQueue?.addLast(fram)
                                        }
                                    }

                                    AVFrame.MEDIA_CODEC_VIDEO_MPEG4 -> {
                                        if (fram.isIFrame() && isFirstIFrame) {
                                            avChannel?.IOCtrlQueue?.Enqueue(
                                                4098, AVIOCTRLDEFs.receiveFirstIFrame(
                                                    avChannel.mChannel, 0
                                                )
                                            )
                                            isFirstIFrame = false
                                        }

                                        if (fram.isIFrame() || (pFrmNo[0].toLong() == nPrevFrmNo + 1)) {
                                            nPrevFrmNo = pFrmNo[0].toLong()

                                            avChannel?.videoFPS = (avChannel?.videoFPS ?: 0) + 1
                                            d(TAG, "recv video quality=${isChangeVideoQuality}")
                                            avChannel?.VideoFrameQueue?.addLast(fram)
                                        }
                                    }

                                    AVFrame.MEDIA_CODEC_VIDEO_MJPEG -> {
//                                        if (LocalRecordHelper.recording) {
//                                            LocalRecordHelper.recordVideoFrame(
//                                                fram.frmData,
//                                                fram.frmSize,
//                                                true
//                                            )
//                                        }
                                        try {
                                            val bmp =
                                                BitmapFactory.decodeByteArray(
                                                    framData,
                                                    0,
                                                    nReadSize
                                                )
                                            d(TAG, "MEDIA_CODEC_VIDEO_MJPEG bmp----")
                                            emit(getFrameBitmapInfo(bmp, fram.deviceCurrentTime))
                                            avChannel?.lastFrame = bmp
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        delay(32L)
                                    }
                                }
                            }

                            nReadSize == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE
                                    || nReadSize == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT -> {
                                d(TAG, "nReadSize=[$nReadSize]")
                            }

                            nReadSize == AVAPIs.AV_ER_DATA_NOREADY -> {
                                delay(32)
                                d(TAG, "-------------- mNoFramIndex=${mNoFramIndex}")
//                                d(TAG, "AVAPIs.AV_ER_DATA_NOREADY mNoFramIndex[$mNoFramIndex]")
                                if (mNoFramIndex >= 0) {
                                    mNoFramIndex++
                                    if (mNoFramIndex >= 35) {
                                        d(TAG, "++++ mNoFramIndex=${mNoFramIndex}")
                                        if (isRunning && isActive && getAvIndex() >= 0 && mSID >= 0) {
                                            d(TAG, "*** mNoFramIndex=${mNoFramIndex}")
                                            //重新请求IFrame
                                            if (nCodecId != AVFrame.MEDIA_CODEC_VIDEO_MJPEG) {
                                                avChannel?.IOCtrlQueue?.Enqueue(
                                                    getAvIndex(), 511,
                                                    Packet.intToByteArray_Little(0)
                                                )
                                            }
                                            d(TAG, "### mNoFramIndex=${mNoFramIndex}")
                                            mNoFramIndex = 0
                                        }
                                    }

                                    d(TAG, "###--- mNoFramIndex=${mNoFramIndex}")
                                }
                            }

                            nReadSize == AVAPIs.AV_ER_MEM_INSUFF
                                    || nReadSize == AVAPIs.AV_ER_LOSED_THIS_FRAME -> {
                                mNoFramIndex = 0
                                nFrmCount++
                                nIncompleteFrmCount++
                                nFlow_total_frame_count++
                            }

                            nReadSize == AVAPIs.AV_ER_INCOMPLETE_FRAME -> {
                                d(TAG, "AVAPIs.AV_ER_INCOMPLETE_FRAME==========")
                                mNoFramIndex = 0
                                nFrmCount++
                                nFlow_total_actual_frame_size += outBufSize[0]
                                nFlow_total_expected_frame_size += outFrmSize[0]
                                nFlow_total_frame_count++
                                avChannel?.videoBPS = (avChannel?.videoBPS ?: 0) + outBufSize[0]

                                if (outFrmInfoBufSize[0] == 0 || outFrmSize[0] != outBufSize[0] || pFrmInfoBuf[2].toInt() == 0) {
                                    nIncompleteFrmCount++
                                } else {
                                    val _framData = ByteArray(outFrmSize[0])
                                    System.arraycopy(buf, 0, _framData, 0, outFrmSize[0])
                                    nCodecId =
                                        Packet.byteArrayToShort_Little(pFrmInfoBuf, 0).toInt()

                                    when (nCodecId) {
                                        AVFrame.MEDIA_CODEC_VIDEO_MJPEG -> {
//                                            if (outFrmInfoBufSize[0] == 0 || outFrmSize[0] != outBufSize[0] || pFrmInfoBuf[2].toInt() == 0) {
                                            nIncompleteFrmCount++
//                                            } else {
//                                                val frame = AVFrame(
//                                                    pFrmNo[0].toLong(),
//                                                    AVFrame.FRM_STATE_COMPLETE,
//                                                    pFrmInfoBuf,
//                                                    _framData,
//                                                    outFrmSize[0],
//                                                    avChannel?.playMode?.value
//                                                        ?: PlayMode.PLAY_LIVE.value
//                                                )
//                                                try {
//                                                    val bmp =
//                                                        BitmapFactory.decodeByteArray(
//                                                            frame.frmData,
//                                                            0,
//                                                            frame.frmSize
//                                                        )
//                                                    d(TAG,"MEDIA_CODEC_VIDEO_MJPEG bmp----")
//                                                    emit(getFrameBitmapInfo(bmp, frame.deviceCurrentTime))
//                                                    avChannel?.lastFrame = bmp
//                                                } catch (e: Exception) {
//                                                    e.printStackTrace()
//                                                }
//                                            }

                                        }

                                        AVFrame.MEDIA_CODEC_VIDEO_MPEG4 -> {
                                            nIncompleteFrmCount++
                                        }

                                        AVFrame.MEDIA_CODEC_VIDEO_H264,
                                        AVFrame.MEDIA_CODEC_VIDEO_H265 -> {
                                            if (outFrmInfoBufSize[0] == 0 || outFrmSize[0] != outBufSize[0] || pFrmInfoBuf[2].toInt() == 0) {
                                                nIncompleteFrmCount++
                                            } else {
                                                val frame = AVFrame(
                                                    pFrmNo[0].toLong(),
                                                    AVFrame.FRM_STATE_COMPLETE,
                                                    pFrmInfoBuf,
                                                    _framData,
                                                    outFrmSize[0],
                                                    avChannel?.playMode?.value
                                                        ?: PlayMode.PLAY_LIVE.value
                                                )
                                                if (frame.isIFrame() || pFrmNo[0].toLong() == nPrevFrmNo + 1) {
                                                    d(
                                                        TAG,
                                                        "AVAPIs.AV_ER_INCOMPLETE_FRAME========== addLast"
                                                    )
                                                    nPrevFrmNo = pFrmNo[0].toLong()
                                                    avChannel?.VideoFrameQueue?.addLast(frame)
                                                    nFlow_total_actual_frame_size += outBufSize[0]
                                                    nFlow_total_expected_frame_size += outFrmSize[0]
                                                } else {
                                                    nIncompleteFrmCount++
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                d(TAG, "recvvideo destroy [$mSID],[${getAvIndex()}]")
                avChannel?.VideoFrameQueue?.removeAll()
                if (mSID >= 0 && getAvIndex() >= 0) {
                    avChannel?.IOCtrlQueue?.Enqueue(
                        getAvIndex(),
                        767,
                        Packet.intToByteArray_Little(0)
                    )
                }
                //移除视频缓存
                AVAPIs.avClientCleanLocalVideoBuf(getAvIndex())
            }.flowOn(Dispatchers.IO)
                .collect {

                    when (it.eventType) {
                        Camera.EXTRA_EVENT_HAD_CLEAN_VIDEOBUF -> {
                            iavChannelStatus?.onAVChannelReceiveExtraInfo(
                                avChannel?.mChannel ?: -1,
                                it.eventType,
                                it.recvFrame,
                                it.dispFrame
                            )
                        }

                        -1 -> {
                            iavChannelStatus?.onAVChannelReceiverFrameInfo(
                                avChannel?.mChannel ?: -1,
                                it.bitRate,
                                it.frameRate,
                                it.onlineNm,
                                it.frameCount,
                                it.incompleteFrameCount
                            )
                        }

                        -2 -> {
                            iavChannelStatus?.onAVChannelReceiverFrameData(
                                avChannel?.mChannel ?: -1, it.bitmap,
                            )
                            iavChannelStatus?.onAVChannelReceiverFrameData(
                                avChannel?.mChannel ?: -1, it.bitmap, it.deviceTime
                            )
                        }

                        -3 -> {
                            iavChannelStatus?.onAVChannelReceiverFrameDataTime(it.deviceTime)
                        }
                    }
                }

        }


    }

    fun stop() {
        isRunning = false
        e(TAG, "amera video data stop")
    }

    fun setChangeVideoQuality(changeVideoQuality: Boolean) {
        Liotc.d("AvChannel", "changeQualityStopDecoderVideo: recv ")
//        avChannel?.VideoFrameQueue?.removeAll()
//        isChangeVideoQuality = changeVideoQuality
    }

    fun changeVideoQualityStopDecoderVideo() {

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

/**视频数据解析*/
class DecodeVideoJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?,
    var withYuv: Boolean = false
) {

    private var mContext: WeakReference<Context?>? = null

    private val TAG = "DecodeVideoJob"

    private var runJob: Job? = null

    private var mVideoDecoder: VideoDecoder? = null
    private var mVideoBuffer: ByteBuffer? = null
    private var mVideoOutBuffer: ByteBuffer? = null
    private var mYuvData: ByteArray? = null
    private var mArgbData: ByteArray? = null

    private fun isActive() = runJob?.isActive == true

    private var ratation = 0

    private var isChangeVideoQuality = false

    fun setRatation(ratation: Int) {
        this.ratation = ratation
    }

    fun start(context: Context?) {

        if (context == null) {
            return
        }
        mContext = WeakReference(context)

        if (isRunning) {
            d("DecodeVideoJob is Running,not rerun")
            return
        }

        var avFrameSize = 0
        var videoWidth = 0
        var videoHeight = 0
        var firstTimeStampFromDevice = 0L
        var firstTimeStampFromLocal = 0L
        var sleepTime = 0L
        var t1 = 0L
        var lastUpdateDispFrmPreSec = 0L
        var lastFrameTimeStamp = 0L
        var delayTime = 0L
        val bufOut = ByteArray(MAX_FRAMEBUF)

        var bmp: Bitmap? = null

        val out_width = IntArray(1)
        val out_height = IntArray(1)
        val out_size = IntArray(1)

        var decoderIsInit = false

        var mpegtIsInit = false

        var mAvFrame: AVFrame? = null

        avChannel?.videoFPS = 0
        isRunning = true

        var isWaitIFrame = false
        var llastFrameSize = -1L
        var lcurrentFrameSize = -1L
        var videoDecodeResult = -1

        isChangeVideoQuality = false

        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                var lastIFrameSize = -1
                avChannel?.refreshSid()
                while (isRunning && isActive) {
                    avChannel?.refreshSid()
                    if (isChangeVideoQuality) {
                        Liotc.d("AvChannel", "videoDecodeResult == -4: destory decoder ")
                        decoderIsInit = false
                        try {
                            mVideoDecoder?.unfinalize()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        mVideoBuffer?.clear()
                        mVideoOutBuffer?.clear()
                        mVideoBuffer = null
                        mVideoOutBuffer = null
                        mVideoDecoder = null
                        continue
                    }

                    if (avChannel?.VideoFrameQueue == null) {
                        return@flow
                    }
                    if ((avChannel.VideoFrameQueue?.mSize ?: 0) <= 0) {
                        delay(4L)
                        continue
                    }
                    d("decode video quality=${isChangeVideoQuality}")
                    //如果正在切换视频分辨率的时候，停止解析

                    d("decode video quality=${isChangeVideoQuality} ----")

                    if ((avChannel.VideoFrameQueue?.mSize ?: 0) > 0) {
                        mAvFrame = avChannel.VideoFrameQueue?.removeHead()
                        if (mAvFrame == null) {
                            delay(4L)
                            continue
                        }

                        if (isWaitIFrame && mAvFrame?.isIFrame() != true) {
                            delay(10L)
                            continue
                        }
                        isWaitIFrame = false

                        avFrameSize = mAvFrame?.frmSize ?: 0

                        d("i frame[${mAvFrame?.isIFrame()}] w=${mAvFrame?.videoWidth} h=${mAvFrame?.videoHeight} size=${avChannel.VideoFrameQueue?.mSize}")

                        val count = avChannel.VideoFrameQueue?.mSize ?: 0

                        if (count > 0 && delayTime > 2000) {
                            avChannel.VideoFrameQueue?.isDroping = true

                            var avFrame = avChannel.VideoFrameQueue?.removeHead()

                            if (avFrame == null) {
                                continue
                            }

                            var skipTime = avFrame.timestamp - lastFrameTimeStamp
                            lastFrameTimeStamp = avFrame.timestamp.toLong()

                            while (isActive) {
                                ensureActive()
                                if (avChannel.VideoFrameQueue?.isFirstIFrame() != true) {
                                    avFrame = avChannel.VideoFrameQueue?.removeHead()
                                    if (avFrame == null) {
                                        avChannel.VideoFrameQueue?.isDroping = true
                                        isWaitIFrame = true
                                        break
                                    } else {
                                        skipTime += (avFrame.timestamp - lastFrameTimeStamp)
                                        d("low decode performance, drop [${if (avFrame?.isIFrame() == true) "I" else "P"}]frame, skip time:[${avFrame.timestamp - lastFrameTimeStamp}],total skip:[$skipTime],index[${avFrame.frmNo}],camera video data")
                                        lastFrameTimeStamp = avFrame.timestamp.toLong()
                                    }
                                } else {
                                    d("low decode performance 2, drop [${if (avFrame?.isIFrame() == true) "I" else "P"}]frame, skip time:[${(avFrame?.timestamp ?: 0) - lastFrameTimeStamp}],total skip:[$skipTime],index[${avFrame?.frmNo}],camera video data")
                                    avChannel.VideoFrameQueue?.isDroping = false
                                    delayTime -= skipTime
                                    isWaitIFrame = true
                                    break
                                }
                                delayTime -= skipTime
                            }
                        } else {
//                            if(videoDecodeResult == -4 && mAvFrame?.isIFrame() != true){
//                                continue
//                            }
                            d("camera video dataavFrameSize[$avFrameSize],mAvFramep[${mAvFrame == null}]")

                            if (avFrameSize > 0 && isActive) {
                                out_size[0] = 0
                                out_width[0] = 0
                                out_height[0] = 0

                                mAvFrame?.let { avFrame ->
                                    val frmData = avFrame.frmData ?: return@let
                                    val total = frmData.size
                                    d("frmData [${total}],[$avFrameSize]")
                                    if (total >= avFrameSize) {
                                        val data = ByteArray(avFrameSize)
                                        System.arraycopy(frmData, 0, data, 0, avFrameSize)

                                        val _data = ByteArray(avFrameSize)
                                        System.arraycopy(frmData, 0, _data, 0, avFrameSize)
                                        d("nCodeId [${avFrame.codec_id}]")

                                        if (avFrame.isIFrame()) {
                                            if (lastIFrameSize < 0) {
                                                lastIFrameSize = avFrame.frmSize
                                            } else if (lastIFrameSize * 2 < avFrame.frmSize || avFrame.frmSize * 2 < lastIFrameSize) {
                                                //切换的视频分辨率
                                                if (decoderIsInit) {
                                                    try {
                                                        mVideoDecoder?.unfinalize()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    decoderIsInit = false
                                                }

                                                d("avcodec_find_decoder -----change I Frame last=${lastIFrameSize}  current=${avFrame.frmSize}")
                                            }

                                            lastIFrameSize = avFrame.frmSize
                                        }

                                        if (avFrame.codec_id.toInt() == AVFrame.MEDIA_CODEC_VIDEO_H264
                                            || avFrame.codec_id.toInt() == AVFrame.MEDIA_CODEC_VIDEO_H265
                                        ) {
                                            if (!decoderIsInit) {

                                                mVideoDecoder = VideoDecoder(
                                                    VideoDecoder.COLOR_FORMAT_BGR32,
                                                    if (avFrame.codec_id.toInt() == AVFrame.MEDIA_CODEC_VIDEO_H265) 1 else 0,
                                                    mContext?.get(),
                                                    avChannel.camera?.getCheckYCount() ?: 100
                                                )
                                                mVideoBuffer =
                                                    ByteBuffer.allocateDirect(MAX_FRAMEBUF)
                                                mVideoOutBuffer =
                                                    ByteBuffer.allocateDirect(MAX_FRAMEBUF)

                                                mYuvData = ByteArray(MAX_FRAMEBUF)
                                                mArgbData = ByteArray(MAX_FRAMEBUF)
                                                decoderIsInit = true
                                            }
                                            mVideoBuffer?.clear()
                                            if (avFrameSize < MAX_FRAMEBUF) {
                                                mVideoBuffer?.put(frmData, 0, avFrameSize)
                                            }
                                            mVideoBuffer?.flip()
                                            mVideoOutBuffer?.clear()
                                            videoDecodeResult = -1
                                            videoDecodeResult = if (withYuv) {
                                                d("yuv decode 111")
                                                mVideoDecoder?.decodeWithYUV(
                                                    mVideoBuffer,
                                                    avFrameSize,
                                                    20,
                                                    mVideoOutBuffer,
                                                    mYuvData
                                                ) ?: -1
                                            } else {
                                                mVideoDecoder?.decode(
                                                    mVideoBuffer,
                                                    avFrameSize,
                                                    20,
                                                    mVideoOutBuffer
                                                ) ?: -1
                                            }
//                                            if(videoDecodeResult == -4){
//                                                d("C_TAG videoDecodeResult == -4")
//                                                avChannel.changeQualityStopDecoderVideo(true)
//                                                isChangeVideoQuality = true
//                                            }else{
                                            out_width[0] = mVideoDecoder?.width ?: 0
                                            out_height[0] = mVideoDecoder?.height ?: 0
//                                            }
                                            out_size[0] = out_width[0] * out_height[0] * 2

                                            d("out_size[${out_size[0]}],out_width[${out_width[0]}],out_height[${out_height[0]}]videoDecodeResult=${videoDecodeResult}")

                                            if (out_size[0] > 0 && out_height[0] > 0 && out_width[0] > 0) {
                                                videoWidth = out_width[0]
                                                videoHeight = out_height[0]



                                                d("llastFrameSize[$llastFrameSize],lcurrentFrameSize[$lcurrentFrameSize],llastFrameSize[$llastFrameSize]")

                                                if (llastFrameSize == -1L && lcurrentFrameSize == llastFrameSize) {
                                                    lcurrentFrameSize = videoWidth.toLong()
                                                    d("set h264/h265 [$llastFrameSize],lc[$lcurrentFrameSize]")
                                                }
                                                llastFrameSize = videoWidth.toLong()

//                                                if (llastFrameSize != lcurrentFrameSize) {
//                                                    d("reset h264/h265 [${llastFrameSize}],lc[$lcurrentFrameSize]")
//                                                    llastFrameSize = -1
//                                                    lcurrentFrameSize = -1
//
//                                                    try {
//                                                        mVideoDecoder?.unfinalize()
//                                                    } catch (e: Exception) {
//                                                        e.printStackTrace()
//                                                    }
//
//                                                    decoderIsInit = false
//                                                }

                                                if (videoWidth > 0 && videoHeight > 0 && videoDecodeResult >= 0) {
                                                    bmp = Bitmap.createBitmap(
                                                        videoWidth,
                                                        videoHeight,
                                                        Bitmap.Config.ARGB_8888
                                                    )

                                                    if (withYuv) {
                                                        d("yuv decode 222")
                                                        LibyuvUtils.I420ToRGBA(
                                                            mYuvData,
                                                            mArgbData,
                                                            videoWidth,
                                                            videoHeight
                                                        )
                                                        val wrap = ByteBuffer.wrap(mArgbData)
                                                        wrap.rewind()
                                                        wrap.position(0)
                                                        bmp?.copyPixelsFromBuffer(wrap)
                                                        d("yuv decode 333")
                                                    } else {
                                                        if( videoWidth * videoHeight * 4 <= MAX_FRAMEBUF){
                                                            mVideoOutBuffer?.let { buffer ->
                                                                bmp?.copyPixelsFromBuffer(buffer)
                                                            }
                                                        }


                                                    }
                                                }

                                                if (firstTimeStampFromDevice != 0L
                                                    && firstTimeStampFromLocal != 0L
                                                ) {
                                                    val currentTimeMillis =
                                                        System.currentTimeMillis()

                                                    val l = currentTimeMillis - t1

                                                    sleepTime =
                                                        (firstTimeStampFromLocal + (avFrame.timestamp.toLong() - firstTimeStampFromDevice) - l)
                                                    delayTime = sleepTime * -1L

                                                    if (sleepTime >= 0) {
                                                        if (avFrame.timestamp.toLong() - lastFrameTimeStamp > 500L) {
                                                            firstTimeStampFromDevice =
                                                                avFrame.timestamp.toLong()
                                                            firstTimeStampFromLocal = l
                                                            if (sleepTime > 1000) {
                                                                sleepTime = 33
                                                            }
                                                        }

                                                        if (sleepTime > 1000) {
                                                            sleepTime = 1000
                                                        }
                                                        delay(sleepTime)
                                                    }

                                                    lastFrameTimeStamp = avFrame.timestamp.toLong()
                                                } else {
                                                    firstTimeStampFromDevice =
                                                        avFrame.timestamp.toLong()
                                                    firstTimeStampFromLocal =
                                                        System.currentTimeMillis()
                                                }

                                                avChannel?.videoFPS = (avChannel?.videoFPS ?: 0) + 1
//                                                emit(bmp)
//                                                emit(avFrame.timestamp)
                                                if (videoDecodeResult >= 0) {
                                                    emit(
                                                        DecoderVideoInfo(
                                                            bmp,
                                                            avFrame.deviceCurrentTime
                                                        )
                                                    )
                                                    avChannel.lastFrame = bmp
                                                }

                                                if (System.currentTimeMillis() - lastUpdateDispFrmPreSec > 60000) {
                                                    lastUpdateDispFrmPreSec =
                                                        System.currentTimeMillis()
                                                }
                                            }

//                                            }
//                                            if (mVideoDecoder?.isFrameReady == true) {

//                                            if (avChannel.recording && LocalRecordHelper.recording) {
//                                            if (LocalRecordHelper.recording) {
//                                                LocalRecordHelper.recordVideoFrame(
//                                                    _data,
//                                                    avFrameSize,
//                                                    avFrame.isIFrame()
//                                                )
//                                            }

                                        } else if (avFrame.codec_id.toInt() == AVFrame.MEDIA_CODEC_VIDEO_MPEG4) {
                                            if (!mpegtIsInit) {
                                                if (frmData.size >= 27) {

                                                    val w =
                                                        ((frmData[23].toInt()) and 0xF shl 9) or (frmData[24].toInt() and 0xff shl 1) or (frmData[25].toInt() and 0x80 shr 7)

                                                    val h =
                                                        (frmData[25].toInt() and 0x3f shl 7) or (frmData[26].toInt() and 0xFE shr 1)

//                                                DecMpeg4.InitDecoder(w, h)
                                                    mpegtIsInit = true
                                                }
                                            }
                                            if (mpegtIsInit) {
                                                DecMpeg4.Decode(
                                                    avFrame.frmData,
                                                    avFrameSize,
                                                    bufOut,
                                                    out_size,
                                                    out_width,
                                                    out_height
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                            mAvFrame?.frmData = null
                        }
                    }

                }

                if (decoderIsInit) {
                    try {
                        mVideoDecoder?.unfinalize()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                mVideoBuffer?.clear()
                mVideoOutBuffer?.clear()
                mVideoBuffer = null
                mVideoOutBuffer = null
                mVideoDecoder = null

                if (mpegtIsInit) {
                    DecMpeg4.UninitDecoder()
                }

                avChannel?.VideoFrameQueue?.removeAll()

            }.flowOn(Dispatchers.IO)
                .collect {
                    iavChannelStatus?.onAVChannelReceiverFrameData(
                        avChannel?.mChannel ?: -1,
                        it.bitmap
                    )
                    iavChannelStatus?.onAVChannelReceiverFrameData(
                        avChannel?.mChannel ?: -1,
                        it.bitmap,
                        it.time
                    )
                }
        }


    }

    fun stop() {
        isRunning = false
    }

    fun setChangeVideoQuality(changeVideoQuality: Boolean) {
        Liotc.d("AvChannel", "changeQualityStopDecoderVideo: decode ")
        isChangeVideoQuality = changeVideoQuality
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

internal class DecoderVideoInfo(val bitmap: Bitmap?, val time: Long)

/**录像帮助类*/
internal object LocalRecordHelper {


    private val TAG = "LocalRecordHelper"
    private val mLocalRecord: LocalRecording by lazy { LocalRecording() }

    private var mContext: WeakReference<Context?>? = null
    private var mAvChannel: WeakReference<AVChannel?>? = null

    private var fileName: String? = null

    //仅模块内可见的属性
    internal var recording = false

    private var startRecording = false

    //是否可以录像了
    internal var canRecording = false

    private var mRecordJob: Job? = null

    private var runJob: Job? = null
    private var mRecordTime = 0


    @Synchronized
    fun startRecord(
        context: Context?,
        name: String?,
        width: Int,
        height: Int,
        avChannel: AVChannel?,
        onResultCallback: OnResultCallback<RecordStatus>?
    ) {
        if (context == null) {
            d(TAG, "don't record because context is null")
            onResultCallback?.onResult(RecordStatus.CONTEXT_NULL)
            return
        }

        if (name.isNullOrEmpty()) {
            d(TAG, "don't record because context file is null [$name]")
            onResultCallback?.onResult(RecordStatus.FILE_NAME_ILLEGAL)
            return
        }

        if (avChannel == null || (avChannel.codeId != AVFrame.MEDIA_CODEC_VIDEO_H265
                    && avChannel.codeId != AVFrame.MEDIA_CODEC_VIDEO_H264
                    && avChannel.codeId != AVFrame.MEDIA_CODEC_VIDEO_MJPEG
                    && avChannel.codeId != AVFrame.MEDIA_CODEC_VIDEO_MPEG4)
        ) {
            d(TAG, "don't record because avChannel is null [${avChannel?.codeId}]")
            onResultCallback?.onResult(RecordStatus.VIDEO_CODEC_NULL)
            return
        }


        fileName = name
        mContext = WeakReference(context)
        mAvChannel = WeakReference(avChannel)

        startRecord(
            if (width == 0) 1280 else width,
            if (height == 0) 720 else height,
            onResultCallback
        )
    }

    private fun startRecord(
        width: Int,
        height: Int,
        onResultCallback: OnResultCallback<RecordStatus>?
    ) {
        if (recording) {
            d(TAG, "have a video recording")
            onResultCallback?.onResult(RecordStatus.HAD_RECORDING)
            return
        }
        d(TAG, "startRecord [$width，$height]")
        val codeId = when (mAvChannel?.get()?.codeId) {
            AVFrame.MEDIA_CODEC_VIDEO_H264 -> AVFrame.MEDIA_CODEC_VIDEO_H264
            AVFrame.MEDIA_CODEC_VIDEO_H265 -> AVFrame.MEDIA_CODEC_VIDEO_H265
            AVFrame.MEDIA_CODEC_VIDEO_MJPEG -> AVFrame.MEDIA_CODEC_VIDEO_MJPEG
            AVFrame.MEDIA_CODEC_VIDEO_MPEG4 -> AVFrame.MEDIA_CODEC_VIDEO_MPEG4
            else -> null
        }

        if (codeId == null) {
            onResultCallback?.onResult(RecordStatus.VIDEO_CODEC_NULL)
            return
        }

        if (fileName.isNullOrEmpty()) {
            onResultCallback?.onResult(RecordStatus.FILE_NAME_ILLEGAL)
            return
        }

//            delay(200L)
        mRecordJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                recording = true
//                if ((mAvChannel?.get()?.SID ?: -1) > 0) {
//                    mAvChannel?.get()?.IOCtrlQueue?.Enqueue(
//                        AVIOCTRLDEFs.IOTYPE_USER_IPCAM_START,
//                        Packet.intToByteArray_Little(0)
//                    )
//                }
                //开启录像要开启音频接收
                mAvChannel?.get()?.let { avChannel ->
                    avChannel.setAudioTrackStatus(mContext?.get(), avChannel.audioPlayStatus, true)
                }
//                delay(500)
                var delaycount = 0
//                while (!canRecording) {
//                    delay(5)
//                    delaycount++
//                    if (delaycount > 700) {
//                        break
//                    }
//                }

                if (recording && isActive) {
                    d(TAG, "---startRecord width[$width],height[$height] 1")
                    mLocalRecord.setRecorderVideoTrack(width, height)
                    d(TAG, "---startRecord width[$width],height[$height] 2,codeId[$codeId]")
                    mLocalRecord.startRecording(codeId, fileName, true)
                    d(TAG, "---startRecord width[$width],height[$height],[$codeId],[$fileName] 3")

                    d(TAG,"recrddddddddd *****")
                    startRecording = true
                    mRecordTime = 0
                    emit(1)
                }

            }.flowOn(Dispatchers.IO)
                .collect {
                    onResultCallback?.onResult(RecordStatus.RECORDING)
                    mAvChannel?.get()?.iavChannelStatus?.onAVChannelRecordStatus(
                        RecordStatus.RECORD_START,
                        fileName, 0
                    )
                    startRecordTime()
                }
        }


    }

    private fun startRecordTime() {
        if (runJob?.isActive != true) {
            runJob?.cancel()
            runJob = null
            runJob = GlobalScope.launch(Dispatchers.Main) {
                flow {
                    while (recording && isActive) {
                        emit(1)
                        delay(1000L)
                    }
                }.flowOn(Dispatchers.IO)
                    .collect {
                        mAvChannel?.get()?.iavChannelStatus?.onAVChannelRecordStatus(
                            RecordStatus.RECORDING,
                            fileName, mRecordTime++
                        )
                    }
            }
        }
    }


    internal fun setParseBuffer(data: ByteArray?) {
        val avchannel = mAvChannel?.get()
//        if (avchannel == null || !avchannel.recording || !recording) {
        if (avchannel == null || !recording) {
            return
        }

        if (data == null) {
            return
        }
        d(TAG, "startRecord setParseBuffer")
        if (recording) {
            mLocalRecord.setParserBuffer(data)
        }
    }

    internal fun recordVideoFrame(data: ByteArray?, size: Int, isIFrame: Boolean) {
        if (data == null) {
            return
        }
        d(TAG, "startRecord recordVideoFrame [$size],[$isIFrame],[$startRecording]")
        if (recording) {
            mLocalRecord.recordVideoFrame(data, size, isIFrame)
        }
    }

    internal fun setAudioEnvironment(samplerate: Int, channel: Int, databits: Int) {
        d(
            TAG,
            "startRecord setAudioEnvironment[$samplerate],channel[$channel]，databits[$databits],[$recording] "
        )
//        if (recording) {
            mLocalRecord.setAudioEnvironment(samplerate, channel, databits)
//        }
    }

    internal fun recodeAudioFrame(data: ByteArray?, size: Int, length: Int) {
        if (data == null || !recording || data.size < size) {
            return
        }
        d(
            TAG,
            "startRecord recodeAudioFrame[$size],channel[$length],[$recording],[$startRecording] "
        )
        if (recording) {
            mLocalRecord.recodeAudioFrame(data, size, length)
        }
    }

    internal fun stopRecord() {
        d(TAG, "stopRecord 111")

        canRecording = false

        mRecordJob?.cancel()
        mRecordJob = null

        recording = false
        startRecording = false
        mLocalRecord.stopRecording()
        runJob?.cancel()
        runJob = null
        d(TAG, "stopRecord 222")

        //开启录像要开启音频接收
        mAvChannel?.get()?.let { avChannel ->
            avChannel.setAudioTrackStatus(mContext?.get(), avChannel.audioPlayStatus, false)
        }

        mAvChannel?.get()?.iavChannelStatus?.onAVChannelRecordStatus(
            RecordStatus.RECORD_STOP,
            fileName,
            mRecordTime
        )
        d(TAG, "stopRecord 333")
        mRecordTime = 0
        mContext?.clear()
        mAvChannel?.clear()
        fileName = null
        d(TAG, "stopRecord")
    }

    private fun d(tag: String, msg: String) {
        Liotc.d(tag, msg)
    }

    private fun i(tag: String, msg: String) {
        Liotc.i(tag, msg)
    }

    private fun e(tag: String, msg: String) {
        Liotc.e(tag, msg)
    }

}

/**
 *视频分辨率
 * 有些设备是没有FHD清晰度的，所以FHD代表DH，SMOOTH还是SMOOTH，其他的代表SD
 * FHD不管是在支持FHD的设备，还是在不支持FHD的设备上，代表的都是最搞得清晰度；SMOOTH也一样.
 */
enum class VideoQuality(val value: Int) {
    FHD(1), HD(2), SD(3), SSD(4), SMOOTH(5)
}

