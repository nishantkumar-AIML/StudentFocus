package com.ai_assistant.studentfocus.database

import com.ai_assistant.studentfocus.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val studyHistory: Flow<List<StudyHistoryEntity>> = taskDao.getAllStudyHistory()
    val allExams: Flow<List<ExamEntity>> = taskDao.getAllExams()
    val allSettings: Flow<List<AppSettingEntity>> = taskDao.getAllSettings()
    val allChatMessages: Flow<List<ChatMessageEntity>> = taskDao.getAllChatMessages()
    val allNotes: Flow<List<NoteEntity>> = taskDao.getAllNotesFlow()

    // 1. Attendance
    val allAttendance: Flow<List<AttendanceEntity>> = taskDao.getAllAttendance()
    suspend fun addAttendance(a: AttendanceEntity) = taskDao.insertAttendance(a)
    suspend fun deleteAttendance(a: AttendanceEntity) = taskDao.deleteAttendance(a)

    // 2. CGPA Courses
    val allCourses: Flow<List<CgpaCourseEntity>> = taskDao.getAllCourses()
    suspend fun addCourse(c: CgpaCourseEntity) = taskDao.insertCourse(c)
    suspend fun deleteCourse(c: CgpaCourseEntity) = taskDao.deleteCourse(c)

    // 3. Decks & Flashcards
    val allDecks: Flow<List<DeckEntity>> = taskDao.getAllDecks()
    val allFlashcards: Flow<List<FlashcardEntity>> = taskDao.getAllFlashcards()
    suspend fun addDeck(d: DeckEntity) = taskDao.insertDeck(d)
    suspend fun deleteDeck(d: DeckEntity) {
        taskDao.deleteFlashcardsByDeckId(d.id)
        taskDao.deleteDeck(d)
    }
    fun getFlashcardsForDeck(deckId: String): Flow<List<FlashcardEntity>> = taskDao.getFlashcardsForDeck(deckId)
    suspend fun addFlashcard(f: FlashcardEntity) = taskDao.insertFlashcard(f)
    suspend fun deleteFlashcard(f: FlashcardEntity) = taskDao.deleteFlashcard(f)
    suspend fun deleteFlashcardById(id: String) = taskDao.deleteFlashcardById(id)

    // 4. Habits
    val allHabits: Flow<List<HabitEntity>> = taskDao.getAllHabits()
    suspend fun addHabit(h: HabitEntity) = taskDao.insertHabit(h)
    suspend fun deleteHabit(h: HabitEntity) = taskDao.deleteHabit(h)

    // 5. Ledger Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = taskDao.getAllExpenses()
    suspend fun addExpense(e: ExpenseEntity) = taskDao.insertExpense(e)
    suspend fun addExpenses(list: List<ExpenseEntity>) = taskDao.insertExpenses(list)
    suspend fun deleteExpense(e: ExpenseEntity) = taskDao.deleteExpense(e)

    // 6. Time Routine Activities
    val allRoutineActivities: Flow<List<RoutineActivityEntity>> = taskDao.getAllRoutineActivities()
    suspend fun addRoutineActivity(a: RoutineActivityEntity) = taskDao.insertRoutineActivity(a)
    suspend fun addRoutineActivities(list: List<RoutineActivityEntity>) = taskDao.insertRoutineActivities(list)
    suspend fun deleteRoutineActivity(a: RoutineActivityEntity) = taskDao.deleteRoutineActivity(a)
    suspend fun getAllRoutineActivitiesList(): List<RoutineActivityEntity> = taskDao.getAllRoutineActivitiesList()

    // 7. App Usage & Watch History
    val allAppUsage: Flow<List<AppUsageEntity>> = taskDao.getAllAppUsage()
    fun getAppUsageByDate(date: String): Flow<List<AppUsageEntity>> = taskDao.getAppUsageByDate(date)
    suspend fun getAppUsageByDateList(date: String): List<AppUsageEntity> = taskDao.getAppUsageByDateList(date)
    suspend fun addAppUsage(usage: AppUsageEntity) = taskDao.insertAppUsage(usage)
    suspend fun addAppUsageList(list: List<AppUsageEntity>) = taskDao.insertAppUsageList(list)
    suspend fun deleteAppUsageByDate(date: String) = taskDao.deleteAppUsageByDate(date)
    suspend fun getAllAppUsageList(): List<AppUsageEntity> = taskDao.getAllAppUsageList()

    // 8. In-App Activity Sessions & Intelligence
    val allActivitySessions: Flow<List<AppActivitySessionEntity>> = taskDao.getAllActivitySessionsFlow()
    fun getActivitySessionsByDate(date: String): Flow<List<AppActivitySessionEntity>> = taskDao.getActivitySessionsByDate(date)
    suspend fun getActivitySessionsByDateDirect(date: String): List<AppActivitySessionEntity> = taskDao.getActivitySessionsByDateDirect(date)
    fun getActivitySessionsBetween(startMillis: Long, endMillis: Long): Flow<List<AppActivitySessionEntity>> = taskDao.getActivitySessionsBetween(startMillis, endMillis)
    suspend fun addActivitySession(session: AppActivitySessionEntity) = taskDao.insertActivitySession(session)
    suspend fun addActivitySessions(sessions: List<AppActivitySessionEntity>) = taskDao.insertActivitySessions(sessions)
    suspend fun getAllActivitySessionsList(): List<AppActivitySessionEntity> = taskDao.getAllActivitySessionsList()
    suspend fun updateActivityCategory(packageName: String, contentTitle: String, newCategory: String) = taskDao.updateActivityCategory(packageName, contentTitle, newCategory)
    suspend fun updateAppCategory(packageName: String, newCategory: String) = taskDao.updateAppCategory(packageName, newCategory)
    suspend fun deleteActivitySessionById(id: String) = taskDao.deleteActivitySessionById(id)
    suspend fun deleteActivitySessionsForDate(date: String) = taskDao.deleteActivitySessionsForDate(date)

    // 9. Category Overrides
    val allCategoryOverrides: Flow<List<AppCategoryOverrideEntity>> = taskDao.getAllCategoryOverrides()
    suspend fun addCategoryOverride(override: AppCategoryOverrideEntity) = taskDao.insertCategoryOverride(override)
    suspend fun deleteCategoryOverride(targetKey: String) = taskDao.deleteCategoryOverride(targetKey)

    // 10. User Feedback & Learning History
    val allUserFeedback: Flow<List<UserFeedbackEntity>> = taskDao.getAllUserFeedback()
    suspend fun getAllUserFeedbackList(): List<UserFeedbackEntity> = taskDao.getAllUserFeedbackList()
    suspend fun addUserFeedback(feedback: UserFeedbackEntity) = taskDao.insertUserFeedback(feedback)
    suspend fun clearAllUserFeedback() = taskDao.clearAllUserFeedback()

    // 11. Personal Behavior Profile
    val personalBehaviorProfile: Flow<PersonalBehaviorProfileEntity?> = taskDao.getPersonalBehaviorProfile()
    suspend fun getPersonalBehaviorProfileDirect(): PersonalBehaviorProfileEntity? = taskDao.getPersonalBehaviorProfileDirect()
    suspend fun savePersonalBehaviorProfile(profile: PersonalBehaviorProfileEntity) = taskDao.insertPersonalBehaviorProfile(profile)
    suspend fun resetPersonalLearningData() {
        taskDao.clearAllUserFeedback()
        taskDao.deletePersonalBehaviorProfile()
    }

    // 12. Time Table Schedule Slots
    val allTimeTableSlots: Flow<List<TimeTableSlotEntity>> = taskDao.getAllTimeTableSlots()
    fun getTimeTableSlotsForDay(day: String): Flow<List<TimeTableSlotEntity>> = taskDao.getTimeTableSlotsForDay(day)
    suspend fun getAllTimeTableSlotsList(): List<TimeTableSlotEntity> = taskDao.getAllTimeTableSlotsDirect()
    suspend fun addTimeTableSlot(slot: TimeTableSlotEntity) = taskDao.insertTimeTableSlot(slot)
    suspend fun deleteTimeTableSlot(slot: TimeTableSlotEntity) = taskDao.deleteTimeTableSlot(slot)
    suspend fun deleteTimeTableSlotById(id: String) = taskDao.deleteTimeTableSlotById(id)

    // Notes, Exams, Settings
    suspend fun addTask(task: TaskEntity) = taskDao.insertTask(task)
    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val nextStatus = if (task.status == "done") "todo" else "done"
        taskDao.insertTask(task.copy(
            isCompleted = nextStatus == "done",
            status = nextStatus
        ))
    }
    suspend fun updateTaskStatus(task: TaskEntity, nextStatus: String) {
        taskDao.insertTask(task.copy(
            status = nextStatus,
            isCompleted = nextStatus == "done"
        ))
    }
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun addStudyHistory(history: StudyHistoryEntity) = taskDao.insertStudyHistory(history)
    fun getNotesForDate(date: String): Flow<List<NoteEntity>> = taskDao.getNotesForDate(date)
    suspend fun getNotesForDateDirect(date: String): List<NoteEntity> = taskDao.getNotesForDateDirect(date)
    fun getNoteForDate(date: String): Flow<NoteEntity?> = taskDao.getNoteForDate(date)
    suspend fun getNoteByDateDirect(date: String): NoteEntity? = taskDao.getNoteByDateDirect(date)
    suspend fun getNoteByIdDirect(id: String): NoteEntity? = taskDao.getNoteByIdDirect(id)
    suspend fun addNote(note: NoteEntity) = taskDao.insertNote(note)
    suspend fun deleteNote(note: NoteEntity) = taskDao.deleteNote(note)
    suspend fun deleteNoteById(id: String) = taskDao.deleteNoteById(id)
    suspend fun addExam(exam: ExamEntity) = taskDao.insertExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = taskDao.deleteExam(exam)
    fun getSetting(key: String): Flow<AppSettingEntity?> = taskDao.getSetting(key)
    suspend fun addSetting(setting: AppSettingEntity) = taskDao.insertSetting(setting)
    suspend fun deleteSetting(setting: AppSettingEntity) = taskDao.deleteSetting(setting)

    suspend fun addChatMessage(message: ChatMessageEntity) = taskDao.insertChatMessage(message)
    suspend fun deleteChatMessage(message: ChatMessageEntity) = taskDao.deleteChatMessage(message)
    suspend fun deleteChatMessageById(id: Long) = taskDao.deleteChatMessageById(id)
    suspend fun deleteChatMessageByContent(text: String, isUser: Boolean) = taskDao.deleteChatMessageByContent(text, isUser)
    suspend fun clearChatHistory() = taskDao.clearChatHistory()

    suspend fun getAllTasksList(): List<TaskEntity> = taskDao.getAllTasksList()
    suspend fun getAllSettingsList(): List<AppSettingEntity> = taskDao.getAllSettingsList()

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Tasks
        val tasksArr = JSONArray()
        taskDao.getAllTasksList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("description", it.description ?: JSONObject.NULL)
            obj.put("category", it.category ?: JSONObject.NULL)
            obj.put("subject", it.subject ?: JSONObject.NULL)
            obj.put("dueDate", it.dueDate)
            obj.put("priority", it.priority)
            obj.put("isPinned", it.isPinned)
            obj.put("isCompleted", it.isCompleted)
            obj.put("isRecurring", it.isRecurring)
            obj.put("createdAt", it.createdAt)
            obj.put("status", it.status)
            obj.put("timeSpentMinutes", it.timeSpentMinutes)
            if (it.linkUrl != null) obj.put("linkUrl", it.linkUrl) else obj.put("linkUrl", JSONObject.NULL)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        // 2. Notes
        val notesArr = JSONArray()
        taskDao.getAllNotesList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("date", it.date)
            obj.put("content", it.content)
            obj.put("topic", it.topic)
            obj.put("createdAt", it.createdAt)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        // 3. Exams
        val examsArr = JSONArray()
        taskDao.getAllExamsList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("date", it.date)
            examsArr.put(obj)
        }
        root.put("exams", examsArr)

        // 4. History
        val histArr = JSONArray()
        taskDao.getAllStudyHistoryList().forEach {
            val obj = JSONObject()
            obj.put("date", it.date)
            obj.put("completedTasks", it.completedTasks)
            obj.put("studyMinutes", it.studyMinutes)
            obj.put("progressPercentage", it.progressPercentage)
            histArr.put(obj)
        }
        root.put("history", histArr)

        // 5. Settings
        val settingsArr = JSONArray()
        taskDao.getAllSettingsList().forEach {
            val obj = JSONObject()
            obj.put("key", it.key)
            obj.put("value", it.value)
            settingsArr.put(obj)
        }
        root.put("settings", settingsArr)

        // 6. Attendance
        val attArr = JSONArray()
        taskDao.getAllAttendanceList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("subjectName", it.subjectName)
            obj.put("presents", it.presents)
            obj.put("absents", it.absents)
            attArr.put(obj)
        }
        root.put("attendance", attArr)

        // 7. CGPA
        val cgpaArr = JSONArray()
        taskDao.getAllCoursesList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("credits", it.credits)
            obj.put("grade", it.grade)
            cgpaArr.put(obj)
        }
        root.put("cgpa", cgpaArr)

        // 8. Decks
        val deckArr = JSONArray()
        taskDao.getAllDecksList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            deckArr.put(obj)
        }
        root.put("decks", deckArr)

        // 9. Flashcards
        val cardArr = JSONArray()
        taskDao.getAllFlashcardsList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("deckId", it.deckId)
            obj.put("front", it.front)
            obj.put("back", it.back)
            obj.put("interval", it.interval)
            obj.put("easeFactor", it.easeFactor)
            obj.put("repetitions", it.repetitions)
            obj.put("nextDueDate", it.nextDueDate)
            cardArr.put(obj)
        }
        root.put("flashcards", cardArr)

        // 10. Habits
        val habitArr = JSONArray()
        taskDao.getAllHabitsList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("completedDates", it.completedDates)
            habitArr.put(obj)
        }
        root.put("habits", habitArr)

        // 11. Expenses
        val expArr = JSONArray()
        taskDao.getAllExpensesList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("description", it.description)
            obj.put("amount", it.amount)
            obj.put("date", it.date)
            obj.put("type", it.type)
            expArr.put(obj)
        }
        root.put("expenses", expArr)

        // 12. Routine Activities
        val routineArr = JSONArray()
        taskDao.getAllRoutineActivitiesList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("activityName", it.activityName)
            obj.put("durationMinutes", it.durationMinutes)
            obj.put("classificationScore", it.classificationScore.toDouble())
            obj.put("category", it.category)
            if (it.notes != null) obj.put("notes", it.notes) else obj.put("notes", JSONObject.NULL)
            obj.put("createdAt", it.createdAt)
            routineArr.put(obj)
        }
        root.put("routine_activities", routineArr)

        // 13. Chat History
        val chatArr = JSONArray()
        taskDao.getAllChatHistoryList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("text", it.text)
            obj.put("isUser", it.isUser)
            obj.put("timestamp", it.timestamp)
            chatArr.put(obj)
        }
        root.put("chat_history", chatArr)

        // 14. App Usage & Screen Time History
        val usageArr = JSONArray()
        taskDao.getAllAppUsageList().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("date", it.date)
            obj.put("packageName", it.packageName)
            obj.put("appName", it.appName)
            obj.put("totalTimeMillis", it.totalTimeMillis)
            obj.put("category", it.category)
            obj.put("lastTimeUsed", it.lastTimeUsed)
            usageArr.put(obj)
        }
        root.put("app_usage_history", usageArr)

        // 15. TimeTable Schedule Slots
        val ttArr = JSONArray()
        taskDao.getAllTimeTableSlotsDirect().forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("dayOfWeek", it.dayOfWeek)
            obj.put("startTime", it.startTime)
            obj.put("endTime", it.endTime)
            if (it.roomOrLocation != null) obj.put("roomOrLocation", it.roomOrLocation) else obj.put("roomOrLocation", JSONObject.NULL)
            if (it.linkUrl != null) obj.put("linkUrl", it.linkUrl) else obj.put("linkUrl", JSONObject.NULL)
            obj.put("isTimeWasteAutoManaged", it.isTimeWasteAutoManaged)
            obj.put("isGoalTrackingEnabled", it.isGoalTrackingEnabled)
            obj.put("colorHex", it.colorHex)
            if (it.notes != null) obj.put("notes", it.notes) else obj.put("notes", JSONObject.NULL)
            obj.put("isNotificationEnabled", it.isNotificationEnabled)
            obj.put("createdAt", it.createdAt)
            ttArr.put(obj)
        }
        root.put("timetable_slots", ttArr)

        root.toString(4)
    }

    suspend fun importFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)

        if (root.has("tasks")) {
            val arr = root.getJSONArray("tasks")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertTask(TaskEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = if (obj.isNull("description")) null else obj.getString("description"),
                    category = if (obj.isNull("category")) null else obj.getString("category"),
                    subject = if (obj.isNull("subject")) null else obj.getString("subject"),
                    dueDate = obj.getString("dueDate"),
                    priority = obj.getString("priority"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isRecurring = obj.optBoolean("isRecurring", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    status = obj.optString("status", "todo"),
                    timeSpentMinutes = obj.optInt("timeSpentMinutes", 0),
                    linkUrl = if (obj.has("linkUrl") && !obj.isNull("linkUrl")) obj.getString("linkUrl").ifBlank { null } else null
                ))
            }
        }

        if (root.has("notes")) {
            val arr = root.getJSONArray("notes")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertNote(NoteEntity(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    date = obj.getString("date"),
                    content = obj.getString("content"),
                    topic = obj.optString("topic", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }
        }

        if (root.has("exams")) {
            val arr = root.getJSONArray("exams")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertExam(ExamEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    date = obj.getString("date")
                ))
            }
        }

        if (root.has("history")) {
            val arr = root.getJSONArray("history")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertStudyHistory(StudyHistoryEntity(
                    date = obj.getString("date"),
                    completedTasks = obj.getInt("completedTasks"),
                    studyMinutes = obj.getInt("studyMinutes"),
                    progressPercentage = obj.getInt("progressPercentage")
                ))
            }
        }

        if (root.has("settings")) {
            val arr = root.getJSONArray("settings")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertSetting(AppSettingEntity(
                    key = obj.getString("key"),
                    value = obj.getString("value")
                ))
            }
        }

        if (root.has("attendance")) {
            val arr = root.getJSONArray("attendance")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertAttendance(AttendanceEntity(
                    id = obj.getString("id"),
                    subjectName = obj.getString("subjectName"),
                    presents = obj.getInt("presents"),
                    absents = obj.getInt("absents")
                ))
            }
        }

        if (root.has("cgpa")) {
            val arr = root.getJSONArray("cgpa")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertCourse(CgpaCourseEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    credits = obj.getInt("credits"),
                    grade = obj.getString("grade")
                ))
            }
        }

        if (root.has("decks")) {
            val arr = root.getJSONArray("decks")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertDeck(DeckEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name")
                ))
            }
        }

        if (root.has("flashcards")) {
            val arr = root.getJSONArray("flashcards")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertFlashcard(FlashcardEntity(
                    id = obj.getString("id"),
                    deckId = obj.getString("deckId"),
                    front = obj.getString("front"),
                    back = obj.getString("back"),
                    interval = obj.optInt("interval", 1),
                    easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                    repetitions = obj.optInt("repetitions", 0),
                    nextDueDate = obj.optLong("nextDueDate", System.currentTimeMillis())
                ))
            }
        }

        if (root.has("habits")) {
            val arr = root.getJSONArray("habits")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertHabit(HabitEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    completedDates = obj.optString("completedDates", "")
                ))
            }
        }

        if (root.has("expenses")) {
            val arr = root.getJSONArray("expenses")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertExpense(ExpenseEntity(
                    id = obj.getString("id"),
                    description = obj.getString("description"),
                    amount = obj.getDouble("amount"),
                    date = obj.getString("date"),
                    type = obj.optString("type", "expense")
                ))
            }
        }

        if (root.has("routine_activities")) {
            val arr = root.getJSONArray("routine_activities")
            val list = mutableListOf<RoutineActivityEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(RoutineActivityEntity(
                    id = obj.getString("id"),
                    activityName = obj.getString("activityName"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    classificationScore = obj.optDouble("classificationScore", 1.0).toFloat(),
                    category = obj.optString("category", "Study"),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }
            if (list.isNotEmpty()) {
                taskDao.insertRoutineActivities(list)
            }
        }

        if (root.has("chat_history")) {
            val arr = root.getJSONArray("chat_history")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertChatMessage(ChatMessageEntity(
                    id = obj.optLong("id", 0L),
                    text = obj.getString("text"),
                    isUser = obj.optBoolean("isUser", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                ))
            }
        }

        if (root.has("app_usage_history")) {
            val arr = root.getJSONArray("app_usage_history")
            val list = mutableListOf<AppUsageEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(AppUsageEntity(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    date = obj.getString("date"),
                    packageName = obj.getString("packageName"),
                    appName = obj.getString("appName"),
                    totalTimeMillis = obj.getLong("totalTimeMillis"),
                    category = obj.optString("category", "Other"),
                    lastTimeUsed = obj.optLong("lastTimeUsed", System.currentTimeMillis())
                ))
            }
            if (list.isNotEmpty()) {
                taskDao.insertAppUsageList(list)
            }
        }

        if (root.has("timetable_slots")) {
            val arr = root.getJSONArray("timetable_slots")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                taskDao.insertTimeTableSlot(TimeTableSlotEntity(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    dayOfWeek = obj.getString("dayOfWeek"),
                    startTime = obj.getString("startTime"),
                    endTime = obj.getString("endTime"),
                    roomOrLocation = if (obj.has("roomOrLocation") && !obj.isNull("roomOrLocation")) obj.getString("roomOrLocation").ifBlank { null } else null,
                    linkUrl = if (obj.has("linkUrl") && !obj.isNull("linkUrl")) obj.getString("linkUrl").ifBlank { null } else null,
                    isTimeWasteAutoManaged = obj.optBoolean("isTimeWasteAutoManaged", false),
                    isGoalTrackingEnabled = obj.optBoolean("isGoalTrackingEnabled", true),
                    colorHex = obj.optString("colorHex", "#6366F1"),
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes").ifBlank { null } else null,
                    isNotificationEnabled = obj.optBoolean("isNotificationEnabled", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }
        }
    }
}
