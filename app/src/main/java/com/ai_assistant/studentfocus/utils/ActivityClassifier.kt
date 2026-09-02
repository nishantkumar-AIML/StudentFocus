package com.ai_assistant.studentfocus.utils

import com.ai_assistant.studentfocus.models.AppActivitySessionEntity
import com.ai_assistant.studentfocus.models.ModelMaturity
import com.ai_assistant.studentfocus.models.PersonalBehaviorProfile
import java.util.Locale

object ActivityClassifier {

    const val CAT_LEARNING = "Learning"
    const val CAT_CODING = "Coding"
    const val CAT_PRODUCTIVITY = "Productivity"
    const val CAT_COMMUNICATION = "Communication"
    const val CAT_ENTERTAINMENT = "Entertainment"
    const val CAT_SOCIAL = "Social Media"
    const val CAT_GAMING = "Gaming"
    const val CAT_BROWSING = "Browsing"
    const val CAT_OTHER = "Other"

    val ALL_CATEGORIES = listOf(
        CAT_LEARNING,
        CAT_CODING,
        CAT_PRODUCTIVITY,
        CAT_COMMUNICATION,
        CAT_ENTERTAINMENT,
        CAT_SOCIAL,
        CAT_GAMING,
        CAT_BROWSING,
        CAT_OTHER
    )

    enum class UserIntent(val displayName: String, val icon: String) {
        LEARNING("Learning / Lecture", "📚"),
        PRACTICING("Practicing / Problem Solving", "🎯"),
        CODING("Coding / Development", "💻"),
        RESEARCH("Technical Research", "🔬"),
        ACADEMIC_WORK("Academic Work / Assignments", "📝"),
        JOB_PREPARATION("Career & Interview Prep", "💼"),
        PROJECT_WORK("Project Work", "🛠️"),
        COMMUNICATION("Communication", "💬"),
        ENTERTAINMENT("Entertainment", "🎬"),
        SOCIAL_BROWSING("Social Media Browsing", "📱"),
        SHOPPING("Shopping", "🛍️"),
        GAMING("Gaming", "🎮"),
        MUSIC("Music & Audio", "🎵"),
        NEWS("News & Articles", "📰"),
        UNKNOWN("Unclassified Intent", "❓")
    }

    enum class GoalAlignment(val displayName: String, val icon: String) {
        GOAL_ALIGNED("Goal-Aligned", "🎯"),
        HIGHLY_RELEVANT("Highly Relevant", "✨"),
        POSSIBLY_RELEVANT("Possibly Relevant", "🟡"),
        NEUTRAL("Neutral", "⚪"),
        OFF_GOAL("Off-Goal", "⚠️"),
        UNKNOWN("Unknown", "❓")
    }

    enum class ConfidenceLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    data class AcademicContext(
        val subjects: List<String> = emptyList(),
        val courses: List<String> = emptyList(),
        val tasks: List<String> = emptyList(),
        val exams: List<String> = emptyList(),
        val mainGoals: List<String> = emptyList(),
        val focusSkills: List<String> = emptyList(),
        val customKeywords: List<String> = emptyList(),
        val userCorrections: Map<String, String> = emptyMap(),
        val personalProfile: PersonalBehaviorProfile = PersonalBehaviorProfile()
    )

    data class ClassificationResult(
        val category: String,
        val goalAlignment: GoalAlignment,
        val goalScore: Int, // 0 to 100
        val confidence: Float, // 0.0 to 1.0
        val confidenceLevel: ConfidenceLevel,
        val classificationSource: String,
        val primaryReason: String,
        val matchedGoal: String? = null,
        val matchedTopics: List<String> = emptyList(),
        val detectedIntent: UserIntent = UserIntent.UNKNOWN,
        val contentRelevance: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
        val personalRelevance: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
        val routineMatch: String = "HIGH", // "HIGH", "MEDIUM", "LOW", "NONE"
        val positiveEvidence: List<String> = emptyList(),
        val negativeEvidence: List<String> = emptyList(),
        val personalEvidence: List<String> = emptyList(),
        val historicalEvidence: List<String> = emptyList(),
        val explanationReasons: List<String> = emptyList(),
        val modelMaturity: ModelMaturity = ModelMaturity.NEW_USER,
        val predictedRoutine: String? = null,
        val observedEvidence: String,
        val inferenceNote: String = "Inferred: Goal alignment and behavioral relevance are estimates based on observed activity, goals, and history."
    )

    // =========================================================================
    // KNOWLEDGE GRAPH & SEMANTIC CONCEPT MAP
    // =========================================================================
    private val GLOBAL_CONCEPT_MAP: Map<String, List<String>> = mapOf(
        "machine learning" to listOf(
            "python", "numpy", "pandas", "scikit-learn", "scikit", "sklearn", "neural network",
            "deep learning", "nlp", "computer vision", "rag", "transformers", "llm", "model evaluation",
            "loss function", "backpropagation", "gradient descent", "pytorch", "tensorflow",
            "huggingface", "embeddings", "vector database", "langchain", "fine-tuning",
            "prompt engineering", "reinforcement learning", "classification", "regression", "clustering", "ai"
        ),
        "ai" to listOf(
            "machine learning", "deep learning", "neural network", "nlp", "llm", "transformers",
            "langchain", "rag", "openai", "gemini", "claude", "prompt engineering", "vector database"
        ),
        "artificial intelligence" to listOf(
            "machine learning", "deep learning", "neural network", "nlp", "llm", "transformers", "rag"
        ),
        "data science" to listOf(
            "python", "r", "sql", "pandas", "matplotlib", "seaborn", "tableau", "power bi", "eda",
            "hypothesis testing", "statistics", "probability", "regression", "bigquery", "spark", "data analysis"
        ),
        "web development" to listOf(
            "javascript", "typescript", "react", "next.js", "vue", "angular", "node.js", "express",
            "django", "fastapi", "spring boot", "html", "css", "tailwind", "rest api", "graphql",
            "mongodb", "postgresql", "sql", "frontend", "backend", "full stack", "fullstack"
        ),
        "frontend" to listOf(
            "javascript", "typescript", "react", "next.js", "vue", "angular", "html", "css", "tailwind", "ui", "ux"
        ),
        "backend" to listOf(
            "node.js", "express", "django", "fastapi", "spring boot", "sql", "postgresql", "mysql", "mongodb", "rest api", "graphql", "database"
        ),
        "android" to listOf(
            "kotlin", "java", "jetpack compose", "android studio", "coroutine", "coroutines", "flow", "viewmodel",
            "room", "hilt", "dagger", "retrofit", "activity", "fragment", "gradle", "xml", "flutter"
        ),
        "android development" to listOf(
            "kotlin", "java", "jetpack compose", "android studio", "coroutine", "coroutines", "flow", "viewmodel",
            "room", "hilt", "dagger", "retrofit", "gradle"
        ),
        "python programming" to listOf(
            "python", "django", "fastapi", "flask", "numpy", "pandas", "pytest", "oop", "decorators", "generator"
        ),
        "competitive programming" to listOf(
            "leetcode", "codeforces", "codechef", "hackerrank", "atcoder", "binary search", "dynamic programming",
            "graph", "tree", "bfs", "dfs", "trie", "segment tree", "two pointers", "sliding window", "striver", "neetcode"
        ),
        "dsa" to listOf(
            "data structures", "algorithms", "leetcode", "codeforces", "binary search", "dynamic programming",
            "graph", "tree", "array", "linked list", "stack", "queue", "heap", "recursion", "sorting", "searching", "striver"
        ),
        "data structures" to listOf(
            "dsa", "algorithms", "array", "linked list", "stack", "queue", "tree", "binary tree", "graph", "heap", "trie", "hash table"
        ),
        "algorithms" to listOf(
            "dsa", "sorting", "searching", "dynamic programming", "greedy", "divide and conquer", "recursion", "backtracking", "graph algorithms"
        ),
        "cloud" to listOf(
            "docker", "kubernetes", "aws", "gcp", "azure", "terraform", "ci/cd", "jenkins", "linux", "bash", "devops"
        ),
        "devops" to listOf(
            "docker", "kubernetes", "aws", "gcp", "azure", "terraform", "ci/cd", "jenkins", "ansible", "linux", "bash", "nginx"
        ),
        "gate" to listOf(
            "operating systems", "dbms", "computer networks", "theory of computation", "compiler design",
            "digital logic", "computer organization", "coa", "discrete mathematics", "algorithms", "gate smashers"
        ),
        "gate cse" to listOf(
            "operating systems", "dbms", "computer networks", "theory of computation", "compiler design",
            "digital logic", "computer organization", "coa", "discrete mathematics", "algorithms"
        ),
        "operating systems" to listOf(
            "os", "process synchronization", "semaphores", "deadlock", "paging", "virtual memory", "cpu scheduling", "threads", "mutex", "linux kernel"
        ),
        "os" to listOf(
            "operating systems", "process synchronization", "semaphores", "deadlock", "paging", "virtual memory", "cpu scheduling", "threads", "mutex", "linux kernel"
        ),
        "dbms" to listOf(
            "database", "sql", "normalization", "b tree", "indexing", "transactions", "acid properties", "concurrency control", "er diagram"
        ),
        "db" to listOf(
            "database", "dbms", "sql", "normalization", "indexing", "transactions", "postgresql", "mysql", "mongodb"
        ),
        "computer networks" to listOf(
            "cn", "tcp/ip", "osi model", "routing", "ip addressing", "subnetting", "http", "https", "dns", "socket programming", "congestion control"
        ),
        "cn" to listOf(
            "computer networks", "tcp/ip", "osi model", "routing", "ip addressing", "subnetting", "http", "https", "dns"
        ),
        "system design" to listOf(
            "high level design", "low level design", "hld", "lld", "scalability", "load balancing", "caching", "microservices", "message queue", "sharding"
        ),
        "biology" to listOf(
            "botany", "zoology", "genetics", "biochemistry", "physiology", "anatomy", "cell biology", "ncert", "neet", "evolution", "ecology"
        ),
        "neet" to listOf(
            "physics", "chemistry", "biology", "botany", "zoology", "organic chemistry", "inorganic chemistry", "human physiology", "genetics", "ncert"
        ),
        "physics" to listOf(
            "mechanics", "thermodynamics", "electromagnetism", "optics", "modern physics", "rotational motion", "kinematics", "waves", "electrostatics", "hc verma"
        ),
        "jee" to listOf(
            "physics", "chemistry", "mathematics", "calculus", "mechanics", "organic chemistry", "coordinate geometry", "jee main", "jee advanced", "pyq"
        ),
        "chemistry" to listOf(
            "organic chemistry", "inorganic chemistry", "physical chemistry", "chemical bonding", "thermodynamics", "equilibrium", "periodic table"
        ),
        "mathematics" to listOf(
            "algebra", "calculus", "trigonometry", "linear algebra", "probability", "statistics", "differential equations", "matrices", "integration"
        ),
        "math" to listOf(
            "algebra", "calculus", "trigonometry", "linear algebra", "probability", "statistics", "integration", "differentiation"
        ),
        "upsc" to listOf(
            "polity", "economy", "history", "geography", "current affairs", "the hindu", "editorial", "ncert", "ethics", "general studies"
        )
    )

    private val ANTI_STUDY_MODIFIERS = listOf(
        "meme", "memes", "funny", "fails", "fail", "comedy", "roast", "parody",
        "troll", "prank", "reaction", "reactions", "montage", "status",
        "whatsapp status", "spoof", "cringe", "bloopers", "vine", "vines"
    )

    private val EDUCATIONAL_INTENT_SIGNALS = listOf(
        "tutorial", "lecture", "course", "lesson", "chapter", "explanation",
        "explained", "guide", "crash course", "walkthrough", "full course",
        "one shot", "revision", "formula sheet", "numerical", "solved examples",
        "pyq", "previous year question", "derivation", "proof", "concept",
        "exam prep", "mock test", "sample paper", "syllabus", "solution",
        "documentation", "api reference", "handbook", "cheatsheet", "notes",
        "architecture", "deep dive", "how to build", "how to implement",
        "step by step", "masterclass", "bootcamp", "roadmap", "getting started",
        "cs50", "computer science", "harvard", "mit opencourseware", "nptel", "swayam"
    )

    private val CODING_PRACTICE_SIGNALS = listOf(
        "leetcode", "hackerrank", "codeforces", "codechef", "atcoder", "geeksforgeeks",
        "github.com", "gitlab.com", "bitbucket.org", "stackoverflow", "solution",
        "two sum", "problem", "accepted", "editorial", "submission", "debug",
        "repository", "pull request", "commit", "branch", "pytorch", "tensorflow",
        "kotlin", "python", "javascript", "typescript", "react", "android studio",
        "sql", "dbms", "data structures", "algorithms", "dsa"
    )

    private val ACADEMIC_COMMUNICATION_KEYWORDS = listOf(
        "college", "class", "batch", "teacher", "professor", "sir", "mam",
        "study group", "assignment", "project discussion", "exam", "syllabus",
        "doubt", "lecture notes", "attendance", "practical", "lab", "coursework",
        "deadline", "submission", "semester", "midterm", "viva"
    )

    private val SHOPPING_KEYWORDS = listOf(
        "amazon", "flipkart", "myntra", "meesho", "ajio", "cart", "checkout",
        "buy now", "discount", "offer", "sale", "price", "order placed", "deals",
        "great indian festival", "big billion days"
    )

    private val GAMING_KEYWORDS = listOf(
        "game", "gaming", "gameplay", "pubg", "bgmi", "free fire", "call of duty", "codm",
        "clash of clans", "roblox", "minecraft", "chess", "candy crush", "subway surfers",
        "asphalt", "valorant", "gta", "gta 5", "fifa", "fortnite", "streamer", "live stream"
    )

    private val ENTERTAINMENT_KEYWORDS = listOf(
        "movie", "film", "trailer", "teaser", "cinema", "song", "music video",
        "remix", "lofi", "web series", "series", "episode", "season", "netflix",
        "prime video", "hotstar", "anime", "naruto", "one piece", "jujutsu kaisen",
        "attack on titan", "drama", "gossip", "celebrity", "dance", "standup",
        "cricket highlights", "ipl", "football highlights", "wwe"
    )

    /**
     * Dynamically derives related concepts and topics from user's registered goals and academic context.
     */
    fun deriveRelatedTopics(context: AcademicContext): Map<String, List<String>> {
        val topicMap = mutableMapOf<String, MutableList<String>>()

        val allUserGoals = (context.mainGoals + context.subjects + context.courses + context.exams + context.tasks + context.focusSkills)
            .filter { it.isNotBlank() && it.length >= 2 }
            .distinct()

        for (goal in allUserGoals) {
            val lowerGoal = goal.trim().lowercase(Locale.ROOT)
            val derivedList = mutableListOf<String>()

            // 1. Check direct matches in GLOBAL_CONCEPT_MAP
            GLOBAL_CONCEPT_MAP[lowerGoal]?.let { derivedList.addAll(it) }

            // 2. Check substring / n-gram matches in GLOBAL_CONCEPT_MAP
            for ((conceptKey, conceptSubtopics) in GLOBAL_CONCEPT_MAP) {
                if (lowerGoal.contains(conceptKey) || conceptKey.contains(lowerGoal)) {
                    derivedList.addAll(conceptSubtopics)
                }
            }

            // 3. Extract individual significant words/tokens from the goal itself (>= 2 chars for acronyms, >= 3 for words)
            val tokens = lowerGoal.split("\\s+".toRegex()).filter {
                (it.length >= 3 || it in listOf("os", "ai", "ml", "db", "cn", "ds", "ui", "ux", "cs", "it", "ee", "ec", "me", "ce")) &&
                it !in listOf("the", "and", "for", "with", "prep", "exam", "course", "test", "mid-term", "term")
            }
            derivedList.addAll(tokens)

            topicMap[goal] = derivedList.distinct().toMutableList()
        }

        return topicMap
    }

    /**
     * Detects user intent from observable app and content signals.
     */
    fun detectIntent(
        packageName: String,
        appName: String,
        activityType: String,
        contentTitle: String,
        domainOrSubtext: String? = null
    ): UserIntent {
        val pkg = packageName.lowercase(Locale.ROOT)
        val text = "${appName.lowercase(Locale.ROOT)} ${contentTitle.lowercase(Locale.ROOT)} ${activityType.lowercase(Locale.ROOT)} ${domainOrSubtext?.lowercase(Locale.ROOT) ?: ""}"
        val titleLower = contentTitle.lowercase(Locale.ROOT)

        // 1. Practicing & Problem Solving
        if (text.contains("leetcode") || text.contains("codeforces") || text.contains("hackerrank") ||
            text.contains("codechef") || text.contains("atcoder") || (text.contains("two sum") && text.contains("solution")) ||
            activityType.contains("Practice", ignoreCase = true) || text.contains("mock test") || text.contains("flashcard")
        ) {
            return UserIntent.PRACTICING
        }

        // 2. Coding & Software Development
        if (text.contains("github.com") || text.contains("gitlab.com") || text.contains("stackoverflow") ||
            pkg.contains("android.studio") || pkg.contains("vscode") || pkg.contains("pycharm") ||
            pkg.contains("intellij") || text.contains("repository") || text.contains("pull request") ||
            text.contains("terminal") || text.contains("devdocs") || activityType.contains("Code", ignoreCase = true)
        ) {
            return UserIntent.CODING
        }

        // 3. Academic Work & Coursework
        if (ACADEMIC_COMMUNICATION_KEYWORDS.any { text.contains(it) } && (text.contains("assignment") || text.contains("submission") || text.contains("homework") || text.contains("lab practical") || text.contains("syllabus") || text.contains("notes"))) {
            return UserIntent.ACADEMIC_WORK
        }

        // 4. Learning & Education (Check before social media/entertainment)
        if (EDUCATIONAL_INTENT_SIGNALS.any { titleLower.contains(it) } || text.contains("tutorial") ||
            text.contains("lecture") || text.contains("course") || text.contains("nptel") ||
            text.contains("mit opencourseware") || text.contains("coursera") || text.contains("udemy") ||
            text.contains("infographic") || text.contains("cheat sheet")
        ) {
            return UserIntent.LEARNING
        }

        // 5. Research & Deep Technical Reading
        if (text.contains("arxiv") || text.contains("researchgate") || text.contains("ieee") ||
            text.contains("documentation") || text.contains("whitepaper") || text.contains("specification")
        ) {
            return UserIntent.RESEARCH
        }

        // 6. Job & Interview Prep
        if (text.contains("interview prep") || text.contains("system design interview") ||
            text.contains("resume") || text.contains("mock interview") || text.contains("hiring")
        ) {
            return UserIntent.JOB_PREPARATION
        }

        // 7. Gaming
        if (GAMING_KEYWORDS.any { text.contains(it) } || pkg.contains("game") || activityType.contains("Game", ignoreCase = true)) {
            return UserIntent.GAMING
        }

        // 8. Shopping
        if (SHOPPING_KEYWORDS.any { text.contains(it) } || pkg.contains("amazon") || pkg.contains("flipkart") || pkg.contains("myntra")) {
            return UserIntent.SHOPPING
        }

        // 9. Short-form Social Media Browsing (Check before general entertainment)
        if (activityType.equals("Shorts", ignoreCase = true) || activityType.equals("Reel", ignoreCase = true) ||
            titleLower.contains("shorts") || titleLower.contains("reels") || pkg.contains("instagram")
        ) {
            return UserIntent.SOCIAL_BROWSING
        }

        // 10. Entertainment (Movies, Comedy, Memes, Songs)
        if (ANTI_STUDY_MODIFIERS.any { titleLower.contains(it) } || ENTERTAINMENT_KEYWORDS.any { text.contains(it) } ||
            pkg.contains("netflix") || pkg.contains("primevideo") || pkg.contains("hotstar")
        ) {
            return UserIntent.ENTERTAINMENT
        }

        // 11. Communication
        if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("discord") || pkg.contains("slack") || pkg.contains("teams") ||
            pkg.contains("meet") || pkg.contains("messages") || pkg.contains("dialer")
        ) {
            return UserIntent.COMMUNICATION
        }

        // 12. Music & Audio
        if (pkg.contains("spotify") || text.contains("spotify") || text.contains("music") || text.contains("lofi") || text.contains("podcast")) {
            return UserIntent.MUSIC
        }

        // 13. News
        if (text.contains("the hindu") || text.contains("indian express") || text.contains("bbc") || text.contains("news") || text.contains("editorial")) {
            return UserIntent.NEWS
        }

        return if (titleLower.contains("unavailable")) UserIntent.UNKNOWN else UserIntent.LEARNING
    }

    private val classificationCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, ClassificationResult>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ClassificationResult>?): Boolean = size > 256
        }
    )

    val classifierExecutionCount = java.util.concurrent.atomic.AtomicLong(0)
    val totalClassifierTimeNanos = java.util.concurrent.atomic.AtomicLong(0)
    val cacheHitCount = java.util.concurrent.atomic.AtomicLong(0)

    fun clearCache() {
        classificationCache.clear()
    }

    /**
     * Primary Intelligence Engine: Classifies an observed screen activity against user goals & context.
     * Fuses goal evidence, personal behavior history, and routine timing while preserving strong goal priority.
     */
    fun classifyActivity(
        packageName: String,
        appName: String,
        activityType: String,
        contentTitle: String,
        domainOrSubtext: String? = null,
        userOverrides: Map<String, String> = emptyMap(),
        academicContext: AcademicContext = AcademicContext(),
        timestamp: Long = System.currentTimeMillis()
    ): ClassificationResult {
        val hourBucket = timestamp / 3600000L
        val cacheKey = "$packageName|$appName|$activityType|$contentTitle|$domainOrSubtext|${userOverrides.hashCode()}|${academicContext.hashCode()}|$hourBucket"
        classificationCache[cacheKey]?.let {
            cacheHitCount.incrementAndGet()
            return it
        }

        val startTimeNanos = System.nanoTime()
        classifierExecutionCount.incrementAndGet()

        val isUnavailableTitle = contentTitle.isBlank() || contentTitle.contains("unavailable", ignoreCase = true)
        val observedEvidence = if (!isUnavailableTitle) {
            "Observed: Title '$contentTitle' in $appName ($activityType)${if (domainOrSubtext != null) " • Domain: $domainOrSubtext" else ""}"
        } else {
            "Observed: Active foreground view in $appName"
        }

        val pkg = packageName.lowercase(Locale.ROOT)
        val textToAnalyze = "${appName.lowercase(Locale.ROOT)} ${contentTitle.lowercase(Locale.ROOT)} ${activityType.lowercase(Locale.ROOT)} ${domainOrSubtext?.lowercase(Locale.ROOT) ?: ""}"
        val titleLower = contentTitle.lowercase(Locale.ROOT)

        // =========================================================================
        // LAYER 1: USER EXPLICIT OVERRIDES (Highest Authority)
        // =========================================================================
        val contentKey = "$packageName:$contentTitle"
        if (userOverrides.containsKey(contentKey)) {
            val cat = userOverrides[contentKey]!!
            val alignment = mapCategoryToGoalAlignment(cat)
            val intent = if (alignment == GoalAlignment.GOAL_ALIGNED) UserIntent.LEARNING else UserIntent.ENTERTAINMENT
            return ClassificationResult(
                category = cat,
                goalAlignment = alignment,
                goalScore = if (alignment == GoalAlignment.GOAL_ALIGNED) 100 else 10,
                confidence = 1.0f,
                confidenceLevel = ConfidenceLevel.HIGH,
                classificationSource = "USER_OVERRIDE",
                primaryReason = "Custom Category Override: $cat",
                detectedIntent = intent,
                contentRelevance = if (alignment == GoalAlignment.GOAL_ALIGNED) "HIGH" else "LOW",
                personalRelevance = "HIGH",
                routineMatch = "HIGH",
                positiveEvidence = listOf("✓ Exact content-level user override ($cat)"),
                personalEvidence = listOf("✓ User explicit feedback override"),
                explanationReasons = listOf(
                    "✓ User explicit override applied for this content",
                    "✓ Category set to '$cat' (${alignment.displayName})"
                ),
                observedEvidence = observedEvidence,
                modelMaturity = academicContext.personalProfile.maturity
            )
        }
        if (userOverrides.containsKey(packageName)) {
            val cat = userOverrides[packageName]!!
            val alignment = mapCategoryToGoalAlignment(cat)
            val intent = if (alignment == GoalAlignment.GOAL_ALIGNED) UserIntent.LEARNING else UserIntent.ENTERTAINMENT
            return ClassificationResult(
                category = cat,
                goalAlignment = alignment,
                goalScore = if (alignment == GoalAlignment.GOAL_ALIGNED) 98 else 15,
                confidence = 0.98f,
                confidenceLevel = ConfidenceLevel.HIGH,
                classificationSource = "USER_OVERRIDE",
                primaryReason = "Custom App Override: $cat",
                detectedIntent = intent,
                contentRelevance = if (alignment == GoalAlignment.GOAL_ALIGNED) "HIGH" else "LOW",
                personalRelevance = "HIGH",
                routineMatch = "HIGH",
                positiveEvidence = listOf("✓ App-level user override applied ($cat)"),
                personalEvidence = listOf("✓ User explicit app override"),
                explanationReasons = listOf(
                    "✓ User explicit override applied for app $appName",
                    "✓ Category set to '$cat' (${alignment.displayName})"
                ),
                observedEvidence = observedEvidence,
                modelMaturity = academicContext.personalProfile.maturity
            )
        }

        // =========================================================================
        // CASE: UNAVAILABLE TITLE -> UNKNOWN IMMEDIATELY
        // =========================================================================
        if (isUnavailableTitle) {
            return ClassificationResult(
                category = CAT_OTHER,
                goalAlignment = GoalAlignment.UNKNOWN,
                goalScore = 0,
                confidence = 0.40f,
                confidenceLevel = ConfidenceLevel.LOW,
                classificationSource = "UNKNOWN_INSUFFICIENT_EVIDENCE",
                primaryReason = "Insufficient Observable Content",
                detectedIntent = UserIntent.UNKNOWN,
                contentRelevance = "NONE",
                personalRelevance = "NONE",
                routineMatch = "NONE",
                explanationReasons = listOf(
                    "❓ Accessibility tree did not provide sufficient text",
                    "✓ Preserved as Unknown without fabricating data"
                ),
                observedEvidence = observedEvidence,
                modelMaturity = academicContext.personalProfile.maturity
            )
        }

        // =========================================================================
        // LAYER 2: DETECT INTENT & EXTRACT EVIDENCE
        // =========================================================================
        val detectedIntent = detectIntent(packageName, appName, activityType, contentTitle, domainOrSubtext)
        val derivedTopicGraph = deriveRelatedTopics(academicContext)

        var rawScore = 0
        val positiveEvidence = mutableListOf<String>()
        val negativeEvidence = mutableListOf<String>()
        val matchedTopics = mutableListOf<String>()
        var matchedGoal: String? = null

        // 2a. Anti-False-Positive Meme/Comedy/Roast Shield
        val matchedAntiModifier = ANTI_STUDY_MODIFIERS.find { titleLower.contains(it) }
        val hasAcademicSignal = EDUCATIONAL_INTENT_SIGNALS.any { titleLower.contains(it) }

        if (matchedAntiModifier != null && !hasAcademicSignal) {
            rawScore -= 50
            negativeEvidence.add("⚠️ Contains comedy/meme/roast modifier: '$matchedAntiModifier'")
        }

        // 2b. Explicit Short-form video (Reels / Shorts)
        val isShorts = activityType.equals("Shorts", ignoreCase = true) || titleLower.contains("shorts") || (activityType.equals("Reel", ignoreCase = true) || titleLower.contains("reels"))
        if (isShorts) {
            rawScore -= 35
            negativeEvidence.add("⚠️ Short-form viral format (Reels/Shorts)")
        }

        // 2c. Shopping or Gaming
        if (detectedIntent == UserIntent.SHOPPING) {
            rawScore -= 35
            negativeEvidence.add("⚠️ Shopping / Commercial intent detected")
        } else if (detectedIntent == UserIntent.GAMING) {
            rawScore -= 40
            negativeEvidence.add("⚠️ Gaming application / stream detected")
        } else if (detectedIntent == UserIntent.ENTERTAINMENT && matchedAntiModifier == null) {
            rawScore -= 30
            negativeEvidence.add("⚠️ Entertainment intent detected")
        }

        // =========================================================================
        // LAYER 3: POSITIVE SEMANTIC EVIDENCE ACCUMULATION
        // =========================================================================

        // 3a. Direct User Goal Match
        for (goal in academicContext.mainGoals) {
            if (goal.isNotBlank() && goal.length >= 3 && textToAnalyze.contains(goal.lowercase(Locale.ROOT))) {
                rawScore += 45
                matchedGoal = goal
                matchedTopics.add(goal)
                positiveEvidence.add("✓ Direct match with User Goal: '$goal'")
                break
            }
        }

        // 3b. Subject / Course Match
        for (subj in academicContext.subjects) {
            if (subj.isNotBlank() && subj.length >= 3 && textToAnalyze.contains(subj.lowercase(Locale.ROOT))) {
                rawScore += 40
                if (matchedGoal == null) matchedGoal = subj
                matchedTopics.add(subj)
                positiveEvidence.add("✓ Matches Registered Subject: '$subj'")
                break
            }
        }
        for (course in academicContext.courses) {
            if (course.isNotBlank() && course.length >= 3 && textToAnalyze.contains(course.lowercase(Locale.ROOT))) {
                rawScore += 40
                if (matchedGoal == null) matchedGoal = course
                matchedTopics.add(course)
                positiveEvidence.add("✓ Matches Registered Course: '$course'")
                break
            }
        }

        // 3c. Exam / Task Match (supports exact match or key token match)
        for (exam in academicContext.exams) {
            val examTokens = exam.split("\\s+".toRegex()).filter { it.length >= 3 && it.lowercase(Locale.ROOT) !in setOf("exam", "test", "prep", "mid-term", "term") }
            val examMatches = examTokens.isNotEmpty() && examTokens.any { textToAnalyze.contains(it.lowercase(Locale.ROOT)) }
            if (exam.isNotBlank() && (textToAnalyze.contains(exam.lowercase(Locale.ROOT)) || examMatches)) {
                rawScore += 35
                if (matchedGoal == null) matchedGoal = exam
                matchedTopics.add(exam)
                positiveEvidence.add("✓ Exam Preparation Relevance: '$exam'")
                break
            }
        }
        for (task in academicContext.tasks) {
            val taskTokens = task.split("\\s+".toRegex()).filter { it.length >= 4 && it.lowercase(Locale.ROOT) !in setOf("complete", "finish", "chapter", "read", "study", "assignment", "homework", "task") }
            val taskMatches = taskTokens.isNotEmpty() && taskTokens.any { textToAnalyze.contains(it.lowercase(Locale.ROOT)) }
            if (task.isNotBlank() && (textToAnalyze.contains(task.lowercase(Locale.ROOT)) || taskMatches)) {
                rawScore += 35
                matchedTopics.add(task)
                positiveEvidence.add("✓ Matches Active Study Task: '$task'")
                break
            }
        }

        // 3d. Semantic Related Topics Match (from dynamic knowledge graph)
        for ((parentGoal, subtopics) in derivedTopicGraph) {
            for (topic in subtopics) {
                if (topic.isNotBlank() && topic.length >= 3 && textToAnalyze.contains(topic.lowercase(Locale.ROOT))) {
                    if (topic !in matchedTopics) {
                        rawScore += 30
                        matchedTopics.add(topic)
                        if (matchedGoal == null) matchedGoal = parentGoal
                        positiveEvidence.add("✓ Related Topic '$topic' (Connected to '$parentGoal')")
                    }
                }
            }
        }

        // 3e. Universal Coding & Practicing Match
        val matchedCodingSignals = CODING_PRACTICE_SIGNALS.filter { textToAnalyze.contains(it) }
        if (matchedCodingSignals.isNotEmpty()) {
            rawScore += 55
            matchedTopics.addAll(matchedCodingSignals)
            positiveEvidence.add("✓ Coding / Problem Solving Signal: '${matchedCodingSignals.first()}'")
        }

        // 3f. Educational Intent Signals
        val matchedEduSignals = EDUCATIONAL_INTENT_SIGNALS.filter { titleLower.contains(it) || textToAnalyze.contains(it) }
        if (matchedEduSignals.isNotEmpty()) {
            val eduScore = (matchedEduSignals.size * 30).coerceAtMost(65)
            rawScore += eduScore
            positiveEvidence.add("✓ Educational Intent Pattern: '${matchedEduSignals.first()}'")
        }

        // 3g. Academic Communication Context
        val isCommApp = pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
                pkg.contains("discord") || pkg.contains("slack") || pkg.contains("teams") || pkg.contains("meet")
        if (isCommApp) {
            val matchedAcademicComm = ACADEMIC_COMMUNICATION_KEYWORDS.find { textToAnalyze.contains(it) }
            if (matchedAcademicComm != null) {
                rawScore += 40
                positiveEvidence.add("✓ Academic Coursework / Group Context: '$matchedAcademicComm'")
            }
        }

        // 3h. Historical User Corrections Learning
        for ((correctionKey, correctionCat) in academicContext.userCorrections) {
            if (textToAnalyze.contains(correctionKey.lowercase(Locale.ROOT))) {
                if (correctionCat == CAT_LEARNING || correctionCat == CAT_CODING || correctionCat == CAT_PRODUCTIVITY) {
                    rawScore += 55
                    positiveEvidence.add("✓ Boosted by previous positive feedback for '$correctionKey'")
                } else {
                    rawScore -= 40
                    negativeEvidence.add("⚠️ Adjusted by previous negative feedback for '$correctionKey'")
                }
                break
            }
        }

        // Determine base category
        val category = when {
            negativeEvidence.isNotEmpty() && (matchedAntiModifier != null || isShorts || detectedIntent == UserIntent.ENTERTAINMENT || detectedIntent == UserIntent.GAMING || detectedIntent == UserIntent.SHOPPING) -> {
                if (detectedIntent == UserIntent.GAMING) CAT_GAMING
                else if (detectedIntent == UserIntent.SHOPPING) CAT_OTHER
                else if (detectedIntent == UserIntent.SOCIAL_BROWSING) CAT_SOCIAL
                else CAT_ENTERTAINMENT
            }
            isCommApp -> CAT_COMMUNICATION
            matchedCodingSignals.isNotEmpty() || detectedIntent == UserIntent.CODING || detectedIntent == UserIntent.PRACTICING -> CAT_CODING
            positiveEvidence.isNotEmpty() && negativeEvidence.isEmpty() -> CAT_LEARNING
            detectedIntent == UserIntent.GAMING -> CAT_GAMING
            detectedIntent == UserIntent.SHOPPING || (pkg.contains("chrome") && negativeEvidence.isNotEmpty()) -> CAT_OTHER
            detectedIntent == UserIntent.SOCIAL_BROWSING && positiveEvidence.isEmpty() -> CAT_SOCIAL
            detectedIntent == UserIntent.MUSIC -> CAT_ENTERTAINMENT
            negativeEvidence.isNotEmpty() || detectedIntent == UserIntent.ENTERTAINMENT -> CAT_ENTERTAINMENT
            pkg.contains("chrome") || pkg.contains("browser") -> CAT_BROWSING
            else -> CAT_OTHER
        }

        // =========================================================================
        // LAYER 4: PERSONAL BEHAVIORAL CONTEXT & EVIDENCE FUSION
        // =========================================================================
        val personalEval = PersonalLearningEngine.evaluatePersonalContext(
            packageName = packageName,
            contentTitle = contentTitle,
            matchedGoal = matchedGoal,
            matchedTopics = matchedTopics.distinct(),
            rawContentScore = rawScore,
            category = category,
            timestamp = timestamp,
            profile = academicContext.personalProfile
        )

        // Fuse scores: add personal & routine bonuses ONLY if negative distraction signals are not present
        if (negativeEvidence.isEmpty()) {
            rawScore += personalEval.personalScoreBonus
            rawScore += personalEval.routineScoreBonus
        }

        val normalizedScore = rawScore.coerceIn(0, 100)

        // Determine 6-Tier Goal Alignment
        val goalAlignment: GoalAlignment
        val confidenceLevel: ConfidenceLevel
        val confidenceScore: Float
        val classificationSource: String
        val primaryReason: String

        when {
            // Case 1: Strong negative signals / Meme / Distraction
            negativeEvidence.isNotEmpty() && (matchedAntiModifier != null || isShorts || detectedIntent == UserIntent.GAMING || detectedIntent == UserIntent.SHOPPING || detectedIntent == UserIntent.ENTERTAINMENT) -> {
                goalAlignment = GoalAlignment.OFF_GOAL
                confidenceLevel = ConfidenceLevel.HIGH
                confidenceScore = 0.94f
                classificationSource = if (matchedAntiModifier != null) "ANTI_FALSE_POSITIVE_RULE" else "EXPLICIT_DISTRACTION"
                primaryReason = negativeEvidence.firstOrNull()?.removePrefix("⚠️ ") ?: "Entertainment / Distraction"
            }

            // Case 2: Goal-Aligned (Score >= 55 with verified positive evidence)
            normalizedScore >= 55 && positiveEvidence.isNotEmpty() -> {
                goalAlignment = GoalAlignment.GOAL_ALIGNED
                confidenceLevel = if (titleLower.length >= 8 || personalEval.personalEvidence.isNotEmpty()) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                confidenceScore = if (confidenceLevel == ConfidenceLevel.HIGH) 0.95f else 0.82f
                classificationSource = if (matchedGoal != null) "EXPLICIT_GOAL_MATCH" else "STRONG_CONTENT_EVIDENCE"
                primaryReason = positiveEvidence.firstOrNull()?.removePrefix("✓ ") ?: "Goal-Aligned Learning"
            }

            // Case 3: Highly Relevant (Score in 45..54)
            normalizedScore >= 45 -> {
                goalAlignment = GoalAlignment.HIGHLY_RELEVANT
                confidenceLevel = ConfidenceLevel.HIGH
                confidenceScore = 0.88f
                classificationSource = "SEMANTIC_RELEVANCE"
                primaryReason = positiveEvidence.firstOrNull()?.removePrefix("✓ ") ?: "Highly Relevant to Study Context"
            }

            // Case 4: Possibly Relevant (Score in 30..44 or productivity/reference browsing)
            normalizedScore >= 30 || category == CAT_PRODUCTIVITY || (category == CAT_BROWSING && positiveEvidence.isNotEmpty()) -> {
                goalAlignment = GoalAlignment.POSSIBLY_RELEVANT
                confidenceLevel = ConfidenceLevel.MEDIUM
                confidenceScore = 0.75f
                classificationSource = "PRODUCTIVITY_SIGNAL"
                primaryReason = if (positiveEvidence.isNotEmpty()) positiveEvidence.first().removePrefix("✓ ") else "Productivity & Reference Context"
            }

            // Case 5: Neutral (Communication, System tools, Background Music)
            isCommApp || detectedIntent == UserIntent.MUSIC || category == CAT_COMMUNICATION -> {
                goalAlignment = GoalAlignment.NEUTRAL
                confidenceLevel = ConfidenceLevel.MEDIUM
                confidenceScore = 0.80f
                classificationSource = "APP_STRUCTURAL"
                primaryReason = if (detectedIntent == UserIntent.MUSIC) "Music / Audio (Neutral)" else "General Communication (Neutral)"
            }

            // Case 6: Off-Goal / General Non-Educational Screen Time
            else -> {
                goalAlignment = GoalAlignment.OFF_GOAL
                confidenceLevel = if (pkg.contains("youtube") || pkg.contains("instagram")) ConfidenceLevel.MEDIUM else ConfidenceLevel.LOW
                confidenceScore = if (confidenceLevel == ConfidenceLevel.MEDIUM) 0.75f else 0.55f
                classificationSource = "APP_STRUCTURAL"
                primaryReason = "Non-Goal Screen Time"
            }
        }

        // Build human-readable explanation bullet points
        val explanationReasons = mutableListOf<String>()
        if (positiveEvidence.isNotEmpty()) {
            explanationReasons.addAll(positiveEvidence)
        }
        if (personalEval.personalEvidence.isNotEmpty()) {
            explanationReasons.addAll(personalEval.personalEvidence)
        }
        if (personalEval.historicalEvidence.isNotEmpty()) {
            explanationReasons.addAll(personalEval.historicalEvidence)
        }
        if (negativeEvidence.isNotEmpty()) {
            explanationReasons.addAll(negativeEvidence)
        }
        if (explanationReasons.isEmpty()) {
            if (goalAlignment == GoalAlignment.UNKNOWN) {
                explanationReasons.add("❓ Accessibility tree did not provide sufficient text")
                explanationReasons.add("✓ Preserved as Unknown without fabricating data")
            } else if (goalAlignment == GoalAlignment.NEUTRAL) {
                explanationReasons.add("⚪ Neutral context (neither direct study nor explicit distraction)")
            } else {
                explanationReasons.add("⚠️ General non-academic screen activity detected")
            }
        }

        val activePred = academicContext.personalProfile.activePredictions.firstOrNull()?.predictionText

        val result = ClassificationResult(
            category = category,
            goalAlignment = goalAlignment,
            goalScore = normalizedScore,
            confidence = confidenceScore,
            confidenceLevel = confidenceLevel,
            classificationSource = classificationSource,
            primaryReason = primaryReason,
            matchedGoal = matchedGoal,
            matchedTopics = matchedTopics.distinct(),
            detectedIntent = detectedIntent,
            contentRelevance = personalEval.contentRelevance,
            personalRelevance = personalEval.personalRelevance,
            routineMatch = personalEval.routineMatch,
            positiveEvidence = positiveEvidence,
            negativeEvidence = negativeEvidence,
            personalEvidence = personalEval.personalEvidence,
            historicalEvidence = personalEval.historicalEvidence,
            explanationReasons = explanationReasons,
            modelMaturity = academicContext.personalProfile.maturity,
            predictedRoutine = activePred,
            observedEvidence = observedEvidence
        )

        totalClassifierTimeNanos.addAndGet(System.nanoTime() - startTimeNanos)
        classificationCache[cacheKey] = result
        return result
    }

    private fun mapCategoryToGoalAlignment(cat: String): GoalAlignment {
        return when (cat) {
            CAT_LEARNING, CAT_CODING -> GoalAlignment.GOAL_ALIGNED
            CAT_PRODUCTIVITY -> GoalAlignment.POSSIBLY_RELEVANT
            CAT_COMMUNICATION -> GoalAlignment.NEUTRAL
            CAT_ENTERTAINMENT, CAT_SOCIAL, CAT_GAMING -> GoalAlignment.OFF_GOAL
            CAT_BROWSING -> GoalAlignment.POSSIBLY_RELEVANT
            else -> GoalAlignment.UNKNOWN
        }
    }

    /**
     * Generates a daily executive focus insight summary based on aggregated session evidence.
     */
    fun generateDailyInsights(sessions: List<AppActivitySessionEntity>): String {
        if (sessions.isEmpty()) {
            return "No activity recorded for this date yet. Open your apps to track focus and goal progress."
        }

        val totalActiveMillis = sessions.sumOf { it.durationMillis }
        if (totalActiveMillis <= 0) return "Active screen time under 1 minute."

        val goalAlignedMillis = sessions.filter { it.goalAlignment == GoalAlignment.GOAL_ALIGNED.name || it.goalAlignment == GoalAlignment.HIGHLY_RELEVANT.name }.sumOf { it.durationMillis }
        val offGoalMillis = sessions.filter { it.goalAlignment == GoalAlignment.OFF_GOAL.name }.sumOf { it.durationMillis }
        val unknownMillis = sessions.filter { it.goalAlignment == GoalAlignment.UNKNOWN.name }.sumOf { it.durationMillis }

        val classifiedMillis = totalActiveMillis - unknownMillis
        val alignmentPercent = if (classifiedMillis > 0) ((goalAlignedMillis * 100) / classifiedMillis).toInt() else 0
        val coveragePercent = if (totalActiveMillis > 0) ((classifiedMillis * 100) / totalActiveMillis).toInt() else 0

        val goalMins = (goalAlignedMillis / (1000 * 60))
        val offMins = (offGoalMillis / (1000 * 60))

        // Find top goal or topics
        val topTopic = sessions.filter { it.goalAlignment == GoalAlignment.GOAL_ALIGNED.name }
            .groupBy { it.contentTitle }
            .maxByOrNull { it.value.sumOf { s -> s.durationMillis } }
            ?.key ?: "Study & Coding"

        val sb = StringBuilder()
        sb.append("🎯 Estimated Goal Alignment: $alignmentPercent% (Coverage: $coveragePercent%)\n")
        sb.append("⏱️ Aligned Time: ${goalMins}m | Off-Goal: ${offMins}m\n")

        if (goalMins > 0) {
            sb.append("💡 Top focus activity: '$topTopic'. ")
        }
        if (offMins > goalMins) {
            sb.append("⚠️ Off-goal entertainment exceeded study time. Consider a focused pomodoro session.")
        } else if (goalMins > 0) {
            sb.append("✨ Great consistency moving toward registered study goals!")
        }

        return sb.toString().trim()
    }
}
