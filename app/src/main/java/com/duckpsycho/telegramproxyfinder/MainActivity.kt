package com.duckpsycho.telegramproxyfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duckpsycho.telegramproxyfinder.ui.ProxyFinderViewModel
import com.duckpsycho.telegramproxyfinder.ui.screen.InfoScreen
import com.duckpsycho.telegramproxyfinder.ui.screen.ProxyFinderScreen
import com.duckpsycho.telegramproxyfinder.ui.theme.MTProtoProxyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ProxyFinderViewModel = viewModel(
                factory = ProxyFinderViewModel.factory(applicationContext),
            )

            MTProtoProxyTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var showInfoScreen by remember { mutableStateOf(false) }

                if (showInfoScreen) {
                    InfoScreen(onBack = { showInfoScreen = false })
                } else {
                    ProxyFinderScreen(
                        uiState = uiState,
                        onInfoClick = { showInfoScreen = true },
                        onRefresh = viewModel::refresh,
                    )
                }
            }
        }
    }
}
