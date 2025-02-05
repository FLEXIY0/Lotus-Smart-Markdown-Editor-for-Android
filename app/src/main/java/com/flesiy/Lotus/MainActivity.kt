package com.flesiy.Lotus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
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
import com.flesiy.Lotus.ui.theme.LotusTheme
import com.flesiy.Lotus.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.composable
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
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
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lotus") },
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
                            navController.navigate("editor")
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "editor",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(
                    route = "notes",
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    }
                ) {
                    // Здесь может быть приветственный экран или список заметок
                }
                composable(
                    route = "editor",
                    enterTransition = {
                        fadeIn(
                            animationSpec = tween(700, easing = LinearOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        ) + slideIn(
                            initialOffset = { IntOffset(0, -300) },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        )
                    },
                    exitTransition = {
                        fadeOut(
                            animationSpec = tween(500, easing = LinearOutSlowInEasing)
                        ) + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + slideOut(
                            targetOffset = { IntOffset(0, -300) },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                ) {
                    NoteEditor(
                        note = currentNote,
                        onContentChange = { content ->
                            viewModel.updateNoteContent(content)
                        },
                        onSave = {
                            viewModel.saveNote()
                        },
                        onStartRecording = {
                            // TODO: Implement speech recognition
                        }
                    )
                }
            }
        }
    }
}