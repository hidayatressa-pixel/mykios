# --- KONFIGURASI ANTI-CRASH MY KIOS ---

# 1. Jangan acak nama paket utama agar tidak bingung saat refleksi
-keep class com.app.mykios.** { *; }
-keepclassmembers class com.app.mykios.** { *; }

# 2. Room Database (Sangat Rawan Force Close di Release)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao

# 3. Retrofit & OkHttp (Koneksi AI)
-keepattributes Signature, InnerClasses, AnnotationDefault
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# 4. Gson (Data JSON)
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# 5. MPAndroidChart (Grafik Laporan)
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# 6. Apache POI (Export Excel)
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# 7. Tetap simpan nomor baris untuk log error yang jelas
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 8. Mencegah optimasi yang terlalu agresif yang bisa merusak logika
-dontoptimize
-dontobfuscate
