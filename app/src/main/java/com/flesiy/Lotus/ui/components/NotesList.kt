package com.flesiy.Lotus.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.flesiy.Lotus.viewmodel.Note
import org.burnoutcrew.reorderable.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SwipeAction {
    PIN, DELETE
}

private fun Modifier.draggedItem(isDragging: Boolean): Modifier = this.then(
    Modifier.graphicsLayer {
        if (isDragging) {
            scaleX = 1.05f
            scaleY = 1.05f
            shadowElevation = 8f
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onNoteDelete: (Long) -> Unit,
    onNotePinned: (Long) -> Unit,
    onNoteMove: (Int, Int) -> Unit,
    skipDeleteConfirmation: Boolean,
    onSkipDeleteConfirmationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedNotes = notes.filter { it.isPinned }
    val unpinnedNotes = notes.filter { !it.isPinned }

    val pinnedState = rememberReorderableLazyListState(onMove = { from, to ->
        // Перемещение только внутри закрепленных заметок
        val realFromIndex = from.index - 1 // Учитываем заголовок
        val realToIndex = to.index - 1
        if (realFromIndex >= 0 && realToIndex >= 0 && 
            realFromIndex < pinnedNotes.size && realToIndex < pinnedNotes.size) {
            onNoteMove(realFromIndex, realToIndex)
        }
    })

    val unpinnedState = rememberReorderableLazyListState(onMove = { from, to ->
        // Перемещение только внутри обычных заметок
        val startIndex = pinnedNotes.size
        val realFromIndex = from.index + startIndex
        val realToIndex = to.index + startIndex
        if (realFromIndex >= startIndex && realToIndex >= startIndex) {
            onNoteMove(realFromIndex, realToIndex)
        }
    })

    LazyColumn(
        modifier = modifier
    ) {
        // Секция закрепленных заметок
        if (pinnedNotes.isNotEmpty()) {
            item(key = "pinned_header") {
                Text(
                    text = "Закрепленные",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(
                items = pinnedNotes,
                key = { it.id }
            ) { note ->
                Box(
                    modifier = Modifier
                        .reorderable(pinnedState)
                        .detectReorderAfterLongPress(pinnedState)
                ) {
                    NoteItem(
                        note = note,
                        onNoteClick = onNoteClick,
                        onNoteDelete = onNoteDelete,
                        onNotePinned = onNotePinned,
                        skipDeleteConfirmation = skipDeleteConfirmation,
                        onSkipDeleteConfirmationChange = onSkipDeleteConfirmationChange,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }

            if (unpinnedNotes.isNotEmpty()) {
                item(key = "divider") {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item(key = "unpinned_header") {
                    Text(
                        text = "Остальные",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Секция обычных заметок
        items(
            items = unpinnedNotes,
            key = { it.id }
        ) { note ->
            Box(
                modifier = Modifier
                    .reorderable(unpinnedState)
                    .detectReorderAfterLongPress(unpinnedState)
            ) {
                NoteItem(
                    note = note,
                    onNoteClick = onNoteClick,
                    onNoteDelete = onNoteDelete,
                    onNotePinned = onNotePinned,
                    skipDeleteConfirmation = skipDeleteConfirmation,
                    onSkipDeleteConfirmationChange = onSkipDeleteConfirmationChange,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteItem(
    note: Note,
    onNoteClick: (Long) -> Unit,
    onNoteDelete: (Long) -> Unit,
    onNotePinned: (Long) -> Unit,
    skipDeleteConfirmation: Boolean,
    onSkipDeleteConfirmationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 150f
    val maxSwipe = 200f
    val flyAwayDistance = 2000f
    var isAnimatingSwipe by remember { mutableStateOf(false) }
    var isReturning by remember { mutableStateOf(false) }
    var targetAction: SwipeAction? by remember { mutableStateOf(null) }
    var velocityX by remember { mutableStateOf(0f) }

    val animatedOffset = animateFloatAsState(
        targetValue = when (targetAction) {
            SwipeAction.PIN -> if (isAnimatingSwipe) -flyAwayDistance else offsetX
            SwipeAction.DELETE -> if (isAnimatingSwipe) maxSwipe else offsetX
            null -> offsetX
        },
        animationSpec = when (targetAction) {
            SwipeAction.PIN -> tween(
                durationMillis = if (abs(velocityX) > 3000) 200 else 400,
                easing = FastOutSlowInEasing
            )
            SwipeAction.DELETE -> tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
            null -> tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        },
        finishedListener = {
            if (isAnimatingSwipe) {
                when (targetAction) {
                    SwipeAction.PIN -> {
                        // Сначала завершаем анимацию
                        MainScope().launch {
                            delay(200) // Даем время на завершение анимации
                            onNotePinned(note.id)
                            delay(50) // Небольшая пауза перед сбросом состояния
                            offsetX = 0f
                            isAnimatingSwipe = false
                            targetAction = null
                            velocityX = 0f
                            isReturning = false
                        }
                    }
                    SwipeAction.DELETE -> {
                        // Сначала завершаем анимацию
                        MainScope().launch {
                            delay(200) // Даем время на завершение анимации
                            if (skipDeleteConfirmation) {
                                onNoteDelete(note.id)
                            } else {
                                showDeleteDialog = true
                            }
                            delay(50) // Небольшая пауза перед сбросом состояния
                            offsetX = 0f
                            isAnimatingSwipe = false
                            targetAction = null
                            velocityX = 0f
                            isReturning = false
                        }
                    }
                    null -> {
                        isReturning = false
                    }
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    if (!isAnimatingSwipe && !isReturning) {
                        // Сглаживаем движение при быстром свайпе
                        val smoothDelta = delta * 0.8f
                        offsetX = (offsetX + smoothDelta).coerceIn(-maxSwipe, maxSwipe)
                        velocityX = delta
                    }
                },
                onDragStopped = { velocity ->
                    velocityX = velocity
                    when {
                        // Учитываем скорость свайпа при определении действия
                        offsetX < -swipeThreshold || velocity < -2000 -> {
                            isAnimatingSwipe = true
                            targetAction = SwipeAction.PIN
                        }
                        offsetX > swipeThreshold || velocity > 2000 -> {
                            isAnimatingSwipe = true
                            targetAction = SwipeAction.DELETE
                        }
                        else -> {
                            isReturning = true
                            offsetX = 0f
                            velocityX = 0f
                        }
                    }
                }
            )
    ) {
        // Фон для свайпа
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    when {
                        // Показываем фон только при активном свайпе
                        !isAnimatingSwipe && abs(offsetX) > 0f -> when {
                            offsetX > 0 -> MaterialTheme.colorScheme.error.copy(
                                alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f) * 0.7f
                            )
                            offsetX < 0 -> MaterialTheme.colorScheme.primary.copy(
                                alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f) * 0.7f
                            )
                            else -> Color.Transparent
                        }
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = when {
                offsetX > 0 -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
        ) {
            // Иконка с плавным появлением
            if (abs(offsetX) > 20f && !isAnimatingSwipe) {
                Icon(
                    imageVector = when {
                        offsetX > 0 -> Icons.Default.Delete
                        else -> Icons.Default.PushPin
                    },
                    contentDescription = null,
                    tint = Color.White.copy(
                        alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // Основной контент
        Card(
            modifier = modifier
                .offset { IntOffset(animatedOffset.value.toInt(), 0) }
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .alpha(if (note.isPinned) 1f else 0.9f),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (note.isPinned) 4.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (note.isPinned) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            ),
            onClick = { onNoteClick(note.id) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (note.isPinned) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (note.preview.isNotEmpty()) {
                        Text(
                            text = note.preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (note.isPinned) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = "Создано: ${formatDate(note.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (note.isPinned) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Закреплено",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteNoteDialog(
            noteTitle = note.title,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    onSkipDeleteConfirmationChange(true)
                }
                onNoteDelete(note.id)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 