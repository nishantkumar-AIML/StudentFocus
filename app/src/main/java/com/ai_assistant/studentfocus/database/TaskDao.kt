package com.ai_assistant.studentfocus.database

import androidx.room.*
import com.ai_assistant.studentfocus.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // Study History queries
    @Query("SELECT * FROM study_history ORDER BY date DESC")
    fun getAllStudyHistory(): Flow<List<StudyHistoryEntity>>

    @Query("SELECT * FROM study_history WHERE date = :date LIMIT 1")
    suspend fun getStudyHistoryDirect(date: String): StudyHistoryEntity?

    @Query("SELECT * FROM study_history")
    suspend fun getAllStudyHistoryDirect(): List<StudyHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyHistory(history: StudyHistoryEntity)

    // Daily Notes queries
    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC")
    fun getNotesForDate(date: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    fun getNoteForDate(date: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getNotesForDateDirect(date: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE date = :date ORDER BY createdAt DESC LIMIT 1")
    suspend fun getNoteByDateDirect(date: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: String): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY date DESC, createdAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    // Exam Countdowns queries
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    // App Settings queries
    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<AppSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingEntity)

    @Delete
    suspend fun deleteSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<AppSettingEntity>>

    // 1. Attendance Tracker
    @Query("SELECT * FROM attendance ORDER BY subjectName ASC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    // 2. CGPA Courses
    @Query("SELECT * FROM cgpa_courses ORDER BY name ASC")
    fun getAllCourses(): Flow<List<CgpaCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CgpaCourseEntity)

    @Delete
    suspend fun deleteCourse(course: CgpaCourseEntity)

    // 3. Decks & Flashcards
    @Query("SELECT * FROM flashcard_decks ORDER BY name ASC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY front ASC")
    fun getFlashcardsForDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY front ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: String)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteFlashcardsByDeckId(deckId: String)

    // 4. Habits Grid
    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    // 5. Ledger Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // 6. Time Routine Activities
    @Query("SELECT * FROM routine_activities ORDER BY createdAt ASC")
    fun getAllRoutineActivities(): Flow<List<RoutineActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineActivity(activity: RoutineActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineActivities(activities: List<RoutineActivityEntity>)

    @Delete
    suspend fun deleteRoutineActivity(activity: RoutineActivityEntity)

    // Chat History
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_history WHERE id = :id")
    suspend fun deleteChatMessageById(id: Long)

    @Query("DELETE FROM chat_history WHERE text = :text AND isUser = :isUser")
    suspend fun deleteChatMessageByContent(text: String, isUser: Boolean)

    @Query("DELETE FROM chat_history")
    suspend fun clearChatHistory()

    // Lists for JSON backup export/restore
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Query("SELECT * FROM notes ORDER BY date DESC, createdAt DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("SELECT * FROM exams")
    suspend fun getAllExamsList(): List<ExamEntity>

    @Query("SELECT * FROM study_history")
    suspend fun getAllStudyHistoryList(): List<StudyHistoryEntity>

    @Query("SELECT * FROM settings")
    suspend fun getAllSettingsList(): List<AppSettingEntity>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceList(): List<AttendanceEntity>

    @Query("SELECT * FROM cgpa_courses")
    suspend fun getAllCoursesList(): List<CgpaCourseEntity>

    @Query("SELECT * FROM flashcard_decks")
    suspend fun getAllDecksList(): List<DeckEntity>

    @Query("SELECT * FROM flashcards")
    suspend fun getAllFlashcardsList(): List<FlashcardEntity>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsList(): List<HabitEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Query("SELECT * FROM chat_history")
    suspend fun getAllChatHistoryList(): List<ChatMessageEntity>

    @Query("SELECT * FROM routine_activities")
    suspend fun getAllRoutineActivitiesList(): List<RoutineActivityEntity>

    // 7. App Usage & Watch History
    @Query("SELECT * FROM app_usage_history WHERE date = :date ORDER BY totalTimeMillis DESC")
    fun getAppUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_history ORDER BY date DESC, totalTimeMillis DESC")
    fun getAllAppUsage(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_history ORDER BY date DESC, totalTimeMillis DESC")
    suspend fun getAllAppUsageList(): List<AppUsageEntity>

    @Query("SELECT * FROM app_usage_history WHERE date = :date ORDER BY totalTimeMillis DESC")
    suspend fun getAppUsageByDateList(date: String): List<AppUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageList(list: List<AppUsageEntity>)

    @Query("DELETE FROM app_usage_history WHERE date = :date")
    suspend fun deleteAppUsageByDate(date: String)

    // 8. Advanced In-App Activity Sessions & Intelligence
    @Query("SELECT * FROM app_activity_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getActivitySessionsByDate(date: String): Flow<List<AppActivitySessionEntity>>

    @Query("SELECT * FROM app_activity_sessions WHERE date = :date ORDER BY startTime DESC")
    suspend fun getActivitySessionsByDateDirect(date: String): List<AppActivitySessionEntity>

    @Query("SELECT * FROM app_activity_sessions WHERE startTime >= :startMillis AND endTime <= :endMillis ORDER BY startTime DESC")
    fun getActivitySessionsBetween(startMillis: Long, endMillis: Long): Flow<List<AppActivitySessionEntity>>

    @Query("SELECT * FROM app_activity_sessions ORDER BY startTime DESC")
    fun getAllActivitySessionsFlow(): Flow<List<AppActivitySessionEntity>>

    @Query("SELECT * FROM app_activity_sessions ORDER BY startTime DESC")
    suspend fun getAllActivitySessionsList(): List<AppActivitySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivitySession(session: AppActivitySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivitySessions(sessions: List<AppActivitySessionEntity>)

    @Query("UPDATE app_activity_sessions SET category = :newCategory, isUserEdited = 1 WHERE packageName = :packageName AND contentTitle = :contentTitle")
    suspend fun updateActivityCategory(packageName: String, contentTitle: String, newCategory: String)

    @Query("UPDATE app_activity_sessions SET category = :newCategory, isUserEdited = 1 WHERE packageName = :packageName")
    suspend fun updateAppCategory(packageName: String, newCategory: String)

    @Query("DELETE FROM app_activity_sessions WHERE id = :id")
    suspend fun deleteActivitySessionById(id: String)

    @Query("DELETE FROM app_activity_sessions WHERE date = :date")
    suspend fun deleteActivitySessionsForDate(date: String)

    // 9. App & Content Category Overrides
    @Query("SELECT * FROM app_category_overrides")
    fun getAllCategoryOverrides(): Flow<List<AppCategoryOverrideEntity>>

    @Query("SELECT * FROM app_category_overrides")
    suspend fun getAllCategoryOverridesList(): List<AppCategoryOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryOverride(override: AppCategoryOverrideEntity)

    @Query("DELETE FROM app_category_overrides WHERE targetKey = :targetKey")
    suspend fun deleteCategoryOverride(targetKey: String)

    // 10. User Feedback & Learning History
    @Query("SELECT * FROM user_feedback_history ORDER BY timestamp DESC")
    fun getAllUserFeedback(): Flow<List<UserFeedbackEntity>>

    @Query("SELECT * FROM user_feedback_history ORDER BY timestamp DESC")
    suspend fun getAllUserFeedbackList(): List<UserFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFeedback(feedback: UserFeedbackEntity)

    @Query("DELETE FROM user_feedback_history")
    suspend fun clearAllUserFeedback()

    // 11. Personal Behavior Profile
    @Query("SELECT * FROM personal_behavior_profile WHERE id = 'PRIMARY_USER_PROFILE' LIMIT 1")
    fun getPersonalBehaviorProfile(): Flow<PersonalBehaviorProfileEntity?>

    @Query("SELECT * FROM personal_behavior_profile WHERE id = 'PRIMARY_USER_PROFILE' LIMIT 1")
    suspend fun getPersonalBehaviorProfileDirect(): PersonalBehaviorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalBehaviorProfile(profile: PersonalBehaviorProfileEntity)

    @Query("DELETE FROM personal_behavior_profile")
    suspend fun deletePersonalBehaviorProfile()

    // 12. Time Table Schedule Slots
    @Query("SELECT * FROM timetable_slots ORDER BY startTime ASC")
    fun getAllTimeTableSlots(): Flow<List<TimeTableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :day OR dayOfWeek = 'Daily' ORDER BY startTime ASC")
    fun getTimeTableSlotsForDay(day: String): Flow<List<TimeTableSlotEntity>>

    @Query("SELECT * FROM timetable_slots ORDER BY startTime ASC")
    suspend fun getAllTimeTableSlotsDirect(): List<TimeTableSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeTableSlot(slot: TimeTableSlotEntity)

    @Delete
    suspend fun deleteTimeTableSlot(slot: TimeTableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteTimeTableSlotById(id: String)
}
