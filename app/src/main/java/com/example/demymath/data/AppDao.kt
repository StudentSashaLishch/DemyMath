package com.example.demymath.data

import android.R
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("""
    SELECT * FROM reflection_marks 
    WHERE markId IN (SELECT MAX(markId) FROM reflection_marks GROUP BY topicId)
""")
    fun getAllLatestMarks(): Flow<List<ReflectionMarkEntity>>

    @Query("SELECT * FROM reflection_marks WHERE topicId = :topicId ORDER BY timestamp ASC")
    fun getMarksForTopic(topicId: String): Flow<List<ReflectionMarkEntity>>

    @Query("SELECT * FROM reflection_notes WHERE topicId = :topicId ORDER BY timestamp DESC")
    fun getNotesForTopic(topicId: String): Flow<List<ReflectionNoteEntity>>

    @Delete
    suspend fun deleteNote(note: ReflectionNoteEntity)

    @Query("""
    SELECT m.*, l.value as topicName 
    FROM reflection_marks m
    JOIN user_progress u ON m.topicId = u.topicId
    JOIN topics t ON m.topicId = t.topicId
    JOIN localization l ON t.titleKey = l.`key`
    WHERE u.status = 2 AND l.lang = :lang
    AND m.markId IN (SELECT MAX(markId) FROM reflection_marks GROUP BY topicId)
""")
    fun getFinishedTopicsWithNames(lang: String): Flow<List<TopicWithMark>>

    // Створимо допоміжний клас для результату
    data class TopicWithMark(
        @Embedded val mark: ReflectionMarkEntity,
        val topicName: String
    )

    @Query("DELETE FROM reflection_marks WHERE userId = :userId")
    suspend fun deleteAllReflectionMarks(userId: Int)

    @Query("DELETE FROM test_progress WHERE userId = :userId")
    suspend fun deleteAllTestProgress(userId: Int)

    @Query("UPDATE user_progress SET status = 0 WHERE userId = :userId")
    suspend fun resetAllUserProgress(userId: Int)

    @Query("DELETE FROM reflection_notes WHERE userId = :userId")
    suspend fun deleteAllNotes(userId: Int)

    @Query("SELECT COUNT(*) FROM topics")
    fun getTotalTopicsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND status = 2")
    fun getFinishedTopicsCount(userId: Int): Flow<Int>

    @Query("SELECT * FROM users WHERE userId = :id")
    fun getUserById(id: Int): Flow<User?>

    @Query("""
    UPDATE user_progress 
    SET status = 3 
    WHERE userId = :userId AND nextReviewDate <= :currentTime AND status = 2
    """)
    suspend fun markTopicsForRepetition(userId: Int, currentTime: Long)

    @Query("""
    UPDATE user_progress 
    SET lastReviewDate = :last, nextReviewDate = :next, status = :status 
    WHERE userId = :userId AND topicId = :topicId
    """)
    suspend fun updateReviewDates(userId: Int, topicId: String, last: Long, next: Long, status: Int)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Delete
    suspend fun deleteUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}