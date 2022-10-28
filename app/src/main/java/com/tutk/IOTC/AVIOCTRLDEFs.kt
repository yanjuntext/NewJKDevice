package com.tutk.IOTC

import com.tutk.IOTC.Packet.intToByteArray_Little
import com.tutk.IOTC.status.*
import com.tutk.bean.TFeedPlan2
import java.util.*

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
object AVIOCTRLDEFs {
    const val IOTYPE_USER_IPCAM_GET_CAMERA_REQ = 0x2040
    const val IOTYPE_USER_IPCAM_GET_CAMERA_RESP = 0x2041

    const val IOTYPE_USER_IPCAM_SET_CAMERA_REQ = 0x203E
    const val IOTYPE_USER_IPCAM_SET_CAMERA_RESP = 0x203F

    const val IOTYPE_USER_IPCAM_EDIT_FILES_REQ = 0x2054
    const val IOTYPE_USER_IPCAM_EDIT_FILES_RESP = 0x2055

    //下载文件
    const val IOTYPE_USER_IPCAM_DOWNLOAD_FILE_REQ = 0x2042
    const val IOTYPE_USER_IPCAM_DOWNLOAD_FILE_RESP = 0x2043

    const val IOTYPE_USER_IPCAM_CHANGED_DOWNLOAD_REQ = 0x20CD
    const val IOTYPE_USER_IPCAM_CHANGED_DOWNLOAD_RESP = 0x20CE

    const val IOTYPE_USER_IPCAM_DG_BLUE_CLOCK_TTY_DATA_REQ = 0x606B
    const val IOTYPE_USER_IPCAM_DG_BLUE_CLOCK_TTY_DATA_RESP = 0x606C

    /* AVAPIs IOCTRL Message Type */
    const val IOTYPE_USER_IPCAM_START = 0x01FF
    const val IOTYPE_USER_IPCAM_STOP = 0x02FF

    const val IOTYPE_USER_IPCAM_AUDIOSTART = 0x0300
    const val IOTYPE_USER_IPCAM_AUDIOSTOP = 0x0301

    //开始通话
    const val IOTYPE_USER_IPCAM_SPEAKERSTART = 0x0350

    //结束通话
    const val IOTYPE_USER_IPCAM_SPEAKERSTOP = 0x0351

    //设置视频清晰度
    const val IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ = 0x0320
    const val IOTYPE_USER_IPCAM_SETSTREAMCTRL_RESP = 0x0321

    //获取视频清晰度
    const val IOTYPE_USER_IPCAM_GETSTREAMCTRL_REQ = 0x0322
    const val IOTYPE_USER_IPCAM_GETSTREAMCTRL_RESP = 0x0323

    const val IOTYPE_USER_IPCAM_SETMOTIONDETECT_REQ = 0x0324
    const val IOTYPE_USER_IPCAM_SETMOTIONDETECT_RESP = 0x0325
    const val IOTYPE_USER_IPCAM_GETMOTIONDETECT_REQ = 0x0326
    const val IOTYPE_USER_IPCAM_GETMOTIONDETECT_RESP = 0x0327

    const val IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_REQ = 0x0328
    const val IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_RESP = 0x0329

    const val IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ = 0x032A
    const val IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_RESP = 0x032B

    const val IOTYPE_USER_IPCAM_DEVINFO_REQ = 0x0330
    const val IOTYPE_USER_IPCAM_DEVINFO_RESP = 0x0331

    const val IOTYPE_USER_IPCAM_SETPASSWORD_REQ = 0x0332
    const val IOTYPE_USER_IPCAM_SETPASSWORD_RESP = 0x0333

    const val IOTYPE_USER_IPCAM_LISTWIFIAP_REQ = 0x0340
    const val IOTYPE_USER_IPCAM_LISTWIFIAP_RESP = 0x0341
    const val IOTYPE_USER_IPCAM_SETWIFI_REQ = 0x0342
    const val IOTYPE_USER_IPCAM_SETWIFI_RESP = 0x0343
    const val IOTYPE_USER_IPCAM_GETWIFI_REQ = 0x0344
    const val IOTYPE_USER_IPCAM_GETWIFI_RESP = 0x0345
    const val IOTYPE_USER_IPCAM_SETWIFI_REQ_2 = 0x0346
    const val IOTYPE_USER_IPCAM_GETWIFI_RESP_2 = 0x0347


    //录像设置、不支持定时录像
    const val IOTYPE_USER_IPCAM_SETRECORD_REQ = 0x0310
    const val IOTYPE_USER_IPCAM_SETRECORD_RESP = 0x0311

    //获取录像状态
    const val IOTYPE_USER_IPCAM_GETRECORD_REQ = 0x0312
    const val IOTYPE_USER_IPCAM_GETRECORD_RESP = 0x0313

    const val IOTYPE_USER_IPCAM_SETRCD_DURATION_REQ = 0x0314
    const val IOTYPE_USER_IPCAM_SETRCD_DURATION_RESP = 0x0315
    const val IOTYPE_USER_IPCAM_GETRCD_DURATION_REQ = 0x0316
    const val IOTYPE_USER_IPCAM_GETRCD_DURATION_RESP = 0x0317

    const val IOTYPE_USER_IPCAM_LISTEVENT_REQ = 0x0318
    const val IOTYPE_USER_IPCAM_LISTEVENT_RESP = 0x0319

    const val IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL = 0x031A
    const val IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP = 0x031B

    const val IOTYPE_USER_IPCAM_GET_EVENTCONFIG_REQ = 0x0400
    const val IOTYPE_USER_IPCAM_GET_EVENTCONFIG_RESP = 0x0401
    const val IOTYPE_USER_IPCAM_SET_EVENTCONFIG_REQ = 0x0402
    const val IOTYPE_USER_IPCAM_SET_EVENTCONFIG_RESP = 0x0403

    const val IOTYPE_USER_IPCAM_SET_ENVIRONMENT_REQ = 0x0360
    const val IOTYPE_USER_IPCAM_SET_ENVIRONMENT_RESP = 0x0361
    const val IOTYPE_USER_IPCAM_GET_ENVIRONMENT_REQ = 0x0362
    const val IOTYPE_USER_IPCAM_GET_ENVIRONMENT_RESP = 0x0363

    const val IOTYPE_USER_IPCAM_SET_VIDEOMODE_REQ = 0x0370
    const val IOTYPE_USER_IPCAM_SET_VIDEOMODE_RESP = 0x0371
    const val IOTYPE_USER_IPCAM_GET_VIDEOMODE_REQ = 0x0372
    const val IOTYPE_USER_IPCAM_GET_VIDEOMODE_RESP = 0x0373

    const val IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_REQ = 0x380
    const val IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_RESP = 0x381

    const val IOTYPE_USER_IPCAM_PTZ_COMMAND = 0x1001

    const val IOTYPE_USER_IPCAM_EVENT_REPORT = 0x1FFF

    const val IOTYPE_REPORT_WIFI_LIST_REQ = 0x211F
    const val IOTYPE_REPORT_WIFI_LIST_RESP = 0x2120

    const val IOTYPE_GET_CONNECT_WIFI_REQ = 0x2121
    const val IOTYPE_GET_CONNECT_WIFI_RESP = 0x2122

    /* AVAPIs IOCTRL Event Type */
    const val AVIOCTRL_EVENT_ALL = 0x00
    const val AVIOCTRL_EVENT_MOTIONDECT = 0x01
    const val AVIOCTRL_EVENT_VIDEOLOST = 0x02
    const val AVIOCTRL_EVENT_IOALARM = 0x03
    const val AVIOCTRL_EVENT_MOTIONPASS = 0x04
    const val AVIOCTRL_EVENT_VIDEORESUME = 0x05
    const val AVIOCTRL_EVENT_IOALARMPASS = 0x06
    const val AVIOCTRL_EVENT_EXPT_REBOOT = 0x10
    const val AVIOCTRL_EVENT_SDFAULT = 0x11
    const val USER_EVENT_PETWANT_PIR_DETECT = 0XF6

    /* AVAPIs IOCTRL Play Record Command */
    const val AVIOCTRL_RECORD_PLAY_PAUSE = 0x00
    const val AVIOCTRL_RECORD_PLAY_STOP = 0x01
    const val AVIOCTRL_RECORD_PLAY_STEPFORWARD = 0x02
    const val AVIOCTRL_RECORD_PLAY_STEPBACKWARD = 0x03
    const val AVIOCTRL_RECORD_PLAY_FORWARD = 0x04
    const val AVIOCTRL_RECORD_PLAY_BACKWARD = 0x05
    const val AVIOCTRL_RECORD_PLAY_SEEKTIME = 0x06
    const val AVIOCTRL_RECORD_PLAY_END = 0x07
    const val AVIOCTRL_RECORD_PLAY_START = 0x10
    const val AVIOCTRL_EVENT_DEVICE_LOG_MOTION_ALARM = 0XFE

    // AVIOCTRL PTZ Command Value
    const val AVIOCTRL_PTZ_STOP = 0
    const val AVIOCTRL_PTZ_UP = 1
    const val AVIOCTRL_PTZ_DOWN = 2
    const val AVIOCTRL_PTZ_LEFT = 3
    const val AVIOCTRL_PTZ_LEFT_UP = 4
    const val AVIOCTRL_PTZ_LEFT_DOWN = 5
    const val AVIOCTRL_PTZ_RIGHT = 6
    const val AVIOCTRL_PTZ_RIGHT_UP = 7
    const val AVIOCTRL_PTZ_RIGHT_DOWN = 8
    const val AVIOCTRL_PTZ_AUTO = 9
    const val AVIOCTRL_PTZ_SET_POINT = 10
    const val AVIOCTRL_PTZ_CLEAR_POINT = 11
    const val AVIOCTRL_PTZ_GOTO_POINT = 12
    const val AVIOCTRL_PTZ_SET_MODE_START = 13
    const val AVIOCTRL_PTZ_SET_MODE_STOP = 14
    const val AVIOCTRL_PTZ_MODE_RUN = 15
    const val AVIOCTRL_PTZ_MENU_OPEN = 16
    const val AVIOCTRL_PTZ_MENU_EXIT = 17
    const val AVIOCTRL_PTZ_MENU_ENTER = 18
    const val AVIOCTRL_PTZ_FLIP = 19
    const val AVIOCTRL_PTZ_START = 20
    const val AVIOCTRL_PTZ_LEFT_RIGHT = 55
    const val AVIOCTRL_PTZ_UP_DOWN = 56

    const val AVIOCTRL_LENS_APERTURE_OPEN = 21
    const val AVIOCTRL_LENS_APERTURE_CLOSE = 22
    const val AVIOCTRL_LENS_ZOOM_IN = 23
    const val AVIOCTRL_LENS_ZOOM_OUT = 24
    const val AVIOCTRL_LENS_FOCAL_NEAR = 25
    const val AVIOCTRL_LENS_FOCAL_FAR = 26

    const val AVIOCTRL_AUTO_PAN_SPEED = 27
    const val AVIOCTRL_AUTO_PAN_LIMIT = 28
    const val AVIOCTRL_AUTO_PAN_START = 29

    const val AVIOCTRL_PATTERN_START = 30
    const val AVIOCTRL_PATTERN_STOP = 31
    const val AVIOCTRL_PATTERN_RUN = 32

    const val AVIOCTRL_SET_AUX = 33
    const val AVIOCTRL_CLEAR_AUX = 34
    const val AVIOCTRL_MOTOR_RESET_POSITION = 35

    /* AVAPIs IOCTRL Quality Type */
    const val AVIOCTRL_QUALITY_UNKNOWN = 0x00
    const val AVIOCTRL_QUALITY_MAX = 0x01
    const val AVIOCTRL_QUALITY_HIGH = 0x02
    const val AVIOCTRL_QUALITY_MIDDLE = 0x03
    const val AVIOCTRL_QUALITY_LOW = 0x04
    const val AVIOCTRL_QUALITY_MIN = 0x05

    /* AVAPIs IOCTRL WiFi Mode */
    const val AVIOTC_WIFIAPMODE_ADHOC = 0x00
    const val AVIOTC_WIFIAPMODE_MANAGED = 0x01

    /* AVAPIs IOCTRL WiFi Enc Type */
    const val AVIOTC_WIFIAPENC_INVALID = 0x00
    const val AVIOTC_WIFIAPENC_NONE = 0x01
    const val AVIOTC_WIFIAPENC_WEP = 0x02
    const val AVIOTC_WIFIAPENC_WPA_TKIP = 0x03
    const val AVIOTC_WIFIAPENC_WPA_AES = 0x04
    const val AVIOTC_WIFIAPENC_WPA2_TKIP = 0x05
    const val AVIOTC_WIFIAPENC_WPA2_AES = 0x06
    const val AVIOTC_WIFIAPENC_WPA_PSK_TKIP = 0x07
    const val AVIOTC_WIFIAPENC_WPA_PSK_AES = 0x08
    const val AVIOTC_WIFIAPENC_WPA2_PSK_TKIP = 0x09
    const val AVIOTC_WIFIAPENC_WPA2_PSK_AES = 0x0A
    const val AVIOTC_WIFIAPENC_AUTO_ADAPT = 99

    /* AVAPIs IOCTRL Recording Type */
    const val AVIOTC_RECORDTYPE_OFF = 0x00
    const val AVIOTC_RECORDTYPE_FULLTIME = 0x01
    const val AVIOTC_RECORDTYPE_ALAM = 0x02
    const val AVIOTC_RECORDTYPE_MANUAL = 0x03

    const val AVIOCTRL_ENVIRONMENT_INDOOR_50HZ = 0x00
    const val AVIOCTRL_ENVIRONMENT_INDOOR_60HZ = 0x01
    const val AVIOCTRL_ENVIRONMENT_OUTDOOR = 0x02
    const val AVIOCTRL_ENVIRONMENT_NIGHT = 0x03

    /* AVIOCTRL VIDEO MODE */
    const val AVIOCTRL_VIDEOMODE_NORMAL = 0x00
    const val AVIOCTRL_VIDEOMODE_FLIP = 0x01
    const val AVIOCTRL_VIDEOMODE_MIRROR = 0x02
    const val AVIOCTRL_VIDEOMODE_FLIP_MIRROR = 0x03

    /*last*/
    const val IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ = 0x2046
    const val IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_RESP = 0x2047
    const val UPGRADE_ONLINE_TYPE_CHECK = 0
    const val UPGRADE_ONLINE_TYPE_SYS = 1
    const val UPGRADE_ONLINE_TYPE_UI = 2 //  猫头鹰  MCU

    const val UPGRADE_ONLINE_TYPE_SYS_UI = 3 // 猫头鹰  SYS _ MCU 升级请求，先发送 2 ；

    const val UPGRADE_ONLINE_TYPE_MCU = 7


    const val IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ = 0x3A0
    const val IOTYPE_USER_IPCAM_GET_TIMEZONE_RESP = 0x3A1
    const val IOTYPE_USER_IPCAM_SET_TIMEZONE_REQ = 0x3B0
    const val IOTYPE_USER_IPCAM_SET_TIMEZONE_RESP = 0x3B1

    const val IOTYPE_USER_IPCAM_SET_TIME_SYNC_REQ = 0x0816
    const val IOTYPE_USER_IPCAM_SET_TIME_SYNC_RESP = 0x0817

    const val IOTYPE_USER_IPCAM_SET_TIME_REQ = 0x2052
    const val IOTYPE_USER_IPCAM_SET_TIME_RESP = 0x2053

    const val IOTYPE_USER_IPCAM_SET_DATETIME_REQ = 0x2006
    const val IOTYPE_USER_IPCAM_GET_DATETIME_RESP = 0x2009

    const val IOTYPE_USER_IPCAM_PETS_SET_LOCALTION_REQ = 0x2082
    const val IOTYPE_USER_IPCAM_PETS_SET_LOCALTION_RESP = 0x2083

    const val IOTYPE_USER_IPCAM_PETS_GET_LOCALTION_REQ = 0x2084
    const val IOTYPE_USER_IPCAM_PETS_GET_LOCALTION_RESP = 0x2085

    const val IOTYPE_USER_IPCAM_DEVREBOOT_REQ = 0x200C
    const val IOTYPE_USER_IPCAM_DEVREBOOT_RESP = 0x200D

    const val IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ = 0x207E
    const val IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_RESP = 0x207F

    const val TTY_CMD_SET_FEEDING_REQ = 0x01
    const val TTY_CMD_GET_TIMING_REQ = 0x02
    const val TTY_CMD_GET_REMAIN_FEED_WEIGHT_REQ = 0x03

    const val TTY_CMD_SET_FEEDING_RESP = 0x07
    const val TTY_CMD_GET_PIECE_REQ = 0x08
    const val TTY_CMD_SET_CALIB = 0x0A
    const val TTY_CMD_SET_FEED_JOY = 0x0F
    const val TTY_CMD_SET_MOTOR_MOVE = 0x13
    const val TTY_CMD_GET_MOTOR_POS = 0x14


    const val IOTYPE_USER_IPCAM_PETS_GET_SOUND_LIST_REQ = 0x2086
    const val IOTYPE_USER_IPCAM_PETS_GET_SOUND_LIST_RESP = 0x2087
    const val IOTYPE_USER_IPCAM_PETS_SET_SOUND_ALIASE_REQ = 0x2088
    const val IOTYPE_USER_IPCAM_PETS_SET_SOUND_ALIASE_RESP = 0x2089

    //通知设备开始发送音频文件
    const val IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_REQ = 0x208A
    const val IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_RESP = 0x208B


    const val IOTYPE_USER_IPCAM_DEVICE_USER_EVENT_REPORT = 0x2072

    /**
     * alarm user event for pet
     */
    const val USER_EVENT_SOUND_DETECT_ALARMED = 0X203
    const val USER_EVENT_FEED_PETS_TIMER_FEED = 0X32 // for pets alarm timer feed

    const val USER_EVENT_FEED_PETS_MANUL_FEED = 0X33 // for pets alarm manul feed

    const val USER_EVENT_FEED_PETS_FEED_WARNING = 0X34
    const val USER_EVENT_FEED_PETS_NO_FOOD_WARING = 0X35 // for pets alarm food

    // warning
    const val USER_EVENT_FEED_PETS_OUT_FOOD_WARING = 0X36 // for pets device

    // out food  failed warning

    // out food  failed warning
    const val USER_EVENT_FEED_PETS_FEED_OK = 0X42 // for pets device out food

    // failed warning
    const val USER_EVENT_FEED_PETS_FEED_FAILED = 0X43 // for pets  feed failed

    const val USER_EVENT_FEED_PETS_FEED_MOTOR_ERROR = 0X44 // 电机卡住

    const val USER_EVENT_FEED_PETS_FEED_FOOD_BLOCK = 0x45 //堵食 过量出食

    const val USER_EVENT_FEED_PETS_FEED_TIME_OUT = 0x46 //喂食超时点击未动 设备异常

    const val USER_EVENT_FEED_PETS_FEED_NO_FOOD = 0x47 //桶内余粮不足

    const val USER_EVENT_FEED_PETS_FEED_BUTTON = 0x48 //按键喂食

    const val USER_EVENT_LOW_POWER = 0XF3

    const val USER_EVENT_LOW_WATER_WARING = 0XFF
    const val USER_EVENT_TIMER_WATER_FEED = 0X100
    const val USER_EVENT_MANUL_WATER_FEED = 0X101
    const val USER_EVENT_OUTWATER_FAILED = 0X102
    const val USER_EVENT_WATER_FILTER_OUTDATE = 0X103


    const val USER_EVENT_FEED_TYPE_TIMER_FEED = 0X1
    const val USER_EVENT_FEED_TYPE_MANUL_FEED = 0X2
    const val USER_EVENT_FEED_TYPE_DEV_TIRED = 0X3

    const val IOTYPE_USER_IPCAM_MANUL_RECORD_SETTING_REQ = 0x20A8
    const val IOTYPE_USER_IPCAM_MANUL_RECORD_SETTING_RESP = 0x20A9

    const val IOTYPE_USER_IPCAM_MANUL_RECORD_GETTING_REQ = 0x20AA
    const val IOTYPE_USER_IPCAM_MANUL_RECORD_GETTING_RESP = 0x20AB

    const val COMMON_COMMAND_SET_WIFI_CONF_PRO_REQ = 0x1F
    const val COMMON_COMMAND_SET_WIFI_CONF_PRO_RESP = 0x20
    const val COMMON_COMMAND_SET_UID_CONF_PRO_REQ = 155
    const val COMMON_COMMAND_SET_UID_CONF_PRO_RESP = 156
    const val COMMON_COMMAND_UDP_SET_WIFI_CONF_PRO_REQ = 165
    const val COMMON_COMMAND_UDP_SET_WIFI_CONF_PRO_RESP = 166
    const val COMMON_COMMAND_SET_REBOOT_CONF_PRO_REQ = 20
    const val COMMON_COMMAND_SET_UID_CONF_PRO_PORT = 9002

    const val IOTYPE_USER_IPCAM_GET_DEVICE_INSTORE_EVENT_REQ = 0x20C0
    const val IOTYPE_USER_IPCAM_GET_DEVICE_INSTORE_EVENT_RESP = 0x20C1

    const val IOTYPE_USER_IPCAM_EDIT_INSTORE_EVENT_REQ = 0x20C2
    const val IOTYPE_USER_IPCAM_EDIT_INSTORE_EVENT_RESP = 0x20c3

    const val IOTYPE_USER_IPCAM_PUSHSERVER_GET_SETTING = 0
    const val IOTYPE_USER_IPCAM_PUSHSERVER_SET_SETTING = 1
    const val IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ = 0x20C4
    const val IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_RESP = 0x20C5
    const val IOTYPE_USER_IPCAM_MANUAL_RECORD_SETTING_REQ = 0x20A8
    const val IOTYPE_USER_IPCAM_MANUAL_RECORD_SETTING_RESP = 0x20A9

    const val IOTYPE_USER_IPCAM_ADD_ARM_TIME_REQ = 0x01
    const val IOTYPE_USER_IPCAM_DEL_ARM_TIME_REQ = 0x00
    const val IOTYPE_USER_IPCAM_SET_ARM_TIME_REQ = 0x207A
    const val IOTYPE_USER_IPCAM_SET_ARM_TIME_RESP = 0x207B
    const val IOTYPE_USER_IPCAM_GET_ARM_TIME_REQ = 0x207C
    const val IOTYPE_USER_IPCAM_GET_ARM_TIME_RESP = 0x207D

    const val IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ = 0x208C
    const val IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_RESP = 0x208D

    const val IOTYPE_USER_IPCAM_GET_DEVICE_INSTORE_EVENT_REQ2 = 0x20D3
    const val IOTYPE_USER_IPCAM_GET_DEVICE_INSTORE_EVENT_RESP2 = 0x20D4

    const val IOTYPE_USER_IPCAM_PETS_GET_PLAY_DOG_SOUND_LIST_REQ = 0x20D5
    const val IOTYPE_USER_IPCAM_PETS_GET_PLAY_DOG_SOUND_LIST_RESP = 0x20D6


    const val IOTYPE_USER_IPCAM_PETS_SET_PLAY_DOG_SOUND_LIST_REQ = 0x20D7
    const val IOTYPE_USER_IPCAM_PETS_SET_PLAY_DOG_SOUND_LIST_RESP = 0x20D8

    const val THIRD_PART_SET_LASER_LIGHT_STAUS = 16
    const val THIRD_PART_GET_LASER_LIGHT_STAUS = 17


    const val IOTYPE_USER_IPCAM_THIRDPART_SETTING_RESP = 0x2057
    const val IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ = 0x2056

    const val IOTYPE_USER_IPCAM_PTZ_TRACK_RUN_SETTIG_REQ = 0x20DF
    const val IOTYPE_USER_IPCAM_PTZ_TRACK_RUN_SETTIG_RESP = 0x20E0

    const val IOTYPE_USER_IPCAM_PTZ_TRACK_RUN_DEFAULT_MODE = -1

    const val IOTYPE_USER_IPCAM_PTZ_TRACK_RUN = 0
    const val IOTYPE_USER_IPCAM_PTZ_TRACK_STOP = 1


    const val IOTYPE_USER_IPCAM_GETTING_PRESET_NAMELIST_REQ = 0x20BC
    const val IOTYPE_USER_IPCAM_GETTING_PRESET_NAMELIST_RESP = 0x20BD


    const val IOTYPE_PTZPRESET_GET = 0
    const val IOTYPE_PTZPRESET_SET = 1
    const val IOTYPE_PTZPRESET_SET_ONE = 2

    const val IOTYPE_USER_IPCAM_PTZ_TRACK_CRUSE_SETTIG_REQ = 0x20DD
    const val IOTYPE_USER_IPCAM_PTZ_TRACK_CRUSE_SETTIG_RESP = 0x20DE


    const val IOTYPE_RECORD_SETTING_REQ = 0x6062
    const val IOTYPE_RECORD_SETTING_RESP = 0x6063

    /**
     * 录像设置/获取。支持定时录像
     * */
    const val IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ = 0x602B
    const val IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_RESP = 0x602C


    const val AVIOCTRL_THIRDPART_GET_LED_STATUS = 0X01
    const val AVIOCTRL_THIRDPART_SET_LED_STATUS = 0X00
    const val AVIOCTRL_THIRDPART_SET_ALARMED_STATUS = 0X02
    const val AVIOCTRL_THIRDPART_GET_ALARMED_STATUS = 0x03

    const val IOTYPE_USER_IPCAM_SET_IRLED_REQ = 0x602D
    const val IOTYPE_USER_IPCAM_SET_IRLED_RESP = 0x602E

    const val IOTYPE_HANDLE_CAM_FILLINLED_REQ = 0x6055
    const val IOTYPE_HANDLE_CAM_FILLINLED_RESP = 0x6056

    const val IOTYPE_USER_IPCAM_SET_IOTYPE_HANDLE_IRLED_TIME_REQ = 0x6051
    const val IOTYPE_USER_IPCAM_SET_IOTYPE_HANDLE_IRLED_TIME_RESP = 0x6052

    const val IOTYPE_USER_IPCAM_CHANGED_FTP_SETTING_REQ = 0x20CF
    const val IOTYPE_USER_IPCAM_CHANGED_FETP_SETTING_RESP = 0x20D0

    const val IOTYPE_USER_GET_BATTERY_CAPACITY_REQ = 0x602F
    const val IOTYPE_USER_GET_BATTERY_CAPACITY_RESP = 0x6030

    const val IOTYPE_USER_GET_ERROR_CODE_SHENDAUDIO_START_TIME = 0x9099
    const val startSOUND_SENCMD = 1
    const val startSOUND_END = 2
    const val startSOUND_ERROR = 3

    /**
     * 获取WIFI信号强度
     * typedef struct
     * {
     * char reserved[4];
     * }SmsgAVIoctrlGetWiFiSignalReq;
     *
     * typedef struct
     * {
     * int signal;
     * }SmsgAVIoctrlGetWiFiSignalResp;
     *
     */
    const val IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_REQ = 0x5029
    const val IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_RESP = 0x5030

    /**
     * OSD 设置和获取
     *
     *   typedef struct
     *   {
     *   char osdEnable; 				// OSD
     *   //0:关 1:开
     *   char reserved[4];
     *   }SMsgAVIoctrlSetVideoOSDReq, SMsgAVIoctrlGetVideoOSDResp;
     *
     *   typedef struct
     *   {
     *   char reserved[4];
     *   }SMsgAVIoctrlGetVideoOSDReq;
     *
     *   typedef struct
     *   {
     *   int result; 		// 0: success; otherwise: failed.
     *   char reserved[4];
     *  }SMsgAVIoctrlSetVideoOSDResp;
     */
    const val IOTYPE_USER_IPCAM_SET_VIDEOOSD_REQ = 0x202E
    const val IOTYPE_USER_IPCAM_SET_VIDEOOSD_RESP = 0x202F
    const val IOTYPE_USER_IPCAM_GET_VIDEOOSD_REQ = 0x2030
    const val IOTYPE_USER_IPCAM_GET_VIDEOOSD_RESP = 0x2031


    /**
     * 童锁 设置和获取
     *
     *    typedef struct
     *   {
     *   char lockEnable; 				// 0：关闭；1：开启；
     *   char reserved[4];
     *   }SMsgAVIoctrlSetLockReq, SMsgAVIoctrlGetLockReq;
     *
     *   typedef struct
     *   {
     *   int result; 		// 0: success; otherwise: failed.
     *   char lockEnable; 				// 0：关闭；1：开启；
     *   char reserved[4];
     *   }SMsgAVIoctrlSetLockResp, SMsgAVIoctrlGetLockResp;
     *
     */
    const val IOTYPE_USER_SET_CHILDREN_LOCK_REQ = 0x5037
    const val IOTYPE_USER_SET_CHILDREN_LOCK_RESP = 0x5038
    const val IOTYPE_USER_GET_CHILDREN_LOCK_REQ = 0x5039
    const val IOTYPE_USER_GET_CHILDREN_LOCK_RESP = 0x503A

    /**设备复位*/
    const val IOTYPE_USER_IPCAM_DEVRESET_REQ = 0x200A
    const val IOTYPE_USER_IPCAM_DEVRESET_RESP = 0x200B


    private fun initByteArray(size: Int) = ByteArray(size)

    /**收到第一条IFrame时回复*/
    fun receiveFirstIFrame(channel: Int, recordType: Int): ByteArray {
        val data = initByteArray(12)
        val _channel = intToByteArray_Little(channel)
        val _recordType = intToByteArray_Little(recordType)

        System.arraycopy(_channel, 0, data, 0, _channel.size)
        System.arraycopy(_recordType, 0, data, 4, _recordType.size)
        return data
    }

    /**
     * 获取支持的流
     * [IOTYPE_USER_IPCAM_GETSUPPORTSTREAM_REQ]
     */
    fun getSupportStream() = initByteArray(4)

    /**
     * 获取音频类型
     * [IOTYPE_USER_IPCAM_GETAUDIOOUTFORMAT_REQ]
     */
    fun getAudioCodec() = initByteArray(8)

    /**
     * 获取视频清晰度
     * [IOTYPE_USER_IPCAM_GETSTREAMCTRL_REQ]
     */
    fun getStreamCtrl(channel: Int): ByteArray? {
        val result = initByteArray(8)
        val ch = intToByteArray_Little(channel)
        System.arraycopy(ch, 0, result, 0, 4)
        return result
    }

    /**
     * 设置视频清晰度
     * [IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ]
     */
    fun setSteamCtrl(channel: Int, quality: Int): ByteArray {
        val result = initByteArray(8)
        val ch = intToByteArray_Little(channel)
        System.arraycopy(ch, 0, result, 0, 4)
        result[4] = quality.toByte()
        return result
    }

    /**
     * 发送音频文件 设置音频文件信息
     * [IOTYPE_USER_IPCAM_PETS_AUDIO_FILE_SEND_REQ]
     */
    fun getAudioFileInfo(name: String?, alias: String?, track: Int, currentTime: Long): ByteArray? {
        if (name.isNullOrEmpty() || alias.isNullOrEmpty()) return null
        val data = initByteArray(280)

        val type = Packet.intToByteArray_Little(0)
        val time = Packet.longToByteArray_Little(currentTime)
        val _track = Packet.intToByteArray_Little(track)
        val _name = name.toByteArray()
        val _alias = alias.toByteArray()


        System.arraycopy(type, 0, data, 0, type.size)
        System.arraycopy(_name, 0, data, 4, _name.size)
        System.arraycopy(_alias, 0, data, 36, _alias.size)
        System.arraycopy(_track, 0, data, 100, _track.size)
        System.arraycopy(time, 0, data, 104, time.size)

        return data

    }

    /**
     * 下载文件
     * [IOTYPE_USER_IPCAM_DOWNLOAD_FILE_REQ]
     */
    fun getDownloadFile(num: Int = 1): ByteArray {
        val data = initByteArray(8)
        val _num = Packet.intToByteArray_Little(num)
        System.arraycopy(_num, 0, data, 0, _num.size)
        return data
    }

    /**
     * 获取移动侦测
     * [IOTYPE_USER_IPCAM_GETMOTIONDETECT_REQ]
     */
    fun getMotionDetect(channel: Int): ByteArray {
        val data = initByteArray(8)
        val ch = channel.littleByteArray()
        System.arraycopy(ch, 0, data, 0, 4)
        return data
    }

    /**
     * 设置移动侦测
     * [IOTYPE_USER_IPCAM_SETMOTIONDETECT_REQ]
     */
    fun setMotionDetect(channel: Int, motionDetect: MotionDetect): ByteArray {
        val data = 8.byteArray()
        val ch = channel.littleByteArray()
        val motion = motionDetect.value.littleByteArray()

        System.arraycopy(ch, 0, data, 0, 4)
        System.arraycopy(motion, 0, data, 4, 4)
        return data
    }

    /**
     * 获取设备信息
     * 包含 设备型号、版本号、存储空间等
     * [IOTYPE_USER_IPCAM_DEVINFO_REQ]
     */
    fun getDeviceInfo() = 4.byteArray()

    /**
     * 设置新密码
     * [IOTYPE_USER_IPCAM_SETPASSWORD_REQ]
     * @param old 旧密码
     * @param new 新密码
     */
    fun resetPassword(old: String, new: String): ByteArray {
        val data = 64.byteArray()
        val _old = old.toByteArray()
        val _new = new.toByteArray()
        System.arraycopy(_old, 0, data, 0, _old.size)
        System.arraycopy(_new, 0, data, 32, _new.size)
        return data
    }

    /**
     * 扫描WIFI
     * [IOTYPE_USER_IPCAM_LISTWIFIAP_REQ]
     */
    fun scanWifi() = 4.byteArray()

    /**
     * 获取当前连接的wifi信息
     * [IOTYPE_USER_IPCAM_GETWIFI_REQ]
     */
    fun getWifi() = initByteArray(4)

    /**
     *设置WIFI
     * @param ssid wifi名称
     * @param pwd wifi密码
     * @param mode 0
     * @param enctype 加密类型
     * @param resverd [AVIOTC_WIFIAPENC_AUTO_ADAPT] or 0
     */
    fun setWifi(
        ssid: ByteArray,
        pwd: ByteArray,
        mode: Int = 0,
        enctype: Int,
        resverd: Int
    ): ByteArray {
        val data = 76.byteArray()
        System.arraycopy(ssid, 0, data, 0, ssid.size)
        System.arraycopy(pwd, 0, data, 32, pwd.size)
        data[64] = mode.toByte()
        data[65] = enctype.toByte()
        data[66] = resverd.toByte()
        return data
    }

    /**
     * 设置录像模式
     * [IOTYPE_USER_IPCAM_SETRECORD_REQ]
     * @param channel
     * @param mode 录像模式
     */
    fun setRecordMode(channel: Int, mode: RecordMode): ByteArray {
        val data = 12.byteArray()
        val _channel = channel.littleByteArray()
        val _mode = mode.value.littleByteArray()
        System.arraycopy(_channel, 0, data, 0, 4)
        System.arraycopy(_mode, 0, data, 4, 4)
        return data
    }

    /**
     * 获取录像模式
     * [IOTYPE_USER_IPCAM_GETRECORD_REQ]
     */
    fun getRecordMode(channel: Int): ByteArray {
        val data = 8.byteArray()
        val _channel = channel.littleByteArray()
        System.arraycopy(_channel, 0, data, 0, 4)
        return data
    }

    /**
     *设置录像模式 支持定时录像的录像模式
     * [IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
     * @param mode 录像模式
     * @param limit 录像时长 每个录像文件的时长，取值范围[1,20]
     * @param curScheduleIndex
     * @param startTime 开始录像时间 秒
     * @param endTime 结束录像时间 秒
     */
    fun setRecordModeWithTime(
        mode: RecordMode,
        limit: Int, curScheduleIndex: Int,
        startTime: Int,
        endTime: Int
    ): ByteArray {
        val data = 12.byteArray()
        data[0] = 1 //0是获取 1是设置
        data[1] = mode.value.toByte()
        data[2] = limit.toByte()
        data[3] = curScheduleIndex.toByte()

        val start = startTime.littleByteArray()
        val end = endTime.littleByteArray()
        System.arraycopy(start, 0, data, 4, start.size)
        System.arraycopy(end, 0, data, 8, end.size)
        return data
    }

    /**
     * 获取录像模式 支持定时录像的录像模式
     * [IOTYPE_USER_IPCAM_SET_SCHEDULE_REOCRD_SEC_REQ]
     */
    fun getRecordModeWithTime(): ByteArray {
        val data = 12.byteArray()
        data[0] = 0 //0是获取 1是设置
        data[1] = 0
        data[2] = 0
        data[3] = 0
        val time = 0.littleByteArray()
        System.arraycopy(time, 0, data, 4, time.size)
        System.arraycopy(time, 0, data, 8, time.size)
        return data
    }

    /**
     * 录像质量
     * [IOTYPE_RECORD_SETTING_REQ]
     * @param type
     * @param cmd
     * @param quality 录像质量
     * @param cycle 循环
     * */
    fun recordQuality(type: Int, cmd: Int, quality: RecordQuality, cycle: Boolean): ByteArray {
        val data = 12.byteArray()
        val _type = type.littleByteArray()
        System.arraycopy(_type, 0, data, 0, _type.size)
        data[4] = cmd.toByte()
        data[5] = quality.value.toByte()
        data[6] = if (cycle) 1 else 0
        return data
    }

    /**
     * 事件列表
     * 包含移动侦测事件、TF卡录像等
     * [IOTYPE_USER_IPCAM_LISTEVENT_REQ]: 目前只用来获取录像事件
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param event 事件类型
     * @param status // 0x00: Recording file exists, Event unreaded
     *              //  0x01: Recording file exists, Event readed
     *             //   0x02: No Recording file in the event
     */
    fun getEventList(
        channel: Int,
        startTime: Long,
        endTime: Long,
        event: Int,
        status: Int
    ): ByteArray {
        val data = 24.byteArray()
        val _channel = channel.littleByteArray()
        System.arraycopy(_channel, 0, data, 0, _channel.size)
        val _startTime = parseTime(startTime)
        System.arraycopy(_startTime, 0, data, 4, _startTime.size)
        val _endTime = parseTime(endTime)
        System.arraycopy(_endTime, 0, data, 12, _endTime.size)
        data[20] = event.toByte()
        data[21] = status.toByte()
        return data
    }

    private fun parseTime(timeInMillis: Long): ByteArray {
        val data = 8.byteArray()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("gmt"))
        calendar.timeInMillis = timeInMillis

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekDay = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = 0

        val _year = year.toShort().littleByteArray()
        System.arraycopy(_year, 0, data, 0, _year.size)
        data[2] = month.toByte()
        data[3] = day.toByte()
        data[4] = weekDay.toByte()
        data[5] = hour.toByte()
        data[6] = minute.toByte()
        data[7] = second.toByte()
        return data
    }

    /**
     * 删除TFCard指定录像
     * [IOTYPE_USER_IPCAM_EDIT_FILES_REQ]
     */
    fun deleteTFRecordVideo(channel: Int, data: ByteArray): ByteArray {
        val result = initByteArray(32)
        val _channel = channel.littleByteArray()
        val mode = (0x10).littleByteArray()
        val param = 0.littleByteArray()
        System.arraycopy(_channel, 0, result, 0, 4)
        System.arraycopy(mode, 0, result, 4, 4)
        System.arraycopy(param, 0, result, 8, 4)
        System.arraycopy(data, 0, result, 12, data.size)
        return result
    }

    /**
     * 获取设备时区
     * [IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ]
     */
    fun getTimeZone() = initByteArray(268)

    /**
     * 设置设备时区
     * [IOTYPE_USER_IPCAM_SET_TIMEZONE_REQ]
     * @param supportTimeZone 是否支持  device is support TimeZone or not, 1: Supported, 0: Unsupported.
     * @param gmtDiff 当前时区与零时区偏移事件 the difference between GMT in hours
     */
    fun setTimeZone(supportTimeZone: Boolean, gmtDiff: Int): ByteArray {
        val data = initByteArray(268)
        val size = 268.littleByteArray()
        val support = (if (supportTimeZone) 1 else 0).littleByteArray()
        val _gmt = gmtDiff.littleByteArray()
        System.arraycopy(size, 0, data, 0, size.size)
        System.arraycopy(support, 0, data, 4, support.size)
        System.arraycopy(_gmt, 0, data, 8, _gmt.size)
        return data
    }

    /**
     * 设置时间
     * [IOTYPE_USER_IPCAM_SET_TIME_REQ]
     * @param currentTime  当前时间时间戳  second
     * @param diffZoneMinute 零时区偏移时间   minute
     */
    fun setTime(currentTime: Int, diffZoneMinute: Int): ByteArray {
        val data = initByteArray(16)
        val timeSecond0Zone = currentTime.littleByteArray()
        System.arraycopy(timeSecond0Zone, 0, data, 0, timeSecond0Zone.size)
        val _diffZoneMinute = diffZoneMinute.littleByteArray()
        System.arraycopy(_diffZoneMinute, 0, data, 4, _diffZoneMinute.size)
        return data
    }

    /**
     * 同步时间和时区
     * [IOTYPE_USER_IPCAM_SET_TIME_SYNC_REQ]
     * @param timeInMillis 时间
     * @param diffTime 时区偏移量  （S）
     */
    fun syncTime(timeInMillis: Long, diffTime: Int): ByteArray {
        val data = initByteArray(12)
        val instance = Calendar.getInstance()
        instance.timeInMillis = timeInMillis
        val year = instance.get(Calendar.YEAR).toShort().littleByteArray()
        System.arraycopy(year, 0, data, 0, year.size)
        val month = instance.get(Calendar.MONTH) + 1
        data[2] = month.toByte()
        val day = instance.get(Calendar.DAY_OF_MONTH)
        data[3] = day.toByte()
        val hour = instance.get(Calendar.HOUR_OF_DAY)
        data[4] = hour.toByte()
        val minute = instance.get(Calendar.MINUTE)
        data[5] = minute.toByte()
        val second = instance.get(Calendar.SECOND)
        data[6] = second.toByte()
        data[7] = 1
        val _diffTime = diffTime.littleByteArray()
        System.arraycopy(_diffTime, 0, data, 8, _diffTime.size)
        return data
    }

    /**
     * 获取推送地址
     * [IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ]
     */
    fun getDevicePushServiceUrl(): ByteArray {
        val data = initByteArray(52)
        val type = 0.littleByteArray() //0获取  1设置
        System.arraycopy(type, 0, data, 0, type.size)
        return data
    }

    /**
     * 设置推送地址
     * [IOTYPE_USER_IPCAM_PUSHSERVER_ADDR_SETTING_REQ]
     */
    fun setDevicePushServiceUrl(ip: String, port: Int, pushPath: String): ByteArray {
        val data = initByteArray(52)
        val type = 1.littleByteArray() //0获取  1设置
        System.arraycopy(type, 0, data, 0, type.size)
        val bIpAddr: ByteArray = ip.toByteArray()
        System.arraycopy(bIpAddr, 0, data, 4, bIpAddr.size)
        val bIpPort = intToByteArray_Little(port)
        System.arraycopy(bIpPort, 0, data, 28, bIpPort.size)
        val bSerPushPath: ByteArray = pushPath.toByteArray()
        System.arraycopy(bSerPushPath, 0, data, 32, bSerPushPath.size)
        return data
    }

    /**
     * 指示灯状态
     * [IOTYPE_USER_IPCAM_THIRDPART_SETTING_REQ]
     * @param type 0-Set 1-Get
     * @param status 0-Off 1-Open
     */
    fun ledStatus(type: Int, status: Boolean): ByteArray {
        val data = initByteArray(16)
        val _type = type.littleByteArray()
        val _status = (if (status) 1 else 0).littleByteArray()
        System.arraycopy(_type, 0, data, 0, _type.size)
        System.arraycopy(_status, 0, data, 4, _status.size)
        return data
    }

    /**
     * 获取设备视频状态
     * [IOTYPE_USER_IPCAM_GET_CAMERA_REQ]
     * @param mode 获取的类型
     */
    fun getCameraStatus(mode: CameraVideoMode): ByteArray {
        val data = initByteArray(8)
        data[0] = mode.status.toByte()
        return data
    }

    /**
     * 获取设备视频状态
     * [IOTYPE_USER_IPCAM_SET_CAMERA_REQ]
     * @param mode 类型
     * @param value 值
     */
    fun setCameraStatus(mode: CameraVideoMode, value: Int): ByteArray {
        val data = initByteArray(9)
        val _mode = mode.status.littleByteArray()
        val _value = value.littleByteArray()
        System.arraycopy(_mode, 0, data, 0, _mode.size)
        System.arraycopy(_value, 0, data, 4, _value.size)
        return data;
    }

    /**
     * 夜视灯状态
     * [IOTYPE_USER_IPCAM_SET_IRLED_REQ]
     * @param type 0->Get 1->Set
     * @param status 状态
     */
    fun irLedStatus(type: Int, status: IrLedStatus): ByteArray {
        val data = initByteArray(4)
        data[0] = type.toByte()
        data[1] = if (status == IrLedStatus.AUTO) 1 else 0
        data[2] = if (status == IrLedStatus.OFF) 0 else 1
        return data
    }

    /**
     * 获取视频镜像
     * [IOTYPE_USER_IPCAM_GET_VIDEOMODE_REQ]
     * 上下镜像  左右镜像
     */
    fun getVideoMirror(channel: Int): ByteArray {
        val data = initByteArray(8)
        val ch = channel.littleByteArray()
        System.arraycopy(ch, 0, data, 0, ch.size)
        return data
    }

    /**
     * 设置视频镜像
     * [IOTYPE_USER_IPCAM_SET_VIDEOMODE_REQ]
     * */
    fun setVideoMirror(channel: Int, mode: VideoMirrorMode): ByteArray {
        val data = initByteArray(8)
        val ch = channel.littleByteArray()
        System.arraycopy(ch, 0, data, 0, ch.size)
        data[4] = mode.value.toByte()
        return data
    }

    /**
     * 设备重启
     * [IOTYPE_USER_IPCAM_DEVREBOOT_REQ]
     */
    fun reboot() = initByteArray(8)

    /**
     * 格式化SDCard
     * [IOTYPE_USER_IPCAM_FORMATEXTSTORAGE_REQ]
     * @param storage // Storage index (ex. sdcard slot = 0, internal flash = 1, ...)
     */
    fun formatSdCard(storage: Int): ByteArray {
        val data = initByteArray(8)
        val _storage = storage.littleByteArray()
        System.arraycopy(_storage, 0, data, 0, _storage.size)
        return data
    }

    /**
     * TFCard 录像回放
     * [IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL]
     * @param type 回放模式
     * @param time 回放视频的日期 回放的具体录像
     * @param percent 百分比
     */
    fun playback(type: PlaybackStatus, time: ByteArray, percent: Int): ByteArray {
        val data = initByteArray(24)
        val channel = 0.littleByteArray()
        val _type = type.status.littleByteArray()
        val param = 0.littleByteArray()

        System.arraycopy(channel, 0, data, 0, channel.size)
        System.arraycopy(_type, 0, data, 4, _type.size)
        System.arraycopy(param, 0, data, 8, param.size)
        System.arraycopy(time, 0, data, 12, time.size)
        if (type == PlaybackStatus.SEEKTIME) {
            val _percent = percent.littleByteArray()
            System.arraycopy(_percent, 0, data, 20, _percent.size)
        }
        return data
    }

    /**
     * 获取设备版本号、检查设备是否可以升级
     * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SET_UPGRADEONLIN_REQ]
     * [com.tutk.io.getDeviceVersionInfo]
     */
    fun getDeviceVersionInfo(type: Int): ByteArray {
        val data = initByteArray(8)
        val littleByteArray = type.littleByteArray()
        System.arraycopy(littleByteArray, 0, data, 0, 4)
        return data
    }

    /**
     * 喂食计划
     * [com.tutk.io.getFeedPlan]
     * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
     */
    fun feedPlan(time: Int): ByteArray {
        val data = initByteArray(32)

        data[0] = 0x02
        data[1] = 0x00
        data[2] = 5

        val HEAD: Short = 0xFFFF.toShort()
        val _head = HEAD.littleByteArray()
        System.arraycopy(_head, 0, data, 4, _head.size)
        data[5] = 0x02
        data[6] = 0x01
        data[7] = time.toByte()

        return data
    }

    /**
     * 修改喂食计划
     * [AVIOCTRLDEFs.IOTYPE_USER_IPCAM_TRANSFER_TTY_DATA_REQ]
     * @param year  年
     * @param month 月
     * @param day 日或星期
     * @param hour 计划小时
     * @param min 计划分钟
     * @param num 喂食份数
     * @param feedType 喂食类型 1：自动喂食
     * @param id 喂食计划ID
     * @param enable 开关
     * @param musicIndex 喂食音频
     */
    fun editFeedPlan(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        min: Int,
        num: Int,
        feedType: Int,
        id: Int,
        enable: Int,
        musicIndex: Int
    ): ByteArray {
        val data = initByteArray(32)
        val cmd: Short = 1
        System.arraycopy(cmd.littleByteArray(), 0, data, 0, 2)
        data[2] = 14
        val info =
            getFeedPlanData(year, month, day, hour, min, num, feedType, id, enable, musicIndex)
        System.arraycopy(info, 0, data, 4, info.size)
        return data
    }

    /**
     * 喂食计划 喂食信息
     */
    private fun getFeedPlanData(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        min: Int,
        num: Int,
        feedType: Int,
        id: Int,
        enable: Int,
        musicIndex: Int
    ): ByteArray {
        val data = initByteArray(14)
        val head: Short = -1
        val _num = num.toShort()
        System.arraycopy(head.littleByteArray(), 0, data, 0, 2)
        data[2] = 1
        data[3] = 10
        data[4] = year.toByte()
        data[5] = month.toByte()
        data[6] = day.toByte()
        data[7] = hour.toByte()
        data[8] = min.toByte()
        System.arraycopy(_num.littleByteArray(), 0, data, 9, 2)
        data[11] = ((feedType shl 4) or enable).toByte()
        data[12] = id.toByte()
        data[13] = musicIndex.toByte()
        return data
    }

    /**
     * 获取喂食计划
     * [IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ]
     * [com.tutk.io.getFeedPlan2]
     * @param type 类型  0是获取
     * */
    fun getFeedPlan2(type: Int = 0): ByteArray {
        val data = initByteArray(8)
        val _type = type.littleByteArray()
        System.arraycopy(_type, 0, data, 0, _type.size)
        return data
    }

    /**
     * 修改喂食计划
     * [IOTYPE_USER_IPCAM_PETS_SET_SIXED_MEAL_LIST_REQ]
     * [com.tutk.io.editFeedPlan2]
     *
     */
    fun editFeedPlan2(list: ArrayList<TFeedPlan2>): ByteArray {
        val total = list.size
        val indexTotal = 60
        val length = indexTotal * total + 8
        val data = initByteArray(if (length >= 368) length else 368)

        val type = 1.littleByteArray()
        System.arraycopy(type, 0, data, 0, type.size)

        val size = total.littleByteArray()
        System.arraycopy(size, 0, data, 4, size.size)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("gmt"))
        (0 until total).forEach {
            val info = list[it]

            val id = info.id.littleByteArray()
            val start = it * indexTotal
            System.arraycopy(id, 0, data, start + 8, 4)

            val assembleEditFeedPlan2FeedInfo = assembleEditFeedPlan2FeedInfo(calendar, info)
            System.arraycopy(
                assembleEditFeedPlan2FeedInfo,
                0,
                data,
                start + 12,
                assembleEditFeedPlan2FeedInfo.size
            )

            data[start + 26] = info.smallTank.toByte()
            data[start + 27] = info.change.toByte()
            val alias = info.alias.toByteArray()
            System.arraycopy(alias, 0, data, start + 28, if (alias.size > 40) 40 else alias.size)
        }
        return data
    }

    /**组装喂食计划数据
     * [editFeedPlan2]
     */
    private fun assembleEditFeedPlan2FeedInfo(calendar: Calendar, info: TFeedPlan2): ByteArray {
        val data = initByteArray(14)
        val head = (0xFFFF.toShort()).littleByteArray()
        System.arraycopy(head, 0, data, 0, head.size)

        data[2] = 0x01
        data[3] = 10
        data[4] = (calendar.get(Calendar.YEAR) - 1960).toByte()
        data[5] = (calendar.get(Calendar.MONTH) + 1).toByte()
        data[6] = info.week.toByte()
        data[7] = info.hour.toByte()
        data[8] = info.min.toByte()

        val weight = (info.num.toShort()).littleByteArray()
        System.arraycopy(weight, 0, data, 9, weight.size)

        data[11] = ((1 shl 4) or (if (info.isEnable) 1 else 0)).toByte()

        data[12] = info.index.toByte()
        data[13] = info.musicIndex.toByte()
        return data
    }

    /**
     * 获取wifi型号强度
     * [IOTYPE_USER_IPCAM_GET_WIFI_SIGNAL_REQ]
     * [com.tutk.io.getWifiSignal]
     */
    fun getWifiSignal() = initByteArray(4)

    /**
     * 设置OSD状态/设置童锁状态
     * [IOTYPE_USER_IPCAM_SET_VIDEOOSD_REQ] or [IOTYPE_USER_SET_CHILDREN_LOCK_REQ]
     * [com.tutk.io.setOsdStatus] or [com.tutk.io.setChildrenLockStatus]
     */
    fun setVideoOsdStatus(status: Boolean): ByteArray {
        val data = initByteArray(5)
        data[0] = if (status) 1 else 0
        return data
    }



}

fun Int.byteArray() = ByteArray(this)