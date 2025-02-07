package com.flesiy.Lotus.ui.components.markdown

import android.content.Context
import android.widget.TextView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flesiy.Lotus.ui.components.CustomMarkdownEditor
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun MarkdownPreviewScreen(
    content: String,
    isPreviewMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Crossfade(
            targetState = isPreviewMode,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.fillMaxSize()
        ) { inPreviewMode ->
            if (inPreviewMode) {
                MarkdownPreview(
                    content = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                CustomMarkdownEditor(
                    value = content,
                    onValueChange = { /* будет добавлено позже */ },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember { createMarkwon(context) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                createPreviewTextView(ctx).apply {
                    markwon.setMarkdown(this, content)
                }
            },
            update = { textView ->
                markwon.setMarkdown(textView, content)
            }
        )
    }
}

private fun createPreviewTextView(context: Context): TextView {
    return TextView(context).apply {
        textSize = 16f
        setPadding(16, 16, 16, 16)
        setTextIsSelectable(true)
        movementMethod = android.text.method.LinkMovementMethod.getInstance()
        gravity = android.view.Gravity.TOP
    }
}

private fun createMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .build()
} 