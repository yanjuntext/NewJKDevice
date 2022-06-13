package com.tutk.IOTC.camera

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
const val AVIOCTRL_MAX_SEND_SIZE = 30
const val AVIOCTRL_FAST_MAX_SEND_SIZE = 10

class IOCtrlQueue {
    var nQueneType = IOCtrlQueueType.COMMON_IO_CMD_QUENE
    val listData = mutableListOf<IOCtrlSet>()

    var isAutoRemoveBuf = false

    constructor(type: IOCtrlQueueType) {
        nQueneType = type
    }

    @Synchronized
    fun getQueueSize() = listData.size

    @Synchronized
    fun isEmpty() = listData.isEmpty()

    @Synchronized
    fun Enqueue(type: Int, data: ByteArray?) {
        if (isAutoRemoveBuf) {
            val total = listData.size
            if (total < AVIOCTRL_MAX_SEND_SIZE) {
                listData.add(IOCtrlSet(type, data))
            } else {
                listData.clear()
                listData.add(IOCtrlSet(type, data))
            }
        } else {
            listData.add(IOCtrlSet(type, data))
        }
    }

    @Synchronized
    fun Enqueue(avIndex: Int, type: Int, data: ByteArray?) {
        listData.add(IOCtrlSet(type, data))
    }

    @Synchronized
    fun Dequeue(): IOCtrlSet? {
        val iterator = listData.iterator()
        return if (iterator.hasNext()) {
            val value = iterator.next()
            listData.remove(value)
            value
        } else {
            null
        }
    }

    fun removeAll() {
        listData.clear()
    }

    class IOCtrlSet(var IOCtrlType: Int, var IOCtrlBuf: ByteArray?)
}

enum class IOCtrlQueueType {
    COMMON_IO_CMD_QUENE, FAST_MOVE_CMD_QUENE
}