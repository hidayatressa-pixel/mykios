package com.app.mykios

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.google.android.material.textfield.TextInputEditText

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import android.graphics.Color
import com.github.mikephil.charting.components.XAxis

class MainActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var session: SessionManager
    private lateinit var onboarding: GuidedOnboardingHelper
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        session = SessionManager(this)
        onboarding = GuidedOnboardingHelper(this)

        if (!session.isRegistered()) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        if (!session.isOnboardingFinished()) {
            startOnboarding()
        }
        
        checkProfileCompletion()

        checkPinAccess()
        scheduleHutangReminders()
        requestNotificationPermission()

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // GREETING
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvNamaWarung = findViewById<TextView>(R.id.tvNamaWarung)
        
        val sdf = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault())
        val dateString = sdf.format(java.util.Date())
        
        tvUsername.text = "Hai, ${session.getNamaPemilik()} \uD83D\uDC4B"
        tvNamaWarung.text = "$dateString | Warung: ${session.getNamaToko()}"

        // SYNC SIDEBAR (DRAWER)
        val headerView = navView.getHeaderView(0)
        val tvStatusAkun = headerView.findViewById<TextView>(R.id.tvStatusAkun)
        
        if (session.isDev()) {
            tvStatusAkun.text = "DEVELOPER MODE"
            tvStatusAkun.setBackgroundResource(R.drawable.bg_button_kasir) // Using a gradient
        } else if (session.isPro()) {
            tvStatusAkun.text = "PRO MEMBER"
            tvStatusAkun.setBackgroundResource(R.drawable.bg_card_saldo)
        } else {
            tvStatusAkun.text = "FREE USER"
            tvStatusAkun.alpha = 0.6f
            tvStatusAkun.setBackgroundResource(android.R.color.darker_gray)
        }

        // Header Menu Button (Open Drawer)
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Header Info Button
        findViewById<View>(R.id.btnInfo).setOnClickListener {
            showAppInfoDialog()
        }

        // Header Profile Button
        findViewById<View>(R.id.btnProfileHeader).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Drawer Menu Listeners
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_stok -> {
                    startActivity(Intent(this, StokActivity::class.java))
                }
                R.id.nav_hutang -> {
                    startActivity(Intent(this, HutangActivity::class.java))
                }
                R.id.nav_laporan -> {
                    startActivity(Intent(this, LaporanActivity::class.java))
                }
                R.id.nav_pengeluaran -> {
                    startActivity(Intent(this, PengeluaranActivity::class.java))
                }
                R.id.nav_ai -> {
                    if (session.isPro() || session.isDev()) {
                        showAiDialog()
                    } else {
                        Toast.makeText(this, "Fitur AI Pintar hanya untuk PRO!", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_logout -> {
                    session.logout()
                    startActivity(Intent(this, RegisterActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_item -> true
                R.id.nav_transactions_item -> {
                    startActivity(Intent(this, TransaksiActivity::class.java))
                    true
                }
                R.id.nav_stock_item -> {
                    startActivity(Intent(this, StokActivity::class.java))
                    true
                }
                R.id.nav_report_item -> {
                    startActivity(Intent(this, LaporanActivity::class.java))
                    true
                }
                R.id.nav_account_item -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Button Listeners
        findViewById<View>(R.id.btnKasir).setOnClickListener {
            startActivity(Intent(this, TransaksiActivity::class.java))
        }

        findViewById<View>(R.id.menuStock).setOnClickListener {
            startActivity(Intent(this, StokActivity::class.java))
        }

        findViewById<View>(R.id.menuHutang).setOnClickListener {
            startActivity(Intent(this, HutangActivity::class.java))
        }

        findViewById<View>(R.id.menuLaporan).setOnClickListener {
            startActivity(Intent(this, LaporanActivity::class.java))
        }

        findViewById<View>(R.id.menuAi).setOnClickListener {
            if (session.isPro() || session.isDev()) {
                showAiDialog()
            } else {
                Toast.makeText(this, "Fitur AI Pintar hanya untuk PRO!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.menuQris).setOnClickListener {
            val qrisPath = session.getQrisPath()
            if (qrisPath != null) {
                showQrisDialog(qrisPath)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("QRIS Belum Tersedia")
                    .setMessage("Harap upload file yang berisi barcode QRIS terlebih dahulu agar bisa menerima pembayaran digital.")
                    .setPositiveButton("Upload Sekarang") { _, _ ->
                        startActivity(Intent(this, AssetsActivity::class.java))
                    }
                    .setNeutralButton("Buat Baru") { _, _ ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qris.id/register"))
                        startActivity(browserIntent)
                    }
                    .setNegativeButton("Nanti", null)
                    .show()
            }
        }

        findViewById<View>(R.id.menuSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        findViewById<View>(R.id.cardPro).setOnClickListener {
            startActivity(Intent(this, UpgradeProActivity::class.java))
        }

        findViewById<View>(R.id.btnUpgradePro).setOnClickListener {
            startActivity(Intent(this, UpgradeProActivity::class.java))
        }

        findViewById<View>(R.id.cardWalletMini).setOnClickListener {
            startActivity(Intent(this, DompetActivity::class.java))
        }

        // Card PRO visibility
        if (session.isPro() || session.isDev()) {
            findViewById<View>(R.id.cardPro).visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        RevenueCatManager.refresh(
            onResult = { active ->
                runOnUiThread {
                    session.setRevenueCatPro(active)
                    refreshProUi()
                    updateSaldoInteraktif()
                }
            }
        )
        refreshProUi()
        updateSaldoInteraktif()
        syncProfileData()
    }

    private fun refreshProUi() {
        val pro = session.isPro() || session.isDev()
        findViewById<View>(R.id.cardPro)?.visibility = if (pro) View.GONE else View.VISIBLE
        findViewById<View>(R.id.cardChart)?.visibility = if (pro) View.VISIBLE else View.GONE

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val status = navView.getHeaderView(0).findViewById<TextView>(R.id.tvStatusAkun)
        when {
            session.isDev() -> {
                status.text = "DEVELOPER MODE"
                status.alpha = 1f
                status.setBackgroundResource(R.drawable.bg_button_kasir)
            }
            session.isPro() -> {
                status.text = "PRO MEMBER"
                status.alpha = 1f
                status.setBackgroundResource(R.drawable.bg_card_saldo)
            }
            else -> {
                status.text = "FREE USER"
                status.alpha = 0.6f
                status.setBackgroundResource(android.R.color.darker_gray)
            }
        }
    }

    private fun updateChartData() {
        val chart = findViewById<LineChart>(R.id.salesChart) ?: return
        val cardChart = findViewById<View>(R.id.cardChart)
        
        if (!session.isPro() && !session.isDev()) {
            cardChart.visibility = View.GONE
            return
        }
        cardChart.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val salesData = db.transaksiDao().getSalesLast7Days().reversed()
            
            withContext(Dispatchers.Main) {
                if (salesData.isEmpty()) {
                    chart.setNoDataText("Belum ada data transaksi")
                    chart.invalidate()
                    return@withContext
                }

                val entries = salesData.mapIndexed { index, data ->
                    Entry(index.toFloat(), data.total.toFloat())
                }

                val dataSet = LineDataSet(entries, "Penjualan").apply {
                    color = Color.parseColor("#4F46E5")
                    setCircleColor(Color.parseColor("#4F46E5"))
                    lineWidth = 3f
                    circleRadius = 5f
                    setDrawCircleHole(true)
                    valueTextSize = 0f
                    setDrawFilled(true)
                    fillDrawable = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, R.drawable.gradient_primary)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawValues(false)
                }

                chart.apply {
                    data = LineData(dataSet)
                    description.isEnabled = false
                    legend.isEnabled = false
                    
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        textColor = Color.GRAY
                        textSize = 10f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val idx = value.toInt()
                                if (idx >= 0 && idx < salesData.size) {
                                    return salesData[idx].tanggal.substring(5) // MM-DD
                                }
                                return ""
                            }
                        }
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        gridColor = Color.parseColor("#10000000")
                        textColor = Color.GRAY
                        textSize = 10f
                        axisMinimum = 0f
                    }
                    axisRight.isEnabled = false
                    
                    animateX(1000)
                    invalidate()
                }
            }
        }
    }

    private fun syncProfileData() {
        try {
            // Sync Header
            val ivHeader = findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivHeaderProfile)
            val tvInitial = findViewById<TextView>(R.id.tvHeaderInitial)
            
            // Sync Drawer
            val navView: NavigationView = findViewById(R.id.nav_view) ?: return
            if (navView.headerCount == 0) return
            val headerView = navView.getHeaderView(0) ?: return
            
            val ivNavProfile = headerView.findViewById<android.widget.ImageView>(R.id.ivNavLogo)
            val tvNavInitial = headerView.findViewById<TextView>(R.id.tvNavInitial)
            val tvNavToko = headerView.findViewById<TextView>(R.id.tvNavNamaToko)
            val tvNavPemilik = headerView.findViewById<TextView>(R.id.tvNavNamaPemilik)

            tvNavToko?.text = session.getNamaToko()
            tvNavPemilik?.text = session.getNamaPemilik()

            session.getPhotoPath()?.let { path ->
                try {
                    val uri = android.net.Uri.parse(path)
                    ivHeader?.setImageURI(uri)
                    ivNavProfile?.setImageURI(uri)
                    tvInitial?.visibility = View.GONE
                    tvNavInitial?.visibility = View.GONE
                } catch (e: Exception) {
                    showDefaultProfile(tvInitial, tvNavInitial, ivNavProfile, ivHeader)
                }
            } ?: run {
                showDefaultProfile(tvInitial, tvNavInitial, ivNavProfile, ivHeader)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDefaultProfile(tvInitial: TextView?, tvNavInitial: TextView?, ivNavProfile: android.widget.ImageView?, ivHeader: android.widget.ImageView?) {
        val initial = session.getNamaToko()?.take(1)?.uppercase() ?: "A"
        tvInitial?.visibility = View.VISIBLE
        tvInitial?.text = initial
        tvNavInitial?.visibility = View.VISIBLE
        tvNavInitial?.text = initial
        ivNavProfile?.setImageResource(0) 
        ivHeader?.setImageResource(0)
    }

    private fun showAppInfoDialog() {
        val message = "My Kios v1.0\nSolusi Kasir Digital Modern\n\nHubungi Kami:\n📧 Email: hidayatressa@gmail.com\n💬 WhatsApp: 083897514568"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Tentang Aplikasi")
            .setMessage(message)
            .setPositiveButton("Chat WhatsApp") { _, _ ->
                try {
                    val url = "https://api.whatsapp.com/send?phone=6283897514568&text=Halo%20Admin%20My%20Kios"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    startActivity(i)
                } catch (e: Exception) {
                    Toast.makeText(this, "WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun updateSaldoInteraktif() {
        val db = AppDatabase.getDatabase(this)
        val tvSaldo = findViewById<TextView>(R.id.tvSaldo)
        val tvLaba = findViewById<TextView>(R.id.tvLabaBersih)
        val cardSaldo = findViewById<View>(R.id.cardSaldo)
        
        // Ringkasan Hari Ini Views
        val tvMiniTransaksi = findViewById<TextView>(R.id.tvMiniTransaksi)
        val tvMiniHutang = findViewById<TextView>(R.id.tvMiniHutang)
        val tvMiniStok = findViewById<TextView>(R.id.tvMiniStok)
        val rvLowStock = findViewById<RecyclerView>(R.id.rvLowStock)
        val layoutLowStock = findViewById<View>(R.id.layoutLowStock)
        val rvRecent = findViewById<RecyclerView>(R.id.rvRecentTransactions)
        rvRecent.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis

            val total = db.transaksiDao().getTotalRange(startOfDay, endOfDay) ?: 0
            val countTransaksi = db.transaksiDao().getAllTransaksi().filter { it.tanggal in startOfDay..endOfDay }.size
            val countHutang = db.transaksiDao().getCountHutangAktif() ?: 0
            val countLowStok = db.transaksiDao().getCountStokMenipis() ?: 0
            val lowStokList = db.transaksiDao().getBarangStokMenipis()
            val recentList = db.transaksiDao().getRecentTransactions()
            val pengeluaranHariIni = db.transaksiDao().getPengeluaranHariIni() ?: 0
            
            // Logic Laba Bersih (PRO Only)
            var totalModal = 0
            if (session.isPro() || session.isDev()) {
                val detailHariIni = db.transaksiDao().getDetailTransaksiHariIni()
                val allBarang = db.transaksiDao().getAllBarangRaw()
                
                detailHariIni.forEach { detail ->
                    val barang = allBarang.find { it.nama == detail.namaBarang }
                    if (barang != null) {
                        totalModal += (barang.hargaModal * detail.jumlah)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                tvSaldo.text = CurrencyUtils.formatRupiah(total)
                findViewById<TextView>(R.id.tvSaldoDompetMain).text = "Dompet: ${CurrencyUtils.formatRupiah(session.getSaldoDigital())}"
                
                // Update Mini Cards
                tvMiniTransaksi.text = countTransaksi.toString()
                tvMiniHutang.text = countHutang.toString()
                tvMiniStok.text = countLowStok.toString()
                
                // Low Stock Carousel
                if (lowStokList.isNotEmpty()) {
                    layoutLowStock.visibility = View.VISIBLE
                    rvLowStock.adapter = LowStockAdapter(lowStokList)
                } else {
                    layoutLowStock.visibility = View.GONE
                }

                // Recent Transactions
                rvRecent.adapter = RecentTransaksiAdapter(recentList)

                if (session.isPro() || session.isDev()) {
                    val labaBersih = total - totalModal - pengeluaranHariIni
                    tvLaba.visibility = if (session.isOwner()) View.VISIBLE else View.GONE
                    tvLaba.text = "Laba: ${CurrencyUtils.formatRupiah(labaBersih)}"
                } else {
                    tvLaba.visibility = View.GONE
                }
            }
        }
    }

    private fun checkProfileCompletion() {
        val regDate = session.getRegistrationDate()
        val diff = System.currentTimeMillis() - regDate
        val days = diff / (1000 * 60 * 60 * 24)

        if (!session.isProfileComplete() && days >= 7) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Lengkapi Profil ⚠️")
                .setMessage("Sudah 7 hari sejak pendaftaran. Harap lengkapi Alamat Email dan Alamat Kios di Pengaturan untuk keamanan data dan fitur Cloud Sync.")
                .setPositiveButton("Lengkapi Sekarang") { _, _ ->
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun startOnboarding() {
        onboarding.showStep(
            findViewById(R.id.menuStock),
            "Halo 👋\nYuk kita isi barang pertama warungmu agar kasir siap digunakan.",
            "Ayo Mulai!"
        ) {
            findViewById<View>(R.id.menuStock).performClick()
        }
    }

    private fun showAiDialog() {
        val db = AppDatabase.getDatabase(this)
        val aiEngine = AiEngine(db.transaksiDao())
        
        lifecycleScope.launch(Dispatchers.IO) {
            val insight = aiEngine.getInsight()
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("🤖 AI Insight")
                    .setMessage(insight)
                    .setPositiveButton("Oke", null)
                    .show()
            }
        }
    }

    private fun checkPinAccess() {
        if (session.isPinSet() && isFirstLoad) {
            val view = layoutInflater.inflate(R.layout.dialog_pin, null)
            val etPin = view.findViewById<TextInputEditText>(R.id.etPinInput)
            
            val dialog = AlertDialog.Builder(this)
                .setTitle("Masukkan PIN Akses")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Masuk", null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (session.verifyPin(etPin.text.toString())) {
                    isFirstLoad = false
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showQrisDialog(path: String) {
        val view = layoutInflater.inflate(R.layout.dialog_qris_pay, null)
        val ivQris = view.findViewById<android.widget.ImageView>(R.id.ivGeneratedQris)
        view.findViewById<android.view.View>(R.id.btnKonfirmasiBayar).visibility = android.view.View.GONE
        view.findViewById<android.widget.TextView>(R.id.tvQrisAmount).visibility = android.view.View.GONE
        
        try {
            val file = java.io.File(path)
            if (file.exists()) {
                ivQris.setImageBitmap(android.graphics.BitmapFactory.decodeFile(path))
            } else {
                ivQris.setImageURI(Uri.parse(path))
            }
            
            MaterialAlertDialogBuilder(this)
                .setView(view)
                .setTitle("QRIS Pembayaran")
                .setPositiveButton("Tutup", null)
                .setNeutralButton("Ganti QRIS") { _, _ ->
                    startActivity(Intent(this, AssetsActivity::class.java))
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat QRIS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleHutangReminders() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<HutangReminderWorker>(24, TimeUnit.HOURS)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()
            
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "HutangReminder",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
