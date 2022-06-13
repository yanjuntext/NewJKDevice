package com.tutk.IOTC.listener

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description:
 */
fun interface OnResultCallback<T> {
    fun onResult(result: T?)
}