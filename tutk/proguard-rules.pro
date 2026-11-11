# ============================================================
# TUTK IoT SDK 混淆规则
# ============================================================

# 保留行号/源文件名信息，便于崩溃日志定位
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解、内部类与泛型签名信息
-keepattributes *Annotation*, InnerClasses, Signature
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# 保留 Kotlin Metadata 注解类。
# 关键：R8 混淆时若 kotlin.Metadata 类未显式保留，会剥离所有类上的 @Metadata 注解。
# 而下游 Kotlin 编译器解析扩展函数/顶层函数（如 ByteArray.littleByteArray()、
# ByteArray.getUtfString2()）完全依赖 @Metadata，注解丢失即报 Unresolved reference。
-keep class kotlin.Metadata { *; }
-keep class kotlin.annotation.AnnotationRetention { *; }

# 保留所有 native 方法名（JNI 静态绑定依赖方法名）
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- JNI 结构体（C 层通过 JNI 读写字段，字段名不可混淆） ----------
-keep class com.tutk.IOTC.St_* { *; }
-keep class com.tutk.IOTC.st_* { *; }

# ---------- TUTK 核心 API（使用方直接调用的公开类） ----------
-keep class com.tutk.IOTC.AVAPIs { *; }
-keep class com.tutk.IOTC.IOTCAPIs { *; }
-keep class com.tutk.IOTC.RDTAPIs { *; }
-keep class com.tutk.IOTC.TUTKGlobalAPIs { *; }
-keep class com.tutk.IOTC.AVAbort { *; }
-keep class com.tutk.IOTC.LocalRecording { *; }
-keep class com.tutk.IOTC.Camera { *; }
-keep class com.tutk.IOTC.AVIOCTRLDEFs { *; }
-keep class com.tutk.IOTC.RDCTRLDEFs { *; }
-keep class com.tutk.IOTC.AVFrame { *; }
-keep class com.tutk.IOTC.AVFrameQueue { *; }
-keep class com.tutk.IOTC.Packet { *; }
-keep class com.tutk.IOTC.Liotc { *; }
-keep class com.tutk.IOTC.AudioProcess { *; }
-keep class com.tutk.IOTC.OnLineResultCBListener { *; }
-keep class com.tutk.IOTC.LogLevel { *; }
-keep class com.tutk.IOTC.Region { *; }
-keep class com.tutk.IOTC.VSaaSContractInfo { *; }
-keep class com.tutk.IOTC.VSaaSPullStreamAttr { *; }
-keep class com.tutk.IOTC.util.LoadLibrary { *; }

# ---------- 对外 API 包（bean/监听器/通道/播放器/状态/解析/工具） ----------
-keep class com.tutk.bean.** { *; }
-keep class com.tutk.IOTC.listener.** { *; }
-keep class com.tutk.IOTC.camera.** { *; }
-keep class com.tutk.IOTC.player.** { *; }
-keep class com.tutk.IOTC.status.** { *; }
-keep class com.tutk.io.** { *; }
-keep class com.tutk.utils.** { *; }

# ---------- Kotlin 顶层函数/扩展函数类（*Kt） ----------
# 扩展函数编译后生成独立静态类：file.kt -> FileKt、Packet.kt -> PacketKt、
# AVIOCTRLDEFs.kt -> AVIOCTRLDEFsKt。这些函数（getUtfString2/littleInt/byteArray 等）
# 是外部可直接调用的 API，方法名混淆后外部编译期即找不到符号，必须整类保留。
# 注意：单类 keep（如 -keep ... Packet）不会覆盖对应的 *Kt 类。
-keep class com.tutk.IOTC.camera.FileKt { *; }
-keep class com.tutk.IOTC.PacketKt { *; }
-keep class com.tutk.IOTC.AVIOCTRLDEFsKt { *; }
-keepclassmembers class com.tutk.IOTC.camera.FileKt {
   *;
}
-keepclassmembers class com.tutk.IOTC.PacketKt {
   *;
}
-keepclassmembers class com.tutk.IOTC.AVIOCTRLDEFsKt {
   *;
}

# ---------- JNI 编解码/音视频类（无 @Keep 注解，必须整类保留） ----------
-keep class com.jkapp.android.media.VideoDecoder { *; }
-keep class com.qxt.yuv420.LibyuvUtils { *; }
-keep class com.encoder.util.EncSpeex { *; }
-keep class com.decoder.util.DecMpeg4 { *; }
-keep class com.tutk.libSLC.AcousticEchoCanceler { *; }

# ---------- WebRTC/AEC 桥接层 ----------
-keep class com.tutk.webtrc.** { *; }

# ---------- 第三方库（libs/*.jar，保留以免破坏其内部调用/反射） ----------
-keep class com.ingenic.** { *; }
-keep class com.android.aec.** { *; }
-keep class com.tutk.liblocalrecorder.** { *; }
-keep class com.tutk.libmediaconvert.** { *; }
-keep class com.google.protobuf.** { *; }

# ---------- 枚举 values/valueOf 兜底 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 缺失引用警告忽略
-dontwarn com.ingenic.**
-dontwarn com.android.aec.**
-dontwarn com.google.protobuf.**
-dontwarn com.tutk.liblocalrecorder.**
-dontwarn com.tutk.libmediaconvert.**

# 禁用代码压缩（shrink），仅做混淆重命名。
# 本库大量使用 JNI/反射，且作为 JitPack 依赖供第三方调用，
# 保守起见不删除未直接引用的代码，避免误删被反射使用的类。
-dontshrink
