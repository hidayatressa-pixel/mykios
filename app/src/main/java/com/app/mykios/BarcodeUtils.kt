package com.app.mykios

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BarcodeUtils {

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun generateBarcodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        return generateQrBitmap(content, width, height, BarcodeFormat.CODE_128)
    }

    fun generateQrBitmap(content: String, width: Int, height: Int, format: BarcodeFormat = BarcodeFormat.QR_CODE): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                format,
                width,
                height
            )
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQris(context: Context, amount: Int): Bitmap? {
        // Mock QRIS String Format (Static/Dynamic Simulation)
        val qrisString = "00020101021226670010ID.CO.QRIS.WWW011893600000012345678902151234567890123450303UME51440014ID.CO.QRIS.WWW02151234567890123450303UME52045999530336054${String.format("%05d", amount)}5802ID5911MY KIOS PRO6007JAKARTA61051234562070703A016304"
        return generateQrBitmap(qrisString, 500, 500)
    }

    fun generateBarcodePdf(context: Context, listBarang: List<Barang>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = Paint().apply {
            textSize = 10f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        // Standard A4: 595 x 842 points
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var xPos = 40f
        var yPos = 50f
        val itemWidth = 160f
        val itemHeight = 100f
        val margin = 20f

        listBarang.forEach { barang ->
            if (yPos + itemHeight > 800) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                xPos = 40f
                yPos = 50f
            }

            // Draw Barcode Frame
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            canvas.drawRect(xPos, yPos, xPos + itemWidth, yPos + itemHeight, paint)

            // Draw Text
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            val displayName = if (barang.nama.length > 20) barang.nama.substring(0, 18) + ".." else barang.nama
            canvas.drawText(displayName, xPos + (itemWidth / 2), yPos + 15f, textPaint)
            
            // Draw Price
            val pricePaint = Paint(textPaint).apply { textSize = 8f; color = Color.DKGRAY }
            canvas.drawText(CurrencyUtils.formatRupiah(barang.harga), xPos + (itemWidth / 2), yPos + 28f, pricePaint)

            // Generate & Draw Barcode
            val barcodeBmp = generateBarcodeBitmap(barang.kodeBarang ?: "N/A", 300, 100)
            barcodeBmp?.let {
                val dst = RectF(xPos + 15f, yPos + 35f, xPos + itemWidth - 15f, yPos + 75f)
                canvas.drawBitmap(it, null, dst, paint)
            }

            canvas.drawText(barang.kodeBarang ?: "", xPos + (itemWidth / 2), yPos + 90f, pricePaint)

            xPos += itemWidth + margin
            if (xPos + itemWidth > 550) {
                xPos = 40f
                yPos += itemHeight + margin
            }
        }

        pdfDocument.finishPage(page)

        // Save
        val fileName = "Barcode_Massal_${System.currentTimeMillis()}.pdf"
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
                Toast.makeText(context.applicationContext, "Barcode PDF berhasil diunduh ke folder Downloads", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context.applicationContext, "Gagal simpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
