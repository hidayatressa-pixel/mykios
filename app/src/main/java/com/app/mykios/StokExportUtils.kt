package com.app.mykios

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object StokExportUtils {
    
    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun exportToCsv(context: Context, listBarang: List<Barang>) {
        val fileName = "Laporan_Stok_${System.currentTimeMillis()}.csv"
        val csvHeader = "Kode,Nama,Harga,Stok\n"
        val csvData = StringBuilder()
        csvData.append(csvHeader)
        listBarang.forEach { b ->
            csvData.append(listOf(b.kodeBarang, b.nama, b.harga, b.stok).joinToString(",") { csv(it) }).append("\n")
        }
        saveToDownloads(context, fileName, csvData.toString())
    }

    fun exportAllData(context: Context, listBarang: List<Barang>, listTransaksi: List<Transaksi>, listHutang: List<Hutang>) {
        val fileName = "Backup_MyKios_${System.currentTimeMillis()}.csv"
        val csvData = StringBuilder()
        
        // Section Barang
        csvData.append("--- DATA BARANG ---\n")
        csvData.append("Kode,Nama,Harga,Modal,Stok\n")
        listBarang.forEach { b ->
            csvData.append(listOf(b.kodeBarang, b.nama, b.harga, b.hargaModal, b.stok).joinToString(",") { csv(it) }).append("\n")
        }
        
        // Section Transaksi
        csvData.append("\n--- DATA TRANSAKSI ---\n")
        csvData.append("Tanggal,Total,Metode\n")
        listTransaksi.forEach { t ->
            csvData.append(listOf(t.tanggal, t.total, t.metode).joinToString(",") { csv(it) }).append("\n")
        }

        // Section Hutang
        csvData.append("\n--- DATA HUTANG ---\n")
        csvData.append("Pelanggan,Jumlah,Jatuh Tempo,Lunas\n")
        listHutang.forEach { h ->
            csvData.append(listOf(h.namaPelanggan, h.jumlah, h.jatuhTempo, h.lunas).joinToString(",") { csv(it) }).append("\n")
        }

        saveToDownloads(context, fileName, csvData.toString())
    }

    private fun csv(value: Any?): String {
        val raw = value?.toString().orEmpty()
        val safe = if (raw.startsWith("=") || raw.startsWith("+") || raw.startsWith("-") || raw.startsWith("@")) "'$raw" else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun saveToDownloads(context: Context, fileName: String, data: String) {
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use {
                it.write(data.toByteArray())
                showToast(context, "Backup berhasil disimpan di folder Download")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Gagal backup: ${e.message}")
        }
    }
}
