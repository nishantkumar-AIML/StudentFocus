package com.ai_assistant.studentfocus.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ai_assistant.studentfocus.audio.RainAudioGenerator
import com.ai_assistant.studentfocus.database.TaskRepository
import com.ai_assistant.studentfocus.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // Audio Generator
    private val rainGenerator = RainAudioGenerator()
    private val _isRainPlaying = MutableStateFlow(false)
    val isRainPlaying = _isRainPlaying.asStateFlow()

    private val _rainVolume = MutableStateFlow(0.5f)
    val rainVolume = _rainVolume.asStateFlow()

    fun toggleRain(context: android.content.Context) {
        if (_isRainPlaying.value) {
            rainGenerator.stop()
            _isRainPlaying.value = false
        } else {
            rainGenerator.start(context)
            _isRainPlaying.value = true
        }
    }

    fun startRain(context: android.content.Context) {
        if (!_isRainPlaying.value) {
            rainGenerator.start(context)
            _isRainPlaying.value = true
        }
    }

    fun setRainVolume(volume: Float) {
        _rainVolume.value = volume
        rainGenerator.setVolume(volume)
    }

    override fun onCleared() {
        super.onCleared()
        rainGenerator.stop()
    }

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val studyHistory: StateFlow<List<StudyHistoryEntity>> = repository.studyHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val exams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<List<AppSettingEntity>?> = repository.allSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val courses: StateFlow<List<CgpaCourseEntity>> = repository.allCourses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val decks: StateFlow<List<DeckEntity>> = repository.allDecks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allFlashcards: StateFlow<List<FlashcardEntity>> = repository.allFlashcards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val habits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val routineActivities: StateFlow<List<RoutineActivityEntity>> = repository.allRoutineActivities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAppUsage: StateFlow<List<AppUsageEntity>> = repository.allAppUsage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val timeTableSlots: StateFlow<List<TimeTableSlotEntity>> = repository.allTimeTableSlots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getAppUsageForDate(date: String): Flow<List<AppUsageEntity>> = repository.getAppUsageByDate(date)

    suspend fun getAppUsageForDateDirect(date: String): List<AppUsageEntity> = repository.getAppUsageByDateList(date)

    fun saveAppUsageList(list: List<AppUsageEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.addAppUsageList(list)
        }
    }

    fun deleteAppUsageForDate(date: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteAppUsageByDate(date)
        }
    }

    // In-App Activity Sessions & Intelligence
    val allActivitySessions: StateFlow<List<AppActivitySessionEntity>> = repository.allActivitySessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategoryOverrides: StateFlow<List<AppCategoryOverrideEntity>> = repository.allCategoryOverrides
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allUserFeedback: StateFlow<List<UserFeedbackEntity>> = repository.allUserFeedback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val personalBehaviorProfile: StateFlow<PersonalBehaviorProfileEntity?> = repository.personalBehaviorProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val personalProfileState: StateFlow<PersonalBehaviorProfile> = repository.personalBehaviorProfile
        .map { entity ->
            if (entity != null) {
                com.ai_assistant.studentfocus.utils.PersonalLearningEngine.fromEntity(entity)
            } else {
                PersonalBehaviorProfile()
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersonalBehaviorProfile()
        )

    fun refreshPersonalBehaviorProfile() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sessions = repository.getAllActivitySessionsList()
                val feedback = repository.getAllUserFeedbackList()
                if (sessions.isNotEmpty() || feedback.isNotEmpty()) {
                    val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        com.ai_assistant.studentfocus.utils.PersonalLearningEngine.buildPersonalProfile(
                            sessions = sessions,
                            userFeedbackList = feedback,
                            currentTimeMillis = System.currentTimeMillis()
                        )
                    }
                    val entity = com.ai_assistant.studentfocus.utils.PersonalLearningEngine.toEntity(profile)
                    repository.savePersonalBehaviorProfile(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getActivitySessionsForDate(date: String): Flow<List<AppActivitySessionEntity>> =
        repository.getActivitySessionsByDate(date)

    suspend fun getActivitySessionsForDateDirect(date: String): List<AppActivitySessionEntity> =
        repository.getActivitySessionsByDateDirect(date)

    fun getActivitySessionsBetween(startMillis: Long, endMillis: Long): Flow<List<AppActivitySessionEntity>> =
        repository.getActivitySessionsBetween(startMillis, endMillis)

    fun saveActivitySession(session: AppActivitySessionEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.addActivitySession(session)
        }
    }

    fun saveActivitySessions(sessions: List<AppActivitySessionEntity>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.addActivitySessions(sessions)
        }
    }

    fun updateActivityCategory(packageName: String, contentTitle: String, newCategory: String, originalCategory: String = "Other") {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateActivityCategory(packageName, contentTitle, newCategory)
            repository.addCategoryOverride(AppCategoryOverrideEntity(targetKey = "$packageName:$contentTitle", customCategory = newCategory))
            repository.addUserFeedback(
                UserFeedbackEntity(
                    packageName = packageName,
                    appName = packageName.substringAfterLast('.'),
                    contentTitle = contentTitle,
                    originalCategory = originalCategory,
                    userCategory = newCategory,
                    feedbackType = "CORRECTION",
                    weight = 3.0f
                )
            )
        }
    }

    fun updateAppCategory(packageName: String, newCategory: String, originalCategory: String = "Other") {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateAppCategory(packageName, newCategory)
            repository.addCategoryOverride(AppCategoryOverrideEntity(targetKey = packageName, customCategory = newCategory))
            repository.addUserFeedback(
                UserFeedbackEntity(
                    packageName = packageName,
                    appName = packageName.substringAfterLast('.'),
                    contentTitle = "All Content",
                    originalCategory = originalCategory,
                    userCategory = newCategory,
                    feedbackType = "CORRECTION",
                    weight = 3.0f
                )
            )
        }
    }

    fun recordUserFeedback(feedback: UserFeedbackEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.addUserFeedback(feedback)
        }
    }

    fun resetPersonalLearningData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.resetPersonalLearningData()
        }
    }

    fun deleteActivitySessionById(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteActivitySessionById(id)
        }
    }

    fun deleteActivitySessionsForDate(date: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteActivitySessionsForDate(date)
        }
    }

    // Task CRUD with status support for Kanban
    fun addTask(
        title: String,
        description: String?,
        category: String?,
        subject: String?,
        dueDate: String,
        priority: String,
        isPinned: Boolean = false,
        isRecurring: Boolean = false,
        status: String = "todo",
        linkUrl: String? = null
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                category = category ?: "General",
                subject = subject,
                dueDate = dueDate,
                priority = priority,
                isPinned = isPinned,
                isRecurring = isRecurring,
                status = status,
                linkUrl = linkUrl
            )
            repository.addTask(task)
        }
    }

    fun updateTask(
        id: String,
        title: String,
        description: String?,
        category: String?,
        subject: String?,
        dueDate: String,
        priority: String,
        isPinned: Boolean,
        isRecurring: Boolean,
        status: String,
        createdAt: Long,
        linkUrl: String? = null
    ) {
        viewModelScope.launch {
            val oldTask = repository.getAllTasksList().find { it.id == id }
            val wasCompleted = oldTask?.isCompleted ?: false
            val isCompletedNow = status == "done"
            val timeSpent = oldTask?.timeSpentMinutes ?: 0
            val compNotes = oldTask?.completionNotes

            val task = TaskEntity(
                id = id,
                title = title,
                description = description,
                category = category ?: "General",
                subject = subject,
                dueDate = dueDate,
                priority = priority,
                isPinned = isPinned,
                isRecurring = isRecurring,
                status = status,
                createdAt = createdAt,
                timeSpentMinutes = timeSpent,
                completionNotes = compNotes,
                linkUrl = linkUrl
            )
            repository.addTask(task)

            if (wasCompleted != isCompletedNow) {
                val xpChange = if (isCompletedNow) 20 else -20
                adjustXp(xpChange)
            }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.isCompleted) {
                markTaskIncomplete(task)
            } else {
                logTaskCompletionTimeAndNotes(task.id, 30, null)
            }
        }
    }

    fun updateTaskStatus(task: TaskEntity, nextStatus: String) {
        viewModelScope.launch {
            val wasCompleted = task.isCompleted
            val isCompletedNow = nextStatus == "done"
            repository.updateTaskStatus(task, nextStatus)
            if (wasCompleted != isCompletedNow) {
                val xpChange = if (isCompletedNow) 20 else -20
                adjustXp(xpChange)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            if (task.isCompleted) {
                adjustXp(-20)
                // Also deduct from study history
                val existing = repository.studyHistory.first().find { it.date == task.dueDate }
                if (existing != null) {
                    val newMins = maxOf(0, existing.studyMinutes - task.timeSpentMinutes).coerceAtMost(1440)
                    repository.addStudyHistory(existing.copy(studyMinutes = newMins))
                }
            }
        }
    }

    fun logTaskCompletionTimeAndNotes(taskId: String, minutes: Int, notes: String?) {
        if (minutes <= 0) return
        viewModelScope.launch {
            val tasksList = repository.getAllTasksList()
            val task = tasksList.find { it.id == taskId } ?: return@launch
            
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val targetDate = task.dueDate.ifBlank { todayStr }
            val existing = repository.studyHistory.first().find { it.date == targetDate }
            val currentMins = existing?.studyMinutes ?: 0
            
            // Check effective routine study budget (e.g. 240 mins) or fallback to 1440
            val routineList = repository.getAllRoutineActivitiesList()
            val routineBudget = routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
            val effectiveDailyLimit = if (routineBudget in 1..1440) routineBudget else 1440

            val remainingCapacity = maxOf(0, effectiveDailyLimit - currentMins)
            if (remainingCapacity <= 0 || minutes > remainingCapacity) {
                // Exceeds effective daily study budget - do not allow saving beyond limit
                return@launch
            }
            
            val actualMinsToAdd = minutes
            val totalMins = currentMins + actualMinsToAdd

            val updatedTask = task.copy(
                isCompleted = true,
                status = "done",
                timeSpentMinutes = actualMinsToAdd,
                completionNotes = notes
            )
            repository.addTask(updatedTask)
            
            adjustXp(20)

            val completedToday = tasksList.count { it.dueDate == targetDate && (it.isCompleted || it.id == taskId) }
            val totalTasksToday = tasksList.count { it.dueDate == targetDate }
            val progressPercent = if (totalTasksToday > 0) (completedToday * 100) / totalTasksToday else 0

            repository.addStudyHistory(StudyHistoryEntity(targetDate, completedToday, totalMins, progressPercent))
        }
    }

    fun markTaskIncomplete(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = false,
                status = "todo",
                timeSpentMinutes = 0,
                completionNotes = null
            )
            repository.addTask(updatedTask)
            adjustXp(-20)
            
            val existing = repository.studyHistory.first().find { it.date == task.dueDate }
            if (existing != null) {
                val newMins = maxOf(0, existing.studyMinutes - task.timeSpentMinutes).coerceAtMost(1440)
                repository.addStudyHistory(existing.copy(studyMinutes = newMins))
            }
        }
    }

    // Daily Notes
    fun getNotesForDate(date: String): Flow<List<NoteEntity>> {
        return repository.getNotesForDate(date)
    }

    suspend fun getNotesForDateDirect(date: String): List<NoteEntity> {
        return repository.getNotesForDateDirect(date)
    }

    fun getNoteForDate(date: String): Flow<NoteEntity?> {
        return repository.getNoteForDate(date)
    }

    suspend fun getNoteByDateDirect(date: String): NoteEntity? {
        return repository.getNoteByDateDirect(date)
    }

    suspend fun getNoteByIdDirect(id: String): NoteEntity? {
        return repository.getNoteByIdDirect(id)
    }

    fun saveNote(
        id: String = java.util.UUID.randomUUID().toString(),
        date: String,
        content: String,
        topic: String = "",
        createdAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addNote(NoteEntity(id = id, date = date, content = content, topic = topic, createdAt = createdAt))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteNoteById(id: String) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // Exam Countdowns
    fun addExam(name: String, date: String) {
        viewModelScope.launch {
            repository.addExam(ExamEntity(name = name, date = date))
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    // Study History Logs - Strictly Capped at max 24 hours (1440 minutes) per day
    fun addStudyHistory(date: String, completedTasks: Int, studyMinutes: Int, progressPercentage: Int) {
        // If user tries to add > 24 hours, reject it
        if (studyMinutes < 0 || studyMinutes > 1440) {
            return
        }
        viewModelScope.launch {
            val clampedMins = studyMinutes.coerceIn(0, 1440)
            repository.addStudyHistory(StudyHistoryEntity(date, completedTasks, clampedMins, progressPercentage))
        }
    }

    fun addStudyMinutes(date: String, additionalMinutes: Int, completedTasks: Int, progressPercentage: Int) {
        if (additionalMinutes <= 0 || additionalMinutes > 1440) {
            return
        }
        viewModelScope.launch {
            val history = repository.studyHistory.first()
            val existing = history.find { it.date == date }
            val currentMins = existing?.studyMinutes ?: 0
            if (currentMins >= 1440) {
                // Max 24 hours already reached for this day - DO NOT ADD!
                return@launch
            }
            val totalMins = minOf(1440, currentMins + additionalMinutes)
            repository.addStudyHistory(StudyHistoryEntity(date, completedTasks, totalMins, progressPercentage))
        }
    }

    // Smart Planner Helpers
    fun getSmartDailyPlan(sustainableCapacity: Double = 4.0, startHour: Int = 8): com.ai_assistant.studentfocus.planner.SmartDailyPlan {
        val currentTasks = tasks.value
        return com.ai_assistant.studentfocus.planner.TaskPlanner.generateSmartDailyPlan(
            tasks = currentTasks,
            sustainableCapacity = minOf(24.0, maxOf(0.5, sustainableCapacity)),
            startHour = startHour
        )
    }

    // Settings
    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.addSetting(AppSettingEntity(key, value))
        }
    }

    fun removeSetting(key: String) {
        viewModelScope.launch {
            repository.deleteSetting(AppSettingEntity(key, ""))
        }
    }

    // 1. Attendance Tracker
    fun addSubject(subjectName: String) {
        viewModelScope.launch {
            repository.addAttendance(AttendanceEntity(subjectName = subjectName))
        }
    }

    fun incrementPresent(a: AttendanceEntity) {
        viewModelScope.launch {
            repository.addAttendance(a.copy(presents = a.presents + 1))
        }
    }

    fun incrementAbsent(a: AttendanceEntity) {
        viewModelScope.launch {
            repository.addAttendance(a.copy(absents = a.absents + 1))
        }
    }

    fun deleteSubject(a: AttendanceEntity) {
        viewModelScope.launch {
            repository.deleteAttendance(a)
        }
    }

    // 2. CGPA Courses
    fun addCourse(name: String, credits: Int, grade: String) {
        viewModelScope.launch {
            repository.addCourse(CgpaCourseEntity(name = name, credits = credits, grade = grade))
        }
    }

    fun deleteCourse(c: CgpaCourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(c)
        }
    }

    // 3. Decks & Flashcards
    fun addDeck(name: String) {
        viewModelScope.launch {
            repository.addDeck(DeckEntity(name = name))
        }
    }

    fun deleteDeck(d: DeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(d)
        }
    }

    fun getFlashcardsForDeck(deckId: String): Flow<List<FlashcardEntity>> {
        return repository.getFlashcardsForDeck(deckId)
    }

    fun addFlashcard(deckId: String, front: String, back: String, difficulty: String = "Good") {
        viewModelScope.launch {
            repository.addFlashcard(FlashcardEntity(
                deckId = deckId,
                front = front,
                back = back,
                difficulty = difficulty
            ))
        }
    }

    fun deleteFlashcard(f: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteFlashcard(f)
        }
    }

    fun deleteFlashcardById(id: String) {
        viewModelScope.launch {
            repository.deleteFlashcardById(id)
        }
    }

    fun reviewFlashcard(f: FlashcardEntity, difficulty: String) {
        viewModelScope.launch {
            val rating = when (difficulty) {
                "Hard" -> 1
                "Easy" -> 5
                else -> 3
            }
            val (newRepetitions, newInterval, newEaseFactor) = if (rating >= 3) {
                val reps = f.repetitions + 1
                val interval = if (reps == 1) 1 else if (reps == 2) 6 else (f.interval * f.easeFactor).toInt()
                val ease = f.easeFactor + (0.1f - (5f - rating) * (0.08f + (5f - rating) * 0.02f))
                Triple(reps, interval, ease.coerceAtLeast(1.3f))
            } else {
                Triple(0, 1, (f.easeFactor - 0.2f).coerceAtLeast(1.3f))
            }

            val nextDueDate = System.currentTimeMillis() + newInterval * 86400L * 1000L
            repository.addFlashcard(f.copy(
                difficulty = difficulty,
                repetitions = newRepetitions,
                interval = newInterval,
                easeFactor = newEaseFactor,
                nextDueDate = nextDueDate
            ))
        }
    }

    fun reviewFlashcard(f: FlashcardEntity, rating: Int) {
        val diff = when (rating) {
            1 -> "Hard"
            5 -> "Easy"
            else -> "Good"
        }
        reviewFlashcard(f, diff)
    }

    // 4. Habits Grid
    fun addHabit(name: String) {
        viewModelScope.launch {
            repository.addHabit(HabitEntity(name = name))
        }
    }

    fun deleteHabit(h: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(h)
        }
    }

    fun toggleHabitDay(h: HabitEntity, date: String) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dateList = h.completedDates.split(",").filter { it.isNotBlank() }.toMutableList()

            if (dateList.contains(date)) {
                // Un-check today only — one tick per day, cannot tick same date twice
                dateList.remove(date)
                repository.addHabit(h.copy(completedDates = dateList.joinToString(",")))
            } else {
                // Already ticked some other date today? Ignore (only today's date is allowed once)
                // Check if yesterday was missed and reset if so
                if (dateList.isNotEmpty()) {
                    val parsedToday = try { sdf.parse(date) } catch (e: Exception) { null }
                    if (parsedToday != null) {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = parsedToday
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val yesterdayStr = sdf.format(cal.time)

                        // If yesterday's date is not in the list, they missed a day -> RESET entire streak
                        if (!dateList.contains(yesterdayStr)) {
                            dateList.clear()
                        }
                    }
                }
                // Only add today's date (cannot add same date more than once)
                if (!dateList.contains(date)) {
                    dateList.add(date)
                }
                // Cap completed days at 21 days maximum
                val finalDates = if (dateList.size > 21) dateList.takeLast(21) else dateList
                repository.addHabit(h.copy(completedDates = finalDates.joinToString(",")))
            }
        }
    }

    fun resetHabitStreak(h: HabitEntity) {
        viewModelScope.launch {
            repository.addHabit(h.copy(completedDates = ""))
        }
    }

    // 5. Ledger Expenses
    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.addExpense(expense)
        }
    }

    fun importExpenses(list: List<ExpenseEntity>) {
        viewModelScope.launch {
            repository.addExpenses(list)
        }
    }

    fun deleteExpense(e: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(e)
        }
    }

    // 6. Time Routine & Daily 24-Hour Time Audit
    fun addRoutineActivity(activity: RoutineActivityEntity) {
        viewModelScope.launch {
            repository.addRoutineActivity(activity)
            updateDailyStudyBudgetFromRoutine()
        }
    }

    fun importRoutineActivities(list: List<RoutineActivityEntity>) {
        viewModelScope.launch {
            repository.addRoutineActivities(list)
            updateDailyStudyBudgetFromRoutine()
        }
    }

    fun deleteRoutineActivity(activity: RoutineActivityEntity) {
        viewModelScope.launch {
            repository.deleteRoutineActivity(activity)
            updateDailyStudyBudgetFromRoutine()
        }
    }

    // 7. Time Table Schedule Slots
    fun saveTimeTableSlot(slot: TimeTableSlotEntity, context: Context) {
        viewModelScope.launch {
            repository.addTimeTableSlot(slot)
            if (slot.isNotificationEnabled) {
                com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, slot)
            } else {
                com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, slot.id)
            }
        }
    }

    fun deleteTimeTableSlot(slot: TimeTableSlotEntity, context: Context) {
        viewModelScope.launch {
            repository.deleteTimeTableSlot(slot)
            com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, slot.id)
        }
    }

    fun deleteTimeTableSlotScope(slot: TimeTableSlotEntity, targetDay: String, deleteAllDays: Boolean, context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            if (deleteAllDays) {
                repository.deleteTimeTableSlot(slot)
                com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, slot.id)

                val allSlots = repository.getAllTimeTableSlotsList()
                val matching = allSlots.filter { it.title.equals(slot.title, ignoreCase = true) && it.startTime == slot.startTime }
                for (m in matching) {
                    repository.deleteTimeTableSlot(m)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, m.id)
                }
            } else {
                if (slot.dayOfWeek.equals("Daily", ignoreCase = true)) {
                    repository.deleteTimeTableSlot(slot)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, slot.id)
                    val remainingDays = allDays.filter { !it.equals(targetDay, ignoreCase = true) }
                    for (day in remainingDays) {
                        val splitSlot = slot.copy(id = java.util.UUID.randomUUID().toString(), dayOfWeek = day)
                        repository.addTimeTableSlot(splitSlot)
                        if (splitSlot.isNotificationEnabled) {
                            com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, splitSlot)
                        }
                    }
                } else {
                    repository.deleteTimeTableSlot(slot)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, slot.id)
                }
            }
        }
    }

    fun saveTimeTableSlotScope(
        originalSlot: TimeTableSlotEntity?,
        updatedSlot: TimeTableSlotEntity,
        targetDay: String,
        applyToAllDays: Boolean,
        context: Context
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            if (applyToAllDays) {
                val finalSlot = updatedSlot.copy(dayOfWeek = "Daily")
                repository.addTimeTableSlot(finalSlot)
                if (finalSlot.isNotificationEnabled) {
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, finalSlot)
                }
                if (originalSlot != null && !originalSlot.dayOfWeek.equals("Daily", ignoreCase = true)) {
                    val allSlots = repository.getAllTimeTableSlotsList()
                    val matching = allSlots.filter { it.id != finalSlot.id && it.title.equals(originalSlot.title, ignoreCase = true) && it.startTime == originalSlot.startTime }
                    for (m in matching) {
                        repository.deleteTimeTableSlot(m)
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, m.id)
                    }
                }
            } else {
                if (originalSlot != null && originalSlot.dayOfWeek.equals("Daily", ignoreCase = true)) {
                    repository.deleteTimeTableSlot(originalSlot)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, originalSlot.id)
                    val daySlot = updatedSlot.copy(id = java.util.UUID.randomUUID().toString(), dayOfWeek = targetDay)
                    repository.addTimeTableSlot(daySlot)
                    if (daySlot.isNotificationEnabled) {
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, daySlot)
                    }
                    val remainingDays = allDays.filter { !it.equals(targetDay, ignoreCase = true) }
                    for (day in remainingDays) {
                        val keepSlot = originalSlot.copy(id = java.util.UUID.randomUUID().toString(), dayOfWeek = day)
                        repository.addTimeTableSlot(keepSlot)
                        if (keepSlot.isNotificationEnabled) {
                            com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, keepSlot)
                        }
                    }
                } else {
                    val finalSlot = updatedSlot.copy(dayOfWeek = targetDay)
                    repository.addTimeTableSlot(finalSlot)
                    if (finalSlot.isNotificationEnabled) {
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, finalSlot)
                    }
                }
            }
        }
    }

    fun saveTimeTableSlotMultiDays(
        originalSlot: TimeTableSlotEntity?,
        updatedSlot: TimeTableSlotEntity,
        selectedDays: Set<String>,
        isDailySeries: Boolean,
        context: Context
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allWeekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            if (isDailySeries || selectedDays.contains("Daily") || selectedDays.size >= 7) {
                // Daily series across all 7 days
                val finalSlot = updatedSlot.copy(dayOfWeek = "Daily")
                repository.addTimeTableSlot(finalSlot)
                if (finalSlot.isNotificationEnabled) {
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, finalSlot)
                }
                if (originalSlot != null && !originalSlot.dayOfWeek.equals("Daily", ignoreCase = true)) {
                    val allSlots = repository.getAllTimeTableSlotsList()
                    val matching = allSlots.filter { it.id != finalSlot.id && it.title.equals(originalSlot.title, ignoreCase = true) && it.startTime == originalSlot.startTime }
                    for (m in matching) {
                        repository.deleteTimeTableSlot(m)
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, m.id)
                    }
                }
            } else {
                // If original slot was Daily and user changed to specific days, clean up old Daily slot
                if (originalSlot != null && originalSlot.dayOfWeek.equals("Daily", ignoreCase = true)) {
                    repository.deleteTimeTableSlot(originalSlot)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, originalSlot.id)
                }

                // If editing an existing single-day slot and user changed the day or selected multiple days
                if (originalSlot != null && !originalSlot.dayOfWeek.equals("Daily", ignoreCase = true) && !selectedDays.contains(originalSlot.dayOfWeek)) {
                    repository.deleteTimeTableSlot(originalSlot)
                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(context, originalSlot.id)
                }

                // Save / Update for each selected day
                val validDays = selectedDays.filter { it in allWeekDays }
                for (day in validDays) {
                    val isOriginalDay = originalSlot != null && originalSlot.dayOfWeek.equals(day, ignoreCase = true)
                    val daySlot = updatedSlot.copy(
                        id = if (isOriginalDay) originalSlot!!.id else java.util.UUID.randomUUID().toString(),
                        dayOfWeek = day
                    )
                    repository.addTimeTableSlot(daySlot)
                    if (daySlot.isNotificationEnabled) {
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(context, daySlot)
                    }
                }
            }
        }
    }

    fun toggleTimeTableNotification(slot: TimeTableSlotEntity, context: Context) {
        val updated = slot.copy(isNotificationEnabled = !slot.isNotificationEnabled)
        saveTimeTableSlot(updated, context)
    }

    fun updateDailyStudyBudgetFromRoutine() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val list = repository.getAllRoutineActivitiesList()
            val totalUsefulMins = list.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
            val clampedBudget = totalUsefulMins.coerceIn(0, 1440)
            repository.addSetting(AppSettingEntity("routine_study_budget_minutes", clampedBudget.toString()))
        }
    }

    fun loadDefaultStudentRoutine() {
        viewModelScope.launch {
            val defaults = listOf(
                RoutineActivityEntity(activityName = "Sleep", durationMinutes = 420, classificationScore = 0.0f, category = "Sleep", notes = "7 hours healthy sleep"),
                RoutineActivityEntity(activityName = "College / School Lectures", durationMinutes = 360, classificationScore = 0.3f, category = "College", notes = "Classes, labs & notes"),
                RoutineActivityEntity(activityName = "Self Study & Deep Work", durationMinutes = 240, classificationScore = 1.0f, category = "Study", notes = "Exam prep & coding"),
                RoutineActivityEntity(activityName = "Exercise & Meals", durationMinutes = 120, classificationScore = 0.5f, category = "Fitness", notes = "Workout, breakfast, dinner"),
                RoutineActivityEntity(activityName = "Relaxation / Social", durationMinutes = 120, classificationScore = 0.0f, category = "Waste", notes = "Gaming, breaks & phone"),
                RoutineActivityEntity(activityName = "Daily Buffer & Travel", durationMinutes = 180, classificationScore = 0.1f, category = "Routine", notes = "Commute & miscellaneous")
            )
            defaults.forEach { repository.addRoutineActivity(it) }
            updateDailyStudyBudgetFromRoutine()
        }
    }

    fun acknowledgeMonthlyRoutineReview() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
            repository.addSetting(AppSettingEntity("last_routine_review_month", currentMonth))
        }
    }

    fun adjustXp(amount: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentXpStr = repository.getSetting("user_xp").first()?.value
            val totalXp = if (currentXpStr == null) {
                val completedTasksCount = repository.getAllTasksList().count { it.isCompleted }
                completedTasksCount * 20
            } else {
                currentXpStr.toIntOrNull() ?: 0
            }
            val newXp = maxOf(0, totalXp + amount)
            repository.addSetting(AppSettingEntity("user_xp", newXp.toString()))
        }
    }

    fun performDailyXpDecayAndSetup(
        tasks: List<TaskEntity>,
        history: List<StudyHistoryEntity>,
        settingsList: List<AppSettingEntity>
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lastActiveStr = settingsList.find { it.key == "last_active_date" }?.value
            val currentXpStr = settingsList.find { it.key == "user_xp" }?.value

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date())

            var totalXp = if (currentXpStr == null) {
                val completedTasksCount = tasks.count { it.isCompleted }
                completedTasksCount * 20
            } else {
                currentXpStr.toIntOrNull() ?: 0
            }

            if (lastActiveStr == null) {
                repository.addSetting(AppSettingEntity("user_xp", totalXp.toString()))
                repository.addSetting(AppSettingEntity("last_active_date", todayStr))
                return@launch
            }

            if (lastActiveStr == todayStr) {
                return@launch
            }

            try {
                val lastActiveDate = sdf.parse(lastActiveStr) ?: java.util.Date()
                val todayDate = sdf.parse(todayStr) ?: java.util.Date()

                val cal = java.util.Calendar.getInstance().apply { time = lastActiveDate }
                val todayCal = java.util.Calendar.getInstance().apply { time = todayDate }

                var decayedXp = totalXp
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)

                while (cal.before(todayCal)) {
                    val dateToCheck = sdf.format(cal.time)
                    val hasStudy = history.any { it.date == dateToCheck && (it.studyMinutes > 0 || it.completedTasks > 0) }
                    val hasCompletedTask = tasks.any { it.dueDate == dateToCheck && it.isCompleted }
                    val worked = hasStudy || hasCompletedTask

                    if (!worked) {
                        decayedXp = maxOf(0, decayedXp - 10)
                    }
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }

                if (decayedXp != totalXp) {
                    totalXp = decayedXp
                    repository.addSetting(AppSettingEntity("user_xp", totalXp.toString()))
                }
                repository.addSetting(AppSettingEntity("last_active_date", todayStr))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Backup & Restore
    fun exportBackup(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportToJson()
                onResult(json)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun restoreBackup(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.importFromJson(jsonString)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
