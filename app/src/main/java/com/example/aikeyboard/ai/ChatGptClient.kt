package com.example.aikeyboard.ai

import com.example.aikeyboard.data.AiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * OpenAI's chat completions endpoint.
 * Note: OpenAI's API has no ongoing free tier (unlike Gemini) — it's pay-as-you-go.
 * Kept as a third fallback in case the user has credit; an unfunded/invalid key
 * will simply cause this client to fail over like any other provider.
 */
class ChatGptClient(
    private val httpClient: OkHttpClient = AiHttpClient.shared,
    private val model: String = "gpt-4o-mini"
) : AiClient {

    override val provider = AiProvider.CHATGPT

    override suspend fun generate(apiKey: String, request: AiRequest): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val messages = JSONArray().apply {
            if (request.systemInstruction != null) {
                put(JSONObject().put("role", "system").put("content", request.systemInstruction))
            }
            put(JSONObject().put("role", "user").put("content", request.prompt))
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", request.temperature)
            put("max_tokens", request.maxOutputTokens)
        }

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val responseBody = try {
            httpClient.newCall(httpRequest).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> text
                    401, 403 -> throw AiClientException.AuthError("ChatGPT rejected the API key (${response.code})")
                    429 -> throw AiClientException.RateLimited("ChatGPT quota/rate limit hit (429)")
                    in 500..599 -> throw AiClientException.ProviderError("ChatGPT server error (${response.code})")
                    else -> throw AiClientException.ProviderError("ChatGPT returned ${response.code}: ${text.take(200)}")
                }
            }
        } catch (io: IOException) {
            throw AiClientException.NetworkError(io.message ?: "Network error calling ChatGPT")
        }

        return try {
            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            throw AiClientException.ProviderError("Could not parse ChatGPT response: ${e.message}")
        }
    }
}