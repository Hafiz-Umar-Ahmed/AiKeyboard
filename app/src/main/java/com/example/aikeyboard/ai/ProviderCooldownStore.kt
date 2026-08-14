package com.example.aikeyboard.ai

import android.content.Context
import com.example.aikeyboard.data.AiProvider

/**
 * Tracks a short cooldown per provider after it returns a rate-limit error,
 * so the router doesn't immediately hammer a provider that just told us to
 * back off. Not sensitive data, so a plain (unencrypted) prefs file is fine.
 */
class ProviderCooldownStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ai_provider_cooldowns", Context.MODE_PRIVATE)

    fun markRateLimited(provider: AiProvider, cooldownMillis: Long = DEFAULT_COOLDOWN_MS) {
        prefs.edit()
            .putLong(keyFor(provider), System.currentTimeMillis() + cooldownMillis)
            .apply()
    }

    fun isInCooldown(provider: AiProvider): Boolean =
        System.currentTimeMillis() < prefs.getLong(keyFor(provider), 0L)

    fun clear(provider: AiProvider) {
        prefs.edit().remove(keyFor(provider)).apply()
    }

    private fun keyFor(provider: AiProvider) = "cooldown_${provider.name}"

    companion object {
        // Free-tier quotas commonly reset per-minute; 5 minutes is a reasonable
        // "stop bothering this one for a bit" default.
        const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L
    }
}