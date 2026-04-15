package com.example.demymath

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.demymath.data.AppRepository
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(userId: Int, repository: AppRepository) {
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    // Стан для імені користувача
    var userName by remember { mutableStateOf("Завантаження...") }

    // Отримуємо статистику з Flow
    val totalTopics by repository.getTotalTopicsCount().collectAsState(initial = 0)
    val finishedTopics by repository.getFinishedTopicsCount(userId).collectAsState(initial = 0)

    // Завантажуємо реальне ім'я з бази
    LaunchedEffect(userId) {
        val user = repository.getUserById(userId)
        userName = user?.displayName ?: "Користувач #$userId"
    }

    val profileImageUrl: String? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Мій Профіль", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        // Аватар
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Реальне ім'я з бази
        Text(
            text = userName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Картка прогресу
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Прогрес навчання", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$finishedTopics / $totalTopics",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("тем завершено", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                val progressValue = if (totalTopics > 0) finishedTopics.toFloat() / totalTopics else 0f
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Кнопка скидання
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Скинути весь прогрес")
        }

        // Діалог підтвердження (код залишається без змін)
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Скинути прогрес?") },
                text = { Text("Це видалить усі ваші оцінки, нотатки та результати тестів. Ви готові почати спочатку?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.resetAllProgress(userId)
                            showResetDialog = false
                        }
                    }) { Text("Так, видалити", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Скасувати") }
                }
            )
        }
    }
}