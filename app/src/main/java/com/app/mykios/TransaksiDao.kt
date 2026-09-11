package com.app.mykios

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Keep
data class SalesData(val total: Long = 0, val tanggal: String = "")

@Dao
interface TransaksiDao {

    // --- CRUD BARANG ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarang(barang: Barang)

    @Update
    suspend fun updateBarang(barang: Barang)

    @Delete
    suspend fun deleteBarang(barang: Barang)

    @Query("SELECT * FROM barang WHERE nama LIKE :query OR kodeBarang LIKE :query ORDER BY nama ASC")
    fun searchBarang(query: String): List<Barang>

    @Query("SELECT * FROM barang ORDER BY nama ASC")
    fun getAllBarangList(): List<Barang>

    @Query("SELECT * FROM barang ORDER BY nama ASC")
    fun getAllBarang(): List<Barang>

    // --- PENGELUARAN ---
    @Insert
    suspend fun insertPengeluaran(pengeluaran: Pengeluaran)

    @Query("SELECT * FROM pengeluaran ORDER BY tanggal DESC")
    fun getAllPengeluaran(): List<Pengeluaran>

    @Query("SELECT SUM(jumlah) FROM pengeluaran WHERE date(tanggal/1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    fun getPengeluaranHariIni(): Long?

    @Query("SELECT * FROM barang")
    fun getAllBarangRaw(): List<Barang>

    @Query("SELECT * FROM barang WHERE kodeBarang IS NULL OR kodeBarang = ''")
    fun getBarangTanpaKode(): List<Barang>

    // --- TRANSAKSI ---
    @Insert
    suspend fun insertTransaksi(transaksi: Transaksi): Long

    @Insert
    suspend fun insertDetail(detail: DetailTransaksi)

    @Query("SELECT SUM(total) FROM transaksi WHERE tanggal >= :start AND tanggal <= :end")
    fun getTotalRange(start: Long, end: Long): Long?

    @Query("SELECT * FROM detail_transaksi WHERE transaksiId IN (SELECT id FROM transaksi WHERE tanggal >= :start AND tanggal <= :end)")
    fun getDetailTransaksiRange(start: Long, end: Long): List<DetailTransaksi>

    @Query("SELECT SUM(total) FROM transaksi WHERE date(tanggal/1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    fun getTotalHariIni(): Long?

    @Query("SELECT * FROM detail_transaksi WHERE transaksiId IN (SELECT id FROM transaksi WHERE date(tanggal/1000, 'unixepoch', 'localtime') = date('now', 'localtime'))")
    fun getDetailTransaksiHariIni(): List<DetailTransaksi>

    @Query("SELECT COUNT(*) FROM transaksi WHERE date(tanggal/1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    fun getCountTransaksiHariIni(): Int?

    @Query("SELECT SUM(jumlah) FROM detail_transaksi WHERE transaksiId IN (SELECT id FROM transaksi WHERE date(tanggal/1000, 'unixepoch', 'localtime') = date('now', 'localtime'))")
    fun getProdukTerjualHariIni(): Long?

    @Query("SELECT namaBarang, SUM(jumlah) as total FROM detail_transaksi GROUP BY namaBarang ORDER BY total DESC LIMIT 3")
    fun getTopProduk(): List<TopProduk>

    // --- HUTANG ---
    @Insert
    suspend fun insertHutang(hutang: Hutang): Long

    @Query("SELECT * FROM hutang WHERE lunas = 0 ORDER BY jatuhTempo ASC")
    fun getHutangAktif(): List<Hutang>

    @Query("SELECT COUNT(*) FROM hutang WHERE lunas = 0")
    fun getCountHutangAktif(): Int?

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAllTransaksi(): List<Transaksi>

    @Query("SELECT * FROM hutang ORDER BY jatuhTempo ASC")
    fun getAllHutang(): List<Hutang>

    @Query("SELECT SUM(total) as total, date(tanggal/1000, 'unixepoch', 'localtime') as tanggal FROM transaksi GROUP BY date(tanggal/1000, 'unixepoch', 'localtime') ORDER BY tanggal DESC LIMIT 7")
    fun getSalesLast7Days(): List<SalesData>

    @Query("SELECT * FROM barang WHERE stok <= 5")
    fun getBarangStokMenipis(): List<Barang>

    @Query("SELECT COUNT(*) FROM barang WHERE stok <= 5")
    fun getCountStokMenipis(): Int?

    @Update
    suspend fun updateHutang(hutang: Hutang)

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC LIMIT 5")
    fun getRecentTransactions(): List<Transaksi>

    @Query("SELECT b.kategori as label, SUM(d.jumlah) as value FROM detail_transaksi d JOIN barang b ON d.namaBarang = b.nama GROUP BY b.kategori")
    fun getSalesByCategory(): List<ChartPoint>

    @Query("DELETE FROM barang")
    suspend fun deleteAllBarang()

    @Query("DELETE FROM transaksi")
    suspend fun deleteAllTransaksi()

    @Query("DELETE FROM detail_transaksi")
    suspend fun deleteAllDetails()

    @Query("DELETE FROM transaksi WHERE tanggal < :timestamp")
    suspend fun deleteTransactionsOlderThan(timestamp: Long)

    @Query("DELETE FROM detail_transaksi WHERE transaksiId NOT IN (SELECT id FROM transaksi)")
    suspend fun deleteOrphanedDetails()
}

@Keep
data class ChartPoint(val label: String? = "Lainnya", val value: Long = 0)
