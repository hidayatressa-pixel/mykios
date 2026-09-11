package com.app.mykios

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "barang")
data class Barang(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val harga: Int,
    val hargaModal: Int = 0,
    val stok: Int,
    val satuan: String? = "Pcs",
    val kategori: String? = "Umum",
    val warna: String? = null,
    val ukuran: String? = null,
    val kodeBarang: String? = null
)
