package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_progress",
    primaryKeys = ["userId", "topicId"], // Складений ключ
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
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserProgress(
    val userId: Int,
    val topicId: String,
    val status: Int,
    val lastReviewDate: Long? = null,
    val nextReviewDate: Long? = null
)