package com.example.aikeyboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The AI providers the keyboard can call. Order here doubles as the
 * default failover priority used by the AI router in step 2.
 */
enum class AiProvider(val displayName: String) {
    GEMINI("Gemini"),
    DEEPSEEK("DeepSeek"),
    CHATGPT("ChatGPT")
}

/**
 * Encrypted, on-device storage for user-supplied API keys.
 * Keys are stored with AES256-GCM via Jetpack Security and are never logged
 * or transmitted anywhere except directly to the provider they belong to.
 *
 * Usage: create ONE instance (e.g. in Application or MainActivity) and pass
 * it down, since EncryptedSharedPreferences construction is not free.
 */
class ApiKeyStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveKey(provider: AiProvider, key: String) {
        prefs.edit().putString(keyFor(provider), key.trim()).apply()
    }

    fun getKey(provider: AiProvider): String? =
        prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() }

    fun hasKey(provider: AiProvider): Boolean = !getKey(provider).isNullOrBlank()

    fun clearKey(provider: AiProvider) {
        prefs.edit().remove(keyFor(provider)).apply()
    }

    /** Providers that currently have a usable key, in enum declaration order. */
    fun configuredProviders(): List<AiProvider> =
        AiProvider.entries.filter { hasKey(it) }

    fun hasAnyKey(): Boolean = configuredProviders().isNotEmpty()

    private fun keyFor(provider: AiProvider) = "api_key_${provider.name}"

    companion object {
        private const val PREFS_FILE_NAME = "ai_keyboard_secure_prefs"
    }
}