package com.duckpsycho.telegramproxyfinder.data.source

import android.util.Log
import com.duckpsycho.telegramproxyfinder.BuildConfig
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
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code} for $url")
                }
                response.body?.string().orEmpty()
            }
        }.onSuccess { body ->
            val lineCount = body.lineSequence().count { it.isNotBlank() }
            Log.d(TAG, "Loaded $url ($lineCount lines)")
        }.onFailure { error -> Log.e(TAG, "Failed to load $url", error) }
            .getOrNull()
    }

    companion object {
        private const val TAG = "ProxySources"
        private val USER_AGENT = "TelegramProxyFinder/${BuildConfig.VERSION_NAME}"

        private val defaultClient =
            OkHttpClient
                .Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
    }
}
