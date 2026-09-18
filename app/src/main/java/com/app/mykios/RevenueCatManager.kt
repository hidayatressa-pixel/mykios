package com.app.mykios

import android.app.Activity
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith

object RevenueCatManager {
    fun isConfigured(): Boolean = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    fun isPro(customerInfo: CustomerInfo): Boolean =
        customerInfo.entitlements[BuildConfig.REVENUECAT_ENTITLEMENT]?.isActive == true

    fun refresh(onResult: (Boolean) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("RevenueCat belum dikonfigurasi pada build ini.")
            return
        }
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { onError(it.message) },
            onSuccess = { onResult(isPro(it)) }
        )
    }

    fun loadPackages(onResult: (List<Package>) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("RevenueCat belum dikonfigurasi pada build ini.")
            return
        }
        Purchases.sharedInstance.getOfferingsWith(
            onError = { onError(it.message) },
            onSuccess = { offerings ->
                val packages = offerings.current?.availablePackages.orEmpty()
                if (packages.isEmpty()) onError("Paket PRO belum tersedia.")
                else onResult(packages)
            }
        )
    }

    fun purchase(
        activity: Activity,
        packageToPurchase: Package,
        onResult: (Boolean) -> Unit,
        onCancelled: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (userCancelled) onCancelled() else onError(error.message)
            },
            onSuccess = { _, info -> onResult(isPro(info)) }
        )
    }

    fun restore(onResult: (Boolean) -> Unit, onError: (String) -> Unit = {}) {
        if (!isConfigured()) {
            onError("RevenueCat belum dikonfigurasi pada build ini.")
            return
        }
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { onError(it.message) },
            onSuccess = { onResult(isPro(it)) }
        )
    }
}
