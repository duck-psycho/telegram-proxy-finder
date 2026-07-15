package com.duckpsycho.telegramproxyfinder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duckpsycho.telegramproxyfinder.data.source.HttpProxySourceLoader
import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceEntry
import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceJsonStore
import com.duckpsycho.telegramproxyfinder.data.source.SourceType
import com.duckpsycho.telegramproxyfinder.data.source.stableKey
import com.duckpsycho.telegramproxyfinder.domain.source.SourceEntryNormalizer
import com.duckpsycho.telegramproxyfinder.domain.source.SourceProxyCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class SettingsViewModel(
    private val sourceStore: ProxySourceJsonStore,
    private val httpLoader: HttpProxySourceLoader,
) : ViewModel() {
    private val _entries = MutableStateFlow<List<ProxySourceEntry>>(emptyList())
    val entries: StateFlow<List<ProxySourceEntry>> = _entries.asStateFlow()

    private val _isRefreshingCounts = MutableStateFlow(false)
    val isRefreshingCounts: StateFlow<Boolean> = _isRefreshingCounts.asStateFlow()

    private var sourcesModified = false

    private var refreshCountsJob: Job? = null

    fun reload() {
        viewModelScope.launch {
            refreshCountsJob?.cancel()
            refreshCountsJob = null
            _entries.value = withContext(Dispatchers.IO) { sourceStore.loadEntries() }
            refreshCountsJob = launch { refreshProxyCounts() }
        }
    }

    fun deleteEntry(index: Int) {
        viewModelScope.launch {
            val updated =
                withContext(Dispatchers.IO) {
                    val list = _entries.value.toMutableList()
                    if (index !in list.indices) return@withContext list
                    list.removeAt(index)
                    sourceStore.saveEntries(list)
                    list
                }
            if (updated.size < _entries.value.size) {
                sourcesModified = true
            }
            _entries.value = updated
        }
    }

    fun moveEntry(
        from: Int,
        to: Int,
    ) {
        val list = _entries.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _entries.value = list
        viewModelScope.launch(Dispatchers.IO) {
            sourceStore.saveEntries(list)
        }
    }

    fun hasDuplicate(
        type: SourceType,
        value: String,
    ): Boolean = SourceEntryNormalizer.isDuplicate(_entries.value, type, value)

    suspend fun addEntry(
        type: SourceType,
        value: String,
        proxyCount: Int,
    ): Boolean {
        val normalized = SourceEntryNormalizer.normalize(type, value)
        if (normalized.isBlank()) return false
        if (hasDuplicate(type, value)) return false

        val updated =
            withContext(Dispatchers.IO) {
                val list = _entries.value.toMutableList()
                list.add(
                    ProxySourceEntry(
                        type = type,
                        value = normalized,
                        createdAt = Instant.now().toString(),
                        proxyCount = proxyCount,
                    ),
                )
                sourceStore.saveEntries(list)
                list
            }
        _entries.value = updated
        sourcesModified = true
        return true
    }

    fun consumeSourcesModified(): Boolean {
        val modified = sourcesModified
        if (modified) sourcesModified = false
        return modified
    }

    private suspend fun refreshProxyCounts() {
        val snapshot = _entries.value
        if (snapshot.isEmpty()) return

        _isRefreshingCounts.value = true
        try {
            coroutineScope {
                snapshot
                    .map { entry ->
                        launch(Dispatchers.IO) {
                            val count = SourceProxyCounter.countFromEntry(httpLoader, entry)
                            updateEntryProxyCount(entry, count)
                        }
                    }.joinAll()
            }
            withContext(Dispatchers.IO) {
                sourceStore.saveEntries(_entries.value)
            }
        } finally {
            _isRefreshingCounts.value = false
        }
    }

    private fun updateEntryProxyCount(
        entry: ProxySourceEntry,
        proxyCount: Int?,
    ) {
        val key = entry.stableKey()
        _entries.update { list ->
            val index = list.indexOfFirst { it.stableKey() == key }
            if (index < 0) return@update list
            list.toMutableList().apply {
                set(index, list[index].copy(proxyCount = proxyCount))
            }
        }
    }

    override fun onCleared() {
        refreshCountsJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
                sourceStore = ProxySourceJsonStore.getInstance(context),
                httpLoader = HttpProxySourceLoader(),
            ) as T
        }
    }
}
