package com.tutk.IOTC

import java.util.concurrent.locks.ReentrantLock

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
class AVFrameQueue {
    private val listData = mutableListOf<AVFrame>()

    @Volatile
    var mSize = 0

    @Volatile
    var isDroping = false

    private val lock = ReentrantLock()

    private val addLock = ReentrantLock()

    fun removeHead(): AVFrame? {
        lock.lock()
        return if (mSize == 0) {
            lock.unlock()
            null
        } else {

            val avFrame = listData.removeAt(0)
            mSize--
            lock.unlock()
            avFrame
        }
    }

    fun addLast(avFrame: AVFrame) {

        addLock.lock()
        if (mSize > 1500) {
            removeHead()
        }
        listData.add(avFrame)
        mSize++
        addLock.unlock()
    }

    fun removeAll() {
        addLock.lock()
        lock.lock()
        listData.clear()
        mSize = 0
        lock.unlock()
        addLock.unlock()
    }

    fun isFirstIFrame(): Boolean {
        var value = false
        lock.lock()
        if (listData.isNotEmpty()) {
            value = listData[0].isIFrame()
        }
        lock.unlock()
        return value
    }
}