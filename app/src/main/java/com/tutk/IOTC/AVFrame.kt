package com.tutk.IOTC

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
class AVFrame(
    frameNo: Long,
    frameState: Byte,
    frameHead: ByteArray,
    frameData: ByteArray,
    frameDataSize: Int,
    playbacmmode: Int
) {

    companion object {
        const val FRAMEINFO_SIZE = 24

        // Codec ID
        const val MEDIA_CODEC_UNKNOWN = 0x00
        const val MEDIA_CODEC_VIDEO_MPEG4 = 0x4C
        const val MEDIA_CODEC_VIDEO_H263 = 0x4D
        const val MEDIA_CODEC_VIDEO_H264 = 0x4E
        const val MEDIA_CODEC_VIDEO_MJPEG = 0x4F
        const val MEDIA_CODEC_VIDEO_H265 = 0x50

        const val MEDIA_CODEC_AUDIO_ADPCM = 0x8B
        const val MEDIA_CODEC_AUDIO_PCM = 0x8C
        const val MEDIA_CODEC_AUDIO_SPEEX = 0x8D
        const val MEDIA_CODEC_AUDIO_MP3 = 0x8E
        const val MEDIA_CODEC_AUDIO_G726 = 0x8F
        const val MEDIA_CODEC_AUDIO_G711U = 0x89 //g711 u-law

        const val MEDIA_CODEC_AUDIO_G711A = 0x8A //g711 a-law

        const val MEDIA_CODEC_AUDIO_AAC_RAW = 0x86
        const val MEDIA_CODEC_AUDIO_AAC_ADTS = 0x87
        const val MEDIA_CODEC_AUDIO_AAC_LATM = 0x88

        // Frame Flag
        const val IPC_FRAME_FLAG_PBFRAME = 0x00 // A/V P/B frame.

        const val IPC_FRAME_FLAG_IFRAME = 0x01 // A/V I frame.

        const val IPC_FRAME_FLAG_MD = 0x02 // For motion detection.

        const val IPC_FRAME_FLAG_IO = 0x03 // For Alarm IO detection.


        // audio sample rate
        const val AUDIO_SAMPLE_8K = 0x00
        const val AUDIO_SAMPLE_11K = 0x01
        const val AUDIO_SAMPLE_12K = 0x02
        const val AUDIO_SAMPLE_16K = 0x03
        const val AUDIO_SAMPLE_22K = 0x04

        const val AUDIO_SAMPLE_24K = 0x05
        const val AUDIO_SAMPLE_32K = 0x06
        const val AUDIO_SAMPLE_44K = 0x07
        const val AUDIO_SAMPLE_48K = 0x08

        // audio sample data bit
        const val AUDIO_DATABITS_8 = 0
        const val AUDIO_DATABITS_16 = 1

        // audio channel number
        const val AUDIO_CHANNEL_MONO = 0
        const val AUDIO_CHANNEL_STERO = 1

        // -----------------------------------------------------
        const val FRM_STATE_UNKOWN: Byte = -1
        const val FRM_STATE_COMPLETE: Byte = 0
        const val FRM_STATE_INCOMPLETE: Byte = 1
        const val FRM_STATE_LOSED: Byte = 2

        fun getSamplerate(flags: Byte) =
            when (flags.toInt() ushr 2) {
                AUDIO_SAMPLE_8K -> 8000
                AUDIO_SAMPLE_11K -> 11025
                AUDIO_SAMPLE_12K -> 12000
                AUDIO_SAMPLE_16K -> 16000
                AUDIO_SAMPLE_22K -> 22050
                AUDIO_SAMPLE_24K -> 24000
                AUDIO_SAMPLE_32K -> 32000
                AUDIO_SAMPLE_44K -> 44100
                AUDIO_SAMPLE_48K -> 48000
                else -> 8000
            }

    }

    var codec_id: Short = 0// UINT16 codec_id;
    // Media codec type defined in sys_mmdef.h,
    // MEDIA_CODEC_AUDIO_PCMLE16 for audio,
    // MEDIA_CODEC_VIDEO_H264 for video.

    // Media codec type defined in sys_mmdef.h,
    // MEDIA_CODEC_AUDIO_PCMLE16 for audio,
    // MEDIA_CODEC_VIDEO_H264 for video.
    var flags: Byte = -1 // Combined with IPC_FRAME_xxx.

    var onlineNum: Byte = 0
    var timestamp = 0 // Timestamp of the frame, in milliseconds.

    var deviceCurrentTime = 0L//设备当前时间，in milliseconds.

    var videoWidth = 0
    var videoHeight = 0

    // -----------------------------
    var frmNo: Long = 0L
    var frmState: Byte = 1 // 0:complete; 1:incomplete; 2: losed

    var frmSize = 0 // Raw data size in bytes.

    var frmData: ByteArray? = null // Raw data of the frame.

    var playmode = 0

    init {
        codec_id = Packet.byteArrayToInt_Little(frameHead).toShort()
        if (frameHead.size > 2) {
            flags = frameHead[2]
        }
        if (frameHead.size > 4) {
            onlineNum = frameHead[4]
        }
        this.playmode = playmode
        timestamp = if (playbacmmode == Camera.AUDIORECORD_LIVEMODE) {
            Packet.byteArrayToInt_Little(frameHead, 12)
        } else {
            Packet.byteArrayToInt_Little(frameHead, 8)
        }

        if (frameHead.size >= 24) {
            deviceCurrentTime = Packet.byteArrayToLong(frameHead, false, 16)
        }

        videoWidth = Packet.byteArrayToInt_Little(frameHead, 16)
        videoHeight = Packet.byteArrayToInt_Little(frameHead, 20)
        frmSize = frameDataSize
        frmData = frameData
        frmNo = frameNo
    }

    fun isIFrame() = (flags.toInt() and IPC_FRAME_FLAG_IFRAME) == IPC_FRAME_FLAG_IFRAME

}