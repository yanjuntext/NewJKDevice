package com.tutk.io

import com.tutk.IOTC.AVIOCTRLDEFs
import com.tutk.IOTC.Camera
import com.tutk.IOTC.Liotc
import com.tutk.IOTC.camera.VideoQuality
import com.tutk.IOTC.littleByteArray
import com.tutk.IOTC.status.*
import com.tutk.bean.TEvent
import com.tutk.bean.TFeedPlan2
import com.tutk.bean.TFeedPlanWithNameFeedInfo
import com.tutk.utils.getDiffMinute0Zone
import java.util.*

/**
 * @Author: wangyj
 * @CreateDate: 2021/12/1
 * @Description: 发送IO命令
 * @param must 不进行在线状态判断
 */

internal fun Camera?.canSend() = this != null && isSessionConnected()

/**
 * 获取支持的流
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_REQ]
 */
fun Camera?.getSupportStream(channel: Int = Camera.DEFAULT_AV_CHANNEL) {
    this?.sendIOCtrl(
        channel,
        AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_REQ,
        AVIOCTRLDEFs.getSupportStream()
    )
}

/**
 *获取视频清晰度
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_REQ]
 */
fun Camera?.getVideoQuality(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETSTREAMCTRL_REQ,
            AVIOCTRLDEFs.getStreamCtrl(channel)
        )
        true
    } else false
}

/**
 * 设置视频清晰度
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ]
 */
fun Camera?.setVideoQuality(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    videoQuality: VideoQuality, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ,
            AVIOCTRLDEFs.setSteamCtrl(channel, videoQuality.value)
        )
        true
    } else false
}

/**
 *获取音频编码方式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ]
 */
fun Camera?.getAudioCodec(channel: Int = Camera.DEFAULT_AV_CHANNEL) {
    this?.sendIOCtrl(
        channel,
        AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ,
        AVIOCTRLDEFs.getAudioCodec()
    )
}


/**
 *下载文件 命令
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DOWNLOAD_FILE_REQ]
 */
fun Camera?.sendDownloadFileOrder(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DOWNLOAD_FILE_REQ,
            AVIOCTRLDEFs.getDownloadFile()
        )
        true
    } else false
}

/**
 *通知设备开始发送音频文件
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_REQ]
 * @param name 文件的名称
 * @param alias 文件的别名
 * @param tracks 音频的时长
 * @param time 文件录制的时间戳(秒)
 */
fun Camera?.sendSendFileOrder(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    name: String?,
    alias: String?,
    tracks: Int,
    time: Long, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        AVIOCTRLDEFs.getAudioFileInfo(name, alias, tracks, time)?.let { data ->
            this?.sendIOCtrl(
                channel,
                AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_REQ,
                data
            )
        }
        true
    } else false
}

/**
 * 获取移动侦测
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETMOTIONDETECT_REQ]
 */
fun Camera?.getMotionDetect(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETMOTIONDETECT_REQ,
            AVIOCTRLDEFs.getMotionDetect(channel)
        )
        true
    } else false
}

/**
 *设置移动侦测
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETMOTIONDETECT_REQ]
 * @param motion 移动侦测强度
 */
fun Camera?.setMotionDetect(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    motion: MotionDetect, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETMOTIONDETECT_REQ,
            AVIOCTRLDEFs.setMotionDetect(channel, motion)
        )
        true
    } else false
}

/**
 * 获取设备信息/TF卡信息
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_REQ]
 */
fun Camera?.getDeviceInfo(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_REQ, AVIOCTRLDEFs.getDeviceInfo()
        )
        true
    } else false
}

/**
 * 设置新密码
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETPASSWORD_REQ]
 * @param oldPwd 旧密码
 * @param newPwd 新密码
 */
fun Camera?.resetPassword(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    oldPwd: String?, newPwd: String?, must: Boolean = false
): Boolean {
    return if ((canSend() && !oldPwd.isNullOrEmpty() && !newPwd.isNullOrEmpty()) || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETPASSWORD_REQ,
            AVIOCTRLDEFs.resetPassword(oldPwd ?: "", newPwd ?: "")
        )
        true
    } else false
}

/**
 * 扫描WIFI
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTWIFIAP_REQ]
 */
fun Camera?.scanWifi(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    Liotc.d("scanWifi", "supportScanWifi[${this?.getAvChannel(channel)?.supportScanWifi()}]")
    return if ((canSend() && this?.getAvChannel(channel)?.supportScanWifi() == true) || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTWIFIAP_REQ,
            AVIOCTRLDEFs.scanWifi()
        )
        true
    } else false
}

/**
 * 获取当前连接的wifi信息
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETWIFI_REQ]
 */
fun Camera?.getWifi(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETWIFI_REQ,
            AVIOCTRLDEFs.scanWifi()
        )
        true
    } else false
}

/**
 * 设置WIFI
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETWIFI_REQ]
 * @param ssid wifi 名称
 * @param pwd wifi密码
 * @param enctype 加密类型 if pwd is empty,enctype=[com.tutk.IOTC.AVIOCTRLDEFs.AVIOTC_WIFIAPENC_NONE],
 * else enctype=[com.tutk.IOTC.AVIOCTRLDEFs.AVIOTC_WIFIAPENC_AUTO_ADAPT]
 */
fun Camera?.setWifi(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    ssid: String,
    pwd: String,
    enctype: Int = AVIOCTRLDEFs.AVIOTC_WIFIAPENC_AUTO_ADAPT, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val _ssid = ssid.toByteArray()
        val _pwd = pwd.toByteArray()
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETWIFI_REQ,
            AVIOCTRLDEFs.setWifi(
                _ssid,
                _pwd,
                enctype = enctype,
                resverd = if (enctype == AVIOCTRLDEFs.AVIOTC_WIFIAPENC_AUTO_ADAPT) AVIOCTRLDEFs.AVIOTC_WIFIAPENC_AUTO_ADAPT else 0
            )
        )
        true
    } else {
        false
    }
}

/**
 * 设置录像模式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETRECORD_REQ]
 * @param channel
 * @param mode 录像模式 如果 mode=[com.tutk.IOTC.status.RecordMode.TIMING],请使用[setRecordModeWithTime]
 */
fun Camera?.setRecordMode(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    mode: RecordMode,
    must: Boolean = false
): Boolean {
    if (mode == RecordMode.TIMING) {
        throw RuntimeException("The current command does not support timing recording，please ")
    }
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETRECORD_REQ,
            AVIOCTRLDEFs.setRecordMode(channel, mode)
        )
        true
    } else false
}

/**
 * 获取录像模式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETRECORD_REQ]
 * 如果设备支持定时录像，请使用 [getRecordModeWithTime]
 */
fun Camera?.getRecordMode(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GETRECORD_REQ,
            AVIOCTRLDEFs.getRecordMode(channel)
        )
        true
    } else false
}

/**
 * 设置录像模式 支持定时录像
 *[com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
 * @param mode 录像模式
 * @param limit 录像时长 每个录像文件的时长，取值范围[1,20]
 * @param curScheduleIndex
 * @param startTime 开始录像时间 秒
 * @param endTime 结束录像时间 秒
 */
fun Camera?.setRecordModeWithTime(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    mode: RecordMode,
    limitTime: Int,
    curScheduleIndex: Int,
    startTime: Int = 0,
    endTime: Int = 86400, must: Boolean = false
): Boolean {
    if (mode == RecordMode.ALARM && (limitTime < 1 || limitTime > 20)) {
        throw RuntimeException("alarm record mode,limitTime in [1,20]")
    }
    if (startTime > endTime) {
        throw RuntimeException("startTime must be less than endTime")
    }
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ,
            AVIOCTRLDEFs.setRecordModeWithTime(
                mode,
                limitTime,
                curScheduleIndex,
                startTime,
                endTime
            )
        )
        true
    } else false
}

/**
 * 获取录像模式 支持定时录像
 *[com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
 */
fun Camera?.getRecordModeWithTime(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ,
            AVIOCTRLDEFs.getRecordModeWithTime()
        )
        true
    } else false
}

/**
 *获取录像质量
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ]
 */
fun Camera?.getRecordQuality(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ,
            AVIOCTRLDEFs.recordQuality(0, 0, RecordQuality.HD, false)
        )
        true
    } else false
}

/**
 * 设置录像质量
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ]
 * @param quality 录像质量
 * @param cycle 循环录像
 */
fun Camera?.setRecordQuality(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    quality: RecordQuality,
    cycle: Boolean, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_RECORD_SETTING_REQ,
            AVIOCTRLDEFs.recordQuality(1, 1, quality, cycle)
        )
        true
    } else false
}

/**
 * 获取录像列表
 * 获取录像列表有两种方式，一种是该方式，查询指定时间段内的视频;
 * 另一种是通过下载文件的方式，获取全部录像文件,详见[com.tutk.IOTC.Camera.downFile],srcFile=[com.tutk.IOTC.camera.FileDirectory.RECORD_VIDEO_DIRECTORY]
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTEVENT_REQ]
 */
fun Camera?.getTFCardRecordVideoList(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    startTime: Long,
    endTime: Long, must: Boolean = false
): Boolean {
    if (startTime > endTime) {
        throw RuntimeException("startTime must be less than endTime")
    }
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTEVENT_REQ,
            AVIOCTRLDEFs.getEventList(channel, startTime, endTime, EventMode.ALL.value, 0)
        )
        true
    } else false
}

/**
 * 删除TFCard指定录像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_EDIT_FILES_REQ]
 * @param info 指定录像
 */
fun Camera?.deleteTFCardRecordVideo(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    info: TEvent,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_EDIT_FILES_REQ,
            AVIOCTRLDEFs.deleteTFRecordVideo(channel, info.buf)
        )
        true
    } else false
}

/**
 * 获取设备时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ]
 * @param channel
 * @param must true：不需要判断在线状态
 */
fun Camera?.getTimeZone(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {

        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ,
            AVIOCTRLDEFs.getTimeZone()
        )
        true
    } else false
}

/**
 * 设置设备时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIMEZONE_REQ]
 * @param gmtDiff 当前时区与零时区偏移分钟 the difference between GMT in hours
 */
fun Camera?.setTimeZone(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    gmtDiff: Int,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIMEZONE_REQ,
            AVIOCTRLDEFs.setTimeZone(this.supportTimeZone(), gmtDiff)
        )
        true
    } else false
}

/**
 * 设置时间 设备自己判断是否同步时区，如果设备时间和设置时间偏差较大，设备可能会同步时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_REQ]
 */
fun Camera?.setTime(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_REQ,
            AVIOCTRLDEFs.setTime((System.currentTimeMillis() / 1000).toInt(), getDiffMinute0Zone())
        )
        true
    } else false
}

/**
 * 同步 时间和时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_SYNC_REQ]
 */
fun Camera?.syncTime(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    val instance = Calendar.getInstance()
    val time = instance.timeInMillis
    return syncTime(channel, time, TimeZone.getDefault().getOffset(time) / 1000, must)
}

/**
 * 同步 时间和时区
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_SYNC_REQ]
 */
fun Camera?.syncTime(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    timeInMillis: Long,
    diffTime: Int,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_TIME_SYNC_REQ,
            AVIOCTRLDEFs.syncTime(timeInMillis, diffTime)
        )
        true
    } else false
}

/**
 * 获取推送定制
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ]
 */
fun Camera?.getDevicePushServiceUrl(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ,
            AVIOCTRLDEFs.getDevicePushServiceUrl()
        )
        true
    } else false
}

/**
 * 设置推送地址
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ]
 * @param url 推送地址  例如：120.24.215.192:8088/OuCamRaise
 */
fun Camera?.setDevicePushServiceUrl(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    url: String, must: Boolean = false
): Boolean {
    //120.24.215.192:8088/OuCamRaise
    return if (canSend() || must) {
        val index = url.indexOf(":")
        if (index <= 0) {
            return false
        }
        val ip = url.substring(0, index)
        val portIndex = url.indexOf("/")

        if (portIndex <= 0) {
            return false
        }
        val port = url.substring(index + 1, portIndex).toInt()

        val name = url.substring(portIndex + 1)

        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ,
            AVIOCTRLDEFs.setDevicePushServiceUrl(ip, port, name)
        )
        true
    } else false
}

/**
 * 获取指示灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ]
 */
fun Camera?.getLedStatus(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return this.ledStatus(channel, 1, false, must)
}

/**
 * 设置指示灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ]
 * @param status 指示灯状态
 */
fun Camera?.setLedStatus(channel: Int, status: Boolean, must: Boolean = false): Boolean {
    return this.ledStatus(channel, 0, status, must)
}

/**
 * 指示灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ]
 * @param type 1-Get 0-Set
 * @param status 状态
 */
private fun Camera?.ledStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ,
            AVIOCTRLDEFs.ledStatus(type, status)
        )
        true
    } else false
}

/**
 * 设备视频状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_CAMERA_REQ]
 * @param mode 状态类型
 */
fun Camera?.getCameraStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    mode: CameraVideoMode, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_CAMERA_REQ,
            AVIOCTRLDEFs.getCameraStatus(mode)
        )
        true
    } else false
}

/**
 * 设置视频状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_CAMERA_REQ]
 */
fun Camera?.setCameraStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    mode: CameraVideoMode,
    value: Int,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_CAMERA_REQ,
            AVIOCTRLDEFs.setCameraStatus(mode, value)
        )
        true
    } else false
}

/**
 * 获取夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_REQ]
 */
fun Camera?.getIrLedStatus(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false) =
    irLedStatus(channel, 0, IrLedStatus.OFF, must)

/**
 * 设置夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_REQ]
 * @param status
 */
fun Camera?.setIrLedStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    status: IrLedStatus,
    must: Boolean = false
) = irLedStatus(channel, 1, status, must)

/**
 * 夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_REQ]
 * @param type 0-Get 1-Set
 * @param status
 */
private fun Camera?.irLedStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    status: IrLedStatus,
    must: Boolean
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_REQ,
            AVIOCTRLDEFs.irLedStatus(type, status)
        )
        true
    } else false
}

/**
 * 获取视频镜像
 * [IOTYPE_USER_IPCAM_GET_VIDEOMODE_REQ]
 */
fun Camera?.getVideoMirrorStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOMODE_REQ,
            AVIOCTRLDEFs.getVideoMirror(channel)
        )
        true
    } else false
}

/**
 * 设置视频镜像
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOMODE_REQ]
 */
fun Camera?.setVideoMirrorStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    mode: VideoMirrorMode,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOMODE_REQ,
            AVIOCTRLDEFs.setVideoMirror(channel, mode)
        )
        true
    } else false
}

/**
 * 设备重启
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVREBOOT_REQ]
 */
fun Camera?.reboot(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVREBOOT_REQ,
            AVIOCTRLDEFs.reboot()
        )
        true
    } else false
}

/**
 * 格式化SDCard
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_REQ]
 */
fun Camera?.formatSdCard(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_REQ,
            AVIOCTRLDEFs.formatSdCard(0)
        )
        true
    } else false
}

/**
 * TFCard 录像回放
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL]
 * @param type 回放状态
 * @param time 回放指定视频
 */
fun Camera?.playback(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: PlaybackStatus,
    time: TEvent,

    duration: Int = 0,
    index: Int = 0,
    must: Boolean = false
): Boolean {
    Liotc.d("playback","restartPlayback send playback  111 canSend=${canSend()}  must=$must ${this==null}  mSid=${this?.isSessionConnected()}")
    return if (canSend() || must) {
        Liotc.d("playback","restartPlayback send playback 222")
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
            AVIOCTRLDEFs.playback(type, time.buf, 0, duration, index)
        )
        true
    } else false
}

/**
 * TFCard 录像回放
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL]
 * @param time 回放指定视频
 * @param percent 百分比
 */
fun Camera?.playbackSeekToPercent(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    time: TEvent, percent: Int, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL,
            AVIOCTRLDEFs.playback(PlaybackStatus.SEEKTIME, time.buf, percent)
        )
        true
    } else false
}


fun Camera?.getEventList(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    startTime: Long, endTime: Long, type: EventMode, value: Int
) {

}

/**
 * 手动喂食
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
 */
fun Camera?.manualFeed(channel: Int = Camera.DEFAULT_AV_CHANNEL, num: Int): Boolean {
    return if (canSend()) {
        val calender = Calendar.getInstance()
        val hour = calender.get(Calendar.HOUR_OF_DAY)
        val min = calender.get(Calendar.MINUTE)

        val instance = Calendar.getInstance(TimeZone.getTimeZone("gmt"))
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ,
            AVIOCTRLDEFs.editFeedPlan(
                instance.get(Calendar.YEAR) - 1960,
                instance.get(Calendar.MONTH) + 1,
                0x7f,
                hour,
                min,
                num,
                0,
                0,
                1,
                10
            )
        )
        true
    } else false
}

/**
 * 手动喂食
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_MANUAL_FEED_REQ]
 */
fun Camera?.manualFeed2(channel: Int = Camera.DEFAULT_AV_CHANNEL, num: Int,must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_MANUAL_FEED_REQ,
            num.littleByteArray()
        )
        true
    } else false
}

/**
 * 获取设备版本号、检查设备是否可以升级
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ]
 */
fun Camera?.getDeviceVersionInfo(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ,
            AVIOCTRLDEFs.getDeviceVersionInfo(AVIOCTRLDEFs.UPGRADE_ONLINE_TYPE_CHECK)
        )
        true
    } else false
}

/**
 * 开始升级
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ]
 * @param type [com.tutk.bean.TDeviceVersionInfo.result]
 */
fun Camera?.upgrade(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int = AVIOCTRLDEFs.UPGRADE_ONLINE_TYPE_SYS
): Boolean {
    return if (canSend()) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ,
            AVIOCTRLDEFs.getDeviceVersionInfo(type)
        )
        true
    } else false
}

/**
 * 获取喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
 *  一次性返回8餐
 */
fun Camera?.getFeedPlan(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    time: Int = 0, must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ,
            AVIOCTRLDEFs.feedPlan(time)
        )
        true
    } else false
}

/**
 * 修改喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
 */
fun Camera?.editFeedPlan(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    year: Int = 0xa5, month: Int = 0xa7, day: Int = 0x7f,
    hour: Int, min: Int, num: Int, id: Int, enable: Boolean, musicIndex: Int
): Boolean {
    return if (canSend()) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ,
            AVIOCTRLDEFs.editFeedPlan(
                year,
                month,
                day,
                hour,
                min,
                num,
                1,
                id,
                if (enable) 1 else 0,
                musicIndex
            )
        )
        true
    } else false
}

/**
 * 删除喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
 */
fun Camera?.deleteFeedPlan(channel: Int = Camera.DEFAULT_AV_CHANNEL, id: Int): Boolean {
    return editFeedPlan(
        channel,
        hour = 0,
        min = 0,
        num = 0,
        id = id,
        enable = false,
        musicIndex = 0
    )
}

/**
 * 获取喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ]
 * */
fun Camera?.getFeedPlan2(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ,
            AVIOCTRLDEFs.getFeedPlan2()
        )
        true
    } else false
}

/**
 * 修改、删除喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ]
 */
fun Camera?.editFeedPlan2(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    list: ArrayList<TFeedPlan2>,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ,
            AVIOCTRLDEFs.editFeedPlan2(list)
        )
        true
    } else false
}


/**
 * 获取喂食计划  带名称的喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_REQ]
 * */
fun Camera?.getFeedPlanWithName(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(648)
        val type = 0.littleByteArray()
        System.arraycopy(type, 0, data, 0, type.size)
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_REQ,
            data
        )
        true
    } else false
}


/**
 * 修改喂食计划  带名称的喂食计划
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_REQ]
 * */
fun Camera?.editFeedPlanWithName(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    planInfoList: MutableList<TFeedPlanWithNameFeedInfo>,
    planInfoNameList: MutableList<String>,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(648)
        //类型
        val type = 1.littleByteArray()
        System.arraycopy(type, 0, data, 0, type.size)
        //喂食计划个数
        val total = planInfoList.size.littleByteArray()
        System.arraycopy(total,0,data,4,total.size)
        val offset = 8
        val infoSize = 24
        planInfoList.forEachIndexed { index, info ->
            var start = index * infoSize + offset
            val week = info.week.littleByteArray()
            System.arraycopy(week,0,data,start,week.size)
            start+=4
            val hour = info.hour.littleByteArray()
            System.arraycopy(hour,0,data,start,hour.size)

            start+=4
            val min = info.min.littleByteArray()
            System.arraycopy(min,0,data,start,min.size)

            start+=4
            val num = info.num.littleByteArray()
            System.arraycopy(num,0,data,start,num.size)

            start+=4
            val enable = (if(info.enable) 1 else 0).littleByteArray()
            System.arraycopy(enable,0,data,start,enable.size)

            start+=4
            val audio = info.audio.littleByteArray()
            System.arraycopy(audio,0,data,start,audio.size)
        }

        val nameOffset = infoSize * 10 + offset
        val nameSize = 40
        planInfoNameList.forEachIndexed { index, info ->
            val start = nameSize * index + nameOffset
            val name = info.toByteArray(Charsets.UTF_8)
            System.arraycopy(name,0,data,start,if(name.size > nameSize) nameSize else name.size)
        }

        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TIMING_FEED_AND_NAME_REQ,
            data
        )
        true
    } else false
}

/**
 * wifi 信号强度
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_REQ]
 */
fun Camera?.getWifiSignal(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_REQ,
            AVIOCTRLDEFs.getWifiSignal()
        )
        true
    } else false
}


/**
 * 获取OSD开关
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOOSD_REQ]
 */
fun Camera?.getOsdStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOOSD_REQ,
            ByteArray(4)
        )
        true
    } else false
}

/**
 * 设置OSD开关
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOOSD_REQ]
 */
fun Camera?.setOsdStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOOSD_REQ,
            AVIOCTRLDEFs.setVideoOsdStatus(status)
        )
        true
    } else false
}

/**
 * 获取童锁状态
 * [AVIOCTRLDEFs.IOTYPE_USER_GET_CHILDREN_LOCK_REQ]
 * */
fun Camera?.getChildrenLockStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_GET_CHILDREN_LOCK_REQ,
            ByteArray(5)
        )
        true
    } else false
}

/**
 * 设置童锁状态
 * [AVIOCTRLDEFs.IOTYPE_USER_SET_CHILDREN_LOCK_REQ]
 */
fun Camera?.setChildrenLockStatus(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_SET_CHILDREN_LOCK_REQ,
            AVIOCTRLDEFs.setVideoOsdStatus(status)
        )
        true
    } else false
}

/**
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVRESET_REQ]
 * 设备复位
 * */
fun Camera?.resetDevice(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return if (canSend() || must) {
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVRESET_REQ,
            ByteArray(8)
        )
        true
    } else false
}

/**
 *  [AVIOCTRLDEFs.IOTYPE_USER_NOSLEEP_MODE_REQ]
 * 低功耗设备模式切换
 */
fun Camera?.getNosLeep(channel: Int = Camera.DEFAULT_AV_CHANNEL, must: Boolean = false): Boolean {
    return nosLeep(channel, 0, false, must)
}

fun Camera?.setNosLeep(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return nosLeep(channel, 1, status, must)
}

private fun Camera?.nosLeep(
    channel: Int = Camera.DEFAULT_AV_CHANNEL,
    type: Int,
    status: Boolean,
    must: Boolean = false
): Boolean {
    return if (canSend() || must) {
        val data = ByteArray(2)
        data[0] = type.toByte()
        data[1] = if (status) 1 else 0
        this?.sendIOCtrl(
            channel,
            AVIOCTRLDEFs.IOTYPE_USER_NOSLEEP_MODE_REQ,
            data
        )
        true
    } else false
}












