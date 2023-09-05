package com.tutk.IOTC

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
fun interface OnLineResultCBListener {
    fun onLineResultCB(resule:Int,userData:ByteArray?)
}