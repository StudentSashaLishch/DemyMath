package com.example.demymath.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val topicId: String,
    val titleKey: String,
    val iconName: String,
    val x: Float = 0f,
    val y: Float = 0f
)