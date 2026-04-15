package com.example.demymath.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prerequisite_relations",
    foreignKeys = [
        ForeignKey(entity = TopicEntity::class, parentColumns = ["topicId"], childColumns = ["parent"]),
        ForeignKey(entity = TopicEntity::class, parentColumns = ["topicId"], childColumns = ["child"])
    ],
    indices = [Index("parent"), Index("child")]
)
data class PrerequisiteRelation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parent: String,
    val child: String
)