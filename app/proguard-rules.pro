# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.yomizora.reader.data.content.**$$serializer { *; }
-keepclassmembers class com.yomizora.reader.data.content.** {
    *** Companion;
}
-keepclasseswithmembers class com.yomizora.reader.data.content.** {
    kotlinx.serialization.KSerializer serializer(...);
}
