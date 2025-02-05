package com.flesiy.Lotus.ui.components.markdown

import android.content.Context
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin

/**
 * A custom Markwon TextView for displaying Markdown text.
 *
 * @param markdown markdown text
 * @param modifier modifiers for Jetpack Compose view
 * @param color text color
 * @param fontSize text size
 * @param textAlign text alignment
 * @param maxLines maximum number of lines to display
 * @param fontResource font resource to use
 * @param style text style to use
 * @param viewId view id to use
 * @param textTruncated callback to indicate if text is truncated
 */
@Composable
@NonRestartableComposable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    fontResource: Int? = null,
    style: TextStyle = TextStyle.Default,
    viewId: Int? = null,
    textTruncated: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var truncated by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createTextView(
                context = ctx,
                maxLines = maxLines,
                textAlign = textAlign,
                color = color,
                fontSize = fontSize,
                fontResource = fontResource,
                style = style,
                viewId = viewId
            )
        },
        update = { textView ->
            textView.text = markdown
            if (truncated != textView.layout?.getEllipsisCount(textView.lineCount - 1)!! > 0) {
                truncated = !truncated
                textTruncated(truncated)
            }
        }
    )
}

private fun createTextView(
    context: Context,
    maxLines: Int,
    textAlign: TextAlign?,
    color: Color,
    fontSize: TextUnit,
    fontResource: Int?,
    style: TextStyle,
    viewId: Int?
): TextView {
    return TextView(context).apply {
        this.maxLines = maxLines
        textAlign?.let { align ->
            textAlignment = when (align) {
                TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
                TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
                TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }
        }

        if (color != Color.Unspecified) {
            setTextColor(color.toArgb())
        }

        if (fontSize != TextUnit.Unspecified) {
            textSize = fontSize.value
        }

        if (style != TextStyle.Default) {
            if (style.fontSize != TextUnit.Unspecified) {
                textSize = style.fontSize.value
            }
            if (style.color != Color.Unspecified) {
                setTextColor(style.color.toArgb())
            }
        }

        fontResource?.let { font ->
            typeface = ResourcesCompat.getFont(context, font)
        }

        viewId?.let { id ->
            this.id = id
        }

        movementMethod = LinkMovementMethod.getInstance()

        val imageLoader = ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .logger(DebugLogger())
            .build()

        val markwon = Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context, imageLoader))
            .build()

        markwon.setMarkdown(this, "")
    }
} 