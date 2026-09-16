# --- MYKIOS RELEASE R8 RULES ---

# Application classes: keep while release regression testing is stabilised.
-keep class com.app.mykios.** { *; }
-keepclassmembers class com.app.mykios.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Reflection / generic metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,AnnotationDefault,*Annotation*

# Gson
-keep class com.google.gson.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Apache POI/XMLBeans contain optional desktop/JVM integrations that are not
# used by MYKIOS' Android XLSX path. These namespaces are absent on Android.
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.**
-dontwarn aQute.bnd.**
-dontwarn com.graphbuilder.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.logging.log4j.**

# Google API client may reference optional JVM transports/security providers.
-dontwarn com.google.api.client.extensions.**
-dontwarn com.google.api.client.googleapis.extensions.**

# Preserve useful release crash traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Conservative optimisation until release regression suite passes.
-dontoptimize
-dontobfuscate
