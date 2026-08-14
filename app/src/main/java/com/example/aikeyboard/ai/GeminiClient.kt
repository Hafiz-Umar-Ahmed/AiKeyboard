package com.example.aikeyboard.ai

import android.util.Log
import com.example.aikeyboard.data.AiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Google Gemini via the Generative Language API. This is the only one of the
 * three providers with a genuine, ongoing free tier (rate-limited per minute/day).
 */
class GeminiClient(
    private val httpClient: OkHttpClient = AiHttpClient.shared,
    private val model: String = "gemini-3.5-flash"
) : AiClient {

    override val provider = AiProvider.GEMINI

    override suspend fun generate(apiKey: String, request: AiRequest): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contents = JSONArray().put(
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", request.prompt))
            )
        )

        val body = JSONObject().apply {
            put("contents", contents)
            if (request.systemInstruction != null) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", request.systemInstruction))
                    )
                )
            }
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxOutputTokens)
                }
            )
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val responseBody = try {
            httpClient.newCall(httpRequest).execute().use { response ->
                val text = response.body?.string().orEmpty()
                Log.d("GeminiResponse",text)
                when (response.code) {
                    200 -> text
                    401, 403 -> throw AiClientException.AuthError("Gemini rejected the API key (${response.code})")
                    429 -> throw AiClientException.RateLimited("Gemini quota/rate limit hit (429)")
                    in 500..599 -> throw AiClientException.ProviderError("Gemini server error (${response.code})")
                    else -> throw AiClientException.ProviderError("Gemini returned ${response.code}: ${text.take(200)}")


                }
            }
        } catch (io: IOException) {
            throw AiClientException.NetworkError(io.message ?: "Network error calling Gemini")
        }

        return try {
            JSONObject(responseBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            throw AiClientException.ProviderError("Could not parse Gemini response: ${e.message}")
        }
    }
}