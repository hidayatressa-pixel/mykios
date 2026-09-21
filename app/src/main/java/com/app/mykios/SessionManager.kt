package com.app.mykios

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveTokoInfo(namaToko: String, namaPemilik: String, kategori: String, password: String) {
        val editor = prefs.edit()
        editor.putString("nama_toko", namaToko)
        editor.putString("nama_pemilik", namaPemilik)
        editor.putString("kategori_usaha", kategori)
        editor.putString("app_password", SecurityUtils.hashSecret(password))
        editor.putLong("registration_timestamp", System.currentTimeMillis())
        editor.putBoolean("is_dev", false)
        editor.putBoolean("is_registered", true)
        editor.apply()
    }

    fun getRegistrationDate(): Long = prefs.getLong("registration_timestamp", 0L)
    fun setEmail(email: String) = prefs.edit().putString("user_email", email).apply()
    fun getEmail(): String? = prefs.getString("user_email", null)
    fun setPhone(phone: String) = prefs.edit().putString("user_phone", phone).apply()
    fun getPhone(): String? = prefs.getString("user_phone", null)
    fun setAlamatDetail(alamat: String) = prefs.edit().putString("alamat_detail", alamat).apply()
    fun getAlamatDetail(): String? = prefs.getString("alamat_detail", null)
    fun setKelurahan(kel: String) = prefs.edit().putString("alamat_kel", kel).apply()
    fun getKelurahan(): String? = prefs.getString("alamat_kel", null)
    fun setKecamatan(kec: String) = prefs.edit().putString("alamat_kec", kec).apply()
    fun getKecamatan(): String? = prefs.getString("alamat_kec", null)
    fun setKota(kota: String) = prefs.edit().putString("alamat_kota_kab", kota).apply()
    fun getKota(): String? = prefs.getString("alamat_kota_kab", null)
    fun setProvinsi(prov: String) = prefs.edit().putString("alamat_prov", prov).apply()
    fun getProvinsi(): String? = prefs.getString("alamat_prov", null)
    fun setKodePos(kodepos: String) = prefs.edit().putString("alamat_kodepos", kodepos).apply()
    fun getKodePos(): String? = prefs.getString("alamat_kodepos", null)
    fun setAlamatToko(alamat: String) = prefs.edit().putString("alamat_toko", alamat).apply()
    fun getAlamatToko(): String = prefs.getString("alamat_toko", "Alamat belum diatur") ?: "Alamat belum diatur"
    fun setPhotoPath(path: String) = prefs.edit().putString("profile_photo", path).apply()
    fun getPhotoPath(): String? = prefs.getString("profile_photo", null)

    fun isProfileComplete(): Boolean = !getEmail().isNullOrEmpty() && getAlamatToko() != "Alamat belum diatur"

    // Existing PRO flavor remains supported during migration. Subscription PRO is only
    // updated after RevenueCat returns a verified CustomerInfo entitlement.
    fun isPro(): Boolean = BuildConfig.MYKIOS_PRO_EDITION || prefs.getBoolean("revenuecat_pro_active", false)
    fun setRevenueCatPro(active: Boolean) = prefs.edit().putBoolean("revenuecat_pro_active", active).apply()
    fun isDev(): Boolean = prefs.getBoolean("is_dev", false)
    fun getKategori(): String = prefs.getString("kategori_usaha", "Sembako") ?: "Sembako"
    fun getNamaToko(): String? = prefs.getString("nama_toko", "Toko Saya")
    fun getNamaPemilik(): String? = prefs.getString("nama_pemilik", "Pemilik")
    fun updateProfileIdentity(namaToko: String, namaPemilik: String) {
        prefs.edit()
            .putString("nama_toko", namaToko)
            .putString("nama_pemilik", namaPemilik)
            .apply()
    }
    fun isRegistered(): Boolean = prefs.getBoolean("is_registered", false)

    fun logout() { prefs.edit().clear().apply() }
    fun setLanguage(lang: String) { prefs.edit().putString("lang", lang).putBoolean("is_lang_set", true).apply() }
    fun isLanguageSet(): Boolean = prefs.getBoolean("is_lang_set", false)
    fun getLanguage(): String = prefs.getString("lang", "in") ?: "in"
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean("dark_mode", enabled).apply()
    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setQrisPath(path: String) { prefs.edit().putString("qris_path", path).apply() }
    fun getQrisPath(): String? = prefs.getString("qris_path", null)
    fun getSaldoDigital(): Int = prefs.getInt("saldo_digital", 0)
    fun addSaldoDigital(amount: Int) { prefs.edit().putInt("saldo_digital", getSaldoDigital() + amount).apply() }

    fun setPin(pin: String?) {
        val value = pin?.takeIf { it.isNotBlank() }?.let(SecurityUtils::hashSecret)
        prefs.edit().putString("app_pin", value).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("app_pin", null) ?: return false
        if (SecurityUtils.isHashed(stored)) return SecurityUtils.verifySecret(pin, stored)
        val matches = java.security.MessageDigest.isEqual(pin.toByteArray(), stored.toByteArray())
        if (matches) setPin(pin)
        return matches
    }

    fun isPinSet(): Boolean = !prefs.getString("app_pin", null).isNullOrEmpty()
    fun setRole(role: String) = prefs.edit().putString("user_role", role).apply()
    fun getRole(): String = prefs.getString("user_role", "OWNER") ?: "OWNER"
    fun isOwner(): Boolean = getRole() == "OWNER"
    fun isIntroDone(): Boolean = prefs.getBoolean("intro_done", false)
    fun setIntroDone(done: Boolean) = prefs.edit().putBoolean("intro_done", done).apply()
    fun isOnboardingFinished(): Boolean = prefs.getBoolean("onboarding_done", false)
    fun setOnboardingFinished(done: Boolean) = prefs.edit().putBoolean("onboarding_done", done).apply()
    fun getXP(): Int = prefs.getInt("user_xp", 0)
    fun addXP(amount: Int) { prefs.edit().putInt("user_xp", getXP() + amount).apply() }
    fun getLevel(): Int = (getXP() / 100) + 1
    fun isTaskCompleted(taskId: String): Boolean = prefs.getBoolean("task_$taskId", false)
    fun setTaskCompleted(taskId: String) = prefs.edit().putBoolean("task_$taskId", true).apply()
}
