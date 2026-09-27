package com.app.mykios

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Cloud identity/authority layer.
 * Room remains the local-first operational database.
 *
 * Expected Firestore document: users/{firebaseUid}
 * fields: role, namaToko, namaPemilik, kategori, active
 */
object FirebaseAccountManager {
    data class CloudProfile(
        val role: String,
        val namaToko: String,
        val namaPemilik: String,
        val kategori: String,
        val active: Boolean
    )

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isConfigured(): Boolean = try {
        com.google.firebase.FirebaseApp.getApps(MyKiosApplication.instance).isNotEmpty()
    } catch (_: Exception) {
        false
    }

    fun signIn(
        email: String,
        password: String,
        onSuccess: (CloudProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onError("Firebase belum dikonfigurasi. Tambahkan app/google-services.json.")
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    auth.signOut()
                    onError("Akun Firebase tidak valid.")
                    return@addOnSuccessListener
                }
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            auth.signOut()
                            onError("Profil akun tidak ditemukan.")
                            return@addOnSuccessListener
                        }
                        val active = doc.getBoolean("active") ?: true
                        if (!active) {
                            auth.signOut()
                            onError("Akun dinonaktifkan.")
                            return@addOnSuccessListener
                        }
                        val role = doc.getString("role")?.uppercase() ?: "OWNER"
                        onSuccess(
                            CloudProfile(
                                role = role,
                                namaToko = doc.getString("namaToko").orEmpty(),
                                namaPemilik = doc.getString("namaPemilik").orEmpty(),
                                kategori = doc.getString("kategori").orEmpty(),
                                active = active
                            )
                        )
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        onError(it.localizedMessage ?: "Gagal membaca profil akun.")
                    }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Login gagal.") }
    }

    fun signOut() {
        if (isConfigured()) auth.signOut()
    }
}
