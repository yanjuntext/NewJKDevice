package com.tutk.IOTC.status

import com.tutk.IOTC.AVIOCTRLDEFs

/**
 * @Author: wangyj
 * @CreateDate: 2021/11/20
 * @Description:
 */
/**音频模式*/
enum class VoiceType {
    TWO_WAY_VOICE,//双向语音
    ONE_WAY_VOICE//单向语音
}

/**视频模式*/
enum class PlayMode(val value: Int) {
    PLAY_LIVE(2),//直播
    PLAY_BACK(1)//回放
}

/**视频录制状态*/
enum class RecordStatus {
    CONTEXT_NULL,
    FILE_NAME_ILLEGAL,
    VIDEO_CODEC_NULL,
    HAD_RECORDING,
    RECORD_START,
    RECORDING,
    RECORD_STOP
}

/**移动侦测*/
enum class MotionDetect(val value: Int) {
    OFF(0),
    LOW(25),
    MIDDLE(50),
    HIGH(75),
    HIGHEST(100)
}


/**
 * WIFI连接状态
 * [com.tutk.bean.TWifiInfo]
 */
enum class WifiStatus(val value: Int) {
    NONE(0),//未连接
    CONNECTED(1),//设备连接是当前WIFI，连接成功
    WRONG_PASSWORD(2),//设备连接的是当前WIFI，但是密码错误
    WEAK_SIGNAL(3),//设备连接的是当前WIFI，但是信号微弱
    READTY(4)//设备准备连接当前WIFI
}

/**
 * 录像模式
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETRECORD_REQ]
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
 * @param OFF 关闭录像
 * @param FULL_TIME 全时录像
 * @param ALARM 报警录像
 * @param TIMING 定时录像
 */
enum class RecordMode(val value: Int) {
    OFF(0),
    FULL_TIME(2),
    ALARM(1),
    TIMING(2)
}

/**
 * 录像质量
 */
enum class RecordQuality(val value: Int) {
    HD(0),
    SD(1)
}

/**
 * SdCard 状态
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_RESP]
 */
enum class SDCardStatus(val value: Int) {
    RECORDING(1),
    INSERT(2),
    NONE(3),
    ERROR(4),
    FULL(5)
}

/**
 * 时间类型
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_LISTEVENT_REQ]
 * @param ALL all event type(general APP-->IPCamera)
 * @param MOTIONDECT motion detect start
 * @param VIDEOLOST  video lost alarm
 * @param IOALARM  io alarmin start
 * @param MOTIONPASS motion detect end
 * @param VIDEORESUME video resume
 * @param IOALARMPASS  IO alarmin end
 * @param EXPT_REBOOT  system exception reboot
 * @param SDFAULT   sd record exception
 * @param DOOR_BELL  sd record exception
 */
enum class EventMode(val value: Int) {
    ALL(0x00),
    MOTIONDECT(0x01),
    VIDEOLOST(0x02),
    IOALARM(0x03),
    MOTIONPASS(0x04),
    VIDEORESUME(0x05),
    IOALARMPASS(0x06),
    EXPT_REBOOT(0x10),
    SDFAULT(0x11),
    DOOR_BELL(0x12),
}

/**
 * 视频 跳转
 * [com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_CAMERA_REQ]
 * @param BRIGHT 亮度
 * @param CONTRAST 对比度
 * @param SATURATION 饱和度
 * @param CHROMA 色度
 */
enum class CameraVideoMode(val status: Int) {
    BRIGHT(0),
    CONTRAST(1),
    SATURATION(2),
    CHROMA(3)
}

/**
 * 夜视灯状态
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_IRLED_REQ]
 * @param OFF  夜视灯关 ->白天
 * @param OPEN 夜视灯开->黑夜
 * @param AUTO 自动->根据周围环境自动切换
 */
enum class IrLedStatus(val status: Int) {
    OFF(0),
    OPEN(1),
    AUTO(2)
}

/**
 * 设备视频镜像模式
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_GET_VIDEOMODE_REQ]
 * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_VIDEOMODE_REQ]
 * @param MIRROR_NORMAL 无镜像
 * @param MIRROR_UP 开启了上下镜像
 * @param MIRROR_LEFT 开启了左右镜像
 * @param MIRROR_ALL 上下、左右镜像全部开启
 */
enum class VideoMirrorMode(val value: Int) {
    MIRROR_NORMAL(0),
    MIRROR_UP(1),
    MIRROR_LEFT(2),
    MIRROR_ALL(3)
}

/**
 * 回放状态
 * @param PAUSE 暂停
 * @param STOP 停止、相当于关闭文件
 * @param STEPFORWARD 快进5S
 * @param STEPBACKWARD 后退5S
 * @param FORWARD 下一个文件
 * @param BACKWARD 上一个文件
 * @param SEEKTIME 跳转到指定时间
 * @param END 录像播放完
 * @param START 开始
 */
enum class PlaybackStatus(val status: Int) {
    PAUSE(0x00),
    STOP(0x01),
    STEPFORWARD(0x02),
    STEPBACKWARD(0x03),
    FORWARD(0x04),
    BACKWARD(0x05),
    SEEKTIME(0x06),
    END(0x07),
    START(0x10),
    PLAYING(0x11),
    ERROR(0x12),
}




