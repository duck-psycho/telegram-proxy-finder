package com.duckpsycho.telegramproxyfinder.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    linkColor: Color = MaterialTheme.colorScheme.primary,
) {
    AndroidView(
        modifier = modifier,
        factory = ::createHtmlTextScrollView,
        update = { scrollView ->
            scrollView.textView.apply {
                setTextColor(textColor.toArgb())
                setLinkTextColor(linkColor.toArgb())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
                text = html.fromHtml()
            }
        },
    )
}

private val ScrollView.textView: TextView
    get() = getChildAt(0) as TextView

private fun createHtmlTextScrollView(context: Context): ScrollView = ScrollView(context).apply {
    isFillViewport = true
    addView(
        TextView(context).apply {
            enableSelectableLinks()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        },
    )
}

private fun String.fromHtml(): Spannable = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT) as Spannable

@SuppressLint("ClickableViewAccessibility")
private fun TextView.enableSelectableLinks() {
    linksClickable = false
    setTextIsSelectable(true)

    var pressedLink: ClickableSpan? = null

    setOnTouchListener { view, event ->
        val textView = view as TextView
        val spannable = textView.text as? Spannable ?: return@setOnTouchListener false
        val link = textView.linkAt(event, spannable)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedLink = link
                if (link != null) {
                    textView.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP -> {
                val openLink = link != null && link == pressedLink
                if (openLink) link.onClick(textView)
                pressedLink = null
                openLink
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedLink = null
                false
            }

            else -> false
        }
    }
}

private fun TextView.linkAt(event: MotionEvent, spannable: Spannable): ClickableSpan? {
    val x = event.x.toInt() - totalPaddingLeft + scrollX
    val y = event.y.toInt() - totalPaddingTop + scrollY
    val textLayout = layout ?: return null
    val offset = textLayout.getOffsetForHorizontal(
        textLayout.getLineForVertical(y),
        x.toFloat(),
    )
    return spannable.getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()
}
