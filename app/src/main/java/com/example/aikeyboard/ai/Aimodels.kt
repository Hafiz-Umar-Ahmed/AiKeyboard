package com.example.aikeyboard.ai

import com.example.aikeyboard.data.AiProvider

/** A single request sent to whichever AI provider is currently active. */
data class AiRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val maxOutputTokens: Int = 4096,
    val temperature: Double = 0.7
)

sealed class AiResult {
    data class Success(val text: String, val provider: AiProvider) : AiResult()
    data class Failure(val error: AiError) : AiResult()
}

/** Classifies what went wrong so the caller can show a sensible message. */
sealed class AiError(val message: String) {
    data class RateLimited(val provider: AiProvider) :
        AiError("${provider.displayName} rate limit reached")

    data class AuthError(val provider: AiProvider, val detail: String) :
        AiError("${provider.displayName} auth error: $detail")

    data class NetworkError(val provider: AiProvider, val detail: String) :
        AiError("${provider.displayName} network error: $detail")

    data class ProviderError(val provider: AiProvider, val detail: String) :
        AiError("${provider.displayName} error: $detail")

    data object NoProviderConfigured :
        AiError("No AI provider is configured. Add an API key in Settings.")

    data object AllProvidersExhausted :
        AiError("All configured AI providers are unavailable right now. Try again shortly.")
}