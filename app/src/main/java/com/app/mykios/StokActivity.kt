package com.app.mykios

import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.os.Build

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView

class StokActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager
    private lateinit var onboarding: GuidedOnboardingHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BarangAdapter
    private lateinit var layoutSelection: android.view.View
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnCloseSelection: android.view.View
    private lateinit var btnDeleteSelected: android.view.View
    private lateinit var fab: FloatingActionButton
    private lateinit var fabScanner: FloatingActionButton
    private lateinit var fabImport: FloatingActionButton
    private lateinit var fabToolbox: FloatingActionButton
    private lateinit var fabMenuToggle: FloatingActionButton
    private var isMenuOpen = false
    private var listBarang: List<Barang> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stok)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        session = SessionManager(this)
        onboarding = GuidedOnboardingHelper(this)

        db = AppDatabase.getDatabase(this)
        recyclerView = findViewById(R.id.rvStok)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        layoutSelection = findViewById(R.id.layoutSelection)
        tvSelectionCount = findViewById(R.id.tvSelectionCount)
        btnCloseSelection = findViewById(R.id.btnCloseSelection)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)

        adapter = BarangAdapter(listOf(), { count ->
            updateSelectionUi(count)
        }) { barang ->
            showOptionsDialog(barang)
        }
        recyclerView.adapter = adapter

        btnCloseSelection.setOnClickListener {
            adapter.clearSelection()
        }

        btnDeleteSelected.setOnClickListener {
            confirmDeleteSelected()
        }
        
        fabScanner = findViewById(R.id.fabScanner)
        fabImport = findViewById(R.id.fabImport)
        fabToolbox = findViewById(R.id.fabToolbox)
        fabMenuToggle = findViewById(R.id.fabMenuToggle)
        fab = findViewById(R.id.fabTambahBarang)

        setupFilterChips()
        setupSwipeToAction()
        loadStok()
        autoFixData()

        if (!session.isOnboardingFinished()) {
            showOnboardingStep2()
        }

        fab.setOnClickListener {
            showTambahDialog()
        }

        fabMenuToggle.setOnClickListener {
            toggleFabMenu()
        }

        fabScanner.setOnClickListener { 
            toggleFabMenu()
            startScanner() 
        }
        
        fabImport.setOnClickListener { 
            toggleFabMenu()
            showImportWarning() 
        }
        
        fabToolbox.setOnClickListener { 
            toggleFabMenu()
            showToolbox() 
        }
    }

    private fun updateSelectionUi(count: Int) {
        if (count > 0) {
            layoutSelection.visibility = android.view.View.VISIBLE
            tvSelectionCount.text = getString(R.string.selection_count_format, count)
            findViewById<android.view.View>(R.id.toolbar).visibility = android.view.View.GONE
        } else {
            layoutSelection.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.toolbar).visibility = android.view.View.VISIBLE
        }
    }

    private fun confirmDeleteSelected() {
        val ids = adapter.getSelectedIds()
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus ${ids.size} Barang?")
            .setMessage("Apakah Anda yakin ingin menghapus ${ids.size} barang terpilih?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    ids.forEach { id ->
                        val barang = listBarang.find { it.id == id }
                        if (barang != null) db.transaksiDao().deleteBarang(barang)
                    }
                    withContext(Dispatchers.Main) {
                        adapter.clearSelection()
                        Toast.makeText(this@StokActivity, "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadStok()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun toggleFabMenu() {
        if (!isMenuOpen) {
            fabScanner.visibility = android.view.View.VISIBLE
            fabImport.visibility = android.view.View.VISIBLE
            fabToolbox.visibility = android.view.View.VISIBLE
            fabMenuToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            isMenuOpen = true
        } else {
            fabScanner.visibility = android.view.View.GONE
            fabImport.visibility = android.view.View.GONE
            fabToolbox.visibility = android.view.View.GONE
            fabMenuToggle.setImageResource(android.R.drawable.ic_input_add)
            isMenuOpen = false
        }
    }

    private fun autoFixData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val barangTanpaKode = db.transaksiDao().getBarangTanpaKode()
            if (barangTanpaKode.isNotEmpty()) {
                barangTanpaKode.forEach { barang ->
                    val randomCode = "MK-${(100..999).random()}-${barang.id}"
                    db.transaksiDao().updateBarang(barang.copy(kodeBarang = randomCode))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StokActivity, "Data stok berhasil dimigrasi ke format barcode", Toast.LENGTH_SHORT).show()
                    loadStok()
                }
            }
        }
    }

    private fun showToolbox() {
        val session = SessionManager(this)
        if (!session.isPro() && !session.isDev()) {
            Toast.makeText(this, "Toolbox hanya untuk member PRO!", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 101)
                return
            }
        }

        val options = arrayOf("Download Excel (CSV)", "Cetak Barcode Massal (PDF)", "Hubungkan Thermal Printer", "Hapus Semua Data Stok (Reset)")
        MaterialAlertDialogBuilder(this)
            .setTitle("Toolbox Persediaan PRO")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1004)
                                return@setItems
                            }
                        }
                        lifecycleScope.launch(Dispatchers.IO) { StokExportUtils.exportToCsv(this@StokActivity, listBarang) }
                    }
                    1 -> lifecycleScope.launch(Dispatchers.IO) { BarcodeUtils.generateBarcodePdf(this@StokActivity, listBarang) }
                    2 -> showPrinterSelection()
                    3 -> confirmResetStok()
                }
            }
            .show()
    }

    private fun confirmResetStok() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Semua Stok?")
            .setMessage("Semua data barang akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus Semua") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().deleteAllBarang()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StokActivity, "Semua data berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                        loadStok()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun showPrinterSelection() {
        val devices = BluetoothPrinterUtils.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Tidak ada printer bluetooth terikat", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = devices.map { it.name ?: "Unknown" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Printer")
            .setItems(deviceNames) { _, which ->
                BluetoothPrinterUtils.connectToDevice(this, devices[which]) {
                    Toast.makeText(this, "Printer Siap!", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun startScanner() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 102)
        } else {
            startActivity(Intent(this, ScannerActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, ScannerActivity::class.java))
        } else if (requestCode == 102) {
            Toast.makeText(this, "Izin kamera diperlukan untuk scanner", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportWarning() {
        val session = SessionManager(this)
        if (!session.isPro() && !session.isDev()) {
            Toast.makeText(this, "Fitur ini hanya untuk member PRO!", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_import_onboarding, null)
        
        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Setuju & Pilih File") { _, _ ->
                pickFile.launch(arrayOf("*/*"))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val contentResolver = contentResolver
            val type = contentResolver.getType(it)
            if (type == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
                importXlsxData(it)
            } else {
                importCsvData(it)
            }
        }
    }

    private fun importXlsxData(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0)
                val listBarangBaru = mutableListOf<Barang>()

                for (i in 1 until sheet.physicalNumberOfRows) { // Skip header
                    val row = sheet.getRow(i) ?: continue
                    
                    val nama = row.getCell(0)?.toString()?.trim() ?: ""
                    if (nama.isEmpty()) continue

                    val harga = try {
                        val cell = row.getCell(1)
                        if (cell?.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            cell.numericCellValue.toInt()
                        } else {
                            cell?.toString()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: 0
                        }
                    } catch (e: Exception) { 0 }

                    val stok = try {
                        val cell = row.getCell(2)
                        if (cell?.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            cell.numericCellValue.toInt()
                        } else {
                            cell?.toString()?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: 0
                        }
                    } catch (e: Exception) { 0 }

                    val kode = row.getCell(3)?.toString()?.trim()?.ifEmpty { "MK-${(100..999).random()}" } ?: "MK-${(100..999).random()}"
                    val satuan = row.getCell(4)?.toString()?.trim() ?: "Pcs"
                    val kategori = row.getCell(5)?.toString()?.trim() ?: "Umum"
                    val warna = row.getCell(6)?.toString()?.trim()
                    val ukuran = row.getCell(7)?.toString()?.trim()

                    listBarangBaru.add(Barang(
                        nama = nama, harga = harga, stok = stok,
                        kodeBarang = kode, satuan = satuan, kategori = kategori,
                        warna = warna, ukuran = ukuran
                    ))
                }
                
                listBarangBaru.forEach { db.transaksiDao().insertBarang(it) }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StokActivity, "Berhasil mengimpor ${listBarangBaru.size} barang dari Excel", Toast.LENGTH_SHORT).show()
                    loadStok()
                }
                workbook.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@StokActivity)
                        .setTitle("Gagal Impor Excel")
                        .setMessage(e.message ?: "Terjadi kesalahan")
                        .setPositiveButton("Oke", null)
                        .show()
                }
            }
        }
    }

    private fun importCsvData(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = inputStream?.bufferedReader()
                val listBarangBaru = mutableListOf<Barang>()
                
                var firstLine = true
                reader?.lineSequence()?.forEach { line ->
                    val currentLine = line.trim()
                    if (currentLine.isEmpty()) return@forEach

                    if (firstLine) {
                        firstLine = false
                        // Check if file starts with Excel binary signature
                        if (currentLine.contains("PK\u0003\u0004") || currentLine.contains("xl/")) {
                            throw Exception("File terdeteksi format Excel (.xlsx). Harap simpan sebagai .CSV terlebih dahulu.")
                        }
                        return@forEach // Skip header
                    }

                    // Detect separator (comma, semicolon, or tab)
                    val separator = when {
                        currentLine.contains(";") -> ";"
                        currentLine.contains("\t") -> "\t"
                        else -> ","
                    }
                    
                    val parts = currentLine.split(separator)
                    
                    if (parts.size >= 2) { // Minimal Nama dan Harga
                        val nama = parts[0].replace("\"", "").trim()
                        
                        // Abaikan jika baris sampah atau terlalu panjang
                        if (nama.length > 150) return@forEach

                        // Membersihkan angka
                        val hargaStr = if (parts.size > 1) parts[1].replace("[^0-9]".toRegex(), "") else "0"
                        val stokStr = if (parts.size > 2) parts[2].replace("[^0-9]".toRegex(), "") else "0"
                        
                        val harga = hargaStr.toIntOrNull() ?: 0
                        val stok = stokStr.toIntOrNull() ?: 0
                        
                        // Kolom Tambahan
                        val kode = if (parts.size > 3) parts[3].replace("\"", "").trim() else "MK-${(100..999).random()}"
                        val satuan = if (parts.size > 4) parts[4].replace("\"", "").trim() else "Pcs"
                        val kategori = if (parts.size > 5) parts[5].replace("\"", "").trim() else "Umum"
                        val warna = if (parts.size > 6) parts[6].replace("\"", "").trim() else null
                        val ukuran = if (parts.size > 7) parts[7].replace("\"", "").trim() else null
                        
                        if (nama.isNotEmpty()) {
                            listBarangBaru.add(Barang(
                                nama = nama, 
                                harga = harga, 
                                stok = stok, 
                                kodeBarang = kode,
                                satuan = satuan,
                                kategori = kategori,
                                warna = warna,
                                ukuran = ukuran
                            ))
                        }
                    }
                }
                
                if (listBarangBaru.isEmpty()) {
                    throw Exception("Tidak ada data valid yang ditemukan. Pastikan format kolom benar.")
                }

                listBarangBaru.forEach { db.transaksiDao().insertBarang(it) }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StokActivity, "Berhasil mengimpor ${listBarangBaru.size} barang", Toast.LENGTH_SHORT).show()
                    loadStok()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@StokActivity)
                        .setTitle("Gagal Impor")
                        .setMessage(e.message ?: "Terjadi kesalahan saat membaca file")
                        .setPositiveButton("Oke", null)
                        .show()
                }
            }
        }
    }

    private fun setupFilterChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFilter)
        val kategoriUsaha = session.getKategori()
        
        chipGroup.removeAllViews()
        
        val categories = mutableListOf("Semua")
        when {
            kategoriUsaha.contains("Sembako", ignoreCase = true) -> {
                categories.addAll(listOf("Makanan", "Minuman", "Sembako"))
            }
            kategoriUsaha.contains("Makanan", ignoreCase = true) -> {
                categories.addAll(listOf("Makanan", "Minuman"))
            }
            kategoriUsaha.contains("Bengkel", ignoreCase = true) -> {
                categories.addAll(listOf("Spare Part", "Oil", "Jasa"))
            }
            kategoriUsaha.contains("Pakaian", ignoreCase = true) || kategoriUsaha.contains("Konveksi", ignoreCase = true) -> {
                categories.addAll(listOf("Atasan", "Bawahan", "Aksesoris"))
            }
            kategoriUsaha.contains("Elektronik", ignoreCase = true) -> {
                categories.addAll(listOf("Gadget", "Komponen", "Aksesoris"))
            }
        }

        categories.forEach { cat ->
            val chip = Chip(this)
            chip.text = cat
            chip.isCheckable = true
            if (cat == "Semua") chip.isChecked = true
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val category = chip.text.toString()
                if (category == "Semua") {
                    loadStok()
                } else {
                    filterByCategory(category)
                }
            }
        }
    }

    private fun filterByCategory(category: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.transaksiDao().getAllBarangRaw().filter { 
                it.kategori?.contains(category, ignoreCase = true) == true || 
                it.satuan?.contains(category, ignoreCase = true) == true // Fallback check
            }
            listBarang = list
            withContext(Dispatchers.Main) {
                adapter.updateData(list)
                findViewById<android.view.View>(R.id.layoutEmptyStok).visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun setupSwipeToAction() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val barang = listBarang[position]

                if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe Right to Edit
                    showEditDialog(barang)
                    adapter.notifyItemChanged(position)
                } else {
                    // Swipe Left to Delete
                    confirmDelete(barang)
                    adapter.notifyItemChanged(position)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun loadStok(query: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = if (query.isEmpty()) {
                db.transaksiDao().getAllBarangList()
            } else {
                db.transaksiDao().searchBarang("%$query%")
            }
            listBarang = list
            withContext(Dispatchers.Main) {
                adapter.updateData(list)
                findViewById<android.view.View>(R.id.layoutEmptyStok).visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_stok, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                loadStok(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun showOptionsDialog(barang: Barang) {
        val options = arrayOf("Edit Barang", "Hapus Barang")
        MaterialAlertDialogBuilder(this)
            .setTitle(barang.nama)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(barang)
                    1 -> confirmDelete(barang)
                }
            }
            .show()
    }

    private fun showEditDialog(barang: Barang) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Edit Barang")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 20, 60, 20)

        val inputNama = TextInputEditText(this)
        inputNama.hint = "Nama Barang"
        inputNama.setText(barang.nama)
        val tilNama = TextInputLayout(this)
        tilNama.addView(inputNama)
        layout.addView(tilNama)

        val inputKode = TextInputEditText(this)
        inputKode.hint = "Kode Barcode"
        inputKode.setText(barang.kodeBarang)
        val tilKode = TextInputLayout(this)
        tilKode.setPadding(0, 16, 0, 0)
        tilKode.addView(inputKode)
        layout.addView(tilKode)

        val inputHarga = TextInputEditText(this)
        inputHarga.hint = "Harga"
        inputHarga.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        inputHarga.setText(barang.harga.toString())
        inputHarga.addTextChangedListener(CurrencyTextWatcher(inputHarga))
        val tilHarga = TextInputLayout(this)
        tilHarga.setPadding(0, 16, 0, 0)
        tilHarga.addView(inputHarga)
        layout.addView(tilHarga)

        val inputStok = TextInputEditText(this)
        inputStok.hint = "Stok"
        inputStok.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        inputStok.setText(barang.stok.toString())
        val tilStok = TextInputLayout(this)
        tilStok.setPadding(0, 16, 0, 0)
        tilStok.addView(inputStok)
        layout.addView(tilStok)

        val kategoriUsaha = session.getKategori()
        val categories = when {
            kategoriUsaha.contains("Sembako", ignoreCase = true) -> arrayOf("Umum", "Makanan", "Minuman", "Sembako")
            kategoriUsaha.contains("Makanan", ignoreCase = true) -> arrayOf("Umum", "Makanan", "Minuman")
            kategoriUsaha.contains("Bengkel", ignoreCase = true) -> arrayOf("Spare Part", "Oil", "Jasa")
            kategoriUsaha.contains("Pakaian", ignoreCase = true) || kategoriUsaha.contains("Konveksi", ignoreCase = true) -> arrayOf("Atasan", "Bawahan", "Aksesoris")
            else -> arrayOf("Umum")
        }

        var selectedKategori = barang.kategori ?: "Umum"
        val tvLabelKategori = TextView(this)
        tvLabelKategori.text = getString(R.string.label_category_product)
        tvLabelKategori.setPadding(0, 16, 0, 4)
        layout.addView(tvLabelKategori)

        val spinnerKategori = Spinner(this)
        spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerKategori.setSelection(categories.indexOf(selectedKategori).coerceAtLeast(0))
        layout.addView(spinnerKategori)
        spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedKategori = categories[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        builder.setView(layout)

        builder.setPositiveButton("Update") { _, _ ->
            val nama = inputNama.text.toString()
            val kode = inputKode.text.toString()
            val hargaString = CurrencyUtils.cleanCurrency(inputHarga.text.toString())
            val stokString = CurrencyUtils.cleanCurrency(inputStok.text.toString())
            
            val harga = hargaString.toIntOrNull() ?: 0
            val stok = stokString.toIntOrNull() ?: 0

            if (nama.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().updateBarang(barang.copy(
                        nama = nama, 
                        kodeBarang = kode, 
                        harga = harga, 
                        stok = stok,
                        kategori = selectedKategori
                    ))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StokActivity, "Barang diperbarui", Toast.LENGTH_SHORT).show()
                        loadStok()
                    }
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun confirmDelete(barang: Barang) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus ${barang.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().deleteBarang(barang)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StokActivity, "Barang dihapus", Toast.LENGTH_SHORT).show()
                        loadStok()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showOnboardingStep2() {
        onboarding.showStep(
            fab,
            "Langkah 2: Klik tombol Tambah Barang ini",
            "OK"
        ) {
            showTambahDialog(isOnboarding = true)
        }
    }

    private fun showTambahDialog(isOnboarding: Boolean = false) {
        val session = SessionManager(this)
        val kategori = session.getKategori()
        
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Tambah Barang Baru")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 24, 60, 24)

        val inputNama = TextInputEditText(this)
        inputNama.hint = "Nama Barang"
        if (isOnboarding) inputNama.setText("Indomie Goreng")
        val tilNama = TextInputLayout(this)
        tilNama.addView(inputNama)
        layout.addView(tilNama)

        val inputKode = TextInputEditText(this)
        inputKode.hint = "Kode Barcode (Opsional)"
        val tilKode = TextInputLayout(this)
        tilKode.setPadding(0, 12, 0, 0)
        tilKode.addView(inputKode)
        layout.addView(tilKode)

        val inputHarga = TextInputEditText(this)
        inputHarga.hint = "Harga Jual"
        inputHarga.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        if (isOnboarding) inputHarga.setText("3500")
        inputHarga.addTextChangedListener(CurrencyTextWatcher(inputHarga))
        val tilHarga = TextInputLayout(this)
        tilHarga.setPadding(0, 12, 0, 0)
        tilHarga.addView(inputHarga)
        layout.addView(tilHarga)

        // PRO FEATURE: Harga Modal
        val inputModal = TextInputEditText(this)
        inputModal.hint = "Harga Modal (Hanya PRO)"
        inputModal.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        inputModal.addTextChangedListener(CurrencyTextWatcher(inputModal))
        val tilModal = TextInputLayout(this)
        tilModal.setPadding(0, 12, 0, 0)
        tilModal.addView(inputModal)
        
        if (session.isPro() || session.isDev()) {
            layout.addView(tilModal)
        }

        val inputStok = TextInputEditText(this)
        inputStok.hint = "Stok Awal"
        inputStok.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        if (isOnboarding) inputStok.setText("40")
        val tilStok = TextInputLayout(this)
        tilStok.setPadding(0, 12, 0, 0)
        tilStok.addView(inputStok)
        layout.addView(tilStok)

        // Logic cerdas berdasarkan kategori
        var inputWarna: TextInputEditText? = null
        var inputUkuran: TextInputEditText? = null
        var selectedSatuan = "Pcs"
        var selectedKategori = "Umum"

        // Tambahkan input Kategori secara dinamis
        val categories = when {
            kategori.contains("Sembako", ignoreCase = true) -> arrayOf("Umum", "Makanan", "Minuman", "Sembako")
            kategori.contains("Makanan", ignoreCase = true) -> arrayOf("Umum", "Makanan", "Minuman")
            kategori.contains("Bengkel", ignoreCase = true) -> arrayOf("Spare Part", "Oil", "Jasa")
            kategori.contains("Pakaian", ignoreCase = true) || kategori.contains("Konveksi", ignoreCase = true) -> arrayOf("Atasan", "Bawahan", "Aksesoris")
            else -> arrayOf("Umum")
        }
        
        val tvLabelKategori = TextView(this)
        tvLabelKategori.text = "Kategori Barang"
        tvLabelKategori.setPadding(0, 12, 0, 4)
        layout.addView(tvLabelKategori)

        val spinnerKategori = Spinner(this)
        spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        layout.addView(spinnerKategori)
        spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedKategori = categories[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        if (kategori.contains("Pakaian") || kategori.contains("Konveksi")) {
            inputWarna = TextInputEditText(this)
            inputWarna.hint = "Warna"
            val tilWarna = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
            tilWarna.setPadding(0, 12, 0, 0)
            tilWarna.addView(inputWarna)
            layout.addView(tilWarna)

            inputUkuran = TextInputEditText(this)
            inputUkuran.hint = "Ukuran (S/M/L/XL)"
            val tilUkuran = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
            tilUkuran.setPadding(0, 12, 0, 0)
            tilUkuran.addView(inputUkuran)
            layout.addView(tilUkuran)
        } else if (kategori.contains("Sembako") || kategori.contains("Warung") || kategori.contains("Bengkel")) {
            val satuanList = if (kategori.contains("Bengkel")) arrayOf("Pcs", "Set", "Botol", "Jasa") 
                             else arrayOf("Pcs", "Kg", "Liter", "Meter", "Bungkus")
            
            val tvLabelSatuan = TextView(this)
            tvLabelSatuan.text = "Satuan"
            tvLabelSatuan.setPadding(0, 12, 0, 4)
            layout.addView(tvLabelSatuan)

            val spinnerSatuan = Spinner(this)
            spinnerSatuan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, satuanList)
            layout.addView(spinnerSatuan)
            spinnerSatuan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    selectedSatuan = satuanList[position]
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }

        builder.setView(layout)

        builder.setPositiveButton("Simpan") { _, _ ->
            val nama = inputNama.text.toString()
            val kode = inputKode.text.toString().ifEmpty { "MK-${(100..999).random()}-${System.currentTimeMillis() % 1000}" }
            
            val hargaString = CurrencyUtils.cleanCurrency(inputHarga.text.toString())
            val modalString = CurrencyUtils.cleanCurrency(inputModal.text.toString())
            val stokString = CurrencyUtils.cleanCurrency(inputStok.text.toString())

            val harga = hargaString.toIntOrNull() ?: 0
            val modal = modalString.toIntOrNull() ?: 0
            val stok = stokString.toIntOrNull() ?: 0
            val warna = inputWarna?.text?.toString()
            val ukuran = inputUkuran?.text?.toString()

            if (nama.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.transaksiDao().insertBarang(Barang(
                        nama = nama, 
                        kodeBarang = kode,
                        harga = harga, 
                        hargaModal = modal,
                        stok = stok,
                        satuan = selectedSatuan,
                        kategori = selectedKategori,
                        warna = warna,
                        ukuran = ukuran
                    ))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StokActivity, "Barang berhasil ditambah", Toast.LENGTH_SHORT).show()
                        NotificationHelper(this@StokActivity).showStockAdded(nama, stok)
                        if (isOnboarding) {
                            finishOnboarding()
                        }
                        loadStok()
                    }
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun finishOnboarding() {
        session.setOnboardingFinished(true)
        session.addXP(100)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Selamat! 🎉")
            .setMessage("Anda telah menyelesaikan setup pertama dan mendapatkan 100 XP!\n\nWarung Anda sekarang siap beroperasi.")
            .setPositiveButton("Buka Kasir") { _, _ ->
                startActivity(Intent(this, TransaksiActivity::class.java))
                finish()
            }
            .show()
    }

    private fun showTambahDialog() {
        showTambahDialog(isOnboarding = false)
    }
}
