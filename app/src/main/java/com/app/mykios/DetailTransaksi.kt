package com.app.mykios

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "detail_transaksi",
    indices = [Index(value = ["transaksiId"])],
    foreignKeys = [
        ForeignKey(entity = Transaksi::class,
            parentColumns = ["id"],
            childColumns = ["transaksiId"],
            onDelete = ForeignKey.CASCADE)
    ]
)
data class DetailTransaksi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transaksiId: Int,
    val namaBarang: String,
    val jumlah: Int,
    val harga: Int
)
