package com.app.mykios

class AiEngine(private val dao: TransaksiDao) {

    suspend fun getInsight(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        val total = dao.getTotalRange(startOfDay, endOfDay) ?: 0
        val menipis = dao.getBarangStokMenipis()
        val top = dao.getTopProduk()

        val sb = StringBuilder()
        
        // Analisis Penjualan
        if (total > 1000000) sb.append("🚀 Luar biasa! Penjualan hari ini sangat tinggi.\n")
        else if (total > 0) sb.append("📈 Penjualan stabil hari ini.\n")
        else sb.append("☕ Belum ada penjualan hari ini, semangat!\n")

        // Analisis Stok
        if (menipis.isNotEmpty()) {
            sb.append("\n⚠️ Peringatan Stok:\n")
            menipis.take(3).forEach {
                sb.append("- ${it.nama} tersisa ${it.stok}, segera restock!\n")
            }
        }

        // Rekomendasi
        if (top.isNotEmpty()) {
            sb.append("\n💡 Tips: Produk '${top[0].namaBarang}' sedang laku keras. Pastikan stok aman untuk besok.")
        }

        // Prediksi AI (Super Power)
        if (menipis.isNotEmpty()) {
            sb.append("\n\n📉 Prediksi AI: Berdasarkan pola penjualan, produk '${menipis[0].nama}' kemungkinan akan habis total dalam 2 hari ke depan.")
        }

        return sb.toString()
    }
}
