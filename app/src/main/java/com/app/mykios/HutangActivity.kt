package com.app.mykios

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class HutangActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: HutangAdapter
    private lateinit var listView: ListView
    private lateinit var emptyState: View
    
    private var etNamaRef: EditText? = null
    private var etWaRef: EditText? = null

    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            try {
                val cursor = contentResolver.query(contactUri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else ""
                    var number = if (numberIndex >= 0) cursor.getString(numberIndex) else ""
                    
                    // Clean number
                    number = number.replace("[^0-9]".toRegex(), "")
                    if (number.startsWith("0")) number = "62" + number.substring(1)
                    
                    etNamaRef?.setText(name)
                    etWaRef?.setText(number)
                    
                    cursor.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat kontak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hutang)

        db = AppDatabase.getDatabase(this)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        listView = findViewById(R.id.listHutang)
        emptyState = findViewById(R.id.emptyStateContainer)
        
        adapter = HutangAdapter(this, listOf()) { hutang, action ->
            when (action) {
                HutangAdapter.ActionType.TAGIH -> {
                    val session = SessionManager(this)
                    if (session.isPro() || session.isDev()) {
                        WhatsAppBot.kirimTagihan(this, hutang)
                    } else {
                        Toast.makeText(this, "Fitur Penagihan Bot WA hanya untuk PRO!", Toast.LENGTH_SHORT).show()
                    }
                }
                HutangAdapter.ActionType.LUNAS -> {
                    confirmHutangLunas(hutang)
                }
                HutangAdapter.ActionType.TAMBAH_TEMPO -> {
                    showTambahTempoDialog(hutang)
                }
            }
        }
        listView.adapter = adapter

        findViewById<MaterialButton>(R.id.btnTambahHutang).setOnClickListener {
            showTambahHutangDialog()
        }

        loadHutang()
    }

    private fun confirmHutangLunas(hutang: Hutang) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Pelunasan")
            .setMessage("Apakah hutang dari ${hutang.namaPelanggan} sebesar ${CurrencyUtils.formatRupiah(hutang.jumlah)} sudah dibayar lunas?")
            .setPositiveButton("Ya, Lunas") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().updateHutang(hutang.copy(lunas = true))
                    db.transaksiDao().insertTransaksi(Transaksi(
                        tanggal = System.currentTimeMillis(),
                        total = hutang.jumlah,
                        metode = "HUTANG_LUNAS"
                    ))
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HutangActivity, "Hutang berhasil dilunasi & dicatat ke laporan", Toast.LENGTH_SHORT).show()
                        loadHutang()
                    }
                }
            }
            .setNegativeButton("Belum", null)
            .show()
    }

    private fun showTambahTempoDialog(hutang: Hutang) {
        val options = arrayOf("Tambah 3 Hari", "Tambah 7 Hari", "Tambah 14 Hari", "Tambah 30 Hari")
        val days = intArrayOf(3, 7, 14, 30)

        MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Jatuh Tempo")
            .setItems(options) { _, which ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = hutang.jatuhTempo
                calendar.add(Calendar.DAY_OF_YEAR, days[which])
                
                val newTempo = calendar.timeInMillis
                
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().updateHutang(hutang.copy(jatuhTempo = newTempo))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HutangActivity, "Jatuh tempo berhasil diperpanjang", Toast.LENGTH_SHORT).show()
                        loadHutang()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadHutang() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.transaksiDao().getHutangAktif()
            withContext(Dispatchers.Main) {
                adapter.updateData(list)
                if (list.isEmpty()) {
                    listView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    listView.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun showTambahHutangDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Catat Hutang Baru")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 24, 60, 24)

        val btnPick = MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_TonalButton)
        btnPick.text = "Pilih dari Kontak 👤"
        btnPick.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        }
        layout.addView(btnPick)

        val etNama = EditText(this)
        etNama.hint = "Nama Pelanggan"
        layout.addView(etNama)
        etNamaRef = etNama

        val etWa = EditText(this)
        etWa.hint = "Nomor WhatsApp (Contoh: 0812...)"
        etWa.inputType = android.text.InputType.TYPE_CLASS_PHONE
        layout.addView(etWa)
        etWaRef = etWa

        val etJumlah = EditText(this)
        etJumlah.hint = "Jumlah Hutang (Rp)"
        etJumlah.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etJumlah.addTextChangedListener(CurrencyTextWatcher(etJumlah))
        layout.addView(etJumlah)

        builder.setView(layout)
        builder.setPositiveButton("Simpan") { _, _ ->
            val nama = etNama.text.toString()
            val wa = etWa.text.toString()
            val jmlString = CurrencyUtils.cleanCurrency(etJumlah.text.toString())
            val jumlah = jmlString.toIntOrNull() ?: 0
            
            if (nama.isNotEmpty() && wa.isNotEmpty() && jumlah > 0) {
                val cal = Calendar.getInstance()
                val pinjam = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7) // Default tempo 7 hari
                val tempo = cal.timeInMillis

                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().insertHutang(Hutang(
                        namaPelanggan = nama,
                        nomorWa = wa,
                        jumlah = jumlah,
                        tanggalPinjam = pinjam,
                        jatuhTempo = tempo
                    ))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HutangActivity, "Hutang berhasil dicatat", Toast.LENGTH_SHORT).show()
                        loadHutang()
                    }
                }
            }
        }
        builder.setNegativeButton("Batal") { _, _ ->
            etNamaRef = null
            etWaRef = null
        }
        builder.setOnDismissListener {
            etNamaRef = null
            etWaRef = null
        }
        builder.show()
    }
}
