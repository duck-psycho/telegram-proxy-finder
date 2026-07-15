package com.duckpsycho.telegramproxyfinder.data.source

import android.content.Context
import android.util.Log
import java.io.File
import java.time.Instant

class ProxySourceJsonStore private constructor(
    context: Context,
) {
    private val file = File(context.filesDir, FILE_NAME)

    fun ensureFileExists() {
        if (file.exists()) return
        writeSeed()
    }

    fun loadEntries(): List<ProxySourceEntry> = runCatching {
        if (!file.exists()) {
            Log.w(TAG, "Proxy sources file is missing")
            return@runCatching emptyList()
        }
        ProxySourceJsonCodec.parse(file.readText())
    }.onFailure { error -> Log.e(TAG, "Failed to load proxy sources from ${file.absolutePath}", error) }
        .getOrElse { emptyList() }

    fun saveEntries(entries: List<ProxySourceEntry>) {
        runCatching { file.writeText(ProxySourceJsonCodec.serialize(entries)) }
            .onFailure { error -> Log.e(TAG, "Failed to save proxy sources", error) }
    }

    fun loadSources(): List<ProxySource> = loadEntries().map { entry -> ProxySource(entry.type, entry.value) }

    private fun writeSeed() {
        val createdAt = Instant.now().toString()
        val entries =
            SEED_SOURCES.map { source ->
                ProxySourceEntry(
                    type = source.type,
                    value = source.value,
                    createdAt = createdAt,
                )
            }
        runCatching { file.writeText(ProxySourceJsonCodec.serialize(entries)) }
            .onFailure { error -> Log.e(TAG, "Failed to write seed proxy sources", error) }
    }

    companion object {
        @Volatile
        private var instance: ProxySourceJsonStore? = null

        fun getInstance(context: Context): ProxySourceJsonStore = instance ?: synchronized(this) {
            instance ?: ProxySourceJsonStore(context.applicationContext).also { store ->
                store.ensureFileExists()
                instance = store
            }
        }

        private const val TAG = "ProxySources"
        private const val FILE_NAME = "proxy_sources.json"

        private val SEED_SOURCES =
            listOf(
                ProxySource(SourceType.Telegram, "tgmtproxylol"),
                ProxySource(SourceType.Telegram, "telemt_free_proxy"),
                ProxySource(SourceType.Telegram, "TProxyRU"),
                ProxySource(
                    SourceType.Web,
                    "https://raw.githubusercontent.com/kort0881/telegram-proxy-collector/refs/heads/main/proxy_all.txt",
                ),
                ProxySource(
                    SourceType.Web,
                    "https://raw.githubusercontent.com/sakha1370/V2rayCollector/refs/heads/main/active_mtproto_proxies.txt",
                ),
                ProxySource(
                    SourceType.Web,
                    "https://raw.githubusercontent.com/devho3ein/tg-proxy/refs/heads/main/proxys/All_Proxys.txt",
                ),
                ProxySource(
                    SourceType.Web,
                    "https://raw.githubusercontent.com/Grim1313/mtproto-for-telegram/refs/heads/master/all_proxies.txt",
                ),
            )
    }
}
