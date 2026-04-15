package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class AnswerEntity(
    @PrimaryKey val answerId: Int,
    val questionId: Int,
    val textKey: String,
    val isCorrect: Boolean
)
