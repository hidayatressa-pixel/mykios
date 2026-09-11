package com.app.mykios

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_TRANSAKSI = "transaksi_channel"
        const val CHANNEL_STOK = "stok_channel"
        const val CHANNEL_SYSTEM = "system_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transaksiChannel = NotificationChannel(
                CHANNEL_TRANSAKSI,
                "Transaksi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi transaksi berhasil"
            }

            val stokChannel = NotificationChannel(
                CHANNEL_STOK,
                "Stok Barang",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi perubahan stok barang"
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "Sistem My Kios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi sistem dan akun"
            }

            notificationManager.createNotificationChannel(transaksiChannel)
            notificationManager.createNotificationChannel(stokChannel)
            notificationManager.createNotificationChannel(systemChannel)
        }
    }

    fun showTransaksiBerhasil(total: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSAKSI)
            .setSmallIcon(R.drawable.logo_mykios)
            .setContentTitle("Transaksi Berhasil!")
            .setContentText("Total pembayaran: $total")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showStockAdded(nama: String, jumlah: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STOK)
            .setSmallIcon(R.drawable.logo_mykios)
            .setContentTitle("Stok Bertambah")
            .setContentText("$nama baru saja ditambah sebanyak $jumlah")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showUpgradeSuccess() {
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.logo_mykios)
            .setContentTitle("Akun PRO Aktif! 🎉")
            .setContentText("Selamat! Anda sekarang memiliki akses penuh ke semua fitur My Kios.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(201, notification)
    }

    fun showWelcomeMessage(isPro: Boolean) {
        val title = if (isPro) "Selamat Datang, PRO User!" else "Selamat Datang di My Kios!"
        val message = if (isPro) "Nikmati fitur eksklusif untuk bisnis Anda." else "Ayo kelola warung Anda jadi lebih modern."

        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.logo_mykios)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(202, notification)
    }
    
    fun showNotaDownloaded(fileName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.logo_mykios)
            .setContentTitle("Nota Berhasil Disimpan")
            .setContentText("File $fileName telah tersimpan di perangkat Anda.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
