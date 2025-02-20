package com.flesiy.Lotus.ui.components.markdown

import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flesiy.Lotus.R

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

    val context = LocalContext.current
    val view = LocalView.current

    IconButton(
        onClick = {
            onToggle()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        },
        modifier = modifier
    ) {
        Icon(
            painter = if (isPreviewMode) 
                painterResource(id = R.drawable.visibility_off_24) 
            else 
                painterResource(id = R.drawable.visibility_24),
            contentDescription = if (isPreviewMode) "Выключить предпросмотр" else "Включить предпросмотр",
            modifier = Modifier
                .size(24.dp)
                .alpha(iconAlpha),
            tint = tint
        )
    }
} 