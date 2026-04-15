package com.example.demymath

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.demymath.data.AppRepository
import com.example.demymath.ui.TopicRenderer
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun KnowledgeGraphScreen(repository: AppRepository, navController: NavController) {
    val state = remember { GraphState() }
    val scope = rememberCoroutineScope() // ВИПРАВЛЕНО: Додано scope
    var topics by remember { mutableStateOf(emptyList<Topic>()) }

    // Стани для діалогу
    var showWarningDialog by remember { mutableStateOf<List<String>?>(null) }
    var pendingTopicId by remember { mutableStateOf<String?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        topics = repository.getGraphData(userId = 1)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = ""
    )

    val iconPainters = topics.associate { it.id to rememberVectorPainter(it.icon) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Обробка зуму та панорамування
                detectTransformGestures { _, pan, zoom, _ ->
                    state.scale *= zoom
                    state.offset += pan
                }
            }
            .pointerInput(topics) {
                // ОБРОБКА КЛІКІВ
                detectTapGestures { offset ->
                    val canvasOffset = (offset - state.offset) / state.scale
                    val clickedTopic = topics.find {
                        sqrt((it.x - canvasOffset.x).pow(2) + (it.y - canvasOffset.y).pow(2)) <= 50f
                    }

                    clickedTopic?.let { topic ->
                        val unfinished = topics.filter { topic.prerequisites.contains(it.id) && it.status < 2 }
                        if (topic.status == 0 && unfinished.isNotEmpty()) {
                            showWarningDialog = unfinished.map { it.title }
                            pendingTopicId = topic.id
                        } else {
                            scope.launch {
                                if (topic.status == 0) repository.updateUserProgress(1, topic.id, 1)
                                navController.navigate("learning/${topic.id}")
                            }
                        }
                    }
                }
            }
    ) {
        translate(left = state.offset.x, top = state.offset.y) {
            // Масштабування теж робимо через блок
            scale(scale = state.scale, pivot = Offset.Zero) {

                val baseRadius = 50f

                // 1. Малюємо ребра
                topics.forEach { topic ->
                    topic.prerequisites.forEach { preId ->
                        val startNode = topics.find { it.id == preId }
                        if (startNode != null) {
                            drawArrow(
                                start = Offset(startNode.x, startNode.y),
                                end = Offset(topic.x, topic.y),
                                color = Color.Gray.copy(alpha = 0.7f),
                                strokeWidth = 3f,
                                nodeRadius = baseRadius
                            )
                        }
                    }
                }

                // 2. Малюємо вузли
                topics.forEach { topic ->
                    val center = Offset(topic.x, topic.y)
                    iconPainters[topic.id]?.let { painter ->
                        TopicRenderer.drawNode(
                            drawScope = this,
                            topic = topic,
                            iconPainter = painter,
                            center = center,
                            radius = if (topic.status == 3) 50f * pulseScale else 50f
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 32f // Розмір тексту
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            // Можна додати шрифт, якщо хочеться
                        }
                        drawText(
                            topic.title,
                            center.x,
                            center.y + baseRadius + 40f, // Відступ вниз від вузла
                            paint
                        )
                    }
                }
            }
        }
    }

    // Діалог попередження
    showWarningDialog?.let { unfinishedTitles ->
        AlertDialog(
            onDismissRequest = { showWarningDialog = null },
            title = { Text("Увага") },
            text = { Text("Ця тема базується на: ${unfinishedTitles.joinToString()}. Продовжити?") },
            confirmButton = {
                Button(onClick = {
                    val id = pendingTopicId ?: return@Button
                    scope.launch {
                        repository.updateUserProgress(1, id, 1)
                        navController.navigate("learning/$id")
                        showWarningDialog = null
                    }
                }) { Text("Продовжити") }
            },
            dismissButton = { TextButton(onClick = { showWarningDialog = null }) { Text("Скасувати") } }
        )
    }
}

fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, strokeWidth: Float, nodeRadius: Float) {
    val angle = atan2(end.y - start.y, end.x - start.x)

    val adjustedEnd = Offset(
        x = end.x - nodeRadius * cos(angle),
        y = end.y - nodeRadius * sin(angle)
    )

    drawLine(
        color = color,
        start = start,
        end = adjustedEnd,
        strokeWidth = strokeWidth
    )

    val arrowSize = 20f
    val arrowAngle = Math.toRadians(30.0)

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(adjustedEnd.x, adjustedEnd.y)
        lineTo(
            adjustedEnd.x - arrowSize * cos(angle - arrowAngle).toFloat(),
            adjustedEnd.y - arrowSize * sin(angle - arrowAngle).toFloat()
        )
        moveTo(adjustedEnd.x, adjustedEnd.y)
        lineTo(
            adjustedEnd.x - arrowSize * cos(angle + arrowAngle).toFloat(),
            adjustedEnd.y - arrowSize * sin(angle + arrowAngle).toFloat()
        )
    }

    drawPath(path, color = color, style = Stroke(width = strokeWidth))
}