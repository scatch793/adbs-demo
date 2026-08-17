package com.omnidapt.pd.real.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val role: String,
    val mustChangePassword: Boolean,
)

class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("omnidapt_secure_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    var serverUrl: String
        get() = preferences.getString("server_url", "http://10.0.2.2:8000")!!
        set(value) {
            preferences.edit().putString("server_url", normalizeUrl(value)).apply()
        }

    fun save(session: AuthSession) {
        preferences.edit().putString("session", encrypt(gson.toJson(session))).apply()
    }

    fun load(): AuthSession? {
        val encoded = preferences.getString("session", null) ?: return null
        return runCatching { gson.fromJson(decrypt(encoded), AuthSession::class.java) }.getOrNull()
    }

    fun clear() {
        preferences.edit().remove("session").apply()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plaintext.encodeToByteArray())
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val ciphertext = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun normalizeUrl(value: String): String =
        value.trim().trimEnd('/').ifBlank { "http://10.0.2.2:8000" }

    companion object {
        private const val KEY_ALIAS = "omnidapt-session-key-v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
    }
}
