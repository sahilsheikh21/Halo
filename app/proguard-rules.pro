# Halo ProGuard Rules

# Keep Matrix Rust SDK JNI/Uniffi bindings (required for native interop)
-keep class org.matrix.rustcomponents.sdk.** { *; }
-keep class uniffi.matrix_sdk.** { *; }
-keep class uniffi.matrix_sdk_ffi.** { *; }
# Keep JNA callback interfaces required by the Rust bridge
-keep class * implements com.sun.jna.Callback { *; }

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
