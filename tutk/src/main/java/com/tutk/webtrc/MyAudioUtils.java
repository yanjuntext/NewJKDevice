package com.tutk.webtrc;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

/**
 * @Author: wangyj
 * @CreateDate: 2022/3/1
 * @Description:
 */
public class MyAudioUtils {
    public MyAudioUtils() {
    }

    public static AudioRecord creatAudioRecord(Context context, int[] var0, int var1, int var2, boolean var3) {
        AudioRecord var4 = null;
        int var5 = AudioRecord.getMinBufferSize(var1, 16, 2);
        if (var5 != -2 && var5 % var2 != 0) {
            var5 = (var5 / var2 + 1) * var2;
        }

        var0[0] = var5 * 3;

//        if(Build.BRAND.equals("samsung") ){

        AudioManager mn = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mn != null) {
            //修复三星手机双向通话时 声音小的问题
            mn.setMode(AudioManager.MODE_IN_COMMUNICATION);
            mn.setSpeakerphoneOn(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] devices = mn.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo deviceInfo : devices) {
                    int deviceType = deviceInfo.getType();
                    if (deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            || deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                        //检测到连接蓝牙耳机，则通过蓝牙耳机进行通话、播放
                        mn.setBluetoothScoOn(true);
                        mn.startBluetoothSco();
                        break;
                    }
                }
            }
        }
//            var4 = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, var1, 16, 2, var5 * 3);
//        }else{
        var4 = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, var1, 16, 2, var5 * 3);
//            var4 = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, var1, 16, 2, var5 * 3);
//        }

//        com.android.aec.util.a.a("AudioUtils", "audiorecord = " + var4 + "status = " + var4.getState());
        Log.d("AudioUtils", "audiorecord = " + var4 + "status = " + var4.getState());
        return var4.getState() == 1 ? var4 : null;
    }

    public static AudioTrack createTracker(int[] var0, int var1, int var2, boolean var3) {
        try {
            int var5 = AudioTrack.getMinBufferSize(var1, 4, 2);
            if (var5 != -2 && var5 % var2 != 0) {
                var5 = (var5 / var2 + 1) * var2;
            }

            var0[0] = var5 * 3;
            AudioTrack var4 = new AudioTrack(3, var1, 4, 2, var5 * 3, 1);
//            a.a("TAG", "track = " + var4 + "status = " + var4.getState());
            return var4.getState() == 1 ? var4 : null;
        } catch (Exception var6) {
            var6.printStackTrace();
            return null;
        }
    }
}
