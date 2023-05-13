package com.tutk.IOTC.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.*
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ingenic.api.AudioFrame
import com.ingenic.api.Frequency
import com.tutk.IOTC.*
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.RecordStatus
import com.tutk.IOTC.status.VoiceType
import com.tutk.libSLC.AcousticEchoCanceler
import com.tutk.webtrc.MyAudioPlayer
import kotlinx.coroutines.*

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
const val HEXES = "0123456789ABCDEF"

//internal const val MAX_BUF_SIZE = 1920 * 1080 * 3
internal const val MAX_BUF_SIZE = 2304 * 1296 * 3

//视频解析最大buffer
//internal const val MAX_FRAMEBUF = 1920 * 1080 * 4
internal const val MAX_FRAMEBUF = 2304 * 1296 * 4

internal const val MAX_AUDIO_BUF_SIZE = 1280


class AVChannel(
    var mChannel: Int,
    var mViewAcc: String,
    var mViewPwd: String,
    var uid: String?,
    var iavChannelStatus: IAVChannelListener?
) {

    var mAvIndex = -1
    var mServiceType: Long = -0x1
        set(value) {
            //防止使用其他不存在的解码格式
            mAudioCodec =
                if ((value and 4096) == 0L) AVFrame.MEDIA_CODEC_AUDIO_G711A else AVFrame.MEDIA_CODEC_AUDIO_G711A
            field = value
        }
    var mAudioCodec: Int = 0
    var IOCtrlQueue: IOCtrlQueue? = IOCtrlQueue(IOCtrlQueueType.COMMON_IO_CMD_QUENE)
    var IOCtrlFastQueue: IOCtrlQueue? = IOCtrlQueue(IOCtrlQueueType.FAST_MOVE_CMD_QUENE)
    var VideoFrameQueue: AVFrameQueue? = AVFrameQueue()
    var AudioFrameQueue: AVFrameQueue? = AVFrameQueue()

    var lastFrame: Bitmap? = null

    var lastThumFrame: Bitmap? = null

    var videoFPS = 0
    var videoBPS = 0
    var audioBPS = 0


    internal var SID: Int = -1
    private var mStartJob: StartJob? = null
    private var mRecvIoJob: RecvIOJob? = null
    private var mSendIoJob: SendIOJob? = null

    //音视频
    private var mRecvVideoJob: RecvVideoJob? = null
    private var mRecvAudioJob: RecvAudioJob? = null
    private var mDecVideoJob: DecodeVideoJob? = null
    private var mSendAudioJob: SendAudioJob? = null

    private var mSendFileJob: SendFileJob? = null
    private var mDownloadFileJob: DownFileJob? = null


    //    internal var recording = false
    internal var codeId = -1

    //音频播放状态
    internal var audioPlayStatus = false

    //音频录音状态 （发送语音）
    internal var audioRecordStatus = false

    internal var playMode: PlayMode = PlayMode.PLAY_LIVE

    //双向语音的音频播放器
//    internal var mAudioPlayer: AudioPlayer? = null
    internal var mAudioPlayer: MyAudioPlayer? = null
    internal var mAudioFrame: AudioFrame? = null

    //语音类型
    internal var mVoiceType = VoiceType.ONE_WAY_VOICE

    private var mAcousticEchoCanceler: AcousticEchoCanceler? = null

    internal fun setSid(sid: Int) {
        SID = sid
        mStartJob?.setSid(sid)
        mRecvIoJob?.setSid(sid)
        mSendIoJob?.setSid(sid)


        mRecvVideoJob?.setSid(sid)
    }

    /**开启IO命令*/
    internal fun start() {

        if (mStartJob == null) {
            mStartJob = StartJob(this, iavChannelStatus = iavChannelStatus)
        }

        if (mRecvIoJob == null) {
            mRecvIoJob = RecvIOJob(this, iavChannelStatus = iavChannelStatus)
        }

        if (mSendIoJob == null) {
            mSendIoJob = SendIOJob(this, iavChannelStatus = iavChannelStatus)
        }

        mStartJob?.setSid(SID)
        mRecvIoJob?.setSid(SID)
        mSendIoJob?.setSid(SID)


        mStartJob?.start()
        mRecvIoJob?.start()
        mSendIoJob?.start()
        //获取音频类型
        IOCtrlQueue?.Enqueue(
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ,
            AVIOCTRLDEFs.getAudioCodec()
        )
    }

    internal fun stop() {
        mRecvIoJob?.stop()
        mSendIoJob?.stop()
        mStartJob?.stop()
        d("RecvAudioJob", "unInitAudioTrack releaseAudio stop")
        //释放音频
        releaseAudio()
        //释放视频
        stopShow()

        //释放文件
        releaseSendFile()
        releaseDownloadFile()

        IOCtrlQueue?.removeAll()
        IOCtrlFastQueue?.removeAll()
    }

    /**视频直播*/
    internal fun startShow(context: Context?, ratation: Int = 0, withYUV: Boolean = false) {

        if (mRecvVideoJob == null) {
            mRecvVideoJob = RecvVideoJob(this, iavChannelStatus = iavChannelStatus)
        }

        if (mDecVideoJob == null) {
            mDecVideoJob =
                DecodeVideoJob(this, iavChannelStatus = iavChannelStatus, withYuv = withYUV)
        }

        mRecvVideoJob?.setSid(SID)

        mDecVideoJob?.withYuv = withYUV

        mRecvVideoJob?.start()
        mDecVideoJob?.start(context)
    }

    internal fun stopShow() {
        mRecvVideoJob?.stop()
        mDecVideoJob?.stop()
        playMode = PlayMode.PLAY_LIVE
    }


    //初始化双向语音
    @Synchronized
    internal fun initAudioPlayer(context: Context?): Boolean {
        if (context == null) {
            return false
        }

        if (mAudioPlayer != null) {
            return true
        }
        d("zrtAudioPlayer", "initAudioPlayer")
        mAudioFrame = AudioFrame()
//        mAudioPlayer = AudioPlayer.getInstance().init(context, null, Frequency.PCM_8K)
        mAudioPlayer = MyAudioPlayer.getInstance().init(context, null, Frequency.PCM_8K)
        //            MLog.e("wangyanjun11111", "onUpdate == ");
        mAudioPlayer?.setPtsUpdateListener {
            d("zrtAudioPlayer", "OnTSUpdateListener[$it]")
        }
        //第一个参数：音频播放 开关
        //第二个参数：音频录制 开关
        mAudioPlayer?.start(false, false)
        return true
    }

    //释放双向语音
    @Synchronized
    internal fun unInitAudioPlayer() {
        //暂停播放
        mAudioPlayer?.soundOff()
        //暂停录制
        mAudioPlayer?.pauseAudioRecord()

        mAudioPlayer?.stop()
        mAudioPlayer?.release()
        mAudioPlayer = null

        mAudioFrame = null

//        releaseAcousticEchoCanceler()
    }

    internal fun audioPlayerIsEmpty() = mAudioPlayer == null

    //播放双向语音:播放音频
    internal fun putPlayData(data: ByteArray?, size: Int, time: Long) {
        if (mAudioFrame == null) {
            mAudioFrame = AudioFrame()
        }
        if (data == null || data.isEmpty() || data.size < size) {
            return
        }

        mAudioFrame?.setAudioData(data, size)
        mAudioFrame?.timeStamp = time
        mAudioFrame?.let {
            mAudioPlayer?.putPlayData(it)
        }

    }

    //设置音频格式
    internal fun setVoiceType(voiceType: VoiceType) {
        mVoiceType = voiceType
    }

    //设置音频播放状态
    internal fun setAudioTrackStatus(
        context: Context?,
        status: Boolean,
        recording: Boolean = false
    ) {
        d("RecvAudioJob", "unInitAudioTrack setAudioTrackStatus=$status")
        audioPlayStatus = status
        if (mRecvAudioJob == null) {
            mRecvAudioJob = RecvAudioJob(this, iavChannelStatus = iavChannelStatus)
        }

        if (status || recording) {
            mRecvAudioJob?.start(context, playMode.value)
        }
        if (!status && !recording) {
            mRecvAudioJob?.isRunning = false
        }
    }


    internal fun setAudioRecordStatus(
        context: Context?,
        status: Boolean
    ) {
        audioRecordStatus = status
        if (context == null) {
            return
        }
        if (mSendAudioJob == null) {
            mSendAudioJob = SendAudioJob(this, iavChannelStatus = iavChannelStatus)
        }
        if (status) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw RuntimeException("plz open android.permission.RECORD_AUDIO permission")
            }
            mSendAudioJob?.start(context)
        }
    }

    //释放音频资源
    internal fun releaseAudio() {
        d("RecvAudioJob", "unInitAudioTrack releaseAudio")
        audioPlayStatus = false
        audioRecordStatus = false

        mRecvAudioJob?.stop()
        mRecvAudioJob = null

        mSendAudioJob?.stop()
        mSendAudioJob = null

        unInitAudioPlayer()

        releaseAcousticEchoCanceler()
    }

    //发送文件
    internal fun startSendFile(context: Context? = null, file: String?) {
        if (mSendFileJob == null) {
            mSendFileJob = SendFileJob(this, iavChannelStatus = iavChannelStatus)
        }
        mSendFileJob?.start(file, context)
    }

    internal fun startSendFile(context: Context? = null, uri: Uri?) {
        if (mSendFileJob == null) {
            mSendFileJob = SendFileJob(this, iavChannelStatus = iavChannelStatus)
        }
        mSendFileJob?.start(uri, context)
    }

    internal fun releaseSendFile() {
        mSendFileJob?.stop()
    }

    //下载文件
    internal fun startDownloadFile(srcFile: String?, dstFile: String?) {
        if (mDownloadFileJob == null) {
            mDownloadFileJob = DownFileJob(this, iavChannelStatus = iavChannelStatus)
        }
        mDownloadFileJob?.start(srcFile, dstFile)
    }

    internal fun startDownloadFile(context: Context?, srcFile: String?, dstUri: Uri?) {
        if (mDownloadFileJob == null) {
            mDownloadFileJob = DownFileJob(this, iavChannelStatus = iavChannelStatus)
        }
        mDownloadFileJob?.start(context, srcFile, dstUri)
    }

    internal fun releaseDownloadFile() {
        mDownloadFileJob?.stop()
    }

    @Synchronized
    internal fun initAcousticEchoCanceler(): Boolean {
        if (mAcousticEchoCanceler == null) {
            d("AcousticEchoCanceler", "initAcousticEchoCanceler")
            mAcousticEchoCanceler = AcousticEchoCanceler()
            mAcousticEchoCanceler?.Open(8000, 16)
        }
        return true
    }

    internal fun captureAcousticEchoCanceler(data: ByteArray, length: Int) {
        if (mAcousticEchoCanceler != null) {
            d("AcousticEchoCanceler", "captureAcousticEchoCanceler")
        }
        mAcousticEchoCanceler?.Capture(data, length)
    }

    internal fun playAcousticEchoCanceler(data: ByteArray, length: Int) {
        if (mAcousticEchoCanceler != null) {
            d("AcousticEchoCanceler", "playAcousticEchoCanceler")
        }
        mAcousticEchoCanceler?.Play(data, length)
    }

    internal fun releaseAcousticEchoCanceler() {
        if (mAcousticEchoCanceler != null) {
            d("AcousticEchoCanceler", "releaseAcousticEchoCanceler")
        }
        mAcousticEchoCanceler?.close()
        mAcousticEchoCanceler = null
    }

    fun supportAudioIn() = (mServiceType and 1L) == 0L
    fun supportAudioOut() = (mServiceType and 2L) == 0L
    fun supportPanTilt() = (mServiceType and 4L) == 0L
    fun supportEventList() = (mServiceType and 8L) == 0L
    fun supportPlayback() = (mServiceType and 16L) == 0L
    fun supportScanWifi() = (mServiceType and 32L) == 0L
    fun supportEventSet() = (mServiceType and 64L) == 0L
    fun supportRecordSet() = (mServiceType and 128L) == 0L
    fun supportSDCardFormat() = (mServiceType and 256L) == 0L
    fun supportVideoFlip() = (mServiceType and 512L) == 0L
    fun supportEnvironmentMode() = (mServiceType and 1024L) == 0L
    fun supportMultiStreamMode() = (mServiceType and 2048L) == 0L
    fun supportAudioOutEncoding() = (mServiceType and 4096L) == 0L
    fun supportVideoQualitySet() = (mServiceType and 8192L) == 0L
    fun supportDeviceInfo() = (mServiceType and 16384L) == 0L
    fun supportTimeZone() = (mServiceType and 32768L) == 0L
    fun timeZone() = (mServiceType and 65536L) == 0L
    fun supportDeviceUpdate() = (mServiceType and 262144L) == 0L
    fun supportDevicePtz() = (mServiceType and 524288L) == 0L
    fun supportDeviceReboot() = (mServiceType and 2097152L) == 0L
    fun supportDeviceHighGrade() = (mServiceType and (1L shl 27)) == 0L
    fun supportAlexa() = ((mServiceType shr 31) and 1L) == 0L

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

interface IAVChannelListener {
    /**Channel 状态*/
    fun onAVChannelStatus(channel: Int, status: Int)

    /**IO接收*/
    fun onAVChannelRecv(channel: Int, type: Int, data: ByteArray?)

    fun onAVChannelReceiveExtraInfo(channel: Int, type: Int, recvFrame: Int, dispFrame: Int)

    /**图片内容*/
    fun onAVChannelReceiverFrameData(channel: Int, bitmap: Bitmap?)

    /**图片内容，包含时间戳*/
    fun onAVChannelReceiverFrameData(channel: Int, bitmap: Bitmap?, time: Long)

    fun onAVChannelReceiverFrameInfo(
        channel: Int,
        bitRate: Int,
        franRate: Int,
        onlineNumber: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    )

    /**
     * 录像状态回调
     * @param status 录像状态
     * @param file 录像保存文件
     * @param time 录像时长/秒
     * */
    fun onAVChannelRecordStatus(status: RecordStatus, file: String?, time: Int)

    /**文件发送状态*/
    fun onAVChannelSendFileStatus(status: SendFileStatus, total: Int, sendTotal: Int, progress: Int)

    /**文件下载状态*/
    fun onAVChanneldownloadFileStatus(
        status: DownLoadFileStatus,
        total: Int,
        downloadTotal: Int,
        progress: Int
    )

    /**音频监听状态*/
    fun onListenerStatus(status: Boolean)

    /**音频发送状态*/
    fun onTalkStatus(status: Boolean)

}


fun ByteArray?.getHex(size: Int = -1): String? {
    if (this == null || this.isEmpty()) {
        return null
    }

    val sb = StringBuilder()
    run outside@{
        this.forEachIndexed { index, it ->
            sb.append(HEXES[(it.toInt() and 0xF0) shr 4])
                .append(HEXES[(it.toInt() and 0x0F)])
                .append(" ")
            if (index + 1 >= size && size >= 0) {
                return@outside
            }
        }
    }

    return sb.toString()
}

fun Int.toHexString() = this.toString(16)

fun Context?.checkPermissions(vararg permissions: String): Boolean {
    return this?.let { ctx ->
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    ctx,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@let false
            }
        }
        true
    } ?: false
}

