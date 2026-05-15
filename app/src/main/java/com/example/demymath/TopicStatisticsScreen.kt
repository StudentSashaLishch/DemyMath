package com.example.demymath

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.demymath.data.AppRepository
import com.example.demymath.data.ReflectionNoteEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicStatisticsScreen(
    topicId: String,
    userId: Int, // Додано параметр
    repository: AppRepository,
    navController: NavController
) {
    // Тепер підписки залежать і від topicId, і від userId
    val marks by repository.getMarksForTopic(topicId, userId).collectAsState(initial = emptyList())
    val notes by repository.getNotesForTopic(topicId, userId).collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    var topicName by remember { mutableStateOf("...") }
    val lang = remember { Locale.getDefault().language }

    LaunchedEffect(topicId) {
        val topic = repository.getTopicById(topicId)
        topic?.let {
            topicName = repository.getString(it.titleKey, lang) ?: it.topicId
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(topicName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Графік впевненості", style = MaterialTheme.typography.titleMedium)

            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (marks.size > 1) {
                        val spacing = size.width / (marks.size - 1)
                        val points = marks.mapIndexed { i, m ->
                            Offset(i * spacing, size.height - (m.confidenceScore / 5f * size.height))
                        }
                        for (i in 0 until points.size - 1) {
                            drawLine(Color.Blue, points[i], points[i+1], strokeWidth = 5f)
                        }
                        points.forEach { drawCircle(Color.Red, radius = 10f, center = it) }
                    } else {
                        drawCircle(Color.Gray, radius = 10f, center = center, style = Stroke(width = 2f))
                    }
                }
            }

            Text("Нотатки (свайп вліво — видалити)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(notes, key = { it.noteId }) { note ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                showDeleteDialog = true
                                false
                            } else false
                        }
                    )

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Видалити нотатку?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.deleteNote(note)
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Нотатку видалено",
                                            actionLabel = "Скасувати",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            repository.saveNote(userId, note.topicId, note.text)
                                        }
                                    }
                                    showDeleteDialog = false
                                }) { Text("Видалити", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") }
                            }
                        )
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { Box(modifier = Modifier.fillMaxSize()) }
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            ListItem(
                                headlineContent = { Text(note.text) },
                                supportingContent = { Text(dateFormat.format(Date(note.timestamp))) }
                            )
                        }
                    }
                }
            }
        }
    }
}