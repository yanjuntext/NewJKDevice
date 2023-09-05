package com.tutk.IOTC

import android.util.Log

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description: logcat 工具
 */


object Liotc {
    private  val TAG = "IOTCamera"
    private var DEBUG = false

    fun setDebugLog(debug: Boolean) {
        DEBUG = debug
    }

    fun d(tag: String?, message: String?) {
        if (!DEBUG) return
        if (message.isNullOrEmpty()) return
        Log.d(TAG, "$tag: $message")
    }

    fun i(tag: String, message: String?) {
        if (!DEBUG) return
        if (message.isNullOrEmpty()) return
        Log.i(TAG, "$tag: $message")
    }

    fun w(tag: String, message: String?) {
        if (!DEBUG) return
        if (message.isNullOrEmpty()) return
        Log.w(TAG, "$tag: $message")
    }

    fun e(tag: String, message: String?) {
        if (!DEBUG) return
        if (message.isNullOrEmpty()) return
        Log.e(TAG, "$tag: $message")
    }

}