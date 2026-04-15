package com.example.demymath.data
import androidx.room.Entity

@Entity(
    tableName = "localization",
    primaryKeys = ["key", "lang"]
)
data class LocalizationEntity(
    val key: String,
    val lang: String, // "uk", "en", ...
    val value: String
)