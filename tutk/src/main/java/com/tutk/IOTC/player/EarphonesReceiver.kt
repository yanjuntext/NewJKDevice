package com.tutk.IOTC.player

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.tutk.IOTC.Liotc
import com.tutk.IOTC.status.VoiceType

/**耳机状态监听*/
class EarphonesReceiver(val audioManager: AudioManager,var voiceType: VoiceType) :BroadcastReceiver(){
    private val TAG = this::class.java.simpleName

    fun getIntentFilter():IntentFilter{
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        return filter
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Liotc.d(TAG,"onReceive action=${intent?.action}")
        when(intent?.action){
            AudioManager.ACTION_HEADSET_PLUG->{
                //获取耳机状态
                val state = intent.getIntExtra("state",-1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val devices: Array<AudioDeviceInfo> =
                        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    for (deviceInfo in devices) {
                        val deviceType = deviceInfo.type
                        if (deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            || deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        ) {
                            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                            audioManager.startBluetoothSco()
                            //检测到连接蓝牙耳机，则通过蓝牙耳机进行通话、播放
                            audioManager.isBluetoothScoOn = true
                            audioManager.isSpeakerphoneOn = false
                            break
                        } else if (deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                            || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        ) {
                            //有线耳机
                            audioManager.isSpeakerphoneOn = false
                        }
                    }
                    if(devices.isEmpty()){
                        //没有耳机
                        audioManager.isSpeakerphoneOn = true
                    }
                    return
                }


                if(state == 1){
                    //耳机连接
                    if(audioManager.isBluetoothA2dpOn){
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                        audioManager.startBluetoothSco()
                        //检测到连接蓝牙耳机，则通过蓝牙耳机进行通话、播放
                        audioManager.isBluetoothScoOn = true
                        audioManager.isSpeakerphoneOn = false
                    }else{
                        //有线耳机
                        audioManager.isSpeakerphoneOn = false
                    }
                }else if(state == 0){
                    // 耳机断开
                    // 切换到扬声器
                    if(voiceType == VoiceType.ONE_WAY_VOICE){
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                    audioManager.isSpeakerphoneOn = false
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED->{
                //蓝牙耳机已连接
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.startBluetoothSco()
                //检测到连接蓝牙耳机，则通过蓝牙耳机进行通话、播放
                audioManager.isBluetoothScoOn = true
                audioManager.isSpeakerphoneOn = false
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED->{
                //蓝牙耳机已断开
                if(voiceType == VoiceType.ONE_WAY_VOICE){
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                audioManager.isSpeakerphoneOn = false
            }
        }
    }
}