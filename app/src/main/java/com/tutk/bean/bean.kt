package com.tutk.bean

import com.tutk.IOTC.AVIOCTRLDEFs
import com.tutk.IOTC.status.*


/**
 * @Author: wangyj
 * @CreateDate: 2021/12/3
 * @Description: 设备端返回的数据
 */

open class TBean(val cmd: Int, val result: Int)

data class TResponseBean(val resule: Boolean)

/**
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_RESP]
 */
data class TSupportStream(val index: Int, val channel: Int)

/**
 * 设置移动侦测
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETMOTIONDETECT_RESP]
 * @param result 设置结果  0: success; otherwise: failed.
 */
data class TSetMotionDetect(val result: Boolean)

/**
 *设备信息
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_RESP]
 * @param model 型号
 * @param vender 厂商
 * @param version 版本号
 * @param sdcardState TF卡状态
 * @param total TF卡总容量 MB
 * @param free TF卡空闲容量 MB
 */
data class TDeviceInfo(
    val model: String?, val vender: String?, val version: String?,
    val sdcardState: SDCardStatus, val total: Int, val free: Int
)

/**
 * 扫描WIFI
 * @param total 总条数
 * @param list wifi信息
 */
data class TScanWifi(val total: Int, val list: MutableList<TWifiInfo>)

/**
 *WIFI信息
 * @param ssid wifi名称
 * @param mode
 * @param enctype 加密类型
 * @param signal 信号轻度 （如果值大于0 ，则是实际的信号强度+110）
 * @param status 连接状态
 */
data class TWifiInfo(
    val ssid: String?,
    val mode: Int,
    val enctype: Int,
    val signal: Int,
    val status: WifiStatus,
    val pwd: String? = null
)

/**
 * 录像模式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETRECORD_RESP]
 * @param result
 * @param mode 录像模式
 */
data class TRecordMode(val result: Int, val mode: RecordMode?)

/**
 * 录像模式 支持定时录像
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
 * @param type 方式 0是获取  1是设置
 * @param mode 录像方式
 * @param limitTime 录像文件时长
 * @param scheduleIndex
 * @param startTime 开始时间
 * @param endTime 结束时间
 */
data class TRecordModeWithTime(
    val type: Int, val mode: RecordMode, val limitTime: Int,
    val scheduleIndex: Int, val startTime: Int, val endTime: Int
)

/**
 * 录像质量
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ]
 * @param type
 * @param cmd
 * @param quality 录像质量
 * @param cycle 循环录像
 */
data class TRecordQuality(
    val type: Int,
    val cmd: Int,
    var quality: RecordQuality,
    var cycle: Boolean
)

/**
 * 录像事件
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTEVENT_REQ]
 * @param channel
 * @param total 录像总个数
 * @param index 第几包 package index, 0,1,2... because avSendIOCtrl() send package up to 1024 bytes one time, you may want split search results to serveral package to send.
 * @param end 是否结束
 * @param count 当前包的视频个数
 */
data class TRecordVideoInfo(
    val channel: Int,
    val total: Int,
    val index: Int,
    val end: Boolean,
    val count: Int,
    val video: MutableList<TEvent>
)

/**
 * 事件
 * @param time 事件时间
 * @param event 事件类型
 * @param status // 0x00: Recording file exists, Event unreaded
 *              // 0x01: Recording file exists, Event readed
 *             // 0x02: No Recording file in the event
 */
data class TEvent(val buf: ByteArray, val time: Long, val event: Int, val status: Int)

/**
 * 设备时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_RESP]
 * @param size the following package size in bytes, should be sizeof(SMsgAVIoctrlTimeZone)
 * @param supportTimeZone 是否支持  device is support TimeZone or not, 1: Supported, 0: Unsupported.
 * @param gmtDiff 当前时区与零时区偏移事件 the difference between GMT in hours
 */
data class TTimeZone(var size: Int, var supportTimeZone: Boolean, var gmtDiff: Int)

/**
 * 同步时间、时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_SYNC_RESP]
 */
data class TSyncTime(val result: Boolean)

/**
 * 设备推送地址
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ]
 * @example 120.24.215.192:8088/OuCamRaise
 * @param type 类型 0-GET, 1-SET
 * @param result 结果 0-ok, orthers-failed
 * @param ip   120.24.215.192
 * @param port  8088
 * @param path  OuCamRaise
 */
data class TDevicePushServiceUrl(
    val type: Int,
    val result: Int,
    val ip: String,
    val port: Int,
    val path: String
)

/**
 * 指示灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_RESP]
 * @param type 1-Get 0-Set
 * @param result 0-Success other-Fail
 * @param status
 */
data class TLedStatus(val type: Int, val result: Int, val status: Boolean)

/**
 * 设备视频状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_CAMERA_RESP]
 * @param mode
 * @param value
 */
data class TCameraStatus(val mode: CameraVideoMode, val value: Int)

/**
 * 夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_RESP]
 * @param type 0-Get 1-Set
 * @param status
 */
data class TIRLedStatus(val type: Int, val status: IrLedStatus)

/**
 * 获取视频镜像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOMODE_RESP]
 */
data class TGetVideoMirror(val channel: Int, var mode: VideoMirrorMode)

/**
 * 设置视频镜像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOMODE_RESP]
 * @param channel
 * @param success
 */
data class TSetVideoMirror(val channel: Int, val success: Boolean)

/**
 * 格式化SDCard
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_RESP]
 * @param storage //Storage index (ex. sdcard slot = 0, internal flash = 1, ...)
 * @param result  // 0: success // -1: format command is not supported. // otherwise: failed.
 */
data class TFormatSdCard(val storage: Int, val result: Boolean)

/**
 * TFCard 录像回放
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP]
 * @param type 回放状态
 * @param channel 回放结果  [type]==[PlaybackStatus.START] channel是通道，取值范围[0,31]  [channel]>=0   real channel no used by device for playback
 * @param time 时间  //if [type]==[PlaybackStatus.START] time is recordVideo total time (ms)
 * if [type]==[PlaybackStatus.SEEKTIME] time is Percent 50%=50
 * @param iType //if [type] is NULL
 */
data class TPlayback(val type: PlaybackStatus?, val channel: Int, val time: Int, val iType: Int)

/**
 * 解析推送地址
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_RESP]
 * @param get 是否是获取
 * @param result 是否成功
 * @param ip  IP地址 ru:47.90.57.61
 * @param port 端口号  8080
 * @param path PW_Server/server.php
 */
data class TPushUrl(
    val get: Boolean,
    val result: Boolean,
    val ip: String?,
    val port: Int,
    val path: String?,
    var url: String?
)

/**
 * 解析喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_RESP]
 * @param result 结果
 * @param isGet 是否是获取
 * @param list 喂食计划
 */
data class TFeedPlanInfo(val result: Boolean, val isGet: Boolean, val list: ArrayList<TFeedPlan>)

/**
 * 解析喂食计划 喂食计划详细信息
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_RESP]
 * @param index id
 * @param week 喂食星期
 * @param hour 喂食小时
 * @param min 喂食分钟
 * @param num 喂食份数
 * @param isEnable 是否喂食
 * @param musicIndex 喂食音频
 */
data class TFeedPlan(
    val index: Int,
    val week: Int,
    val hour: Int,
    val min: Int,
    val num: Int,
    val isEnable: Boolean,
    val musicIndex: Int
)

/**
 * 解析喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_RESP]
 * @param result 结果，0成功，其他失败
 * @param isGet 是否是获取
 * @param list 喂食计划
 */
data class TFeedPlanInfo2(val result: Int, val isGet: Boolean, val list: ArrayList<TFeedPlan2>)

/**
 * 解析喂食计划 喂食计划详细信息
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_RESP]
 * @param id 喂食计划ID
 * @param week 喂食星期
 * @param hour 喂食小时
 * @param min 喂食分钟
 * @param num 喂食重量
 * @param isEnable 是否开启
 * @param index 第几餐
 * @param musicIndex 喂食音频id
 * @param smallTank 是否是小份
 * @param change 当前喂食计划是否更改 修改当前喂食计划是值为1，否则为0
 * @param alias 喂食计划别名
 */
data class TFeedPlan2(
    val id: Int,
    var week: Int,
    var hour: Int,
    var min: Int,
    var num: Int,
    var isEnable: Boolean,
    val index: Int,
    var musicIndex: Int,
    var smallTank: Int,
    var change: Int,
    var alias: String
)

/**
 * 解析设备版本号
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_RESP]
 *  @param type 0:仅检查check  1:仅升级system 2:仅升级ui 3:升级system and ui  7:仅升级mcu
 *  @param result 1：系统版本可以升级   2：UI（单片机）版本可以升级  3:系统和单片机都可以升级
 *  @param systemVersion 系统版本
 *  @param uiVersion ui(单片机)版本
 *  @param systemVersionLatest 最新系统版本
 *  @param uiVersionLatest 最新UI版本或MCU版本
 */
data class TDeviceVersionInfo(
    val type: Int,
    val result: Int,
    val systemVersion: String,
    val uiVersion: String,
    val systemVersionLatest: String,
    val uiVersionLatest: String
)

/**
 * 解析事件记录
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVICE_USER_EVENT_REPORT]
 */
data class TEventReport(
    val time: Long,
    val eventType: Int,
    val id: String,
    val devName: String,
    val picName: String,
    val feedType: Int,
    val alarmReson: Int,
    val feedWeight: Int
)

/**
 * 童锁
 * [AVIOCTRLDEFs.IOTYPE_USER_GET_CHILDREN_LOCK_RESP]/[AVIOCTRLDEFs.IOTYPE_USER_SET_CHILDREN_LOCK_RESP]
 * @param result 请求是否成功
 * @param status 童锁状态 true:童锁开启  false:童锁关闭
 */
data class TChildrenLock(
    val result: Int,
    val status: Boolean
) {
    fun isSuccess() = result == 0
}

/**
 * OSD 设置状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOOSD_RESP]
 */
data class TOsdSetStatus(val result: Int) {
    fun isSuccess() = result == 0
}

/**
 * OSD 获取状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOOSD_RESP]
 */
data class TOsdGetStatus(val status: Boolean)

/**
 * 复位设备
 *  [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVRESET_RESP]
 */
data class TResetDevice(val result: Int) {
    fun isSuccess() = result == 0
}
/**
 *  [AVIOCTRLDEFs.IOTYPE_USER_NOSLEEP_MODE_RESP]
 * 低功耗设备模式切换
 */
data class TNosLeep(val result: Int,val type:Int,val status: Boolean){
    fun isGet() = type == 0 && isSuccess()
    fun isSuccess() = result == 0
}