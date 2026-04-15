package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tests",
)
data class TestEntity(
    @PrimaryKey val testId: Int,
    val titleKey: String
)