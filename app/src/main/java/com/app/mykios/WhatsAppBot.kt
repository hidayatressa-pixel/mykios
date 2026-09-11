package com.app.mykios

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppBot {

    fun kirimTagihan(context: Context, hutang: Hutang) {
        val message = """
            *PENGINGAT PEMBAYARAN - MY KIOS*
            
            Halo Kak *${hutang.namaPelanggan}*, 
            Kami ingin mengingatkan catatan hutang Anda:
            
            💰 *Jumlah:* ${CurrencyUtils.formatRupiah(hutang.jumlah)}
            🗓️ *Jatuh Tempo:* ${formatDate(hutang.jatuhTempo)}
            
            Mohon segera melakukan pembayaran. Terima kasih 🙏
            
            _Powered by @afterproject2026_
        """.trimIndent()

        kirimPesan(context, hutang.nomorWa, message)
    }

    fun kirimNota(context: Context, phone: String, nota: String) {
        val message = """
            *NOTA PEMBAYARAN - MY KIOS*
            
            $nota
            
            Terima kasih telah berbelanja di toko kami! 🙏
        """.trimIndent()
        kirimPesan(context, phone, message)
    }

    private fun kirimPesan(context: Context, phone: String, message: String) {
        var formattedPhone = phone
        if (formattedPhone.startsWith("0")) {
            formattedPhone = "62" + formattedPhone.substring(1)
        }
        
        try {
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDate(time: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(time))
    }
}