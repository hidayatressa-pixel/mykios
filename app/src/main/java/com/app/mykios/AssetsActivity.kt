package com.app.mykios

import android.net.Uri
import java.io.File
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileOutputStream
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AssetsActivity : BaseActivity() {
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val session = SessionManager(this)
            val localPath = copyUriToInternalStorage(it, "qris_code.png")
            if (localPath != null) {
                session.setQrisPath(localPath)
                Toast.makeText(this, "QRIS berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadQris()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assets)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadQris()

        findViewById<Button>(R.id.btnUploadQris).setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadQris() {
        val session = SessionManager(this)
        val ivQris = findViewById<ImageView>(R.id.ivAssetQris)
        val tvNoQris = findViewById<TextView>(R.id.tvNoQris)
        val qrisPath = session.getQrisPath()

        if (qrisPath != null) {
            try {
                if (qrisPath.startsWith("content://") || qrisPath.startsWith("file://")) {
                    ivQris.setImageURI(Uri.parse(qrisPath))
                } else {
                    val file = File(qrisPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(qrisPath)
                        ivQris.setImageBitmap(bitmap)
                    } else {
                        ivQris.visibility = View.GONE
                        tvNoQris.text = "File tidak ditemukan"
                        tvNoQris.visibility = View.VISIBLE
                        return
                    }
                }
                ivQris.visibility = View.VISIBLE
                tvNoQris.visibility = View.GONE
            } catch (e: Exception) {
                ivQris.visibility = View.GONE
                tvNoQris.text = "Gagal memuat gambar: ${e.message}"
                tvNoQris.visibility = View.VISIBLE
            }
        } else {
            ivQris.visibility = View.GONE
            tvNoQris.text = "Belum ada QRIS yang diupload"
            tvNoQris.visibility = View.VISIBLE
        }
    }
}
