package com.app.mykios

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "pengeluaran")
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keterangan: String,
    val jumlah: Int,
    val tanggal: Long = System.currentTimeMillis()
)
