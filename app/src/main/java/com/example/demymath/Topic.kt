package com.example.demymath

import androidx.compose.ui.graphics.vector.ImageVector

data class Topic(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val status: Int = 0,
    val prerequisites: List<String> = emptyList(),
    var x: Float = 0f,
    var y: Float = 0f
)

sealed class ContentItem(val order: Int) {
    data class Text(val orderNum: Int, val text: String) : ContentItem(orderNum)
    data class Video(val orderNum: Int, val url: String, val title: String) : ContentItem(orderNum)
}

data class TopicWithContent(
    val title: String,
    val items: List<ContentItem>
)