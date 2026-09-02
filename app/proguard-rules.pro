# ProGuard and R8 Optimization Rules for StudentFocus
# Configured for maximum shrinking, aggressive obfuscation, and code optimization

# General Optimization Settings
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses 'com.ai_assistant.studentfocus.opt'
-dontusemixedcaseclassnames

# Keep Attributes for reflection & generics
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Native JNI Methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# MediaPipe GenAI & LLM Inference (Native bindings)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Room Database Entities and DAOs (Reflection entry points)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-dontwarn androidx.room.**

# Models and Database schemas
-keep class com.ai_assistant.studentfocus.models.** { *; }
-keep class com.ai_assistant.studentfocus.database.** { *; }

# ViewModel Constructors for reflection Factories
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Suppress benign library warnings
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**
-dontwarn androidx.biometric.**
