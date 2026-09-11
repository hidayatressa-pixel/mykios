package com.app.mykios

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private const val PREFIX = "v1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun hashSecret(secret: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(secret.toCharArray(), salt, ITERATIONS)
        return listOf(PREFIX, ITERATIONS.toString(), b64(salt), b64(hash)).joinToString(":")
    }

    fun verifySecret(secret: String, encoded: String): Boolean {
        val parts = encoded.split(":")
        if (parts.size != 4 || parts[0] != PREFIX) return false
        return try {
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = pbkdf2(secret.toCharArray(), salt, iterations)
            java.security.MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) { false }
    }

    fun isHashed(value: String?): Boolean = value?.startsWith("$PREFIX:") == true

    private fun pbkdf2(secret: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret, salt, iterations, KEY_LENGTH)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
