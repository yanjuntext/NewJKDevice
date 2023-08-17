package com.tutk.io

import android.util.Log
import com.tutk.IOTC.AVIOCTRLDEFs
import com.tutk.IOTC.Camera
import com.tutk.IOTC.littleByteArray
import com.tutk.bean.MeoofFeedPlanItem
import java.util.*

/**
 * @Author: wangyj
 * @CreateDate: 2023/7/21
 * @Description:
 */

/**meoof  手动喂食*/
fun Camera?.meoofManualFeed(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    left: Int,
    right: Int,
    sound: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(7)
        data[0] = left.toByte()
        data[1] = right.toByte()
        data[2] = if (sound) 1 else 0
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_MEOOF_APP_FEED_REQ,
            data
        )
        true
    } else false
}

/**meoof 设备状态*/
fun Camera?.meoofGetStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,

    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(1)
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_MEOOF_DEVICE_STATUS_REQ,
            data
        )
        true
    } else false
}

/**meoof  获取喂食计划*/
fun Camera?.meoofGetFeedPlan(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return meoofFeedPlan(channel, 0, ByteArray(720), must)
}

/**meoof 设置喂食计划*/
fun Camera?.meoofSetFeedPlan(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    list: MutableList<MeoofFeedPlanItem>,
    must: Boolean = false
): Boolean {
    val itemSize = 74
    val data = ByteArray(592)
    list.forEachIndexed { index, info ->
        var start = itemSize * index

        data[start++] = info.week.toByte()
        data[start++] = if (info.status) 1 else 0

        val itemList = info.list
        itemList.forEach { item ->
            data[start++] = item.index.toByte()
            data[start++] = item.left.toByte()
            data[start++] = item.right.toByte()
            data[start++] = item.hour.toByte()
            data[start++] = item.minute.toByte()
            data[start++] = if (item.sound) 1 else 0
            data[start++] = if (item.status) 1 else 0
            start += 2
        }
    }
    return meoofFeedPlan(channel, 1, data, must)
}

private fun Camera?.meoofFeedPlan(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    data: ByteArray,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val feedPlan = ByteArray(data.size + 1)
        feedPlan[0] = type.toByte()
        System.arraycopy(data, 0, feedPlan, 1, data.size)

        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_MEOOF_SET_FEEDPLAN_REQ,
            feedPlan
        )
        true
    } else false
}

/**meoof  获取开关状态*/
fun Camera?.meoofGetSwitchStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    must: Boolean = false
): Boolean {
    return meoofSwitchStatus(channel, 0, type, false, must)
}

fun Camera?.meoofSetSwitchStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return meoofSwitchStatus(channel, 1, type, status, must)
}

private fun Camera?.meoofSwitchStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    index: Int,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(3)
        data[0] = type.toByte()
        data[1] = index.toByte()
        data[2] = if (status) 1 else 0
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_MEOOF_SET_DEVICE_SWITCH_REQ,
            data
        )
        true
    } else false
}

//获取meoof 设备 摄像头开启或关闭
fun Camera?.meoofGetLiveStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return meoofLiveStatus(channel, 0, true, must)
}

fun Camera?.meoofSetLiveStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return meoofLiveStatus(channel, 1, status, must)
}

private fun Camera?.meoofLiveStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(2)
        data[0] = type.toByte()
        data[1] = if (status) 0 else 1
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_MEOOF_PRIVATE_MODE_REQ,
            data
        )
        true
    } else false
}

/**meoof   录像列表*/
fun Camera?.meoofRecordList(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    year: Int = -1,
    month: Int = -1,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        if (year > 0 && month > 0) {
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
        }
        calendar.add(Calendar.MONTH, -12)
        val data = ByteArray(12 * 12)
        (1..12).forEach {
            val _dataIndex = ByteArray(12)
            calendar.add(Calendar.MONTH, 1)
            Log.d("meoofRecordList", "meoofRecordList: year=${calendar.get(Calendar.YEAR)}  month=${calendar.get(Calendar.MONTH)+1}")
            val _year = calendar.get(Calendar.YEAR).littleByteArray()
            System.arraycopy(_year, 0, _dataIndex, 0, _year.size)
            val _month = (calendar.get(Calendar.MONTH) + 1).littleByteArray()
            System.arraycopy(_month, 0, _dataIndex, 4, _month.size)
            System.arraycopy(_dataIndex, 0, data, (it - 1) * 12, _dataIndex.size)
        }
        this?.sendIOCtrl(channel, AVIOCTRLDEFs.IOTYPE_MEOOF_GET_MONTH_RECORD_LIST_REQ, data)
        true
    } else false
}
