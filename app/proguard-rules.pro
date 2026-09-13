# --- MYKIOS RELEASE R8 RULES ---

# Application classes currently remain unobfuscated while commercial hardening
# is stabilised. This can be tightened after release regression testing.
-keep class com.app.mykios.** { *; }
-keepclassmembers class com.app.mykios.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Preserve generic signatures/annotations used by Android libraries.
-keepattributes Signature,InnerClasses,EnclosingMethod,AnnotationDefault,*Annotation*

# Gson
-keep class com.google.gson.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Apache POI / XMLBeans bring optional desktop/JVM integrations that are not
# used by MYKIOS' Android XLSX import/export path. R8 still sees references to
# those optional classes (Saxon, OSGi, AWT, StAX, BouncyCastle, etc.), so ignore
# only those optional namespaces instead of disabling R8 globally.
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn org.osgi.**
-dontwarn aQute.bnd.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.logging.log4j.**

# Keep line information so release crash traces remain useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep optimisation conservative until the full release regression suite passes.
-dontoptimize
-dontobfuscate
