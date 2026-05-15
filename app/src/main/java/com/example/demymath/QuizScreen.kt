package com.example.demymath

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.demymath.data.AppRepository
import com.example.demymath.data.AppDao // Переконайтеся, що імпорт є
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    topicId: String,
    userId: Int,
    repository: AppRepository,
    navController: NavController
) {
    // Вказуємо повний шлях до моделі, якщо вона всередині Dao
    var questions by remember { mutableStateOf(emptyList<AppDao.QuestionWithAnswers>()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerId by remember { mutableStateOf<Int?>(null) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        questions = repository.getTestData(topicId.toInt())
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Тестування: Тема $topicId") })
        }
    ) { padding ->
        if (questions.isNotEmpty()) {
            val currentQuestionWrap = questions[currentQuestionIndex]
            val question = currentQuestionWrap.question
            val answers = currentQuestionWrap.answers

            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(
                    text = "Питання ${currentQuestionIndex + 1}/${questions.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Відображаємо ключ (в ідеалі тут має бути переклад, як в LearningScreen)
                Text(text = question.textKey, style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(24.dp))

                answers.forEach { answer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAnswerId == answer.answerId,
                            onClick = { selectedAnswerId = answer.answerId }
                        )
                        Text(text = answer.textKey, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val isCorrect = answers.find { it.answerId == selectedAnswerId }?.isCorrect ?: false
                        if (isCorrect) correctAnswersCount++

                        if (currentQuestionIndex < questions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswerId = null
                        } else {
                            val finalScore = (correctAnswersCount.toFloat() / questions.size * 100).toInt()
                            scope.launch {
                                repository.completeTest(userId, topicId, finalScore)
                                navController.navigate("reflection/$topicId/$finalScore")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedAnswerId != null
                ) {
                    Text(if (currentQuestionIndex < questions.size - 1) "Далі" else "Завершити")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}