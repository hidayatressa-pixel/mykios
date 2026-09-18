package com.app.mykios

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class EditProfileActivity : BaseActivity() {

    private lateinit var session: SessionManager
    private lateinit var ivProfile: ImageView
    private var photoUri: Uri? = null

    private lateinit var etNamaToko: EditText
    private lateinit var etNamaPemilik: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAlamatDetail: EditText
    private lateinit var etKelurahan: EditText
    private lateinit var etKecamatan: EditText
    private lateinit var etKota: EditText
    private lateinit var etProvinsi: EditText
    private lateinit var etKodePos: EditText

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ivProfile.setImageURI(it)
            savePhotoToInternal(it)
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let {
                ivProfile.setImageURI(it)
                session.setPhotoPath(it.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        session = SessionManager(this)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        initViews()
        loadData()

        findViewById<View>(R.id.btnChangePhoto).setOnClickListener { showPhotoOptions() }
        findViewById<View>(R.id.btnGetLocation).setOnClickListener { checkLocationPermission() }
        findViewById<View>(R.id.btnSaveProfile).setOnClickListener { validateAndConfirm() }

        etKodePos.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                findViewById<View>(R.id.btnSaveProfile).performClick()
                true
            } else false
        }
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.ivEditProfile)
        etNamaToko = findViewById(R.id.etEditNamaToko)
        etNamaPemilik = findViewById(R.id.etEditNamaPemilik)
        etPhone = findViewById(R.id.etEditPhone)
        etEmail = findViewById(R.id.etEditEmail)
        etAlamatDetail = findViewById(R.id.etEditAlamatDetail)
        etKelurahan = findViewById(R.id.etEditKelurahan)
        etKecamatan = findViewById(R.id.etEditKecamatan)
        etKota = findViewById(R.id.etEditKota)
        etProvinsi = findViewById(R.id.etEditProvinsi)
        etKodePos = findViewById(R.id.etEditKodePos)
    }

    private fun loadData() {
        etNamaToko.setText(session.getNamaToko())
        etNamaPemilik.setText(session.getNamaPemilik())
        etPhone.setText(session.getPhone())
        etEmail.setText(session.getEmail())
        etAlamatDetail.setText(session.getAlamatDetail())
        etKelurahan.setText(session.getKelurahan())
        etKecamatan.setText(session.getKecamatan())
        etKota.setText(session.getKota())
        etProvinsi.setText(session.getProvinsi())
        etKodePos.setText(session.getKodePos())

        session.getPhotoPath()?.let { 
            try {
                ivProfile.setImageURI(Uri.parse(it))
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.logo_mykios)
            }
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Ambil Kamera", "Galeri")
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Foto Profil")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile_${System.currentTimeMillis()}.jpg")
                    photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
                    takePhoto.launch(photoUri)
                } else {
                    pickImage.launch("image/*")
                }
            }
            .show()
    }

    private fun savePhotoToInternal(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(filesDir, "profile_user.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                session.setPhotoPath(Uri.fromFile(file).toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getCurrentLocation()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) getCurrentLocation()
        else Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Toast.makeText(this, "Mengambil lokasi...", Toast.LENGTH_SHORT).show()
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fetchAddressFromLocation(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "Izin lokasi tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAddressFromLocation(lat: Double, lng: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(this@EditProfileActivity, Locale.getDefault())
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val addr = addresses[0]
                    withContext(Dispatchers.Main) {
                        etAlamatDetail.setText(addr.getAddressLine(0) ?: "")
                        etKelurahan.setText(addr.subLocality ?: "")
                        etKecamatan.setText(addr.locality ?: "")
                        etKota.setText(addr.subAdminArea ?: "")
                        etProvinsi.setText(addr.adminArea ?: "")
                        etKodePos.setText(addr.postalCode ?: "")
                        Toast.makeText(this@EditProfileActivity, "Alamat berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateAndConfirm() {
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            return
        }

        if (phone.length < 12 || phone.length > 13) {
            etPhone.error = "Nomor HP harus 12-13 digit"
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah data profil sudah benar?")
            .setPositiveButton("Ya, Simpan") { _, _ -> saveData() }
            .setNegativeButton("Cek Lagi", null)
            .show()
    }

    private fun saveData() {
        session.setEmail(etEmail.text.toString())
        session.setPhone(etPhone.text.toString())
        session.setAlamatDetail(etAlamatDetail.text.toString())
        session.setKelurahan(etKelurahan.text.toString())
        session.setKecamatan(etKecamatan.text.toString())
        session.setKota(etKota.text.toString())
        session.setProvinsi(etProvinsi.text.toString())
        session.setKodePos(etKodePos.text.toString())
        
        // Update basic info if changed
        session.saveTokoInfo(
            etNamaToko.text.toString(),
            etNamaPemilik.text.toString(),
            session.getKategori(),
            "NOPASS" // Password stays same
        )

        showSuccessDialog()
    }

    private fun showSuccessDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success_anim, null)
        val ivCheck = dialogView.findViewById<ImageView>(R.id.ivSuccessCheck)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        // Animation
        ivCheck.alpha = 0f
        ivCheck.scaleX = 0f
        ivCheck.scaleY = 0f
        ivCheck.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                ivCheck.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                
                ivCheck.postDelayed({
                    dialog.dismiss()
                    finish()
                }, 1500)
            }
            .start()
    }
}
