package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_content",
    foreignKeys = [ForeignKey(
        entity = TopicEntity::class,
        parentColumns = ["topicId"],
        childColumns = ["topicId"]
    )]
)
data class VideoContentEntity(
    @PrimaryKey(autoGenerate = true) val videoId: Int = 0,
    val topicId: String,
    val orderNumber: Int,
    val href: String,
    val titleKey: String // FK -> localization.key
)
