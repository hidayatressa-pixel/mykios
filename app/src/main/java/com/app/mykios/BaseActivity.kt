package com.app.mykios

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val session = SessionManager(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, session.getLanguage()))
    }
}
