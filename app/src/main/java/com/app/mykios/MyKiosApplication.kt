package com.app.mykios

import android.app.Application
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

class MyKiosApplication : Application(), UpdatedCustomerInfoListener {
    override fun onCreate() {
        super.onCreate()

        val apiKey = BuildConfig.REVENUECAT_API_KEY.trim()
        if (apiKey.isBlank()) return

        if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.DEBUG

        Purchases.configure(PurchasesConfiguration.Builder(this, apiKey).build())
        Purchases.sharedInstance.updatedCustomerInfoListener = this

        RevenueCatManager.refresh(
            onResult = { active -> SessionManager(this).setRevenueCatPro(active) }
        )
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        SessionManager(this).setRevenueCatPro(RevenueCatManager.isPro(customerInfo))
    }
}
