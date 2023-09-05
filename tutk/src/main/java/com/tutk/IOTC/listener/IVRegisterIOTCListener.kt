package com.tutk.IOTC.listener

import com.tutk.IOTC.Camera

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:硬解码接口
 */
fun interface IVRegisterIOTCListener {
    fun receiveFrameDataForMediaCodec(
        var1: Camera?, var2: Int, var3: ByteArray?, var4: Int,
        var5: Int, var6: ByteArray?, var7: Boolean, var8: Int,
        playmode: Int
    )
}