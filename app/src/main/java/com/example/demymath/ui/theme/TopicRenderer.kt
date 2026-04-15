package com.example.demymath.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate // Важливо для виправлення помилки 2
import androidx.compose.ui.graphics.painter.Painter
import com.example.demymath.Topic

object TopicRenderer {

    fun drawNode(
        drawScope: DrawScope,
        topic: Topic,
        iconPainter: Painter,
        center: Offset,
        radius: Float
    ) {
        // Логіка кольору
        val (baseColor, alpha) = when (topic.status) {
            1 -> Color(0xFF2196F3) to 1f  // Блакитний
            2 -> Color(0xFF4CAF50) to 1f  // Зелений
            3 -> Color(0xFF4CAF50) to 0.6f // Напівпрозорий зелений
            else -> Color.Gray to 1f      // Статус 0
        }

        // Виправляємо помилку 1: явно використовуємо drawScope
        drawScope.apply {
            val strokeStyle = if (topic.status == 3) {
                Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            } else {
                Stroke(width = 4f)
            }

            // Малюємо коло (тіло вузла)
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = radius,
                center = center
            )

            // Малюємо обводку
            drawCircle(
                color = if (topic.status == 3) baseColor else Color.White,
                radius = radius,
                center = center,
                style = strokeStyle
            )

            // Виправляємо помилку 2: правильний виклик translate
            val iconSize = radius * 1.2f
            translate(
                left = center.x - iconSize / 2,
                top = center.y - iconSize / 2
            ) {
                with(iconPainter) {
                    draw(size = Size(iconSize, iconSize))
                }
            }
        }
    }
}