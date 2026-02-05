-allowaccessmodification
-repackageclasses
-optimizationpasses 5

-keepattributes *Annotation*, InnerClasses, SourceFile, LineNumberTable
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class androidx.media3.exoplayer.ExoPlayer {
    public <methods>;
}
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.common.Player$Listener { *; }
-keep class androidx.media3.common.VideoSize { *; }
-keep class androidx.media3.common.Format { *; }
-keep class androidx.media3.exoplayer.DecoderCounters { *; }

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keep class com.tencent.mmkv.MMKV { *; }
-keep class com.tencent.mmkv.MMKVLogLevel { *; }

-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    public <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn org.graalvm.nativeimage.**