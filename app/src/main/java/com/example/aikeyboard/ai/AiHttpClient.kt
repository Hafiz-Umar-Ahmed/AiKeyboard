package com.example.aikeyboard.ai

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One shared OkHttpClient for every provider — avoids paying connection-pool /
 * thread-pool setup cost per request from a memory-constrained IME process.
 */
object AiHttpClient {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}