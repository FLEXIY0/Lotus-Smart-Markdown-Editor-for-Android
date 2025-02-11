package com.flesiy.Lotus.ui.components.markdown

import android.widget.EditText
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flesiy.Lotus.ui.components.CustomMarkdownEditor

@Composable
fun AnimatedMarkdownContent(
    content: String,
    onContentChange: (String) -> Unit,
    isPreviewMode: Boolean,
    modifier: Modifier = Modifier,
    hint: String = "",
    onEditorCreated: ((EditText) -> Unit)? = null
) {
    Crossfade(
        targetState = isPreviewMode,
        animationSpec = tween(durationMillis = 300),
        modifier = modifier
    ) { inPreviewMode ->
        if (inPreviewMode) {
            MarkdownPreview(
                content = content,
                onContentChange = onContentChange,
                modifier = modifier
            )
        } else {
            CustomMarkdownEditor(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth(),
                hint = hint,
                onEditorCreated = { editor -> 
                    onEditorCreated?.invoke(editor)
                }
            )
        }
    }
} 