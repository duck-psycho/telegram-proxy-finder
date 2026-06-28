package com.duckpsycho.telegramproxyfinder.data.source

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HttpProxySourceLoader(
    private val client: OkHttpClient = defaultClient,
) {

    suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code} for $url")
                }
                response.body?.string().orEmpty()
            }
        }
            .onSuccess { body ->
                val lineCount = body.lineSequence().count { it.isNotBlank() }
                Log.d(TAG, "Loaded $url ($lineCount lines)")
            }
            .onFailure { error -> Log.e(TAG, "Failed to load $url", error) }
            .getOrNull()
    }

    companion object {
        private const val TAG = "ProxySources"
        private const val USER_AGENT = "TelegramProxyFinder/1.1"

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
