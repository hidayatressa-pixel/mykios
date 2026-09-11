package com.app.mykios

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object NotaUtils {

    fun getNotaText(
        context: Context,
        transaksiId: Long,
        total: Int,
        items: List<Pair<Barang, Int>>,
        metodeBayar: String,
        diskon: Int,
        tunai: Int,
        kembalian: Int
    ): String {
        val session = SessionManager(context)
        val sb = StringBuilder()
        val sdfDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val now = Date()

        sb.append("${session.getNamaToko()?.uppercase() ?: "MY KIOS"}\n")
        sb.append("${session.getAlamatToko()}\n")
        sb.append("==============================\n")
        sb.append("No: INV-${String.format(Locale.getDefault(), "%05d", transaksiId)}\n")
        sb.append("Tgl: ${sdfDate.format(now)}\n")
        sb.append("Kasir: ${session.getNamaPemilik()}\n")
        sb.append("==============================\n")

        items.forEach { (item, qty) ->
            sb.append("${item.nama}\n")
            sb.append("  ${qty} x ${CurrencyUtils.formatRupiah(item.harga)} = ${CurrencyUtils.formatRupiah(item.harga * qty)}\n")
        }

        sb.append("------------------------------\n")
        sb.append("Subtotal: ${CurrencyUtils.formatRupiah(total + diskon)}\n")
        sb.append("Diskon: ${CurrencyUtils.formatRupiah(diskon)}\n")
        sb.append("TOTAL: ${CurrencyUtils.formatRupiah(total)}\n")
        sb.append("Tunai: ${CurrencyUtils.formatRupiah(tunai)}\n")
        sb.append("Kembalian: ${CurrencyUtils.formatRupiah(kembalian)}\n")
        sb.append("==============================\n")
        sb.append("Metode: $metodeBayar\n\n")
        sb.append("   Terima Kasih 🙏\n")
        sb.append(" Belanja Hemat di MYKIOS\n")

        return sb.toString()
    }

    fun generateNota(
        context: Context,
        transaksiId: Long,
        total: Int,
        items: List<Pair<Barang, Int>>,
        metodeBayar: String = "CASH",
        diskon: Int = 0,
        tunai: Int = 0,
        kembalian: Int = 0
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val session = SessionManager(context)

        // Page info: Wider for 80mm (approx 300 points)
        val height = 600 + (items.size * 30)
        val pageInfo = PdfDocument.PageInfo.Builder(300, height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Logo
        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_mykios)
            if (logoBitmap != null) {
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 100, 100, true)
                canvas.drawBitmap(scaledLogo, 100f, 10f, paint)
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Header
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 16f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(session.getNamaToko()?.uppercase() ?: "MY KIOS", 150f, 105f, titlePaint)

        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(session.getAlamatToko(), 150f, 120f, paint)
        canvas.drawText("Email: ${session.getEmail() ?: "-"}", 150f, 132f, paint)

        // Line
        canvas.drawText("===========================================", 150f, 145f, paint)

        // Info
        paint.textAlign = Paint.Align.LEFT
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        
        canvas.drawText("Tanggal : ${sdfDate.format(now)}", 20f, 165f, paint)
        canvas.drawText("Jam     : ${sdfTime.format(now)}", 160f, 165f, paint)
        canvas.drawText("Kasir   : ${session.getNamaPemilik()}", 20f, 180f, paint)
        canvas.drawText("Shift   : 1", 160f, 180f, paint)
        canvas.drawText("No Nota : INV-${String.format(Locale.getDefault(), "%05d", transaksiId)}", 20f, 195f, paint)

        // Line
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("===========================================", 150f, 210f, paint)

        // Table Header
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Nama Barang", 20f, 225f, paint)
        canvas.drawText("Qty", 150f, 225f, paint)
        canvas.drawText("Harga", 195f, 225f, paint)
        canvas.drawText("Total", 250f, 225f, paint)

        // Line
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("---------------------------------------------------------------------------", 150f, 235f, paint)

        // Items
        var yPos = 250f
        items.forEach { (item, qty) ->
            paint.textAlign = Paint.Align.LEFT
            // Handle long names
            val displayName = if (item.nama.length > 20) item.nama.substring(0, 18) + ".." else item.nama
            canvas.drawText(displayName, 20f, yPos, paint)
            canvas.drawText(qty.toString(), 155f, yPos, paint)
            canvas.drawText(item.harga.toString(), 195f, yPos, paint)
            canvas.drawText((item.harga * qty).toString(), 250f, yPos, paint)
            yPos += 20f
        }

        // Line
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("---------------------------------------------------------------------------", 150f, yPos, paint)
        yPos += 20f

        // Calculation Area
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Subtotal", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatRupiah(total + diskon), 280f, yPos, paint)

        yPos += 18f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Diskon", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatRupiah(diskon), 280f, yPos, paint)

        yPos += 18f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("PPN", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rp.0", 280f, yPos, paint)

        yPos += 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("===========================================", 150f, yPos, paint)
        
        yPos += 20f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatRupiah(total), 280f, yPos, paint)

        yPos += 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Tunai", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatRupiah(tunai), 280f, yPos, paint)

        yPos += 18f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Kembalian", 20f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyUtils.formatRupiah(kembalian), 280f, yPos, paint)

        yPos += 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("===========================================", 150f, yPos, paint)

        yPos += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Metode Bayar : $metodeBayar", 20f, yPos, paint)

        // Draw QRIS if selected
        if (metodeBayar == "QRIS") {
            val qrisPath = session.getQrisPath()
            if (qrisPath != null) {
                try {
                    val qrisBitmap = if (qrisPath.startsWith("content://")) {
                        val inputStream = context.contentResolver.openInputStream(Uri.parse(qrisPath))
                        BitmapFactory.decodeStream(inputStream)
                    } else {
                        BitmapFactory.decodeFile(qrisPath)
                    }

                    if (qrisBitmap != null) {
                        yPos += 20f
                        val scaledQris = Bitmap.createScaledBitmap(qrisBitmap, 120, 120, true)
                        canvas.drawBitmap(scaledQris, 90f, yPos, paint)
                        yPos += 130f
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Footer
        yPos += 40f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Terima Kasih 🙏", 150f, yPos, paint)
        yPos += 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Belanja Hemat di MYKIOS", 150f, yPos, paint)
        
        // Watermark check: Only for FREE users
        if (!session.isPro() && !session.isDev()) {
            yPos += 25f
            paint.textSize = 7f
            canvas.drawText("powered by @afterproject2026", 150f, yPos, paint)
        }

        pdfDocument.finishPage(page)

        // Save file
        val fileName = "Nota_INV_${transaksiId}_${System.currentTimeMillis()}.pdf"
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
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
                pdfDocument.writeTo(it)
                Toast.makeText(context.applicationContext, "Nota disimpan di folder Downloads", Toast.LENGTH_LONG).show()
                NotificationHelper(context).showNotaDownloaded(fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context.applicationContext, "Gagal mengunduh nota: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}