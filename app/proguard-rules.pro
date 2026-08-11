# R8 rules for the release build.
#
# DORMANT: isMinifyEnabled is false, so none of this runs today. It is kept
# because the findings below cost more to rediscover than to carry, and because
# enabling minification should be a one-line change rather than a research task.
#
# Most of the app needs nothing here: the network layer is HttpURLConnection and
# every payload is parsed by hand with org.json, so no class is looked up by name
# for serialization. Compose, Room and Navigation ship their own consumer rules.
#
# What does need help is below.

# --- Azure Cognitive Services Speech -----------------------------------------
# The SDK is a JNI wrapper: libMicrosoft.CognitiveServices.Speech.java.bindings.so
# resolves these classes, their fields and their constructors by name from native
# code, which R8 cannot see. Renaming or removing any of them fails at runtime,
# when the tutor tries to speak, rather than at build time. The AAR ships no
# consumer rules of its own, so this is the only thing standing between R8 and a
# silent TTS failure.
-keep class com.microsoft.cognitiveservices.speech.** { *; }
-dontwarn com.microsoft.cognitiveservices.speech.**

# Native callbacks in general: the JVM finds these by signature, not by reference.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# --- Room ---------------------------------------------------------------------
# Room resolves its generated implementation by name from the database class.
# room-runtime carries rules for this, but the app's own database is named here
# too so a change in that packaging cannot quietly break persistence.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class com.example.data.local.**_Impl { *; }

# --- Crash reports ------------------------------------------------------------
# Without this a release stack trace is a list of obfuscated single letters. The
# mapping file at app/build/outputs/mapping/release/mapping.txt translates them
# back, and must be kept for any build that ships.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
