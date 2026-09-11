package com.app.mykios

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HutangReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val overdueDebt = db.transaksiDao().getHutangAktif().filter { 
                it.jatuhTempo < System.currentTimeMillis()
            }

            if (overdueDebt.isNotEmpty()) {
                showNotification(overdueDebt.size)
            }

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private fun showNotification(count: Int) {
        val channelId = "hutang_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pengingat Hutang", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hutang Jatuh Tempo!")
            .setContentText("Ada $count catatan hutang yang sudah melewati batas waktu.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
