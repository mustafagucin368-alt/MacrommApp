package com.blocklegends.macro

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.SimpleDateFormat
import java.util.*

object KeyManager {

    private const val PREFS_NAME = "secure_license"
    private const val KEY_ACTIVE = "active_key"
    private const val KEY_EXPIRY = "key_expiry"
    private const val ADMIN_PASS_KEY = "admin_pass"
    private const val DEFAULT_ADMIN = "admin123"

    private fun getSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun validateKey(context: Context, inputKey: String): Boolean {
        return try {
            val prefs = getSecurePrefs(context)
            val storedKey = prefs.getString(KEY_ACTIVE, null) ?: return false
            val expiry = prefs.getLong(KEY_EXPIRY, 0L)
            if (inputKey.uppercase() != storedKey) return false
            if (System.currentTimeMillis() > expiry) return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveKey(context: Context, key: String, expiryMillis: Long) {
        getSecurePrefs(context).edit().apply {
            putString(KEY_ACTIVE, key.uppercase())
            putLong(KEY_EXPIRY, expiryMillis)
            apply()
        }
    }

    fun revokeKey(context: Context) {
        getSecurePrefs(context).edit().apply {
            remove(KEY_ACTIVE)
            remove(KEY_EXPIRY)
            apply()
        }
    }

    fun getActiveKey(context: Context): String? {
        return try {
            getSecurePrefs(context).getString(KEY_ACTIVE, null)
        } catch (e: Exception) { null }
    }

    fun getExpiry(context: Context): Long {
        return try {
            getSecurePrefs(context).getLong(KEY_EXPIRY, 0L)
        } catch (e: Exception) { 0L }
    }

    fun generateKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        fun segment() = (1..4).map { chars.random() }.joinToString("")
        return "BLMX-${segment()}-${segment()}-${segment()}"
    }

    fun getExpiryMillis(days: Int): Long =
        System.currentTimeMillis() + days.toLong() * 24L * 60L * 60L * 1000L

    fun formatExpiry(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

    fun validateAdmin(context: Context, password: String): Boolean {
        return try {
            val stored = getSecurePrefs(context).getString(ADMIN_PASS_KEY, DEFAULT_ADMIN)
            password == stored
        } catch (e: Exception) {
            password == DEFAULT_ADMIN
        }
    }

    fun setAdminPassword(context: Context, newPass: String) {
        getSecurePrefs(context).edit().putString(ADMIN_PASS_KEY, newPass).apply()
    }
}
