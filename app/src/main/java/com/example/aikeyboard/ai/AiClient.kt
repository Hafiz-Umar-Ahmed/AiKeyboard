package com.example.aikeyboard.ai

import com.example.aikeyboard.data.AiProvider

/** Implemented once per AI provider (Gemini, DeepSeek, ChatGPT, ...). */
interface AiClient {
    val provider: AiProvider

    /**
     * Executes [request] against this provider using [apiKey].
     * Throws an [AiClientException] subtype on failure so [AiRouter] can decide
     * whether to fail over to the next provider.
     */
    suspend fun generate(apiKey: String, request: AiRequest): String
}

/** Internal exceptions thrown by individual clients; caught and classified by the router. */
sealed class AiClientException(message: String) : Exception(message) {
    class RateLimited(message: String) : AiClientException(message)

    class Error(message: String): AiClientException(message)
    class AuthError(message: String) : AiClientException(message)
    class NetworkError(message: String) : AiClientException(message)
    class ProviderError(message: String) : AiClientException(message)
}