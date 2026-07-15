package com.duckpsycho.telegramproxyfinder.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duckpsycho.telegramproxyfinder.BuildConfig
import com.duckpsycho.telegramproxyfinder.R
import com.duckpsycho.telegramproxyfinder.data.source.ProxySourceEntry
import com.duckpsycho.telegramproxyfinder.data.source.SourceType
import com.duckpsycho.telegramproxyfinder.data.source.stableKey
import com.duckpsycho.telegramproxyfinder.ui.util.CreatedAtFormatter
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    entries: List<ProxySourceEntry>,
    isRefreshingCounts: Boolean,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    val pendingDeleteEntry = pendingDeleteIndex?.let { entries.getOrNull(it) }

    if (pendingDeleteEntry != null && pendingDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text(stringResource(R.string.delete_source_title)) },
            text = {
                Text(stringResource(R.string.delete_source_message, pendingDeleteEntry.value))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val index = pendingDeleteIndex
                        if (index != null) {
                            onDelete(index)
                        }
                        pendingDeleteIndex = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sources_title),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_source),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_sources),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val lazyListState = rememberLazyListState()
                val reorderableState =
                    rememberReorderableLazyListState(lazyListState) { from, to ->
                        onMove(from.index, to.index)
                    }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(
                        items = entries,
                        key = { _, entry -> entry.stableKey() },
                    ) { index, entry ->
                        ReorderableItem(
                            state = reorderableState,
                            key = entry.stableKey(),
                        ) { isDragging ->
                            ProxySourceListItem(
                                entry = entry,
                                isDragging = isDragging,
                                isRefreshingCounts = isRefreshingCounts,
                                onDelete = { pendingDeleteIndex = index },
                                dragHandle = {
                                    IconButton(
                                        modifier = Modifier.longPressDraggableHandle(),
                                        onClick = {},
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_drag_handle),
                                            contentDescription = stringResource(R.string.reorder_source),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProxySourceListItem(
    entry: ProxySourceEntry,
    isDragging: Boolean,
    isRefreshingCounts: Boolean,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val typeLabel =
        when (entry.type) {
            SourceType.Telegram -> stringResource(R.string.source_type_telegram)
            SourceType.Web -> stringResource(R.string.source_type_web)
        }
    val dragBackground =
        if (isDragging) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.background
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(dragBackground)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            dragHandle()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = entry.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text =
                when {
                    isRefreshingCounts && entry.proxyCount == null ->
                        stringResource(R.string.source_proxy_count_loading)

                    entry.proxyCount != null ->
                        stringResource(R.string.source_proxy_count, entry.proxyCount)

                    else ->
                        stringResource(R.string.source_proxy_count_unknown)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.source_created_at, CreatedAtFormatter.format(entry.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_source),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
