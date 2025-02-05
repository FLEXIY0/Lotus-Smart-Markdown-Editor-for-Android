package com.flesiy.Lotus.ui.components.markdown

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun PreviewToggleButton(
    isPreviewMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconAlpha by animateFloatAsState(
        targetValue = if (isPreviewMode) 1f else 0.6f,
        label = "iconAlpha"
    )
    
    val tint by animateColorAsState(
        targetValue = if (isPreviewMode) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "tint"
    )

    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isPreviewMode) "Выключить предпросмотр" else "Включить предпросмотр",
            modifier = Modifier
                .size(24.dp)
                .alpha(iconAlpha),
            tint = tint
        )
    }
} 