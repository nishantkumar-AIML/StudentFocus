# 🎓 Student Focus — AI-Powered Academic & Productivity Ecosystem

[![Android Version](https://img.shields.io/badge/Android-10.0%2B%20%28API%2029%2B%29-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![R8 Full Mode](https://img.shields.io/badge/Optimization-R8%20Full%20Mode-blue)](https://developer.android.com/topic/performance/app-optimization)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Student Focus** is a modern, privacy-first Android application and desktop productivity companion designed specifically for students and self-learners. It combines on-device local AI intelligence, automated routine auditing, smart class timetable synchronization, deep focus timers, and gamification to eliminate digital distractions and boost academic achievement.

---

## 🌟 Key Features

### 1. ⏳ Smart Time Waste Tracker & Auto-Pause
* **Background Waste Tracking:** Continuously measures non-productive screen and idle time when no study session is active.
* **Intelligent Timetable Auto-Pause:** When scheduled college classes or study periods begin, the Time Waste Tracker automatically pauses without creating artificial study sessions.

### 2. 📋 Dynamic Timetable & Pre-Class Alerts
* **5-Minute Pre-Class Notifications:** High-priority heads-up alerts ring 5 minutes before scheduled classes with room/location details.
* **One-Tap Class Link Launcher:** Embed Zoom, Google Meet, Microsoft Teams, or YouTube lecture links and join instantly from the notification bar or timetable card.
* **Independent Slot Controls:** Customize whether each slot auto-pauses waste time or counts toward routine study capacity.

### 3. 🎯 Active Study Mission & Routine Budget Planner
* **Target-Based Study Goals:** Set multi-day targets (e.g., 20 Hours in 7 Days) and track real study progress.
* **24-Hour Routine Capacity Audit:** Automatically calculates your daily available study capacity from your 24-hour routine activities (Invested vs. Wasted time).
* **Live Task Focus Logging:** Seamlessly credits focused study sessions to your mission.

### 4. 🧠 On-Device AI Assistance (Google MediaPipe & Gemma LLM)
* **100% Offline & Private:** Powered by Gemma on-device inference via Google MediaPipe GenAI.
* **Instant Academic Help:** Ask questions, plan study roadmaps, and generate flashcard summaries without sending private data to cloud servers.

### 5. 🎮 Scholar Gamification Engine
* **XP, Levels, and Badges:** Earn experience points (XP) for completing tasks, finishing study intervals, and maintaining streaks.
* **Pet / Avatar Evolution:** Watch your scholar avatar evolve from an Egg 🥚 to a Hatching Scholar 🐣, Curious Chick 🐥, Soaring Eagle 🦅, and Wise Owl 🦉.

### 6. 📌 Kanban Board & Deep Focus Timer
* **Visual Task Management:** Organize assignments and syllabus chapters across *To-Do*, *In Progress*, and *Completed* columns.
* **Deep Focus Sessions:** Includes Pomodoro, Custom Stopwatch, and Task-Linked Timers with interactive notifications and Chronometer battery optimizations.
* **Gesture Controls:** Dismiss active task focus widgets on the dashboard with a smooth right-swipe gesture.

### 7. 🔒 Biometric Security & Data Privacy
* **Biometric Lock:** Secure your tasks, notes, and study logs using fingerprint or face authentication.
* **Local Offline Storage:** 100% offline Room database architecture with zero telemetry trackers.

### 8. 💻 Desktop Companion (C++17 & Dear ImGui)
* Includes a native, lightweight C++17 desktop client (`src/`, `include/`) with Dear ImGui and SQLite ORM for desktop workflows.

---

## 🏗️ Architecture & Tech Stack

* **Language:** Kotlin 2.0 & C++17 (Native desktop backend)
* **UI Framework:** Jetpack Compose with Material 3 Design
* **State & Concurrency:** Kotlin Coroutines, `StateFlow`, `SharedFlow`, `SupervisorJob`
* **Local Database:** Room Database v20 with indexing on timestamps and package names
* **AI Runtime:** Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai`)
* **Optimization:** ProGuard / R8 in **Full Mode** with aggressive interface merging and class repackaging
* **Lifecycle & Architecture:** Android Architecture Components (MVVM, Repository Pattern, WorkManager, Foreground Services)

---

## ⚙️ System Setup & Build Instructions

### Prerequisites
1. **JDK:** Java Development Kit 17 (or newer)
2. **Android SDK:** Platform 34+ (API level 29 minimum supported)
3. **Android Studio:** Ladybug / Meerkat (or newer) or Command-Line Tools
4. **Git:** Installed and configured
5. *(Optional for Desktop Build)*: CMake 3.14+, C++17 compatible compiler (`clang++` or `g++`), GLFW, OpenGL, SQLite3

---

### 🚀 Building the Android App

#### 1. Clone the Repository
```bash
git clone https://github.com/nishantkumar-AIML/StudentFocus.git
cd StudentFocus
```

#### 2. Create `local.properties`
Create a `local.properties` file in the root directory pointing to your Android SDK path:
```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
# On Windows: sdk.dir=C\:\\Users\\<your-username>\\AppData\\Local\\Android\\Sdk
# On Linux: sdk.dir=/home/<your-username>/Android/Sdk
```

#### 3. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 4. Install & Run on Connected Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 5. Build Release Android App Bundle (.aab) with R8 Optimizations
```bash
./gradlew bundleRelease
```
The generated bundle will be at:
```
app/build/outputs/bundle/release/app-release.aab
```

#### 6. Run Unit Tests
```bash
./gradlew test
```

---

### 🖥️ Building the Desktop Companion (Optional)

```bash
mkdir -p build && cd build
cmake ..
make -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
./student_focus
```

---

## 🤝 Contributing to Student Focus

We welcome contributions from the developer and student community! Whether you want to fix a bug, enhance the UI, add new AI prompts, or build new productivity tools, your help is appreciated.

### How to Contribute:

1. **Fork the Repository:** Click the **Fork** button at the top right of this repository.
2. **Clone your Fork:**
   ```bash
   git clone https://github.com/<your-username>/StudentFocus.git
   cd StudentFocus
   ```
3. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/amazing-new-feature
   ```
4. **Implement your Changes:** Follow clean architecture guidelines, preserve existing docstrings, and write unit tests where appropriate.
5. **Verify the Build:**
   ```bash
   ./gradlew test assembleDebug
   ```
6. **Commit & Push:**
   ```bash
   git commit -m "feat: Add [Feature Name] with [brief description]"
   git push origin feature/amazing-new-feature
   ```
7. **Open a Pull Request:** Go to the original repository and submit a Pull Request describing your changes.

### 💡 Ideas for New Features to Contribute:
* 🌐 Multi-language localization (Hindi, Spanish, German, French, etc.)
* ☁️ End-to-End Encrypted Cloud Sync (Google Drive / WebDAV)
* 📊 Advanced Weekly & Monthly PDF Performance Reports
* 🎙️ Voice-guided Flashcard Quiz Mode
* 📅 Google Calendar / Outlook two-way Timetable synchronization

---

## 📄 License
This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.

---

## 👨‍💻 Author & Maintainer
Created with ❤️ by **Nishant Kumar** ([@nishantkumar-AIML](https://github.com/nishantkumar-AIML))
