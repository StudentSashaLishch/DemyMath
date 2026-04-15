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
        VideoContentEntity::class,
        ReflectionMarkEntity::class,
        TestEntity::class,
        QuestionEntity::class,
        AnswerEntity::class,
        TestProgressEntity::class,
        ReflectionNoteEntity::class],
    version = 7,
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

                                    val quizLoc = listOf(
                                        // Арифметика (1)
                                        LocalizationEntity("t_1", "uk", "Тест з арифметики"),
                                        LocalizationEntity("q_1_1", "uk", "Скільки буде 7 + 8?"),
                                        LocalizationEntity("a_1_1_1", "uk", "13"),
                                        LocalizationEntity("a_1_1_2", "uk", "15"),
                                        LocalizationEntity("a_1_1_3", "uk", "16"),

                                        LocalizationEntity("q_1_2", "uk", "6 помножити на 9 дорівнює?"),
                                        LocalizationEntity("a_1_2_1", "uk", "54"),
                                        LocalizationEntity("a_1_2_2", "uk", "48"),
                                        LocalizationEntity("a_1_2_3", "uk", "63"),

                                        LocalizationEntity("q_1_3", "uk", "Яке число парне?"),
                                        LocalizationEntity("a_1_3_1", "uk", "11"),
                                        LocalizationEntity("a_1_3_2", "uk", "22"),
                                        LocalizationEntity("a_1_3_3", "uk", "33"),

                                        // Алгебра (2)
                                        LocalizationEntity("t_2", "uk", "Тест з алгебри"),
                                        LocalizationEntity("q_2_1", "uk", "Розв'яжіть: x + 5 = 12"),
                                        LocalizationEntity("a_2_1_1", "uk", "7"),
                                        LocalizationEntity("a_2_1_2", "uk", "17"),
                                        LocalizationEntity("a_2_1_3", "uk", "8"),

                                        LocalizationEntity("q_2_2", "uk", "Чому дорівнює x у виразі 2x = 10?"),
                                        LocalizationEntity("a_2_2_1", "uk", "2"),
                                        LocalizationEntity("a_2_2_2", "uk", "5"),
                                        LocalizationEntity("a_2_2_3", "uk", "20"),

                                        LocalizationEntity("q_2_3", "uk", "Якщо x = 3, чому дорівнює x² + 1?"),
                                        LocalizationEntity("a_2_3_1", "uk", "7"),
                                        LocalizationEntity("a_2_3_2", "uk", "10"),
                                        LocalizationEntity("a_2_3_3", "uk", "9"),

                                        // Геометрія (3)
                                        LocalizationEntity("t_3", "uk", "Тест з геометрії"),
                                        LocalizationEntity("q_3_1", "uk", "Скільки градусів у прямому куті?"),
                                        LocalizationEntity("a_3_1_1", "uk", "90°"),
                                        LocalizationEntity("a_3_1_2", "uk", "180°"),
                                        LocalizationEntity("a_3_1_3", "uk", "45°"),

                                        LocalizationEntity("q_3_2", "uk", "Найдовша сторона прямокутного трикутника?"),
                                        LocalizationEntity("a_3_2_1", "uk", "Катет"),
                                        LocalizationEntity("a_3_2_2", "uk", "Медіана"),
                                        LocalizationEntity("a_3_2_3", "uk", "Гіпотенуза"),

                                        LocalizationEntity("q_3_3", "uk", "Сума кутів трикутника?"),
                                        LocalizationEntity("a_3_3_1", "uk", "360°"),
                                        LocalizationEntity("a_3_3_2", "uk", "180°"),
                                        LocalizationEntity("a_3_3_3", "uk", "100°"),

                                        // Тригонометрія (4)
                                        LocalizationEntity("t_4", "uk", "Тест з тригонометрії"),
                                        LocalizationEntity("q_4_1", "uk", "sin(90°) дорівнює?"),
                                        LocalizationEntity("a_4_1_1", "uk", "1"),
                                        LocalizationEntity("a_4_1_2", "uk", "0"),
                                        LocalizationEntity("a_4_1_3", "uk", "-1"),

                                        LocalizationEntity("q_4_2", "uk", "Відношення протилежного катета до гіпотенузи?"),
                                        LocalizationEntity("a_4_2_1", "uk", "Косинус"),
                                        LocalizationEntity("a_4_2_2", "uk", "Синус"),
                                        LocalizationEntity("a_4_2_3", "uk", "Тангенс"),

                                        LocalizationEntity("q_4_3", "uk", "sin²α + cos²α = ?"),
                                        LocalizationEntity("a_4_3_1", "uk", "0"),
                                        LocalizationEntity("a_4_3_2", "uk", "1"),
                                        LocalizationEntity("a_4_3_3", "uk", "2")
                                    )

                                    val testsList = listOf(
                                        TestEntity(1, "t_1"),
                                        TestEntity(2, "t_2"),
                                        TestEntity(3, "t_3"),
                                        TestEntity(4, "t_4")
                                    )

                                    val questionsList = listOf(
                                        QuestionEntity(101, 1, "q_1_1"),
                                        QuestionEntity(102, 1, "q_1_2"),
                                        QuestionEntity(103, 1, "q_1_3"),

                                        QuestionEntity(201, 2, "q_2_1"),
                                        QuestionEntity(202, 2, "q_2_2"),
                                        QuestionEntity(203, 2, "q_2_3"),

                                        QuestionEntity(301, 3, "q_3_1"),
                                        QuestionEntity(302, 3, "q_3_2"),
                                        QuestionEntity(303, 3, "q_3_3"),

                                        QuestionEntity(401, 4, "q_4_1"),
                                        QuestionEntity(402, 4, "q_4_2"),
                                        QuestionEntity(403, 4, "q_4_3")
                                    )

                                    val answersList = listOf(
                                        // Арифметика
                                        AnswerEntity(1, 101, "a_1_1_1", false),
                                        AnswerEntity(2, 101, "a_1_1_2", true),
                                        AnswerEntity(3, 101, "a_1_1_3", false),

                                        AnswerEntity(4, 102, "a_1_2_1", true),
                                        AnswerEntity(5, 102, "a_1_2_2", false),
                                        AnswerEntity(6, 102, "a_1_2_3", false),

                                        AnswerEntity(7, 103, "a_1_3_1", false),
                                        AnswerEntity(8, 103, "a_1_3_2", true),
                                        AnswerEntity(9, 103, "a_1_3_3", false),
                                        // Алгебра
                                        AnswerEntity(10, 201, "a_2_1_1", true),
                                        AnswerEntity(11, 201, "a_2_1_2", false),
                                        AnswerEntity(12, 201, "a_2_1_3", false),

                                        AnswerEntity(13, 202, "a_2_2_1", false),
                                        AnswerEntity(14, 202, "a_2_2_2", true),
                                        AnswerEntity(15, 202, "a_2_2_3", false),

                                        AnswerEntity(16, 203, "a_2_3_1", false),
                                        AnswerEntity(17, 203, "a_2_3_2", true),
                                        AnswerEntity(18, 203, "a_2_3_3", false),
                                        // Геометрія
                                        AnswerEntity(19, 301, "a_3_1_1", true),
                                        AnswerEntity(20, 301, "a_3_1_2", false),
                                        AnswerEntity(21, 301, "a_3_1_3", false),

                                        AnswerEntity(22, 302, "a_3_2_1", false),
                                        AnswerEntity(23, 302, "a_3_2_2", false),
                                        AnswerEntity(24, 302, "a_3_2_3", true),

                                        AnswerEntity(25, 303, "a_3_3_1", false),
                                        AnswerEntity(26, 303, "a_3_3_2", true),
                                        AnswerEntity(27, 303, "a_3_3_3", false),
                                        // Тригонометрія
                                        AnswerEntity(28, 401, "a_4_1_1", true),
                                        AnswerEntity(29, 401, "a_4_1_2", false),
                                        AnswerEntity(30, 401, "a_4_1_3", false),

                                        AnswerEntity(31, 402, "a_4_2_1", false),
                                        AnswerEntity(32, 402, "a_4_2_2", true),
                                        AnswerEntity(33, 402, "a_4_2_3", false),

                                        AnswerEntity(34, 403, "a_4_3_1", false),
                                        AnswerEntity(35, 403, "a_4_3_2", true),
                                        AnswerEntity(36, 403, "a_4_3_3", false)
                                    )

                                    dao.insertLocalization(quizLoc)
                                    dao.insertTest(testsList)
                                    dao.insertQuestion(questionsList)
                                    dao.insertAnswer(answersList)
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