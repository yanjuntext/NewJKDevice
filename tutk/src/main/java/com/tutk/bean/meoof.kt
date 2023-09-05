package com.tutk.bean

import java.time.Year

/**
 * @Author: wangyj
 * @CreateDate: 2023/7/21
 * @Description:
 */
/** 查询设备在线状态*/
data class MeoofStatus(val oneStatus: Int, val twoStatus: Int)

/**手动喂食*/
data class MeoofManualFeed(val result: Int) {
    fun isSuccess() = result == 0
}

/**喂食计划*/
data class MeoofFeedPlan(val type: Int, val result: Int, val list: MutableList<MeoofFeedPlanItem>){
    fun isSuccess() = result == 0
    fun isEdit() = type == 1
}

data class MeoofFeedPlanItem(
    val week: Int,
    val status: Boolean,
    val list: MutableList<MeoofFeedPlanItemInfo>
)

data class MeoofFeedPlanItemInfo(
    val index: Int,
    val left: Int,
    val right: Int,
    val hour: Int,
    val minute: Int,
    val sound: Boolean,
    val status: Boolean
)


/**meoof 开关状态*/
data class MeoofSwitchStatus(val type:Int,val result:Int,val index:Int,val value:Boolean){
    fun isSuccess() = result == 0
    fun isEdit() = type == 1
}
/**meoof  设备上报当前喂食状态*/
data class MeoofFeedStatus(val status:Int)

/**meoof  摄像头开关状态*/
data class MeoofLiveStatus(val result:Int,val type:Int,val status:Boolean){
    fun isSuccess() = result == 0
    fun isEdit() = type == 1
}
/**meoof  查询进食、喂食记录*/
data class MeoofFeedNotifu(val type:Int){
    fun isOutFood() = type == 0
}
/**meoof  录像列表*/
data class MeoofRecrdList(val result: Int,val list:MutableList<MeoofRecordItemInfo>){
    fun isSuccess() = result == 0
}

data class MeoofRecordItemInfo(val year: Int,val month:Int,val data:Int)
