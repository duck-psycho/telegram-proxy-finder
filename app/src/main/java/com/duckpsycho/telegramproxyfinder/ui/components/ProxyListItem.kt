package com.duckpsycho.telegramproxyfinder.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duckpsycho.telegramproxyfinder.R
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy
import com.duckpsycho.telegramproxyfinder.ui.theme.pingIndicatorColor
import com.duckpsycho.telegramproxyfinder.ui.util.toTelegramProxyUri

@Composable
fun ProxyListItem(
    proxy: WorkingMtProtoProxy,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val noTgAppMessage = stringResource(R.string.no_tg_app)
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    val scheme = MaterialTheme.colorScheme
    val pingColor = pingIndicatorColor(proxy.pingMs)
    val proxyUri = proxy.toTelegramProxyUri()
    val copyProxyLink = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("proxy", proxyUri.toString()))
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }
    val openProxy = {
        val intent = Intent(Intent.ACTION_VIEW, proxyUri)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, noTgAppMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = openProxy,
                    )
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(pingColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = proxy.pingMs.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = pingColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.ping_ms_unit),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = "${proxy.server}:${proxy.port}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    proxy.secretDomain?.let { domain ->
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.sni, domain),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.wrapContentWidth(align = Alignment.End),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = copyProxyLink,
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.copy),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                TextButton(
                    onClick = openProxy,
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.connect),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            color = scheme.outline.copy(alpha = 0.35f),
            thickness = 0.5.dp,
        )
    }
}
