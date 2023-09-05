# Add project specific ProGuard rules here.

# By default, the flags in this file are appended to flags specified

# in D:\android\sdk/tools/proguard/proguard-android.txt

# You can edit the include path and order by changing the proguardFiles

# directive in build.gradle.

#

# For more details, see

#  http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following

# and specify the fully qualified class name to the JavaScript interface

# class:

#-keep classmembers class fqcn.of.javascript.interface.for.webview {

#  public *;

#}

# Uncomment this to preserve the line number information for

# debugging stack traces.

#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to

# hide the original source file name.

#-renamesourcefileattribute SourceFile

#-------------------------------------------定制化区域----------------------------------------------
-keepclassmembers class com.tutk.utils.**
-dontwarn com.tutk.utils.**

-keepclassmembers class com.tutk.io.ParseKt
-dontwarn class com.tutk.io.ParseKt

-keepclassmembers class com.tutk.io.SendKt
-dontwarn class com.tutk.io.SendKt

-keepclassmembers class com.jkapp.android.media.*{*;}

-keepclassmembers class com.tutk.IOTC.listener.**

-keepclassmembers class com.tutk.IOTC.Packet
-keepclassmembers class com.tutk.IOTC.camera.AVChannel{
  #保持该类下所有的共有内容不被混淆
   public *;
}
-keepclassmembers class com.tutk.IOTC.camera.RecvAudioJob{
    #保持该类下所有的共有内容不被混淆
    public *;
}

-keepclassmembers class com.tutk.IOTC.Camera{
     *;
}

-keepclassmembers class com.tutk.IOTC.camera.DownFileJob{
     *;
}

-keepclassmembers class com.tutk.IOTC.camera.* {
     *;
}

-keepclassmembers interface com.tutk.IOTC.listener.* {
    *;
}

-keepclassmembers class com.tutk.IOTC.player.* {
    public *;
}

-keepclassmembers class com.tutk.** {
    public *;
}
-dontwarn com.tutk.**

-keepclassmembers class com.tutk.IOTC.Camera{*;}
-dontwarn com.decoder.**

-keepclassmembers class com.decoder.**{*;}
-dontwarn com.encoder.**

-keepclassmembers class com.encoder.** {*;}

-keepclassmembers class com.jkapp.** {*;}
-dontwarn com.jkapp.**

-keepclassmembers class com.tutk.IOTC.St_SInfoEx {
    *;
}
-keep class com.tutk.IOTC.St_SInfoEx{*;}

#---------------------------------1.实体类---------------------------------
-keep class com.tutk.bean.TBean.*{*;}
-keep class com.tutk.bean.TSupportStream.*{*;}
-keep class com.tutk.bean.TSetMotionDetect.*{*;}
-keep class com.tutk.bean.TDeviceInfo.*{*;}
-keep class com.tutk.bean.TScanWifi.*{*;}
-keep class com.tutk.bean.TWifiInfo.*{*;}
-keep class com.tutk.bean.TRecordMode.*{*;}
-keep class com.tutk.bean.TRecordModeWithTime.*{*;}
-keep class com.tutk.bean.TRecordQuality.*{*;}
-keep class com.tutk.bean.TRecordVideoInfo.*{*;}
-keep class com.tutk.bean.TEvent.*{*;}
-keep class com.tutk.bean.TTimeZone.*{*;}
-keep class com.tutk.bean.TDevicePushServiceUrl.*{*;}
-keep class com.tutk.bean.TLedStatus.*{*;}
-keep class com.tutk.bean.TCameraStatus.*{*;}
-keep class com.tutk.bean.TIRLedStatus.*{*;}
-keep class com.tutk.bean.TGetVideoMirror.*{*;}
-keep class com.tutk.bean.TSetVideoMirror.*{*;}
-keep class com.tutk.bean.TFormatSdCard.*{*;}
-keep class com.tutk.bean.TPlayback.*{*;}
#-------------------------------------------------------------------------

# 设置混淆的压缩比率 0 ~ 7
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# 混淆时不做预校验
-dontpreverify
# 混淆时不记录日志
-verbose
# 忽略警告
#-ignorewarning
# 代码优化
-dontshrink
# 不优化输入的类文件
-dontoptimize
# 保留注解不混淆
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型
-keepattributes Signature
# 保留代码行号，方便异常信息的追踪
-keepattributes SourceFile,LineNumberTable
# 混淆采用的算法
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# dump.txt文件列出apk包内所有class的内部结构
-dump class_files.txt
# seeds.txt文件列出未混淆的类和成员
-printseeds seeds.txt
# usage.txt文件列出从apk中删除的代码
-printusage unused.txt
# mapping.txt文件列出混淆前后的映射
-printmapping mapping.txt

#不需混淆的Android类
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.preference.Preference
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
#support下的所有类及其内部类
-keep class androidx.** {*;}
-keep interface androidx.** {*;}
-keep class * extends androidx.**  { *; }
-dontwarn androidx.**

#design库
-keep class com.google.android.material.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
#避免资源混淆
-keep class **.R$* {*;}
#避免混淆枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
#Natvie 方法不混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers interface *

-keep class com.tutk.IOTC.Camera {
    *;
}

-keep class com.decoder.*{*;}

-keep class com.encoder.*{*;}

-keep class com.jkapp.*{*;}

#kotlin 相关
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclasseswithmembers @kotlin.Metadata class * { *; }
-keepclassmembers class **.WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keep class kotlinx.** { *; }
-keep interface kotlinx.** { *; }
-dontwarn kotlinx.**
-dontnote kotlinx.serialization.SerializationKt

-keep class org.jetbrains.** { *; }
-keep interface org.jetbrains.** { *; }
-dontwarn org.jetbrains.**
