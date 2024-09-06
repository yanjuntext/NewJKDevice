package com.tutk.IOTC.camera

import android.content.Context
import android.media.*
import android.util.Log
import androidx.annotation.RequiresPermission
import com.decoder.util.G711Code
import com.tutk.IOTC.*
import com.tutk.IOTC.camera.*
import com.tutk.IOTC.status.VoiceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.ref.WeakReference

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description: 音频处理类
 */

/**音频接收*/
class RecvAudioJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "RecvAudioJob"
    private var runJob: Job? = null

    private var mPlayMode: Int = Camera.AUDIORECORD_LIVEMODE


    private var mContext: WeakReference<Context?>? = null


    //上次语音格式
    private var mLastVoiceType: VoiceType? = null

    private var soundStatus: Boolean = false


    private fun isSupportAudio(codeId: Int) =
        codeId == AVFrame.MEDIA_CODEC_AUDIO_MP3
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_MP3
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_SPEEX
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_ADPCM
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_PCM
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_G726
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_G711A
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_AAC_RAW
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_AAC_LATM
                || codeId == AVFrame.MEDIA_CODEC_AUDIO_AAC_ADTS

    private fun isTwoWayVoiceType() = avChannel?.mVoiceType == VoiceType.TWO_WAY_VOICE

    /**
     * @param playMode 播放类型  直播和回放
     * */
    fun start(
        context: Context?,
        playMode: Int = Camera.AUDIORECORD_LIVEMODE,
    ) {

        mContext = WeakReference(context)
        if (isRunning) {
            d("RecvAudioJob is Running,not rerun")
            return
        }
        d("start  1 index=${avChannel?.mAvIndex}")
        if (isTwoWayVoiceType() && avChannel?.mAudioCodec == AVFrame.MEDIA_CODEC_AUDIO_PCM) {
            //只有君正的才需要关闭通话
            d("unInitAudioTrack  111")
            AudioTrackHelper.unInitAudioTrack()
        } else {
            d("zrtAudioPlayer unInitAudioTrack")
            avChannel?.unInitAudioPlayer()
            avChannel?.releaseAcousticEchoCanceler()
        }
        d("start  2")
        var nReadSize = 0
        isRunning = true
        mPlayMode = playMode

        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow<Int> {
                avChannel?.refreshSid()
                while (isRunning && isActive && ((avChannel?.SID
                        ?: -1) < 0 || (avChannel?.mAvIndex
                        ?: -1) < 0 || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    avChannel?.refreshSid()
                    delay(100)
                }
                if (!isActive) {
                    return@flow
                }
                avChannel?.audioBPS = 0
                val recvBuf = ByteArray(MAX_AUDIO_BUF_SIZE)
                val bytAvFrame = ByteArray(AVFrame.FRAMEINFO_SIZE)
                val pFrmNo = IntArray(1)


                var mFirst = true
                var mInitAudio = false

                var mSamplerate = 44100
                var mDatabits = 1
                var mChannel = 1
                var mCodecId = 0
                var mFPS = 0

                val decodeOutPutBuffer = ByteArray(65535)

                avChannel?.refreshSid()
                if (isRunning && isActive && (avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex
                        ?: -1) >= 0
                ) {
                    avChannel?.refreshSid()
                    d("IOTYPE_USER_IPCAM_AUDIOSTART [${avChannel?.mAvIndex}]")
                    avChannel?.IOCtrlQueue?.Enqueue(
                        avChannel.mAvIndex,
                        AVIOCTRLDEFs.IOTYPE_USER_IPCAM_AUDIOSTART,
                        0.littleByteArray()
//                        Packet.intToByteArray_Little(0)
                    )
                }
                avChannel?.refreshSid()

                var cachePcmAudiBuffer = ByteArray(1280)
                var cachePcmAudiSize = 0
                var lastPcmTime = 0L
//                while (isRunning && isActive && (avChannel?.audioPlayStatus == true || LocalRecordHelper.recording)) {
                while (isRunning && isActive && (avChannel?.isAudioPlaying() == true || LocalRecordHelper.recording)) {
//                    while ((avChannel?.audioPlayStatus == true || LocalRecordHelper.recording) && isActive) {
                    while ((avChannel?.isAudioPlaying() == true || LocalRecordHelper.recording) && isActive && isRunning) {
                        avChannel?.refreshSid()
                        if ((avChannel?.SID ?: -1) >= 0 && (avChannel?.mAvIndex ?: -1) >= 0) {
                            d("get audio [${avChannel?.mAvIndex}]")

                            if (avChannel?.audioPlayStatus == true) {
                                iavChannelStatus?.onListenerStatus(true)
                            } else {
                                iavChannelStatus?.onListenerStatus(false)
                            }

                            nReadSize = AVAPIs.avRecvAudioData(
                                avChannel?.mAvIndex ?: -1,
                                recvBuf,
                                recvBuf.size,
                                bytAvFrame,
                                AVFrame.FRAMEINFO_SIZE,
                                pFrmNo
                            )
                            d("nReadSize [$nReadSize]")
                            when {
                                nReadSize > 0 -> {
                                    avChannel?.audioBPS = (avChannel?.audioBPS ?: 0) + nReadSize

                                    val frameData = ByteArray(nReadSize)
                                    System.arraycopy(recvBuf, 0, frameData, 0, nReadSize)

                                    val frame = AVFrame(
                                        pFrmNo[0].toLong(),
                                        AVFrame.FRM_STATE_COMPLETE,
                                        bytAvFrame,
                                        frameData,
                                        nReadSize,
                                        mPlayMode
                                    )
                                    mCodecId = frame.codec_id.toInt()

                                    d("nCodecId[$mCodecId]")

//                                    if (isTwoWayVoiceType() && mPlayMode != Camera.AUDIORECORD_PLAYBACKMODE) {
//                                        mCodecId = AVFrame.MEDIA_CODEC_AUDIO_PCM
//                                    }

                                    if ((mLastVoiceType != avChannel?.mVoiceType && mLastVoiceType != null)
                                        || ((avChannel?.mVoiceType == VoiceType.ONE_WAY_VOICE || (avChannel?.mVoiceType == VoiceType.TWO_WAY_VOICE && mCodecId != AVFrame.MEDIA_CODEC_AUDIO_PCM))
                                                && AudioTrackHelper.audioTrackIsEmpty() && avChannel.audioPlayStatus)
                                        || (avChannel?.mVoiceType == VoiceType.TWO_WAY_VOICE
                                                && mCodecId == AVFrame.MEDIA_CODEC_AUDIO_PCM
                                                && avChannel.audioPlayerIsEmpty()
                                                && avChannel.audioPlayStatus)
                                    ) {
                                        mFirst = true
                                        mInitAudio = false
                                        d("chang voiceType 1")
                                        //释放双向语音资源
                                        d("zrtAudioPlayer unInitAudioTrack 2222")
                                        avChannel?.unInitAudioPlayer()
                                        d("chang voiceType 2")
                                        //释放当向语音播放资源
                                        d("unInitAudioTrack  222")
                                        AudioTrackHelper.unInitAudioTrack()
                                        d("chang voiceType 3")
                                        mLastVoiceType = avChannel?.mVoiceType
                                        d("chang voiceType 4")
                                    }

                                    if (mFirst) {
                                        if (!mInitAudio && isSupportAudio(mCodecId)) {

                                            mSamplerate = AVFrame.getSamplerate(frame.flags)
                                            mDatabits = frame.flags.toInt() and 0x02
                                            mDatabits = if (mDatabits == 0x02) 1 else 0
                                            mChannel = frame.flags.toInt() and 0x01

                                            val mono =
                                                if (mChannel == AVFrame.AUDIO_CHANNEL_MONO) 1 else 2

                                            val databits =
                                                if (mDatabits == AVFrame.AUDIO_DATABITS_8) 8 else 16

                                            val _fps = mSamplerate * mono * databits / 8

                                            when (mCodecId) {
                                                AVFrame.MEDIA_CODEC_AUDIO_SPEEX -> mFPS = _fps / 160
                                                AVFrame.MEDIA_CODEC_AUDIO_ADPCM -> mFPS = _fps / 640
                                                AVFrame.MEDIA_CODEC_AUDIO_PCM -> mFPS =
                                                    _fps / frame.frmSize

                                                AVFrame.MEDIA_CODEC_AUDIO_G711A -> mFPS = _fps / 320
                                            }
                                            d("first localRecord")
                                            LocalRecordHelper.setAudioEnvironment(
                                                mSamplerate,
                                                if (mChannel == 0) 1 else 2,
                                                if (mDatabits == 0) 8 else 16
                                            )

                                            d("first AudioProcessHelper")
                                            AudioProcessHelper.initDecode(
                                                if (isTwoWayVoiceType() && mPlayMode == Camera.AUDIORECORD_PLAYBACKMODE) 134 else mCodecId,
                                                mSamplerate, mDatabits, mChannel
                                            )
                                            cachePcmAudiSize = 0
                                            //初始化单向语音播放器
                                            val initAudioTrack = if (!isTwoWayVoiceType()) {
                                                d("first AudioTrackHelper")
                                                AudioTrackHelper.initAudioTrack(
                                                    mSamplerate,
                                                    mChannel,
                                                    mDatabits,
                                                    mCodecId
                                                )
                                            } else {
                                                d("first initAudioPlayer")
                                                if (mCodecId == AVFrame.MEDIA_CODEC_AUDIO_PCM) {
                                                    avChannel?.initAudioPlayer(mContext?.get())
                                                        ?: false
                                                } else {
                                                    val flag = avChannel?.initAcousticEchoCanceler()
                                                        ?: false
                                                    AudioTrackHelper.initAudioTrack(
                                                        mSamplerate,
                                                        mChannel,
                                                        mDatabits,
                                                        mCodecId
                                                    )
                                                    flag
                                                }
                                            }
                                            d("first initAudioPlayer result=$initAudioTrack")
                                            if (!initAudioTrack) {
                                                break
                                            }
                                            d("first over")
                                            mInitAudio = true
                                            mFirst = false
                                        }
                                    }
                                    if (isTwoWayVoiceType()) {
                                        if (mCodecId == AVFrame.MEDIA_CODEC_AUDIO_PCM) {
                                            //PCM 双向语音使用的是君正
                                            if (avChannel?.audioPlayStatus == true) {
                                                if (avChannel.mAudioPlayer?.isSoundOn != true) {
                                                    d("sound on")
                                                    avChannel.mAudioPlayer?.soundOn()
                                                }
                                            } else {
                                                if (avChannel?.mAudioPlayer?.isSoundOn == true) {
                                                    d("sound off")
                                                    avChannel.mAudioPlayer?.soundOff()
                                                }
                                            }
                                        }

                                    }
                                    when (mCodecId) {
                                        AVFrame.MEDIA_CODEC_AUDIO_PCM -> {
                                            if (nReadSize > 0) {
                                                val isNeedNewCacheBuffer =
                                                    cachePcmAudiBuffer.size < nReadSize
                                                val values = ByteArray(nReadSize)
                                                System.arraycopy(recvBuf, 0, values, 0, nReadSize)

                                                if (avChannel?.audioPlayStatus == true) {
                                                    if (isTwoWayVoiceType()) {
                                                        d("putPlayData isPcmAudioRecvSize800=${avChannel.camera?.isPcmAudioRecvSize800}")

                                                        if (avChannel.camera?.isPcmAudioRecvSize800 == true) {
                                                            //PCM 格式音频强制使用800

                                                            var playTotalSize = nReadSize + cachePcmAudiSize
                                                            var start = 0
                                                            if(playTotalSize < 800){
                                                                System.arraycopy(values,0,cachePcmAudiBuffer,cachePcmAudiSize,nReadSize)
                                                                cachePcmAudiSize+=nReadSize
                                                            }else{
                                                                while (playTotalSize >= 800){
                                                                    val pcmValue = ByteArray(800)
                                                                    var total = 0
                                                                    if(cachePcmAudiSize != 0){
                                                                        System.arraycopy(cachePcmAudiBuffer,0,pcmValue,0,cachePcmAudiSize)
                                                                    }
                                                                    total+=cachePcmAudiSize
                                                                    val offset = 800 - cachePcmAudiSize

                                                                    if(start + offset < nReadSize){
                                                                        System.arraycopy(values,start,pcmValue,cachePcmAudiSize,offset)
                                                                        start+=offset
                                                                        total+=offset
                                                                    }else{
                                                                        val length = nReadSize-start
                                                                        System.arraycopy(values,start,pcmValue,cachePcmAudiSize,length)
                                                                        start+=length
                                                                        total+=length
                                                                    }
                                                                    d("putPlayData total=$total start=$start lastPcmTime=$lastPcmTime c=${frame.timestamp.toLong()} o=${frame.timestamp.toLong()-lastPcmTime}")
                                                                    avChannel?.putPlayData(
                                                                        pcmValue,
                                                                        total,
                                                                        frame.timestamp.toLong()
                                                                    )
                                                                    if(cachePcmAudiSize != 0) cachePcmAudiSize = 0
                                                                    playTotalSize-=total
                                                                    if(playTotalSize < 800){
                                                                        lastPcmTime = frame.timestamp.toLong()
                                                                        System.arraycopy(values,start,cachePcmAudiBuffer,0,playTotalSize)
                                                                        cachePcmAudiSize = playTotalSize
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            avChannel?.putPlayData(
                                                                values,
                                                                nReadSize,
                                                                frame.timestamp.toLong()
                                                            )
                                                        }


                                                    } else {
                                                        AudioTrackHelper.playAudio(
                                                            values,
                                                            0,
                                                            nReadSize
                                                        )
                                                    }
                                                }
                                                //录像 录制音频
//                                                if (avChannel?.recording) {
                                                if (LocalRecordHelper.recording) {
                                                    val length =
                                                        nReadSize / ((if (mDatabits == 0) 8 else 16) * (mSamplerate / 8000))
                                                    LocalRecordHelper.recodeAudioFrame(
                                                        values,
                                                        nReadSize,
                                                        length
                                                    )
                                                }

                                            }
                                        }

                                        AVFrame.MEDIA_CODEC_AUDIO_G711A -> {
                                            AudioProcessHelper.decode(
                                                recvBuf,
                                                nReadSize,
                                                decodeOutPutBuffer
                                            )?.let { decode ->
                                                d("decode size[$decode]，[${avChannel?.audioPlayStatus}]")
                                                if (decode > 0) {
                                                    //录像 录制音频
//                                                    if (avChannel?.recording == true) {
                                                    if (LocalRecordHelper.recording) {
                                                        val length =
                                                            decode / ((if (mDatabits == 0) 8 else 16) * (mSamplerate / 8000))
                                                        LocalRecordHelper.recodeAudioFrame(
                                                            decodeOutPutBuffer,
                                                            decode,
                                                            length
                                                        )
                                                    }
                                                    if (avChannel?.audioPlayStatus == true) {
                                                        if (isTwoWayVoiceType()) {
                                                            avChannel.captureAcousticEchoCanceler(
                                                                decodeOutPutBuffer,
                                                                decode
                                                            )
                                                        }
                                                        AudioTrackHelper.playAudio(
                                                            decodeOutPutBuffer,
                                                            0,
                                                            decode
                                                        )
                                                    }
                                                }

                                            }
                                        }

                                        AVFrame.MEDIA_CODEC_AUDIO_AAC_ADTS,
                                        AVFrame.MEDIA_CODEC_AUDIO_AAC_LATM,
                                        AVFrame.MEDIA_CODEC_AUDIO_AAC_RAW -> {
                                            AudioProcessHelper.decode(
                                                recvBuf,
                                                nReadSize,
                                                decodeOutPutBuffer
                                            )?.let { decode ->
                                                if (decode > 0) {
                                                    //录像 录制音频
//                                                    if (avChannel?.recording == true) {
                                                    if (LocalRecordHelper.recording) {
                                                        val length =
                                                            decode / ((if (mDatabits == 0) 8 else 16) * (mSamplerate / 8000))
                                                        LocalRecordHelper.recodeAudioFrame(
                                                            decodeOutPutBuffer,
                                                            decode,
                                                            length
                                                        )
                                                    }
                                                    if (avChannel?.audioPlayStatus == true) {
                                                        if (isTwoWayVoiceType()) {
                                                            avChannel.captureAcousticEchoCanceler(
                                                                decodeOutPutBuffer,
                                                                decode
                                                            )

                                                        }
                                                        AudioTrackHelper.playAudio(
                                                            decodeOutPutBuffer,
                                                            0,
                                                            decode
                                                        )

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    delay(
                                        if (mFPS == 0) 33L else (1000 / mFPS).toLong()
                                    )
                                }
                            }
                        }
                    }
                    if (avChannel?.mAudioPlayer?.isSoundOn == true) {
                        avChannel?.mAudioPlayer?.soundOff()
                    }
                }
                d("stop 1")
                avChannel?.mAvIndex?.let { index ->

//                    if(isTwoWayVoiceType() &&  avChannel?.audioRecordStatus != true){
//
//                    }else{
                    d("avClientCleanAudioBuf [$index]")
                    AVAPIs.avClientCleanAudioBuf(index)
                    avChannel.IOCtrlQueue?.Enqueue(
                        index,
                        AVIOCTRLDEFs.IOTYPE_USER_IPCAM_AUDIOSTOP,
                        0.littleByteArray()
//                        Packet.intToByteArray_Little(0)
                    )
                    d("IOTYPE_USER_IPCAM_AUDIOSTOP [$index]")
//                    }

                }
                d("stop 2 ")
                iavChannelStatus?.onListenerStatus(false)
                //关闭双向语音
                avChannel?.mAudioPlayer?.soundOff()
                if (isTwoWayVoiceType() && avChannel?.audioRecordStatus != true) {
                    d("zrtAudioPlayer unInitAudioTrack  333")
                    //释放双向语音资源
                    avChannel?.unInitAudioPlayer()
                }


                d("stop 3")
                //释放单向语音播放资源
                d("unInitAudioTrack  333  $isRunning,${isActive},${(avChannel?.audioPlayStatus == true || LocalRecordHelper.recording)},${LocalRecordHelper.recording},${avChannel?.audioPlayStatus == true}")
                AudioTrackHelper.unInitAudioTrack()

                d("stop 4 ${avChannel?.mChannel}")
                //释放解码器
                AudioProcessHelper.unDecode()
                d("stop 5 ${avChannel?.mChannel}")
                isRunning = false
            }.flowOn(Dispatchers.IO)
                .collect {

                }
        }

    }


    fun stop() {
        isRunning = false
        iavChannelStatus?.onListenerStatus(false)
    }

    fun setTwoWayVoiceStatus(status: Boolean) {
        soundStatus = status
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

internal data class SendAudioEmitInfo(val type: Int? = null, var volume: Double = 0.0)

/**音频发送*/
class SendAudioJob(
    val avChannel: AVChannel?,
    var isRunning: Boolean = false,
    var iavChannelStatus: IAVChannelListener?
) {
    private val TAG = "SendAudioJob"
    private var runJob: Job? = null
    private var mContext: WeakReference<Context?>? = null

    private var mLastVoiceType: VoiceType? = null

    private fun isTwoWayVoiceType() = avChannel?.mVoiceType == VoiceType.TWO_WAY_VOICE

    //chIndexForSendAudio
    private var mSendAudioSessionIndex = -1

    //avIndexForSendAudio
    private var mSendAudioChannelIndex = -1


    private fun getAudioInfo(
        codec_id: Short,
        flags: Byte,
        cam_index: Byte = 0,
        online_num: Byte = 0,
        timestamp: Int = System.currentTimeMillis().toInt()
    ): ByteArray {
        val result = ByteArray(16)
        val codec: ByteArray = Packet.shortToByteArray_little(codec_id)
        System.arraycopy(codec, 0, result, 0, 2)
        result[2] = flags
        result[3] = cam_index
        result[4] = online_num
        val time = Packet.intToByteArray_Little(timestamp)
        System.arraycopy(time, 0, result, 12, 4)
        return result
    }

    private fun startAudioInfo(index: Int): ByteArray {
        val data = ByteArray(8)
        val ch = index.littleByteArray()
        System.arraycopy(ch, 0, data, 0, 4)
        return data
    }


    private fun avServStart2(): Int {
        d("avServStart2 SID[${avChannel?.SID}],[$mSendAudioSessionIndex]")
//        val inConfig = St_AVServStartInConfig().apply {
//            iotc_session_id = avChannel?.SID?:-1
//            iotc_channel_id = mSendAudioSessionIndex
//            timeout_sec = 60
//            server_type = 65535
//        }
//        val outConfig = St_AVServStartOutConfig()
//        AVAPIs.avServStartEx(inConfig,outConfig)
        mSendAudioChannelIndex =
            AVAPIs.avServStart2(avChannel?.SID ?: -1, null, null, 60, 65535, mSendAudioSessionIndex)
        d("avServStart2 [$mSendAudioChannelIndex],SID[${avChannel?.SID}]")
        return mSendAudioChannelIndex
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun start(context: Context?) {
        mContext = WeakReference(context)
        if (isRunning) {
            d("SendAudioJob is Running,not rerun")
            return
        }
        runJob?.cancel()
        runJob = null
        isRunning = true

        if (isTwoWayVoiceType() && avChannel?.mAudioCodec == AVFrame.MEDIA_CODEC_AUDIO_PCM) {
            d("unInitAudioTrack  444")
            AudioTrackHelper.unInitAudioTrack()
        } else {
            d("zrtAudioPlayer unInitAudioTrack  4444")
            avChannel?.unInitAudioPlayer()
            avChannel?.releaseAcousticEchoCanceler()
        }
        val sid = avChannel?.SID ?: -1
        if (sid < 0) {
            isRunning = false
            return
        }
        runJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                avChannel?.refreshSid()
                while (isRunning && isActive
                    && ((avChannel?.SID ?: -1) < 0
                            || (avChannel?.mAvIndex ?: -1) < 0
                            || avChannel?.SID == IOTC_CONNECT_ING)
                ) {
                    avChannel?.refreshSid()
                    delay(100)
                }

                while (isRunning && isActive) {
                    avChannel?.refreshSid()
                    d("send audio session sid[${avChannel?.SID}]")
                    //获取空闲的信道
                    mSendAudioSessionIndex =
                        IOTCAPIs.IOTC_Session_Get_Free_Channel(avChannel?.SID ?: -1)

                    if (mSendAudioSessionIndex < 0) {
                        d("send audio session error[$mSendAudioSessionIndex],sid[${avChannel?.SID}]")
                        delay(200)
                    } else {
                        d("send audio session sid[${avChannel?.SID}]  [$mSendAudioSessionIndex]-------------------")
                        break
                    }

                }
                ensureActive()
                d("11111")
                if (!isActive) {
                    return@flow
                }
                d("22222")
                avChannel?.IOCtrlQueue?.Enqueue(
                    AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTART,
                    startAudioInfo(mSendAudioSessionIndex)
//                    AVIOCTRLDEFs.SMsgAVIoctrlAVStream.parseContent(mSendAudioSessionIndex)
                )
                d("start avServerStart[${avChannel?.SID}],[${mSendAudioSessionIndex}]")
                while (isRunning && isActive && avServStart2() < 0) {
                    if (!isRunning || !isActive) {
                        break
                    }
                    delay(20L)
                    if (!isRunning || !isActive) {
                        break
                    }
                    d("mSendAudioChannelIndex [$mSendAudioChannelIndex]")
                    if (mSendAudioChannelIndex == AVAPIs.AV_ER_IOTC_CHANNEL_IN_USED
                        || mSendAudioChannelIndex != AVAPIs.AV_ER_SERVER_EXIT
                    ) {

                        avChannel?.IOCtrlQueue?.Enqueue(
                            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTOP,
                            startAudioInfo(mSendAudioSessionIndex)
//                            AVIOCTRLDEFs.SMsgAVIoctrlAVStream.parseContent(mSendAudioSessionIndex)
                        )

                        if (mSendAudioChannelIndex != AVAPIs.AV_ER_SERVER_EXIT) {
                            avChannel?.let { avChannel ->
                                AVAPIs.avServExit(avChannel.SID, mSendAudioSessionIndex)
                            }
                        }
                        delay(200)
                        if (!isRunning || !isActive) {
                            break
                        }
                        val max = 10
                        val min = 2
                        val ran2 = (Math.random() * (max - min) + min).toInt()
                        mSendAudioSessionIndex = ran2

                        avChannel?.IOCtrlQueue?.Enqueue(
                            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTART,
                            startAudioInfo(mSendAudioSessionIndex)
//                            AVIOCTRLDEFs.SMsgAVIoctrlAVStream.parseContent(mSendAudioSessionIndex)
                        )
                    }
                }
                if (isActive && isRunning) {
                    if (isRunning && isActive) {
                        d("emit 1")
                        emit(SendAudioEmitInfo(type = 1))
                    }
                    //初始话编码器
                    avChannel?.mAudioCodec?.let { codeId ->
                        AudioProcessHelper.initEncode(codeId, 8000, 1, 0)
                    }

                    val pcmBuf = ByteArray(800)
                    val inG711Buf = ShortArray(320)
                    val inG711BufByte = ByteArray(640)

                    val outG711Buf = ByteArray(2048)


                    val flag =
                        (AVFrame.AUDIO_SAMPLE_8K shl 2) or (AVFrame.AUDIO_DATABITS_16 shl 1) or AVFrame.AUDIO_CHANNEL_MONO
                    var lastEmptyTime = 0L
                    while (isRunning && isActive && mSendAudioChannelIndex >= 0) {

                        while (avChannel?.audioRecordStatus == true && isActive) {
                            //切换了语音类型 例如：单向语音变双向语音 或者 双向语音变单向语音
                            if (mLastVoiceType != avChannel?.mVoiceType && mLastVoiceType != null) {
                                //释放双向语音资源
                                d("zrtAudioPlayer unInitAudioTrack  000")
                                avChannel?.unInitAudioPlayer()
                                d("unInitAudioTrack  000")
                                //释放当向语音播放资源
                                AudioTrackHelper.unInitAudioTrack()
                                AudioTrackHelper.unInitAudioRecord()
                                mLastVoiceType = avChannel?.mVoiceType
                            }
                            if (mLastVoiceType == null) {
                                mLastVoiceType = avChannel?.mVoiceType
                            }

                            if (avChannel.mVoiceType == VoiceType.TWO_WAY_VOICE
                                && avChannel.mAudioCodec == AVFrame.MEDIA_CODEC_AUDIO_PCM
                            ) {
                                avChannel.initAudioPlayer(mContext?.get())
//                                d("TWO_WAY_VOICE isAudioRecord[${avChannel.mAudioPlayer?.isAudioRecord}],codec[${avChannel.mAudioCodec}]")

                                if (avChannel.audioRecordStatus && avChannel.mAudioPlayer?.isAudioRecord != true) {
                                    d("zrtAudioPlayer audioplayer status= resumeAudioRecord")
                                    avChannel.mAudioPlayer?.resumeAudioRecord()
                                } else if (!avChannel.audioRecordStatus) {

                                }


                                if (!avChannel.audioRecordStatus) {
                                    //当关掉录音的时候，需要发送空数据，否则新的设备5S后会主动断开通话通道
                                    val frameInfo = getAudioInfo(
                                        AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
                                        flag.toByte()
                                    )
                                    val result = AVAPIs.avSendAudioData(
                                        mSendAudioChannelIndex, ByteArray(800),
                                        800, frameInfo, 16
                                    )
                                } else {
                                    //如果是双向语音
                                    val filterData = avChannel.mAudioPlayer?.filterData
                                    emit(SendAudioEmitInfo(volume = avChannel.audioRecordVolume))
                                    filterData?.let { takeAll ->
                                        d("audioplayer status=${avChannel.mAudioPlayer?.isAudioRecord}  data=${takeAll.size}")
                                        when (avChannel.mAudioCodec) {
                                            AVFrame.MEDIA_CODEC_AUDIO_G711A -> {
                                                val length =
                                                    AudioProcessHelper.encode(takeAll, outG711Buf)

                                                val frameInfo = getAudioInfo(
                                                    AVFrame.MEDIA_CODEC_AUDIO_G711A.toShort(),
                                                    flag.toByte()
                                                )

//                                                AVIOCTRLDEFs.SFrameInfo.parseContent(
//                                                AVFrame.MEDIA_CODEC_AUDIO_G711A.toShort(),
//                                                flag.toByte(),
//                                                0,
//                                                0,
//                                                System.currentTimeMillis().toInt()
//                                            )

                                                AVAPIs.avSendAudioData(
                                                    mSendAudioChannelIndex,
                                                    outG711Buf,
                                                    length,
                                                    frameInfo,
                                                    16
                                                )
                                            }

                                            else -> {
                                                val frameInfo = getAudioInfo(
                                                    AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
                                                    flag.toByte()
                                                )
//                                                AVIOCTRLDEFs.SFrameInfo.parseContent(
//                                                AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
//                                                flag.toByte(),
//                                                0,
//                                                0,
//                                                System.currentTimeMillis().toInt()
//                                            )

//                                            AVAPIs.avSendAudioData(mSendAudioChannelIndex,ByteArray(800),800,frameInfo,16)
                                                val result = AVAPIs.avSendAudioData(
                                                    mSendAudioChannelIndex, takeAll,
                                                    takeAll.size, frameInfo, 16
                                                )
                                                d("audioplayer status=${avChannel.mAudioPlayer?.isAudioRecord}  data=${takeAll.size} avSendAudioData=${result}")
                                            }
                                        }
                                    }
                                }

                            } else if (avChannel.mVoiceType == VoiceType.ONE_WAY_VOICE
                                || (avChannel.mVoiceType == VoiceType.TWO_WAY_VOICE
                                        && avChannel.mAudioCodec != AVFrame.MEDIA_CODEC_AUDIO_PCM)
                            ) {

                                if (avChannel.mVoiceType == VoiceType.TWO_WAY_VOICE) {
                                    avChannel.initAcousticEchoCanceler()
                                }

                                //单向语音
                                AudioTrackHelper.initAudioRecord()

                                //开启录音
                                AudioTrackHelper.resumeAudioRecord()
                                d("ONE_WAY_VOICE [${avChannel.mAudioCodec}]")

                                if (avChannel?.audioRecordStatus == true) {
                                    iavChannelStatus?.onTalkStatus(true)
                                }

                                when (avChannel.mAudioCodec) {
                                    AVFrame.MEDIA_CODEC_AUDIO_G711A -> {
                                        var size = AudioTrackHelper.readAudioRecord(inG711BufByte)
                                        if (isTwoWayVoiceType()) {
                                            avChannel.playAcousticEchoCanceler(inG711BufByte, size)
                                        }
                                        size = G711Code.encode(inG711BufByte, 0, size, outG711Buf)
                                        d("readShortAudioRecord size[$size]")
                                        if (size > 0) {
                                            val frameInfo = getAudioInfo(
                                                AVFrame.MEDIA_CODEC_AUDIO_G711A.toShort(),
                                                flag.toByte()
                                            )

                                            AVAPIs.avSendAudioData(
                                                mSendAudioChannelIndex,
                                                outG711Buf,
                                                size,
                                                frameInfo,
                                                16
                                            )
                                        }
                                    }

                                    else -> {
                                        val size = AudioTrackHelper.readAudioRecord(pcmBuf)
                                        if (isTwoWayVoiceType()) {
                                            avChannel.playAcousticEchoCanceler(inG711BufByte, size)
                                        }
                                        d("readShortAudioRecord size[$size] ---")
                                        if (size > 0) {
                                            val frameInfo = getAudioInfo(
                                                AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
                                                flag.toByte()
                                            )
//                                                AVIOCTRLDEFs.SFrameInfo.parseContent(
//                                                AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
//                                                flag.toByte(),
//                                                0,
//                                                0,
//                                                System.currentTimeMillis().toInt()
//                                            )
                                            AVAPIs.avSendAudioData(
                                                mSendAudioChannelIndex, pcmBuf,
                                                size, frameInfo, 16
                                            )
                                        }
                                    }
                                }
                            } else {
                                break
                            }
                        }

                        if (avChannel?.mVoiceType == VoiceType.TWO_WAY_VOICE && avChannel.mAudioCodec == AVFrame.MEDIA_CODEC_AUDIO_PCM) {
                            //双向语音
                            if (avChannel.mAudioPlayer?.isAudioRecord == true) {
                                d("zrtAudioPlayer audioplayer status= pauseAudioRecord")
                                avChannel.mAudioPlayer?.pauseAudioRecord()
                            }
                            if (System.currentTimeMillis() - lastEmptyTime >= 2000) {
                                lastEmptyTime = System.currentTimeMillis();
                                //当关掉录音的时候，需要发送空数据，否则新的设备5S后会主动断开通话通道
                                val frameInfo = getAudioInfo(
                                    AVFrame.MEDIA_CODEC_AUDIO_PCM.toShort(),
                                    flag.toByte()
                                )
                                val result = AVAPIs.avSendAudioData(
                                    mSendAudioChannelIndex, ByteArray(800),
                                    800, frameInfo, 16
                                )
                                d("zrtAudioPlayer audioplayer status= pauseAudioRecord send empty result=$result")
                            }

                            d("zrtAudioPlayer audioplayer status= pauseAudioRecord ")
                        } else {
                            //单向语音
                            isRunning = false
                        }

                    }
                }
                d("sendAudio stop 1")
                try {
                    AudioProcessHelper.unEncode()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                iavChannelStatus?.onTalkStatus(false)
                //释放单向语音资源
                AudioTrackHelper.unInitAudioRecord()
                d("sendAudio stop 2")
                avChannel?.SID?.let { sid ->
                    if (sid >= 0 && mSendAudioChannelIndex >= 0) {
                        AVAPIs.avServExit(sid, mSendAudioChannelIndex)
                        avChannel.IOCtrlQueue?.Enqueue(
                            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTOP,
                            startAudioInfo(mSendAudioChannelIndex)
//                            AVIOCTRLDEFs.SMsgAVIoctrlAVStream.parseContent(mSendAudioChannelIndex)
                        )
                    }
                }

                d("sendAudio stop 3")
                //关闭双向语音音频
                avChannel?.mAudioPlayer?.pauseAudioRecord()
                d("sendAudio stop 4")
                if (isTwoWayVoiceType()) {
                    if (mSendAudioChannelIndex >= 0) {
                        AVAPIs.avServExit(avChannel?.SID ?: -1, mSendAudioChannelIndex)
                    }
                    if (mSendAudioSessionIndex >= 0) {
                        avChannel?.SID?.let { sid ->
                            if (sid >= 0 && mSendAudioChannelIndex >= 0) {
                                AVAPIs.avServExit(sid, mSendAudioChannelIndex)
                                avChannel.IOCtrlQueue?.Enqueue(
                                    AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTOP,
                                    startAudioInfo(mSendAudioChannelIndex)
                                )
                            }
                        }

                    }
                } else {
                    if (mSendAudioChannelIndex >= 0) {
//                        AVAPIs.avServExit(avChannel?.SID?:-1,mSendAudioChannelIndex)
                        AVAPIs.avServStop(mSendAudioChannelIndex)
                    }

                    d("sendAudio stop 5")
                    if (mSendAudioSessionIndex >= 0) {
                        avChannel?.SID?.let { sid ->

                            IOTCAPIs.IOTC_Session_Channel_OFF(sid, mSendAudioSessionIndex)
                        }
                    }
                }


                mSendAudioChannelIndex = -1
                mSendAudioSessionIndex = -1
                d("sendAudio stop 6")

                if (isActive) {
                    emit(SendAudioEmitInfo(type = 0))
                }
                d("stop sendAudio")
            }.flowOn(Dispatchers.IO)
                .collect {
                    if (it.type == 1) {
                        iavChannelStatus?.onAVChannelReceiveExtraInfo(
                            avChannel?.mChannel ?: -1,
                            AVIOCTRLDEFs.IOTYPE_USER_GET_ERROR_CODE_SHENDAUDIO_START_TIME,
                            AVIOCTRLDEFs.startSOUND_END,
                            0
                        )
                    } else if (it.type == null) {
                        iavChannelStatus?.onAudioRecordVolume(it.volume)
                    }


                }
        }
    }

    fun stop() {
        isRunning = false
        iavChannelStatus?.onTalkStatus(false)
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

/**音视频解码帮助类*/
internal object AudioProcessHelper {

    private var mAudioProcess: AudioProcess? = null

    private var initDecode = false
    private var initEncode = false

    fun initDecode(codeId: Int, samplerate: Int, databits: Int, channel: Int) {


        if (mAudioProcess == null) {
            mAudioProcess = AudioProcess()
        }
        initDecode = true
        mAudioProcess?.mDecode?.init(codeId, samplerate, databits, channel)
    }

    fun decode(data: ByteArray?, size: Int, outData: ByteArray?): Int? {
        if (data == null || outData == null || data.size < size) {
            Liotc.d(
                "AudioProcessHelper",
                "decode: return [${data == null}],[${outData == null}],[${data?.size}],[$size]"
            )
            return null
        }
        return mAudioProcess?.mDecode?.decode(data, size, outData)
    }

    fun unDecode() {
        if (initDecode) {
            mAudioProcess?.mDecode?.unInit()
        }
        initDecode = false
    }

    fun initEncode(codeId: Int, samplerate: Int, databits: Int, channel: Int) {
//        mAudioProcess?.mEncode?.unInit()
        if (mAudioProcess == null) {
            mAudioProcess = AudioProcess()
        }
        initEncode = true
        mAudioProcess?.mEncode?.init(codeId, samplerate, databits, channel)
    }

    fun encode(data: ByteArray?, outData: ByteArray): Int {
        if (data == null) {
            return 0
        }
        return mAudioProcess?.mEncode?.encode(data, data.size, outData) ?: 0
    }

    fun unEncode() {
        if (initEncode) {
            mAudioProcess?.mEncode?.unInit()
        }
        initEncode = false
    }

}

/**音频播放、录制*/
internal object AudioTrackHelper {
    //非双向语音的音频播放
    private var mAudioTrack: AudioTrack? = null

    //非双向语音的音频录制
    private var mAudioRecord: AudioRecord? = null


    fun initAudioTrack(sampleRateHz: Int, channel: Int, databits: Int, codec_id: Int): Boolean {
        Liotc.d("RecvAudioJob", "initAudioTrack")
        mAudioTrack?.stop()
        mAudioTrack?.release()

        val channelConfig =
            if (channel == AVFrame.AUDIO_CHANNEL_STERO) AudioFormat.CHANNEL_CONFIGURATION_STEREO else AudioFormat.CHANNEL_CONFIGURATION_MONO

        val audioFormat =
            if (databits == AVFrame.AUDIO_DATABITS_16) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT

        val mMinBufSize = AudioTrack.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)

        if (mMinBufSize == AudioTrack.ERROR_BAD_VALUE
            || mMinBufSize == AudioTrack.ERROR
        ) {
            return false
        }


        try {
            mAudioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateHz,
                channelConfig,
                audioFormat,
                mMinBufSize,
                AudioTrack.MODE_STREAM
            )
            mAudioTrack?.setStereoVolume(1f, 1f)
            mAudioTrack?.play()

        } catch (e: Exception) {
            e.printStackTrace()
            mAudioTrack = null
            return false
        }

        return true
    }

    fun playAudio(data: Any?, offset: Int, size: Int) {
        Liotc.d("RecvAudioJob", "playAudio [${mAudioTrack == null}],[$size]")
        when (data) {
            is ByteArray -> {
                if (data.size >= offset + size) {
                    mAudioTrack?.write(data, offset, size)
                }
            }

            is ShortArray -> {
                if (data.size >= offset + size) {
                    mAudioTrack?.write(data, offset, size)
                }
            }

        }

    }

    //释放单向语音播放资源
    fun unInitAudioTrack() {
        Liotc.d("RecvAudioJob", "unInitAudioTrack")
        mAudioTrack?.flush()
        mAudioTrack?.stop()
        mAudioTrack?.release()
        mAudioTrack = null
    }

    fun audioTrackIsEmpty() = mAudioTrack == null


    //初始化音频录制
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun initAudioRecord() {
        if (mAudioRecord == null) {
            mAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
        }
    }

    //开始录制
    fun resumeAudioRecord() {

        if (mAudioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING && mAudioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            Log.d(
                "AudioTrackHelper",
                "resumeAudioRecord: [${mAudioRecord?.state}],recordingState[${mAudioRecord?.recordingState}]"
            )
            mAudioRecord?.startRecording()
        }
    }

    fun readAudioRecord(data: ByteArray): Int {
        return if (mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord?.read(data, 0, data.size) ?: 0
        } else 0
    }

    fun readShortAudioRecord(data: ShortArray): Int {
        return if (mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord?.read(data, 0, data.size) ?: 0
        } else 0
    }


    fun unInitAudioRecord() {
        Log.d("AudioTrackHelper", "unInitAudioRecord [${mAudioRecord == null}]")

        mAudioRecord?.stop()
        mAudioRecord?.release()
        mAudioRecord = null
    }


}





