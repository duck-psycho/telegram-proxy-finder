package com.duckpsycho.telegramproxyfinder.ui

import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy

data class ProxyFinderUiState(
    val isLoading: Boolean = false,
    val status: SearchStatus = SearchStatus.LoadingSources,
    val workingProxies: List<WorkingMtProtoProxy> = emptyList(),
)
