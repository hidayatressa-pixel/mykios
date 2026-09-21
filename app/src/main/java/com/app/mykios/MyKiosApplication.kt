package com.app.mykios

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

class MyKiosApplication : Application(), UpdatedCustomerInfoListener {
    override fun onCreate() {
        super.onCreate()

        // Apply the persisted theme once for the whole process. Doing this from
        // every Activity can trigger repeated recreation during startup.
        AppCompatDelegate.setDefaultNightMode(
            if (SessionManager(this).isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

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
