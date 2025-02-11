package com.flesiy.Lotus.ui.components.markdown

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.ui.components.CustomMarkdownEditor

@Composable
fun MarkdownEditorContainer(
    content: String,
    onContentChange: (String) -> Unit,
    isPreviewMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPreviewMode) {
        MarkdownPreview(
            content = content,
            onContentChange = onContentChange,
            modifier = modifier
        )
    } else {
        CustomMarkdownEditor(
            value = content,
            onValueChange = onContentChange,
            modifier = modifier
        )
    }
}

@Composable
fun MarkdownEditorContainerWithToggle(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPreviewMode by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Верхняя панель с кнопками
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                PreviewToggleButton(
                    isPreviewMode = isPreviewMode,
                    onToggle = { isPreviewMode = !isPreviewMode },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // Редактор/Превью с анимированным переходом
            MarkdownPreviewScreen(
                content = content,
                onContentChange = onContentChange,
                isPreviewMode = isPreviewMode,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
} 