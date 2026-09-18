package com.app.mykios

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.revenuecat.purchases.Package

class UpgradeProActivity : BaseActivity() {
    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar
    private lateinit var plan: TextView
    private lateinit var status: TextView
    private lateinit var subscribe: MaterialButton
    private lateinit var restore: MaterialButton
    private var selectedPackage: Package? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgrade_pro)

        session = SessionManager(this)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        progress = findViewById(R.id.progressBar)
        plan = findViewById(R.id.tvPlan)
        status = findViewById(R.id.tvStatus)
        subscribe = findViewById(R.id.btnSubscribe)
        restore = findViewById(R.id.btnRestore)

        if (session.isPro()) {
            showProActive()
            return
        }

        subscribe.setOnClickListener { selectedPackage?.let(::buy) }
        restore.setOnClickListener { restorePurchase() }
        loadOffering()
    }

    private fun loadOffering() {
        setBusy(true, "Memuat paket PRO...")
        RevenueCatManager.loadPackages(
            onResult = { packages ->
                runOnUiThread {
                    selectedPackage = packages.firstOrNull()
                    val pkg = selectedPackage
                    if (pkg == null) {
                        showError("Paket PRO belum tersedia.")
                    } else {
                        plan.text = "${pkg.product.title}\n${pkg.product.price.formatted}"
                        status.text = "Harga dan periode mengikuti paket yang tersedia di Google Play."
                        setBusy(false)
                    }
                }
            },
            onError = { runOnUiThread { showError(it) } }
        )
    }

    private fun buy(pkg: Package) {
        setBusy(true, "Membuka Google Play...")
        RevenueCatManager.purchase(
            activity = this,
            packageToPurchase = pkg,
            onResult = { active ->
                runOnUiThread {
                    session.setRevenueCatPro(active)
                    if (active) {
                        Toast.makeText(this, "MYKIOS PRO aktif!", Toast.LENGTH_LONG).show()
                        showProActive()
                    } else showError("Pembelian selesai, tetapi entitlement PRO belum aktif.")
                }
            },
            onCancelled = { runOnUiThread { setBusy(false, "Pembelian dibatalkan.") } },
            onError = { runOnUiThread { showError(it) } }
        )
    }

    private fun restorePurchase() {
        setBusy(true, "Memulihkan pembelian...")
        RevenueCatManager.restore(
            onResult = { active ->
                runOnUiThread {
                    session.setRevenueCatPro(active)
                    if (active) {
                        Toast.makeText(this, "Pembelian PRO berhasil dipulihkan.", Toast.LENGTH_LONG).show()
                        showProActive()
                    } else {
                        setBusy(false, "Tidak ditemukan entitlement PRO aktif pada akun Play Store ini.")
                    }
                }
            },
            onError = { runOnUiThread { showError(it) } }
        )
    }

    private fun showProActive() {
        progress.visibility = View.GONE
        plan.text = "MYKIOS PRO AKTIF"
        status.text = "Semua fitur PRO yang tersedia pada versi ini sudah terbuka."
        subscribe.visibility = View.GONE
        restore.visibility = View.GONE
    }

    private fun showError(message: String) {
        setBusy(false, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setBusy(busy: Boolean, message: String? = null) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        subscribe.isEnabled = !busy && selectedPackage != null
        restore.isEnabled = !busy
        message?.let { status.text = it }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
