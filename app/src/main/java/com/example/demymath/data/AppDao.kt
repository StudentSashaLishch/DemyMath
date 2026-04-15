package com.example.demymath.data

import android.R
import androidx.room.*

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUser(): User?

    // Для графа нам знадобляться теми та зв'язки
    @Query("SELECT * FROM topics")
    suspend fun getAllTopics(): List<TopicEntity>

    @Query("SELECT * FROM prerequisite_relations")
    suspend fun getAllRelations(): List<PrerequisiteRelation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<PrerequisiteRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: List<UserProgress>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalization(localization: List<LocalizationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextContent(text: List<TextContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoContent(video: List<VideoContentEntity>)

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    suspend fun getUserProgress(userId: Int): List<UserProgress>

    @Query("""
    SELECT value FROM localization 
    WHERE `key` = :key AND lang = :lang 
    LIMIT 1
    """)
    suspend fun getString(key: String, lang: String): String?

    @Query("SELECT * FROM text_content WHERE topicId = :topicId ORDER BY orderNumber ASC")
    suspend fun getTextContent(topicId: String): List<TextContentEntity>

    @Query("SELECT * FROM video_content WHERE topicId = :topicId ORDER BY orderNumber ASC")
    suspend fun getVideoContent(topicId: String): List<VideoContentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: UserProgress)

    @Query("SELECT * FROM topics WHERE topicId = :id LIMIT 1")
    suspend fun getTopicById(id: String): TopicEntity?
}