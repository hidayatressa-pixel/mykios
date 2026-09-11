package com.app.mykios

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import android.widget.Button
import android.widget.Toast

import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color
import com.github.mikephil.charting.utils.ColorTemplate
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter

class LaporanActivity : BaseActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val session = SessionManager(this)
        val cardTable = findViewById<View>(R.id.cardLaporanTable)
        val listView = findViewById<ListView>(R.id.listLaporan)
        lineChart = findViewById(R.id.salesChart)
        pieChart = findViewById(R.id.pieChart)
        
        if (session.isPro() || session.isDev()) {
            cardTable.visibility = View.VISIBLE
            loadDataLaporan(listView)
            loadLineChartData()
            loadPieChartData()
            checkAutoClearOldReports()
        } else {
            cardTable.visibility = View.GONE
            findViewById<View>(R.id.cardChart).visibility = View.GONE
            findViewById<View>(R.id.cardPieChart).visibility = View.GONE
        }

        findViewById<Button>(R.id.btnExportExcel).setOnClickListener {
            if (session.isPro() || session.isDev()) {
                Toast.makeText(this, "Mengekspor laporan ke Excel...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Fitur Export Excel hanya untuk member PRO!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_laporan, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_report) {
            confirmClearReport()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmClearReport() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bersihkan Laporan?")
            .setMessage("Seluruh riwayat transaksi akan dihapus permanen dari perangkat ini.")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@LaporanActivity)
                    db.transaksiDao().deleteAllTransaksi()
                    db.transaksiDao().deleteAllDetails()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LaporanActivity, "Laporan dibersihkan", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkAutoClearOldReports() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@LaporanActivity)
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            db.transaksiDao().deleteTransactionsOlderThan(sevenDaysAgo)
            db.transaksiDao().deleteOrphanedDetails()
        }
    }

    private fun loadLineChartData() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val salesData = db.transaksiDao().getSalesLast7Days().reversed()
            
            withContext(Dispatchers.Main) {
                if (salesData.isEmpty()) {
                    lineChart.setNoDataText("Belum ada data transaksi")
                    lineChart.invalidate()
                    return@withContext
                }

                val entries = salesData.mapIndexed { index, data ->
                    Entry(index.toFloat(), data.total.toFloat())
                }

                val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val textColor = if (isNightMode) Color.WHITE else Color.GRAY

                val dataSet = LineDataSet(entries, "Penjualan").apply {
                    color = Color.parseColor("#4F46E5")
                    setCircleColor(Color.parseColor("#4F46E5"))
                    lineWidth = 3f
                    circleRadius = 5f
                    setDrawCircleHole(true)
                    valueTextColor = textColor
                    valueTextSize = 10f
                    setDrawFilled(true)
                    fillDrawable = androidx.core.content.ContextCompat.getDrawable(this@LaporanActivity, R.drawable.gradient_primary)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }

                lineChart.apply {
                    data = LineData(dataSet)
                    description.isEnabled = false
                    legend.isEnabled = false
                    
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        this.textColor = textColor
                        textSize = 10f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                if (idx >= 0 && idx < salesData.size) {
                                    return salesData[idx].tanggal.substring(5) // MM-DD
                                }
                                return ""
                            }
                        }
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        gridColor = Color.parseColor("#10000000")
                        this.textColor = textColor
                        textSize = 10f
                        axisMinimum = 0f
                    }
                    axisRight.isEnabled = false
                    
                    animateX(1000)
                    invalidate()
                }
            }
        }
    }

    private fun loadPieChartData() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val points = db.transaksiDao().getSalesByCategory()
            withContext(Dispatchers.Main) {
                val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                val textColor = if (isNightMode) Color.WHITE else Color.BLACK

                val entries = points.map { 
                    PieEntry(it.value.toFloat(), it.label ?: "Lainnya") 
                }
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                dataSet.valueTextColor = Color.WHITE
                dataSet.valueTextSize = 12f
                
                val pieData = PieData(dataSet)
                pieChart.data = pieData
                pieChart.description.isEnabled = false
                pieChart.centerText = "Kategori"
                pieChart.setCenterTextColor(textColor)
                pieChart.setEntryLabelColor(textColor)
                pieChart.legend.textColor = textColor
                pieChart.setCenterTextSize(14f)
                pieChart.setHoleColor(Color.TRANSPARENT)
                pieChart.animateXY(1000, 1000)
                pieChart.invalidate()
            }
        }
    }

    private fun loadDataLaporan(listView: ListView) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis

            val listDetail = db.transaksiDao().getDetailTransaksiRange(startOfDay, endOfDay)
            withContext(Dispatchers.Main) {
                val data = listDetail.map { 
                    "${it.namaBarang} x${it.jumlah}\nTotal: ${CurrencyUtils.formatRupiah(it.harga * it.jumlah)}"
                }
                if (data.isEmpty()) {
                    listView.adapter = ArrayAdapter(this@LaporanActivity, android.R.layout.simple_list_item_1, arrayOf("Belum ada transaksi hari ini"))
                } else {
                    listView.adapter = ArrayAdapter(this@LaporanActivity, android.R.layout.simple_list_item_1, data)
                }
            }
        }
    }
}
