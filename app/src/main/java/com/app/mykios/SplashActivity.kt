package com.app.mykios

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<View>(R.id.ivSplashLogo)
        val tvAppName = findViewById<View>(R.id.tvAppNameSplash)
        val tvTagline = findViewById<View>(R.id.tvTaglineSplash)

        // Animasi Logo: Pop up & Fade In
        ivLogo.alpha = 0f
        ivLogo.scaleX = 0.5f
        ivLogo.scaleY = 0.5f
        ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animasi Teks: Slide Up
        tvAppName.translationY = 50f
        tvAppName.alpha = 0f
        tvAppName.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(500)
            .start()

        tvTagline.alpha = 0f
        tvTagline.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(800)
            .start()

        // Delay 2.5 detik untuk transisi halus
        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this)
            session.ensureAfterProjectDebugAdmin()
            val intent = when {
                session.isRegistered() -> {
                    NotificationHelper(this).showWelcomeMessage(session.isPro())
                    Intent(this, MainActivity::class.java)
                }
                !session.isIntroDone() -> Intent(this, OnboardingActivity::class.java)
                else -> Intent(this, RegisterActivity::class.java)
            }
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}
