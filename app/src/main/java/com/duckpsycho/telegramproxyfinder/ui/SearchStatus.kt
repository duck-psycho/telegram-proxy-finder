package com.duckpsycho.telegramproxyfinder.ui

sealed interface SearchStatus {
    data object LoadingSources : SearchStatus

    data class SourcesProgress(
        val loaded: Int,
        val total: Int,
    ) : SearchStatus

    data object Parsing : SearchStatus

    data class Testing(
        val checked: Int,
        val total: Int,
    ) : SearchStatus

    data class Completed(
        val foundCount: Int,
    ) : SearchStatus

    data object NoValidProxies : SearchStatus

    data class Failed(
        val message: String?,
    ) : SearchStatus
}
