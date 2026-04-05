# Halo ProGuard Rules

# Keep Matrix SDK
-keep class org.matrix.** { *; }
-keep class uniffi.** { *; }

# Keep Halo custom events (serialization)
-keep class com.halo.data.matrix.events.** { *; }
-keep class com.halo.domain.model.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.halo.**$$serializer { *; }
-keepclassmembers class com.halo.** {
    *** Companion;
}
-keepclasseswithmembers class com.halo.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
