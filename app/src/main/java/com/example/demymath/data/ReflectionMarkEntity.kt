package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reflection_marks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE)
    ]
)
data class ReflectionMarkEntity(
    @PrimaryKey(autoGenerate = true) val markId: Int = 0,
    val userId: Int,
    val topicId: String,
    val confidenceScore: Int, // 1-5
    val timestamp: Long = System.currentTimeMillis()
)