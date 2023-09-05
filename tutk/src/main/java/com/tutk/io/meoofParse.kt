package com.tutk.io

import com.tutk.IOTC.littleInt
import com.tutk.bean.*

/**
 * @Author: wangyj
 * @CreateDate: 2023/7/21
 * @Description:
 */
/**meoof  解析状态*/
fun ByteArray?.meoofParseStatus(): MeoofStatus? {
    this ?: return null
    if ((this.size) < 8) return null
    val oneStatus = this.littleInt(0)
    val twoStatus = this.littleInt(4)
    return MeoofStatus(oneStatus, twoStatus)
}

/**meoof 手动喂食*/
fun ByteArray?.meoofParseManualFeed(): MeoofManualFeed? {
    this ?: return null
    if (this.isEmpty()) return null
    return MeoofManualFeed(this[0].toInt())
}

/**解析喂食计划*/
fun ByteArray?.meoofParseFeedPlan(): MeoofFeedPlan? {
    this ?: return null
    if (this.size < 594) return null
    val type = this[0].toInt()
    val result = this[1].toInt()
    val offset = 2
    val size = 74
    val itemOffset = 2

    val list = mutableListOf<MeoofFeedPlanItem>()
    val feedPlan = MeoofFeedPlan(type, result, list)
    (0 until 8).forEach { index ->

        val trueWeek = index + 1

        var start = offset + index * size
        val end = start + size
        if ((end == this.size && index != 7) || (end > this.size)) {
            return feedPlan
        }
//        if (end >= this.size) {
//            return feedPlan
//        }
        val _week = this[start++].toInt()
        val week = if (_week != trueWeek) trueWeek else _week
        val status = this[start++].toInt() == 1
        val feedPlanItemInfoList = mutableListOf<MeoofFeedPlanItemInfo>()
        list.add(MeoofFeedPlanItem(week, status, feedPlanItemInfoList))

        (0 until 8).forEach { itemIndex ->
            val trueIndex = itemIndex + 1
            val item = this[start++].toInt()
            feedPlanItemInfoList.add(
                MeoofFeedPlanItemInfo(
                    if (item != trueIndex) trueIndex else item,
                    this[start++].toInt(),
                    this[start++].toInt(),
                    this[start++].toInt(),
                    this[start++].toInt(),
                    this[start++].toInt() == 1,
                    this[start++].toInt() == 1,
                )
            )
            start += itemOffset
        }

    }
    return feedPlan
}

/**解析开关状态*/
fun ByteArray?.meoofParseSwitchStatus(): MeoofSwitchStatus? {
    this ?: return null
    if (this.size < 4) return null

    return MeoofSwitchStatus(
        this[0].toInt(),
        this[1].toInt(),
        this[2].toInt(),
        this[3].toInt() == 1
    )
}

/**解析喂食状态*/
fun ByteArray?.meoofParseFeedStatus(): MeoofFeedStatus? {
    this ?: return null
    if (isEmpty()) return null
    return MeoofFeedStatus(this[0].toInt())
}

/**解析喂食、进食记录*/
fun ByteArray?.meoofParseFeedNotify(): MeoofFeedNotifu? {
    this ?: return null
    if (isEmpty()) return null
    return MeoofFeedNotifu(this[0].toInt())
}

/**解析摄像头状态*/
fun ByteArray?.meoofParseLiveStatys(): MeoofLiveStatus? {
    this ?: return null
    if (size < 3) return null
    return MeoofLiveStatus(
        this[0].toInt(),
        this[1].toInt(),
        this[2].toInt() == 0
    )
}

/**meoof   录像列表*/
fun ByteArray?.meoofRecordList(): MeoofRecrdList? {
    this ?: return null
    if (size < 12 * 12 + 4) return null

    val result = this.littleInt(0)
    val list = mutableListOf<MeoofRecordItemInfo>()

    val itemSize = 12
    val offset = 4

    (0 until 12).forEach { index->

        val start = index*itemSize+offset
        if(start + 12 > size) return MeoofRecrdList(result, list)

        val year = this.littleInt(start)
        val month = this.littleInt(start+4)
        val data = this.littleInt(start + 8)
        list.add(MeoofRecordItemInfo(year, month, data))
    }

    return MeoofRecrdList(result, list)
}
