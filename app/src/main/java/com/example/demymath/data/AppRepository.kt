package com.example.demymath.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.demymath.ContentItem
import com.example.demymath.Topic
import com.example.demymath.TopicWithContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AppRepository(private val db: AppDatabase) {
    private val dao = db.appDao()

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

        // Проходимо по кожному питанню і відповіді, замінюючи ключ на переклад
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
            // Якщо здано, міняємо статус теми на 2 (Пройдено)
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

    fun getFinishedTopicsWithNames(lang: String) = dao.getFinishedTopicsWithNames(lang)

    // Онови ці методи (переконайся, що вони використовують нові запити з DAO)
    fun getAllLatestMarks() = dao.getAllLatestMarks()
    fun getMarksForTopic(topicId: String) = dao.getMarksForTopic(topicId)
    fun getNotesForTopic(topicId: String) = dao.getNotesForTopic(topicId)

    suspend fun deleteNote(note: ReflectionNoteEntity) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }
}