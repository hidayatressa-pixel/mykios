package com.app.mykios

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MyKiosApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val apiKey = BuildConfig.REVENUECAT_API_KEY.trim()
        if (apiKey.isBlank()) return

        if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.DEBUG

        Purchases.configure(PurchasesConfiguration.Builder(this, apiKey).build())

        val session = SessionManager(this)
        Purchases.sharedInstance.updatedCustomerInfoListener = { info ->
            session.setRevenueCatPro(RevenueCatManager.isPro(info))
        }

        RevenueCatManager.refresh(
            onResult = { active -> session.setRevenueCatPro(active) }
        )
    }
}
