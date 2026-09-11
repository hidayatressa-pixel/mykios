package com.app.mykios

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "hutang")
data class Hutang(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val namaPelanggan: String,
    val nomorWa: String,
    val jumlah: Int,
    val tanggalPinjam: Long,
    val jatuhTempo: Long,
    val lunas: Boolean = false
)