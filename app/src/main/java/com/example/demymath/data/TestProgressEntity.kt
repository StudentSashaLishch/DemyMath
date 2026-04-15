package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "test_progress",
    primaryKeys = ["userId", "testId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TestEntity::class,
            parentColumns = ["testId"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TestProgressEntity(
    val userId: Int,
    val testId: Int,
    val score: Int
)
