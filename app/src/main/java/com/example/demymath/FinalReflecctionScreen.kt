package com.example.demymath

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.demymath.data.AppRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Composable
fun FinalReflectionScreen(
    topicId: String,
    score: Int,
    userId: Int,
    repository: AppRepository,
    navController: NavController
) {
    var noteText by remember { mutableStateOf("") }
    var selectedConfidence by remember { mutableStateOf(3) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (score >= 60) "Вітаємо! Тест пройдено" else "Спробуйте ще раз",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Ваш результат: $score%",
            color = if (score >= 60) Color(0xFF4CAF50) else Color.Red,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Як ви оцінюєте своє розуміння тепер?", style = MaterialTheme.typography.bodyLarge)

        // Передаємо userId в EmojiDropdown, щоб він бачив оцінки цього юзера
        EmojiDropdown(
            topicId = topicId,
            userId = userId,
            repository = repository,
            onSelect = { selectedConfidence = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Ваші нотатки") },
            placeholder = { Text("Що було складним? Що варто повторити?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    // Використовуємо переданий userId замість '1'
                    if (noteText.isNotBlank()) {
                        repository.saveNote(userId, topicId, noteText)
                    }
                    repository.scheduleNextReview(userId, topicId, score, selectedConfidence)

                    navController.navigate("graph") {
                        popUpTo("graph") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Зберегти та повернутись до карти")
        }
    }
}