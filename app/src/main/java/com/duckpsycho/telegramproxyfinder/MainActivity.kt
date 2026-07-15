package com.duckpsycho.telegramproxyfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duckpsycho.telegramproxyfinder.platform.update.GitHubReleaseChecker
import com.duckpsycho.telegramproxyfinder.platform.update.ReleaseUpdate
import com.duckpsycho.telegramproxyfinder.ui.AddSourceViewModel
import com.duckpsycho.telegramproxyfinder.ui.ProxyFinderViewModel
import com.duckpsycho.telegramproxyfinder.ui.SettingsViewModel
import com.duckpsycho.telegramproxyfinder.ui.screen.AddSourceScreen
import com.duckpsycho.telegramproxyfinder.ui.screen.InfoScreen
import com.duckpsycho.telegramproxyfinder.ui.screen.ProxyFinderScreen
import com.duckpsycho.telegramproxyfinder.ui.screen.SettingsScreen
import com.duckpsycho.telegramproxyfinder.ui.theme.MTProtoProxyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ProxyFinderViewModel =
                viewModel(
                    factory = ProxyFinderViewModel.factory(applicationContext),
                )
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory = SettingsViewModel.factory(applicationContext),
                )
            val addSourceViewModel: AddSourceViewModel =
                viewModel(
                    factory = AddSourceViewModel.factory(applicationContext),
                )

            MTProtoProxyTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val sourceEntries by settingsViewModel.entries.collectAsStateWithLifecycle()
                val isRefreshingCounts by settingsViewModel.isRefreshingCounts.collectAsStateWithLifecycle()
                val addSourceUiState by addSourceViewModel.uiState.collectAsStateWithLifecycle()
                var showInfoScreen by remember { mutableStateOf(false) }
                var showSettingsScreen by remember { mutableStateOf(false) }
                var showAddSourceScreen by remember { mutableStateOf(false) }
                var availableUpdate by remember { mutableStateOf<ReleaseUpdate?>(null) }
                var wasInSettingsFlow by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    GitHubReleaseChecker.checkAsync { update ->
                        if (isFinishing) return@checkAsync
                        availableUpdate = update
                    }
                }

                LaunchedEffect(showSettingsScreen) {
                    if (showSettingsScreen) {
                        settingsViewModel.reload()
                    }
                }

                LaunchedEffect(showSettingsScreen, showAddSourceScreen) {
                    val inSettingsFlow = showSettingsScreen || showAddSourceScreen
                    if (wasInSettingsFlow && !inSettingsFlow && settingsViewModel.consumeSourcesModified()) {
                        viewModel.refresh()
                    }
                    wasInSettingsFlow = inSettingsFlow
                }

                LaunchedEffect(showAddSourceScreen) {
                    if (showAddSourceScreen) {
                        addSourceViewModel.resetVerification()
                    }
                }

                when {
                    showInfoScreen -> {
                        InfoScreen(onBack = { showInfoScreen = false })
                    }

                    showAddSourceScreen -> {
                        AddSourceScreen(
                            uiState = addSourceUiState,
                            onBack = { showAddSourceScreen = false },
                            onVerify = addSourceViewModel::verify,
                            onAdd = { type, value ->
                                val added =
                                    settingsViewModel.addEntry(
                                        type = type,
                                        value = value,
                                        proxyCount = addSourceUiState.verifiedCount ?: 0,
                                    )
                                if (added) showAddSourceScreen = false
                                added
                            },
                            onInputChanged = addSourceViewModel::resetVerification,
                            hasDuplicate = settingsViewModel::hasDuplicate,
                        )
                    }

                    showSettingsScreen -> {
                        SettingsScreen(
                            entries = sourceEntries,
                            isRefreshingCounts = isRefreshingCounts,
                            onBack = { showSettingsScreen = false },
                            onAddClick = { showAddSourceScreen = true },
                            onDelete = settingsViewModel::deleteEntry,
                            onMove = settingsViewModel::moveEntry,
                        )
                    }

                    else -> {
                        ProxyFinderScreen(
                            uiState = uiState,
                            availableUpdate = availableUpdate,
                            onInfoClick = { showInfoScreen = true },
                            onSettingsClick = { showSettingsScreen = true },
                            onRefresh = viewModel::refresh,
                        )
                    }
                }
            }
        }
    }
}
