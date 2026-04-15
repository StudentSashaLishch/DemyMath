package com.example.demymath

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.demymath.data.AppRepository
import java.util.Locale

@Composable
fun GeneralStatisticsScreen(repository: AppRepository, navController: NavController) {
    val lang = remember { Locale.getDefault().language }
    // Використовуємо новий запит, який повертає TopicWithMark
    val topicsWithMarks by repository.getFinishedTopicsWithNames(lang).collectAsState(initial = emptyList())

    val averageConfidence = if (topicsWithMarks.isNotEmpty()) {
        topicsWithMarks.map { it.mark.confidenceScore }.average()
    } else 0.0

    val emojis = listOf("❓", "😞", "😐", "🙂", "😀", "🤩")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Загальний прогрес", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Середній рівень впевненості", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${"%.1f".format(averageConfidence)} / 5.0",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text("Пройдені теми", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        if (topicsWithMarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("У вас поки немає пройдених тем", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn {
                items(topicsWithMarks) { item ->
                    ListItem(
                        headlineContent = { Text(item.topicName) },
                        supportingContent = { Text("Остання самооцінка") },
                        leadingContent = {
                            Text(emojis.getOrElse(item.mark.confidenceScore) { "❓" }, fontSize = 28.sp)
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable {
                            navController.navigate("topic_stats/${item.mark.topicId}")
                        }
                    )
                }
            }
        }
    }
}