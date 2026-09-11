package com.app.mykios

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        session = SessionManager(this)
        viewPager = findViewById(R.id.viewPagerOnboarding)
        btnNext = findViewById(R.id.btnNextOnboarding)
        val tabLayout = findViewById<TabLayout>(R.id.tabIndicator)

        val items = listOf(
            OnboardingItem(
                "Kasir Digital Cepat",
                "Transaksi jadi lebih mudah dan cepat dengan scanner barcode pintar.",
                imageRes = android.R.drawable.ic_menu_add,
                lottieRawRes = R.raw.pos_machine
            ),
            OnboardingItem(
                "Manajemen Stok",
                "Pantau stok barang real-time dan dapatkan peringatan stok menipis.",
                imageRes = android.R.drawable.ic_menu_save,
                lottieRawRes = R.raw.loading_box
            ),
            OnboardingItem(
                "Laporan Otomatis",
                "Dapatkan laporan penjualan dan laba bersih secara otomatis setiap hari.",
                imageRes = android.R.drawable.ic_menu_report_image,
                lottieRawRes = R.raw.file_analysis
            )
        )

        viewPager.adapter = OnboardingAdapter(items)
        
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == items.size - 1) {
                    btnNext.text = "Mulai Sekarang"
                } else {
                    btnNext.text = "Lanjut"
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) {
                viewPager.currentItem += 1
            } else {
                session.setIntroDone(true)
                startActivity(Intent(this, RegisterActivity::class.java))
                finish()
            }
        }

        findViewById<View>(R.id.tvSkip).setOnClickListener {
            session.setIntroDone(true)
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}

data class OnboardingItem(
    val title: String, 
    val desc: String, 
    val imageRes: Int,
    val lottieRawRes: Int = 0
)

class OnboardingAdapter(private val items: List<OnboardingItem>) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivOnboarding)
        val lottie: LottieAnimationView = view.findViewById(R.id.lottieOnboarding)
        val title: TextView = view.findViewById(R.id.tvTitleOnboarding)
        val desc: TextView = view.findViewById(R.id.tvDescOnboarding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.desc.text = item.desc
        
        if (item.lottieRawRes != 0) {
            try {
                holder.lottie.setAnimation(item.lottieRawRes)
                holder.lottie.visibility = View.VISIBLE
                holder.image.visibility = View.GONE
                holder.lottie.playAnimation()
            } catch (e: Exception) {
                // Fallback jika lottie gagal load
                holder.lottie.visibility = View.GONE
                holder.image.visibility = View.VISIBLE
                holder.image.setImageResource(item.imageRes)
            }
        } else {
            holder.lottie.visibility = View.GONE
            holder.image.visibility = View.VISIBLE
            holder.image.setImageResource(item.imageRes)
        }
    }

    override fun getItemCount(): Int = items.size
}
