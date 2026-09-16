package com.app.mykios

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.restorePurchasesWith

object RevenueCatManager {
    const val ENTITLEMENT_PRO = "pro"

    fun isConfigured(): Boolean = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    fun isPro(customerInfo: CustomerInfo): Boolean =
        customerInfo.entitlements[BuildConfig.REVENUECAT_ENTITLEMENT]?.isActive == true

    fun refresh(onResult: (Boolean) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onResult(false)
            return
        }

        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error -> onError(error.message) },
            onSuccess = { info -> onResult(isPro(info)) }
        )
    }

    fun restore(onResult: (Boolean) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("RevenueCat belum dikonfigurasi.")
            return
        }

        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error -> onError(error.message) },
            onSuccess = { info -> onResult(isPro(info)) }
        )
    }
}
