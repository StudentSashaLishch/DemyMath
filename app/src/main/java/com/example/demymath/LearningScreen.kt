package com.example.demymath

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.demymath.data.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(topicId: String, repository: AppRepository, navController: NavController) {
    var topicData by remember { mutableStateOf<TopicWithContent?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        topicData = repository.getFullTopicData(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topicData?.title ?: "Завантаження...",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    EmojiDropdown(topicId, repository, {})
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Область контенту, яка скролиться
            if (topicData == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(topicData!!.items) { item ->
                        when (item) {
                            is ContentItem.Text -> {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 24.sp
                                )
                            }
                            is ContentItem.Video -> {
                                VideoBlock(item)
                            }
                        }
                    }
                }
            }

            // Статична кнопка внизу
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        navController.navigate("quiz/$topicId")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Перейти до вправ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmojiDropdown(
    topicId: String,
    repository: AppRepository,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val emojiMap = mapOf(1 to "🤯", 2 to "😥", 3 to "🤨", 4 to "😃", 5 to "😎")

    var selectedScore by remember { mutableStateOf(5) }
    var pendingScore by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        repository.getLastConfidenceScore(1, topicId)?.let {
            selectedScore = it
            onSelect(it)
        }
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Text(emojiMap[selectedScore] ?: "😎", fontSize = 24.sp)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            emojiMap.forEach { (score, emoji) ->
                DropdownMenuItem(
                    text = { Text("$emoji", fontSize = 24.sp) },
                    onClick = {
                        pendingScore = score
                        showConfirmDialog = true
                        expanded = false
                    }
                )
            }
        }
    }

    if (showConfirmDialog && pendingScore != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Підтвердження") },
            text = { Text("Ви впевнені, що ваша оцінка ${emojiMap[pendingScore]}?") },
            confirmButton = {
                Button(onClick = {
                    val score = pendingScore!!
                    scope.launch {
                        repository.saveReflectionMark(1, topicId, score)
                        selectedScore = score
                        onSelect(score)
                        Toast.makeText(context, "Оцінку збережено: ${emojiMap[score]}", Toast.LENGTH_SHORT).show()
                        showConfirmDialog = false
                    }
                }) { Text("Так") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Ні") }
            }
        )
    }
}

@Composable
fun VideoBlock(video: ContentItem.Video) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Відеоматеріал доступний за посиланням:",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.url))
                    context.startActivity(intent)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = video.url, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}