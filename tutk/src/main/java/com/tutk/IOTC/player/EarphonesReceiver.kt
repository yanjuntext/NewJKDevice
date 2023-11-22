package com.tutk.IOTC.player

import android.bluetooth.BluetoothAdapter
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
    private var isBluetoothEarPhone = false
    fun getIntentFilter():IntentFilter{
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
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
                           openBluetoothEarPhone()
                            break
                        } else if (deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                            || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        ) {
                            isBluetoothEarPhone = false
                            //有线耳机
                            audioManager.isBluetoothScoOn = false
                            audioManager.isSpeakerphoneOn = false
                        }
                    }
                    if(devices.isEmpty()){
                        isBluetoothEarPhone = false
                        //没有耳机
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = true
                    }
                    return
                }


                if(state == 1){
                    //耳机连接
                    if(audioManager.isBluetoothA2dpOn){
                       openBluetoothEarPhone()
                    }else{
                        isBluetoothEarPhone = false
                        audioManager.isBluetoothScoOn = false
                        //有线耳机
                        audioManager.isSpeakerphoneOn = false
                    }
                }else if(state == 0){
                    // 耳机断开
                    // 切换到扬声器
                    closeBluetoothEarPhone()
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED->{
                //蓝牙耳机已连接
               openBluetoothEarPhone()
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED->{

                //蓝牙耳机已断开
                closeBluetoothEarPhone()
            }
            BluetoothAdapter.ACTION_STATE_CHANGED->{
                var blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,0)
                 if(blueState == BluetoothAdapter.STATE_OFF && isBluetoothEarPhone){
                    closeBluetoothEarPhone()
                }
            }
        }
    }
    //关闭蓝牙耳机
    private fun closeBluetoothEarPhone(){
        Liotc.d(TAG,"closeBluetoothEarPhone")
        isBluetoothEarPhone = false
        //蓝牙耳机已断开
        if(voiceType == VoiceType.ONE_WAY_VOICE){
            audioManager.mode = AudioManager.MODE_NORMAL
        }
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = true
    }
    //打开蓝牙耳机
    private fun openBluetoothEarPhone(){
        Liotc.d(TAG,"openBluetoothEarPhone")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        isBluetoothEarPhone = true
        //检测到连接蓝牙耳机，则通过蓝牙耳机进行通话、播放
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        audioManager.isSpeakerphoneOn = false
    }
}