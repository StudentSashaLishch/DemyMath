package com.example.demymath.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.util.copy
import com.example.demymath.ContentItem
import com.example.demymath.Topic
import com.example.demymath.TopicWithContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.room.withTransaction
import com.example.demymath.FirebaseSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AppRepository(private val db: AppDatabase) {
    private val dao = db.appDao()
    private val syncManager by lazy { FirebaseSyncManager(db) }

    suspend fun getGraphData(userId: Int): List<Topic> = withContext(Dispatchers.IO) {
        val currentLang = Locale.getDefault().language
        val entities = dao.getAllTopics()
        val relations = dao.getAllRelations()
        val progressList = dao.getUserProgress(userId)

        entities.map { entity ->
            val title = dao.getString(entity.titleKey, currentLang) ?: entity.titleKey
            Topic(
                id = entity.topicId,
                title = title,
                icon = mapIconNameToVector(entity.iconName),
                status = progressList.find { it.topicId == entity.topicId }?.status ?: 0,
                prerequisites = relations.filter { it.child == entity.topicId }.map { it.parent },
                x = entity.x,
                y = entity.y
            )
        }
    }

    suspend fun getFullTopicData(topicId: String): TopicWithContent = withContext(Dispatchers.IO) {
        val currentLang = Locale.getDefault().language

        // Спочатку дістаємо саму сутність теми, щоб дізнатися її titleKey
        val topicEntity = dao.getTopicById(topicId)
        val titleKey = topicEntity?.titleKey ?: "t_${topicId}_title"

        // Тепер шукаємо переклад за цим ключем
        val translatedTitle = dao.getString(titleKey, currentLang)
            ?: dao.getString(titleKey, "uk")
            ?: "Тема $topicId"

        val texts = dao.getTextContent(topicId).map { entity ->
            ContentItem.Text(
                entity.orderNumber,
                dao.getString(entity.textKey, currentLang) ?: "Текст відсутній"
            )
        }

        val videos = dao.getVideoContent(topicId).map { entity ->
            ContentItem.Video(
                entity.orderNumber,
                entity.href,
                dao.getString(entity.titleKey, currentLang) ?: "Відео"
            )
        }

        TopicWithContent(translatedTitle, (texts + videos).sortedBy { it.order })
    }

    suspend fun updateUserProgress(userId: Int, topicId: String, newStatus: Int) = withContext(Dispatchers.IO) {
        dao.upsertProgress(
            UserProgress(
                userId = userId,
                topicId = topicId,
                status = newStatus,
                lastReviewDate = null,
                nextReviewDate = null
            )
        )
    }

    private fun mapIconNameToVector(name: String): ImageVector = when (name) {
        "functions" -> Icons.Default.Functions
        "history" -> Icons.Default.ChangeHistory
        "add" -> Icons.Default.Add
        else -> Icons.Default.Help
    }

    suspend fun saveReflectionMark(userId: Int, topicId: String, score: Int) = withContext(Dispatchers.IO) {
        dao.insertReflectionMark(ReflectionMarkEntity(userId = userId, topicId = topicId, confidenceScore = score))
    }

    suspend fun getLastConfidenceScore(userId: Int, topicId: String): Int? = withContext(Dispatchers.IO) {
        dao.getLastMarkForTopic(topicId, userId)?.confidenceScore
    }

    suspend fun getTestData(topicId: Int): List<AppDao.QuestionWithAnswers> = withContext(Dispatchers.IO) {
        val currentLang = Locale.getDefault().language
        val data = dao.getQuestionsWithAnswers(topicId)

        data.map { wrap ->
            val translatedQuestion = wrap.question.copy(
                textKey = dao.getString(wrap.question.textKey, currentLang) ?: wrap.question.textKey
            )
            val translatedAnswers = wrap.answers.map { ans ->
                ans.copy(textKey = dao.getString(ans.textKey, currentLang) ?: ans.textKey)
            }
            AppDao.QuestionWithAnswers(translatedQuestion, translatedAnswers)
        }
    }

    suspend fun completeTest(userId: Int, topicId: String, score: Int) = withContext(Dispatchers.IO) {
        dao.upsertTestProgress(TestProgressEntity(userId, topicId.toInt(), score))
        if (score >= 60) {
            updateUserProgress(userId, topicId, 2)
        }
    }

    suspend fun saveNote(userId: Int, topicId: String, text: String) = withContext(Dispatchers.IO) {
        dao.insertReflectionNote(ReflectionNoteEntity(userId = userId, topicId = topicId, text = text))
    }

    suspend fun getTopicById(topicId: String): TopicEntity? = withContext(Dispatchers.IO) {
        dao.getTopicById(topicId)
    }

    suspend fun getString(key: String, lang: String): String? = withContext(Dispatchers.IO) {
        dao.getString(key, lang)
    }

    fun getFinishedTopicsWithNames(userId: Int, lang: String) = dao.getFinishedTopicsWithNames(userId, lang)

    fun getAllLatestMarks() = dao.getAllLatestMarks()
    fun getMarksForTopic(topicId: String, userId: Int) = dao.getMarksForTopic(topicId, userId)
    fun getNotesForTopic(topicId: String, userId: Int) = dao.getNotesForTopic(topicId, userId)

    suspend fun deleteNote(note: ReflectionNoteEntity) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }

    fun getTotalTopicsCount() = dao.getTotalTopicsCount()

    fun getFinishedTopicsCount(userId: Int) = dao.getFinishedTopicsCount(userId)

    suspend fun resetAllProgress(userId: Int) = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.deleteAllReflectionMarks(userId)
            dao.deleteAllTestProgress(userId)
            dao.resetAllUserProgress(userId)
            dao.deleteAllNotes(userId)
        }
    }

    suspend fun getUserById(userId: Int): Flow<User?> = dao.getUserById(userId)

    suspend fun checkAndRefreshRepetitions(userId: Int) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        dao.markTopicsForRepetition(userId, currentTime)
    }

    suspend fun scheduleNextReview(userId: Int, topicId: String, testScore: Int, confidence: Int) = withContext(Dispatchers.IO) {
        val daysToAdd = (testScore / 20) + confidence
        val lastReview = System.currentTimeMillis()
        val nextReview = lastReview + (daysToAdd * 24 * 60 * 60 * 1000L)

        dao.updateReviewDates(userId, topicId, lastReview, nextReview, status = 2)
    }

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        dao.getAllUsers()
    }

    suspend fun createNewUser(user: User): Long = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) {
        dao.deleteUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        dao.updateUser(user)
    }

    suspend fun linkAccountWithFirebase(userId: Int, email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = try {
                auth.createUserWithEmailAndPassword(email, pass).await()
            } catch (e: Exception) {
                auth.signInWithEmailAndPassword(email, pass).await()
            }

            val uid = authResult.user?.uid
            val localUser = dao.getUserByIdDirect(userId)

            if (localUser != null) {
                dao.updateUser(localUser.copy(email = email, password = pass, firebaseUid = uid))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithCloud(userId: Int, upload: Boolean): Result<Unit> {
        return if (upload) {
            syncManager.uploadDataToCloud(userId)
        } else {
            syncManager.downloadDataFromCloud(userId)
        }
    }

    suspend fun migrateGuestProgressToUser(newUserId: Int) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val guestId = 1

            val guestProgress = dao.getUserProgressDirect(guestId)
            guestProgress.forEach { progress ->
                dao.upsertProgress(progress.copy(userId = newUserId))
            }

            val guestMarks = dao.getAllMarksForUserDirect(guestId)
            guestMarks.forEach { mark ->
                dao.insertReflectionMark(mark.copy(userId = newUserId, markId = 0))
            }

            val guestNotes = dao.getAllNotesForUserDirect(guestId)
            guestNotes.forEach { note ->
                dao.insertReflectionNote(note.copy(userId = newUserId, noteId = 0))
            }

            dao.deleteAllReflectionMarks(guestId)
            dao.deleteAllTestProgress(guestId)
            dao.resetAllUserProgress(guestId)
            dao.deleteAllNotes(guestId)
        }
    }

    suspend fun verifyUserPassword(userId: Int, passwordEntered: String): Boolean = withContext(Dispatchers.IO) {
        val user = dao.getUserByIdDirect(userId)
        user?.password == null || user.password == passwordEntered
    }
}