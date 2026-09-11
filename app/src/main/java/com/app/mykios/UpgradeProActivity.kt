package com.app.mykios

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class UpgradeProActivity : BaseActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_pro)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webViewUpgrade)

        webView.settings.javaScriptEnabled = false
        webView.settings.domStorageEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.contains("status_code=202") || url.contains("cancel")) {
                    Toast.makeText(this@UpgradeProActivity, "Pembayaran Dibatalkan", Toast.LENGTH_SHORT).show()
                    finish()
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = android.view.View.GONE
            }
        }

        webView.loadUrl("file:///android_asset/upgrade_web.html")
    }

    // PRO cannot be unlocked by WebView/URL callbacks. The PRO edition is a separately
    // signed build flavor. Integrate Play Billing or a server-verified license before
    // offering in-app upgrades.

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
