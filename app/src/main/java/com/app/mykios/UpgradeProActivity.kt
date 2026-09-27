package com.app.mykios

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.revenuecat.purchases.Package

class UpgradeProActivity : BaseActivity() {
    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar
    private lateinit var plan: TextView
    private lateinit var status: TextView
    private lateinit var subscribe: MaterialButton
    private lateinit var restore: MaterialButton
    private lateinit var packageContainer: LinearLayout
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
        packageContainer = findViewById(R.id.layoutPackages)

        subscribe.setOnClickListener { selectedPackage?.let(::buy) }
        restore.setOnClickListener { restorePurchase() }

        if (!RevenueCatManager.isConfigured()) {
            showConfigurationRequired()
            return
        }

        refreshEntitlement()
    }

    private fun refreshEntitlement() {
        setBusy(true, "Memeriksa status PRO...")
        RevenueCatManager.refresh(
            onResult = { active ->
                runOnUiThread {
                    session.setRevenueCatPro(active)
                    if (active) showProActive() else loadOffering()
                }
            },
            onError = {
                runOnUiThread {
                    // A temporary CustomerInfo refresh failure must not block the
                    // paywall. Offerings can still be fetched from RevenueCat cache/network.
                    loadOffering()
                }
            }
        )
    }

    private fun loadOffering() {
        setBusy(true, "Memuat paket PRO...")
        RevenueCatManager.loadPackages(
            onResult = { packages ->
                runOnUiThread {
                    if (packages.isEmpty()) {
                        showError("Paket PRO belum tersedia.")
                    } else {
                        renderPackages(packages)
                        status.text = "Pilih paket yang sesuai. Akses PRO aktif setelah pembelian terverifikasi."
                        setBusy(false)
                    }
                }
            },
            onError = { runOnUiThread { showError(it) } }
        )
    }

    private fun renderPackages(packages: List<Package>) {
        packageContainer.removeAllViews()
        selectedPackage = packages.firstOrNull()
        packages.forEach { pkg ->
            val card = MaterialCardView(this).apply {
                radius = 18f * resources.displayMetrics.density
                cardElevation = 0f
                strokeWidth = (1f * resources.displayMetrics.density).toInt()
                setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
                setOnClickListener {
                    selectedPackage = pkg
                    renderPackages(packages)
                }
            }
            val selected = pkg.identifier == selectedPackage?.identifier
            card.strokeColor = MaterialColors.getColor(
                card,
                if (selected) com.google.android.material.R.attr.colorPrimary
                else com.google.android.material.R.attr.colorOutline
            )
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18.dp(), 15.dp(), 18.dp(), 15.dp())
            }
            val title = TextView(this).apply {
                text = packageLabel(pkg)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val price = TextView(this).apply {
                text = pkg.product.price.formatted
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary))
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 5.dp(), 0, 0)
            }
            content.addView(title)
            content.addView(price)
            card.addView(content)
            packageContainer.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10.dp() })
        }
        plan.text = selectedPackage?.let { "Dipilih: ${packageLabel(it)} • ${it.product.price.formatted}" } ?: ""
        subscribe.isEnabled = selectedPackage != null
    }

    private fun packageLabel(pkg: Package): String = when (pkg.identifier.lowercase()) {
        "\$rc_monthly" -> "Bulanan"
        "\$rc_annual", "\$rc_yearly" -> "Tahunan"
        "\$rc_lifetime" -> "Lifetime"
        else -> pkg.product.title.ifBlank { pkg.identifier }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

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
        packageContainer.visibility = View.GONE
        plan.text = "MYKIOS PRO AKTIF"
        status.text = "Semua fitur PRO yang tersedia pada versi ini sudah terbuka."
        subscribe.visibility = View.GONE
        restore.visibility = View.GONE
    }

    private fun showConfigurationRequired() {
        progress.visibility = View.GONE
        packageContainer.visibility = View.GONE
        plan.text = "RevenueCat belum dikonfigurasi"
        status.text = "Tambahkan public SDK key RevenueCat ke MYKIOS_REVENUECAT_API_KEY untuk menguji PRO."
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
