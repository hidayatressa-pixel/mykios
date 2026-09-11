package com.app.mykios

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import java.util.*

class RegisterActivity : BaseActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            detectLocationAndSetLanguage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val session = SessionManager(this)
        if (session.isRegistered()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Detect Location on First Run
        if (!session.isLanguageSet()) {
            checkLocationPermission()
        }

        setContentView(R.layout.activity_register)

        // Modern Entrance Animation
        val cardRegister = findViewById<android.view.View>(R.id.cardRegister)
        cardRegister.translationY = 300f
        cardRegister.alpha = 0f
        cardRegister.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        val etNamaToko = findViewById<EditText>(R.id.etNamaToko)
        val etNamaPemilik = findViewById<EditText>(R.id.etNamaPemilik)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val actKategori = findViewById<AutoCompleteTextView>(R.id.actKategori)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val kategoriList = arrayOf("Sembako / Warung", "Konveksi / Pakaian", "Bengkel", "Elektronik", "Makanan & Minuman", "Lainnya")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, kategoriList)
        actKategori.setAdapter(adapter)

        btnRegister.setOnClickListener {
            val toko = etNamaToko.text.toString()
            val pemilik = etNamaPemilik.text.toString()
            val password = etPassword.text.toString()
            val kategori = actKategori.text.toString()

            if (toko.isNotEmpty() && pemilik.isNotEmpty() && kategori.isNotEmpty() && password.isNotEmpty()) {
                session.saveTokoInfo(toko, pemilik, kategori, password)
                Toast.makeText(this, "Toko Berhasil Didaftarkan!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Harap isi semua data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            detectLocationAndSetLanguage()
        }
    }

    private fun detectLocationAndSetLanguage() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val countryCode = addresses[0].countryCode // Contoh: ID, MY, JP
                            if (countryCode != null) {
                                setLanguageByCountry(countryCode)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun setLanguageByCountry(countryCode: String) {
        val session = SessionManager(this)
        val lang = when (countryCode.uppercase()) {
            "ID" -> "in"
            "MY" -> "ms"
            "CN" -> "zh"
            "JP" -> "ja"
            "DE" -> "de"
            "ES" -> "es"
            "SA", "AE", "EG", "QA", "KW" -> "ar"
            else -> "en"
        }
        
        if (session.getLanguage() != lang) {
            session.setLanguage(lang)
            // Restart current activity to apply language immediately
            val intent = Intent(this, RegisterActivity::class.java)
            finish()
            startActivity(intent)
        }
    }
}
