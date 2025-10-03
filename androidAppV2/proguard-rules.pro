
 -ignorewarnings
 -keepattributes *Annotation*
 -keepattributes Exceptions
 -keepattributes InnerClasses
 -keepattributes Signature
 -keepattributes SourceFile,LineNumberTable

# 通用混淆规则
-keep public class * extends java.lang.Exception

 -keepclasseswithmembers class * {
     native <methods>;
 }
 # 保持 Native 方法不被混淆
 -keepclasseswithmembernames class * {
     native <methods>;
 }

 -keepclasseswithmembers class * {
     public <init>(...);
 }


# 保持所有 R 类（资源类）不被混淆
-keep class **.R$* {
    *;
}

# 保持所有注解类不被混淆
-keep @interface * {
    *;
}

# 保持

# 保持自定义的 Application 类不被混淆
-keep public class * extends android.app.Application {
    public protected *;
}

# 保持 Activity、Service、BroadcastReceiver、ContentProvider 及其方法不被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
{
    public protected *;
    public *;
}

# 保持 View 及其子类不被混淆（注意：有时需要根据具体 View 类调整）
-keep public class * extends android.view.View {
    public protected *;
    public static ** get*(...);
    public static ** findById(...);
}

# 保持 Serializable 序列化的类不被混淆
-keep class * implements java.io.Serializable {
    !static !transient <fields>;
}

# 保留 androidx.annotation.Keep 注解类
-keep class androidx.annotation.Keep {
    *;
}
# 保留被 @Keep 注解的类
-keep @androidx.annotation.Keep class * {
    *;
}

# 保留被 @Serializable 注解的类
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# 保留被 @Keep 注解的方法
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 保留被 @Keep 注解的字段
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
}

# 保持第三方库不被混淆（以 Gson 为例）
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }

# 保持第三方库（如 Retrofit、OkHttp）不被混淆
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

-keep class * extends java.lang.annotation.Annotation { *; }
-keep interface * extends java.lang.annotation.Annotation { *; }

-keep class com.tencent.** { *; }
-keep interface com.tencent.** { *; }
-dontwarn com.tencent.beacon.event.UserAction
-dontwarn com.tencent.raft.measure.RAFTMeasure
-dontwarn com.tencent.raft.measure.config.RAFTComConfig
-dontwarn javax.lang.model.element.Modifier

#xiaomi推送
 -keep class com.smjcco.wxpusher.push.xiaomi.XiaomiPushMessageReceiver {*;}
 #华为推送
 -keep class com.huawei.hianalytics.**{*;}
 -keep class com.huawei.updatesdk.**{*;}
 -keep class com.huawei.hms.**{*;}
 #ali log
-keep class com.aliyun.sls.android.producer.* { *; }
-keep interface com.aliyun.sls.android.producer.* { *; }

#vivo推送
-dontwarn com.vivo.push.**
-keep class com.vivo.push.**{*; }
-keep class com.vivo.vms.**{*; }
-keep class com.smjcco.wxpusher.push.vivo.VIVOClientPushMessageReceiver{*;}
#荣耀
-keep class com.hihonor.push.**{*;}
#oppo
-keep public class * extends android.app.Service
-keep class com.heytap.msp.** { *;}