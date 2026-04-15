package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = TestEntity::class,
            parentColumns = ["testId"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class QuestionEntity(
    @PrimaryKey val questionId: Int,
    val testId: Int,
    val textKey: String
)
