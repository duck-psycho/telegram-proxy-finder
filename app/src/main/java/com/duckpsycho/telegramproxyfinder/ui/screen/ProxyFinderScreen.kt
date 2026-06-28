package com.duckpsycho.telegramproxyfinder.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duckpsycho.telegramproxyfinder.R
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.identityKey
import com.duckpsycho.telegramproxyfinder.ui.ProxyFinderUiState
import com.duckpsycho.telegramproxyfinder.ui.SearchStatus
import com.duckpsycho.telegramproxyfinder.ui.components.ProxyListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyFinderScreen(
    uiState: ProxyFinderUiState,
    onInfoClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onInfoClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.info),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            StatusBar(
                statusText = uiState.status.toDisplayString(),
                isLoading = uiState.isLoading,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.5.dp,
            )
            val pullRefreshState = rememberPullToRefreshState()
            val scope = rememberCoroutineScope()

            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = false,
                onRefresh = {
                    onRefresh()
                    scope.launch {
                        pullRefreshState.animateToHidden()
                    }
                },
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = false,
                        state = pullRefreshState,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ProxyList(
                    proxies = uiState.workingProxies,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SearchStatus.toDisplayString(): String = when (this) {
    SearchStatus.LoadingSources -> stringResource(R.string.status_loading_sources)
    is SearchStatus.SourcesProgress -> stringResource(
        R.string.status_sources_progress,
        loaded,
        total,
    )
    SearchStatus.Parsing -> stringResource(R.string.status_parsing)
    is SearchStatus.Testing -> stringResource(R.string.status_testing, checked, total)
    is SearchStatus.Completed -> stringResource(R.string.status_found_proxies, foundCount)
    SearchStatus.NoValidProxies -> stringResource(R.string.status_no_valid_proxies)
    is SearchStatus.Failed -> {
        val message = message?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.error_unknown)
        stringResource(R.string.status_error, message)
    }
}

@Composable
private fun StatusBar(
    statusText: String,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = statusText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProxyList(
    proxies: List<WorkingMtProtoProxy>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(proxies.size) {
        if (proxies.isEmpty()) return@LaunchedEffect
        val pinnedToTop =
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        if (pinnedToTop) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        state = listState,
    ) {
        if (proxies.isEmpty()) {
            item(key = "pull_refresh_placeholder") {
                Box(modifier = Modifier.fillParentMaxSize())
            }
        }
        items(
            items = proxies,
            key = { it.identityKey() },
        ) { proxy ->
            ProxyListItem(proxy = proxy)
        }
    }
}
