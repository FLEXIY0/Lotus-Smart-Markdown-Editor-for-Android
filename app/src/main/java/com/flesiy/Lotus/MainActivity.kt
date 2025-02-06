package com.flesiy.Lotus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flesiy.Lotus.ui.components.NoteEditor
import com.flesiy.Lotus.ui.components.NotesList
import com.flesiy.Lotus.ui.components.TrashScreen
import com.flesiy.Lotus.ui.theme.LotusTheme
import com.flesiy.Lotus.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LotusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LotusApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotusApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notes by viewModel.notes.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val trashNotes by viewModel.trashNotes.collectAsState()
    val trashSize by viewModel.trashSize.collectAsState()
    val isTrashOverLimit by viewModel.isTrashOverLimit.collectAsState()
    val currentRetentionPeriod by viewModel.currentRetentionPeriod.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    NotesList(
                        notes = notes,
                        onNoteClick = { noteId ->
                            viewModel.loadNote(noteId)
                            scope.launch {
                                drawerState.close()
                            }
                            navController.navigate("editor")
                        },
                        onNoteDelete = { noteId ->
                            viewModel.deleteNote(noteId)
                        },
                        skipDeleteConfirmation = viewModel.skipDeleteConfirmation.collectAsState().value,
                        onSkipDeleteConfirmationChange = { skip ->
                            viewModel.setSkipDeleteConfirmation(skip)
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Кнопка корзины
                    ListItem(
                        headlineContent = { Text("Корзина") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Корзина",
                                tint = if (isTrashOverLimit) MaterialTheme.colorScheme.error 
                                      else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = if (trashSize > 0) {
                            {
                                Text(
                                    text = String.format("%.1f MB", trashSize / (1024.0 * 1024.0)),
                                    color = if (isTrashOverLimit) MaterialTheme.colorScheme.error 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else null,
                        modifier = Modifier.clickable {
                            scope.launch {
                                drawerState.close()
                            }
                            navController.navigate("trash")
                        }
                    )
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "editor",
            modifier = Modifier
        ) {
            composable(
                route = "editor"
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(currentNote.title.ifEmpty { "Без названия" }) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    viewModel.createNewNote()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                                }
                            }
                        )
                    }
                ) { padding ->
                    NoteEditor(
                        note = currentNote,
                        onContentChange = { content ->
                            viewModel.updateNoteContent(content)
                            viewModel.saveNote()
                        },
                        onSave = {
                            viewModel.saveNote()
                        },
                        onStartRecording = {
                            // TODO: Implement speech recognition
                        },
                        onPreviewModeChange = { isPreview ->
                            viewModel.updatePreviewMode(isPreview)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            
            composable(
                route = "trash"
            ) {
                TrashScreen(
                    notes = trashNotes,
                    currentRetentionPeriod = currentRetentionPeriod,
                    trashSize = trashSize,
                    isOverLimit = isTrashOverLimit,
                    onRetentionPeriodChange = { period ->
                        viewModel.setRetentionPeriod(period)
                    },
                    onRestoreNote = { noteId ->
                        viewModel.restoreNote(noteId)
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}