package com.duckpsycho.telegramproxyfinder.search

import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy

sealed interface ProxySearchPhase {
    data object LoadingSources : ProxySearchPhase

    data class SourcesProgress(
        val loaded: Int,
        val total: Int,
    ) : ProxySearchPhase

    data object Parsing : ProxySearchPhase

    data class Testing(
        val checked: Int,
        val total: Int,
    ) : ProxySearchPhase

    data class ProxyFound(
        val proxy: WorkingMtProtoProxy,
    ) : ProxySearchPhase

    data class Completed(
        val foundCount: Int,
    ) : ProxySearchPhase

    data object NoValidProxies : ProxySearchPhase

    data class Failed(
        val message: String,
    ) : ProxySearchPhase
}
