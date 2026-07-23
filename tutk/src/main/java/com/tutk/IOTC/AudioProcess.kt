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

        // 标记解码器是否已成功初始化（并且尚未释放）
        @Volatile
        private var isInitialized = false

        // 保护初始化和释放操作的同步锁
        private val lock = Any()

        fun decode(data: ByteArray?, size: Int, outData: ByteArray?): Int {
            if (data == null || size == 0 || outData == null) {
                return -1
            }
            // 如果未初始化或已释放，直接返回错误，避免访问 Native 层
            if (!isInitialized) {
                return -1
            }
            return decoder.decode(data, size, outData)
        }

        fun unInit() {
            synchronized(lock) {
                // 如果未初始化或已释放，直接返回，避免重复释放
                if (!isInitialized) {
                    return
                }
                try {
                    decoder.release()
                } catch (e: Exception) {
                    // 捕获可能的异常（如底层 JNI 崩溃前的异常），但一般不会抛出
                    // 记录日志以便追踪
                } finally {
                    // 无论 release 是否成功，标记为未初始化，防止再次调用
                    isInitialized = false
                }
            }
//            decoder.release()
        }

        fun init(codeId: Int, sample_rate: Int, sample_fmt: Int, channel_fmt: Int) {
            synchronized(lock) {
                // 如果已经初始化，先释放旧的（避免资源泄漏），再重新初始化
                if (isInitialized) {
                    try {
                        decoder.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                    isInitialized = false
                }

                try {
                    decoder.create(
                        AudioConvert.AudioCodec.getAudioCodec(codeId),
                        sample_rate,
                        sample_fmt,
                        channel_fmt
                    )
                    // 假设 create 成功，标记为已初始化
                    // （若 create 内部失败并抛出异常，则不会执行到此行）
                    isInitialized = true
                } catch (e: Throwable) {
                    // 创建失败，保持 isInitialized = false
                    // 可打印日志
                    isInitialized = false
                }

                // 无论初始化成功与否，都设置 bufferSize（但若失败，实际解码不会成功）
                when (codeId) {
                    134, 135, 136, 140, 142 -> bufferSize = 65535
                    137, 138, 143 -> bufferSize = 2048
                    139 -> bufferSize = 640
                    141 -> bufferSize = 320
                }
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

        // 标记编码器是否已成功初始化且未释放
        @Volatile
        private var isInitialized = false

        // 保护 init/unInit/encode 的同步锁
        private val lock = Any()

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
            synchronized(lock) {
                // 如果已初始化，先释放旧的再重新初始化（避免资源泄漏）
                if (isInitialized) {
                    try {
                        encoder.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                    isInitialized = false
                }

                var success = false
                try {
                    encoder.create(AudioConvert.AudioCodec.getAudioCodec(var1), var2, var3, var4)
                    q = var1

                    when (q) {
                        134, 135, 136 -> {
                            r = 512
                            m = 65535
                            success = true
                        }
                        137, 138, 143 -> {
                            r = 320
                            m = 2048
                            success = true
                        }
                        139 -> {
                            r = 640
                            m = 160
                            success = true
                        }
                        140 -> {
                            r = t
                            m = s
                            success = true
                        }
                        141 -> {
                            r = 320
                            m = 38
                            success = true
                        }
                    }
                } catch (e: Throwable) {
                    // 创建失败，success 保持 false
                    success = false
                }

                isInitialized = success
                return success
            }
        }

        fun unInit() {
            synchronized(lock) {
                if (!isInitialized) {
                    return
                }
                try {
                    encoder.release()
                } catch (e: Exception) {
                    // 记录日志，忽略
                } finally {
                    isInitialized = false
                }
            }
        }

        fun encode(data: ByteArray?, size: Int, outData: ByteArray?): Int {
            if (data == null || size == 0 || outData == null) {
                return -1
            }
            // 如果编码器未就绪，直接返回错误，避免访问 Native 层
            if (!isInitialized) {
                return -1
            }
            return encoder.encode(data, size, outData)
        }

    }

}