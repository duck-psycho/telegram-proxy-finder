package com.duckpsycho.telegramproxyfinder.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.duckpsycho.telegramproxyfinder.R
import com.duckpsycho.telegramproxyfinder.data.source.SourceType
import com.duckpsycho.telegramproxyfinder.ui.AddSourceUiState
import com.duckpsycho.telegramproxyfinder.ui.AddSourceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(
    uiState: AddSourceUiState,
    onBack: () -> Unit,
    onVerify: (SourceType, String) -> Unit,
    onAdd: suspend (SourceType, String) -> Boolean,
    onInputChanged: () -> Unit,
    hasDuplicate: (SourceType, String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedType by rememberSaveable { mutableStateOf(SourceType.Web) }
    var value by rememberSaveable { mutableStateOf("") }

    val normalizedValue = AddSourceViewModel.normalizeValue(selectedType, value)
    val isDuplicate = hasDuplicate(selectedType, normalizedValue)
    val canAct = normalizedValue.isNotBlank() && !uiState.isVerifying
    val canAdd = uiState.verifiedCount != null && uiState.verifiedCount > 0 && !isDuplicate
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_source),
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
        bottomBar = {
            if (canAdd) {
                Button(
                    onClick = {
                        scope.launch {
                            onAdd(selectedType, normalizedValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.add))
                }
            } else {
                OutlinedButton(
                    onClick = { onVerify(selectedType, normalizedValue) },
                    enabled = canAct,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(stringResource(R.string.verify_source))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.source_type_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == SourceType.Web,
                    onClick = {
                        selectedType = SourceType.Web
                        onInputChanged()
                    },
                    enabled = !uiState.isVerifying,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(stringResource(R.string.source_type_web))
                }
                SegmentedButton(
                    selected = selectedType == SourceType.Telegram,
                    onClick = {
                        selectedType = SourceType.Telegram
                        onInputChanged()
                    },
                    enabled = !uiState.isVerifying,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(stringResource(R.string.source_type_telegram))
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    onInputChanged()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isVerifying,
                label = {
                    Text(
                        if (selectedType == SourceType.Telegram) {
                            stringResource(R.string.source_value_label_telegram)
                        } else {
                            stringResource(R.string.source_value_label_web)
                        },
                    )
                },
                placeholder = {
                    Text(
                        if (selectedType == SourceType.Telegram) {
                            stringResource(R.string.source_value_hint_telegram)
                        } else {
                            stringResource(R.string.source_value_hint_web)
                        },
                    )
                },
                singleLine = selectedType == SourceType.Telegram,
                keyboardOptions =
                if (selectedType == SourceType.Web) {
                    KeyboardOptions(keyboardType = KeyboardType.Uri)
                } else {
                    KeyboardOptions(keyboardType = KeyboardType.Text)
                },
            )

            if (isDuplicate) {
                Text(
                    text = stringResource(R.string.source_duplicate_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.loadFailed) {
                Text(
                    text = stringResource(R.string.source_verify_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            uiState.verifiedCount?.let { count ->
                Text(
                    text =
                    if (count > 0) {
                        stringResource(R.string.status_found_proxies, count)
                    } else {
                        stringResource(R.string.status_no_valid_proxies)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                    if (count > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
