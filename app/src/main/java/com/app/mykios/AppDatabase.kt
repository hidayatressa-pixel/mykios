package com.app.mykios

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Barang::class, Transaksi::class, DetailTransaksi::class, Hutang::class, Pengeluaran::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transaksiDao(): TransaksiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mykios_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
