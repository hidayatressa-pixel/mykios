package com.app.mykios

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {

    private lateinit var driveHelper: GoogleDriveHelper
    private lateinit var ivProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val session = SessionManager(this)
        ivProfile = findViewById(R.id.ivSettingProfile)

        // Load Header Data
        findViewById<TextView>(R.id.tvSettingNamaTokoHeader).text = session.getNamaToko()
        findViewById<TextView>(R.id.tvSettingNamaKasirHeader).text = session.getNamaPemilik()
        
        session.getPhotoPath()?.let { 
            try {
                ivProfile.setImageURI(Uri.parse(it))
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.logo_mykios)
            }
        }

        // --- MENU NAVIGATION ---
        findViewById<View>(R.id.btnMenuProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btnAssetPengguna).setOnClickListener {
            startActivity(Intent(this, AssetsActivity::class.java))
        }

        findViewById<View>(R.id.btnSetPin).setOnClickListener {
            showSetPinDialog(session)
        }

        findViewById<View>(R.id.btnBackupData).setOnClickListener {
            if (session.isPro() || session.isDev()) {
                exportData()
            } else {
                Toast.makeText(this, "Fitur Backup hanya untuk member PRO!", Toast.LENGTH_SHORT).show()
            }
        }

        // Dark Mode Switch
        val switchDark = findViewById<MaterialSwitch>(R.id.switchDarkMode)
        switchDark.isChecked = session.isDarkMode()
        switchDark.setOnCheckedChangeListener { _, isChecked ->
            if (session.isDarkMode() == isChecked) return@setOnCheckedChangeListener
            session.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Hidden / Unused driveHelper initialization if needed in future
        driveHelper = GoogleDriveHelper(this)
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data in case it was updated in EditProfileActivity
        val session = SessionManager(this)
        findViewById<TextView>(R.id.tvSettingNamaTokoHeader).text = session.getNamaToko()
        findViewById<TextView>(R.id.tvSettingNamaKasirHeader).text = session.getNamaPemilik()
        session.getPhotoPath()?.let { 
            try {
                ivProfile.setImageURI(Uri.parse(it))
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.logo_mykios)
            }
        }
    }

    private fun showSetPinDialog(session: SessionManager) {
        val input = EditText(this)
        input.hint = "PIN 6 Digit"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Atur PIN Keamanan")
            .setMessage("Kosongkan jika ingin menghapus PIN.")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val pin = input.text.toString()
                session.setPin(if (pin.isEmpty()) null else pin)
                Toast.makeText(this, "PIN berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1002)
                return
            }
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val listBarang = db.transaksiDao().getAllBarangRaw()
            val listTransaksi = db.transaksiDao().getAllTransaksi()
            val listHutang = db.transaksiDao().getAllHutang()
            
            StokExportUtils.exportAllData(this@SettingsActivity, listBarang, listTransaksi, listHutang)
        }
    }
}
