package com.example.demymath.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class,
        TopicEntity::class,
        PrerequisiteRelation::class,
        UserProgress::class,
        LocalizationEntity::class,
        TextContentEntity::class,
        VideoContentEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "demymath_db"
                )
                    .fallbackToDestructiveMigration(false) //Це тілки для розробки
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.appDao()

                                    // 1. Створюємо гостя
                                    val userId = dao.insertUser(User(displayName = "Гість")).toInt()

                                    val localization = listOf(
                                        LocalizationEntity("t_arith_title", "uk", "Арифметика"),
                                        LocalizationEntity("t_arith_title", "en", "Arithmetic"),
                                        LocalizationEntity("t_1_text_1", "uk", "Арифметика — це фундамент математики, що вивчає числа та базові операції."),
                                        LocalizationEntity("t_1_text_2", "uk", "До основних операцій належать додавання, віднімання, множення та ділення."),
                                        LocalizationEntity("t_1_vid_title", "uk", "Вступ до арифметики"),

                                        LocalizationEntity("t_alg_title", "uk", "Алгебра"),
                                        LocalizationEntity("t_alg_title", "en", "Algebra"),
                                        LocalizationEntity("t_2_text_1", "uk", "Алгебра використовує символи та букви для представлення чисел у формулах."),
                                        LocalizationEntity("t_2_text_2", "uk", "Розв'язання рівнянь дозволяє знайти невідомі величини в математичних виразах."),
                                        LocalizationEntity("t_2_vid_title", "uk", "Основи рівнянь"),

                                        LocalizationEntity("t_geo_title", "uk", "Геометрія"),
                                        LocalizationEntity("t_3_text_1", "uk", "Геометрія досліджує форми, розміри та властивості просторових фігур."),
                                        LocalizationEntity("t_3_text_2", "uk", "Точки, лінії та площини є базовими поняттями в цій науці."),

                                        LocalizationEntity("t_trig_title", "uk", "Тригонометрія"),
                                        LocalizationEntity("t_4_text_1", "uk", "Тригонометрія вивчає залежності між сторонами та кутами трикутників."),
                                        LocalizationEntity("t_4_text_2", "uk", "Функції синус, косинус та тангенс є ключовими в тригонометричних обчисленнях."),
                                        LocalizationEntity("t_4_vid_title", "uk", "Одиничне коло")
                                    )
                                    dao.insertLocalization(localization)

                                    // 2. теми
                                    val topic1 = TopicEntity("1", "t_arith_title", "add", 600f, 1000f)
                                    val topic2 = TopicEntity("2", "t_alg_title", "functions", 800f, 600f)
                                    val topic3 = TopicEntity("3", "t_geo_title", "history", 400f, 600f)
                                    val topic4 = TopicEntity("4", "t_trig_title", "functions", 700f, 200f)

                                    dao.insertTopics(listOf(topic1, topic2, topic3, topic4))

                                    // 3. зв'язки
                                    // Арифметика -> Алгебра
                                    // Арифметика -> Геометрія
                                    // Алгебра -> Тригонометрія
                                    dao.insertRelations(listOf(
                                        PrerequisiteRelation(parent = "1", child = "2"),
                                        PrerequisiteRelation(parent = "1", child = "3"),
                                        PrerequisiteRelation(parent = "2", child = "4")
                                    ))

                                    // 4. прогрес для перевірки рендерингу (статуси 0-3)
                                    dao.insertProgress(listOf(
                                        UserProgress(userId = userId, topicId = "1", status = 0, lastReviewDate = null, nextReviewDate = null),
                                        UserProgress(userId = userId, topicId = "2", status = 0, lastReviewDate = null, nextReviewDate = null),
                                        UserProgress(userId = userId, topicId = "3", status = 0, lastReviewDate = null, nextReviewDate = null),
                                        UserProgress(userId = userId, topicId = "4", status = 0, lastReviewDate = null, nextReviewDate = null)
                                    ))

                                    val textItems = listOf(
                                        TextContentEntity(topicId = "1", orderNumber = 1, textKey = "t_1_text_1"),
                                        TextContentEntity(topicId = "1", orderNumber = 2, textKey = "t_1_text_2"),

                                        TextContentEntity(topicId = "2", orderNumber = 1, textKey = "t_2_text_1"),
                                        TextContentEntity(topicId = "2", orderNumber = 2, textKey = "t_2_text_2"),

                                        TextContentEntity(topicId = "3", orderNumber = 1, textKey = "t_3_text_1"),
                                        TextContentEntity(topicId = "3", orderNumber = 2, textKey = "t_3_text_2"),

                                        TextContentEntity(topicId = "4", orderNumber = 1, textKey = "t_4_text_1"),
                                        TextContentEntity(topicId = "4", orderNumber = 2, textKey = "t_4_text_2")
                                    )
                                    dao.insertTextContent(textItems)

                                    val videoItems = listOf(
                                        VideoContentEntity(topicId = "1", orderNumber = 3, href = "https://www.youtube.com/watch?v=dQw4w9WgXcQ", titleKey = "t_1_vid_title"),
                                        VideoContentEntity(topicId = "2", orderNumber = 3, href = "https://www.youtube.com/watch?v=fC9pS_pY88M", titleKey = "t_2_vid_title"),
                                        VideoContentEntity(topicId = "4", orderNumber = 3, href = "https://www.youtube.com/watch?v=0_u6eXfRNoM", titleKey = "t_4_vid_title")
                                    )
                                    dao.insertVideoContent(videoItems)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}