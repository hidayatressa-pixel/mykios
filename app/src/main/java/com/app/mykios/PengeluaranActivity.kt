package com.app.mykios

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengeluaranActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rvPengeluaran: RecyclerView
    private lateinit var etKeterangan: TextInputEditText
    private lateinit var etJumlah: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var adapter: PengeluaranAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengeluaran)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        db = AppDatabase.getDatabase(this)
        rvPengeluaran = findViewById(R.id.rvPengeluaran)
        etKeterangan = findViewById(R.id.etKeterangan)
        etJumlah = findViewById(R.id.etJumlah)
        btnSimpan = findViewById(R.id.btnSimpan)

        etJumlah.addTextChangedListener(CurrencyTextWatcher(etJumlah))

        rvPengeluaran.layoutManager = LinearLayoutManager(this)
        adapter = PengeluaranAdapter(listOf())
        rvPengeluaran.adapter = adapter

        loadPengeluaran()

        btnSimpan.setOnClickListener {
            simpanPengeluaran()
        }
    }

    private fun loadPengeluaran() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.transaksiDao().getAllPengeluaran()
            withContext(Dispatchers.Main) {
                adapter.updateData(list)
            }
        }
    }

    private fun simpanPengeluaran() {
        val ket = etKeterangan.text.toString()
        val jmlString = CurrencyUtils.cleanCurrency(etJumlah.text.toString())
        val jml = jmlString.toIntOrNull() ?: 0

        if (ket.isEmpty() || jml <= 0) {
            Toast.makeText(this, "Data tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.transaksiDao().insertPengeluaran(Pengeluaran(keterangan = ket, jumlah = jml))
            withContext(Dispatchers.Main) {
                etKeterangan.setText("")
                etJumlah.setText("")
                Toast.makeText(this@PengeluaranActivity, "Pengeluaran berhasil dicatat", Toast.LENGTH_SHORT).show()
                loadPengeluaran()
            }
        }
    }
}
