package com.app.mykios

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val session = SessionManager(this)
        if (session.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        val session = SessionManager(newBase)
        val lang = session.getLanguage()
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onResume() {
        super.onResume()
        LocaleHelper.applyLocale(this)
    }
}
