package com.example.aikeyboard.ai

import android.content.Context
import com.example.aikeyboard.data.AiProvider
import com.example.aikeyboard.data.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tries each configured provider in order (Gemini -> DeepSeek -> ChatGPT by
 * default — see [AiProvider] enum order), skipping any provider currently in
 * cooldown, and automatically fails over to the next one on rate-limit, auth,
 * network, or provider errors. Returns the first success, or a [AiResult.Failure]
 * describing why every provider was unavailable.
 *
 * Create ONE instance and reuse it (e.g. held by the IME service or a small
 * singleton), since it wraps ApiKeyStore which itself should not be recreated
 * per-call.
 */
class AiRouter(
    context: Context,
    private val apiKeyStore: ApiKeyStore = ApiKeyStore(context),
    private val cooldownStore: ProviderCooldownStore = ProviderCooldownStore(context),
    private val clients: Map<AiProvider, AiClient> = mapOf(
        AiProvider.GEMINI to GeminiClient(),
        AiProvider.DEEPSEEK to DeepSeekClient(),
        AiProvider.CHATGPT to ChatGptClient()
    )
) {

    suspend fun generate(request: AiRequest): AiResult {
        val configured = apiKeyStore.configuredProviders()
        if (configured.isEmpty()) {
            return AiResult.Failure(AiError.NoProviderConfigured)
        }

        val candidates = configured.filterNot { cooldownStore.isInCooldown(it) }
        if (candidates.isEmpty()) {
            return AiResult.Failure(AiError.AllProvidersExhausted)
        }

        var lastError: AiError = AiError.AllProvidersExhausted

        for (provider in candidates) {
            val client = clients[provider] ?: continue
            val apiKey = apiKeyStore.getKey(provider) ?: continue

            when (val result = tryProvider(client, apiKey, request)) {
                is AiResult.Success -> return result
                is AiResult.Failure -> lastError = result.error // fall through to next candidate
            }
        }

        return AiResult.Failure(lastError)
    }

    private suspend fun tryProvider(client: AiClient, apiKey: String, request: AiRequest): AiResult =
        withContext(Dispatchers.IO) {
            try {
                AiResult.Success(client.generate(apiKey, request), client.provider)
            } catch (e: AiClientException.RateLimited) {
                cooldownStore.markRateLimited(client.provider)
                AiResult.Failure(AiError.RateLimited(client.provider))
            } catch (e: AiClientException.AuthError) {
                AiResult.Failure(AiError.AuthError(client.provider, e.message ?: "unknown"))
            } catch (e: AiClientException.NetworkError) {
                AiResult.Failure(AiError.NetworkError(client.provider, e.message ?: "unknown"))
            } catch (e: AiClientException.ProviderError) {
                AiResult.Failure(AiError.ProviderError(client.provider, e.message ?: "unknown"))
            } catch (e: Exception) {
                AiResult.Failure(AiError.ProviderError(client.provider, e.message ?: "unexpected error"))
            }
        }
}