package com.tutk.IOTC

import com.tutk.libmediaconvert.AudioConvert
import com.tutk.libmediaconvert.AudioDecoder
import com.tutk.libmediaconvert.AudioEncoder

/**
 * @Author: wangyj
 * @CreateDate: 2021/10/9
 * @Description:
 */
class AudioProcess {

    val mEncode = Encode()
    val mDecode = Decode()

    companion object {
        fun UpdateValToLeavl(data: ByteArray?, var1: Float) {

            data?.forEachIndexed { index, value ->
                data[index] = (value * var1).toInt().toByte()
                if (data[index] > 127) {
                    data[index] = 127
                }
                if (data[index] < -128) {
                    data[index] = -128
                }
            }
        }
    }

    public class Decode {
        private var bufferSize = 0
        private var decoder = AudioDecoder()

        fun getOutputBufferSize() = bufferSize

        fun decode(data: ByteArray?, size: Int, outData: ByteArray?): Int {
            if (data == null || size == 0 || outData == null) {
                return -1
            }
            return decoder.decode(data, size, outData)
        }

        fun unInit() {
            decoder.release()
        }

        fun init(codeId: Int, sample_rate: Int, sample_fmt: Int, channel_fmt: Int) {
            decoder.create(
                AudioConvert.AudioCodec.getAudioCodec(codeId),
                sample_rate,
                sample_fmt,
                channel_fmt
            )
            when (codeId) {
                134, 135, 136, 140, 142 -> bufferSize = 65535
                137, 138, 143 -> bufferSize = 2048
                139 -> bufferSize = 640
                141 -> bufferSize = 320
            }
        }
    }

    public class Encode {

        private val encoder = AudioEncoder()

        private var q = 0
        private var r = 0
        private var m = 0
        private var s = 640
        private var t = 640

        fun getAudioSampleRate(var1: Int) =
            when (var1) {
                1 -> 11000
                2 -> 12000
                3 -> 16000
                4 -> 22000
                5 -> 24000
                6 -> 32000
                7 -> 44000
                8 -> 48000
                else -> 8000
            }

        fun getAudioSample(var1: Int) =
            when (var1) {
                in 0..8 -> var1
                else -> 0
            }

        fun setAudioSizePCM(var1: Int, var2: Int) {
            s = var1
            t = var2
        }

        fun getInputBufferSize() = r
        fun getOutputBufferSize() = m

        fun init(var1: Int, var2: Int, var3: Int, var4: Int): Boolean {
            encoder.create(AudioConvert.AudioCodec.getAudioCodec(var1), var2, var3, var4)
            q = var1
            var flag = false

            when (q) {
                134, 135, 136 -> {
                    r = 512
                    m = 65535
                    flag = true
                }
                137, 138, 143 -> {
                    r = 320
                    m = 2048
                    flag = true
                }
                139 -> {
                    r = 640
                    m = 160
                    flag = true
                }
                140 -> {
                    r = t
                    m = s
                    flag = true
                }
                141 -> {
                    r = 320
                    m = 38
                    flag = true
                }

            }

            return flag
        }

        fun unInit() {
            encoder.release()
        }

        fun encode(data: ByteArray?, size: Int, outData: ByteArray?): Int {
            if (data == null || size == 0 || outData == null) {
                return -1
            }
            return encoder.encode(data, size, outData)
        }

    }

}