package com.app.mykios

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DompetActivity : BaseActivity() {

    private lateinit var session: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var tvSaldo: TextView
    private lateinit var rvHistory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dompet)

        session = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvSaldo = findViewById(R.id.tvSaldoDompet)
        rvHistory = findViewById(R.id.rvTransaksiDigital)

        tvSaldo.text = CurrencyUtils.formatRupiah(session.getSaldoDigital())

        findViewById<android.view.View>(R.id.btnTarikSaldo).setOnClickListener {
            Toast.makeText(this, "Fitur Penarikan hanya untuk Akun yang Terverifikasi!", Toast.LENGTH_LONG).show()
        }

        loadHistory()
    }

    private fun loadHistory() {
        rvHistory.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transaksiDao().getAllTransaksi().filter { it.metode == "QRIS" }
            withContext(Dispatchers.Main) {
                rvHistory.adapter = RecentTransaksiAdapter(transactions)
            }
        }
    }
}
