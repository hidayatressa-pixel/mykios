package com.app.mykios

import android.os.Bundle
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class TransaksiActivity : BaseActivity() {

    private lateinit var totalText: TextView
    private lateinit var kembalianText: TextView
    private lateinit var listView: ListView
    private lateinit var btnSimpan: Button
    private lateinit var etDiskon: EditText
    private lateinit var etTunai: EditText
    private lateinit var db: AppDatabase
    private lateinit var adapter: BarangKasirAdapter

    private val keranjangMap = mutableMapOf<Int, Int>() // barangId -> quantity
    private var listBarangData: List<Barang> = listOf()
    private var subtotal = 0
    private var totalAkhir = 0
    private var diskon = 0
    private var tunai = 0
    private var kembalian = 0

    private lateinit var printerHelper: BluetoothPrinterHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaksi)

        printerHelper = BluetoothPrinterHelper(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        db = AppDatabase.getDatabase(this)

        totalText = findViewById(R.id.totalHarga)
        kembalianText = findViewById(R.id.tvKembalian)
        listView = findViewById(R.id.listBarang)
        btnSimpan = findViewById(R.id.btnSimpan)
        etDiskon = findViewById(R.id.etDiskon)
        etTunai = findViewById(R.id.etTunai)

        adapter = BarangKasirAdapter(this, listOf()) { barang, qty ->
            if (qty > 0) {
                keranjangMap[barang.id!!] = qty
            } else {
                keranjangMap.remove(barang.id!!)
            }
            updateCalculations()
        }
        listView.adapter = adapter

        setupWatchers()
        loadBarang()

        val tilSearch = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSearch)
        tilSearch.setEndIconOnClickListener {
            startActivityForResult(Intent(this, ScannerActivity::class.java), 1001)
        }
        
        tilSearch.setStartIconOnClickListener {
            startVoiceRecognition()
        }

        val etSearch = findViewById<EditText>(R.id.searchBarang)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    val match = listBarangData.find { it.kodeBarang == query }
                    if (match != null) {
                        // Sound/Haptic feedback
                        findViewById<View>(R.id.searchBarang).performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                        val currentQty = keranjangMap[match.id] ?: 0
                        keranjangMap[match.id!!] = currentQty + 1
                        etSearch.setText("") // Clear for next scan
                        updateCalculations()
                        adapter.syncQty(keranjangMap)
                        Toast.makeText(this@TransaksiActivity, "${match.nama} ditambahkan", Toast.LENGTH_SHORT).show()
                    } else {
                        // Normal search filtering
                        val filtered = listBarangData.filter { it.nama.contains(query, ignoreCase = true) }
                        adapter.updateData(filtered)
                    }
                } else {
                    adapter.updateData(listBarangData)
                }
            }
        })
        val rbQris = findViewById<View>(R.id.rbQris)
        val session = SessionManager(this)
        if (session.getQrisPath() == null) {
            rbQris.visibility = View.GONE
        } else {
            rbQris.visibility = View.VISIBLE
        }

        btnSimpan.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            simpanTransaksi()
        }
    }

    private var currentNotaText: String = ""
    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data
            val cursor = contentResolver.query(contactUri!!, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                var number = cursor.getString(numberIndex)
                
                // Clean number and send
                number = number.replace("[^0-9]".toRegex(), "")
                if (number.startsWith("0")) number = "62" + number.substring(1)
                
                WhatsAppBot.kirimNota(this, number, currentNotaText)
                cursor.close()
            }
        }
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                findViewById<EditText>(R.id.searchBarang).setText(spokenText)
                Toast.makeText(this, "Mencari: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama barang...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val code = data?.getStringExtra("SCAN_RESULT")
            if (!code.isNullOrEmpty()) {
                findViewById<EditText>(R.id.searchBarang).setText(code)
            }
        }
    }

    private fun setupWatchers() {
        etDiskon.addTextChangedListener(CurrencyTextWatcher(etDiskon) {
            updateCalculations()
        })
        etTunai.addTextChangedListener(CurrencyTextWatcher(etTunai) {
            updateCalculations()
        })
    }

    private fun loadBarang() {
        lifecycleScope.launch(Dispatchers.IO) {
            listBarangData = db.transaksiDao().getAllBarang()
            withContext(Dispatchers.Main) {
                adapter.updateData(listBarangData)
            }
        }
    }

    private fun updateCalculations() {
        subtotal = 0
        keranjangMap.forEach { (id, qty) ->
            val barang = listBarangData.find { it.id == id }
            if (barang != null) {
                subtotal += (barang.harga * qty)
            }
        }

        val diskonString = CurrencyUtils.cleanCurrency(etDiskon.text.toString())
        val tunaiString = CurrencyUtils.cleanCurrency(etTunai.text.toString())

        diskon = diskonString.toIntOrNull() ?: 0
        tunai = tunaiString.toIntOrNull() ?: 0
        
        totalAkhir = subtotal - diskon
        if (totalAkhir < 0) totalAkhir = 0
        
        kembalian = tunai - totalAkhir
        if (kembalian < 0) kembalian = 0

        totalText.text = "Total: ${CurrencyUtils.formatRupiah(totalAkhir)}"
        kembalianText.text = "Kembalian: ${CurrencyUtils.formatRupiah(kembalian)}"
        
        btnSimpan.isEnabled = subtotal > 0
        btnSimpan.alpha = if (subtotal > 0) 1.0f else 0.5f
    }

    private fun simpanTransaksi() {
        if (keranjangMap.isEmpty()) {
            Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedMetodeId = findViewById<RadioGroup>(R.id.metodeBayar).checkedRadioButtonId
        val metode = when (selectedMetodeId) {
            R.id.rbQris -> "QRIS"
            R.id.rbHutang -> "Hutang"
            else -> "CASH"
        }

        btnSimpan.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val itemsForNota = keranjangMap.mapNotNull { (id, qty) ->
                    listBarangData.find { it.id == id }?.let { it to qty }
                }
                if (itemsForNota.size != keranjangMap.size) {
                    throw IllegalStateException("Ada barang yang sudah tidak tersedia. Muat ulang stok.")
                }

                val transaksiId = db.transaksiDao().createSale(
                    Transaksi(tanggal = System.currentTimeMillis(), total = totalAkhir, metode = metode),
                    itemsForNota
                )

                withContext(Dispatchers.Main) {
                    if (metode == "QRIS") {
                        showQrisGeneratorDialog(transaksiId, itemsForNota, totalAkhir)
                    } else {
                        showActionDialog(transaksiId, itemsForNota, metode)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSimpan.isEnabled = true
                    Toast.makeText(this@TransaksiActivity, e.message ?: "Transaksi gagal disimpan", Toast.LENGTH_LONG).show()
                    loadBarang()
                }
            }
        }
    }

    private fun showQrisGeneratorDialog(transaksiId: Long, items: List<Pair<Barang, Int>>, amount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_qris_pay, null)
        val ivQris = dialogView.findViewById<android.widget.ImageView>(R.id.ivGeneratedQris)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvQrisAmount)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btnKonfirmasiBayar)

        tvAmount.text = "Total: ${CurrencyUtils.formatRupiah(amount)}"
        
        val qrisBmp = BarcodeUtils.generateQris(this, amount)
        ivQris.setImageBitmap(qrisBmp)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnConfirm.setOnClickListener {
            val session = SessionManager(this)
            session.addSaldoDigital(amount) // Add to wallet
            dialog.dismiss()
            showPreviewNotaDialog(transaksiId, items, "QRIS")
        }

        dialog.show()
    }

    private fun showPreviewNotaDialog(transaksiId: Long, items: List<Pair<Barang, Int>>, metode: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nota_preview, null)
        val tvContent = dialogView.findViewById<TextView>(R.id.tvPreviewContent)
        val btnDownload = dialogView.findViewById<View>(R.id.btnDownloadNota)
        val btnPrint = dialogView.findViewById<View>(R.id.btnPrintNota)
        val btnSelesai = dialogView.findViewById<View>(R.id.btnSelesaiNota)

        tvContent.text = NotaUtils.getNotaText(this, transaksiId, totalAkhir, items, metode, diskon, tunai, kembalian)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnDownload.setOnClickListener {
            NotaUtils.generateNota(this, transaksiId, totalAkhir, items, metode, diskon, tunai, kembalian)
        }

        btnPrint.setOnClickListener {
            showPrinterSelection(items)
        }

        btnSelesai.setOnClickListener {
            dialog.dismiss()
            showSuccessDialog(transaksiId, items, metode)
        }

        dialog.show()
    }

    private fun showPrinterSelection(items: List<Pair<Barang, Int>>) {
        val devices = printerHelper.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Tidak ada printer bluetooth terikat", Toast.LENGTH_SHORT).show()
            return
        }

        val names = devices.map { it.name ?: "Unknown" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Printer")
            .setItems(names) { _, which ->
                lifecycleScope.launch {
                    val connected = printerHelper.connect(devices[which])
                    if (connected) {
                        val session = SessionManager(this@TransaksiActivity)
                        val itemStrings = items.map { "${it.first.nama} x${it.second}" }
                        printerHelper.printReceipt(
                            session.getNamaToko() ?: "My Kios",
                            itemStrings,
                            CurrencyUtils.formatRupiah(totalAkhir)
                        )
                        Toast.makeText(this@TransaksiActivity, "Mencetak...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TransaksiActivity, "Gagal menyambung printer", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showActionDialog(transaksiId: Long, items: List<Pair<Barang, Int>>, metode: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Transaksi Berhasil")
            .setMessage("Metode: $metode\nTotal: ${CurrencyUtils.formatRupiah(totalAkhir)}")
            .setPositiveButton("Download Nota") { _, _ ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1003)
                        return@setPositiveButton
                    }
                }
                NotaUtils.generateNota(this, transaksiId, totalAkhir, items, metode, diskon, tunai, kembalian)
                showSuccessDialog(transaksiId, items, metode)
            }
            .setNeutralButton("Print Struk") { _, _ ->
                showPrinterSelection(items)
                showSuccessDialog(transaksiId, items, metode)
            }
            .setNegativeButton("Selesai") { _, _ ->
                showSuccessDialog(transaksiId, items, metode)
            }
            .setCancelable(false)
            .show()
    }

    private fun showSuccessDialog(transaksiId: Long, items: List<Pair<Barang, Int>>, metode: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pembayaran_berhasil, null)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOkBerhasil)
        val btnWa = dialogView.findViewById<Button>(R.id.btnKirimWa)

        // Kirim Notifikasi Transaksi
        NotificationHelper(this).showTransaksiBerhasil(CurrencyUtils.formatRupiah(totalAkhir))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnWa?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
            
            // Simpan data nota sementara untuk dikirim setelah kontak terpilih
            currentNotaText = NotaUtils.getNotaText(this, transaksiId, totalAkhir, items, metode, diskon, tunai, kembalian)
        }

        btnOk.setOnClickListener {
            dialog.dismiss()
            resetTransaksi()
            loadBarang()
            finish()
        }

        dialog.show()
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val session = SessionManager(this)
            val localPath = copyUriToInternalStorage(it, "qris_code.png")
            if (localPath != null) {
                session.setQrisPath(localPath)
                Toast.makeText(this, "QRIS berhasil disimpan", Toast.LENGTH_SHORT).show()
                findViewById<View>(R.id.rbQris).visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_transaksi, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_upload_qris) {
            pickImage.launch("image/*")
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resetTransaksi() {
        keranjangMap.clear()
        subtotal = 0
        totalAkhir = 0
        diskon = 0
        tunai = 0
        kembalian = 0
        
        etDiskon.setText("0")
        etTunai.setText("")
        totalText.text = "Total: ${CurrencyUtils.formatRupiah(0)}"
        kembalianText.text = "Kembalian: ${CurrencyUtils.formatRupiah(0)}"
        adapter.clearQty()
    }
}
