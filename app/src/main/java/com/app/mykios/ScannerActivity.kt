package com.app.mykios

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannerActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "SCANNER_MODE"
        const val MODE_CASHIER = "CASHIER"
        const val EXTRA_SCAN_RESULT = "SCAN_RESULT"
    }

    private lateinit var barcodeView: CompoundBarcodeView
    private var isProcessing = false
    private var isFlashOn = false

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                barcodeView.resume()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk scan barcode", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        barcodeView = findViewById(R.id.barcodeScannerView)
        
        findViewById<android.view.View>(R.id.btnCancelScan).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.fabFlashlight).setOnClickListener {
            if (isFlashOn) {
                barcodeView.setTorchOff()
            } else {
                barcodeView.setTorchOn()
            }
            isFlashOn = !isFlashOn
        }

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { code ->
                    if (!isProcessing) {
                        isProcessing = true
                        barcodeView.pause()
                        showScanResultPopup(code)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                barcodeView.resume()
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun showScanResultPopup(code: String) {
        if (intent.getStringExtra(EXTRA_MODE) == MODE_CASHIER) {
            val intent = android.content.Intent()
            intent.putExtra(EXTRA_SCAN_RESULT, code)
            setResult(RESULT_OK, intent)
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val existingBarang = db.transaksiDao().getAllBarangRaw().find { it.kodeBarang == code }
            
            withContext(Dispatchers.Main) {
                if (existingBarang != null) {
                    MaterialAlertDialogBuilder(this@ScannerActivity)
                        .setTitle("Barang Ditemukan")
                        .setMessage("Nama: ${existingBarang.nama}\nStok: ${existingBarang.stok}\nHarga: ${CurrencyUtils.formatRupiah(existingBarang.harga)}\n\nApa yang ingin Anda lakukan?")
                        .setPositiveButton("+1 Stok") { _, _ ->
                            updateStock(existingBarang)
                        }
                        .setNeutralButton("Edit") { _, _ ->
                            // Open Edit dialog (simplified here)
                            Toast.makeText(this@ScannerActivity, "Gunakan menu Stok untuk edit detail", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                            barcodeView.resume()
                        }
                        .setNegativeButton("Kembali") { _, _ ->
                            isProcessing = false
                            barcodeView.resume()
                        }
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this@ScannerActivity)
                        .setTitle("Barang Baru")
                        .setMessage("Kode: $code\nBarang tidak ditemukan di database. Tambahkan sekarang?")
                        .setPositiveButton("Tambah") { _, _ ->
                            // Here we could open a pre-filled Add dialog
                            Toast.makeText(this@ScannerActivity, "Membuka input barang baru...", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                            barcodeView.resume()
                        }
                        .setNegativeButton("Batal") { _, _ ->
                            isProcessing = false
                            barcodeView.resume()
                        }
                        .show()
                }
            }
        }
    }

    private fun updateStock(barang: Barang) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            db.transaksiDao().updateBarang(barang.copy(stok = barang.stok + 1))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ScannerActivity, "${barang.nama}: Stok menjadi ${barang.stok + 1}", Toast.LENGTH_SHORT).show()
                isProcessing = false
                barcodeView.resume()
            }
        }
    }
}
