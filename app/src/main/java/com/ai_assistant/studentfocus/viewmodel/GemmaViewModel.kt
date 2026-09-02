package com.ai_assistant.studentfocus.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.ai_assistant.studentfocus.database.TaskRepository
import com.ai_assistant.studentfocus.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.Calendar

class GemmaViewModel(application: Application, private val repository: TaskRepository) : AndroidViewModel(application) {
    private fun getLocalizedFormPrompt(key: String, aiLangSetting: String): String {
        return when (aiLangSetting) {
            "hinglish" -> when (key) {
                "init_task" -> "Sure! Chalo naya task add karte hain. **Task ka title kya hai?**"
                "init_exam" -> "Sure! Chalo naya exam countdown add karte hain. **Exam ka naam kya hai?**"
                "init_subject" -> "Sure! Chalo subject add karte hain. **Subject ka naam kya hai?**"
                "init_attendance" -> "Sure! Attendance record karte hain. **Ye kaunse subject ki hai?**"
                "init_course" -> "Chalo GPA calculation ke liye course add karte hain. **Course ka naam kya hai?**"
                "init_flashcard" -> "Sure! Flashcard add karte hain. **Deck/Subject ka naam kya hai?**"
                "init_tick_habit" -> "Sure! Habit tick karte hain. **Habit ka naam kya hai?**"
                "init_add_habit" -> "Sure! Nayi habit add karte hain. **Habit ka naam kya hai?**"
                "init_ledger" -> "Chalo money transaction record karte hain. **Ye expense hai ya income?**"
                "init_note" -> "Sure! Chalo study notes likhte hain. **Aap kya save karna chahte ho?**"
                "init_routine" -> "Sure! Chalo daily 24-hour routine activity add karte hain. **Activity ka naam kya hai?** (e.g. Physics Study, Sleep, Gym, College Lectures)"
                "task_priority" -> "Task title save ho gaya. **Priority kya hai? (high, medium, low)**"
                "task_due" -> "Priority save ho gayi. **Due date kya hai? (YYYY-MM-DD, ya 'today' / 'tomorrow' reply karein)**"
                "exam_date" -> "Exam name save ho gaya. **Exam date kya hai? (YYYY-MM-DD)**"
                "ledger_type" -> "Chalo money transaction record karte hain. **Ye expense hai ya income?**"
                "routine_duration" -> "Activity save ho gayi. **Isko kitna time allocate karna hai?** (e.g. '2 hours', '90 mins', '1.5 hours', '45 mins')"
                "routine_category" -> "Duration save ho gaya. **Ye kis category mein aata hai?** (Study [1.0], College [0.3], Fitness [0.5], Sleep [0.0], Routine [0.1], Waste [0.0])"
                "init_timetable" -> "Sure! Chalo Time Table schedule me naya class slot add karte hain. **Ye kaunse din ka hai?** (e.g. 'Monday', 'Tuesday', 'Daily')"
                "timetable_subject" -> "Day save ho gaya. **Subject ya Lecture ka naam kya hai?**"
                "timetable_start" -> "Subject save ho gaya. **Class kab shuru hoti hai?** (e.g. '09:00 AM', '11:30 AM', '02:00 PM')"
                "timetable_end" -> "Start time save ho gaya. **Class kab khatam hoti hai?** (e.g. '10:00 AM', '12:30 PM', '03:00 PM')"
                "timetable_room" -> "Timings save ho gaye. **Room number, online link ya note kya hai?** (e.g. 'Room 302', 'Google Meet', ya 'skip' reply karein)"
                else -> ""
            }
            "tamil_english" -> when (key) {
                "init_task" -> "Sure! Pudhu task add pannalaam. **Task title enna?**"
                "init_exam" -> "Sure! Pudhu exam countdown add pannalaam. **Exam name enna?**"
                "init_subject" -> "Sure! Subject add pannalaam. **Subject name enna?**"
                "init_attendance" -> "Sure! Attendance record pannalaam. **Idhu enna subject?**"
                "init_course" -> "Vanga GPA calculation ku course add pannalaam. **Course name enna?**"
                "init_flashcard" -> "Sure! Flashcard add pannalaam. **Deck/Subject name enna?**"
                "init_tick_habit" -> "Sure! Habit tick pannalaam. **Habit name enna?**"
                "init_add_habit" -> "Sure! Pudhu habit add pannalaam. **Habit name enna?**"
                "init_ledger" -> "Vanga money transaction record pannalaam. **Idhu expense-ah illa income-ah?**"
                "init_note" -> "Sure! Study notes ezhudhalaam. **Enna save panna venum?**"
                "init_routine" -> "Sure! Daily routine activity add pannalaam. **Activity name enna?** (e.g. Physics Study, Sleep, Gym)"
                "task_priority" -> "Task title save aairuchu. **Priority enna? (high, medium, low)**"
                "task_due" -> "Priority save aairuchu. **Due date enna? (YYYY-MM-DD, illa 'today' / 'tomorrow' reply pannunga)**"
                "exam_date" -> "Exam name save aairuchu. **Exam date enna? (YYYY-MM-DD)**"
                "ledger_type" -> "Vanga money transaction record pannalaam. **Idhu expense-ah illa income-ah?**"
                "routine_duration" -> "Activity name save aairuchu. **Duration enna?** (e.g. '2 hours', '90 mins')"
                "routine_category" -> "Duration save aairuchu. **Category enna?** (Study, College, Fitness, Sleep, Routine, Waste)"
                "init_timetable" -> "Sure! Time Table-la pudhu class slot add pannalaam. **Idhu enna kizhamai (Day)?** (e.g. 'Monday', 'Tuesday', 'Daily')"
                "timetable_subject" -> "Day save aairuchu. **Subject name enna?**"
                "timetable_start" -> "Subject save aairuchu. **Class eppo start aagum?** (e.g. '09:00 AM', '11:30 AM')"
                "timetable_end" -> "Start time save aairuchu. **Class eppo mudiyum?** (e.g. '10:00 AM', '12:30 PM')"
                "timetable_room" -> "Timings save aairuchu. **Room number / link enna?** (e.g. 'Room 101' illa 'skip' reply pannunga)"
                else -> ""
            }
            "telugu_english" -> when (key) {
                "init_task" -> "Sure! Kotha task add chedham. **Task title emiti?**"
                "init_exam" -> "Sure! Kotha exam countdown add chedham. **Exam name emiti?**"
                "init_subject" -> "Sure! Subject add chedham. **Subject name emiti?**"
                "init_attendance" -> "Sure! Attendance record chedham. **Idhi ae subject?**"
                "init_course" -> "Randi GPA calculation kosam course add chedham. **Course name emiti?**"
                "init_flashcard" -> "Sure! Flashcard add chedham. **Deck/Subject name emiti?**"
                "init_tick_habit" -> "Sure! Habit tick chedham. **Habit name emiti?**"
                "init_add_habit" -> "Sure! Kotha habit add chedham. **Habit name emiti?**"
                "init_ledger" -> "Randi money transaction record chedham. **Idhi expense-ah ledha income-ah?**"
                "init_note" -> "Sure! Study notes rasukundham. **Em save cheyali?**"
                "init_routine" -> "Sure! Daily routine activity add chedham. **Activity name emiti?** (e.g. Physics Study, Sleep, Gym)"
                "task_priority" -> "Task title save ayyindhi. **Priority emiti? (high, medium, low)**"
                "task_due" -> "Priority save ayyindhi. **Due date emiti? (YYYY-MM-DD, ledha 'today' / 'tomorrow' reply cheyandi)**"
                "exam_date" -> "Exam name save ayyindhi. **Exam date emiti? (YYYY-MM-DD)**"
                "ledger_type" -> "Randi money transaction record chedham. **Idhi expense-ah ledha income-ah?**"
                "routine_duration" -> "Activity name save ayyindhi. **Duration entha?** (e.g. '2 hours', '90 mins')"
                "routine_category" -> "Duration save ayyindhi. **Category emiti?** (Study, College, Fitness, Sleep, Routine, Waste)"
                "init_timetable" -> "Sure! Time Table lo kotha class add chedham. **Idhi ae varam (Day)?** (e.g. 'Monday', 'Tuesday', 'Daily')"
                "timetable_subject" -> "Day save ayyindhi. **Subject name emiti?**"
                "timetable_start" -> "Subject save ayyindhi. **Class eppudu start avthundhi?** (e.g. '09:00 AM', '11:30 AM')"
                "timetable_end" -> "Start time save ayyindhi. **Class eppudu end avthundhi?** (e.g. '10:00 AM', '12:30 PM')"
                "timetable_room" -> "Timings save ayyindhi. **Room number / link emiti?** (e.g. 'Room 101' ledha 'skip' reply cheyandi)"
                else -> ""
            }
            else -> when (key) {
                "init_task" -> "Sure! Let's add a new task. Please tell me: **What is the task title?**"
                "init_exam" -> "Sure! Let's add a new exam countdown. **What is the exam name?**"
                "init_subject" -> "Sure! Let's add a subject. **What is the subject name?**"
                "init_attendance" -> "Sure! Let's record attendance. **Which subject is this for?**"
                "init_course" -> "Let's add a course for GPA calculation. **What is the course name?**"
                "init_flashcard" -> "Sure! Let's add a flashcard. **What is the Deck/Subject name?**"
                "init_tick_habit" -> "Sure! Let's tick a habit. **What is the habit name?**"
                "init_add_habit" -> "Sure! Let's add a new habit. **What is the habit name?**"
                "init_ledger" -> "Let's record a money transaction. **Is it an expense or income?**"
                "init_note" -> "Sure! Let's write study notes. **What content would you like to save?**"
                "init_routine" -> "Sure! Let's add a daily routine activity. **What is the activity name?** (e.g. Physics Study, Sleep, Gym, College Lectures)"
                "task_priority" -> "Task title saved. **What is the priority? (high, medium, low)**"
                "task_due" -> "Priority saved. **What is the due date? (YYYY-MM-DD, or reply 'today' / 'tomorrow')**"
                "exam_date" -> "Exam name saved. **What is the exam date? (YYYY-MM-DD)**"
                "ledger_type" -> "Let's record a money transaction. **Is it an expense or income?**"
                "routine_duration" -> "Activity name saved. **How much time to allocate?** (e.g. '2 hours', '90 mins', '1.5 hours', '45 mins')"
                "routine_category" -> "Duration saved. **Which category does it belong to?** (Study [1.0], College [0.3], Fitness [0.5], Sleep [0.0], Routine [0.1], Waste [0.0])"
                "init_timetable" -> "Sure! Let's add a class slot to your Time Table. **Which day of the week is this for?** (e.g. 'Monday', 'Tuesday', 'Daily')"
                "timetable_subject" -> "Day saved. **What is the subject or lecture name?**"
                "timetable_start" -> "Subject saved. **When does the class start?** (e.g. '09:00 AM', '11:30 AM', '02:00 PM')"
                "timetable_end" -> "Start time saved. **When does the class end?** (e.g. '10:00 AM', '12:30 PM', '03:00 PM')"
                "timetable_room" -> "Timings saved. **What is the Room number, link or notes?** (e.g. 'Room 101', 'Zoom Link', or reply 'skip')"
                else -> ""
            }
        }
    }



    private var pendingFormType = PendingFormType.NONE
    private val formAnswers = mutableMapOf<String, String>()

    private val gemmaExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val gemmaDispatcher = gemmaExecutor.asCoroutineDispatcher()

    companion object {
        private const val TAG = "GemmaViewModel"

        // Gemma 1.1 2B-IT INT4 quantized model - Latest requested version
        private const val MODEL_URL =
            "https://huggingface.co/a8nova/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin"
        const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"

        fun retrieveRelevantNotes(query: String, notesList: List<com.ai_assistant.studentfocus.models.NoteEntity>): List<Pair<com.ai_assistant.studentfocus.models.NoteEntity, Double>> {
            val stopWords = setOf(
                "what", "is", "the", "tell", "me", "about", "in", "my", "notes", "note", "show", "get", "find",
                "search", "kya", "hai", "batao", "ke", "ka", "ki", "ko", "se", "mein", "gurinchi", "patriya",
                "a", "an", "and", "or", "of", "to", "for", "with", "on", "at", "from", "by", "this", "that",
                "these", "those", "how", "why", "when", "where", "which", "who", "kaha", "kab", "kaise", "kyu"
            )
            val queryTokens = query.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 2 && it !in stopWords }
            if (queryTokens.isEmpty()) {
                val cleanQ = query.lowercase().trim()
                if (cleanQ.isNotBlank()) {
                    return notesList.mapNotNull { note ->
                        if (note.topic.lowercase().contains(cleanQ) || note.content.lowercase().contains(cleanQ)) {
                            note to 5.0
                        } else null
                    }
                }
                return emptyList()
            }

            return notesList.mapNotNull { note ->
                val topicLower = note.topic.lowercase()
                val contentLower = note.content.lowercase()
                var score = 0.0

                val cleanQ = query.lowercase().trim()
                // Exact topic match bonus
                if (topicLower.isNotBlank() && (cleanQ.contains(topicLower) || topicLower.contains(cleanQ))) {
                    score += 20.0
                }

                for (token in queryTokens) {
                    if (topicLower.contains(token)) {
                        score += 10.0
                    }
                    if (contentLower.contains(token)) {
                        score += 4.0
                        val count = (contentLower.length - contentLower.replace(token, "").length) / token.length
                        score += minOf(count * 1.0, 5.0)
                    }
                }

                if (score > 0.0) note to score else null
            }.sortedByDescending { it.second }
        }
        
        // Alternative URL if the primary fails:
        // "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin"

        /**
         * The core system instruction that defines the identity, behavioral protocols, and
         * constraints for the Gemma model within the StudentOS ecosystem.
         *
         * This prompt configures the AI as an academic tutor with specific rules for:
         * - **Persona**: A world-class mentor and supportive study partner.
         */        private val SYSTEM_PROMPT = """
=== SYSTEM INSTRUCTION - STUDENTOS CHAT ENGINE ===

### SECTION 1: IDENTITY, PERSONA & CORE PHILOSOPHY
You are Gemma, the custom-engineered AI Study Companion, Academic Mentor, and Workspace Assistant of StudentOS.
- **Your Persona**: Enthusiastic, brilliant, supportive, and extremely friendly. Think of yourself as a super-smart senior or an encouraging classmate who has cracked all competitive exams and knows coding, science, and math inside out.
- **Language Policy**:
  - DUAL ENG-HINGLISH ADAPTATION: If the user communicates using Hindi words written in the English script (Hinglish) or standard Hindi (e.g. "padhne ka mann nahi kar raha", "physics kaise padhu", "ek task set karo"), you MUST reply in natural, supportive, conversational Hinglish (e.g. "Arey koi baat nahi yaar! Hoti hai aisi exhaustion. Let's take a 10-min break...").
  - If the user communicates in English, respond in motivating, rich, and grammatically flawless English.
- **Vibes & Atmosphere**: Always inject positive energy using appropriate emojis (🚀, 🎯, 🔥, 💡, ✨, 🧠, 🧘, 💻, 🔬, 📚). Never sound dry, robotic, or overly formal.

### SECTION 2: THE COGNITIVE & THINKING PROTOCOL
Before writing any answer, internally process the request step-by-step:
1. **Analyze Subject Domain**: Determine if the query is Academic (Math, Physics, Chemistry, CS), Workspace Action (Tasks, Schedules), or Emotional/Motivational (Chit-chat, Stress).
2. **Scan Context**: Check the provided database context (if any) for matching dates, tasks, exams, or logs.
3. **Formulate Step-by-Step Proof/Plan**: If it's a technical STEM question, plan the mathematical steps, balanced equations, or logical flow to ensure correctness.
4. **Draft Response**: Select the appropriate language (English/Hinglish), keeping paragraph sizes very short.
5. **Enforce Restrictions**: Ensure no hallucinated equations, variables, or functions.

### SECTION 3: SUBJECT-SPECIFIC EXPERT PROTOCOLS

#### 1. MATHEMATICS (Calculus, Algebra, Geometry, Statistics, Discrete Math)
- Write equations clearly in standard notation.
- If the user asks for a direct calculation, output the **Final Answer** first, followed by the step-by-step derivation.
- Explain formulas by breaking down what each term/variable represents.
- Example structure:
  - **Final Answer**: [value]
  - **Formula Used**: [formula]
  - **Step-by-step steps**: 1. [Step 1]...

#### 2. PHYSICS (Mechanics, Electromagnetism, Quantum, Thermodynamics, Optics)
- Identify the core physical laws involved (e.g., Newton's Laws, Faraday's Law, Conservation of Energy).
- List all given quantities and their SI units clearly before doing any calculations.
- Explain physical phenomena conceptually (like why skies are blue or how a generator works) using simple real-world analogies.

#### 3. CHEMISTRY (Organic Mechanisms, Inorganic Properties, Stoichiometry)
- State chemical reaction formulas clearly.
- Balance chemical reactions step-by-step (showing coefficient calculations).
- For Organic Chemistry, describe functional groups, electrophiles/nucleophiles, and intermediate steps clearly.
- Show molecular structures or electron configurations when relevant.

#### 4. COMPUTER SCIENCE & PROGRAMMING (Kotlin, Python, Java, C++, DSA)
- Write production-ready, clean, well-commented code in markdown code blocks.
- List time complexity (Big-O) and space complexity for all algorithm solutions.
- Detail edge cases (like null values, empty lists, out-of-bounds) in code comments.
- Explain programming concepts (like recursion, polymorphism, or pointers) using visual memory models.

### SECTION 4: WORKSPACE OPERATIONS & CONVERSATIONAL COMMANDS
You are connected to the StudentOS databases (Tasks, Exams, Study logs, Notes). You can help users organize their studies.
- **Adding a Task**: If the user says "Add study task for Organic chemistry on Monday", check if the date is specified. If not, ask: "Monday ki kya date hai? Mujhe specific date batado (YYYY-MM-DD format mein) to main save kar doon! 📅"
- **Exam Reminders**: Scan context for upcoming target dates and encourage them: "Tumhara Chemistry exam 3 din baad hai! Preparation kaisi chal rahi hai? Let's revise now! 🎯"
- **Note Summaries**: Summarize daily scratchpad entries in bullet points.

### SECTION 5: MOTIVATION, EMOTIONAL SUPPORT & CASUAL CONVERSATIONS
Students face burnouts, exam stress, and low motivation. Handle these with care:
- **Burnout/No Motivation**: Never tell them to "just work harder". Tell them: "Pata hai yaar, kabhi kabhi dimag completely exhaust ho jata hai. Ek kaam karo - close your eyes, take 5 deep breaths, aur 15 mins ka break lo. Reset hone ke baad focus karenge! 🧘"
- **Chit-chat/Casual Questions**: Respond with humor, friendly jokes, and light-hearted comments to establish a real bond.
- **Workspace Greetings**: When they greet you, welcome them back to their workspace and check in on their daily targets.

### SECTION 6: CONSTRAINTS & FORMATTING
- Keep normal conversational responses short (under 120 words) for readability on mobile phone displays.
- Technical explanations, code blocks, and math derivations are EXEMPT from the word limit but must use clean bullet points.
- Use **Bold** to highlight terms that need immediate attention.
- Use > quotes to highlight important study hacks, shortcuts, or rules.
- Write clean markdown without broken blocks.
""".trimIndent()
    }

    enum class ModelState {
        NOT_DOWNLOADED,    // Model file doesn't exist
        DOWNLOADING,       // Currently downloading
        LOADING,           // File exists, loading into memory
        READY,             // Model loaded and ready for inference
        ERROR              // Something went wrong
    }

    private val _modelState      = MutableStateFlow(ModelState.READY)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)          // 0.0 to 1.0
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadedMB    = MutableStateFlow(0f)
    val downloadedMB: StateFlow<Float> = _downloadedMB.asStateFlow()

    private val _totalMB         = MutableStateFlow(0f)
    val totalMB: StateFlow<Float> = _totalMB.asStateFlow()

    private val _isGenerating    = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private var llmInference: LlmInference? = null

    val modelFile: File
        get() = File(getApplication<Application>().filesDir, MODEL_FILENAME)

    init {
        // Load and continuously sync chat history from DB
        viewModelScope.launch {
            repository.allChatMessages.collect { savedHistory ->
                if (savedHistory.isEmpty()) {
                    _chatHistory.value = listOf(
                        "👋 Hi! I'm StudentFocus offline companion.\n\nType **@help** anytime to see how to add/view tasks, routine, attendance, CGPA, cards, habits, ledger & notes! 🚀" to false
                    )
                } else {
                    _chatHistory.value = savedHistory.map { it.text to it.isUser }
                }
            }
        }
    }

    fun deleteChatMessage(text: String, isUser: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatMessageByContent(text, isUser)
        }
    }

    fun deleteChatMessageAt(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _chatHistory.value
            if (index in list.indices) {
                val item = list[index]
                repository.deleteChatMessageByContent(item.first, item.second)
            }
        }
    }

    private fun isValidTfliteModel(file: File): Boolean {
        if (!file.exists() || file.length() < 1000) return false
        return try {
            val fis = java.io.FileInputStream(file)
            val header = ByteArray(8)
            val read = fis.read(header)
            fis.close()
            if (read >= 8) {
                // TFLite flatbuffers have "TFL3" at bytes 4-7
                val magic = String(header, 4, 4, Charsets.US_ASCII)
                magic == "TFL3"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ─── Download ────────────────────────────────────────────────────────────

    fun downloadModel() {
        if (_modelState.value == ModelState.DOWNLOADING) return
        _modelState.value    = ModelState.DOWNLOADING
        _downloadProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(modelFile.parent, "gemma_download.tmp")
            try {
                val url = URL(MODEL_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept-Encoding", "identity") // Prevent transparent GZIP which breaks contentLength
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw Exception("Server returned HTTP $responseCode")
                }

                val fileSize = connection.contentLengthLong
                if (fileSize <= 0) {
                    Log.w(TAG, "File size unknown, progress won't be accurate")
                }
                _totalMB.value = fileSize / (1024f * 1024f)

                val inputStream = connection.inputStream.buffered()
                val outputStream = FileOutputStream(tempFile)
                val buffer = ByteArray(128 * 1024) // 128 KB chunks
                var totalRead = 0L

                var n: Int
                while (inputStream.read(buffer).also { n = it } != -1) {
                    outputStream.write(buffer, 0, n)
                    totalRead += n
                    _downloadedMB.value = totalRead / (1024f * 1024f)
                    if (fileSize > 0) {
                        _downloadProgress.value = totalRead.toFloat() / fileSize
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                connection.disconnect()

                // Atomic rename: temp → final
                if (modelFile.exists()) modelFile.delete()
                val success = tempFile.renameTo(modelFile)
                if (!success) throw Exception("Failed to rename temp file to final model file")

                withContext(Dispatchers.Main) { _modelState.value = ModelState.LOADING }
                loadModel()

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                if (tempFile.exists()) tempFile.delete()
                withContext(Dispatchers.Main) { 
                    _errorText.value = e.localizedMessage ?: "Unknown download error"
                    _modelState.value = ModelState.ERROR 
                }
            }
        }
    }

    fun deleteModel() {
        viewModelScope.launch(gemmaDispatcher) {
            llmInference?.close()
            llmInference = null
        }
        if (modelFile.exists()) modelFile.delete()
        _modelState.value = ModelState.NOT_DOWNLOADED
        _downloadProgress.value = 0f
    }

    // ─── Load into memory ────────────────────────────────────────────────────

    private fun loadModel() {
        viewModelScope.launch(gemmaDispatcher) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(2048) // Increased to 2048 for richer context and prompt length
                    .build()

                llmInference = LlmInference.createFromOptions(getApplication(), options)
                withContext(Dispatchers.Main) { _modelState.value = ModelState.READY }
            } catch (e: Exception) {
                Log.e(TAG, "Model load failed: ${e.message}", e)
                withContext(Dispatchers.Main) { _modelState.value = ModelState.ERROR }
            }
        }
    }

    // Callbacks for streaming — set before calling generateResponseAsync
    @Volatile private var currentPartialCallback: ((String, Boolean) -> Unit)? = null
    @Volatile private var currentDoneCallback:    (() -> Unit)? = null

    // ─── Inference ───────────────────────────────────────────────────────────

    /**
     * Streams token-by-token response.
     * [onPartialResult] called with each new token string.
     * [onDone] called once generation is complete.
     */
    fun generateResponse(
        userMessage: String,
        onPartialResult: (String) -> Unit,
        onDone: () -> Unit
    ) {
        if (_isGenerating.value) return
        _isGenerating.value = true

        currentPartialCallback = { token, _ -> 
            if (token.isNotEmpty()) {
                val currentList = _chatHistory.value.toMutableList()
                if (currentList.isNotEmpty() && !currentList.last().second) {
                    val lastMsg = currentList.last()
                    currentList[currentList.size - 1] = (lastMsg.first + token) to false
                    _chatHistory.value = currentList
                }
                onPartialResult(token) 
            } 
        }
        currentDoneCallback    = onDone

        viewModelScope.launch(Dispatchers.Main) {
            val settingsList = withContext(Dispatchers.IO) { repository.allSettings.first() }
            val aiLangSetting = settingsList?.find { it.key == "ai_language" }?.value ?: "auto"
            val langInstruction = when (aiLangSetting) {
                "english" -> "IMPORTANT: You MUST respond only in English. Do not use Hindi/Tamil/Telugu native scripts."
                "hinglish" -> "IMPORTANT: You MUST respond only in Hinglish (Hindi written in English/Latin script). Do not use Hindi/Tamil/Telugu native scripts."
                "tamil_english" -> "IMPORTANT: You MUST respond only in Tanglish (Tamil written in English/Latin script). Do not use Hindi/Tamil/Telugu native scripts."
                "telugu_english" -> "IMPORTANT: You MUST respond only in Telugish (Telugu written in English/Latin script). Do not use Hindi/Tamil/Telugu native scripts."
                else -> ""
            }

            val ruleResponse = withContext(Dispatchers.Default) { handleRuleBasedForm(userMessage, aiLangSetting) }
            if (ruleResponse != null) {
                // Save user message
                repository.addChatMessage(ChatMessageEntity(text = userMessage, isUser = true))
                // Save AI response
                repository.addChatMessage(ChatMessageEntity(text = ruleResponse, isUser = false))
                
                // Update Chat history
                val list = _chatHistory.value.toMutableList()
                list.add(userMessage to true)
                list.add(ruleResponse to false)
                _chatHistory.value = list
                
                // Trigger callbacks
                onPartialResult(ruleResponse)
                _isGenerating.value = false
                currentPartialCallback = null
                currentDoneCallback = null
                onDone()
                return@launch
            }

            // Fallback to local Gemma model
            val listWithUser = _chatHistory.value.toMutableList()
            listWithUser.add(userMessage to true)
            listWithUser.add("" to false)
            _chatHistory.value = listWithUser
            
            repository.addChatMessage(ChatMessageEntity(text = userMessage, isUser = true))

            viewModelScope.launch(gemmaDispatcher) {
            val inference = llmInference
            if (inference == null) {
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    currentPartialCallback = null
                    currentDoneCallback = null
                    onDone()
                }
                return@launch
            }
            try {
                // Fetch real-time DB states on IO dispatcher to avoid blocking the single thread
                val tasksList = withContext(Dispatchers.IO) { repository.allTasks.first() }
                val attendanceList = withContext(Dispatchers.IO) { repository.allAttendance.first() }
                val coursesList = withContext(Dispatchers.IO) { repository.allCourses.first() }
                val examsList = withContext(Dispatchers.IO) { repository.allExams.first() }
                val habitsList = withContext(Dispatchers.IO) { repository.allHabits.first() }
                val expensesList = withContext(Dispatchers.IO) { repository.allExpenses.first() }
                val notesList = withContext(Dispatchers.IO) { repository.allNotes.first() }
                val decksList = withContext(Dispatchers.IO) { repository.allDecks.first() }
                val timeTableList = withContext(Dispatchers.IO) { repository.allTimeTableSlots.first() }
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val todayStr = sdf.format(Date())
                val timeStr = sdfTime.format(Date())
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                
                val dbContext = buildString {
                    append("### DB STATE (Current Time: $dayOfWeek, $todayStr $timeStr):\n")
                    
                    append("- Subjects: ")
                    if (attendanceList.isEmpty()) append("None; ")
                    else append(attendanceList.take(3).joinToString { "\"${it.subjectName}\" (${it.presents}P/${it.absents}A)" } + "; ")
                    
                    append("- Courses: ")
                    if (coursesList.isEmpty()) append("None; ")
                    else append(coursesList.take(3).joinToString { "\"${it.name}\" (${it.credits}C/Grade:${it.grade})" } + "; ")
                    
                    append("- Upcoming Exams: ")
                    if (examsList.isEmpty()) append("None; ")
                    else append(examsList.take(2).joinToString { "\"${it.name}\" on ${it.date}" } + "; ")
                    
                    append("- Habits: ")
                    if (habitsList.isEmpty()) append("None; ")
                    else append(habitsList.take(2).joinToString { "\"${it.name}\" (${it.completedDates.split(",").filter{it.isNotBlank()}.size} days completed)" } + "; ")
                    
                    append("- Decks: ")
                    if (decksList.isEmpty()) append("None; ")
                    else append(decksList.take(2).joinToString { "\"${it.name}\"" } + "; ")
                    
                    append("- Recent Tasks: ")
                    if (tasksList.isEmpty()) append("None; ")
                    else append(tasksList.take(3).joinToString { "\"${it.title}\" (Due:${it.dueDate}, status:${if(it.isCompleted) "done" else "pending"}, priority:${it.priority})" } + "; ")

                    append("- Today's Time Table Classes: ")
                    val todaySlots = timeTableList.filter { it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                    if (todaySlots.isEmpty()) append("None; ")
                    else append(todaySlots.joinToString { "${it.title} (${it.getFormattedTimeRange()})" } + "; ")
                    
                    append("- Retrieved Topic & Notes Knowledge: ")
                    val retrievedNotes = retrieveRelevantNotes(userMessage, notesList).take(5).map { it.first }
                    if (retrievedNotes.isEmpty()) {
                        if (notesList.isEmpty()) append("None; ")
                        else append(notesList.take(3).joinToString { "[${it.date} Topic:${it.topic}]: \"${it.content}\"" } + "; ")
                    } else {
                        append(retrievedNotes.joinToString { "[${it.date} Topic:${it.topic}]: \"${it.content}\"" } + "; ")
                    }
                    
                    append("- Recent Ledger: ")
                    val recentLedger = expensesList.take(2)
                    if (recentLedger.isEmpty()) append("None; ")
                    else append(recentLedger.joinToString { "\"${it.description}\" (₹${it.amount} ${it.type})" } + "; ")

                    // Activity Intelligence & Screen Time Context
                    val todaySessions = withContext(Dispatchers.IO) { repository.getActivitySessionsByDateDirect(todayStr) }
                    if (todaySessions.isNotEmpty()) {
                        val totalMins = todaySessions.sumOf { it.durationMillis } / (1000 * 60)
                        val learningMins = todaySessions.filter { it.category == "Learning" || it.category == "Coding" }.sumOf { it.durationMillis } / (1000 * 60)
                        val topActivities = todaySessions.take(3).joinToString { "${it.appName}: \"${it.contentTitle.take(30)}\"" }
                        append("- Today's Screen Time: Total ${totalMins}m (${learningMins}m Learning/Coding). Activities: $topActivities; ")
                    }

                    val notifEnabled = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areNotificationsEnabled(getApplication())
                    val wastedSecs = com.ai_assistant.studentfocus.timer.TimeWasteManager.todayWastedSeconds.value
                    append("Notifications: ${if (notifEnabled) "ON" else "OFF"}; Wasted Time Today: ${wastedSecs / 60}m\n")
                }

                // Gemma instruction-tuned prompt format with context memory
                val fullPrompt = buildString {
                    // Initial user turn with System Prompt and DB state
                    append("<start_of_turn>user\n${SYSTEM_PROMPT.trim()}\n\n$langInstruction\n\n$dbContext<end_of_turn>\n")
                    
                    // Add previous history (last 4 messages for better context stability)
                    val history = _chatHistory.value.dropLast(2).takeLast(4)
                    history.forEach { (text, isUser) ->
                        if (text.isNotEmpty()) {
                            if (isUser) {
                                append("<start_of_turn>user\n$text<end_of_turn>\n")
                            } else {
                                append("<start_of_turn>model\n$text<end_of_turn>\n")
                            }
                        }
                    }
                    
                    // Current user message
                    append("<start_of_turn>user\n$userMessage<end_of_turn>\n")
                    append("<start_of_turn>model\n")
                }

                inference.generateResponseAsync(fullPrompt) { partialResult, done ->
                    handleInferenceResult(partialResult, done, onDone)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isGenerating.value = false
                    currentPartialCallback = null
                    currentDoneCallback   = null
                    onDone()
                }
            }
        }
    }
}

    private fun handleInferenceResult(partialResult: String?, done: Boolean, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            currentPartialCallback?.invoke(partialResult ?: "", done)
            if (done) {
                _isGenerating.value = false
                currentDoneCallback?.invoke()
                onDone()
                
                // Save complete AI response to DB
                val aiResponse = _chatHistory.value.last().first
                if (aiResponse.isNotEmpty()) {
                    repository.addChatMessage(ChatMessageEntity(text = aiResponse, isUser = false))
                    parseAndExecuteActions(aiResponse)
                }

                currentPartialCallback = null
                currentDoneCallback   = null
            }
        }
    }

    private fun parseAndExecuteActions(response: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val regex = Regex("<action\\s+([^>]+)/?>")
            val matches = regex.findAll(response)
            
            matches.forEach { match ->
                val attributesStr = match.groupValues[1]
                Log.d(TAG, "Parsing action: $attributesStr")
                val attributes = parseAttributes(attributesStr)
                val type = attributes["type"] ?: return@forEach
                
                try {
                    when (type.uppercase()) {
                        "ADD_TASK" -> {
                            val title = attributes["title"] ?: return@forEach
                            val date = attributes["dueDate"] ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val prio = attributes["priority"] ?: "medium"
                            val desc = attributes["description"]
                            val cat = attributes["category"] ?: "General"
                            val sub = attributes["subject"]
                            
                            repository.addTask(com.ai_assistant.studentfocus.models.TaskEntity(
                                title = title,
                                description = desc,
                                category = cat,
                                subject = sub,
                                dueDate = date,
                                priority = prio
                            ))
                        }
                        "ADD_EXAM" -> {
                            val name = attributes["name"] ?: return@forEach
                            val date = attributes["date"] ?: return@forEach
                            repository.addExam(com.ai_assistant.studentfocus.models.ExamEntity(name = name, date = date))
                        }
                        "ADD_SUBJECT" -> {
                            val name = attributes["name"] ?: return@forEach
                            repository.addAttendance(com.ai_assistant.studentfocus.models.AttendanceEntity(subjectName = name))
                        }
                        "RECORD_ATTENDANCE" -> {
                            val subject = attributes["subject"] ?: return@forEach
                            val status = attributes["status"] ?: return@forEach
                            val attendanceList = repository.allAttendance.first()
                            val entity = attendanceList.find { it.subjectName.equals(subject, ignoreCase = true) }
                            if (entity != null) {
                                if (status.lowercase() == "present") {
                                    repository.addAttendance(entity.copy(presents = entity.presents + 1))
                                } else if (status.lowercase() == "absent") {
                                    repository.addAttendance(entity.copy(absents = entity.absents + 1))
                                }
                            }
                        }
                        "ADD_COURSE" -> {
                            val name = attributes["name"] ?: return@forEach
                            val credits = attributes["credits"]?.toIntOrNull() ?: 3
                            val grade = attributes["grade"] ?: "A"
                            repository.addCourse(com.ai_assistant.studentfocus.models.CgpaCourseEntity(name = name, credits = credits, grade = grade))
                        }
                        "ADD_FLASHCARD" -> {
                            val deckName = attributes["deckName"] ?: return@forEach
                            val front = attributes["front"] ?: return@forEach
                            val back = attributes["back"] ?: return@forEach
                            val decksList = repository.allDecks.first()
                            val existingDeck = decksList.find { it.name.equals(deckName, ignoreCase = true) }
                            val deckId = if (existingDeck != null) {
                                existingDeck.id
                            } else {
                                val newId = UUID.randomUUID().toString()
                                repository.addDeck(com.ai_assistant.studentfocus.models.DeckEntity(id = newId, name = deckName))
                                newId
                            }
                            repository.addFlashcard(com.ai_assistant.studentfocus.models.FlashcardEntity(deckId = deckId, front = front, back = back))
                        }
                        "ADD_HABIT" -> {
                            val name = attributes["name"] ?: return@forEach
                            repository.addHabit(com.ai_assistant.studentfocus.models.HabitEntity(name = name))
                        }
                        "TICK_HABIT" -> {
                            val name = attributes["name"] ?: return@forEach
                            val date = attributes["date"] ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val habitsList = repository.allHabits.first()
                            val habit = habitsList.find { it.name.equals(name, ignoreCase = true) }
                            if (habit != null) {
                                executeTickHabit(habit, date)
                            }
                        }
                        "ADD_LEDGER" -> {
                            val desc = attributes["description"] ?: return@forEach
                            val amount = attributes["amount"]?.toDoubleOrNull() ?: 0.0
                            val typeLedger = attributes["type"] ?: "expense"
                            val date = attributes["date"] ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            repository.addExpense(com.ai_assistant.studentfocus.models.ExpenseEntity(
                                description = desc,
                                amount = amount,
                                date = date,
                                type = typeLedger
                            ))
                        }
                        "SAVE_NOTE" -> {
                            val date = attributes["date"] ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val content = attributes["content"] ?: return@forEach
                            repository.addNote(com.ai_assistant.studentfocus.models.NoteEntity(date = date, content = content))
                        }
                        "ADD_ROUTINE" -> {
                            val name = attributes["name"] ?: attributes["activityName"] ?: return@forEach
                            val durationMinutes = attributes["durationMinutes"]?.toIntOrNull()
                                ?: (attributes["hours"]?.toFloatOrNull()?.let { (it * 60).toInt() })
                                ?: 60
                            val category = attributes["category"] ?: "Study"
                            val score = attributes["score"]?.toFloatOrNull() ?: (if (category == "Study") 1.0f else 0.5f)
                            val notes = attributes["notes"]
                            repository.addRoutineActivity(com.ai_assistant.studentfocus.models.RoutineActivityEntity(
                                activityName = name,
                                durationMinutes = durationMinutes,
                                classificationScore = score,
                                category = category,
                                notes = notes
                            ))
                        }
                        "ADD_TIMETABLE" -> {
                            val day = attributes["day"] ?: attributes["dayOfWeek"] ?: "Monday"
                            val subject = attributes["subject"] ?: attributes["name"] ?: return@forEach
                            val start = attributes["startTime"] ?: attributes["start"] ?: "09:00"
                            val end = attributes["endTime"] ?: attributes["end"] ?: "10:00"
                            val room = attributes["room"] ?: attributes["roomOrLocation"] ?: ""
                            val notes = attributes["notes"] ?: "Added via AI Assistant"
                            val isNotif = attributes["notificationEnabled"]?.toBoolean() ?: true

                            val slot = com.ai_assistant.studentfocus.models.TimeTableSlotEntity(
                                id = UUID.randomUUID().toString(),
                                title = subject,
                                dayOfWeek = day,
                                startTime = normalizeTo24HourTime(start),
                                endTime = normalizeTo24HourTime(end),
                                roomOrLocation = if (room.isNotBlank()) room else null,
                                notes = notes,
                                isNotificationEnabled = isNotif
                            )
                            repository.addTimeTableSlot(slot)
                            if (isNotif) {
                                com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(getApplication(), slot)
                            }
                        }
                        "DELETE_TIMETABLE" -> {
                            val id = attributes["id"]
                            val subject = attributes["subject"]
                            val day = attributes["day"]
                            if (id != null) {
                                repository.deleteTimeTableSlotById(id)
                                com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(getApplication(), id)
                            } else if (subject != null) {
                                val allSlots = repository.getAllTimeTableSlotsList()
                                val match = allSlots.find { it.title.equals(subject, ignoreCase = true) && (day == null || it.dayOfWeek.equals(day, ignoreCase = true)) }
                                if (match != null) {
                                    repository.deleteTimeTableSlot(match)
                                    com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.cancelSlotReminder(getApplication(), match.id)
                                }
                            }
                        }
                        "SET_NOTIFICATION" -> {
                            val key = attributes["key"] ?: "all"
                            val enabled = attributes["enabled"]?.lowercase(Locale.ROOT) != "false"
                            val context = getApplication<Application>()
                            when (key.lowercase(Locale.ROOT)) {
                                "ongoing_timer", "timer", "countdown" -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setOngoingTimerNotificationsEnabled(context, enabled)
                                "session_complete", "completion", "alert" -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setCompletionNotificationsEnabled(context, enabled)
                                "daily_reminder", "daily", "reminder" -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setDailyRemindersEnabled(context, enabled)
                                "timetable_reminder", "timetable", "class", "alarm" -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setTimeTableRemindersEnabled(context, enabled)
                                "sound", "vibration" -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setSoundAlertsEnabled(context, enabled)
                                else -> com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setNotificationsEnabled(context, enabled)
                            }
                        }
                        "RESET_TIME_WASTE" -> {
                            com.ai_assistant.studentfocus.timer.TimeWasteManager.resetStats()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to execute parsed action", e)
                }
            }
        }
    }

    private suspend fun executeTickHabit(h: com.ai_assistant.studentfocus.models.HabitEntity, date: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateList = h.completedDates.split(",").filter { it.isNotBlank() }.toMutableList()

        if (dateList.contains(date)) {
            dateList.remove(date)
            repository.addHabit(h.copy(completedDates = dateList.joinToString(",")))
        } else {
            if (dateList.isNotEmpty()) {
                val parsedToday = try { sdf.parse(date) } catch (e: Exception) { null }
                if (parsedToday != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsedToday
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdf.format(cal.time)

                    if (!dateList.contains(yesterdayStr)) {
                        dateList.clear()
                    }
                }
            }
            if (!dateList.contains(date)) {
                dateList.add(date)
            }
            val finalDates = if (dateList.size > 21) dateList.takeLast(21) else dateList
            repository.addHabit(h.copy(completedDates = finalDates.joinToString(",")))
        }
    }

    private fun parseAttributes(attributesStr: String): Map<String, String> {
        val attrMap = mutableMapOf<String, String>()
        val regex = Regex("(\\w+)\\s*=\\s*[\"']([^\"']*)[\"']")
        regex.findAll(attributesStr).forEach { match ->
            attrMap[match.groupValues[1]] = match.groupValues[2]
        }
        return attrMap
    }

    enum class PendingFormType {
        NONE, ADD_TASK, ADD_EXAM, ADD_SUBJECT, RECORD_ATTENDANCE, ADD_COURSE, ADD_FLASHCARD, ADD_HABIT, TICK_HABIT, ADD_LEDGER, SAVE_NOTE, ADD_ROUTINE, ADD_TIMETABLE
    }

    private fun parseDurationToMinutes(input: String): Int {
        val lower = input.lowercase(Locale.getDefault())
        val hourMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:h|hr|hrs|hour|hours|ghante|ghanta)").find(lower)
        val minMatch = Regex("(\\d+)\\s*(?:m|min|mins|minute|minutes|minut)").find(lower)

        var total = 0
        if (hourMatch != null) {
            val h = hourMatch.groupValues[1].toFloatOrNull() ?: 0f
            total += (h * 60).toInt()
        }
        if (minMatch != null) {
            val m = minMatch.groupValues[1].toIntOrNull() ?: 0
            total += m
        }
        if (total == 0) {
            val num = Regex("\\d+").find(lower)?.value?.toIntOrNull() ?: 60
            total = if (num <= 12) num * 60 else num
        }
        return maxOf(5, minOf(1440, total))
    }

    private fun normalizeTo24HourTime(input: String): String {
        return try {
            val clean = input.trim()
            if (clean.contains("am", ignoreCase = true) || clean.contains("pm", ignoreCase = true)) {
                val parser = SimpleDateFormat("h:mm a", Locale.US)
                val parser2 = SimpleDateFormat("hh:mm a", Locale.US)
                val parser3 = SimpleDateFormat("ha", Locale.US)
                val parsed = try {
                    parser.parse(clean)
                } catch (e: Exception) {
                    try { parser2.parse(clean) } catch (e2: Exception) { parser3.parse(clean) }
                }
                if (parsed != null) {
                    SimpleDateFormat("HH:mm", Locale.US).format(parsed)
                } else "09:00"
            } else {
                val parts = clean.split(":")
                val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
                val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                String.format(Locale.US, "%02d:%02d", h, m)
            }
        } catch (e: Exception) {
            "09:00"
        }
    }

    private fun classifyRoutineCategory(input: String): Pair<String, Float> {
        val lower = input.lowercase(Locale.getDefault())
        return when {
            lower.contains("study") || lower.contains("padhai") || lower.contains("code") || lower.contains("exam") || lower.contains("revision") || lower.contains("prep") || lower.contains("homework") || lower.contains("book") || lower.contains("math") || lower.contains("science") -> "Study" to 1.0f
            lower.contains("college") || lower.contains("school") || lower.contains("class") || lower.contains("lecture") || lower.contains("lab") -> "College" to 0.3f
            lower.contains("gym") || lower.contains("exercise") || lower.contains("fitness") || lower.contains("workout") || lower.contains("walk") || lower.contains("meal") || lower.contains("eat") || lower.contains("khana") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("breakfast") -> "Fitness" to 0.5f
            lower.contains("sleep") || lower.contains("sona") || lower.contains("rest") || lower.contains("nap") -> "Sleep" to 0.0f
            lower.contains("game") || lower.contains("phone") || lower.contains("social") || lower.contains("insta") || lower.contains("waste") || lower.contains("reel") || lower.contains("movie") || lower.contains("tv") || lower.contains("relax") || lower.contains("youtube") -> "Waste" to 0.0f
            lower.contains("travel") || lower.contains("commute") || lower.contains("buffer") || lower.contains("chores") -> "Routine" to 0.1f
            else -> "Routine" to 0.5f
        }
    }

    private suspend fun processFormStep(userMsg: String, aiLangSetting: String): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        when (pendingFormType) {
            PendingFormType.ADD_TASK -> {
                if (!formAnswers.containsKey("title")) {
                    formAnswers["title"] = userMsg
                    return getLocalizedFormPrompt("task_priority", aiLangSetting)
                }
                if (!formAnswers.containsKey("priority")) {
                    val p = userMsg.trim().lowercase(Locale.getDefault())
                    formAnswers["priority"] = if (p == "high" || p == "low") p else "medium"
                    return getLocalizedFormPrompt("task_due", aiLangSetting)
                }
                if (!formAnswers.containsKey("dueDate")) {
                    val rawDate = userMsg.trim().lowercase(Locale.getDefault())
                    val parsedDate = when (rawDate) {
                        "today" -> todayStr
                        "tomorrow" -> {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                        }
                        else -> {
                            if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) rawDate else todayStr
                        }
                    }
                    formAnswers["dueDate"] = parsedDate
                    
                    val title = formAnswers["title"] ?: "New Task"
                    val priority = formAnswers["priority"] ?: "medium"
                    val dueDate = formAnswers["dueDate"] ?: todayStr
                    
                    val task = TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = null,
                        category = "General",
                        subject = null,
                        dueDate = dueDate,
                        priority = priority,
                        status = "todo"
                    )
                    withContext(Dispatchers.IO) { repository.addTask(task) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Task added successfully!**\n- Title: $title\n- Priority: $priority\n- Due Date: $dueDate"
                }
            }
            PendingFormType.ADD_EXAM -> {
                if (!formAnswers.containsKey("name")) {
                    formAnswers["name"] = userMsg
                    return getLocalizedFormPrompt("exam_date", aiLangSetting)
                }
                if (!formAnswers.containsKey("date")) {
                    val date = userMsg.trim()
                    val parsedDate = if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) date else todayStr
                    formAnswers["date"] = parsedDate
                    
                    val name = formAnswers["name"] ?: "New Exam"
                    val exam = ExamEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        date = parsedDate
                    )
                    withContext(Dispatchers.IO) { repository.addExam(exam) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Exam Countdown added!**\n- Exam: $name\n- Date: $parsedDate"
                }
            }
            PendingFormType.ADD_SUBJECT -> {
                if (!formAnswers.containsKey("name")) {
                    val name = userMsg.trim()
                    val att = AttendanceEntity(
                        id = UUID.randomUUID().toString(),
                        subjectName = name,
                        presents = 0,
                        absents = 0
                    )
                    withContext(Dispatchers.IO) { repository.addAttendance(att) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Subject '$name' added to Attendance list!**"
                }
            }
            PendingFormType.RECORD_ATTENDANCE -> {
                if (!formAnswers.containsKey("subject")) {
                    formAnswers["subject"] = userMsg.trim()
                    if (formAnswers.containsKey("status")) {
                        return finalizeAttendance(aiLangSetting)
                    } else {
                        return "Subject saved. **Are you 'present' or 'absent'?**"
                    }
                }
                if (!formAnswers.containsKey("status")) {
                    val status = userMsg.trim().lowercase(Locale.getDefault())
                    formAnswers["status"] = if (status.contains("absent")) "absent" else "present"
                    return finalizeAttendance(aiLangSetting)
                }
            }
            PendingFormType.ADD_COURSE -> {
                if (!formAnswers.containsKey("name")) {
                    formAnswers["name"] = userMsg.trim()
                    return "Course name saved. **How many credits? (e.g. 4)**"
                }
                if (!formAnswers.containsKey("credits")) {
                    formAnswers["credits"] = userMsg.trim()
                    return "Credits saved. **What grade did you get? (A, B, C, D, F)**"
                }
                if (!formAnswers.containsKey("grade")) {
                    val name = formAnswers["name"] ?: "Course"
                    val credits = formAnswers["credits"]?.toIntOrNull() ?: 4
                    val grade = userMsg.trim().uppercase(Locale.getDefault())
                    
                    val course = CgpaCourseEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        credits = credits,
                        grade = grade
                    )
                    withContext(Dispatchers.IO) { repository.addCourse(course) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Course added!**\n- Course: $name\n- Credits: $credits\n- Grade: $grade"
                }
            }
            PendingFormType.ADD_FLASHCARD -> {
                if (!formAnswers.containsKey("deckName")) {
                    formAnswers["deckName"] = userMsg.trim()
                    return "Deck saved. **What is the front side (Question)?**"
                }
                if (!formAnswers.containsKey("front")) {
                    formAnswers["front"] = userMsg.trim()
                    return "Question saved. **What is the back side (Answer)?**"
                }
                if (!formAnswers.containsKey("back")) {
                    val deckName = formAnswers["deckName"] ?: "General"
                    val front = formAnswers["front"] ?: "Q"
                    val back = userMsg.trim()
                    
                    val decks = withContext(Dispatchers.IO) { repository.allDecks.first() }
                    var deck = decks.find { it.name.lowercase() == deckName.lowercase() }
                    if (deck == null) {
                        deck = DeckEntity(id = UUID.randomUUID().toString(), name = deckName)
                        withContext(Dispatchers.IO) { repository.addDeck(deck) }
                    }
                    
                    val card = FlashcardEntity(
                        id = UUID.randomUUID().toString(),
                        deckId = deck.id,
                        front = front,
                        back = back
                    )
                    withContext(Dispatchers.IO) { repository.addFlashcard(card) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Flashcard saved in deck '$deckName'!**\n- Q: $front\n- A: $back"
                }
            }
            PendingFormType.TICK_HABIT -> {
                if (!formAnswers.containsKey("name")) {
                    val habitName = userMsg.trim()
                    val habits = withContext(Dispatchers.IO) { repository.allHabits.first() }
                    val h = habits.find { it.name.lowercase().contains(habitName.lowercase()) }
                    if (h != null) {
                        val currentDates = h.completedDates.split(",").filter { it.isNotBlank() }.toMutableList()
                        if (!currentDates.contains(todayStr)) {
                            currentDates.add(todayStr)
                            val updatedHabit = h.copy(completedDates = currentDates.joinToString(","))
                            withContext(Dispatchers.IO) { repository.addHabit(updatedHabit) }
                            pendingFormType = PendingFormType.NONE
                            return "✅ **Ticked habit '${h.name}' for today ($todayStr)!**"
                        } else {
                            pendingFormType = PendingFormType.NONE
                            return "Habit '${h.name}' was already completed today."
                        }
                    } else {
                        pendingFormType = PendingFormType.NONE
                        return "Habit '$habitName' not found in Streaks list."
                    }
                }
            }
            PendingFormType.ADD_HABIT -> {
                if (!formAnswers.containsKey("name")) {
                    val name = userMsg.trim()
                    val habit = HabitEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        completedDates = ""
                    )
                    withContext(Dispatchers.IO) { repository.addHabit(habit) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **New habit '$name' added!** Keep up your daily streak! 🔥"
                }
            }
            PendingFormType.ADD_LEDGER -> {
                if (!formAnswers.containsKey("type")) {
                    val type = userMsg.trim().lowercase(Locale.getDefault())
                    formAnswers["type"] = if (type.contains("income")) "income" else "expense"
                    return getLocalizedFormPrompt("ledger_type", aiLangSetting)
                }
                if (!formAnswers.containsKey("amount")) {
                    formAnswers["amount"] = userMsg.trim()
                    return "Amount saved. **What is the description? (e.g., Tea, Books, Salary)**"
                }
                if (!formAnswers.containsKey("description")) {
                    val type = formAnswers["type"] ?: "expense"
                    val amount = formAnswers["amount"]?.toDoubleOrNull() ?: 0.0
                    val description = userMsg.trim()
                    
                    val expense = ExpenseEntity(
                        id = UUID.randomUUID().toString(),
                        description = description,
                        amount = amount,
                        date = todayStr,
                        type = type
                    )
                    withContext(Dispatchers.IO) { repository.addExpense(expense) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Ledger record saved!**\n- Type: ${type.uppercase()}\n- Amount: ₹$amount\n- Description: $description"
                }
            }
            PendingFormType.SAVE_NOTE -> {
                if (!formAnswers.containsKey("content")) {
                    val content = userMsg.trim()
                    val note = NoteEntity(date = todayStr, content = content)
                    withContext(Dispatchers.IO) { repository.addNote(note) }
                    pendingFormType = PendingFormType.NONE
                    return "✅ **Study note saved for today ($todayStr)!**"
                }
            }
            PendingFormType.ADD_ROUTINE -> {
                if (!formAnswers.containsKey("activityName")) {
                    formAnswers["activityName"] = userMsg.trim()
                    return getLocalizedFormPrompt("routine_duration", aiLangSetting)
                }
                if (!formAnswers.containsKey("duration")) {
                    val durationMins = parseDurationToMinutes(userMsg)
                    formAnswers["duration"] = durationMins.toString()
                    return getLocalizedFormPrompt("routine_category", aiLangSetting)
                }
                if (!formAnswers.containsKey("category")) {
                    val activityName = formAnswers["activityName"] ?: "Routine Activity"
                    val durationMins = formAnswers["duration"]?.toIntOrNull() ?: 60
                    val (cat, score) = classifyRoutineCategory(userMsg)

                    val activity = com.ai_assistant.studentfocus.models.RoutineActivityEntity(
                        id = UUID.randomUUID().toString(),
                        activityName = activityName,
                        durationMinutes = durationMins,
                        classificationScore = score,
                        category = cat,
                        notes = "Added via AI Assistant"
                    )
                    withContext(Dispatchers.IO) { repository.addRoutineActivity(activity) }
                    pendingFormType = PendingFormType.NONE

                    val hours = durationMins / 60
                    val mins = durationMins % 60
                    val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                    return when (aiLangSetting) {
                        "hinglish" -> "✅ **Routine Activity Add ho gayi!**\n- 📌 **Activity**: $activityName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Aapka daily 24-hour routine budget aur study target update ho gaya hai!*"
                        "tamil_english" -> "✅ **Routine Activity Add aairuchu!**\n- 📌 **Activity**: $activityName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Unga daily 24-hour routine budget update aairuchu!*"
                        "telugu_english" -> "✅ **Routine Activity Add ayyindhi!**\n- 📌 **Activity**: $activityName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Mee daily 24-hour routine budget update ayyindhi!*"
                        else -> "✅ **Routine Activity Added Successfully!**\n- 📌 **Activity**: $activityName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Your 24-hour daily routine budget & effective study target have been updated!*"
                    }
                }
            }
            PendingFormType.ADD_TIMETABLE -> {
                if (!formAnswers.containsKey("dayOfWeek")) {
                    val rawDay = userMsg.trim()
                    val normalizedDay = when {
                        rawDay.contains("mon", ignoreCase = true) -> "Monday"
                        rawDay.contains("tue", ignoreCase = true) -> "Tuesday"
                        rawDay.contains("wed", ignoreCase = true) -> "Wednesday"
                        rawDay.contains("thu", ignoreCase = true) -> "Thursday"
                        rawDay.contains("fri", ignoreCase = true) -> "Friday"
                        rawDay.contains("sat", ignoreCase = true) -> "Saturday"
                        rawDay.contains("sun", ignoreCase = true) -> "Sunday"
                        rawDay.contains("daily", ignoreCase = true) || rawDay.contains("roz", ignoreCase = true) -> "Daily"
                        else -> rawDay.replaceFirstChar { it.uppercase() }
                    }
                    formAnswers["dayOfWeek"] = normalizedDay
                    return getLocalizedFormPrompt("timetable_subject", aiLangSetting)
                }
                if (!formAnswers.containsKey("subjectName")) {
                    formAnswers["subjectName"] = userMsg.trim()
                    return getLocalizedFormPrompt("timetable_start", aiLangSetting)
                }
                if (!formAnswers.containsKey("startTime")) {
                    formAnswers["startTime"] = userMsg.trim()
                    return getLocalizedFormPrompt("timetable_end", aiLangSetting)
                }
                if (!formAnswers.containsKey("endTime")) {
                    formAnswers["endTime"] = userMsg.trim()
                    return getLocalizedFormPrompt("timetable_room", aiLangSetting)
                }
                if (!formAnswers.containsKey("roomOrLink")) {
                    val room = if (userMsg.trim().lowercase(Locale.ROOT) in listOf("skip", "none", "nahi", "no", "-")) "" else userMsg.trim()
                    val day = formAnswers["dayOfWeek"] ?: "Monday"
                    val subject = formAnswers["subjectName"] ?: "Class"
                    val start = formAnswers["startTime"] ?: "09:00 AM"
                    val end = formAnswers["endTime"] ?: "10:00 AM"

                    val slot = com.ai_assistant.studentfocus.models.TimeTableSlotEntity(
                        id = UUID.randomUUID().toString(),
                        title = subject,
                        dayOfWeek = day,
                        startTime = normalizeTo24HourTime(start),
                        endTime = normalizeTo24HourTime(end),
                        roomOrLocation = if (room.isNotBlank()) room else null,
                        notes = "Added via AI Assistant",
                        isNotificationEnabled = true
                    )
                    withContext(Dispatchers.IO) {
                        repository.addTimeTableSlot(slot)
                        com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(getApplication(), slot)
                    }
                    pendingFormType = PendingFormType.NONE

                    val timeRange = slot.getFormattedTimeRange()
                    return when (aiLangSetting) {
                        "hinglish" -> "✅ **Time Table Slot Add ho gaya!**\n- 📅 **Day**: $day\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n- 📍 **Room/Location**: ${if (room.isNotBlank()) room else "None"}\n\n🔔 *Is class ke time par automatic reminder alarm set ho gaya hai!*"
                        "tamil_english" -> "✅ **Time Table Slot Add aairuchu!**\n- 📅 **Day**: $day\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n- 📍 **Room/Location**: ${if (room.isNotBlank()) room else "None"}\n\n🔔 *Class time-la reminder alarm set aairuchu!*"
                        "telugu_english" -> "✅ **Time Table Slot Add ayyindhi!**\n- 📅 **Day**: $day\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n- 📍 **Room/Location**: ${if (room.isNotBlank()) room else "None"}\n\n🔔 *Class time ki reminder alarm set ayyindhi!*"
                        else -> "✅ **Time Table Slot Added Successfully!**\n- 📅 **Day**: $day\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n- 📍 **Room/Location**: ${if (room.isNotBlank()) room else "None"}\n\n🔔 *Exact alarm reminder scheduled for this class!*"
                    }
                }
            }
            else -> {
                pendingFormType = PendingFormType.NONE
            }
        }
        return "Command processed."
    }

    private suspend fun finalizeAttendance(aiLangSetting: String): String {
        val subjectName = formAnswers["subject"] ?: ""
        val status = formAnswers["status"] ?: "present"
        
        val attendances = withContext(Dispatchers.IO) { repository.allAttendance.first() }
        var attendance = attendances.find { it.subjectName.lowercase().contains(subjectName.lowercase()) }
        if (attendance == null) {
            attendance = AttendanceEntity(
                id = UUID.randomUUID().toString(),
                subjectName = subjectName,
                presents = 0,
                absents = 0
            )
        }
        val updatedAttendance = if (status == "present") {
            attendance.copy(presents = attendance.presents + 1)
        } else {
            attendance.copy(absents = attendance.absents + 1)
        }
        withContext(Dispatchers.IO) { repository.addAttendance(updatedAttendance) }
        pendingFormType = PendingFormType.NONE
        return when (aiLangSetting) {
            "hinglish" -> "✅ **Attendance record ho gayi!**\n- Subject: ${updatedAttendance.subjectName}\n- Action: Marked ${status.uppercase()}\n- Total presents: ${updatedAttendance.presents}\n- Total absents: ${updatedAttendance.absents}"
            "tamil_english" -> "✅ **Attendance record aairuchu!**\n- Subject: ${updatedAttendance.subjectName}\n- Action: Marked ${status.uppercase()}\n- Total presents: ${updatedAttendance.presents}\n- Total absents: ${updatedAttendance.absents}"
            "telugu_english" -> "✅ **Attendance record ayyindhi!**\n- Subject: ${updatedAttendance.subjectName}\n- Action: Marked ${status.uppercase()}\n- Total presents: ${updatedAttendance.presents}\n- Total absents: ${updatedAttendance.absents}"
            else -> "✅ **Attendance recorded!**\n- Subject: ${updatedAttendance.subjectName}\n- Action: Marked ${status.uppercase()}\n- Total presents: ${updatedAttendance.presents}\n- Total absents: ${updatedAttendance.absents}"
        }
    }

    private fun getHelpGuide(aiLangSetting: String): String {
        return when (aiLangSetting) {
            "hinglish" -> """
🤖 **StudentOS AI Command & Tagging Guide**

Aap AI ko direct natural language ya `@tags` likhkar use kar sakte hain:

---

### 1. 📝 Task Management
- **Task Dekhne ke liye (@show)**:
  - `@task @show` *(Saare pending aur completed tasks ki list)*
  - `@task Physics @show` *(Specific subject/task search)*
- **Task Add karne ke liye (@add)**:
  - `@task Complete Maths Assignment @add` *(Direct single-shot add)*
  - `@task @add` *(AI aapse title, date aur priority ek-ek karke puchega)*

---

### 2. ⏱️ 24-Hour Routine & Time Audit
- **Routine Dekhne ke liye (@show)**:
  - `@routine @show` ya `routine dikhao` *(Useful study vs Waste vs Routine breakdown & daily target)*
- **Routine Add karne ke liye (@add)**:
  - `@routine Sleep for 8 hours @add`
  - `@routine Gym 1 hour @add`
  - `@routine @add` *(Step-by-step activity name, duration aur category add karne ke liye)*

---

### 3. 📊 Attendance Tracking
- **Attendance Dekhna**: `@attendance @show` *(Subject-wise % aur present/absent summary)*
- **Attendance Record Karna**: `@attendance Physics present @add` ya `@attendance @add`

---

### 4. 🎓 CGPA & Courses
- **CGPA & Grades Dekhna**: `@CGPA @show`
- **Course Add Karna**: `@CGPA @add` *(Course name, credits aur grade step-by-step)*

---

### 5. 🗂️ Flashcards & Decks
- **Flashcards Dekhna**: `@Cards @show`
- **Flashcard Add Karna**: `@Cards @add` *(Deck, Question aur Answer)*

---

### 6. 🔥 Daily Habit Streaks
- **Habits Dekhna**: `@habits @show`
- **Naya Habit Add Karna**: `@habits Read 15 mins @add` ya `@habits @add`
- **Aaj Ka Habit Tick Karna**: `@habits tick` ya `aaj ka kaam done`

---

### 7. 💰 Expense & Income Ledger
- **Transactions Dekhna**: `@ledger @show`
- **Kharcha / Income Record Karna**: `@ledger @add`

---

### 8. 📓 Study Notes & Vector Search
- **Notes Dekhna / Search**: `@notes @show` ya `@notes Thermodynamics @show`
- **Note Likhna / Save**: `@notes Physics summary @likhdo` ya `@notes @add`

---

### 9. 📅 Academic Time Table Schedule
- **Time Table Dekhna (@show)**: `@timetable @show` ya `aaj ki classes dikhao` ya `@timetable Monday @show`
- **Class Slot Add Karna (@add)**: `@timetable Monday Physics 09:00 AM to 10:00 AM @add` ya `@timetable @add`

---

### 10. 🔔 Notification Control Center
- **Status Dekhna**: `@notification status` ya `notification settings`
- **Toggle Controls**:
  - `@notification on` / `@notification off` *(Master toggle)*
  - `turn off timer notification` *(Live ongoing timer bar)*
  - `turn off daily reminder` *(Morning/Evening alerts)*
  - `turn off sound` *(Silent mode)*

---

### 11. ⏳ Time Waste & Idle Tracker
- **Wasted Time Dekhna**: `@waste @show` ya `aaj kitna time waste hua`
- **Reset Karna**: `@waste reset`

---

### 12. 🎯 Screen Time & Goal Alignment
- **Activity & Alignment Dekhna**: `@activity @show` ya `screen time status`

---

### 13. 🎨 Dashboard Layout Customization
- **Presets Apply Karna**: `@dashboard preset minimal` *(Sirf Timer + Tasks + Notes)* / `@dashboard preset scholar` / `@dashboard preset analytics` / `@dashboard preset default`
- **Widget Show/Hide**: `@dashboard hide heatmap` / `@dashboard show pace` / `@dashboard hide waste`
- **Status & Order Dekhna**: `@dashboard status` ya `@dashboard layout`

---

### 📅 Calendar Date Picker:
- Input mein `@date` likhein — Calendar popup khulega aur selected date automatically insert ho jayegi!

---

💡 **Tip:** Kisi bhi message par **Double-Click** karke aap use chat history se delete kar sakte hain!
""".trimIndent()

            "tamil_english" -> """
🤖 **StudentOS AI Command & Tagging Guide**

AI kitta direct natural language-la illa `@tags` use panni commands solla mudiyum:

---

### 1. 📝 Task Management
- **Task Paaka (@show)**: `@task @show` / `@task Physics @show`
- **Task Add Panna (@add)**: `@task Complete Assignment @add` / `@task @add`

### 2. ⏱️ 24-Hour Routine Audit
- **Routine Paaka (@show)**: `@routine @show` / `routine kaatu`
- **Routine Add Panna (@add)**: `@routine Sleep 8 hours @add` / `@routine @add`

### 3. 📊 Attendance Tracking
- **Attendance Paaka**: `@attendance @show`
- **Record Panna**: `@attendance Maths present @add` / `@attendance @add`

### 4. 🎓 CGPA & Courses
- **CGPA Paaka**: `@CGPA @show`
- **Course Add Panna**: `@CGPA @add`

### 5. 🗂️ Flashcards & Decks
- **Flashcards Paaka**: `@Cards @show`
- **Flashcard Add Panna**: `@Cards @add`

### 6. 🔥 Habits & Streaks
- **Habits Paaka**: `@habits @show`
- **Habit Add / Tick**: `@habits @add` / `@habits tick`

### 7. 💰 Ledger / Expenses
- **Ledger Paaka**: `@ledger @show`
- **Kharcha Add Panna**: `@ledger @add`

### 8. 📓 Study Notes
- **Notes Search / Paaka**: `@notes @show` / `@notes Physics @show`
- **Note Save Panna**: `@notes @add`

### 9. 📅 Academic Time Table
- **Classes Paaka**: `@timetable @show` / `@timetable Monday @show`
- **Class Add Panna**: `@timetable Monday Maths 09:00 AM to 10:00 AM @add` / `@timetable @add`

### 10. 🔔 Notifications & ⏳ Time Waste
- **Notification Controls**: `@notification status` / `@notification on` / `@notification off`
- **Time Waste Check / Reset**: `@waste @show` / `@waste reset`

---

💡 **Tip:** Message mela **Double-Click** panni chat history-la irundhu delete pannalam!
""".trimIndent()

            "telugu_english" -> """
🤖 **StudentOS AI Command & Tagging Guide**

Meeru AI ni direct natural language tho leda `@tags` tho use cheyavachu:

---

### 1. 📝 Task Management
- **Tasks Chudadaniki (@show)**: `@task @show` / `@task Physics @show`
- **Task Add Cheyadaniki (@add)**: `@task Complete Assignment @add` / `@task @add`

### 2. ⏱️ 24-Hour Routine Audit
- **Routine Chudadaniki (@show)**: `@routine @show` / `routine choopinchu`
- **Routine Add Cheyadaniki (@add)**: `@routine Sleep 8 hours @add` / `@routine @add`

### 3. 📊 Attendance Tracking
- **Attendance Chudadaniki**: `@attendance @show`
- **Record Cheyadaniki**: `@attendance Physics present @add` / `@attendance @add`

### 4. 🎓 CGPA & Courses
- **CGPA Chudadaniki**: `@CGPA @show`
- **Course Add Cheyadaniki**: `@CGPA @add`

### 5. 🗂️ Flashcards & Decks
- **Flashcards Chudadaniki**: `@Cards @show`
- **Flashcard Add Cheyadaniki**: `@Cards @add`

### 6. 🔥 Habits & Streaks
- **Habits Chudadaniki**: `@habits @show`
- **Habit Add / Tick**: `@habits @add` / `@habits tick`

### 7. 💰 Ledger / Expenses
- **Ledger Chudadaniki**: `@ledger @show`
- **Kharcha Add Cheyadaniki**: `@ledger @add`

### 8. 📓 Study Notes
- **Notes Search / Chudadaniki**: `@notes @show` / `@notes Physics @show`
- **Note Save Cheyadaniki**: `@notes @add`

### 9. 📅 Academic Time Table
- **Classes Chudadaniki**: `@timetable @show` / `@timetable Monday @show`
- **Class Add Cheyadaniki**: `@timetable Monday Maths 09:00 AM to 10:00 AM @add` / `@timetable @add`

### 10. 🔔 Notifications & ⏳ Time Waste
- **Notification Controls**: `@notification status` / `@notification on` / `@notification off`
- **Time Waste Check / Reset**: `@waste @show` / `@waste reset`

---

💡 **Tip:** E message meedha aina **Double-Click** chesi chat history nundi delete cheyavachu!
""".trimIndent()

            else -> """
🤖 **StudentOS AI Command & Mention Guide**

You can interact with StudentOS AI using natural language or direct `@tags`:

---

### 1. 📝 Task Management
- **View Tasks (@show)**:
  - `@task @show` *(Displays all pending and completed tasks)*
  - `@task Physics @show` *(Search specific task or subject)*
- **Add Tasks (@add)**:
  - `@task Complete Maths Assignment @add` *(Direct single-shot add)*
  - `@task @add` *(AI asks step-by-step for title, date, and priority)*

---

### 2. ⏱️ 24-Hour Routine & Time Audit
- **View Routine (@show)**:
  - `@routine @show` or `show routine` *(Useful Study vs Waste vs Routine breakdown & daily target)*
- **Add Routine (@add)**:
  - `@routine Sleep for 8 hours @add`
  - `@routine Gym 1 hour @add`
  - `@routine @add` *(Step-by-step activity name, duration, and category)*

---

### 3. 📊 Attendance Tracking
- **View Attendance**: `@attendance @show` *(Subject-wise percentage & present/absent summary)*
- **Record Attendance**: `@attendance Physics present @add` or `@attendance @add`

---

### 4. 🎓 CGPA & Courses
- **View CGPA**: `@CGPA @show`
- **Add Course**: `@CGPA @add` *(Course name, credits, expected grade step-by-step)*

---

### 5. 🗂️ Flashcards & Decks
- **View Flashcards**: `@Cards @show`
- **Add Flashcard**: `@Cards @add` *(Deck, Question, and Answer)*

---

### 6. 🔥 Daily Habit Streaks
- **View Habits**: `@habits @show`
- **Add New Habit**: `@habits Read 15 mins @add` or `@habits @add`
- **Tick Habit Today**: `@habits tick` or `habit complete`

---

### 7. 💰 Financial Ledger
- **View Ledger**: `@ledger @show`
- **Add Transaction**: `@ledger @add`

---

### 8. 📓 Study Notes & RAG Search
- **Search Notes**: `@notes @show` or `@notes Thermodynamics @show`
- **Save Note**: `@notes Physics formulas summary @likhdo` or `@notes @add`

---

### 9. 📅 Academic Time Table
- **View Schedule (@show)**: `@timetable @show` or `@timetable Monday @show`
- **Add Class Slot (@add)**: `@timetable Monday Physics 09:00 AM to 10:00 AM @add` or `@timetable @add`

---

### 10. 🔔 Notification Control Center
- **Check Status**: `@notification status`
- **Toggle Controls**:
  - `@notification on` / `@notification off` *(Master toggle)*
  - `turn off timer notification` *(Live ongoing countdown bar)*
  - `turn off daily reminder` *(Morning & evening alerts)*
  - `turn off sound` *(Silent alerts)*

---

### 11. ⏳ Time Waste & Idle Tracker
- **View Wasted Time**: `@waste @show` or `how much time wasted today`
- **Reset Counter**: `@waste reset`

---

### 12. 🎯 Screen Time & Goal Alignment
- **View Activity & Alignment**: `@activity @show`

---

### 13. 🎨 Dashboard Layout Customization
- **Apply Presets**: `@dashboard preset minimal` *(Timer + Tasks + Notes)* / `@dashboard preset scholar` / `@dashboard preset analytics` / `@dashboard preset default`
- **Widget Show/Hide**: `@dashboard hide heatmap` / `@dashboard show pace` / `@dashboard hide waste`
- **View Status & Order**: `@dashboard status` or `@dashboard layout`

---

### 📅 Calendar Date Picker:
- Type `@date` in the chat input — the Calendar picker will pop up and insert your selected date automatically!

---

💡 **Tip:** **Double-Tap** any chat message to delete it from your chat history!
""".trimIndent()
        }
    }

    private suspend fun handleRuleBasedForm(userMsg: String, aiLangSetting: String): String {
        val msg = userMsg.trim().lowercase(Locale.getDefault())
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 0. Check @help command
        if (msg.contains("@help") || msg.trim() == "help" || msg.contains("@guide") || msg.contains("kaise use kare") || msg.contains("how to use") || msg.contains("help me") || msg.contains("guide")) {
            return getHelpGuide(aiLangSetting)
        }

        // 1. If currently in a form flow, collect slot answer
        if (pendingFormType != PendingFormType.NONE) {
            return processFormStep(userMsg, aiLangSetting)
        }

        // Detect Add vs Show intents
        val isAddIntent = msg.contains("@add") || msg.contains("add") || msg.contains("likhdo") || msg.contains("likho") || msg.contains("create") || msg.contains("naya") || msg.contains("set") || msg.contains("daal") || msg.contains("dalo") || msg.contains("save") || msg.contains("record")
        val isShowIntent = msg.contains("@show") || msg.contains("@give") || msg.contains("@dikhao") || msg.contains("@dikhna") || msg.contains("@batao") || msg.contains("@view") || msg.contains("show") || msg.contains("give") || msg.contains("dikhao") || msg.contains("dikhna") || msg.contains("batao") || msg.contains("view") || msg.contains("list") || msg.contains("status")

        // 2. Check Add intents (only if not explicitly asking to show)
        if (isAddIntent && !isShowIntent) {
            when {
                // Routine Add (Single-Shot or Multi-Step)
                msg.contains("@routine") || msg.contains("routine") || msg.contains("time audit") || msg.contains("schedule") -> {
                    val durationMins = parseDurationToMinutes(msg)
                    val cleanMsg = msg.replace("@routine", "").replace("@add", "").replace("add", "").replace("routine", "").replace("naya", "").replace("set", "").replace("create", "").replace("daal", "").replace("dalo", "").replace("karo", "").replace("ghante", "").replace("ghanta", "").replace("hours", "").replace("hour", "").replace("mins", "").replace("minutes", "").replace("for", "").replace("mai", "").replace("mein", "").trim()
                    
                    if (cleanMsg.length >= 3 && msg.matches(Regex(".*\\d+.*"))) {
                        val (cat, score) = classifyRoutineCategory(cleanMsg)
                        val actName = cleanMsg.split(Regex("\\s+")).filter { !it.matches(Regex("\\d+.*")) }.joinToString(" ").replaceFirstChar { it.uppercase() }
                        val finalName = if (actName.isNotBlank()) actName else "Routine Activity"

                        val activity = com.ai_assistant.studentfocus.models.RoutineActivityEntity(
                            id = UUID.randomUUID().toString(),
                            activityName = finalName,
                            durationMinutes = durationMins,
                            classificationScore = score,
                            category = cat,
                            notes = "Added via AI Assistant"
                        )
                        withContext(Dispatchers.IO) { repository.addRoutineActivity(activity) }
                        
                        val hours = durationMins / 60
                        val mins = durationMins % 60
                        val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                        return when (aiLangSetting) {
                            "hinglish" -> "✅ **Routine Activity Add ho gayi!**\n- 📌 **Activity**: $finalName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Aapka daily 24-hour routine schedule update ho gaya!*"
                            "tamil_english" -> "✅ **Routine Activity Add aairuchu!**\n- 📌 **Activity**: $finalName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Unga daily 24-hour routine schedule update aairuchu!*"
                            "telugu_english" -> "✅ **Routine Activity Add ayyindhi!**\n- 📌 **Activity**: $finalName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Mee daily 24-hour routine schedule update ayyindhi!*"
                            else -> "✅ **Routine Activity Added Successfully!**\n- 📌 **Activity**: $finalName\n- ⏳ **Duration**: $durationText ($durationMins mins)\n- 🏷️ **Category**: $cat (Score: $score)\n\n📊 *Your 24-hour daily routine budget & effective study target have been updated!*"
                        }
                    }

                    pendingFormType = PendingFormType.ADD_ROUTINE
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_routine", aiLangSetting)
                }

                // Task Add
                msg.contains("@task") || msg.contains("task") || msg.contains("target") -> {
                    val cleanTitle = msg.replace("@task", "").replace("@add", "").replace("add", "").replace("task", "").replace("target", "").replace("create", "").replace("naya", "").replace("karo", "").replace("likho", "").replace("likhdo", "").trim()
                    if (cleanTitle.length >= 3 && !cleanTitle.equals("kar do", ignoreCase = true) && !cleanTitle.equals("likh do", ignoreCase = true)) {
                        val task = TaskEntity(
                            id = UUID.randomUUID().toString(),
                            title = cleanTitle.replaceFirstChar { it.uppercase() },
                            description = null,
                            category = "General",
                            subject = null,
                            dueDate = todayStr,
                            priority = "medium",
                            status = "todo"
                        )
                        withContext(Dispatchers.IO) { repository.addTask(task) }
                        return "✅ **Task added successfully!**\n- Title: ${task.title}\n- Priority: medium\n- Due Date: $todayStr"
                    }
                    pendingFormType = PendingFormType.ADD_TASK
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_task", aiLangSetting)
                }

                // Exam Add
                msg.contains("@exam") || msg.contains("exam") || msg.contains("countdown") -> {
                    pendingFormType = PendingFormType.ADD_EXAM
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_exam", aiLangSetting)
                }

                // Attendance Add / Record
                msg.contains("@attendance") || msg.contains("attendance") || msg.contains("present") || msg.contains("absent") || msg.contains("@subject") -> {
                    pendingFormType = PendingFormType.RECORD_ATTENDANCE
                    formAnswers.clear()
                    if (msg.contains("present")) formAnswers["status"] = "present"
                    if (msg.contains("absent")) formAnswers["status"] = "absent"
                    val subName = msg.replace("@attendance", "").replace("@add", "").replace("attendance", "").replace("present", "").replace("absent", "").replace("add", "").replace("record", "").trim()
                    if (subName.length >= 2 && !subName.equals("kar do", ignoreCase = true)) formAnswers["subject"] = subName
                    return getLocalizedFormPrompt("init_attendance", aiLangSetting)
                }

                // Course / CGPA Add
                msg.contains("@cgpa") || msg.contains("@course") || msg.contains("course") || msg.contains("cgpa") || msg.contains("gpa") -> {
                    pendingFormType = PendingFormType.ADD_COURSE
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_course", aiLangSetting)
                }

                // Flashcard Add
                msg.contains("@cards") || msg.contains("@flashcard") || msg.contains("flashcard") || msg.contains("deck") -> {
                    pendingFormType = PendingFormType.ADD_FLASHCARD
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_flashcard", aiLangSetting)
                }

                // Habit Add or Tick
                msg.contains("@habits") || msg.contains("@habit") || msg.contains("habit") -> {
                    if (msg.contains("tick") || msg.contains("complete") || msg.contains("done") || msg.contains("ajh ka kaam") || msg.contains("aaj ka kaam")) {
                        pendingFormType = PendingFormType.TICK_HABIT
                        formAnswers.clear()
                        return getLocalizedFormPrompt("init_tick_habit", aiLangSetting)
                    } else {
                        val habitName = msg.replace("@habits", "").replace("@habit", "").replace("@add", "").replace("habit", "").replace("add", "").replace("new", "").replace("naya", "").trim()
                        if (habitName.length >= 3 && !habitName.equals("kar do", ignoreCase = true)) {
                            val h = HabitEntity(
                                id = UUID.randomUUID().toString(),
                                name = habitName.replaceFirstChar { it.uppercase() },
                                completedDates = ""
                            )
                            withContext(Dispatchers.IO) { repository.addHabit(h) }
                            return "✅ **New Habit '${h.name}' created!** Keep up your daily streak! 🔥"
                        }
                        pendingFormType = PendingFormType.ADD_HABIT
                        formAnswers.clear()
                        return getLocalizedFormPrompt("init_add_habit", aiLangSetting)
                    }
                }

                // Ledger Add
                msg.contains("@ledger") || msg.contains("@expense") || msg.contains("@money") || msg.contains("ledger") || msg.contains("expense") || msg.contains("income") || msg.contains("rupees") || msg.contains("rupee") || msg.contains("rs") -> {
                    pendingFormType = PendingFormType.ADD_LEDGER
                    formAnswers.clear()
                    if (msg.contains("expense")) formAnswers["type"] = "expense"
                    if (msg.contains("income")) formAnswers["type"] = "income"
                    return getLocalizedFormPrompt("init_ledger", aiLangSetting)
                }

                // Note Add
                msg.contains("@notes") || msg.contains("@note") || msg.contains("note") || msg.contains("likhdo") || msg.contains("likho") -> {
                    val noteText = msg.replace("@notes", "").replace("@note", "").replace("@add", "").replace("@likhdo", "").replace("note", "").replace("notes", "").replace("add", "").replace("likho", "").replace("likhdo", "").replace("save", "").trim()
                    if (noteText.length >= 3 && !noteText.equals("kar do", ignoreCase = true) && !noteText.equals("likh do", ignoreCase = true)) {
                        val note = NoteEntity(date = todayStr, content = noteText, topic = "")
                        withContext(Dispatchers.IO) { repository.addNote(note) }
                        return "✅ **Study note saved for today ($todayStr)!**\n- Note: *${note.content}*"
                    }
                    pendingFormType = PendingFormType.SAVE_NOTE
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_note", aiLangSetting)
                }

                // Time Table Add (Single-Shot or Multi-Step)
                msg.contains("@timetable") || msg.contains("timetable") || msg.contains("time table") || msg.contains("class schedule") || msg.contains("lecture") -> {
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Daily")
                    val matchedDay = days.find { msg.contains(it.lowercase(Locale.ROOT)) }
                    
                    val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\s*(?:to|-)\\s*(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm))", RegexOption.IGNORE_CASE)
                    val timeMatch = timeRegex.find(msg)

                    if (matchedDay != null && timeMatch != null) {
                        val startTime = timeMatch.groupValues[1].trim().uppercase(Locale.ROOT)
                        val endTime = timeMatch.groupValues[2].trim().uppercase(Locale.ROOT)
                        val cleanSubject = msg.replace("@timetable", "")
                            .replace("@add", "")
                            .replace("add", "")
                            .replace("timetable", "")
                            .replace("time table", "")
                            .replace(matchedDay.lowercase(Locale.ROOT), "")
                            .replace(timeMatch.value.lowercase(Locale.ROOT), "")
                            .replace("room", "")
                            .trim()
                        val subject = if (cleanSubject.isNotBlank()) cleanSubject.replaceFirstChar { it.uppercase() } else "Lecture"

                        val slot = com.ai_assistant.studentfocus.models.TimeTableSlotEntity(
                            id = UUID.randomUUID().toString(),
                            title = subject,
                            dayOfWeek = matchedDay,
                            startTime = normalizeTo24HourTime(startTime),
                            endTime = normalizeTo24HourTime(endTime),
                            roomOrLocation = null,
                            notes = "Added via AI Assistant",
                            isNotificationEnabled = true
                        )
                        withContext(Dispatchers.IO) {
                            repository.addTimeTableSlot(slot)
                            com.ai_assistant.studentfocus.timer.TimeTableReminderScheduler.scheduleSlotReminder(getApplication(), slot)
                        }
                        val timeRange = slot.getFormattedTimeRange()
                        return when (aiLangSetting) {
                            "hinglish" -> "✅ **Time Table Slot Add ho gaya!**\n- 📅 **Day**: $matchedDay\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n\n🔔 *Is class ke liye reminder alarm schedule ho gaya hai!*"
                            "tamil_english" -> "✅ **Time Table Slot Add aairuchu!**\n- 📅 **Day**: $matchedDay\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n\n🔔 *Class reminder alarm schedule aairuchu!*"
                            "telugu_english" -> "✅ **Time Table Slot Add ayyindhi!**\n- 📅 **Day**: $matchedDay\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n\n🔔 *Class reminder alarm schedule ayyindhi!*"
                            else -> "✅ **Time Table Slot Added Successfully!**\n- 📅 **Day**: $matchedDay\n- 📚 **Subject**: $subject\n- ⏰ **Timings**: $timeRange\n\n🔔 *Exact reminder alarm scheduled for this class!*"
                        }
                    }

                    pendingFormType = PendingFormType.ADD_TIMETABLE
                    formAnswers.clear()
                    return getLocalizedFormPrompt("init_timetable", aiLangSetting)
                }
            }
        }

        // Notification Controls Command Interceptor
        if (msg.contains("@notification") || msg.contains("@notif") || 
            (msg.contains("notification") && (msg.contains("on") || msg.contains("off") || msg.contains("band") || msg.contains("chalu") || msg.contains("mute") || msg.contains("status") || msg.contains("setting")))) {
            val isOff = msg.contains("off") || msg.contains("band") || msg.contains("disable") || msg.contains("mute") || msg.contains("silent")
            val isOn = msg.contains("on") || msg.contains("chalu") || msg.contains("enable") || msg.contains("unmute")

            val isTimerNotif = msg.contains("timer") || msg.contains("ongoing") || msg.contains("countdown") || msg.contains("bar")
            val isCompleteNotif = msg.contains("complete") || msg.contains("completion") || msg.contains("finish") || msg.contains("target") || msg.contains("alert")
            val isDailyNotif = msg.contains("daily") || msg.contains("task reminder") || msg.contains("morning") || msg.contains("evening")
            val isTimeTableNotif = msg.contains("timetable") || msg.contains("class") || msg.contains("alarm")
            val isSoundNotif = msg.contains("sound") || msg.contains("vibrate") || msg.contains("vibration") || msg.contains("audio")

            val context = getApplication<Application>()

            if (isOff) {
                when {
                    isTimerNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setOngoingTimerNotificationsEnabled(context, false)
                        return "🔕 **Live Ongoing Timer Notification OFF ho gayi!** (Study timers will run quietly without notification bar)."
                    }
                    isCompleteNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setCompletionNotificationsEnabled(context, false)
                        return "🔕 **Session Completion Alerts OFF ho gaye!**"
                    }
                    isDailyNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setDailyRemindersEnabled(context, false)
                        return "🔕 **Daily Task Reminders (9 AM / 7 PM) OFF ho gaye!**"
                    }
                    isTimeTableNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setTimeTableRemindersEnabled(context, false)
                        return "🔕 **Time Table Class Alarms OFF ho gaye!**"
                    }
                    isSoundNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setSoundAlertsEnabled(context, false)
                        return "🔇 **Notification Sound & Vibration OFF (Silent Mode) ho gaye!**"
                    }
                    else -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setNotificationsEnabled(context, false)
                        return "🔕 **Master Switch OFF: Sabhi App Notifications band kar diye gaye hain!**"
                    }
                }
            } else if (isOn) {
                when {
                    isTimerNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setOngoingTimerNotificationsEnabled(context, true)
                        return "🔔 **Live Ongoing Timer Notification ON ho gayi!** (Notification tray me countdown timer dikhega)."
                    }
                    isCompleteNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setCompletionNotificationsEnabled(context, true)
                        return "🔔 **Session Completion Alerts ON ho gaye!**"
                    }
                    isDailyNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setDailyRemindersEnabled(context, true)
                        return "🔔 **Daily Task Reminders (9 AM / 7 PM) ON ho gaye!**"
                    }
                    isTimeTableNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setTimeTableRemindersEnabled(context, true)
                        return "🔔 **Time Table Class Alarms ON ho gaye!**"
                    }
                    isSoundNotif -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setSoundAlertsEnabled(context, true)
                        return "🔊 **Notification Sound & Vibration ON ho gaye!**"
                    }
                    else -> {
                        com.ai_assistant.studentfocus.timer.FocusNotificationHelper.setNotificationsEnabled(context, true)
                        return "🔔 **Master Switch ON: Sabhi App Notifications active ho gaye hain!**"
                    }
                }
            } else {
                // Show Status Board
                val master = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areNotificationsEnabled(context)
                val timer = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areOngoingTimerNotificationsEnabled(context)
                val complete = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areCompletionNotificationsEnabled(context)
                val daily = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areDailyRemindersEnabled(context)
                val timetable = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areTimeTableRemindersEnabled(context)
                val sound = com.ai_assistant.studentfocus.timer.FocusNotificationHelper.areSoundAlertsEnabled(context)

                return """
                    🔔 **StudentOS Notification Control Center Status:**
                    
                    - 🔘 **Master Switch (All Notifications):** ${if (master) "🟢 ON" else "🔴 OFF"}
                    - ⏱️ **Live Ongoing Timer Bar:** ${if (timer) "🟢 ON" else "🔴 OFF"}
                    - 🎉 **Session Completion Alerts:** ${if (complete) "🟢 ON" else "🔴 OFF"}
                    - 📅 **Daily Task Reminders (9AM/7PM):** ${if (daily) "🟢 ON" else "🔴 OFF"}
                    - 🗓️ **Time Table Class Alarms:** ${if (timetable) "🟢 ON" else "🔴 OFF"}
                    - 🔊 **Sound & Vibration:** ${if (sound) "🟢 ON" else "🔴 OFF (Silent)"}
                    
                    💡 *Aap kisi bhi notification ko directly control kar sakte hain, e.g.:*
                    - *"turn off timer notification"*
                    - *"turn on daily reminder"*
                    - *"turn off all notifications"*
                """.trimIndent()
            }
        }

        // Time Waste & Idle Tracker Command Interceptor
        if (msg.contains("@waste") || msg.contains("waste time") || msg.contains("wasted time") || msg.contains("barbaad samay") || msg.contains("time waste")) {
            val todayWastedSecs = com.ai_assistant.studentfocus.timer.TimeWasteManager.todayWastedSeconds.value
            val totalWastedSecs = com.ai_assistant.studentfocus.timer.TimeWasteManager.totalWastedSeconds.value
            
            if (msg.contains("reset") || msg.contains("zero") || msg.contains("clear") || msg.contains("mitao")) {
                com.ai_assistant.studentfocus.timer.TimeWasteManager.resetStats()
                return "🔄 **Time Waste Counter Reset ho gaya!**\n- Aaj ka Wasted Time: **0s**\n- Total Wasted Time: **0s**\n\nAb fresh focus ke sath study shuru karein! 🚀"
            }
            val todayHrs = todayWastedSecs / 3600
            val todayMins = (todayWastedSecs % 3600) / 60
            val todaySecs = todayWastedSecs % 60
            val totalHrs = totalWastedSecs / 3600
            val totalMins = (totalWastedSecs % 3600) / 60

            val isTicking = com.ai_assistant.studentfocus.timer.TimeWasteManager.isWasteTicking.value
            return """
                ⏳ **Time Waste & Idle Tracker Status:**
                
                - 📉 **Aaj Ka Barbaad Samay (Today):** ${todayHrs}h ${todayMins}m ${todaySecs}s
                - 📊 **Total Wasted Time:** ${totalHrs}h ${totalMins}m
                - ⚡ **Current State:** ${if (isTicking) "⚠️ Wasted timer ticking (No active study task)" else "✅ Paused (Study focus session active)"}
                
                💡 *Time waste counter ko reset karne ke liye likhein: `@waste reset`*
            """.trimIndent()
        }

        // App Activity & Screen Time Query
        if (msg.contains("@activity") || msg.contains("activity status") || msg.contains("goal alignment") || msg.contains("screen time") || msg.contains("app usage")) {
            val todaySessions = withContext(Dispatchers.IO) { repository.getActivitySessionsByDateDirect(todayStr) }
            val totalMins = todaySessions.sumOf { it.durationMillis } / (1000 * 60)
            val learningMins = todaySessions.filter { it.category == "Learning" || it.category == "Coding" }.sumOf { it.durationMillis } / (1000 * 60)
            val distractionMins = todaySessions.filter { it.category == "Distraction" || it.category == "Social Media" }.sumOf { it.durationMillis } / (1000 * 60)
            val alignment = if (totalMins > 0) (learningMins.toFloat() / totalMins.toFloat() * 100).toInt() else 100

            val topActivities = todaySessions.take(4).map { "- **${it.appName}**: ${(it.durationMillis / (1000 * 60))}m (${it.category})" }

            return """
                📊 **Today's Screen Time & Goal Alignment:**
                
                - 📱 **Total Screen Time:** ${totalMins / 60}h ${totalMins % 60}m
                - 🎯 **Useful Learning/Study Time:** ${learningMins / 60}h ${learningMins % 60}m
                - ⚠️ **Distraction Time:** ${distractionMins / 60}h ${distractionMins % 60}m
                - 📈 **Goal Alignment Score:** **$alignment%** ${if (alignment >= 70) "🔥 Excellent Focus!" else if (alignment >= 40) "⚠️ Needs Improvement" else "🚨 High Distraction"}
                
                **Top Tracked Apps Today:**
                ${if (topActivities.isEmpty()) "- No app activity recorded yet today." else topActivities.joinToString("\n")}
            """.trimIndent()
        }

        // Dashboard Layout Customization Command Interceptor
        if (msg.contains("@dashboard") || msg.contains("dashboard layout") || msg.contains("customize dashboard") || msg.contains("dashboard customize")) {
            val context = getApplication<Application>()
            
            // Preset handling
            if (msg.contains("preset minimal") || msg.contains("minimal focus") || msg.contains("minimal layout") || msg.contains("preset focus")) {
                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.applyPreset(context, "minimal")
                return "⚡ **Minimal Focus Dashboard Preset Apply ho gaya!**\n- Visible Sections: Greeting, Pomodoro Timer, Priority Targets, Daily Notes.\n- Other analytics & heatmaps hide kar diye gaye hain taaki aapka focus bani rahe!"
            }
            if (msg.contains("preset scholar") || msg.contains("academic scholar") || msg.contains("scholar layout")) {
                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.applyPreset(context, "scholar")
                return "🎓 **Academic Scholar Dashboard Preset Apply ho gaya!**\n- Visible Sections: Scholar XP, Greeting, Exams, Priority Targets, Study Pace, Daily Notes."
            }
            if (msg.contains("preset analytics") || msg.contains("analytics heavy") || msg.contains("stats layout")) {
                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.applyPreset(context, "analytics")
                return "📊 **Analytics Heavy Dashboard Preset Apply ho gaya!**\n- Visible Sections: Greeting, Stats, 365-Day Heatmap, Weekly Chart, Time Waste Tracker, Priority Targets."
            }
            if (msg.contains("preset default") || msg.contains("reset") || msg.contains("default layout") || msg.contains("all widgets")) {
                com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.resetToDefault(context)
                return "🌟 **Dashboard Default Layout Reset ho gaya!**\n- Sabhi 11 widgets enable kar diye gaye hain aur standard order restore ho gaya hai."
            }

            // Hide / Show specific widgets
            val isHide = msg.contains("hide") || msg.contains("chupao") || msg.contains("band") || msg.contains("remove") || msg.contains("disable")
            val isShow = msg.contains("show") || msg.contains("dikhao") || msg.contains("chalu") || msg.contains("add") || msg.contains("enable")

            val targetWidget = when {
                msg.contains("heatmap") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_HEATMAP
                msg.contains("waste") || msg.contains("idle") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_TIME_WASTE
                msg.contains("chart") || msg.contains("weekly") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_WEEKLY_CHART
                msg.contains("timer") || msg.contains("pomodoro") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_TIMER
                msg.contains("pace") || msg.contains("goal") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_PACE
                msg.contains("exam") || msg.contains("countdown") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_EXAMS
                msg.contains("note") || msg.contains("scratchpad") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_DAILY_NOTES
                msg.contains("scholar") || msg.contains("avatar") || msg.contains("xp") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_SCHOLAR
                msg.contains("stat") -> com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.WIDGET_STATS
                else -> null
            }

            if (targetWidget != null) {
                if (isHide) {
                    com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.setWidgetVisibility(context, targetWidget, false)
                    val widgetInfo = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.ALL_WIDGETS.find { it.id == targetWidget }
                    return "👁️‍🗨️ **${widgetInfo?.title ?: targetWidget}** dashboard se **HIDE** kar diya gaya hai!"
                } else if (isShow) {
                    com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.setWidgetVisibility(context, targetWidget, true)
                    val widgetInfo = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.ALL_WIDGETS.find { it.id == targetWidget }
                    return "👁️ **${widgetInfo?.title ?: targetWidget}** dashboard par **SHOW** kar diya gaya hai!"
                }
            }

            // Status / Summary
            val currentOrder = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getWidgetOrder(context)
            val hidden = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getHiddenWidgets(context)
            val presetName = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.getActivePresetName(context)
            val widgetMap = com.ai_assistant.studentfocus.utils.DashboardCustomizationManager.ALL_WIDGETS.associateBy { it.id }

            val orderLines = currentOrder.mapIndexed { idx, id ->
                val w = widgetMap[id]
                val isVis = id !in hidden
                "${idx + 1}. ${w?.icon ?: "📌"} **${w?.title ?: id}** — ${if (isVis) "✅ Visible" else "❌ Hidden"}"
            }.joinToString("\n")

            return """
                🎨 **StudentOS Dashboard Customization Status:**
                
                - **Active Preset:** `${presetName.uppercase()}`
                - **Total Sections:** ${currentOrder.size} (${currentOrder.size - hidden.size} Visible, ${hidden.size} Hidden)
                
                📋 **Current Card Order & State:**
                $orderLines
                
                💡 **Quick AI Commands:**
                - `@dashboard preset minimal` *(Sirf Timer + Tasks + Notes)*
                - `@dashboard preset scholar` *(Exams + Tasks + Pace)*
                - `@dashboard preset analytics` *(Heatmap + Chart + Waste)*
                - `@dashboard preset default` *(Reset all to default)*
                - `@dashboard hide heatmap` / `@dashboard show pace`
            """.trimIndent()
        }
        
        // Fetch database context in coroutine scope to return live answers!
        val tasksList = withContext(Dispatchers.IO) { repository.allTasks.first() }
        val attendanceList = withContext(Dispatchers.IO) { repository.allAttendance.first() }
        val examsList = withContext(Dispatchers.IO) { repository.allExams.first() }
        val habitsList = withContext(Dispatchers.IO) { repository.allHabits.first() }
        val expensesList = withContext(Dispatchers.IO) { repository.allExpenses.first() }
        val notesList = withContext(Dispatchers.IO) { repository.allNotes.first() }
        val coursesList = withContext(Dispatchers.IO) { repository.allCourses.first() }
        val decksList = withContext(Dispatchers.IO) { repository.allDecks.first() }
        val routineList = withContext(Dispatchers.IO) { repository.allRoutineActivities.first() }
        val timeTableList = withContext(Dispatchers.IO) { repository.allTimeTableSlots.first() }

        return getMultilingualAcademicResponse(msg, tasksList, attendanceList, examsList, habitsList, expensesList, notesList, coursesList, decksList, routineList, timeTableList, aiLangSetting)
    }

    private fun solveQuadratic(a: Double, b: Double, c: Double): String {
        val d = b * b - 4 * a * c
        return when {
            d > 0 -> {
                val r1 = (-b + Math.sqrt(d)) / (2 * a)
                val r2 = (-b - Math.sqrt(d)) / (2 * a)
                """
                📝 **Quadratic Equation Step-by-Step Solution:**
                Equation: ${a}x² + (${b})x + (${c}) = 0
                
                1. **Identify Coefficients:**
                   - a = $a, b = $b, c = $c
                
                2. **Calculate Discriminant (D):**
                   - D = b² - 4ac
                   - D = ($b)² - 4 × $a × $c
                   - D = ${b*b} - ${4*a*c} = $d
                   *(Since D > 0, roots are real and distinct)*
                   
                3. **Apply Quadratic Formula:**
                   - x = [-b ± √D] / 2a
                   - x = [-($b) ± √$d] / [2 × $a]
                   - x₁ = [-($b) + ${Math.sqrt(d)}] / ${2*a} = **$r1**
                   - x₂ = [-($b) - ${Math.sqrt(d)}] / ${2*a} = **$r2**
                   
                **Roots:** x = $r1 or x = $r2
                """.trimIndent()
            }
            d == 0.0 -> {
                val r = -b / (2 * a)
                """
                📝 **Quadratic Equation Step-by-Step Solution:**
                Equation: ${a}x² + (${b})x + (${c}) = 0
                
                1. **Identify Coefficients:**
                   - a = $a, b = $b, c = $c
                
                2. **Calculate Discriminant (D):**
                   - D = b² - 4ac = 0
                   *(Since D = 0, roots are real and equal)*
                   
                3. **Apply Quadratic Formula:**
                   - x = -b / 2a = -($b) / (2 × $a) = **$r**
                   
                **Root:** x = $r (Repeated root)
                """.trimIndent()
            }
            else -> {
                val real = -b / (2 * a)
                val imag = Math.sqrt(-d) / (2 * a)
                """
                📝 **Quadratic Equation Step-by-Step Solution:**
                Equation: ${a}x² + (${b})x + (${c}) = 0
                
                1. **Identify Coefficients:**
                   - a = $a, b = $b, c = $c
                
                2. **Calculate Discriminant (D):**
                   - D = b² - 4ac = $d
                   *(Since D < 0, roots are complex/imaginary)*
                   
                3. **Apply Quadratic Formula:**
                   - x = [-b ± i√(-D)] / 2a
                   - x₁ = **$real + ${imag}i**
                   - x₂ = **$real - ${imag}i**
                   
                **Roots:** x = $real ± ${imag}i
                """.trimIndent()
            }
        }
    }

    private fun parseAndSolveQuadratic(msg: String): String? {
        if (!msg.contains("x^2") && !msg.contains("x²")) return null
        val clean = msg.replace(" ", "").replace("x²", "x^2").replace("=0", "")
        
        val aMatch = Regex("([+-]?\\d*)x\\^2").find(clean) ?: return null
        val aStr = aMatch.groupValues[1]
        val a = when {
            aStr.isEmpty() || aStr == "+" -> 1.0
            aStr == "-" -> -1.0
            else -> aStr.toDoubleOrNull() ?: 1.0
        }
        
        val bMatch = Regex("([+-]?\\d*)x(?!\\^2)").find(clean)
        val b = if (bMatch != null) {
            val bStr = bMatch.groupValues[1]
            when {
                bStr.isEmpty() || bStr == "+" -> 1.0
                bStr == "-" -> -1.0
                else -> bStr.toDoubleOrNull() ?: 0.0
            }
        } else {
            0.0
        }
        
        val cMatch = Regex("([+-]?\\d+)$").find(clean)
        val c = if (cMatch != null) {
            cMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        
        return solveQuadratic(a, b, c)
    }

    private fun solveCalculus(msg: String): String? {
        val isDiff = msg.contains("derivative") || msg.contains("diff") || msg.contains("d/dx") || msg.contains("differentiate")
        val isInt = msg.contains("integral") || msg.contains("integrate") || msg.contains("integration")
        if (!isDiff && !isInt) return null
        
        val powerMatch = Regex("x\\^([+-]?\\d+\\.?\\d*)").find(msg)
        if (powerMatch != null) {
            val n = powerMatch.groupValues[1].toDoubleOrNull() ?: return null
            return if (isDiff) {
                val newPower = n - 1
                val step = "1. Use power rule: d/dx(x^n) = n * x^(n-1)\n2. For n = $n: d/dx(x^$n) = $n * x^($newPower)"
                "📈 **Calculus Derivative:**\n\n- Expression: x^$n\n- Derivative: **${n}x^$newPower**\n\n**Step-by-step:**\n$step"
            } else {
                if (n == -1.0) {
                    "📈 **Calculus Integral:**\n\n- Expression: x^-1 or 1/x\n- Integral: **ln|x| + C**"
                } else {
                    val newPower = n + 1
                    val denom = newPower
                    val step = "1. Use integration power rule: ∫ x^n dx = [x^(n+1)] / (n+1) + C\n2. For n = $n: ∫ x^$n dx = [x^$newPower] / $denom + C"
                    "📈 **Calculus Integral:**\n\n- Expression: x^$n\n- Integral: **(x^$newPower) / $denom + C**\n\n**Step-by-step:**\n$step"
                }
            }
        }
        
        if (msg.contains("sin(x)") || msg.contains("sinx")) {
            return if (isDiff) {
                "📈 **Calculus Derivative:**\n\n- Expression: sin(x)\n- Derivative: **cos(x)**"
            } else {
                "📈 **Calculus Integral:**\n\n- Expression: sin(x)\n- Integral: **-cos(x) + C**"
            }
        }
        if (msg.contains("cos(x)") || msg.contains("cosx")) {
            return if (isDiff) {
                "📈 **Calculus Derivative:**\n\n- Expression: cos(x)\n- Derivative: **-sin(x)**"
            } else {
                "📈 **Calculus Integral:**\n\n- Expression: cos(x)\n- Integral: **sin(x) + C**"
            }
        }
        if (msg.contains("tan(x)") || msg.contains("tanx")) {
            return if (isDiff) {
                "📈 **Calculus Derivative:**\n\n- Expression: tan(x)\n- Derivative: **sec^2(x)**"
            } else {
                "📈 **Calculus Integral:**\n\n- Expression: tan(x)\n- Integral: **ln|sec(x)| + C**"
            }
        }
        return null
    }

    private fun evaluateArithmetic(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            
            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }
            
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }
            
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
                return x
            }
            
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }
            
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }
            
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                
                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, this.pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = Math.pow(x, parseFactor())
                return x
            }
        }.parse()
    }

    private fun trySolveArithmetic(msg: String): String? {
        val mathRegex = Regex("[0-9+\\-*/().^ ]+")
        val matches = mathRegex.findAll(msg)
        val bestMatch = matches
            .map { it.value.trim() }
            .filter { it.contains("+") || it.contains("-") || it.contains("*") || it.contains("/") || it.contains("^") }
            .maxByOrNull { it.length } ?: return null
            
        if (bestMatch.length < 3) return null
        if (bestMatch.matches(Regex("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})|(\\d{1,2}[-/]\\d{1,2}[-/]\\d{4})"))) return null
        return try {
            val result = evaluateArithmetic(bestMatch)
            "🧮 **Arithmetic Calculation:**\n\n- Expression: `$bestMatch`\n- Calculated Result: **$result**"
        } catch (e: Exception) {
            null
        }
    }

    private fun trySolveTrig(msg: String): String? {
        val norm = msg.lowercase(Locale.getDefault()).replace(" ", "").replace("°", "")
        val trigRegex = Regex("(sin|cos|tan)(30|45|60|90|0)")
        val match = trigRegex.find(norm) ?: return null
        val func = match.groupValues[1]
        val valDeg = match.groupValues[2]
        
        return when (func) {
            "sin" -> when (valDeg) {
                "0" -> "📐 **Trigonometry:**\n\n- sin(0°) = 0"
                "30" -> "📐 **Trigonometry:**\n\n- sin(30°) = 1/2 = 0.5"
                "45" -> "📐 **Trigonometry:**\n\n- sin(45°) = 1/√2 ≈ 0.707"
                "60" -> "📐 **Trigonometry:**\n\n- sin(60°) = √3/2 ≈ 0.866"
                "90" -> "📐 **Trigonometry:**\n\n- sin(90°) = 1"
                else -> null
            }
            "cos" -> when (valDeg) {
                "0" -> "📐 **Trigonometry:**\n\n- cos(0°) = 1"
                "30" -> "📐 **Trigonometry:**\n\n- cos(30°) = √3/2 ≈ 0.866"
                "45" -> "📐 **Trigonometry:**\n\n- cos(45°) = 1/√2 ≈ 0.707"
                "60" -> "📐 **Trigonometry:**\n\n- cos(60°) = 1/2 = 0.5"
                "90" -> "📐 **Trigonometry:**\n\n- cos(90°) = 0"
                else -> null
            }
            "tan" -> when (valDeg) {
                "0" -> "📐 **Trigonometry:**\n\n- tan(0°) = 0"
                "30" -> "📐 **Trigonometry:**\n\n- tan(30°) = 1/√3 ≈ 0.577"
                "45" -> "📐 **Trigonometry:**\n\n- tan(45°) = 1"
                "60" -> "📐 **Trigonometry:**\n\n- tan(60°) = √3 ≈ 1.732"
                "90" -> "📐 **Trigonometry:**\n\n- tan(90°) = Undefined"
                else -> null
            }
            else -> null
        }
    }
    private fun trySolveMatrix(msg: String): String? {
        if (!msg.contains("determinant") && !msg.contains("matrix")) return null
        val numbers = Regex("-?\\d+\\.?\\d*").findAll(msg).map { it.value.toDoubleOrNull() }.toList()
        if (numbers.size >= 4) {
            val a = numbers[0] ?: 0.0
            val b = numbers[1] ?: 0.0
            val c = numbers[2] ?: 0.0
            val d = numbers[3] ?: 0.0
            
            val det = a * d - b * c
            return """
            🧮 **2x2 Matrix Determinant Solver:**
            
            Matrix:
            | $a   $b |
            | $c   $d |
            
            1. **Formula:** det(A) = ad - bc
            2. **Calculation:** det(A) = ($a × $d) - ($b × $c)
            3. **Result:** det(A) = ${a*d} - ${b*c} = **$det**
            """.trimIndent()
        }
        return null
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                if (s1[i - 1] == s2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j - 1], dp[j], prev) + 1
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }

    private fun fuzzyMatch(msg: String, vararg keywords: String): Boolean {
        val cleanMsg = msg.lowercase(Locale.getDefault())
            .replace(Regex("[^a-zA-Z0-9\\s\u0B80-\u0BFF\u0C00-\u0C7F]"), "")
        val words = cleanMsg.split(Regex("\\s+")).filter { it.isNotBlank() }
        for (keyword in keywords) {
            val kw = keyword.lowercase(Locale.getDefault())
            if (cleanMsg.contains(kw)) return true
            for (word in words) {
                if (word == kw || word.contains(kw) || kw.contains(word)) return true
                val limit = when {
                    kw.length >= 6 -> 2
                    kw.length >= 4 -> 1
                    else -> 0
                }
                if (limit > 0 && Math.abs(word.length - kw.length) <= limit) {
                    if (levenshtein(word, kw) <= limit) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun synthesizeResponse(msg: String, isTamil: Boolean, isTelugu: Boolean, isHinglish: Boolean): String? {
        val lower = msg.lowercase(Locale.getDefault())
        
        // 1. Math Topics
        val mathTopics = mapOf(
            "probability" to Pair(
                "Probability is the measure of the likelihood that an event will occur, ranging from 0 (impossible) to 1 (certain). The formula is: \\(P(A) = \\frac{\\text{Favorable Outcomes}}{\\text{Total Outcomes}}\\). Key concepts include independent events, mutual exclusivity, and Bayes' Theorem.",
                "Probability kisi event ke hone ke chance ko kehte hain (0 se 1 ke beech). Formula: \\(P(A) = \\frac{\\text{Favorable Outcomes}}{\\text{Total Outcomes}}\\). Jaise coin toss mein head aane ki probability 0.5 hoti hai."
            ),
            "statistics" to Pair(
                "Statistics involves collecting, analyzing, interpreting, and presenting data. Key metrics include Mean (average), Median (middle value), Mode (most frequent value), and Standard Deviation (measure of data spread).",
                "Statistics ka matlab hai data ko collect aur analyze karna. Iske main terms hain: Mean (average), Median (beech ki value), Mode (jo sabse zyada baar aaye), aur Standard Deviation (data kitna spread hai)."
            ),
            "vector" to Pair(
                "A Vector is a quantity having both magnitude and direction, represented by an arrow. Examples include velocity and force. In 3D space, it is written as \\(\\mathbf{v} = a\\hat{i} + b\\hat{j} + c\\hat{k}\\).",
                "Vector aisi quantity hai jisme magnitude (size) aur direction (disha) dono hote hain. Jaise velocity aur force. Isse \\(\\mathbf{v} = a\\hat{i} + b\\hat{j} + c\\hat{k}\\) se represent karte hain."
            )
        )
        
        // 2. Physics Topics
        val physicsTopics = mapOf(
            "relativity" to Pair(
                "Einstein's Theory of Relativity consists of Special Relativity (laws of physics are the same for all non-accelerating observers, speed of light is constant) and General Relativity (gravity is the bending of spacetime by mass).",
                "Einstein ki Relativity theory ke do parts hain: Special Relativity (speed of light constant hai) aur General Relativity (gravity spacetime curvature hai jo mass ki wajah se hota hai)."
            ),
            "sound" to Pair(
                "Sound is a mechanical wave that is an oscillation of pressure transmitted through a solid, liquid, or gas. It travels fastest in solids and cannot travel through a vacuum.",
                "Sound ek mechanical wave hai jo molecules ke vibration se travel karti hai. Ye solids mein sabse fast chalti hai aur vacuum (khali jagah) mein travel nahi kar sakti."
            ),
            "wave" to Pair(
                "A Wave is a disturbance that transfers energy from one point to another without transferring matter. Types include transverse waves (e.g., light) and longitudinal waves (e.g., sound).",
                "Wave energy ko ek jagah se doosri jagah transfer karti hai bina matter transfer kiye. Jaise transverse waves (light) aur longitudinal waves (sound)."
            )
        )
        
        // 3. Chemistry Topics
        val chemistryTopics = mapOf(
            "bonding" to Pair(
                "Chemical bonding refers to the attraction between atoms that enables the formation of chemical compounds. Main types: Ionic (transfer of electrons), Covalent (sharing of electrons), and Metallic bonds.",
                "Chemical bonding atoms ke beech attraction ko kehte hain jo compounds banate hain. Types: Ionic bond (electron transfer), Covalent bond (electron sharing), aur Metallic bond."
            ),
            "periodic" to Pair(
                "The Periodic Table organizes chemical elements by atomic number, electron configuration, and recurring chemical properties. Rows are called Periods and columns are called Groups.",
                "Periodic Table chemical elements ko unke atomic number ke bases par arrange karti hai. Horizontal rows ko Periods aur vertical columns ko Groups kehte hain."
            )
        )
        
        // 4. Computer Science
        val csTopics = mapOf(
            "database" to Pair(
                "A Database is an organized collection of structured data, typically stored and accessed electronically. Relational databases (SQL) use tables, while Non-relational databases (NoSQL) use documents or graphs.",
                "Database data ko arrange kar ke store karne ka system hai. SQL databases tables ka use karte hain aur NoSQL databases documents ya graphs ka use karte hain."
            ),
            "recursion" to Pair(
                "Recursion is a programming technique where a function calls itself directly or indirectly to solve a problem by breaking it down into smaller sub-problems. It requires a base case to prevent infinite loops.",
                "Recursion ek programming technique hai jisme function khud ko call karta hai kisi badi problem ko choti sub-problems mein todne ke liye. Isme infinite loop se bachne ke liye base case zaroori hota hai."
            ),
            "git" to Pair(
                "Git is a distributed version control system that tracks changes in source code during software development, allowing multiple developers to collaborate without overwriting each other's work.",
                "Git ek version control system hai jo code change tracks rakhta hai. Isse team collaborate kar ke kaam kar sakti hai bina code overwrite kiye."
            )
        )
        
        // 5. General / Philosophy
        val generalTopics = mapOf(
            "stoicism" to Pair(
                "Stoicism is a school of Hellenistic philosophy that teaches the development of self-control and fortitude as a means of overcoming destructive emotions, focusing only on what is in our control.",
                "Stoicism ek philosophy hai jo self-control aur calmness sikhati hai. Iska main rule hai ki hume sirf un cheezon par focus karna chahiye jo humare control mein hain, baaki sab chhod dena chahiye."
            ),
            "career" to Pair(
                "Career building requires a balance of hard skills (technical knowledge) and soft skills (communication, empathy). Focus on internships, project building, and continuous learning to stay ahead.",
                "Career banane ke liye hard skills (technical gyaan) aur soft skills (communication, team work) dono zaroori hain. Projects banayein aur continuous learning par dhyan dein."
            ),
            "history" to Pair(
                "History is the study of past events, civilizations, and historical figures. Understanding history helps us learn from mistakes, understand human culture development, and predict future trends.",
                "History beete hue samay aur sabhyataon (civilizations) ka adhyayan hai. History padhne se hum past ki mistakes se seekh sakte hain aur aaj ke samaj ko behtar samajh sakte hain."
            )
        )
        
        // Match math
        for ((key, pair) in mathTopics) {
            if (lower.contains(key)) return formatSubjectResponse("Maths", key, pair, isTamil, isTelugu, isHinglish)
        }
        // Match physics
        for ((key, pair) in physicsTopics) {
            if (lower.contains(key)) return formatSubjectResponse("Physics", key, pair, isTamil, isTelugu, isHinglish)
        }
        // Match chemistry
        for ((key, pair) in chemistryTopics) {
            if (lower.contains(key)) return formatSubjectResponse("Chemistry", key, pair, isTamil, isTelugu, isHinglish)
        }
        // Match cs
        for ((key, pair) in csTopics) {
            if (lower.contains(key)) return formatSubjectResponse("Computer Science", key, pair, isTamil, isTelugu, isHinglish)
        }
        // Match general
        for ((key, pair) in generalTopics) {
            if (lower.contains(key)) return formatSubjectResponse("General Knowledge", key, pair, isTamil, isTelugu, isHinglish)
        }
        
        return null
    }

    private fun formatSubjectResponse(subject: String, topic: String, pair: Pair<String, String>, isTamil: Boolean, isTelugu: Boolean, isHinglish: Boolean): String {
        val topicCap = topic.uppercase(Locale.getDefault())
        return when {
            isTamil -> """
### 📚 $subject: $topicCap

- **Vilhakkam (Explanation)**: ${pair.first}
- **Note (Kuripu)**: Idhu unga padipula mukkiyamana topic. Thodarnthu practice pannunga! 🚀
""".trimIndent()
            isTelugu -> """
### 📚 $subject: $topicCap

- **Vivarana (Explanation)**: ${pair.first}
- **Note (Gamanika)**: Idhi mee study lo chala important. Practice chesthu undandi! 🚀
""".trimIndent()
            isHinglish -> """
### 📚 $subject: $topicCap

- **Explanation**: ${pair.second}
- **Tip**: Ye ek important topic hai. Iske notes bana kar revise karte rahein! 🚀
""".trimIndent()
            else -> """
### 📚 $subject: $topicCap

- **Explanation**: ${pair.first}
- **Tip**: Keep this concept clear, it's highly important for examinations! 🚀
""".trimIndent()
        }
    }

    private fun generateAntiDistractionResponse(
        msg: String,
        isTamil: Boolean,
        isTelugu: Boolean,
        isHinglish: Boolean
    ): String {
        // Detect Activity
        val activity = when {
            fuzzyMatch(msg, "game", "gaming", "pubg", "freefire") -> "gaming"
            fuzzyMatch(msg, "shopping", "myntra", "amazon", "flipkart") -> "online shopping"
            fuzzyMatch(msg, "whatsapp", "chatting", "chat") -> "whatsapp"
            fuzzyMatch(msg, "tiktok", "reels", "shorts", "scroll", "scrolling") -> "scrolling/reels"
            fuzzyMatch(msg, "youtube", "yt") -> "youtube"
            fuzzyMatch(msg, "instagram", "insta") -> "instagram"
            fuzzyMatch(msg, "facebook", "fb") -> "facebook"
            fuzzyMatch(msg, "porn", "pornography", "adult") -> "pornography"
            fuzzyMatch(msg, "procrastination", "procrastinate", "talna", "delay") -> "procrastination"
            fuzzyMatch(msg, "movie", "movies", "netflix", "prime") -> "movies"
            else -> "entertainment/timepass"
        }

        // Detect Duration
        val numberMatch = Regex("\\d+").find(msg)?.value ?: "some"
        val unit = when {
            msg.contains("din") || msg.contains("day") -> "day(s)"
            msg.contains("ghante") || msg.contains("hour") -> "hour(s)"
            msg.contains("minute") || msg.contains("min") -> "minute(s)"
            msg.contains("weekend") -> "weekend(s)"
            else -> "time"
        }
        val duration = if (numberMatch == "some") "a lot of" else "$numberMatch $unit"

        // Random Tone selection
        val tone = listOf("strict", "empathetic", "sarcastic").random()

        return when {
            isTamil -> when (tone) {
                "strict" -> "🚨 **Warning (Reality Check):** $duration neram $activity-la waste panniteenga! Padhikara velaya vitutu idhu thevaya? Udane phone-ah orama vechutu padhika ponga! 😡"
                "empathetic" -> "🤝 **Friendly Coaching:** $activity-la $duration neram waste aana vishayatha nenachu kavalai padadhiga. Ippo phone-ah switch off panitu, oru 20 mins padhikka start pannunga. Ungalaala mudiyum! 🌟"
                else -> "🎭 **Sarcastic Joke:** Romba sandhosham! $activity-la $duration neram spend pannadhuku seekramae ungalku oru award thara poranga! Paritchai varudhu, olunga padhikka ponga. 😂"
            }
            isTelugu -> when (tone) {
                "strict" -> "🚨 **Strict Warning (Reality Check):** $activity kosam $duration waste chesava? Chadhuvu pakkana petti time waste chesthe future undadhu. Ventane phone pakkana petti chadhuvuko! 😡"
                "empathetic" -> "🤝 **Friendly Advice (Coaching):** $activity lo $duration time waste ayyindhani tension padaku. Ippudaina phone pakkana petti, just 20 minutes chadhuvuko. Meeru chadhuvagalaru! 🌟"
                else -> "🎭 **Sarcastic Comment:** Wah! $activity lo $duration samayama? Tvaraloni meeku gold medal ravadam guarantee! Exams vasthunnai, velli chadhuvuko. 😂"
            }
            isHinglish -> when (tone) {
                "strict" -> "🚨 **Reality Check (Strict):** Bhai $duration $activity mein uda diye? Dimaag theek hai? Padhai-likhai chhod ke bas timepass karna hai? Phone side rakh aur abhi padhne baith! 😡"
                "empathetic" -> "🤝 **Friendly Advice (Empathetic):** Wasting $duration on $activity happens to the best of us. Padhai shuru karne ka mann nahi kar raha? Don't worry, app band karo aur bas 15 minutes padho. Start now! 🌟"
                else -> "🎭 **Sarcasm (Funny):** Wah! $duration on $activity? Pro level waste of time! Exam mein organic answers likhne ka record banega lagta hai. Close the app now! 😂"
            }
            else -> when (tone) {
                "strict" -> "🚨 **Strict Alert:** Wasted $duration on $activity? This is a critical risk to your goals! Close the distraction immediately and focus on your workspace tasks now. 😡"
                "empathetic" -> "🤝 **Coaching Advice:** Wasting $duration on $activity can feel exhausting. Take a deep breath, exit the app, drink a glass of water, and tackle just one study task. Let's recover! 🌟"
                else -> "🎭 **Witty Sarcasm:** Impressive! Wasting $duration on $activity is a professional skill. Hopefully, exams test you on this. Otherwise, close the app and open your books! 😂"
            }
        }
    }

    private fun extractDateFromMessage(msg: String): String? {
        val lower = msg.lowercase(Locale.getDefault())
        
        // 1. Check for yyyy-MM-dd format (e.g., 2026-07-15 or 2026-7-5)
        val yyyyMMddRegex = Regex("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b")
        val match = yyyyMMddRegex.find(lower)
        if (match != null) {
            val parts = match.value.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].padStart(2, '0')
                val day = parts[2].padStart(2, '0')
                return "$year-$month-$day"
            }
        }
        
        // 2. Check for dd/MM/yyyy or dd-MM-yyyy or d/M/yyyy
        val ddMMyyyyRegex = Regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b")
        val match2 = ddMMyyyyRegex.find(lower)
        if (match2 != null) {
            val parts = match2.value.split(Regex("[-/.]"))
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                return "$year-$month-$day"
            }
        }

        // 3. Check for dd/MM or dd-MM
        val ddMMRegex = Regex("\\b(\\d{1,2})[-/](\\d{1,2})\\b")
        val match3 = ddMMRegex.find(lower)
        if (match3 != null) {
            val parts = match3.value.split(Regex("[-/]"))
            if (parts.size == 2) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                return "$year-$month-$day"
            }
        }
        
        // 4. Check for textual month patterns (e.g., "12th july", "july 12", "12 july")
        val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        for (i in months.indices) {
            val m = months[i]
            if (lower.contains(m)) {
                val numMatch = Regex("\\b\\d{1,2}(?:st|nd|rd|th)?\\b").find(lower)?.value?.replace(Regex("[a-z]"), "")
                if (numMatch != null) {
                    val day = numMatch.padStart(2, '0')
                    val month = (i + 1).toString().padStart(2, '0')
                    val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                    return "$year-$month-$day"
                }
            }
        }
        
        return null
    }

    private fun getMultilingualAcademicResponse(
        msg: String,
        tasksList: List<TaskEntity>,
        attendanceList: List<AttendanceEntity>,
        examsList: List<ExamEntity>,
        habitsList: List<HabitEntity>,
        expensesList: List<ExpenseEntity>,
        notesList: List<NoteEntity>,
        coursesList: List<com.ai_assistant.studentfocus.models.CgpaCourseEntity>,
        decksList: List<com.ai_assistant.studentfocus.models.DeckEntity>,
        routineList: List<com.ai_assistant.studentfocus.models.RoutineActivityEntity>,
        timeTableList: List<com.ai_assistant.studentfocus.models.TimeTableSlotEntity>,
        aiLangSetting: String
    ): String {
        // Try solving math queries dynamically first
        val quadSolved = parseAndSolveQuadratic(msg)
        if (quadSolved != null) return quadSolved
        
        val calcSolved = solveCalculus(msg)
        if (calcSolved != null) return calcSolved
        
        val trigSolved = trySolveTrig(msg)
        if (trigSolved != null) return trigSolved
        
        val matrixSolved = trySolveMatrix(msg)
        if (matrixSolved != null) return matrixSolved
        
        val arithSolved = trySolveArithmetic(msg)
        if (arithSolved != null) return arithSolved

        // Detect language based on selection or auto-detect keywords
        val isTamil = aiLangSetting == "tamil_english" || (aiLangSetting == "auto" && fuzzyMatch(msg, "vanakkam", "epdi", "iruka", "irukinga", "nalla", "saptiya", "enna pandra", "inniku", "naalai", "padikkanum", "kavalai", "bayama", "thookkam", "kasu", "tanglish", "tamilenglish", "வணக்கம்", "எப்படி", "நன்றி", "நாளை", "இன்று", "தேர்வு", "வேலை"))
        val isTelugu = aiLangSetting == "telugu_english" || (aiLangSetting == "auto" && fuzzyMatch(msg, "namaste", "unnav", "bagunnava", "bagundha", "em chestunnav", "eroju", "ee roju", "repu", "pani", "pariksha", "chadavali", "bhayam", "dabbulu", "telugish", "teluguenglish", "నమస్తే", "ఎలా", "ధన్యవాదాలు", "రేపు", "నేడు", "పరీక్ష", "పని"))
        val isHinglish = aiLangSetting == "hinglish" || (aiLangSetting == "auto" && fuzzyMatch(msg, "kaise", "yaar", "haal", "padhai", "tension", "samjhao", "batao", "hai", "kya", "kal", "aaj", "mann", "chutkula", "kaam", "paise"))

        // Topic & Workspace flags
        val isGreeting = fuzzyMatch(msg, "hello", "hi", "hey", "namaste", "vanakkam", "namaskaram", "wassup", "kaise ho", "kya haal", "yo", "hola")
        val isIdentity = fuzzyMatch(msg, "who are you", "your name", "tum kaun ho", "nee yaar", "nuvvu evaru", "who built you", "creator", "naam kya")
        val isStress = fuzzyMatch(msg, "sad", "lonely", "depressed", "stress", "tension", "fear", "mann nahi", "darr", "fail", "burnout", "depression", "tired", "thak gaya")
        val isFocus = fuzzyMatch(msg, "how to study", "study tips", "focus", "memorize", "recall", "feynman", "spaced", "time management", "study technique")
        val isSpace = fuzzyMatch(msg, "space", "universe", "black hole", "mars", "star", "science fact", "fact", "earth", "dinosaur")
        val isTech = fuzzyMatch(msg, "technology", "future", "programming", "coding", "software", "artificial intelligence", "ai", "git", "python", "java")
        val isSports = fuzzyMatch(msg, "sports", "cricket", "football", "movie", "hobby", "hobbies", "song", "music", "books", "game")
        val isJoke = fuzzyMatch(msg, "joke", "riddle", "chutkula", "make me laugh", "funny")

        // Subject flags
        val isMath = fuzzyMatch(msg, "math", "integration", "differentiation", "calculus", "trigonometry", "matrix", "algebra", "limit", "statistics")
        val isPhysics = fuzzyMatch(msg, "physics", "newton", "thermodynamics", "optics", "quantum", "force", "friction")
        val isChemistry = fuzzyMatch(msg, "chemistry", "organic", "periodic", "bonding", "acid", "base", "chemical", "pH", "gas")
        val isCS = fuzzyMatch(msg, "computer", "programming", "data structure", "python", "java", "c++", "sql", "linked list", "recursion", "git", "oop", "big-o")
        val isMotivation = fuzzyMatch(msg, "stressed", "burnout", "motivation", "tired", "study tips", "depression", "tension", "mann nahi", "thak gaya")

        // Dynamic Workspace Triggers
        val isTaskQuery = fuzzyMatch(msg, "task", "work", "target", "kaam", "@task")
        val isExamQuery = fuzzyMatch(msg, "exam", "countdown", "pariksha", "@exam")
        val isAttendanceQuery = fuzzyMatch(msg, "attendance", "present", "absent", "@attendance", "@subject")
        val isLedgerQuery = fuzzyMatch(msg, "ledger", "expense", "money", "rupees", "kharcha", "budget", "income", "@ledger", "@expense", "@money")
        val isNoteQuery = fuzzyMatch(msg, "note", "notes", "scratchpad", "@note", "@notes")
        val isHabitQuery = fuzzyMatch(msg, "habit", "habits", "@habit", "@habits")
        val isCourseQuery = fuzzyMatch(msg, "course", "courses", "gpa", "cgpa", "@course", "@cgpa", "@courses")
        val isDeckQuery = fuzzyMatch(msg, "deck", "decks", "flashcard", "flashcards", "@card", "@cards", "@flashcard", "@flashcards")
        val isRoutineQuery = fuzzyMatch(msg, "routine", "time audit", "24 hour", "24 hours", "dinacharya", "routine dikhao", "routine dekh", "show routine", "my routine", "daily schedule", "routine status", "@routine", "@schedule")
        val isTimeTableQuery = fuzzyMatch(msg, "@timetable", "timetable", "time table", "class schedule", "periods", "lecture", "lectures", "kaunsi class", "classes")

        // Target Date Resolution
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val isTomorrowQuery = fuzzyMatch(msg, "tomorrow", "kal", "naalai", "repu")
        val isTodayQuery = fuzzyMatch(msg, "today", "aaj", "indru", "nedu", "inniku", "inniki", "eroju")
        val extractedDate = extractDateFromMessage(msg)

        val targetDate = when {
            isTomorrowQuery -> tomorrowStr
            isTodayQuery -> todayStr
            else -> extractedDate
        }

        // ──────────────── TIME, DATE & DAY QUERIES ────────────────
        val isDirectTimeQuery = fuzzyMatch(
            msg,
            "what is the time", "what time is it", "what time is now", "current time", "tell me time", "tell time",
            "time kya hai", "kya time", "time batao", "samay kya hai", "kya samay", "samay batao", "kitna time",
            "time entha", "ippudu time", "time emiti",
            "time enna", "ippo enna time", "neram enna",
            "time please", "the time", "exact time", "live time"
        ) || (fuzzyMatch(msg, "time", "samay", "neram") && fuzzyMatch(msg, "what", "kya", "enna", "entha", "batao", "tell", "show", "ippudu", "ippo", "aaj", "today", "now", "abhi"))

        val isDirectDateQuery = fuzzyMatch(
            msg,
            "what is the date", "what date is today", "what is today's date", "today's date", "today date", "todays date",
            "date kya hai", "aaj kya date hai", "aaj kaun si date", "aaj ki date", "taarikh kya hai", "aaj ki taarikh", "date batao",
            "eroju date", "date emiti", "ee roju date",
            "inniku date", "inniki date", "date enna",
            "current date", "tell me date", "date please"
        ) || (fuzzyMatch(msg, "date", "taarikh") && fuzzyMatch(msg, "what", "kya", "enna", "emiti", "batao", "tell", "show", "aaj", "today", "today's", "now", "abhi") && !fuzzyMatch(msg, "task", "exam", "note", "expense", "agenda", "schedule"))

        val isDirectDayQuery = fuzzyMatch(
            msg,
            "what day is today", "what is today", "which day is today", "which day is it", "what day is it",
            "aaj kaun sa din hai", "aaj kya din hai", "aaj konsa din", "aaj kaunsa day", "aaj konsa day", "din kaun sa hai", "day kya hai",
            "eroju emi varam", "ee roju varam", "eroju varam", "day emiti",
            "inniku enna kizhamai", "inniki enna kizhamai", "kizhamai enna", "day enna",
            "current day", "tell me day"
        ) || (fuzzyMatch(msg, "day", "din", "kizhamai", "varam") && fuzzyMatch(msg, "what", "which", "kya", "kaun", "konsa", "enna", "emi", "batao", "tell", "aaj", "today") && !fuzzyMatch(msg, "task", "exam", "note", "expense", "agenda", "schedule"))

        val isDateTimeDayCombined = (isDirectTimeQuery && isDirectDateQuery) || (isDirectDateQuery && isDirectDayQuery) || fuzzyMatch(msg, "date time", "time date", "date day", "day time", "time date day")

        if (isDateTimeDayCombined || isDirectTimeQuery || isDirectDateQuery || isDirectDayQuery) {
            val now = Date()
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(now)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(now)
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)

            if (isDateTimeDayCombined) {
                return when {
                    isTamil -> "⏰ **Inniku Date, Day & Time:**\n\n- 📅 **Date:** $dateFormat\n- 🗓️ **Kizhamai (Day):** $dayFormat\n- ⏰ **Time:** $timeFormat"
                    isTelugu -> "⏰ **Eroju Date, Day & Time:**\n\n- 📅 **Date:** $dateFormat\n- 🗓️ **Varam (Day):** $dayFormat\n- ⏰ **Time:** $timeFormat"
                    isHinglish -> "⏰ **Aaj ki Date, Day aur Time:**\n\n- 📅 **Date:** $dateFormat\n- 🗓️ **Din:** $dayFormat\n- ⏰ **Samay:** $timeFormat"
                    else -> "⏰ **Current Date, Day & Time:**\n\n- 📅 **Date:** $dateFormat\n- 🗓️ **Day:** $dayFormat\n- ⏰ **Time:** $timeFormat"
                }
            } else if (isDirectTimeQuery) {
                return when {
                    isTamil -> "⏰ **Ippo Time:** $timeFormat\n\n*(Inniku $dayFormat, $dateFormat)*"
                    isTelugu -> "⏰ **Ippudu Time:** $timeFormat\n\n*(Eroju $dayFormat, $dateFormat)*"
                    isHinglish -> "⏰ **Abhi ka Samay:** $timeFormat\n\n*(Aaj $dayFormat, $dateFormat hai)*"
                    else -> "⏰ **Current Time:** $timeFormat\n\n*(Today is $dayFormat, $dateFormat)*"
                }
            } else if (isDirectDateQuery) {
                return when {
                    isTamil -> "📅 **Inniku Date:** $dateFormat\n\n*(Kizhamai: $dayFormat | Time: $timeFormat)*"
                    isTelugu -> "📅 **Eroju Date:** $dateFormat\n\n*(Varam: $dayFormat | Time: $timeFormat)*"
                    isHinglish -> "📅 **Aaj ki Date:** $dateFormat\n\n*(Din: $dayFormat | Samay: $timeFormat)*"
                    else -> "📅 **Today's Date:** $dateFormat\n\n*(Day: $dayFormat | Time: $timeFormat)*"
                }
            } else {
                return when {
                    isTamil -> "🗓️ **Inniku Kizhamai (Day):** $dayFormat ($dateFormat)\n\n*(Ippo Time: $timeFormat)*"
                    isTelugu -> "🗓️ **Eroju Varam (Day):** $dayFormat ($dateFormat)\n\n*(Ippudu Time: $timeFormat)*"
                    isHinglish -> "🗓️ **Aaj ka Din:** $dayFormat ($dateFormat)\n\n*(Abhi ka Samay: $timeFormat)*"
                    else -> "🗓️ **Today is:** $dayFormat ($dateFormat)\n\n*(Current Time: $timeFormat)*"
                }
            }
        }

        val isTomorrowDateDayQuery = isTomorrowQuery && (fuzzyMatch(msg, "date", "day", "din", "kizhamai", "varam", "taarikh") && !fuzzyMatch(msg, "task", "exam", "note", "expense", "agenda", "schedule"))
        if (isTomorrowDateDayQuery) {
            val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowDateFormatted = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(tomorrowCal.time)
            val tomorrowDayFormatted = SimpleDateFormat("EEEE", Locale.getDefault()).format(tomorrowCal.time)

            return when {
                isTamil -> "🗓️ **Naalai (Tomorrow):** $tomorrowDayFormatted ($tomorrowDateFormatted)"
                isTelugu -> "🗓️ **Repu (Tomorrow):** $tomorrowDayFormatted ($tomorrowDateFormatted)"
                isHinglish -> "🗓️ **Kal (Tomorrow):** $tomorrowDayFormatted ($tomorrowDateFormatted)"
                else -> "🗓️ **Tomorrow is:** $tomorrowDayFormatted ($tomorrowDateFormatted)"
            }
        }

        // ──────────────── SEMANTIC & VECTOR RETRIEVAL FOR TOPICS & NOTES ────────────────
        val retrievedNotes = retrieveRelevantNotes(msg, notesList)
        val topMatch = retrievedNotes.firstOrNull()

        if (topMatch != null && (topMatch.second >= 3.0 || isNoteQuery) && targetDate == null) {
            val matchingNotes = retrievedNotes.take(4).map { it.first }
            val primaryTopic = topMatch.first.topic.ifBlank { "Study Notes" }

            return when {
                isTamil -> "📖 **'$primaryTopic' patriya kurippugal (Retrieved Notes):**\n\n" +
                        matchingNotes.joinToString("\n\n") { note ->
                            val t = if (note.topic.isNotBlank()) "📌 **Topic:** ${note.topic} " else ""
                            "$t(📅 ${note.date})\n> ${note.content}"
                        }
                isTelugu -> "📖 **'$primaryTopic' gurinchi notes (Retrieved Notes):**\n\n" +
                        matchingNotes.joinToString("\n\n") { note ->
                            val t = if (note.topic.isNotBlank()) "📌 **Topic:** ${note.topic} " else ""
                            "$t(📅 ${note.date})\n> ${note.content}"
                        }
                isHinglish -> "📖 **'$primaryTopic' ke notes me yeh mila:**\n\n" +
                        matchingNotes.joinToString("\n\n") { note ->
                            val t = if (note.topic.isNotBlank()) "📌 **Topic:** ${note.topic} " else ""
                            "$t(📅 ${note.date})\n> ${note.content}"
                        } + "\n\n💡 *Aap is topic se related koi aur sawal bhi pooch sakte hain!*"
                else -> "📖 **Retrieved Knowledge for '$primaryTopic':**\n\n" +
                        matchingNotes.joinToString("\n\n") { note ->
                            val t = if (note.topic.isNotBlank()) "📌 **Topic:** ${note.topic} " else ""
                            "$t(📅 ${note.date})\n> ${note.content}"
                        } + "\n\n💡 *Ask any questions or request summaries of this topic!*"
            }
        }

        // ──────────────── KEYWORD SEARCH FOR TASKS ────────────────
        if (isTaskQuery && targetDate == null) {
            val cleanQuery = msg.replace("show", "")
                                .replace("tasks", "")
                                .replace("task", "")
                                .replace("get", "")
                                .replace("search", "")
                                .trim()
            if (cleanQuery.length >= 3) {
                val matchingTasks = tasksList.filter { it.title.lowercase().contains(cleanQuery) }
                if (matchingTasks.isNotEmpty()) {
                    return when {
                        isTamil -> "📌 **'$cleanQuery' matching tasks:**\n\n" + matchingTasks.joinToString("\n") { "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority})" }
                        isTelugu -> "📌 **'$cleanQuery' matching tasks:**\n\n" + matchingTasks.joinToString("\n") { "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority})" }
                        isHinglish -> "📌 **'$cleanQuery' se match hone wale tasks:**\n\n" + matchingTasks.joinToString("\n") { "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority})" }
                        else -> "📌 **Tasks matching '$cleanQuery':**\n\n" + matchingTasks.joinToString("\n") { "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority})" }
                    }
                }
            }
        }

        // ──────────────── UNIFIED DATE AGENDA QUERY ────────────────
        if (targetDate != null) {
            val dateTasks = tasksList.filter { it.dueDate == targetDate }
            val dateExams = examsList.filter { it.date == targetDate }
            val dateNotes = notesList.filter { it.date == targetDate }
            val dateExpenses = expensesList.filter { it.date == targetDate }

            // If specifically asking for notes on a date
            if (isNoteQuery) {
                return when {
                    isTamil -> "📓 **$targetDate ku kurippugal (Notes):**\n\n" + (if (dateNotes.isEmpty()) "- Indha dharla notes edhum illa." else dateNotes.joinToString("\n") { "- *${it.content}*" })
                    isTelugu -> "📓 **$targetDate ku notes:**\n\n" + (if (dateNotes.isEmpty()) "- Ee date roju notes emi levu." else dateNotes.joinToString("\n") { "- *${it.content}*" })
                    isHinglish -> "📓 **$targetDate ke study notes:**\n\n" + (if (dateNotes.isEmpty()) "- Is date ko koi notes nahi hain." else dateNotes.joinToString("\n") { "- *${it.content}*" })
                    else -> "📓 **Notes for $targetDate:**\n\n" + (if (dateNotes.isEmpty()) "- No notes saved for this date." else dateNotes.joinToString("\n") { "- *${it.content}*" })
                }
            }

            // If specifically asking for tasks on a date
            if (isTaskQuery) {
                val activeTasks = dateTasks.filter { !it.isCompleted }
                val completedTasks = dateTasks.filter { it.isCompleted }
                return when {
                    isTamil -> "📌 **$targetDate ku velai (Tasks):**\n\n" + 
                            (if (dateTasks.isEmpty()) "- Indha dharla tasks edhum illa.\n" else {
                                "Pending:\n" + (if (activeTasks.isEmpty()) "- No pending tasks.\n" else activeTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n" +
                                "Completed:\n" + (if (completedTasks.isEmpty()) "- No completed tasks.\n" else completedTasks.joinToString("\n") { "- ~${it.title}~" })
                            })
                    isTelugu -> "📌 **$targetDate ku tasks:**\n\n" + 
                            (if (dateTasks.isEmpty()) "- Ee date roju tasks emi levu.\n" else {
                                "Pending:\n" + (if (activeTasks.isEmpty()) "- No pending tasks.\n" else activeTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n" +
                                "Completed:\n" + (if (completedTasks.isEmpty()) "- No completed tasks.\n" else completedTasks.joinToString("\n") { "- ~${it.title}~" })
                            })
                    isHinglish -> "📌 **$targetDate ke tasks:**\n\n" + 
                            (if (dateTasks.isEmpty()) "- Is date ko koi tasks nahi hain.\n" else {
                                "Pending (Bache hue):\n" + (if (activeTasks.isEmpty()) "- Koi pending tasks nahi hain.\n" else activeTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n" +
                                "Completed (Poore hue):\n" + (if (completedTasks.isEmpty()) "- Koi tasks poore nahi hue.\n" else completedTasks.joinToString("\n") { "- ~${it.title}~" })
                            })
                    else -> "📌 **Tasks for $targetDate:**\n\n" + 
                            (if (dateTasks.isEmpty()) "- No tasks scheduled for this date.\n" else {
                                "Pending:\n" + (if (activeTasks.isEmpty()) "- No pending tasks.\n" else activeTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n" +
                                "Completed:\n" + (if (completedTasks.isEmpty()) "- No completed tasks.\n" else completedTasks.joinToString("\n") { "- ~${it.title}~" })
                            })
                }
            }

            // If specifically asking for exams on a date
            if (isExamQuery) {
                return when {
                    isTamil -> "📝 **$targetDate ku parikshai (Exams):**\n\n" + (if (dateExams.isEmpty()) "- Indha dharla exams edhum illa." else dateExams.joinToString("\n") { "- **${it.name}**" })
                    isTelugu -> "📝 **$targetDate ku exams:**\n\n" + (if (dateExams.isEmpty()) "- Ee date roju exams emi levu." else dateExams.joinToString("\n") { "- **${it.name}**" })
                    isHinglish -> "📝 **$targetDate ke exams:**\n\n" + (if (dateExams.isEmpty()) "- Is date ko koi exams nahi hain." else dateExams.joinToString("\n") { "- **${it.name}**" })
                    else -> "📝 **Exams on $targetDate:**\n\n" + (if (dateExams.isEmpty()) "- No exams scheduled for this date." else dateExams.joinToString("\n") { "- **${it.name}**" })
                }
            }

            // If specifically asking for ledger/expenses on a date
            if (isLedgerQuery) {
                val totalExp = dateExpenses.filter { it.type == "expense" }.sumOf { it.amount }
                val totalInc = dateExpenses.filter { it.type == "income" }.sumOf { it.amount }
                return when {
                    isTamil -> "💰 **$targetDate ku ledger transaction details:**\n- Expenses: ₹$totalExp\n- Income: ₹$totalInc\n\n" + 
                            (if (dateExpenses.isEmpty()) "- Transactions edhum illa." else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    isTelugu -> "💰 **$targetDate ku ledger details:**\n- Expenses: ₹$totalExp\n- Income: ₹$totalInc\n\n" + 
                            (if (dateExpenses.isEmpty()) "- Transactions emi levu." else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    isHinglish -> "💰 **$targetDate ke ledger transactions:**\n- Kharch: ₹$totalExp\n- Income (Aamdani): ₹$totalInc\n\n" + 
                            (if (dateExpenses.isEmpty()) "- Is date ko koi transactions nahi hain." else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    else -> "💰 **Transactions on $targetDate:**\n- Total Expense: ₹$totalExp\n- Total Income: ₹$totalInc\n\n" + 
                            (if (dateExpenses.isEmpty()) "- No transactions found." else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                }
            }

            // General agenda for that date
            return when {
                isTamil -> "📅 **Unga $targetDate plan details:**\n\n" +
                        "**Tasks:**\n" + (if (dateTasks.isEmpty()) "- Indha dharla tasks edhum illa.\n" else dateTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n\n" +
                        "**Exams:**\n" + (if (dateExams.isEmpty()) "- Exams edhum illa.\n" else dateExams.joinToString("\n") { "- **${it.name}**" }) + "\n\n" +
                        "**Notes:**\n" + (if (dateNotes.isEmpty()) "- Notes edhum illa.\n" else dateNotes.joinToString("\n") { "- *${it.content}*" }) + "\n\n" +
                        "**Expenses:**\n" + (if (dateExpenses.isEmpty()) "- Transactions edhum illa.\n" else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" }) + "\n\n" +
                        "**Attendance Report:\n**" + (if (attendanceList.isEmpty()) "- Attendance details edhum illa.\n" else attendanceList.joinToString("\n") {
                            val total = it.presents + it.absents
                            val percent = if (total > 0) (it.presents * 100) / total else 100
                            "- **${it.subjectName}**: $percent% (${it.presents}P / ${it.absents}A)"
                        }) + "\n\n" +
                        "**Tracked Habits:\n**" + (if (habitsList.isEmpty()) "- Habits edhum illa.\n" else habitsList.joinToString("\n") {
                            val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                            "- **${it.name}**: $streak days logged"
                        }) + "\n\n" +
                        "**Courses & CGPA:\n**" + (if (coursesList.isEmpty()) "- Courses edhum illa.\n" else coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }) + "\n\n" +
                        "**Study Decks:\n**" + (if (decksList.isEmpty()) "- Decks edhum illa." else decksList.joinToString("\n") { "- **${it.name}**" })
                
                isTelugu -> "📅 **Mee $targetDate plan details:**\n\n" +
                        "**Tasks:**\n" + (if (dateTasks.isEmpty()) "- Ee date roju tasks em levu.\n" else dateTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n\n" +
                        "**Exams:**\n" + (if (dateExams.isEmpty()) "- Exams emi levu.\n" else dateExams.joinToString("\n") { "- **${it.name}**" }) + "\n\n" +
                        "**Notes:**\n" + (if (dateNotes.isEmpty()) "- Notes emi levu.\n" else dateNotes.joinToString("\n") { "- *${it.content}*" }) + "\n\n" +
                        "**Expenses:**\n" + (if (dateExpenses.isEmpty()) "- Transactions emi levu.\n" else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" }) + "\n\n" +
                        "**Attendance Report:\n**" + (if (attendanceList.isEmpty()) "- Attendance details emi levu.\n" else attendanceList.joinToString("\n") {
                            val total = it.presents + it.absents
                            val percent = if (total > 0) (it.presents * 100) / total else 100
                            "- **${it.subjectName}**: $percent% (${it.presents}P / ${it.absents}A)"
                        }) + "\n\n" +
                        "**Tracked Habits:\n**" + (if (habitsList.isEmpty()) "- Habits emi levu.\n" else habitsList.joinToString("\n") {
                            val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                            "- **${it.name}**: $streak days logged"
                        }) + "\n\n" +
                        "**Courses & CGPA:\n**" + (if (coursesList.isEmpty()) "- Courses emi levu.\n" else coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }) + "\n\n" +
                        "**Study Decks:\n**" + (if (decksList.isEmpty()) "- Decks emi levu." else decksList.joinToString("\n") { "- **${it.name}**" })
                
                isHinglish -> "📅 **$targetDate ke plans aur targets:**\n\n" +
                        "**Tasks:**\n" + (if (dateTasks.isEmpty()) "- Is date ko koi tasks nahi hain.\n" else dateTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n\n" +
                        "**Exams:**\n" + (if (dateExams.isEmpty()) "- Is date ko koi exams nahi hain.\n" else dateExams.joinToString("\n") { "- **${it.name}**" }) + "\n\n" +
                        "**Notes:**\n" + (if (dateNotes.isEmpty()) "- Is date ko koi notes nahi hain.\n" else dateNotes.joinToString("\n") { "- *${it.content}*" }) + "\n\n" +
                        "**Expenses:**\n" + (if (dateExpenses.isEmpty()) "- Is date ko koi ledger transaction nahi hai.\n" else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" }) + "\n\n" +
                        "**Attendance Summary:\n**" + (if (attendanceList.isEmpty()) "- Attendance details khali hain.\n" else attendanceList.joinToString("\n") {
                            val total = it.presents + it.absents
                            val percent = if (total > 0) (it.presents * 100) / total else 100
                            "- **${it.subjectName}**: $percent% (${it.presents} Present / ${it.absents} Absent)"
                        }) + "\n\n" +
                        "**Habits & Streaks:\n**" + (if (habitsList.isEmpty()) "- Koi habits nahi hain.\n" else habitsList.joinToString("\n") {
                            val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                            "- **${it.name}**: $streak days logged"
                        }) + "\n\n" +
                        "**Courses & CGPA:\n**" + (if (coursesList.isEmpty()) "- Koi courses active nahi hain.\n" else coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }) + "\n\n" +
                        "**Study Decks:\n**" + (if (decksList.isEmpty()) "- Koi active decks nahi hain." else decksList.joinToString("\n") { "- **${it.name}**" })
                
                else -> "📅 **Agenda for $targetDate:**\n\n" +
                        "**Tasks:**\n" + (if (dateTasks.isEmpty()) "- No tasks scheduled for this date.\n" else dateTasks.joinToString("\n") { "- **${it.title}** (${it.priority})" }) + "\n\n" +
                        "**Exams:**\n" + (if (dateExams.isEmpty()) "- No exams scheduled for this date.\n" else dateExams.joinToString("\n") { "- **${it.name}**" }) + "\n\n" +
                        "**Notes:**\n" + (if (dateNotes.isEmpty()) "- No notes saved for this date.\n" else dateNotes.joinToString("\n") { "- *${it.content}*" }) + "\n\n" +
                        "**Expenses:**\n" + (if (dateExpenses.isEmpty()) "- No ledger transactions.\n" else dateExpenses.joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" }) + "\n\n" +
                        "**Attendance Report:\n**" + (if (attendanceList.isEmpty()) "- Attendance metrics are empty.\n" else attendanceList.joinToString("\n") {
                            val total = it.presents + it.absents
                            val percent = if (total > 0) (it.presents * 100) / total else 100
                            "- **${it.subjectName}**: $percent% (${it.presents} Present / ${it.absents} Absent)"
                        }) + "\n\n" +
                        "**Tracked Habits:\n**" + (if (habitsList.isEmpty()) "- No habits tracked yet.\n" else habitsList.joinToString("\n") {
                            val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                            "- **${it.name}**: $streak days logged"
                        }) + "\n\n" +
                        "**Courses & CGPA:\n**" + (if (coursesList.isEmpty()) "- No courses added yet.\n" else coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }) + "\n\n" +
                        "**Study Decks:\n**" + (if (decksList.isEmpty()) "- No study decks found." else decksList.joinToString("\n") { "- **${it.name}**" })
            }
        }

        val synthesized = synthesizeResponse(msg, isTamil, isTelugu, isHinglish)
        if (synthesized != null) return synthesized

        val isConfession = fuzzyMatch(msg, "game", "gaming", "shopping", "whatsapp", "tiktok", "reels", "shorts", "youtube", "instagram", "facebook", "fb", "porn", "procrastination", "movie", "movies", "timepass", "barbad", "guilty", "scroll", "scrolling", "waste", "guilt", "spend", "spent")
        if (isConfession) {
            return generateAntiDistractionResponse(msg, isTamil, isTelugu, isHinglish)
        }

        // Construct response based on Language & Subject
        return when {
            // ──────────────── TAMIL RESPONSES ────────────────
            isTamil -> {
                when {
                    isGreeting -> ConversationalDataset.getGreeting(isTamil = true, isTelugu = false, isHinglish = false)
                    isIdentity -> ConversationalDataset.getIdentity(isTamil = true, isTelugu = false, isHinglish = false)
                    isStress -> ConversationalDataset.getStress(isTamil = true, isTelugu = false, isHinglish = false)
                    isFocus -> ConversationalDataset.getFocus(isTamil = true, isTelugu = false, isHinglish = false)
                    isSpace -> ConversationalDataset.getSpace(isTamil = true, isTelugu = false, isHinglish = false)
                    isTech -> ConversationalDataset.getTech(isTamil = true, isTelugu = false, isHinglish = false)
                    isJoke -> ConversationalDataset.getJoke(isTamil = true, isTelugu = false, isHinglish = false)
                    isSports -> ConversationalDataset.getSports(isTamil = true, isTelugu = false, isHinglish = false)
                    isCourseQuery -> {
                        if (coursesList.isEmpty()) "📊 **Courses edhum illa.**"
                        else "📚 **Unga courses list & GPA:\n\n**" + coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }
                    }
                    isDeckQuery -> {
                        if (decksList.isEmpty()) "🎴 **Decks edhum illa.**"
                        else "🎴 **Unga flashcard study decks:\n\n**" + decksList.joinToString("\n") { "- **${it.name}**" }
                    }
                    isTaskQuery -> {
                        val active = tasksList.filter { !it.isCompleted }
                        if (active.isEmpty()) {
                            "🎉 **Unga pending tasks edhum illa!** Ella target-aiyum mudichteenga."
                        } else {
                            "📌 **Pending Tasks (Active Tasks):\n\n**" + active.joinToString("\n") { 
                                "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority.uppercase()})" 
                            }
                        }
                    }
                    isExamQuery -> {
                        if (examsList.isEmpty()) {
                            "📅 **Exams edhum illa.** Pudhu exam settings-la add pannunga."
                        } else {
                            "📝 **Upcoming Exams:\n\n**" + examsList.joinToString("\n") {
                                "- **${it.name}**: Date: ${it.date}"
                            }
                        }
                    }
                    isAttendanceQuery -> {
                        if (attendanceList.isEmpty()) {
                            "📊 **Attendance details edhum illa.**"
                        } else {
                            "📈 **Attendance Report:\n\n**" + attendanceList.joinToString("\n") {
                                val total = it.presents + it.absents
                                val percent = if (total > 0) (it.presents * 100) / total else 100
                                "- **${it.subjectName}**: $percent% (${it.presents}P / ${it.absents}A)"
                            }
                        }
                    }
                    isLedgerQuery -> {
                        val totalExp = expensesList.filter { it.type == "expense" }.sumOf { it.amount }
                        val totalInc = expensesList.filter { it.type == "income" }.sumOf { it.amount }
                        "💰 **Financial Ledger Summary:\n- Total Expense: **₹$totalExp**\n- Total Income: **₹$totalInc**\n\n**Recent transactions:\n**" + 
                                (if (expensesList.isEmpty()) "Records edhum illa" else expensesList.take(3).joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    }
                    isRoutineQuery -> {
                        val totalAlloc = routineList.sumOf { it.durationMinutes }
                        val usefulM = routineList.filter { it.classificationScore >= 0.7f }.sumOf { it.durationMinutes }
                        val necM = routineList.filter { it.classificationScore in 0.1f..0.69f }.sumOf { it.durationMinutes }
                        val wasteM = routineList.filter { it.classificationScore == 0.0f }.sumOf { it.durationMinutes }
                        val effStudyM = routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
                        "⏱️ **24-Hour Routine Time Audit:\n\n" +
                                "- 🟢 **Invested (Useful Study):** ${usefulM / 60}h ${usefulM % 60}m\n" +
                                "- 🟡 **Necessary Routine:** ${necM / 60}h ${necM % 60}m\n" +
                                "- 🔴 **Wasted Time:** ${wasteM / 60}h ${wasteM % 60}m\n" +
                                "- ⚪ **Free / Unallocated:** ${(maxOf(0, 1440 - totalAlloc)) / 60}h ${(maxOf(0, 1440 - totalAlloc)) % 60}m\n\n" +
                                "🎯 **Daily Study Budget:** ${effStudyM / 60}h ${effStudyM % 60}m ($effStudyM mins/day)\n\n" +
                                (if (routineList.isEmpty()) "Routine edhum illa. Ledger pakkathula Routine tab-la add pannunga!" else routineList.joinToString("\n") { "- **${it.activityName}**: ${it.durationMinutes / 60}h ${it.durationMinutes % 60}m (Score: ${it.classificationScore})" })
                    }
                    isTimeTableQuery -> {
                        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                        val targetDay = when {
                            fuzzyMatch(msg, "monday", "thingal") -> "Monday"
                            fuzzyMatch(msg, "tuesday", "sevvai") -> "Tuesday"
                            fuzzyMatch(msg, "wednesday", "budhan") -> "Wednesday"
                            fuzzyMatch(msg, "thursday", "vyazhan") -> "Thursday"
                            fuzzyMatch(msg, "friday", "velli") -> "Friday"
                            fuzzyMatch(msg, "saturday", "sani") -> "Saturday"
                            fuzzyMatch(msg, "sunday", "nyayiru") -> "Sunday"
                            else -> currentDay
                        }
                        val slots = timeTableList.filter { it.dayOfWeek.equals(targetDay, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                        if (slots.isEmpty()) {
                            "📅 **$targetDay ku Time Table slots edhum illa.**\n\nClass add panna type pannunga: `@timetable @add`"
                        } else {
                            "📅 **$targetDay Time Table Schedule:\n\n**" + slots.joinToString("\n\n") {
                                "⏰ **${it.getFormattedTimeRange()}**: **${it.title}**\n📍 ${if (!it.roomOrLocation.isNullOrBlank()) it.roomOrLocation else "Regular Room"} | 🔔 ${if (it.isNotificationEnabled) "Reminder ON" else "Reminder OFF"}"
                            }
                        }
                    }
                    isNoteQuery -> {
                        if (notesList.isEmpty()) {
                            "📓 **Notes edhum illa.**"
                        } else {
                            "✍️ **Recent Notes:\n\n**" + notesList.take(4).joinToString("\n") {
                                "- [${it.date}]: *${it.content}*"
                            }
                        }
                    }
                    isHabitQuery -> {
                        if (habitsList.isEmpty()) {
                            "🔄 **Habits edhum illa.**"
                        } else {
                            "🔥 **Tracked Habits:\n\n**" + habitsList.joinToString("\n") {
                                val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                                "- **${it.name}**: $streak days logged"
                            }
                        }
                    }
                    isMath -> """
### 🧮 Maths: Simple-ah!

- **Differentiation (Rate of change)**:
  - *Example*: Car speedometer madhiri! Instant rate of change measure pannum.
  - *Formula*: \(f'(x) = \lim_{h 	o 0} rac{f(x+h) - f(x)}{h}\) (distance change / time change).
- **Integration (Total area calculation)**:
  - *Example*: Suvar(wall) kattara madhiri. Tiny bricks-ah sum panna full area kedaikum.
  - *Formula*: \(\int x^n dx = rac{x^{n+1}}{n+1} + C\).
- **Quadratic Formula (Root Solver)**:
  - *Example*: Ball-ah throw panna adhu eppo ground touch pannum-nu kandupudikka.
  - *Formula*: \(x = rac{-b \pm \sqrt{b^2-4ac}}{2a}\).
- **Matrices (Grid system)**:
  - *Example*: Spreadsheet grid madhiri structure! Inverse \(A^{-1}\) grid division madhiri.

> **Tip**: Formula-va mug up pannama daily practice pannunga! 🚀
""".trimIndent()

                    isPhysics -> """
### 🌌 Physics: Easy Facts!

- **Newton's Laws of Motion**:
  1. *First Law (Inertia)*: Oru object-ah thallaradhuku munnadi adhu nagaradhu, dynamic-ah irukadhu.
  2. *Second Law (Force)*: \(F = ma\) (Bala-ma kick panna ball acceleration adhigama irukum!).
  3. *Third Law (Action-Reaction)*: Boat-la irundhu jump pannumbodhu boat pin-nadi pogum (every action has an equal and opposite reaction).
- **Thermodynamics**:
  - *Energy Conservation*: Energy create or destroy panna mudiyadhu (pocket money transfer madhiri). \(\Delta U = Q - W\).
- **Optics (Snell's law)**:
  - *Formula*: \(n_1 \sin 	heta_1 = n_2 \sin 	heta_2\) (Water-la scale tediya theriyara madhiri, light speed change aagi refract aagum).

> **Tip**: Diagrams vechu easy-ah purinjukonga! 🎯
""".trimIndent()

                    isChemistry -> """
### 🧪 Chemistry: Simple Bonds & pH!

- **Chemical Bonding (Dosti)**: Atoms stable aaga friends aagum.
  - *Covalent Bond*: Share panna dosti.
  - *Ionic Bond*: Oru atom accept panna, innoru atom donate pannum.
- **Hybridization**:
  - *Example*: Two colors clay mix panna uniform single color shape varum (sp is linear, sp3 is tetrahedral).
- **pH calculations**:
  - *Formula*: \(pH = -\log_{10}[H^+]\) (measures acid/base).
  - *Example*: Lemon juice acid (pH < 7), soap water base (pH > 7), pure water neutral (pH = 7).

> **Tip**: Periodic table block patterns-ah song-ah padichu nyabagam vechugonga! 🔬
""".trimIndent()

                    isCS -> """
### 💻 Computer Science & Coding Basics

- **Data Structures**:
  - **Stack (LIFO)**: Plate stack madhiri, top plate dhan first remove panna mudiyum.
  - **Queue (FIFO)**: Ticket counter line madhiri, first vandhavangaluku first ticket kedaikum.
- **OOP (Object Oriented Programming)**:
  - Code-ah real-life object-ah design panradhu. E.g. `Car` class has properties (color, speed) and actions (drive, brake).
- **Recursion**:
  - *Example*: Parallel mirror-la continuous reflection theriyara madhiri. Single process complete panna function thane call aagum.

> **Tip**: Computer coding-ku munnadi paper-la logic draw panni parunga! 🚀
""".trimIndent()

                    isMotivation -> ConversationalDataset.getMotivation(isTamil = true, isTelugu = false, isHinglish = false)
                    else -> ConversationalDataset.getGeneralHelp(isTamil = true, isTelugu = false, isHinglish = false)
                }
            }

            // ──────────────── TELUGU RESPONSES ────────────────
            isTelugu -> {
                when {
                    isGreeting -> ConversationalDataset.getGreeting(isTamil = false, isTelugu = true, isHinglish = false)
                    isIdentity -> ConversationalDataset.getIdentity(isTamil = false, isTelugu = true, isHinglish = false)
                    isStress -> ConversationalDataset.getStress(isTamil = false, isTelugu = true, isHinglish = false)
                    isFocus -> ConversationalDataset.getFocus(isTamil = false, isTelugu = true, isHinglish = false)
                    isSpace -> ConversationalDataset.getSpace(isTamil = false, isTelugu = true, isHinglish = false)
                    isTech -> ConversationalDataset.getTech(isTamil = false, isTelugu = true, isHinglish = false)
                    isJoke -> ConversationalDataset.getJoke(isTamil = false, isTelugu = true, isHinglish = false)
                    isSports -> ConversationalDataset.getSports(isTamil = false, isTelugu = true, isHinglish = false)
                    isCourseQuery -> {
                        if (coursesList.isEmpty()) "📊 **Courses emi levu.**"
                        else "📚 **Mee courses list & GPA:\n\n**" + coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }
                    }
                    isDeckQuery -> {
                        if (decksList.isEmpty()) "🎴 **Decks emi levu.**"
                        else "🎴 **Mee flashcard study decks:\n\n**" + decksList.joinToString("\n") { "- **${it.name}**" }
                    }
                    isTaskQuery -> {
                        val active = tasksList.filter { !it.isCompleted }
                        if (active.isEmpty()) {
                            "🎉 **Mee pending tasks emi levu!** Ella target-aiyum successfully complete chesaru."
                        } else {
                            "📌 **Pending Tasks (Active Tasks):\n\n**" + active.joinToString("\n") { 
                                "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority.uppercase()})" 
                            }
                        }
                    }
                    isExamQuery -> {
                        if (examsList.isEmpty()) {
                            "📅 **Exams emi levu.** Settings lo add cheyandi."
                        } else {
                            "📝 **Upcoming Exams:\n\n**" + examsList.joinToString("\n") {
                                "- **${it.name}**: Date: ${it.date}"
                            }
                        }
                    }
                    isAttendanceQuery -> {
                        if (attendanceList.isEmpty()) {
                            "📊 **Attendance details emi levu.**"
                        } else {
                            "📈 **Attendance Report:\n\n**" + attendanceList.joinToString("\n") {
                                val total = it.presents + it.absents
                                val percent = if (total > 0) (it.presents * 100) / total else 100
                                "- **${it.subjectName}**: $percent% (${it.presents}P / ${it.absents}A)"
                            }
                        }
                    }
                    isLedgerQuery -> {
                        val totalExp = expensesList.filter { it.type == "expense" }.sumOf { it.amount }
                        val totalInc = expensesList.filter { it.type == "income" }.sumOf { it.amount }
                        "💰 **Financial Ledger Summary:\n- Total Expense: **₹$totalExp**\n- Total Income: **₹$totalInc**\n\n**Recent transactions:\n**" + 
                                (if (expensesList.isEmpty()) "Records emi levu" else expensesList.take(3).joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    }
                    isRoutineQuery -> {
                        val totalAlloc = routineList.sumOf { it.durationMinutes }
                        val usefulM = routineList.filter { it.classificationScore >= 0.7f }.sumOf { it.durationMinutes }
                        val necM = routineList.filter { it.classificationScore in 0.1f..0.69f }.sumOf { it.durationMinutes }
                        val wasteM = routineList.filter { it.classificationScore == 0.0f }.sumOf { it.durationMinutes }
                        val effStudyM = routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
                        "⏱️ **24-Hour Routine Time Audit:\n\n" +
                                "- 🟢 **Invested (Useful Study):** ${usefulM / 60}h ${usefulM % 60}m\n" +
                                "- 🟡 **Necessary Routine:** ${necM / 60}h ${necM % 60}m\n" +
                                "- 🔴 **Wasted Time:** ${wasteM / 60}h ${wasteM % 60}m\n" +
                                "- ⚪ **Free / Unallocated:** ${(maxOf(0, 1440 - totalAlloc)) / 60}h ${(maxOf(0, 1440 - totalAlloc)) % 60}m\n\n" +
                                "🎯 **Daily Study Budget:** ${effStudyM / 60}h ${effStudyM % 60}m ($effStudyM mins/day)\n\n" +
                                (if (routineList.isEmpty()) "Routine emi ledu. Ledger pakkana Routine tab-lo add cheyandi!" else routineList.joinToString("\n") { "- **${it.activityName}**: ${it.durationMinutes / 60}h ${it.durationMinutes % 60}m (Score: ${it.classificationScore})" })
                    }
                    isTimeTableQuery -> {
                        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                        val targetDay = when {
                            fuzzyMatch(msg, "monday", "somavaram") -> "Monday"
                            fuzzyMatch(msg, "tuesday", "mangalavaram") -> "Tuesday"
                            fuzzyMatch(msg, "wednesday", "budhavaram") -> "Wednesday"
                            fuzzyMatch(msg, "thursday", "guruvaram") -> "Thursday"
                            fuzzyMatch(msg, "friday", "sukravaram") -> "Friday"
                            fuzzyMatch(msg, "saturday", "sanivaram") -> "Saturday"
                            fuzzyMatch(msg, "sunday", "adivaram") -> "Sunday"
                            else -> currentDay
                        }
                        val slots = timeTableList.filter { it.dayOfWeek.equals(targetDay, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                        if (slots.isEmpty()) {
                            "📅 **$targetDay roju Time Table slots emi levu.**\n\nKotha class add cheyadaniki type cheyandi: `@timetable @add`"
                        } else {
                            "📅 **$targetDay Time Table Schedule:\n\n**" + slots.joinToString("\n\n") {
                                "⏰ **${it.getFormattedTimeRange()}**: **${it.title}**\n📍 ${if (!it.roomOrLocation.isNullOrBlank()) it.roomOrLocation else "Regular Room"} | 🔔 ${if (it.isNotificationEnabled) "Reminder ON" else "Reminder OFF"}"
                            }
                        }
                    }
                    isNoteQuery -> {
                        if (notesList.isEmpty()) {
                            "📓 **Notes emi levu.**"
                        } else {
                            "📓 **Recent Notes:\n\n**" + notesList.take(4).joinToString("\n") {
                                "- [${it.date}]: *${it.content}*"
                            }
                        }
                    }
                    isHabitQuery -> {
                        if (habitsList.isEmpty()) {
                            "🔄 **Habits emi levu.**"
                        } else {
                            "🔥 **Tracked Habits:\n\n**" + habitsList.joinToString("\n") {
                                val streakVal = it.completedDates.split(",").filter { it.isNotBlank() }.size
                                "- **${it.name}**: $streakVal days logged"
                            }
                        }
                    }
                    isMath -> """
### 🧮 Maths: Simple-ga!

- **Differentiation (Rate of change)**:
  - *Example*: Car speedometer lantidhi! Instant rate of change measure chesthundhi.
  - *Formula*: \(f'(x) = \lim_{h 	o 0} rac{f(x+h) - f(x)}{h}\) (distance change / time change).
- **Integration (Total area calculation)**:
  - *Example*: Wall kattara lantidhi. Sanna bricks-ni sum chesthe full area vasthundhi.
  - *Formula*: \(\int x^n dx = rac{x^{n+1}}{n+1} + C\).
- **Quadratic Formula (Root Solver)**:
  - *Example*: Ball-ni throw chesthe ground touch eppo chesthundhi ani check cheyadaniki.
  - *Formula*: \(x = rac{-b \pm \sqrt{b^2-4ac}}{2a}\).
- **Matrices (Grid system)**:
  - *Example*: Grid system structure! Inverse \(A^{-1}\) grid division lantidhi.

> **Tip**: Formula-lu rote learning cheyakunda daily practice cheyandi! 🚀
""".trimIndent()

                    isPhysics -> """
### 🌌 Physics: Easy Facts!

- **Newton's Laws of Motion**:
  1. *First Law (Inertia)*: Force apply cheyanidhi ye object move kadhu, dynamic-ga undadhu.
  2. *Second Law (Force)*: \(F = ma\) (Speed-ga kick chesthe ball acceleration ekkuva untundhi!).
  3. *Third Law (Action-Reaction)*: Boat nunchi jump chesinapudu boat venakki velthundhi (every action has an equal and opposite reaction).
- **Thermodynamics**:
  - *Energy Conservation*: Energy ni create/destroy cheyalem (pocket money transfer lantidhi). \(\Delta U = Q - W\).
- **Optics (Snell's law)**:
  - *Formula*: \(n_1 \sin 	heta_1 = n_2 \sin 	heta_2\) (Water lo scale bend aynatlu untundhi, light velocity difference valla refract avthundhi).

> **Tip**: Diagrams draw chesi easy-ga chadhuvukondi! 🎯
""".trimIndent()

                    isChemistry -> """
### 🧪 Chemistry: Simple Bonds & pH!

- **Chemical Bonding (Dosti)**: Atoms stable avvadaniki friends avthai.
  - *Covalent Bond*: Share cheskune dosti.
  - *Ionic Bond*: Okati donate chesthe inkoti accept chesthundhi.
- **Hybridization**:
  - *Example*: Two colors clay mix chesthe uniform new shape chesthundhi (sp is linear, sp3 is tetrahedral).
- **pH calculations**:
  - *Formula*: \(pH = -\log_{10}[H^+]\) (measures acid/base).
  - *Example*: Lemon juice acid (pH < 7), soap water base (pH > 7), pure water neutral (pH = 7).

> **Tip**: Periodic table patterns-ni song-la padukunte memory clear-ga untundhi! 🔬
""".trimIndent()

                    isCS -> """
### 💻 Computer Science & Coding Basics

- **Data Structures**:
  - **Stack (LIFO)**: Plate stack lantidhi, top plate first thinte queue clean untundhi.
  - **Queue (FIFO)**: Ticket line lantidhi, first vachinollake ticket first.
- **OOP (Object Oriented Programming)**:
  - Real-world objects-la code plan cheyadam. E.g. `Car` class key properties (color, speed) and actions (drive, brake) unthai.
- **Recursion**:
  - *Example*: Parallel mirrors lo infinite reflection unnattu. Code complete cheyadaniki function thananu thane call chesthundhi.

> **Tip**: Computers lo code type cheyaka mundhe logic paper lo check cheyandi! 🚀
""".trimIndent()

                    isMotivation -> ConversationalDataset.getMotivation(isTamil = false, isTelugu = true, isHinglish = false)
                    else -> ConversationalDataset.getGeneralHelp(isTamil = false, isTelugu = true, isHinglish = false)
                }
            }

            // ──────────────── HINGLISH RESPONSES ────────────────
            isHinglish -> {
                when {
                    isGreeting -> ConversationalDataset.getGreeting(isTamil = false, isTelugu = false, isHinglish = true)
                    isIdentity -> ConversationalDataset.getIdentity(isTamil = false, isTelugu = false, isHinglish = true)
                    isStress -> ConversationalDataset.getStress(isTamil = false, isTelugu = false, isHinglish = true)
                    isFocus -> ConversationalDataset.getFocus(isTamil = false, isTelugu = false, isHinglish = true)
                    isSpace -> ConversationalDataset.getSpace(isTamil = false, isTelugu = false, isHinglish = true)
                    isTech -> ConversationalDataset.getTech(isTamil = false, isTelugu = false, isHinglish = true)
                    isJoke -> ConversationalDataset.getJoke(isTamil = false, isTelugu = false, isHinglish = true)
                    isSports -> ConversationalDataset.getSports(isTamil = false, isTelugu = false, isHinglish = true)
                    isCourseQuery -> {
                        if (coursesList.isEmpty()) "📊 **Koi course active nahi hai.**"
                        else "📚 **Aapke courses aur GPA details:\n\n**" + coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }
                    }
                    isDeckQuery -> {
                        if (decksList.isEmpty()) "🎴 **Koi active study decks nahi hain.**"
                        else "🎴 **Aapke study decks:\n\n**" + decksList.joinToString("\n") { "- **${it.name}**" }
                    }
                    isTaskQuery -> {
                        val active = tasksList.filter { !it.isCompleted }
                        if (active.isEmpty()) {
                            "🎉 **Aapka koi target ya task pending nahi hai!** Sab time par complete ho gaya."
                        } else {
                            "📌 **Pending Tasks (Active Targets):\n\n**" + active.joinToString("\n") { 
                                "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority.uppercase()})" 
                            }
                        }
                    }
                    isExamQuery -> {
                        if (examsList.isEmpty()) {
                            "📅 **Koi upcoming exam registered nahi hai.** Settings se exam list update/add karein."
                        } else {
                            "📝 **Aapke upcoming exams:\n\n**" + examsList.joinToString("\n") {
                                "- **${it.name}**: Date: ${it.date}"
                            }
                        }
                    }
                    isAttendanceQuery -> {
                        if (attendanceList.isEmpty()) {
                            "📊 **Attendance details khali hain.**"
                        } else {
                            "📈 **Subject-wise Attendance Report:\n\n**" + attendanceList.joinToString("\n") {
                                val total = it.presents + it.absents
                                val percent = if (total > 0) (it.presents * 100) / total else 100
                                "- **${it.subjectName}**: $percent% (${it.presents} Present / ${it.absents} Absent)"
                            }
                        }
                    }
                    isLedgerQuery -> {
                        val totalExp = expensesList.filter { it.type == "expense" }.sumOf { it.amount }
                        val totalInc = expensesList.filter { it.type == "income" }.sumOf { it.amount }
                        "💰 **Expense Ledger Details:\n- Total Kharcha: **₹$totalExp**\n- Total Income: **₹$totalInc**\n\n**Recent Transactions:\n**" + 
                                (if (expensesList.isEmpty()) "No transactions found" else expensesList.take(3).joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    }
                    isRoutineQuery -> {
                        val totalAlloc = routineList.sumOf { it.durationMinutes }
                        val usefulM = routineList.filter { it.classificationScore >= 0.7f }.sumOf { it.durationMinutes }
                        val necM = routineList.filter { it.classificationScore in 0.1f..0.69f }.sumOf { it.durationMinutes }
                        val wasteM = routineList.filter { it.classificationScore == 0.0f }.sumOf { it.durationMinutes }
                        val effStudyM = routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
                        "⏱️ **24-Ghante ka Time Routine Audit:\n\n" +
                                "- 🟢 **Invested (Useful Study):** ${usefulM / 60}h ${usefulM % 60}m\n" +
                                "- 🟡 **Necessary (College/Routine):** ${necM / 60}h ${necM % 60}m\n" +
                                "- 🔴 **Wasted Time:** ${wasteM / 60}h ${wasteM % 60}m\n" +
                                "- ⚪ **Free / Unallocated:** ${(maxOf(0, 1440 - totalAlloc)) / 60}h ${(maxOf(0, 1440 - totalAlloc)) % 60}m\n\n" +
                                "🎯 **Effective Daily Study Task Budget:** ${effStudyM / 60}h ${effStudyM % 60}m ($effStudyM mins per day)\n\n" +
                                (if (routineList.isEmpty()) "Abhi koi routine add nahi kiya hai. Ledger ke bagal mein Routine tab mein jaakar add karein!" else routineList.joinToString("\n") { "- **${it.activityName}**: ${it.durationMinutes / 60}h ${it.durationMinutes % 60}m (Score: ${it.classificationScore})" })
                    }
                    isTimeTableQuery -> {
                        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                        val targetDay = when {
                            fuzzyMatch(msg, "monday", "somwar") -> "Monday"
                            fuzzyMatch(msg, "tuesday", "mangalwar") -> "Tuesday"
                            fuzzyMatch(msg, "wednesday", "budhwar") -> "Wednesday"
                            fuzzyMatch(msg, "thursday", "guruwar", "brihaspativar") -> "Thursday"
                            fuzzyMatch(msg, "friday", "shukrawar") -> "Friday"
                            fuzzyMatch(msg, "saturday", "shaniwar") -> "Saturday"
                            fuzzyMatch(msg, "sunday", "raviwar", "itwar") -> "Sunday"
                            else -> currentDay
                        }
                        val slots = timeTableList.filter { it.dayOfWeek.equals(targetDay, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                        if (slots.isEmpty()) {
                            "📅 **$targetDay ke liye Time Table me koi classes add nahi hain.**\n\nNayi class add karne ke liye likhein: `@timetable @add` ya `@timetable $targetDay Physics 09:00 AM to 10:00 AM @add`"
                        } else {
                            "📅 **$targetDay ka Time Table Schedule:\n\n**" + slots.joinToString("\n\n") {
                                "⏰ **${it.getFormattedTimeRange()}**: **${it.title}**\n📍 ${if (!it.roomOrLocation.isNullOrBlank()) it.roomOrLocation else "Regular Room"} | 🔔 ${if (it.isNotificationEnabled) "Alarm ON" else "Alarm OFF"}"
                            }
                        }
                    }
                    isNoteQuery -> {
                        if (notesList.isEmpty()) {
                            "📓 **Notes list khali hai.**"
                        } else {
                            "✍️ **Aapke recent study notes:\n\n**" + notesList.take(4).joinToString("\n") {
                                "- [${it.date}]: *${it.content}*"
                            }
                        }
                    }
                    isHabitQuery -> {
                        if (habitsList.isEmpty()) {
                            "🔄 **Habits list khali hai.**"
                        } else {
                            "🔥 **Streak habits details:\n\n**" + habitsList.joinToString("\n") {
                                val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                                "- **${it.name}**: $streak days logged"
                            }
                        }
                    }
                    isMath -> """
### 🧮 Math Tricks: Ab aaram se!

- **Differentiation (Rate of change)**:
  - *Analogy*: Gaadi ka speedometer! Instant speed batata hai, waise hi calculus instant rate of change deta hai.
  - *Formula*: \(f'(x) = \lim_{h 	o 0} rac{f(x+h) - f(x)}{h}\) (distance change divided by time change).
- **Integration (Total area calculation)**:
  - *Analogy*: Deewar (wall) banana! Choti-choti bricks ko jod kar deewar banti hai, waise hi chote areas ko sum karke total area banta hai.
  - *Formula*: \(\int x^n dx = rac{x^{n+1}}{n+1} + C\).
- **Quadratic Formula (Root Solver)**:
  - *Analogy*: Cricket ball ko throw kiya, toh kab zameen ko touch karegi, ye find karne ke liye.
  - *Formula*: \(x = rac{-b \pm \sqrt{b^2-4ac}}{2a}\).
- **Matrices (Grid representation)**:
  - *Analogy*: Excel grid! Data ko rows aur columns mein represent karne ke liye.

> **Tip**: Formula ratne ki jagah physics/real-life cases mein apply karke dekhein! 🚀
""".trimIndent()

                    isPhysics -> """
### 🌌 Physics Tricks: Super Simple!

- **Newton's Laws of Motion**:
  1. *First Law (Inertia/Lazy rules)*: Jab tak laat nahi padegi (external force), lazy banda hilega nahi (object state change nahi karega).
  2. *Second Law (Force)*: \(F = ma\) (Jitne zor se kick maroge, ball utni tezi se acceleration legi).
  3. *Third Law (Action-Reaction)*: Boat se jump kiya, toh boat peeche jayegi aur tum aage (equal & opposite reaction).
- **Thermodynamics (Energy Rules)**:
  - *Energy conservation*: Pocket money ki tarah! Paisa ek pocket se doosri pocket mein transfer hoga, destroy nahi hoga. \(\Delta U = Q - W\).
- **Optics (Snell's law)**:
  - *Formula*: \(n_1 \sin 	heta_1 = n_2 \sin 	heta_2\) (Pencil paani mein bend dikhti hai kyunki light refractive index badalne se bend ho jati hai).

> **Tip**: Concepts ko diagram ke sath draw karein, cheezein crystal clear ho jayengi! 🎯
""".trimIndent()

                    isChemistry -> """
### 🧪 Chemistry: Concept Guide!

- **Chemical Bonding (Dosti)**: Atoms stable hone ke liye dosti karte hain.
  - *Covalent Bond*: Sharing is caring dosti (electrons sharing).
  - *Ionic Bond*: Ek ne daan kiya, doosre ne accept kiya (electrons transfer).
- **Hybridization (Mixing)**:
  - *Analogy*: Do colors ke clay ko mix kiya toh unique shapes bante hain (sp is linear, sp3 is tetrahedral).
- **pH metrics (Acid/Base strength)**:
  - *Formula*: \(pH = -\log_{10}[H^+]\) (measures hydrogen concentration).
  - *Analogy*: Lemon juice acidic hai (pH < 7), soap water basic hai (pH > 7), pure water neutral hai (pH = 7).

> **Tip**: Periodic table ko blocks ke song patterns mein yaad karo, chemistry bohot fun ban jayegi! 🔬
""".trimIndent()

                    isCS -> """
### 💻 Computer Science: Simple Guide!

- **Data Structures**:
  - **Stack (LIFO)**: Shaadi ke dinner plates ka stack! Jo plate sabse end mein aayi, wahi sabse pehle uthayi jayegi.
  - **Queue (FIFO)**: Ticket counter ki line! Jo pehle aayega, ticket pehle usi ko milegi.
- **OOP (Object Oriented Programming)**:
  - Software code ko real-world objects ke forms mein structure karna. E.g. `Car` class mein attributes (color, speed) aur actions (accelerate, brake) honge.
- **Recursion**:
  - *Analogy*: Do mirrors ke samne khade hona! Infinite loops banenge, jab tak stop function na ho (base case).

> **Tip**: Logic sheet par draw karke check karein, code automatic solid banega! 🚀
""".trimIndent()

                    isMotivation -> ConversationalDataset.getMotivation(isTamil = false, isTelugu = false, isHinglish = true)
                    else -> ConversationalDataset.getGeneralHelp(isTamil = false, isTelugu = false, isHinglish = true)
                }
            }

            // ──────────────── ENGLISH RESPONSES ────────────────
            else -> {
                when {
                    isGreeting -> ConversationalDataset.getGreeting(isTamil = false, isTelugu = false, isHinglish = false)
                    isIdentity -> ConversationalDataset.getIdentity(isTamil = false, isTelugu = false, isHinglish = false)
                    isStress -> ConversationalDataset.getStress(isTamil = false, isTelugu = false, isHinglish = false)
                    isFocus -> ConversationalDataset.getFocus(isTamil = false, isTelugu = false, isHinglish = false)
                    isSpace -> ConversationalDataset.getSpace(isTamil = false, isTelugu = false, isHinglish = false)
                    isTech -> ConversationalDataset.getTech(isTamil = false, isTelugu = false, isHinglish = false)
                    isJoke -> ConversationalDataset.getJoke(isTamil = false, isTelugu = false, isHinglish = false)
                    isSports -> ConversationalDataset.getSports(isTamil = false, isTelugu = false, isHinglish = false)
                    isCourseQuery -> {
                        if (coursesList.isEmpty()) "📊 **No courses added yet.**"
                        else "📚 **Your active courses and GPA breakdown:\n\n**" + coursesList.joinToString("\n") { "- **${it.name}**: Credits ${it.credits}, Grade: ${it.grade}" }
                    }
                    isDeckQuery -> {
                        if (decksList.isEmpty()) "🎴 **No active study decks found.**"
                        else "🎴 **Your active flashcard decks:\n\n**" + decksList.joinToString("\n") { "- **${it.name}**" }
                    }
                    isTaskQuery -> {
                        val active = tasksList.filter { !it.isCompleted }
                        if (active.isEmpty()) {
                            "🎉 **Excellent! No pending tasks left.** All targets achieved."
                        } else {
                            "📌 **Pending Tasks (Active Targets):\n\n**" + active.joinToString("\n") { 
                                "- **${it.title}** (DueDate: ${it.dueDate}, Priority: ${it.priority.uppercase()})" 
                            }
                        }
                    }
                    isExamQuery -> {
                        if (examsList.isEmpty()) {
                            "📅 **No upcoming exams registered.** You can add them in Settings."
                        } else {
                            "📝 **Upcoming Exams:\n\n**" + examsList.joinToString("\n") {
                                "- **${it.name}**: Date: ${it.date}"
                            }
                        }
                    }
                    isAttendanceQuery -> {
                        if (attendanceList.isEmpty()) {
                            "📊 **Attendance metrics are currently empty.**"
                        } else {
                            "📈 **Attendance Summary:\n\n**" + attendanceList.joinToString("\n") {
                                val total = it.presents + it.absents
                                val percent = if (total > 0) (it.presents * 100) / total else 100
                                "- **${it.subjectName}**: $percent% (${it.presents} Present / ${it.absents} Absent)"
                            }
                        }
                    }
                    isLedgerQuery -> {
                        val totalExp = expensesList.filter { it.type == "expense" }.sumOf { it.amount }
                        val totalInc = expensesList.filter { it.type == "income" }.sumOf { it.amount }
                        "💰 **Expense Ledger Overview:\n- Total Expense: **₹$totalExp**\n- Total Income: **₹$totalInc**\n\n**Recent Transactions:\n**" + 
                                (if (expensesList.isEmpty()) "No transaction history" else expensesList.take(3).joinToString("\n") { "- **${it.description}**: ₹${it.amount} (${it.type})" })
                    }
                    isRoutineQuery -> {
                        val totalAlloc = routineList.sumOf { it.durationMinutes }
                        val usefulM = routineList.filter { it.classificationScore >= 0.7f }.sumOf { it.durationMinutes }
                        val necM = routineList.filter { it.classificationScore in 0.1f..0.69f }.sumOf { it.durationMinutes }
                        val wasteM = routineList.filter { it.classificationScore == 0.0f }.sumOf { it.durationMinutes }
                        val effStudyM = routineList.sumOf { (it.durationMinutes * it.classificationScore).toInt() }
                        "⏱️ **24-Hour Routine Time Audit Overview:\n\n" +
                                "- 🟢 **Invested (Useful Study):** ${usefulM / 60}h ${usefulM % 60}m\n" +
                                "- 🟡 **Necessary Routine:** ${necM / 60}h ${necM % 60}m\n" +
                                "- 🔴 **Wasted Time:** ${wasteM / 60}h ${wasteM % 60}m\n" +
                                "- ⚪ **Free / Unallocated:** ${(maxOf(0, 1440 - totalAlloc)) / 60}h ${(maxOf(0, 1440 - totalAlloc)) % 60}m\n\n" +
                                "🎯 **Effective Daily Study Capacity:** ${effStudyM / 60}h ${effStudyM % 60}m ($effStudyM mins/day)\n\n" +
                                (if (routineList.isEmpty()) "No routine configured yet. Visit the Routine tab next to Ledger to audit your 24 hours!" else routineList.joinToString("\n") { "- **${it.activityName}**: ${it.durationMinutes / 60}h ${it.durationMinutes % 60}m (Usefulness Score: ${it.classificationScore})" })
                    }
                    isTimeTableQuery -> {
                        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                        val targetDay = when {
                            fuzzyMatch(msg, "monday") -> "Monday"
                            fuzzyMatch(msg, "tuesday") -> "Tuesday"
                            fuzzyMatch(msg, "wednesday") -> "Wednesday"
                            fuzzyMatch(msg, "thursday") -> "Thursday"
                            fuzzyMatch(msg, "friday") -> "Friday"
                            fuzzyMatch(msg, "saturday") -> "Saturday"
                            fuzzyMatch(msg, "sunday") -> "Sunday"
                            else -> currentDay
                        }
                        val slots = timeTableList.filter { it.dayOfWeek.equals(targetDay, ignoreCase = true) || it.dayOfWeek.equals("Daily", ignoreCase = true) }
                        if (slots.isEmpty()) {
                            "📅 **No Time Table slots scheduled for $targetDay.**\n\nTo add a class, type: `@timetable @add`"
                        } else {
                            "📅 **$targetDay Time Table Schedule:\n\n**" + slots.joinToString("\n\n") {
                                "⏰ **${it.getFormattedTimeRange()}**: **${it.title}**\n📍 ${if (!it.roomOrLocation.isNullOrBlank()) it.roomOrLocation else "Regular Room"} | 🔔 ${if (it.isNotificationEnabled) "Alarm ON" else "Alarm OFF"}"
                            }
                        }
                    }
                    isNoteQuery -> {
                        if (notesList.isEmpty()) {
                            "📓 **Notes repository is currently empty.**"
                        } else {
                            "✍️ **Your recent study notes:\n\n**" + notesList.take(4).joinToString("\n") {
                                "- [${it.date}]: *${it.content}*"
                            }
                        }
                    }
                    isHabitQuery -> {
                        if (habitsList.isEmpty()) {
                            "🔄 **No habits tracked yet.**"
                        } else {
                            "🔥 **Tracked Habits & Streaks:\n\n**" + habitsList.joinToString("\n") {
                                val streak = it.completedDates.split(",").filter { it.isNotBlank() }.size
                                "- **${it.name}**: $streak days logged"
                            }
                        }
                    }
                    isMath -> """
### 🧮 Mathematics: Conceptual Guide!

- **Differentiation (Rate of change)**:
  - *Example*: Car speedometer! It measures the instant rate of speed change at any given second.
  - *Formula*: \(f'(x) = \lim_{h 	o 0} rac{f(x+h) - f(x)}{h}\) (distance delta over time delta).
- **Integration (Total area calculation)**:
  - *Example*: Building a wall brick by brick. Summing up tiny individual sections to calculate the entire surface area.
  - *Formula*: \(\int x^n dx = rac{x^{n+1}}{n+1} + C\).
- **Quadratic Formula (Root Solver)**:
  - *Example*: Finding exactly when a thrown ball will hit the ground.
  - *Formula*: \(x = rac{-b \pm \sqrt{b^2-4ac}}{2a}\).
- **Matrices (Grid organization)**:
  - *Example*: Standard Excel spreadsheets grid system. Matrix inversion is similar to grid division.

> **Tip**: Daily mathematical exercises are better than rote learning. Revise regularly! 🚀
""".trimIndent()

                    isPhysics -> """
### 🌌 Physics Concepts Made Simple!

- **Newton's Laws of Motion**:
  1. *First Law (Inertia)*: A lazy object stays at rest, and a moving object continues moving unless an external force is applied.
  2. *Second Law (Force)*: \(F = ma\) (Kicking a soccer ball harder yields greater acceleration).
  3. *Third Law (Action-Reaction)*: Jumping off a small boat pushes it backwards while propelling you forwards.
- **Thermodynamics (Energy Conservation)**:
  - *First Law*: Energy can neither be created nor destroyed, only transformed (similar to transferring balance between accounts). \(\Delta U = Q - W\).
- **Optics (Refraction)**:
  - *Formula*: \(n_1 \sin 	heta_1 = n_2 \sin 	heta_2\) (Snell's Law). A straw looks bent in water because light slows down and changes angle.

> **Tip**: Focus on free-body diagrams to resolve mechanical problems quickly! 🎯
""".trimIndent()

                    isChemistry -> """
### 🧪 Chemistry: Bonds & Equations!

- **Chemical Bonding**: Atoms share or exchange electrons to achieve stability.
  - *Covalent Bond*: Sharing electrons (like a shared partnership).
  - *Ionic Bond*: Transferring electrons from a donor to an acceptor.
- **Hybridization (Orbital Mixing)**:
  - *Example*: Mixing two colors of clay to produce uniform intermediate geometries (sp is linear, sp3 is tetrahedral).
- **pH scale**:
  - *Formula*: \(pH = -\log_{10}[H^+]\). Lemon juice is acidic (pH < 7), soap is basic (pH > 7), and pure water is neutral (pH = 7).

> **Tip**: Memorize standard electronegativity trends to predict chemical reactions accurately! 🔬
""".trimIndent()

                    isCS -> """
### 💻 Computer Science & Data Structures

- **Core Data Structures**:
  - **Stack (LIFO)**: Stack of cafeteria plates! The last plate added is the first one removed.
  - **Queue (FIFO)**: Waiting line at a movie theater! The first person in line gets served first.
- **OOP (Object-Oriented Programming)**:
  - Designing code as objects. Class `Car` has attributes (color, speed) and actions (drive, brake).
- **Recursion**:
  - *Example*: Endless nested reflections between parallel mirrors. A function calls itself to solve smaller instances until a base case is hit.

> **Tip**: Always draft your algorithms on paper before coding them on screen! 🚀
""".trimIndent()

                    isMotivation -> ConversationalDataset.getMotivation(isTamil = false, isTelugu = false, isHinglish = false)
                    else -> ConversationalDataset.getGeneralHelp(isTamil = false, isTelugu = false, isHinglish = false)
                }
            }
        }
    }
    override fun onCleared() {
        gemmaExecutor.execute {
            try {
                llmInference?.close()
                llmInference = null
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                gemmaExecutor.shutdown()
            }
        }
        super.onCleared()
    }
}

class GemmaViewModelFactory(private val application: Application, private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GemmaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GemmaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
