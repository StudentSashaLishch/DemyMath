package com.example.demymath

import androidx.room.withTransaction
import com.example.demymath.data.AppDatabase
import com.example.demymath.data.ReflectionMarkEntity
import com.example.demymath.data.ReflectionNoteEntity
import com.example.demymath.data.UserProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncManager(private val db: AppDatabase) {
    private val auth get() = FirebaseAuth.getInstance()
    private val firestore get() = FirebaseFirestore.getInstance()
    private val dao = db.appDao()

    suspend fun uploadDataToCloud(localUserId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val firebaseUid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Користувач не авторизований у Firebase"))

        try {
            val progress = dao.getUserProgressDirect(localUserId)
            val marks = dao.getAllMarksForUserDirect(localUserId)
            val notes = dao.getAllNotesForUserDirect(localUserId)

            val userCloudData = mapOf(
                "progress" to progress.map { mapOf(
                    "topicId" to it.topicId,
                    "status" to it.status,
                    "lastReviewDate" to it.lastReviewDate,
                    "nextReviewDate" to it.nextReviewDate
                ) },
                "marks" to marks.map { mapOf(
                    "topicId" to it.topicId,
                    "confidenceScore" to it.confidenceScore,
                    "timestamp" to it.timestamp
                ) },
                "notes" to notes.map { mapOf(
                    "topicId" to it.topicId,
                    "text" to it.text,
                    "timestamp" to it.timestamp
                ) }
            )

            firestore.collection("users_sync").document(firebaseUid).set(userCloudData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadDataFromCloud(localUserId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val firebaseUid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Користувач не авторизований у Firebase"))

        try {
            val snapshot = firestore.collection("users_sync").document(firebaseUid).get().await()
            if (!snapshot.exists()) return@withContext Result.success(Unit)

            val progressMaps = snapshot.get("progress") as? List<*> ?: emptyList<Any>()
            val marksMaps = snapshot.get("marks") as? List<*> ?: emptyList<Any>()
            val notesMaps = snapshot.get("notes") as? List<*> ?: emptyList<Any>()

            db.withTransaction {
                progressMaps.filterIsInstance<Map<String, Any>>().forEach { map ->
                    dao.upsertProgress(
                        UserProgress(
                            userId = localUserId,
                            topicId = map["topicId"] as? String ?: "",
                            status = (map["status"] as? Long ?: 0L).toInt(),
                            lastReviewDate = map["lastReviewDate"] as? Long,
                            nextReviewDate = map["nextReviewDate"] as? Long
                        )
                    )
                }
                marksMaps.filterIsInstance<Map<String, Any>>().forEach { map ->
                    dao.insertReflectionMark(
                        ReflectionMarkEntity(
                            userId = localUserId,
                            topicId = map["topicId"] as? String ?: "",
                            confidenceScore = (map["confidenceScore"] as? Long ?: 0L).toInt(),
                            timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                    )
                }
                notesMaps.filterIsInstance<Map<String, Any>>().forEach { map ->
                    dao.insertReflectionNote(
                        ReflectionNoteEntity(
                            userId = localUserId,
                            topicId = map["topicId"] as? String ?: "",
                            text = map["text"] as? String ?: "",
                            timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis()
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}