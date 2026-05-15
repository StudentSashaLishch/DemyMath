package com.example.demymath.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val email: String? = null,
    val displayName: String = "Гість",
    val password: String? = null,
    val avatarUrі: String? = null,
    val totalSolved: Int = 0,
    val firebaseUid: String? = null
)