package com.app.mykios

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.etLoginEmail)
        val password = findViewById<EditText>(R.id.etLoginPassword)
        val button = findViewById<MaterialButton>(R.id.btnLogin)
        val progress = findViewById<View>(R.id.loginProgress)

        button.setOnClickListener {
            val mail = email.text.toString().trim()
            val pass = password.text.toString()
            if (mail.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Isi email dan password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            button.isEnabled = false
            progress.visibility = View.VISIBLE
            FirebaseAccountManager.signIn(mail, pass, { profile ->
                runOnUiThread {
                    val session = SessionManager(this)
                    session.saveCloudAccount(
                        profile.namaToko.ifBlank { "After Project" },
                        profile.namaPemilik.ifBlank { "Admin" },
                        profile.kategori.ifBlank { "Fotokopi & ATK" },
                        profile.role
                    )
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }, { message ->
                runOnUiThread {
                    button.isEnabled = true
                    progress.visibility = View.GONE
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            })
        }

        findViewById<View>(R.id.tvBackToRegister).setOnClickListener {
            finish()
        }
    }
}
