package com.app.mykios

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.*

class RegisterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val session = SessionManager(this)
        if (session.isRegistered()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Locale is enough for first-run language selection; location permission is unnecessary.
        if (!session.isLanguageSet()) {
            setLanguageByCountry(Locale.getDefault().country.ifBlank { "ID" }, restart = false)
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

    private fun setLanguageByCountry(countryCode: String, restart: Boolean = true) {
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
        
        if (session.getLanguage() != lang || !session.isLanguageSet()) {
            session.setLanguage(lang)
            if (restart) {
                val intent = Intent(this, RegisterActivity::class.java)
                finish()
                startActivity(intent)
            }
        }
    }
}
