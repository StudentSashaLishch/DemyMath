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

    @Insert
    suspend fun insertReflectionMark(mark: ReflectionMarkEntity)

    @Query("SELECT * FROM reflection_marks WHERE topicId = :topicId AND userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMarkForTopic(topicId: String, userId: Int): ReflectionMarkEntity?

    @Transaction
    @Query("SELECT * FROM questions WHERE testId = :testId")
    suspend fun getQuestionsWithAnswers(testId: Int): List<QuestionWithAnswers>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTestProgress(progress: TestProgressEntity)

    @Insert
    suspend fun insertReflectionNote(note: ReflectionNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: List<TestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: List<AnswerEntity>)

    // Допоміжна модель для зв'язку
    data class QuestionWithAnswers(
        @Embedded val question: QuestionEntity,
        @Relation(parentColumn = "questionId", entityColumn = "questionId")
        val answers: List<AnswerEntity>
    )
}