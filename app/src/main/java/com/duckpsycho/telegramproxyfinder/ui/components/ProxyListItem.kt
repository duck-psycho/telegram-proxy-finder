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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val compactLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )
    val hostTextStyle = MaterialTheme.typography.titleMedium.copy(
        lineHeight = 16.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = compactLineHeight,
    )
    val actionTextStyle = MaterialTheme.typography.labelSmall.copy(
        lineHeight = 11.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = compactLineHeight,
    )
    val sniTextStyle = MaterialTheme.typography.bodyMedium.copy(
        lineHeight = 14.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = compactLineHeight,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        val itemInteractionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(
                    interactionSource = itemInteractionSource,
                    indication = ripple(),
                    onClick = openProxy,
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
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
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(start = 0.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = if (proxy.secretDomain != null) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.Top
                },
            ) {
                Text(
                    text = "${proxy.server}:${proxy.port}",
                    modifier = Modifier.fillMaxWidth(),
                    style = hostTextStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                proxy.secretDomain?.let { domain ->
                    Text(
                        text = stringResource(R.string.sni, domain),
                        modifier = Modifier.fillMaxWidth(),
                        style = sniTextStyle,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(R.string.connect),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = actionTextStyle,
                    color = scheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.copy),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 16.dp),
                            onClick = copyProxyLink,
                        )
                        .padding(horizontal = 4.dp),
                    style = actionTextStyle,
                    color = scheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            color = scheme.outline.copy(alpha = 0.35f),
            thickness = 0.5.dp,
        )
    }
}
