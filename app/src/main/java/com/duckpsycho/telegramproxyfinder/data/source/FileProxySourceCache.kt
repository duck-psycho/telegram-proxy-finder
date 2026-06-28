package com.duckpsycho.telegramproxyfinder.data.source

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

class FileProxySourceCache(context: Context) {

    private val cacheDir = File(context.filesDir, CACHE_DIR_NAME).apply { mkdirs() }

    fun read(url: String): String? = runCatching {
        val file = cacheFile(url)
        if (!file.exists()) return@runCatching null
        val body = file.readText()
        val lineCount = body.lineSequence().count { it.isNotBlank() }
        Log.w(TAG, "Using cache for $url ($lineCount lines)")
        body
    }
        .onFailure { error -> Log.e(TAG, "Failed to read cache for $url", error) }
        .getOrNull()

    fun write(url: String, body: String) {
        runCatching { cacheFile(url).writeText(body) }
            .onFailure { error -> Log.e(TAG, "Failed to write cache for $url", error) }
    }

    private fun cacheFile(url: String): File = File(cacheDir, urlHash(url))

    companion object {
        private const val TAG = "ProxySources"
        private const val CACHE_DIR_NAME = "proxy-sources-cache"

        private fun urlHash(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(url.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
