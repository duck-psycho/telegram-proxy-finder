package com.duckpsycho.telegramproxyfinder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duckpsycho.telegramproxyfinder.data.source.DefaultProxySourceUrls
import com.duckpsycho.telegramproxyfinder.data.source.CachingProxySourceLoader
import com.duckpsycho.telegramproxyfinder.data.source.FileProxySourceCache
import com.duckpsycho.telegramproxyfinder.data.source.HttpProxySourceLoader
import com.duckpsycho.telegramproxyfinder.data.tdlib.TdLibProxyTester
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.identityKey
import com.duckpsycho.telegramproxyfinder.search.ProxySearchPhase
import com.duckpsycho.telegramproxyfinder.search.ProxySearchService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProxyFinderViewModel(
    private val searchService: ProxySearchService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyFinderUiState())
    val uiState: StateFlow<ProxyFinderUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        launchSearch()
    }

    fun refresh() {
        searchJob?.cancel()
        searchJob = null
        launchSearch()
    }

    private fun launchSearch() {
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    status = SearchStatus.LoadingSources,
                    workingProxies = emptyList(),
                )
            }

            searchService.search().collect { phase ->
                when (phase) {
                    ProxySearchPhase.LoadingSources ->
                        updateStatus(SearchStatus.LoadingSources)

                    is ProxySearchPhase.SourcesProgress ->
                        updateStatus(SearchStatus.SourcesProgress(phase.loaded, phase.total))

                    ProxySearchPhase.Parsing ->
                        updateStatus(SearchStatus.Parsing)

                    is ProxySearchPhase.Testing ->
                        updateStatus(SearchStatus.Testing(phase.checked, phase.total))

                    is ProxySearchPhase.ProxyFound ->
                        recordProxy(phase.proxy)

                    is ProxySearchPhase.Completed ->
                        updateStatus(SearchStatus.Completed(phase.foundCount))

                    ProxySearchPhase.NoValidProxies ->
                        updateStatus(SearchStatus.NoValidProxies)

                    is ProxySearchPhase.Failed ->
                        updateStatus(SearchStatus.Failed(phase.message))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateStatus(status: SearchStatus) {
        _uiState.update { it.copy(status = status) }
    }

    private fun recordProxy(proxy: WorkingMtProtoProxy) {
        _uiState.update { state ->
            val withoutOld = state.workingProxies.filterNot { it.identityKey() == proxy.identityKey() }
            state.copy(workingProxies = (withoutOld + proxy).sortedBy { it.pingMs })
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val searchService = ProxySearchService(
                        sourceLoader = CachingProxySourceLoader(
                            httpLoader = HttpProxySourceLoader(),
                            cache = FileProxySourceCache(context),
                        ),
                        tester = TdLibProxyTester(context),
                        sourceUrls = DefaultProxySourceUrls.urls,
                    )
                    return ProxyFinderViewModel(searchService = searchService) as T
                }
            }
    }
}
