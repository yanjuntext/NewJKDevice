package com.tutk.IOTC.listener

import android.graphics.Bitmap
import com.tutk.IOTC.Camera

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
//帧消息监听
interface OnFrameCallback {
    fun receiveFrameData(camera: Camera?, avChannel: Int, bmp: Bitmap?)

    fun receiveFrameData(camera: Camera?,avChannel: Int,bmp: Bitmap?,time:Long)

    fun receiveFrameDataTime(time: Long)

    fun receiveFrameInfo(
        camera: Camera?,
        avChannel: Int,
        bitRate: Long,
        frameRate: Int,
        onlineNm: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    )
}

/**
 * io命令监听
 * @param camera
 * @param avChannel channel 通道号
 * @param avIOCtrlMsgType 命令号
 * @param data 返回数据
 */
fun interface OnIOCallback {
    fun receiveIOCtrlData(camera: Camera?, avChannel: Int, avIOCtrlMsgType: Int, data: ByteArray?)
}

//其他命令监听
fun interface OnExtraCallback {
    fun receiveExtraInfo(
        camera: Camera?,
        avChannel: Int,
        eventType: Int,
        recvFrame: Int,
        dispFrame: Int
    )
}

//通道监听 在线状态监听
interface OnSessionChannelCallback {
    fun receiveSessionInfo(camera: Camera?, resultCode: Int)

    fun receiveChannelInfo(camera: Camera?, avChannel: Int, resultCode: Int)
}