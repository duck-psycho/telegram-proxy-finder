package com.duckpsycho.telegramproxyfinder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duckpsycho.telegramproxyfinder.data.source.HttpProxySourceLoader
import com.duckpsycho.telegramproxyfinder.data.source.ProxySource
import com.duckpsycho.telegramproxyfinder.data.source.SourceType
import com.duckpsycho.telegramproxyfinder.domain.source.SourceEntryNormalizer
import com.duckpsycho.telegramproxyfinder.domain.source.SourceProxyCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddSourceUiState(
    val isVerifying: Boolean = false,
    val verifiedCount: Int? = null,
    val loadFailed: Boolean = false,
)

class AddSourceViewModel(
    private val httpLoader: HttpProxySourceLoader,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddSourceUiState())
    val uiState: StateFlow<AddSourceUiState> = _uiState.asStateFlow()
    private var verifyJob: Job? = null

    fun resetVerification() {
        verifyJob?.cancel()
        verifyJob = null
        _uiState.value = AddSourceUiState()
    }

    fun verify(
        type: SourceType,
        value: String,
    ) {
        val normalized = normalizeValue(type, value)
        if (normalized.isBlank()) return

        verifyJob?.cancel()
        verifyJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isVerifying = true,
                        verifiedCount = null,
                        loadFailed = false,
                    )
                }

                val count =
                    withContext(Dispatchers.IO) {
                        val body = httpLoader.fetch(ProxySource(type, normalized).resolveUrl())
                        if (body.isNullOrBlank()) return@withContext null
                        SourceProxyCounter.countProxies(body)
                    }

                _uiState.update {
                    if (count == null) {
                        it.copy(
                            isVerifying = false,
                            verifiedCount = null,
                            loadFailed = true,
                        )
                    } else {
                        it.copy(
                            isVerifying = false,
                            verifiedCount = count,
                            loadFailed = false,
                        )
                    }
                }
            }
    }

    companion object {
        fun normalizeValue(
            type: SourceType,
            value: String,
        ): String = SourceEntryNormalizer.normalize(type, value)

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AddSourceViewModel(httpLoader = HttpProxySourceLoader()) as T
        }
    }
}
