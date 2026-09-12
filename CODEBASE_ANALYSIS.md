# 🧠 Student Focus — Master Codebase Analysis & Architecture Cheatsheet

> **Purpose:** This document is the definitive technical reference for the entire *Student Focus* project. It captures all architectural decisions, state flows, database schemas, timer loops, and calculations so that any developer or AI assistant can instantly understand the codebase without re-analyzing raw files.

---

## 📌 Quick Summary & Metadata
* **App Name:** Student Focus
* **Package Name / Namespace:** `com.ai_assistant.studentfocus`
* **Version:** Version 4.0 (`versionCode = 4`, `versionName = "4.0"`)
* **Minimum SDK:** 29 (Android 10.0+) | **Target/Compile SDK:** 37
* **Kotlin Version:** 2.0 | **Java Version:** JDK 17
* **UI Framework:** Jetpack Compose (Material 3)
* **Optimization & Shrinking:** R8 Full Mode (`android.enableR8.fullMode=true`)
* **Room Database Version:** v20 (with `MIGRATION_19_20`)
* **GitHub Repository:** [https://github.com/nishantkumar-AIML/StudentFocus](https://github.com/nishantkumar-AIML/StudentFocus)

---

## 🗂️ Project Directory Layout & File Responsibilities

```
studentfocus/
├── app/src/main/
│   ├── java/com/ai_assistant/studentfocus/
│   │   ├── database/             # Room DB, DAOs, and Repositories
│   │   ├── models/               # All Room Entities and domain data classes
│   │   ├── planner/              # Smart task planning and calendar helpers
│   │   ├── service/              # Foreground and Accessibility services
│   │   ├── timer/                # Timers, receivers, notification helpers, schedulers
│   │   ├── ui/                   # Jetpack Compose UI screens, dialogs, custom charts
│   │   ├── utils/                # AI heuristics, learning engine, usage helpers
│   │   └── viewmodel/            # ViewModels, factories, dataset
│   └── res/                      # Drawables, layouts, mipmaps, XML configs
├── CMakeLists.txt                # Desktop companion C++17 build config (GLFW/ImGui)
├── src/ & include/               # C++17 native desktop client source files
├── gradle.properties             # R8 full mode & JVM parameters
└── build.gradle.kts              # Root and app build configurations
```

---

## 🏛️ Core Architectural Components

### 1. Data Layer (`database/` & `models/`)
* **`AppDatabase.kt`**:
  * Room database singleton, version **20**.
  * Contains `MIGRATION_19_20` which added performance indices to `app_activity_sessions` (`date`, `startTime`, `packageName`) and `app_usage` (`date`).
* **`TaskDao.kt`**:
  * Unified Data Access Object for tasks, study history, notes, exams, timetable slots, routine activities, habit logs, attendance, CGPA, and behavior profiles.
  * Key direct query: `getStudyHistoryDirect(date: String)` used for synchronous/worker logging without blocking UI flows.
* **`TaskRepository.kt`**:
  * Single source of truth abstracting `TaskDao`. Emits reactive `Flow`s for UI consumption and provides backup export/import (JSON).

#### Key Database Entities:
1. `TaskEntity`: Tasks with priority, status (`todo`, `in_progress`, `done`), due date, `timeSpentMinutes`, completion notes.
2. `StudyHistoryEntity`: Daily logged study minutes (`date`, `completedTasks`, `studyMinutes`, `progressPercentage`).
3. `TimeTableSlotEntity`: Weekly/daily schedule slots with:
   * `isTimeWasteAutoManaged`: Auto-pause waste tracker during class.
   * `isGoalTrackingEnabled`: Count in routine budget / timetable capacity.
   * `linkUrl`: Direct URL for Zoom/Google Meet lectures.
4. `RoutineActivityEntity`: 24-hour daily routine activities (`durationMinutes`, `classificationScore` 0.0 to 1.0, `category`).
5. `AppActivitySessionEntity` & `AppUsageEntity`: Real-time application usage logs categorized as Productive, Neutral, or Distracting.
6. `PersonalBehaviorProfileEntity`: Learned personal productivity profile ($w = e^{-\lambda \Delta t}$).

---

### 2. Timer & Background Execution Engine (`timer/`)

#### A. `FocusSessionManager.kt` (Task Focus Sessions)
* **Role:** Manages active and paused task-linked study sessions.
* **State:**
  * `_taskSessions`: `StateFlow<Map<String, ActiveFocusSession>>` (supports multiple parallel tasks, one actively ticking).
  * `_activeSession`: `StateFlow<ActiveFocusSession?>` (the currently running session).
* **Battery & Notification Optimization:**
  * Throttles IPC updates to state transitions + 60s fallback sync.
  * Uses native Android Chronometer countdowns (`setUsesChronometer(true)`).
* **Automatic Progress Persistence:**
  * In `stopSession(context, taskId, saveProgress)`: When a session is stopped or swiped away, `saveStudiedMinutesToDb(context, studiedMinutes)` immediately writes elapsed minutes to `StudyHistoryEntity` in Room DB. **Studied time never drops or disappears.**

#### B. `GeneralTimerManager.kt` (Stopwatch & Pomodoro)
* **Role:** Manages general non-task timers (Mode 0 = Stopwatch, Mode 1 = Pomodoro).
* **State:** `isRunning`, `stopwatchElapsedSeconds`, `pomodoroRemainingSeconds`.
* **Persistence:** When `resetTimer(context)` is called, any accumulated study time is auto-persisted to `StudyHistoryEntity`.

#### C. `TimeWasteManager.kt` (Time Waste Tracker)
* **Role:** Measures idle and non-productive screen time in the background.
* **Ticking Condition:** Only runs when `isStudyActive()` is `false`.
* **`isStudyActive()` Logic:**
  ```kotlin
  fun isStudyActive(): Boolean {
      val isTaskActive = FocusSessionManager.activeSession.value?.isRunning == true
      val isGeneralTimerActive = GeneralTimerManager.isRunning.value
      val isAutoTimeTableActive = isAutoTimeTableSlotActiveNow()
      return isTaskActive || isGeneralTimerActive || isAutoTimeTableActive
  }
  ```
* **Native Timetable Synchronization:**
  * `updateTimeTableSlots(slots)` receives reactive updates from `MainActivity`.
  * `isAutoTimeTableSlotActiveNow()` inspects current day and minute of day. If user is in a timetable slot with `isTimeWasteAutoManaged == true`, **Time Waste Tracker pauses automatically** without launching artificial task timers.

#### D. Schedulers & Receivers
* **`TimeTableReminderScheduler.kt` & `TimeTableReminderReceiver.kt`**:
  * Schedules exact alarms **5 minutes before class start** (`add(Calendar.MINUTE, -5)`).
  * Notification: Channel `student_daily_reminder_channel` with `IMPORTANCE_HIGH`, vibration, heads-up display, room location, and an **"🔗 Open Link"** action button.
  * Receiver uses `goAsync()` pattern with `pendingResult.finish()` in `finally`.
* **`DailyReminderScheduler.kt` & `DailyTaskReminderReceiver.kt`**:
  * Handles morning/evening daily task reviews via `goAsync()`.

---

### 3. AI & Behavioral Intelligence (`utils/` & `viewmodel/`)

#### A. Primary Engine: Rule-Based & Adaptive Math Engine (0ms, 0% Battery)
* **`PersonalLearningEngine.kt`**:
  * Exponential time decay: $w = e^{-\lambda \Delta t}$ with 14-day half-life ($\lambda = \frac{\ln 2}{14 \text{ days}}$).
  * Automatically detects peak focus hours and advances profile maturity:
    $$\text{NEW\_USER} \longrightarrow \text{LEARNING} \longrightarrow \text{ADAPTIVE} \longrightarrow \text{PERSONALIZED}$$
* **`ConversationalDataset.kt`**:
  * Deterministic multilingual response bank (English, Hinglish, Tamil, Telugu).
  * Delivers immediate study tips, routine recommendations, and motivational responses with zero hallucinations and zero cloud dependencies.
* **`ActivityClassifier.kt` & `AppUsageClassifier.kt`**:
  * Real-time heuristics mapping package names and window titles to `STUDY`, `COLLEGE`, `NEUTRAL`, or `WASTE`.

#### B. Secondary Engine: On-Device Gemma LLM (Deep Reasoning)
* **`GemmaViewModel.kt`**:
  * Wraps Google MediaPipe Tasks GenAI (`LlmInference`).
  * Dedicated for on-demand deep tutoring, long-form syllabus planning, and conceptual questions.
  * Properly closes model and releases executor memory on `onCleared()`.

---

### 4. User Interface Architecture (`ui/`)

* **`MainActivity.kt`**:
  * Single Activity hosting Jetpack Compose Navigation.
  * Initializes `FocusSessionManager`, `GeneralTimerManager`, `TimeWasteManager`.
  * Collects `taskViewModel.timeTableSlots` and updates `TimeWasteManager.updateTimeTableSlots(slots)`.
  * Handles Biometric unlock prompt before showing dashboard content.

* **`DashboardUI.kt`**:
  * **Active Session Card:** Displays live ticking focus session. Supports **Right-Swipe-to-Dismiss** gesture (`pointerInput` + `detectHorizontalDragGestures`). Swiping right past 250px stops session and auto-persists progress.
  * **Scholar Card (Novice Learner):** Gamification widget showing Level, XP progress bar, and evolving avatar.
  * **Study Goal Pace Calculator Card (`StudyGoalPaceCalculatorCard`):**
    * Target Goal (e.g., 20h in 7 days).
    * `studyMinutesSinceStart`: Sum of history records from goal start date.
    * `liveTaskMins`: Active focus session minutes.
    * `accumulatedStudiedMinutes = studyMinutesSinceStart + liveTaskMins`.
    * Clean transition: When a timer stops, `liveTaskMins` moves into `studyMinutesSinceStart` instantly.
  * **Daily Routine Budget (`routineBudgetMinutes`):**
    * Calculated strictly from 24-hour routine activities:
      `routineActivities.sumOf { (it.durationMinutes * it.classificationScore).toInt() }` clamped to `1..1440`.

* **`AcademicSubTabs.kt`**:
  * Contains 6 academic sub-modules:
    1. Timetable (Daily & Weekly grid, 5-min notifications, link launcher).
    2. Attendance Tracker (Subject-wise 75% criteria calculator).
    3. CGPA & Semester Grade Tracker.
    4. Student Budget & Expense Manager.
    5. Exam Schedule & Countdown.
    6. 24-Hour Routine Audit & Time Invested vs. Wasted Analytics.

* **`KanbanBoard.kt`**:
  * Three-column board (`To Do`, `In Progress`, `Done`) with drag-and-drop / click-to-move workflows and completion dialogs.

---

## ⚙️ ProGuard & R8 Optimization Reference

* **File:** `app/proguard-rules.pro`
* **Configuration:**
  * Enabled: R8 Full Mode (`android.enableR8.fullMode=true` in `gradle.properties`).
  * Repackaging: `-repackageclasses 'com.ai_assistant.studentfocus.opt'`
  * Optimization Passes: `-optimizationpasses 5`
  * Interface Merging: `-mergeinterfacesaggressively`
  * Access Modification: `-allowaccessmodification`
  * Safe Preserves: Room `@Entity`, `@Dao`, RoomDatabase subclasses, MediaPipe JNI native bindings, and ViewModel reflection constructors.
  * Removed obsolete broad `-keep class androidx.compose.**` and `kotlinx.coroutines.**` which previously lowered Play Console scores.

---

## 🛠️ Essential Gradle Commands

| Action | Command | Output Artifact |
| :--- | :--- | :--- |
| **Debug Build** | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| **Release Bundle (AAB)** | `./gradlew bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` |
| **Release APK** | `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| **Run Unit Tests** | `./gradlew test` | `app/build/reports/tests/testDebugUnitTest/` |
| **Install on Device** | `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Live on device/emulator |

---

## 💡 Troubleshooting & Common Gotchas

1. **Why was Time Waste continuing during college classes?**
   * *Fixed:* `TimeWasteManager.isStudyActive()` now checks `isAutoTimeTableSlotActiveNow()`. Keep `isTimeWasteAutoManaged = true` on the slot.
2. **Why was Study Pace showing 21h 59m daily budget?**
   * *Fixed:* `routineBudgetMinutes` previously added weekly timetable slots on top of the 24h routine. It now calculates strictly from `routineActivities` (max 24 hours).
3. **Why did studied minutes disappear when the timer was stopped?**
   * *Fixed:* `FocusSessionManager.stopSession()` now automatically commits `saveStudiedMinutesToDb()` to Room DB before clearing in-memory maps.
4. **Why was Play Console reporting 15% optimization?**
   * *Fixed:* Removed broad `-keep` rules on Compose/Coroutines and enabled R8 Full Mode with package repackaging.
