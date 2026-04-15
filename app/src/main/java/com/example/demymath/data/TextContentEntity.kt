package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "text_content",
    foreignKeys = [ForeignKey(
        entity = TopicEntity::class,
        parentColumns = ["topicId"],
        childColumns = ["topicId"]
    )]
)
data class TextContentEntity(
    @PrimaryKey(autoGenerate = true) val contentId: Int = 0,
    val topicId: String,
    val orderNumber: Int,
    val textKey: String // FK -> localization.key
)