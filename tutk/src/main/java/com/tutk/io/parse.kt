package com.tutk.io

import com.tutk.IOTC.*
import com.tutk.IOTC.camera.getString
import com.tutk.IOTC.camera.getUtfString
import com.tutk.IOTC.camera.getUtfString2
import com.tutk.IOTC.status.*
import com.tutk.bean.*
import java.util.*

/**
 * @Author: wangyj
 * @CreateDate: 2021/12/3
 * @Description:数据解析
 */

/**
 *解析获取移动侦测
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_REQ]
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_RESP]
 */
fun ByteArray?.parseGetMotionDetect(): MotionDetect? {
    if (this == null || this.size < 8) return null
    val motion = this.littleInt(4)
    return when {
        motion == 0 -> MotionDetect.OFF
        motion in 1..35 -> MotionDetect.LOW
        motion in 36..65 -> MotionDetect.MIDDLE
        motion in 66..95 -> MotionDetect.HIGH
        motion >= 96 -> MotionDetect.HIGHEST
        else -> null
    }
}

/**
 *解析设置移动侦测
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETMOTIONDETECT_REQ]
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETMOTIONDETECT_RESP]
 */
fun ByteArray?.parseSetMotionDetect(): TSetMotionDetect? {
    if (this == null || size < 4) return null
    return TSetMotionDetect(littleInt(0) == 0)
}


/**
 * 设备信息/TF卡信息
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_RESP]
 */
fun ByteArray?.parseDeviceInfo(): TDeviceInfo? {
    if (this == null || size < 48) return null
    val mode = 16.byteArray()
    val vender = 16.byteArray()
    System.arraycopy(this, 0, mode, 0, mode.size)
    System.arraycopy(this, 16, vender, 0, vender.size)
    val version = littleInt(32)
    val sdcardState = littleInt(36)
    val total = littleInt(40)
    val free = littleInt(44)

    val state = when (sdcardState) {
        SDCardStatus.ERROR.value -> SDCardStatus.ERROR
        SDCardStatus.FULL.value -> SDCardStatus.FULL
        SDCardStatus.INSERT.value -> SDCardStatus.INSERT
        SDCardStatus.RECORDING.value -> SDCardStatus.RECORDING
        else -> SDCardStatus.NONE
    }

    return TDeviceInfo(
        mode.getString(),
        vender.getString(),
        version.toVersion(),
        state,
        total,
        free
    )
}

internal fun Int.toVersion(): String {
    val one = (this ushr 24) and 0xff
    val two = (this ushr 16) and 0xff
    val three = (this ushr 8) and 0xff
    val four = (this) and 0xff
    return "${one}.${two}.${three}.${four}"
}

/**
 *解析 设置密码
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETPASSWORD_RESP]
 */
fun ByteArray?.parseResetPassword(): TResponseBean? {
    if (this == null || size < 4) return null
    return TResponseBean(littleInt(0) == 0)
}

/**
 *解析 扫描WIFI
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTWIFIAP_RESP]
 */
fun ByteArray?.parseScanWifi(): TScanWifi {
    Liotc.d("parseScanWifi", "size[${this?.size}]")
    if (this == null || this.size < 40) return TScanWifi(0, mutableListOf())
    var total = this.littleInt(0)
    Liotc.d("parseScanWifi", "total[$total]")
    val list = mutableListOf<TWifiInfo>()
    val offset = 4
    val eachSize = 36
    val size = total * eachSize + offset
    Liotc.d("parseScanWifi", "total length[$size]")
    if (this.size != size) {
        total = (this.size - offset) / eachSize
    }
    Liotc.d("parseScanWifi", "effective total[$total]")

    if (total > 0) {
        (0 until total).forEach { index ->
            val ssid = 32.byteArray()
            val start = index * eachSize + offset
            System.arraycopy(this, start, ssid, 0, ssid.size)
            val mode = this[start + ssid.size].toInt()
            val enctype = this[start + ssid.size + 1].toInt()
            val signal = this[start + ssid.size + 2].toInt()
            val status = this[start + ssid.size + 3].toInt()
            list.add(TWifiInfo(ssid.getString(), mode, enctype, signal, getWifiStatus(status)))
        }
    }
    return TScanWifi(total, list)
}

internal fun getWifiStatus(status: Int) = when (status) {
    WifiStatus.CONNECTED.value -> WifiStatus.CONNECTED
    WifiStatus.WRONG_PASSWORD.value -> WifiStatus.WRONG_PASSWORD
    WifiStatus.WEAK_SIGNAL.value -> WifiStatus.WEAK_SIGNAL
    WifiStatus.READTY.value -> WifiStatus.READTY
    else -> WifiStatus.NONE
}

/**
 * 当前连接WIFI的信息
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETWIFI_RESP]
 */
fun ByteArray?.parseGetWifi(): TWifiInfo? {
    if (this == null || size < 68) return null
    val ssid = ByteArray(32)
    val pwd = ByteArray(32)
    System.arraycopy(this, 0, ssid, 0, ssid.size)
    System.arraycopy(this, 32, pwd, 0, pwd.size)
    return TWifiInfo(
        ssid.getString(),
        this[64].toInt(),
        this[65].toInt(),
        this[66].toInt(),
        getWifiStatus(this[67].toInt()),
        pwd.getString()
    )
}

/**
 * 解析 设置WIFI
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETWIFI_RESP]
 */
fun ByteArray?.parseSetWifi(): TResponseBean? {
    if (this == null || size < 8) return null
    return TResponseBean(littleInt(0) == 0)
}


/**
 *解析 获取录像模式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETRECORD_RESP]
 */
fun ByteArray?.parseGetRecordMode(): TRecordMode {
    if (this == null || this.size < 8) return TRecordMode(-1, null)
    val result = this.littleInt(0)
    val mode = littleInt(4)
    val recordMode = when (mode) {
        RecordMode.OFF.value -> RecordMode.OFF
        RecordMode.FULL_TIME.value -> RecordMode.FULL_TIME
        RecordMode.ALARM.value -> RecordMode.ALARM
        else -> null
    }
    return TRecordMode(result, recordMode)
}

/**
 * 解析 设置录像模式
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETRECORD_RESP]
 */
fun ByteArray?.parseSetRecordMode(): TResponseBean? {
    if (this == null || size < 8) return null
    return TResponseBean(littleInt(0) == 0)
}

/**
 * 解析录像模式  支持定时录像
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_RESP]
 */
fun ByteArray?.parseRecordModeWithTime(): TRecordModeWithTime? {
    if (this == null || size < 12) return null
    val type = this[0].toInt()
    val mode = this[1].toInt()
    val limitTime = this[2].toInt()
    val scheduleIndex = this[3].toInt()

    val sTime = littleInt(4)
    val eTime = littleInt(8)
    val recordMode = when (mode) {
        RecordMode.OFF.value -> RecordMode.OFF
        RecordMode.ALARM.value -> RecordMode.ALARM
        RecordMode.FULL_TIME.value -> {
            if (eTime - sTime != 86400) {
                RecordMode.TIMING
            } else {
                RecordMode.FULL_TIME
            }
        }
        else -> RecordMode.OFF
    }
    return TRecordModeWithTime(type, recordMode, limitTime, scheduleIndex, sTime, eTime)
}


/**
 * 解析录像质量
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ]
 * @param type
 * @param cmd
 * @param quality 录像质量
 * @param cycle 循环录像
 */
fun ByteArray?.parseRecordQuality(): TRecordQuality? {
    if (this == null || size < 7) return null
    val type = littleInt(0)
    val cmd = this[4].toInt()
    val quality = this[5].toInt()
    val cycle = this[6].toInt() == 1
    val recordQuality =
        if (quality == RecordQuality.HD.value) RecordQuality.HD else RecordQuality.SD
    return TRecordQuality(type, cmd, recordQuality, cycle)
}

/**
 * 解析录像事件
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTEVENT_REQ]
 */
fun ByteArray?.parseRecordVideoEvent(): TRecordVideoInfo? {
    if (this == null || size < 12) return null
    val channel = littleInt(0)
    val total = littleInt(4)
    val index = this[8].toInt()
    val end: Boolean = this[9].toInt() == 1
    var count = this[10].toInt()
    Liotc.d("parseRecordVideoEvent", "total[$total],index=$index,end=$end,count=$count")
    val offset = 12
    val eachSize = 12
    val sCount = (size - offset) / eachSize
    if(sCount != count && count != 0){
        count = sCount
    }

    Liotc.d("parseRecordVideoEvent", "count[$count]")
    val list = mutableListOf<TEvent>()
    //设备返回的时间是零时区的时间
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("gmt"))
    (0 until count).forEach {
        val start = it * eachSize + offset

        val year = littleShort(start).toInt()
        val month = this[start + 2].toInt()
        val day = this[start + 3].toInt()
        val wDay = this[start + 4].toInt()
        val hour = this[start + 5].toInt()
        val minute = this[start + 6].toInt()
        val second = this[start + 7].toInt()
        calendar.set(year, month - 1, day, hour, minute, second)

        val buf = ByteArray(8)
        System.arraycopy(this, start, buf, 0, buf.size)

        val event = this[start + 8].toInt()
        val status = this[start + 9].toInt()

        if (event == EventMode.ALL.value
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_MOTIONDECT
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_VIDEOLOST
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_IOALARM
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_MOTIONPASS
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_VIDEORESUME
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_IOALARMPASS
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_EXPT_REBOOT
            || event == AVIOCTRLDEFs.AVIOCTRL_EVENT_SDFAULT
        ) {
            list.add(TEvent(buf, calendar.timeInMillis, event, status))
        }
    }
    return TRecordVideoInfo(channel, total, index, end, count, list)
}

/**
 * 解析设备时区 获取/设置
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_RESP]
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIMEZONE_RESP]
 */
fun ByteArray?.parseTimeZone(): TTimeZone? {
    if (this == null || size < 4) return null
    val cbSize = littleInt(0)
    Liotc.d("parseTimeZone", "size=${size},cbSize=$cbSize")
    if (size < cbSize) return null
    val supportTimeZone = littleInt(4) == 1
    val gmtDiff = littleInt(8)
    return TTimeZone(cbSize, supportTimeZone, gmtDiff)
}

fun ByteArray?.parseSyncTime(): TSyncTime? {
    if (this == null || size < 8) return null
    return TSyncTime(littleInt(0) == 0)
}

/**
 * 解析 指示灯 状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_RESP]
 */
fun ByteArray?.parseLedStatus(): TLedStatus? {
    if (this == null || size < 12) return null
    val type = littleInt(0)
    val result = littleInt(4)
    val status = littleInt(8)
    return TLedStatus(type, result, status == 1)
}

/**
 * 解析视频状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_CAMERA_RESP]
 */
fun ByteArray?.parseCameraVideoStatus(): TCameraStatus? {
    if (this == null || size < 5) return null
    val cMode = this[0].toInt()
    val value = this[4].toInt() and 0xFF
    val mode = when (cMode) {
        CameraVideoMode.BRIGHT.status -> CameraVideoMode.BRIGHT
        CameraVideoMode.CONTRAST.status -> CameraVideoMode.CONTRAST
        CameraVideoMode.SATURATION.status -> CameraVideoMode.SATURATION
        else -> CameraVideoMode.CHROMA
    }
    return TCameraStatus(mode, value)
}

/**
 * 解析 夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_RESP]
 * 0 白天 1 夜视 2 自动
 */
fun ByteArray?.parseIrLedStatus(): TIRLedStatus? {
    if (this == null || size < 3) return null
    val type = this[0].toInt()
    val auto = this[1].toInt() == 1
    val status = this[2].toInt() == 1
    return TIRLedStatus(
        type,
        if (auto) IrLedStatus.AUTO else if (status) IrLedStatus.OPEN else IrLedStatus.OFF
    )
}

/**
 * 解析视频镜像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOMODE_RESP]
 */
fun ByteArray?.parseGetVideoMirror(): TGetVideoMirror? {
    if (this == null || this.size < 8) return null
    val channel = littleInt(0)
    val mode = this[4].toInt()
    val mirrorMode = when (mode) {
        VideoMirrorMode.MIRROR_UP.value -> VideoMirrorMode.MIRROR_UP
        VideoMirrorMode.MIRROR_LEFT.value -> VideoMirrorMode.MIRROR_LEFT
        VideoMirrorMode.MIRROR_ALL.value -> VideoMirrorMode.MIRROR_ALL
        else -> VideoMirrorMode.MIRROR_NORMAL
    }
    return TGetVideoMirror(channel, mirrorMode)
}

/**
 * 解析 设置视频镜像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOMODE_RESP]
 * */
fun ByteArray?.parseSetVideoMirror(): TSetVideoMirror? {
    if (this == null || size < 8) return null
    return TSetVideoMirror(littleInt(0), this[4].toInt() == 0)
}

/**
 * 解析 格式化SDCard
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_RESP]
 */
fun ByteArray?.parseFormatSdCard(): TFormatSdCard? {
    if (this == null || size < 8) return null
    val storage = littleInt(0)
    return TFormatSdCard(storage, this[4].toInt() == 0)
}

/**
 * 解析 视频回放
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP]
 */
fun ByteArray?.parsePlayBack(): TPlayback? {
    if (this == null || size < 12) return null
    val type = littleInt(0)
    val channel = littleInt(4)
    val time = littleInt(8)

    val status = when (type) {
        PlaybackStatus.PAUSE.status -> PlaybackStatus.PAUSE
        PlaybackStatus.STOP.status -> PlaybackStatus.STOP
        PlaybackStatus.STEPFORWARD.status -> PlaybackStatus.STEPFORWARD
        PlaybackStatus.STEPBACKWARD.status -> PlaybackStatus.STEPBACKWARD
        PlaybackStatus.FORWARD.status -> PlaybackStatus.FORWARD
        PlaybackStatus.BACKWARD.status -> PlaybackStatus.BACKWARD
        PlaybackStatus.SEEKTIME.status -> PlaybackStatus.SEEKTIME
        PlaybackStatus.END.status -> PlaybackStatus.END
        PlaybackStatus.START.status -> PlaybackStatus.START
        PlaybackStatus.PLAYING.status -> PlaybackStatus.PLAYING
        PlaybackStatus.ERROR.status -> PlaybackStatus.ERROR
        else -> null
    }
    return TPlayback(status, channel, time, type)
}

/**
 * 解析推送地址
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_RESP]
 * @param get 是否是获取
 * @param result 是否成功
 * @param ip  IP地址 ru:47.90.57.61
 * @param port 端口号  8080
 * @param path PW_Server/server.php
 */
fun ByteArray?.parsePush(): TPushUrl? {
    if (this == null || size < 56) return null
    val get = littleInt(0) == 0
    val result = littleInt(4) == 0
    val ip = ByteArray(24)
    System.arraycopy(this, 8, ip, 0, ip.size)
    val port = littleInt(32)
    val path = ByteArray(20)
    System.arraycopy(this, 36, path, 0, path.size)
    val _ip = ip.getString()
    val _path = path.getString()
    return TPushUrl(get, result, ip.getString(), port, path.getString(), "${_ip}:${port}/${_path}")
}

/**
 * 解析设备版本号
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_RESP]
 */
fun ByteArray?.parseDeviceVersionInfo(): TDeviceVersionInfo? {
    if (this == null || this.size < 72) return null
    //固件版本号
    val system = ByteArray(16)
    //UI版本号
    val ui = ByteArray(16)

    val systemLatest = ByteArray(16)

    val uiLatest = ByteArray(16)

    val type = this.littleInt(0)
    val result = this.littleInt(4)


    System.arraycopy(this, 8, ui, 0, 16)
    System.arraycopy(this, 24, system, 0, 16)
    System.arraycopy(this, 40, uiLatest, 0, 16)
    System.arraycopy(this, 56, systemLatest, 0, 16)

    return TDeviceVersionInfo(
        type,
        result,
        systemVersion = system.getString(),
        uiVersion = ui.getString(),
        systemVersionLatest = systemLatest.getString(),
        uiVersionLatest = uiLatest.getString()
    )

}

/**
 * 解析喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_RESP]
 * @param total 一共几餐
 */
fun ByteArray?.parseFeedPlan(total: Int = 8): TFeedPlanInfo? {
    val list = mutableListOf<TFeedPlan>()
    if (this == null) return null
    val result = this[0].toInt()
    val dataSize = this[4].toInt()
    Liotc.d("parseFeedPlan", "result=$result,size=$dataSize")
    if (dataSize > this.size) return null

    val cmdType = this[10].toInt()
    val cmdSize = this[11]
    Liotc.d("parseFeedPlan", "cmdType=$cmdType,cmdSize=$cmdSize")
    val offset = 8
    val time = this.littleInt(12)
    val size = cmdSize + 4
    if (cmdType == AVIOCTRLDEFs.TTY_CMD_GET_TIMING_REQ && cmdSize > 0) {
        (0 until total).forEach { index ->
            val start = index * size + offset
            if (start > this.size || start + size > this.size) return@forEach
            var week = this[start + 6].toInt()

            if (week < 0) week = 0

            var hour = this[start + 7].toInt()

            if (hour < 0 || hour >= 24) hour = 0

            var min = this[start + 8].toInt()
            if (min < 0 || min >= 60) min = 0

            var num = this.littleShort(start + 9)
            if (num < 0) num = 0

            val enable = (this[start + 11].toInt() and 0x01) == 1
            val id = this[start + 12].toInt()
            val musicIndex = this[start + 13].toInt()
            if (id > 0) {
                val plan = TFeedPlan(id, week, hour, min, num = num.toInt(), enable, musicIndex)
                Liotc.d("parseFeedPlan", "plan=$plan")
                list.add(plan)
            }
        }
    }
    return TFeedPlanInfo(
        result == 0,
        cmdType == AVIOCTRLDEFs.TTY_CMD_GET_TIMING_REQ,
        list as ArrayList<TFeedPlan>
    )
}

/**
 * 解析喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_RESP]
 */
fun ByteArray?.parseFeedPlan2(): TFeedPlanInfo2? {
    if (this == null || this.size < 8) return null
    //结果
    val result = this[0].toInt()
    //是否是获取
    val isGet = this[4].toInt() == 0
    //总条数
    val total = this[8].toInt()

    val offset = 12
    //每条计划数据内容长度
    val size = 60
    val list = mutableListOf<TFeedPlan2>()

    if (size * total + offset < this.size) return null

    (0 until total).forEach {
        val start = it * size + offset

        val id = this[start].toInt()
        val week = this[start + 10].toInt()
        val hour = this[start + 11].toInt()
        val min = this[start + 12].toInt()
        val num = this.littleShort(start + 13).toInt()

        val isEnable = ((this[start + 15].toInt()) and 0x01) == 1
        val index = this[start + 16].toInt()
        val musicIndex = this[start + 17].toInt()
        val smallTank = this[start + 18].toInt()
        val change = (this[start + 19].toInt())
        val _alais = ByteArray(40)
        System.arraycopy(this, start + 20, _alais, 0, _alais.size)
        val alias = _alais.getUtfString2()
        list.add(
            TFeedPlan2(
                id,
                week,
                hour,
                min,
                num,
                isEnable,
                index,
                musicIndex,
                smallTank,
                change,
                alias
            )
        )
    }
    return TFeedPlanInfo2(result, isGet, list = list as ArrayList<TFeedPlan2>)
}


/**
 * 修改喂食计划  带名称的喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_RESP]
 * */
fun ByteArray?.parseFeedPlanWithName():TFeedPlanWithName?{
    this?:return null
    kotlin.runCatching {
        val result = littleInt(0)
        val type = littleInt(4)
        val num = littleInt(8)
        val offset = 12
        val infoSize = 24
        val infos = mutableListOf<TFeedPlanWithNameFeedInfo>()
        val names = mutableListOf<String>()
        (0 until num).forEach { index->
            val start = infoSize* index + offset
            val week = littleInt(start)
            val hour = littleInt(start + 4)
            val minuter = littleInt(start + 8)
            val number = littleInt(start + 12)
            val enable = littleInt(start + 16) == 1
            val audio = littleInt(start + 20)
            infos.add(TFeedPlanWithNameFeedInfo(week,hour,minuter,number,enable, audio))
            Liotc.d("parseFeedPlanWithName", "info=${infos[infos.size-1]}")
        }
        val nameOffset = infoSize * 10 + offset
        val nameSize = 40
        (0 until num).forEach {index->
            val name = ByteArray(nameSize)
            System.arraycopy(this,index* nameSize + nameOffset,name,0,nameSize)
            names.add(name.getUtfString())
            Liotc.d("parseFeedPlanWithName", "info name=${names[names.size-1]}")
        }
        Liotc.d("parseFeedPlanWithName", TFeedPlanWithName(result,type,num,infos,names).toString())
        return TFeedPlanWithName(result,type,num,infos,names)
    }.onFailure {
        Liotc.d("parseFeedPlanWithName", "parse error=${it.message}")
    }

    return null
}

/**
 * 解析事件记录 设备主动推送过来的
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_RESP]
 */
fun ByteArray?.parseEventReport(): TEventReport? {
    if (this == null) return null

    return try {
        val time = this.littleInt(8)
        val eventType = this.littleInt(16)
        val _id = ByteArray(16)
        System.arraycopy(this, 20, _id, 0, 16)
        val _devName = ByteArray(16)
        System.arraycopy(this, 36, _devName, 0, 16)
        val _picName = ByteArray(32)
        System.arraycopy(this, 52, _picName, 0, 32)

        val feedType = this[84].toInt()

        val alarmReson = this[85].toInt()

        val feedWeight = this.littleShort(86)

        TEventReport(
            time.toLong(),
            eventType,
            _id.getUtfString(),
            _devName.getUtfString(),
            _picName.getUtfString(),
            feedType,
            alarmReson,
            feedWeight.toInt()
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}

/**
 * 解析wifi信号强度
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_RESP]
 */
fun ByteArray?.parseWifiSignal(): Int {
    if (this == null || this.size < 4) return 0
    return this.littleInt(0)
}

/**
 * 解析OSD 获取状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOOSD_RESP]
 */
fun ByteArray?.parseGetOsdStatus(): TOsdGetStatus? {
    if (this == null || this.size < 5) return null
    return TOsdGetStatus(this[0].toInt() == 1)
}

/**
 * 解析OSD 设置状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOOSD_RESP]
 */
fun ByteArray?.parseSetOsdStatus(): TOsdSetStatus? {
    if (this == null || this.size < 8) return null
    return TOsdSetStatus(this.littleInt(0))
}

/**
 * 解析 童锁状态
 * [AVIOCTRLDEFs.IOTYPE_USER_SET_CHILDREN_LOCK_RESP] or [AVIOCTRLDEFs.IOTYPE_USER_GET_CHILDREN_LOCK_RESP]
 */
fun ByteArray?.parseChildrenLock(): TChildrenLock? {
    if (this == null || this.size < 8) return null
    return TChildrenLock(this.littleInt(0), this[4].toInt() == 1)
}

/**
 * 解析 复位设备
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVRESET_RESP]
 */
fun ByteArray?.parseResetDevice(): TResetDevice? {
    if (this == null || this.size < 4) return null
    return TResetDevice(this.littleInt(0))
}

/**
 *  [AVIOCTRLDEFs.IOTYPE_USER_NOSLEEP_MODE_RESP]
 * 低功耗设备模式切换
 */
fun ByteArray?.parseNosLeep():TNosLeep?{
    if (this == null || this.size < 6) return null
    return TNosLeep(this.littleInt(0),this[4].toInt(),this[5].toInt() == 1)
}


