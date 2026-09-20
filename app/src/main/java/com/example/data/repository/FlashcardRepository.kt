package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Achievement
import com.example.data.model.Flashcard
import com.example.data.model.FlashcardDeck
import com.example.data.model.PracticeProblem
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizRecord
import com.example.data.model.SubjectMasteryStatus
import com.example.data.model.SubjectPerformanceSummary
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE_SAVED
}

class FlashcardRepository(private val context: Context) {

    companion object {
        private const val TAG = "FlashcardRepository"
        private const val PREFS_NAME = "flashsnap_prefs"
        private const val KEY_DECKS = "saved_decks_json"
        private const val KEY_PROFILE = "saved_profile_json"
        private const val KEY_SYNC_ACCOUNT_ID = "sync_account_id"
        private const val KEY_LAST_BACKUP = "last_backup_timestamp"
        private const val FIRESTORE_COLLECTION_DECKS = "flashcard_decks"
        private const val FIRESTORE_COLLECTION_USERS = "user_profiles"
        private const val FIRESTORE_COLLECTION_ACCOUNTS = "sync_accounts"
        private const val DEFAULT_USER_ID = "student_user_primary"
        private const val FILE_DECKS_STORE = "flashcard_decks_persistent.json"
        private const val FILE_DECKS_BACKUP = "flashcard_decks_backup.json"
        private const val FILE_PROFILE_STORE = "user_profile_persistent.json"
        private const val KEY_QUIZ_HISTORY = "quiz_history_records_json"
        private const val FILE_QUIZ_HISTORY_STORE = "quiz_history_persistent.json"
        const val DEFAULT_WEBSITE_URL = ""
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _decks = MutableStateFlow<List<FlashcardDeck>>(emptyList())
    val decks: StateFlow<List<FlashcardDeck>> = _decks.asStateFlow()

    private val _quizHistory = MutableStateFlow<List<QuizRecord>>(emptyList())
    val quizHistory: StateFlow<List<QuizRecord>> = _quizHistory.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.IDLE)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _syncAccountId = MutableStateFlow(
        prefs.getString(KEY_SYNC_ACCOUNT_ID, "") ?: ""
    )
    val syncAccountId: StateFlow<String> = _syncAccountId.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(
        if (prefs.contains(KEY_LAST_BACKUP)) prefs.getLong(KEY_LAST_BACKUP, 0L) else null
    )
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var realtimeDb: FirebaseDatabase? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase Firestore initialized successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Firestore not available in this environment: ${e.message}")
        }
        try {
            realtimeDb = FirebaseDatabase.getInstance("https://study-space-a6417-default-rtdb.firebaseio.com")
            Log.d(TAG, "Firebase Realtime Database initialized successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Realtime Database notice: ${e.message}")
        }
        loadLocalData()
        syncWithCloud()
    }

    private fun loadLocalData() {
        var savedProfileJson = prefs.getString(KEY_PROFILE, null)
        if (savedProfileJson.isNullOrBlank()) {
            try {
                val profileFile = File(context.filesDir, FILE_PROFILE_STORE)
                if (profileFile.exists()) {
                    savedProfileJson = profileFile.readText(Charsets.UTF_8)
                    Log.d(TAG, "Restored profile from internal disk store ($FILE_PROFILE_STORE)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice reading profile file: ${e.message}")
            }
        }

        if (savedProfileJson != null) {
            try {
                val profileObj = JSONObject(savedProfileJson)
                val name = profileObj.optString("name", "Student")
                val studyField = profileObj.optString("studyField", "")
                val studyGoal = profileObj.optString("studyGoal", "")
                val avatarEmoji = profileObj.optString("avatarEmoji", "🎓")
                val studySpaceWorkspaceId = profileObj.optString("studySpaceWorkspaceId", "")
                val studySpaceSyncEnabled = profileObj.optBoolean("studySpaceSyncEnabled", false)
                val xp = profileObj.optInt("xp", 0)
                val streakDays = profileObj.optInt("streakDays", 0)
                val totalQuizzesTaken = profileObj.optInt("totalQuizzesTaken", 0)
                val totalCardsMastered = profileObj.optInt("totalCardsMastered", 0)
                val correctAnswers = profileObj.optInt("correctAnswers", 0)
                val wrongAnswers = profileObj.optInt("wrongAnswers", 0)
                val netQuizPoints = profileObj.optInt("netQuizPoints", 0)
                val studyLanguage = profileObj.optString("studyLanguage", "EN")
                val reminderEnabled = profileObj.optBoolean("reminderEnabled", true)
                val reminderHour = profileObj.optInt("reminderHour", 20)
                val reminderMinute = profileObj.optInt("reminderMinute", 0)
                val typicalStudyTimeLabel = profileObj.optString("typicalStudyTimeLabel", "8:00 PM")
                var websiteUrl = profileObj.optString("websiteUrl", "")
                if (websiteUrl.contains("studyspace-nightmare") || websiteUrl.contains("study-space-a6417")) {
                    websiteUrl = ""
                }

                _userProfile.value = UserProfile(
                    name = name,
                    studyField = studyField,
                    studyGoal = studyGoal,
                    avatarEmoji = avatarEmoji,
                    studySpaceWorkspaceId = studySpaceWorkspaceId,
                    studySpaceSyncEnabled = studySpaceSyncEnabled,
                    websiteUrl = websiteUrl,
                    xp = xp,
                    streakDays = streakDays,
                    totalQuizzesTaken = totalQuizzesTaken,
                    totalCardsMastered = totalCardsMastered,
                    correctAnswers = correctAnswers,
                    wrongAnswers = wrongAnswers,
                    netQuizPoints = netQuizPoints,
                    studyLanguage = studyLanguage,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    typicalStudyTimeLabel = typicalStudyTimeLabel
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved profile: ${e.message}")
            }
        }

        // Multi-tier local loading for flashcard decks:
        // 1. SharedPreferences fast-cache
        // 2. Persistent internal disk file (survives APK updates / data wipes)
        // 3. Redundancy backup file
        var loadedDecks: List<FlashcardDeck>? = null

        val savedDecksJson = prefs.getString(KEY_DECKS, null)
        if (!savedDecksJson.isNullOrBlank()) {
            try {
                val parsed = parseDecksFromJson(savedDecksJson)
                if (parsed.isNotEmpty()) {
                    loadedDecks = parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice parsing decks from prefs: ${e.message}")
            }
        }

        if (loadedDecks == null || loadedDecks.isEmpty()) {
            try {
                val storeFile = File(context.filesDir, FILE_DECKS_STORE)
                if (storeFile.exists()) {
                    val fileContent = storeFile.readText(Charsets.UTF_8)
                    val parsed = parseDecksFromJson(fileContent)
                    if (parsed.isNotEmpty()) {
                        Log.d(TAG, "Successfully restored ${parsed.size} decks from internal disk file ($FILE_DECKS_STORE)")
                        loadedDecks = parsed
                        prefs.edit().putString(KEY_DECKS, fileContent).apply()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice reading decks from disk store file: ${e.message}")
            }
        }

        if (loadedDecks == null || loadedDecks.isEmpty()) {
            try {
                val backupFile = File(context.filesDir, FILE_DECKS_BACKUP)
                if (backupFile.exists()) {
                    val backupContent = backupFile.readText(Charsets.UTF_8)
                    val parsed = parseDecksFromJson(backupContent)
                    if (parsed.isNotEmpty()) {
                        Log.d(TAG, "Successfully restored ${parsed.size} decks from backup file ($FILE_DECKS_BACKUP)")
                        loadedDecks = parsed
                        prefs.edit().putString(KEY_DECKS, backupContent).apply()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice reading decks from backup file: ${e.message}")
            }
        }

        val nonStarterDecks = loadedDecks?.filterNot { it.id.startsWith("starter_") } ?: emptyList()
        _decks.value = nonStarterDecks
        saveLocalDecks(nonStarterDecks)

        // Load Quiz Performance History
        loadQuizHistory()
    }

    fun saveDeck(deck: FlashcardDeck) {
        val current = _decks.value.toMutableList()
        val index = current.indexOfFirst { it.id == deck.id }
        if (index >= 0) {
            current[index] = deck
        } else {
            current.add(0, deck)
        }
        _decks.value = current
        saveLocalDecks(current)

        // Save to Firebase Cloud
        uploadDeckToFirebase(deck)
    }

    fun updateDeckCardMastery(deckId: String, cardId: String, isMastered: Boolean) {
        val currentDecks = _decks.value.map { deck ->
            if (deck.id == deckId) {
                val updatedCards = deck.cards.map { card ->
                    if (card.id == cardId) {
                        card.copy(
                            isMastered = isMastered,
                            reviewCount = card.reviewCount + 1
                        )
                    } else card
                }
                deck.copy(cards = updatedCards)
            } else deck
        }
        _decks.value = currentDecks
        saveLocalDecks(currentDecks)

        val updatedMasteredCount = currentDecks.sumOf { d -> d.cards.count { it.isMastered } }
        val currentProfile = _userProfile.value
        val xpGain = if (isMastered) 5 else 0
        updateProfile(
            currentProfile.copy(
                xp = currentProfile.xp + xpGain,
                totalCardsMastered = updatedMasteredCount
            )
        )
    }

    fun updateProblemSolved(deckId: String, problemId: String, isSolved: Boolean) {
        val currentDecks = _decks.value.map { deck ->
            if (deck.id == deckId) {
                val updatedProblems = deck.practiceProblems.map { p ->
                    if (p.id == problemId) p.copy(isSolved = isSolved) else p
                }
                deck.copy(practiceProblems = updatedProblems)
            } else deck
        }
        _decks.value = currentDecks
        saveLocalDecks(currentDecks)
        if (isSolved) {
            val currentProfile = _userProfile.value
            updateProfile(currentProfile.copy(xp = currentProfile.xp + 20))
        }
    }

    fun updateProblemDraft(deckId: String, problemId: String, draft: String) {
        val currentDecks = _decks.value.map { deck ->
            if (deck.id == deckId) {
                val updatedProblems = deck.practiceProblems.map { p ->
                    if (p.id == problemId) p.copy(userDraftAnswer = draft) else p
                }
                deck.copy(practiceProblems = updatedProblems)
            } else deck
        }
        _decks.value = currentDecks
        saveLocalDecks(currentDecks)
    }

    fun recordQuizResult(
        deckId: String,
        deckTitle: String,
        subject: String,
        correctCount: Int,
        wrongCount: Int,
        netPoints: Int,
        durationSeconds: Int = 0
    ) {
        val total = correctCount + wrongCount
        val record = QuizRecord(
            deckId = deckId,
            deckTitle = if (deckTitle.isNotBlank()) deckTitle else "Quiz",
            subject = if (subject.isNotBlank()) subject else "General",
            timestamp = System.currentTimeMillis(),
            totalQuestions = total,
            correctCount = correctCount,
            wrongCount = wrongCount,
            netPoints = netPoints,
            durationSeconds = durationSeconds
        )
        val currentHistory = _quizHistory.value.toMutableList()
        currentHistory.add(0, record)
        _quizHistory.value = currentHistory
        saveQuizHistory(currentHistory)

        val current = _userProfile.value
        val newXp = maxOf(0, current.xp + netPoints)
        val updatedProfile = current.copy(
            xp = newXp,
            totalQuizzesTaken = current.totalQuizzesTaken + 1,
            correctAnswers = current.correctAnswers + correctCount,
            wrongAnswers = current.wrongAnswers + wrongCount,
            netQuizPoints = current.netQuizPoints + netPoints
        )
        updateProfile(updatedProfile)
    }

    fun recordQuizResult(correctCount: Int, wrongCount: Int, netPoints: Int) {
        recordQuizResult(
            deckId = "",
            deckTitle = "Practice Quiz",
            subject = "General",
            correctCount = correctCount,
            wrongCount = wrongCount,
            netPoints = netPoints
        )
    }

    private fun loadQuizHistory() {
        var savedHistoryJson = prefs.getString(KEY_QUIZ_HISTORY, null)
        if (savedHistoryJson.isNullOrBlank()) {
            try {
                val file = File(context.filesDir, FILE_QUIZ_HISTORY_STORE)
                if (file.exists()) {
                    savedHistoryJson = file.readText(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice reading quiz history file: ${e.message}")
            }
        }

        val loadedHistory = mutableListOf<QuizRecord>()
        if (!savedHistoryJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedHistoryJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val record = QuizRecord(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        deckId = obj.optString("deckId", ""),
                        deckTitle = obj.optString("deckTitle", "Quiz"),
                        subject = obj.optString("subject", "General"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        totalQuestions = obj.optInt("totalQuestions", 0),
                        correctCount = obj.optInt("correctCount", 0),
                        wrongCount = obj.optInt("wrongCount", 0),
                        netPoints = obj.optInt("netPoints", 0),
                        durationSeconds = obj.optInt("durationSeconds", 0)
                    )
                    loadedHistory.add(record)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing quiz history: ${e.message}")
            }
        }

        if (loadedHistory.isEmpty()) {
            val starterHistory = createStarterQuizHistory(_decks.value)
            _quizHistory.value = starterHistory
            saveQuizHistory(starterHistory)
        } else {
            _quizHistory.value = loadedHistory.sortedByDescending { it.timestamp }
        }
    }

    private fun saveQuizHistory(records: List<QuizRecord>) {
        try {
            val array = JSONArray()
            for (r in records) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("deckId", r.deckId)
                    put("deckTitle", r.deckTitle)
                    put("subject", r.subject)
                    put("timestamp", r.timestamp)
                    put("totalQuestions", r.totalQuestions)
                    put("correctCount", r.correctCount)
                    put("wrongCount", r.wrongCount)
                    put("netPoints", r.netPoints)
                    put("durationSeconds", r.durationSeconds)
                }
                array.put(obj)
            }
            val jsonStr = array.toString()
            prefs.edit().putString(KEY_QUIZ_HISTORY, jsonStr).apply()
            try {
                File(context.filesDir, FILE_QUIZ_HISTORY_STORE).writeText(jsonStr, Charsets.UTF_8)
            } catch (fe: Exception) {
                Log.w(TAG, "Notice writing quiz history file: ${fe.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving quiz history: ${e.message}")
        }
    }

    private fun createStarterQuizHistory(currentDecks: List<FlashcardDeck>): List<QuizRecord> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        val chemDeck = currentDecks.firstOrNull { it.subject.equals("Chemistry", ignoreCase = true) }
        val physDeck = currentDecks.firstOrNull { it.subject.equals("Physics", ignoreCase = true) }
        val mathDeck = currentDecks.firstOrNull { it.subject.equals("Math", ignoreCase = true) || it.subject.equals("Mathematics", ignoreCase = true) }
        val bioDeck = currentDecks.firstOrNull { it.subject.equals("Biology", ignoreCase = true) }

        return listOf(
            QuizRecord(
                deckId = physDeck?.id ?: "physics_01",
                deckTitle = physDeck?.title ?: "Kinematics & Projectile Motion",
                subject = "Physics",
                timestamp = now - (dayMs * 1L) + 3600000L,
                totalQuestions = 10,
                correctCount = 8,
                wrongCount = 2,
                netPoints = 8 * 15 - 2 * 10,
                durationSeconds = 145
            ),
            QuizRecord(
                deckId = chemDeck?.id ?: "chemistry_01",
                deckTitle = chemDeck?.title ?: "Acids, Bases & pH Calculations",
                subject = "Chemistry",
                timestamp = now - (dayMs * 2L) + 7200000L,
                totalQuestions = 8,
                correctCount = 4,
                wrongCount = 4,
                netPoints = 4 * 15 - 4 * 10,
                durationSeconds = 190
            ),
            QuizRecord(
                deckId = mathDeck?.id ?: "math_01",
                deckTitle = mathDeck?.title ?: "Differential Calculus & Limits",
                subject = "Math",
                timestamp = now - (dayMs * 3L) + 1800000L,
                totalQuestions = 10,
                correctCount = 9,
                wrongCount = 1,
                netPoints = 9 * 15 - 1 * 10,
                durationSeconds = 120
            ),
            QuizRecord(
                deckId = chemDeck?.id ?: "chemistry_02",
                deckTitle = chemDeck?.title ?: "Chemical Equilibrium & Le Chatelier",
                subject = "Chemistry",
                timestamp = now - (dayMs * 4L) + 5400000L,
                totalQuestions = 6,
                correctCount = 3,
                wrongCount = 3,
                netPoints = 3 * 15 - 3 * 10,
                durationSeconds = 160
            ),
            QuizRecord(
                deckId = bioDeck?.id ?: "bio_01",
                deckTitle = bioDeck?.title ?: "Cell Respiration & ATP Cycle",
                subject = "Biology",
                timestamp = now - (dayMs * 5L) + 3600000L,
                totalQuestions = 8,
                correctCount = 7,
                wrongCount = 1,
                netPoints = 7 * 15 - 1 * 10,
                durationSeconds = 110
            ),
            QuizRecord(
                deckId = physDeck?.id ?: "physics_02",
                deckTitle = physDeck?.title ?: "Newton's Laws & Force Vectors",
                subject = "Physics",
                timestamp = now - (dayMs * 6L) + 2400000L,
                totalQuestions = 8,
                correctCount = 6,
                wrongCount = 2,
                netPoints = 6 * 15 - 2 * 10,
                durationSeconds = 135
            ),
            QuizRecord(
                deckId = mathDeck?.id ?: "math_02",
                deckTitle = mathDeck?.title ?: "Integration & Definite Integrals",
                subject = "Math",
                timestamp = now - (dayMs * 7L) + 4800000L,
                totalQuestions = 8,
                correctCount = 7,
                wrongCount = 1,
                netPoints = 7 * 15 - 1 * 10,
                durationSeconds = 150
            )
        )
    }

    fun getSubjectPerformanceSummaries(): List<SubjectPerformanceSummary> {
        val history = _quizHistory.value
        val allDecks = _decks.value
        if (history.isEmpty()) return emptyList()

        val grouped = history.groupBy { it.subject.ifBlank { "General" } }
        val summaries = mutableListOf<SubjectPerformanceSummary>()

        for ((subject, records) in grouped) {
            val totalQuizzes = records.size
            val totalQuestions = records.sumOf { it.totalQuestions }
            val totalCorrect = records.sumOf { it.correctCount }
            val totalWrong = records.sumOf { it.wrongCount }
            val netPoints = records.sumOf { it.netPoints }
            val avgAccuracy = if (totalQuestions > 0) {
                ((totalCorrect.toFloat() / totalQuestions) * 100).toInt()
            } else 0
            val lastTimestamp = records.maxOfOrNull { it.timestamp } ?: 0L

            val masteryStatus = when {
                avgAccuracy < 70 -> SubjectMasteryStatus.NEEDS_REVIEW
                avgAccuracy < 85 -> SubjectMasteryStatus.DEVELOPING
                else -> SubjectMasteryStatus.MASTERED
            }

            val matchingDeck = allDecks.firstOrNull { it.subject.equals(subject, ignoreCase = true) }
                ?: records.firstOrNull()?.let { r -> allDecks.firstOrNull { it.id == r.deckId } }

            val weakTopicsSummary = if (masteryStatus == SubjectMasteryStatus.NEEDS_REVIEW) {
                "Recent accuracy is $avgAccuracy% ($totalWrong mistakes). Prioritize reviewing definitions & practice equations."
            } else if (masteryStatus == SubjectMasteryStatus.DEVELOPING) {
                "Steady progress at $avgAccuracy%. Review occasional missed cards to reach full mastery."
            } else {
                "Strong retention ($avgAccuracy% accuracy)! All key concepts mastered."
            }

            summaries.add(
                SubjectPerformanceSummary(
                    subject = subject,
                    totalQuizzes = totalQuizzes,
                    totalQuestions = totalQuestions,
                    correctCount = totalCorrect,
                    wrongCount = totalWrong,
                    averageAccuracy = avgAccuracy,
                    netPoints = netPoints,
                    lastQuizTimestamp = lastTimestamp,
                    masteryStatus = masteryStatus,
                    weakTopicsSummary = weakTopicsSummary,
                    recommendedDeckId = matchingDeck?.id ?: records.firstOrNull()?.deckId,
                    recommendedDeckTitle = matchingDeck?.title ?: records.firstOrNull()?.deckTitle
                )
            )
        }

        return summaries.sortedWith(
            compareBy(
                { it.masteryStatus.ordinal },
                { it.averageAccuracy }
            )
        )
    }

    fun clearQuizHistory() {
        _quizHistory.value = emptyList()
        prefs.edit().remove(KEY_QUIZ_HISTORY).apply()
        try {
            val file = File(context.filesDir, FILE_QUIZ_HISTORY_STORE)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Notice clearing quiz history file: ${e.message}")
        }
    }

    fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
        // Save local
        try {
            val json = JSONObject().apply {
                put("name", profile.name)
                put("studyField", profile.studyField)
                put("studyGoal", profile.studyGoal)
                put("avatarEmoji", profile.avatarEmoji)
                put("studySpaceWorkspaceId", profile.studySpaceWorkspaceId)
                put("studySpaceSyncEnabled", profile.studySpaceSyncEnabled)
                put("websiteUrl", profile.websiteUrl)
                put("xp", profile.xp)
                put("streakDays", profile.streakDays)
                put("totalQuizzesTaken", profile.totalQuizzesTaken)
                put("totalCardsMastered", profile.totalCardsMastered)
                put("correctAnswers", profile.correctAnswers)
                put("wrongAnswers", profile.wrongAnswers)
                put("netQuizPoints", profile.netQuizPoints)
                put("studyLanguage", profile.studyLanguage)
                put("reminderEnabled", profile.reminderEnabled)
                put("reminderHour", profile.reminderHour)
                put("reminderMinute", profile.reminderMinute)
                put("typicalStudyTimeLabel", profile.typicalStudyTimeLabel)
            }
            val jsonStr = json.toString()
            prefs.edit().putString(KEY_PROFILE, jsonStr).apply()
            try {
                val profileFile = File(context.filesDir, FILE_PROFILE_STORE)
                profileFile.writeText(jsonStr, Charsets.UTF_8)
            } catch (fe: Exception) {
                Log.w(TAG, "Notice writing profile file: ${fe.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile: ${e.message}")
        }

        // Upload to Firebase Firestore
        uploadProfileToFirebase(profile)
    }

    fun deleteDeck(deckId: String) {
        val current = _decks.value.filter { it.id != deckId }
        _decks.value = current
        saveLocalDecks(current)

        scope.launch {
            try {
                firestore?.collection(FIRESTORE_COLLECTION_DECKS)?.document(deckId)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete deck from Firestore: ${e.message}")
            }
        }
    }

    fun setSyncAccountId(accountId: String) {
        val clean = accountId.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        val finalId = if (clean.isNotBlank()) clean else "student_sync_1"
        _syncAccountId.value = finalId
        prefs.edit().putString(KEY_SYNC_ACCOUNT_ID, finalId).apply()
        _syncMessage.value = "Cloud Sync Account updated to: $finalId"
        syncWithCloud()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun backupAllToCloud(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        scope.launch {
            val fs = firestore
            val currentDecks = _decks.value
            val accountId = _syncAccountId.value
            val now = System.currentTimeMillis()

            if (fs == null) {
                // Offline fallback backup saved to persistent local cache
                prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()
                _lastBackupTime.value = now
                val msg = "Offline mode: ${currentDecks.size} decks (${currentDecks.sumOf { it.cards.size }} cards) securely cached locally."
                _syncMessage.value = msg
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
                onResult(true, msg)
                return@launch
            }

            try {
                _syncStatus.value = CloudSyncStatus.SYNCING

                // Upload each deck to sync_accounts/{accountId}/decks and flashcard_decks
                for (deck in currentDecks) {
                    val cardsList = deck.cards.map { c ->
                        mapOf(
                            "id" to c.id,
                            "front" to c.front,
                            "back" to c.back,
                            "keyTerm" to c.keyTerm,
                            "tag" to c.tag,
                            "difficulty" to c.difficulty,
                            "isMastered" to c.isMastered,
                            "reviewCount" to c.reviewCount
                        )
                    }

                    val quizList = deck.quiz.map { q ->
                        mapOf(
                            "id" to q.id,
                            "question" to q.question,
                            "options" to q.options,
                            "correctIndex" to q.correctIndex,
                            "explanation" to q.explanation,
                            "xpValue" to q.xpValue,
                            "penaltyXp" to q.penaltyXp
                        )
                    }

                    val deckData = mapOf(
                        "id" to deck.id,
                        "title" to deck.title,
                        "description" to deck.description,
                        "createdAt" to deck.createdAt,
                        "pagesCount" to deck.pagesCount,
                        "cards" to cardsList,
                        "quiz" to quizList,
                        "syncAccountId" to accountId,
                        "lastBackupTime" to now
                    )

                    // Write to account collection
                    fs.collection(FIRESTORE_COLLECTION_ACCOUNTS)
                        .document(accountId)
                        .collection("decks")
                        .document(deck.id)
                        .set(deckData, SetOptions.merge())
                        .await()

                    // Also mirror to global decks collection for broad retrieval
                    fs.collection(FIRESTORE_COLLECTION_DECKS)
                        .document(deck.id)
                        .set(deckData, SetOptions.merge())
                        .await()
                }

                // Write backup metadata document
                val totalCards = currentDecks.sumOf { it.cards.size }
                val profile = _userProfile.value
                val metaData = mapOf(
                    "accountId" to accountId,
                    "lastBackupTime" to now,
                    "deckCount" to currentDecks.size,
                    "cardCount" to totalCards,
                    "userXp" to profile.xp,
                    "userLevel" to profile.levelInfo.level,
                    "streakDays" to profile.streakDays
                )

                fs.collection(FIRESTORE_COLLECTION_ACCOUNTS)
                    .document(accountId)
                    .set(metaData, SetOptions.merge())
                    .await()

                // Mirror to Firebase Realtime Database for web application synchronization
                try {
                    val rtdb = realtimeDb
                    if (rtdb != null) {
                        for (deck in currentDecks) {
                            val deckMap = mapOf(
                                "id" to deck.id,
                                "title" to deck.title,
                                "description" to deck.description,
                                "createdAt" to deck.createdAt,
                                "pagesCount" to deck.pagesCount,
                                "cardsCount" to deck.cards.size,
                                "cards" to deck.cards.map { c ->
                                    mapOf(
                                        "id" to c.id,
                                        "front" to c.front,
                                        "back" to c.back,
                                        "frontHindi" to c.frontHindi,
                                        "backHindi" to c.backHindi,
                                        "keyTerm" to c.keyTerm,
                                        "tag" to c.tag,
                                        "difficulty" to c.difficulty,
                                        "subject" to c.subject,
                                        "reviewCount" to c.reviewCount,
                                        "isMastered" to c.isMastered
                                    )
                                }
                            )
                            rtdb.getReference("website_sync")
                                .child("decks")
                                .child(deck.id)
                                .setValue(deckMap)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Realtime DB backup write notice: ${e.message}")
                }

                prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()
                _lastBackupTime.value = now
                _syncStatus.value = CloudSyncStatus.SYNCED

                val successMsg = "Successfully backed up ${currentDecks.size} decks ($totalCards flashcards) to Firebase Cloud!"
                _syncMessage.value = successMsg
                onResult(true, successMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Cloud backup error: ${e.message}", e)
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
                val errMsg = "Cloud backup fallback: saved locally. (${e.message})"
                _syncMessage.value = errMsg
                onResult(false, errMsg)
            }
        }
    }

    fun syncWithWebsite(customUrl: String? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        scope.launch {
            _syncStatus.value = CloudSyncStatus.SYNCING
            val targetUrl = customUrl ?: _userProfile.value.websiteUrl
            backupAllToCloud { success, backupMsg ->
                if (success) {
                    val currentDecks = _decks.value
                    val now = System.currentTimeMillis()
                    try {
                        val metaMap = mapOf(
                            "lastSyncTimestamp" to now,
                            "deckCount" to currentDecks.size,
                            "cardCount" to currentDecks.sumOf { it.cards.size },
                            "websiteUrl" to targetUrl,
                            "projectId" to "study-space-a6417",
                            "firebaseUrl" to "https://study-space-a6417-default-rtdb.firebaseio.com"
                        )
                        realtimeDb?.getReference("website_sync")?.child("metadata")?.setValue(metaMap)
                    } catch (e: Exception) {
                        Log.w(TAG, "Realtime DB metadata write notice: ${e.message}")
                    }
                    val msg = "Connected & live synced with website ($targetUrl)! ${currentDecks.size} decks available."
                    _syncMessage.value = msg
                    _syncStatus.value = CloudSyncStatus.SYNCED
                    onResult(true, msg)
                } else {
                    onResult(false, backupMsg)
                }
            }
        }
    }

    fun restoreFromCloud(targetAccountId: String? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        scope.launch {
            val fs = firestore
            val accountId = (targetAccountId?.trim()?.takeIf { it.isNotBlank() } ?: _syncAccountId.value).lowercase()

            if (fs == null) {
                val msg = "Firestore unavailable. Loaded from local persistent cache."
                _syncMessage.value = msg
                onResult(false, msg)
                return@launch
            }

            try {
                _syncStatus.value = CloudSyncStatus.SYNCING

                // First check target account's subcollection
                var snapshot = fs.collection(FIRESTORE_COLLECTION_ACCOUNTS)
                    .document(accountId)
                    .collection("decks")
                    .get()
                    .await()

                // If empty, check Study Space application collections on Firebase
                if (snapshot.isEmpty) {
                    try {
                        val spaceSnap = fs.collection("study_space")
                            .document(accountId)
                            .collection("decks")
                            .get()
                            .await()
                        if (!spaceSnap.isEmpty) snapshot = spaceSnap
                    } catch (e: Exception) {
                        Log.w(TAG, "Study space query: ${e.message}")
                    }
                }

                if (snapshot.isEmpty) {
                    try {
                        val spaceDecksSnap = fs.collection("studyspace_decks").get().await()
                        if (!spaceDecksSnap.isEmpty) snapshot = spaceDecksSnap
                    } catch (e: Exception) {
                        Log.w(TAG, "Studyspace decks query: ${e.message}")
                    }
                }

                // If still empty, fall back to global decks
                if (snapshot.isEmpty) {
                    snapshot = fs.collection(FIRESTORE_COLLECTION_DECKS).get().await()
                }

                val restoredDecks = mutableListOf<FlashcardDeck>()
                for (doc in snapshot.documents) {
                    val title = doc.getString("title") ?: continue
                    val desc = doc.getString("description") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val pagesCount = doc.getLong("pagesCount")?.toInt() ?: 1

                    val cardsRaw = doc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                    val cards = cardsRaw.map { c ->
                        Flashcard(
                            id = c["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                            front = c["front"] as? String ?: "",
                            back = c["back"] as? String ?: "",
                            keyTerm = c["keyTerm"] as? String ?: "",
                            tag = c["tag"] as? String ?: "Core",
                            difficulty = c["difficulty"] as? String ?: "Medium",
                            isMastered = c["isMastered"] as? Boolean ?: false,
                            reviewCount = (c["reviewCount"] as? Long)?.toInt() ?: 0
                        )
                    }

                    val quizRaw = doc.get("quiz") as? List<Map<String, Any>> ?: emptyList()
                    val quiz = quizRaw.map { q ->
                        @Suppress("UNCHECKED_CAST")
                        val options = (q["options"] as? List<String>) ?: emptyList()
                        QuizQuestion(
                            id = q["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                            question = q["question"] as? String ?: "",
                            options = options,
                            correctIndex = (q["correctIndex"] as? Long)?.toInt() ?: 0,
                            explanation = q["explanation"] as? String ?: "",
                            xpValue = (q["xpValue"] as? Long)?.toInt() ?: 15,
                            penaltyXp = (q["penaltyXp"] as? Long)?.toInt() ?: 10
                        )
                    }

                    restoredDecks.add(
                        FlashcardDeck(
                            id = doc.id,
                            title = title,
                            description = desc,
                            createdAt = createdAt,
                            pagesCount = pagesCount,
                            cards = cards,
                            quiz = quiz,
                            isCloudSynced = true
                        )
                    )
                }

                // Also try to restore account metadata profile
                try {
                    val metaDoc = fs.collection(FIRESTORE_COLLECTION_ACCOUNTS).document(accountId).get().await()
                    if (metaDoc.exists()) {
                        val cloudXp = metaDoc.getLong("userXp")?.toInt()
                        val cloudStreak = metaDoc.getLong("streakDays")?.toInt()
                        if (cloudXp != null) {
                            val currentProf = _userProfile.value
                            updateProfile(
                                currentProf.copy(
                                    xp = maxOf(currentProf.xp, cloudXp),
                                    streakDays = maxOf(currentProf.streakDays, cloudStreak ?: 1)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Meta profile restore non-fatal: ${e.message}")
                }

                if (restoredDecks.isNotEmpty()) {
                    _decks.value = restoredDecks
                    saveLocalDecks(restoredDecks)
                    _syncStatus.value = CloudSyncStatus.SYNCED
                    val msg = "Retrieved ${restoredDecks.size} decks (${restoredDecks.sumOf { it.cards.size }} flashcards) from Cloud account '$accountId'!"
                    _syncMessage.value = msg
                    onResult(true, msg)
                } else {
                    val msg = "No cloud decks found for account '$accountId'. Retaining existing decks."
                    _syncMessage.value = msg
                    _syncStatus.value = CloudSyncStatus.SYNCED
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Restore error: ${e.message}", e)
                val msg = "Failed to restore from Cloud: ${e.message}"
                _syncMessage.value = msg
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
                onResult(false, msg)
            }
        }
    }

    fun syncWithCloud() {
        scope.launch {
            val fs = firestore ?: run {
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
                return@launch
            }
            try {
                _syncStatus.value = CloudSyncStatus.SYNCING
                val snapshot = fs.collection(FIRESTORE_COLLECTION_DECKS).get().await()
                val cloudDecks = mutableListOf<FlashcardDeck>()

                for (doc in snapshot.documents) {
                    val title = doc.getString("title") ?: continue
                    val desc = doc.getString("description") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val pagesCount = doc.getLong("pagesCount")?.toInt() ?: 1

                    val cardsRaw = doc.get("cards") as? List<Map<String, Any>> ?: emptyList()
                    val cards = cardsRaw.map { c ->
                        Flashcard(
                            id = c["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                            front = c["front"] as? String ?: "",
                            back = c["back"] as? String ?: "",
                            keyTerm = c["keyTerm"] as? String ?: "",
                            tag = c["tag"] as? String ?: "Core",
                            difficulty = c["difficulty"] as? String ?: "Medium",
                            isMastered = c["isMastered"] as? Boolean ?: false
                        )
                    }

                    val quizRaw = doc.get("quiz") as? List<Map<String, Any>> ?: emptyList()
                    val quiz = quizRaw.map { q ->
                        @Suppress("UNCHECKED_CAST")
                        val options = (q["options"] as? List<String>) ?: emptyList()
                        QuizQuestion(
                            id = q["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                            question = q["question"] as? String ?: "",
                            options = options,
                            correctIndex = (q["correctIndex"] as? Long)?.toInt() ?: 0,
                            explanation = q["explanation"] as? String ?: "",
                            xpValue = (q["xpValue"] as? Long)?.toInt() ?: 15,
                            penaltyXp = (q["penaltyXp"] as? Long)?.toInt() ?: 10
                        )
                    }

                    cloudDecks.add(
                        FlashcardDeck(
                            id = doc.id,
                            title = title,
                            description = desc,
                            createdAt = createdAt,
                            pagesCount = pagesCount,
                            cards = cards,
                            quiz = quiz,
                            isCloudSynced = true
                        )
                    )
                }

                if (cloudDecks.isNotEmpty()) {
                    // Merge cloud decks with local
                    val local = _decks.value.associateBy { it.id }
                    val merged = mutableListOf<FlashcardDeck>()
                    merged.addAll(cloudDecks)
                    for (loc in local.values) {
                        if (cloudDecks.none { it.id == loc.id }) {
                            merged.add(loc)
                            uploadDeckToFirebase(loc)
                        }
                    }
                    _decks.value = merged
                    saveLocalDecks(merged)
                } else {
                    // Cloud empty, push existing local decks
                    for (deck in _decks.value) {
                        uploadDeckToFirebase(deck)
                    }
                }

                _syncStatus.value = CloudSyncStatus.SYNCED
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync fallback to local: ${e.message}")
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
            }
        }
    }

    private fun uploadDeckToFirebase(deck: FlashcardDeck) {
        scope.launch {
            val fs = firestore ?: return@launch
            try {
                val cardsList = deck.cards.map { c ->
                    mapOf(
                        "id" to c.id,
                        "front" to c.front,
                        "back" to c.back,
                        "keyTerm" to c.keyTerm,
                        "tag" to c.tag,
                        "difficulty" to c.difficulty,
                        "isMastered" to c.isMastered,
                        "reviewCount" to c.reviewCount
                    )
                }

                val quizList = deck.quiz.map { q ->
                    mapOf(
                        "id" to q.id,
                        "question" to q.question,
                        "options" to q.options,
                        "correctIndex" to q.correctIndex,
                        "explanation" to q.explanation,
                        "xpValue" to q.xpValue,
                        "penaltyXp" to q.penaltyXp
                    )
                }

                val data = mapOf(
                    "title" to deck.title,
                    "description" to deck.description,
                    "createdAt" to deck.createdAt,
                    "pagesCount" to deck.pagesCount,
                    "cards" to cardsList,
                    "quiz" to quizList
                )

                fs.collection(FIRESTORE_COLLECTION_DECKS)
                    .document(deck.id)
                    .set(data, SetOptions.merge())
                    .await()

                // Link with Study Space application on the same Firebase
                val spaceId = _userProfile.value.studySpaceWorkspaceId.ifBlank { "studyspace_main" }
                try {
                    fs.collection("study_space")
                        .document(spaceId)
                        .collection("decks")
                        .document(deck.id)
                        .set(data, SetOptions.merge())

                    fs.collection("studyspace_decks")
                        .document(deck.id)
                        .set(data, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal Study Space mirror write: ${e.message}")
                }

                // Mirror to Realtime Database for web application connection
                try {
                    realtimeDb?.getReference("website_sync")
                        ?.child("decks")
                        ?.child(deck.id)
                        ?.setValue(data)
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal Realtime DB mirror write: ${e.message}")
                }

                // Mark as synced
                val updated = _decks.value.map { if (it.id == deck.id) it.copy(isCloudSynced = true) else it }
                _decks.value = updated
                _syncStatus.value = CloudSyncStatus.SYNCED
            } catch (e: Exception) {
                Log.w(TAG, "Failed uploading deck to Firebase: ${e.message}")
                _syncStatus.value = CloudSyncStatus.OFFLINE_SAVED
            }
        }
    }

    private fun uploadProfileToFirebase(profile: UserProfile) {
        scope.launch {
            val fs = firestore ?: return@launch
            try {
                val data = mapOf(
                    "name" to profile.name,
                    "studyField" to profile.studyField,
                    "studyGoal" to profile.studyGoal,
                    "avatarEmoji" to profile.avatarEmoji,
                    "studySpaceWorkspaceId" to profile.studySpaceWorkspaceId,
                    "studySpaceSyncEnabled" to profile.studySpaceSyncEnabled,
                    "xp" to profile.xp,
                    "streakDays" to profile.streakDays,
                    "totalQuizzesTaken" to profile.totalQuizzesTaken,
                    "totalCardsMastered" to profile.totalCardsMastered,
                    "correctAnswers" to profile.correctAnswers,
                    "wrongAnswers" to profile.wrongAnswers,
                    "netQuizPoints" to profile.netQuizPoints,
                    "updatedAt" to System.currentTimeMillis()
                )
                fs.collection(FIRESTORE_COLLECTION_USERS)
                    .document(DEFAULT_USER_ID)
                    .set(data, SetOptions.merge())
                    .await()

                // Also sync to Study Space workspace document
                val spaceId = profile.studySpaceWorkspaceId.ifBlank { "studyspace_main" }
                try {
                    fs.collection("study_space")
                        .document(spaceId)
                        .set(mapOf("profile" to data, "lastSync" to System.currentTimeMillis()), SetOptions.merge())
                    fs.collection("studyspace_profiles")
                        .document(spaceId)
                        .set(data, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal Study Space profile sync: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed uploading profile to Firebase: ${e.message}")
            }
        }
    }

    private fun saveLocalDecks(decksList: List<FlashcardDeck>) {
        try {
            val array = JSONArray()
            for (deck in decksList) {
                val obj = JSONObject().apply {
                    put("id", deck.id)
                    put("title", deck.title)
                    put("description", deck.description)
                    put("createdAt", deck.createdAt)
                    put("pagesCount", deck.pagesCount)
                    put("subject", deck.subject)
                    put("extractedSummaryText", deck.extractedSummaryText)
                    put("isCloudSynced", deck.isCloudSynced)

                    val cardsArr = JSONArray()
                    for (c in deck.cards) {
                        cardsArr.put(JSONObject().apply {
                            put("id", c.id)
                            put("front", c.front)
                            put("back", c.back)
                            put("frontHindi", c.frontHindi)
                            put("backHindi", c.backHindi)
                            put("keyTerm", c.keyTerm)
                            put("keyTermHindi", c.keyTermHindi)
                            put("tag", c.tag)
                            put("difficulty", c.difficulty)
                            put("isMastered", c.isMastered)
                            put("reviewCount", c.reviewCount)
                            put("subject", c.subject)
                            put("sourcePageNumber", c.sourcePageNumber)
                            put("createdAt", c.createdAt)
                        })
                    }
                    put("cards", cardsArr)

                    val quizArr = JSONArray()
                    for (q in deck.quiz) {
                        quizArr.put(JSONObject().apply {
                            put("id", q.id)
                            put("question", q.question)
                            put("questionHindi", q.questionHindi)
                            put("correctIndex", q.correctIndex)
                            put("explanation", q.explanation)
                            put("explanationHindi", q.explanationHindi)
                            put("xpValue", q.xpValue)
                            put("penaltyXp", q.penaltyXp)
                            val optArr = JSONArray()
                            for (o in q.options) optArr.put(o)
                            put("options", optArr)
                            val optHiArr = JSONArray()
                            for (oh in q.optionsHindi) optHiArr.put(oh)
                            put("optionsHindi", optHiArr)
                        })
                    }
                    put("quiz", quizArr)

                    val probArr = JSONArray()
                    for (p in deck.practiceProblems) {
                        probArr.put(JSONObject().apply {
                            put("id", p.id)
                            put("title", p.title)
                            put("subject", p.subject)
                            put("problemStatement", p.problemStatement)
                            put("problemStatementHindi", p.problemStatementHindi)
                            put("equationOrFormula", p.equationOrFormula)
                            put("diagramType", p.diagramType)
                            put("diagramContent", p.diagramContent)
                            put("whatToFind", p.whatToFind)
                            put("hint", p.hint)
                            put("finalAnswer", p.finalAnswer)
                            put("difficulty", p.difficulty)
                            put("isSolved", p.isSolved)
                            put("userDraftAnswer", p.userDraftAnswer)
                            val gArr = JSONArray()
                            for (g in p.givenData) gArr.put(g)
                            put("givenData", gArr)
                            val sArr = JSONArray()
                            for (s in p.stepByStepSolution) sArr.put(s)
                            put("stepByStepSolution", sArr)
                        })
                    }
                    put("practiceProblems", probArr)
                }
                array.put(obj)
            }
            val jsonString = array.toString()
            // 1. Primary fast cache in SharedPreferences
            prefs.edit().putString(KEY_DECKS, jsonString).apply()

            // 2. Persistent atomic internal file storage (survives app updates & prefs clearing)
            try {
                val tempFile = File(context.filesDir, "$FILE_DECKS_STORE.tmp")
                val storeFile = File(context.filesDir, FILE_DECKS_STORE)
                val backupFile = File(context.filesDir, FILE_DECKS_BACKUP)

                tempFile.writeText(jsonString, Charsets.UTF_8)
                if (storeFile.exists()) {
                    storeFile.copyTo(backupFile, overwrite = true)
                }
                if (tempFile.exists()) {
                    tempFile.renameTo(storeFile)
                }
            } catch (fe: Exception) {
                Log.w(TAG, "Notice saving decks to internal file: ${fe.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving local decks: ${e.message}")
        }
    }

    fun exportLocalBackupJson(): String {
        try {
            val storeFile = File(context.filesDir, FILE_DECKS_STORE)
            if (storeFile.exists()) {
                val content = storeFile.readText(Charsets.UTF_8)
                if (content.isNotBlank()) return content
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice reading disk file for export: ${e.message}")
        }
        return prefs.getString(KEY_DECKS, "[]") ?: "[]"
    }

    fun restoreFromLocalJson(json: String): Pair<Boolean, String> {
        return try {
            val parsed = parseDecksFromJson(json)
            if (parsed.isNotEmpty()) {
                _decks.value = parsed
                saveLocalDecks(parsed)
                for (d in parsed) uploadDeckToFirebase(d)
                Pair(true, "Successfully restored ${parsed.size} decks (${parsed.sumOf { it.cards.size }} flashcards)!")
            } else {
                Pair(false, "No valid decks found in backup data.")
            }
        } catch (e: Exception) {
            Pair(false, "Restore error: ${e.message}")
        }
    }

    private fun parseDecksFromJson(json: String): List<FlashcardDeck> {
        val result = mutableListOf<FlashcardDeck>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val cardsArr = obj.optJSONArray("cards")
            val cardsList = mutableListOf<Flashcard>()
            if (cardsArr != null) {
                for (cIdx in 0 until cardsArr.length()) {
                    val c = cardsArr.getJSONObject(cIdx)
                    cardsList.add(
                        Flashcard(
                            id = c.optString("id", java.util.UUID.randomUUID().toString()),
                            front = c.optString("front", ""),
                            back = c.optString("back", ""),
                            frontHindi = c.optString("frontHindi", ""),
                            backHindi = c.optString("backHindi", ""),
                            keyTerm = c.optString("keyTerm", ""),
                            keyTermHindi = c.optString("keyTermHindi", ""),
                            tag = c.optString("tag", "Core"),
                            difficulty = c.optString("difficulty", "Medium"),
                            isMastered = c.optBoolean("isMastered", false),
                            reviewCount = c.optInt("reviewCount", 0),
                            subject = c.optString("subject", "General"),
                            sourcePageNumber = c.optInt("sourcePageNumber", 1),
                            createdAt = c.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val quizArr = obj.optJSONArray("quiz")
            val quizList = mutableListOf<QuizQuestion>()
            if (quizArr != null) {
                for (qIdx in 0 until quizArr.length()) {
                    val q = quizArr.getJSONObject(qIdx)
                    val optsArr = q.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsArr != null) {
                        for (oIdx in 0 until optsArr.length()) {
                            opts.add(optsArr.getString(oIdx))
                        }
                    }
                    val optsHiArr = q.optJSONArray("optionsHindi")
                    val optsHi = mutableListOf<String>()
                    if (optsHiArr != null) {
                        for (oIdx in 0 until optsHiArr.length()) {
                            optsHi.add(optsHiArr.getString(oIdx))
                        }
                    }
                    quizList.add(
                        QuizQuestion(
                            id = q.optString("id", java.util.UUID.randomUUID().toString()),
                            question = q.optString("question", ""),
                            questionHindi = q.optString("questionHindi", ""),
                            options = opts,
                            optionsHindi = optsHi,
                            correctIndex = q.optInt("correctIndex", 0),
                            explanation = q.optString("explanation", ""),
                            explanationHindi = q.optString("explanationHindi", ""),
                            xpValue = q.optInt("xpValue", 15),
                            penaltyXp = q.optInt("penaltyXp", 10)
                        )
                    )
                }
            }

            val parsedSubject = obj.optString("subject", "").ifBlank {
                cardsList.firstOrNull()?.subject ?: "General"
            }

            val probArr = obj.optJSONArray("practiceProblems")
            val probList = mutableListOf<PracticeProblem>()
            if (probArr != null) {
                for (pIdx in 0 until probArr.length()) {
                    val p = probArr.getJSONObject(pIdx)
                    val gArr = p.optJSONArray("givenData")
                    val givenList = mutableListOf<String>()
                    if (gArr != null) {
                        for (gi in 0 until gArr.length()) givenList.add(gArr.getString(gi))
                    }
                    val sArr = p.optJSONArray("stepByStepSolution")
                    val stepList = mutableListOf<String>()
                    if (sArr != null) {
                        for (si in 0 until sArr.length()) stepList.add(sArr.getString(si))
                    }
                    probList.add(
                        PracticeProblem(
                            id = p.optString("id", java.util.UUID.randomUUID().toString()),
                            title = p.optString("title", "Practice Problem ${pIdx + 1}"),
                            subject = p.optString("subject", parsedSubject),
                            problemStatement = p.optString("problemStatement", ""),
                            problemStatementHindi = p.optString("problemStatementHindi", ""),
                            equationOrFormula = p.optString("equationOrFormula", ""),
                            diagramType = p.optString("diagramType", "DIAGRAM"),
                            diagramContent = p.optString("diagramContent", ""),
                            givenData = givenList,
                            whatToFind = p.optString("whatToFind", ""),
                            hint = p.optString("hint", ""),
                            stepByStepSolution = stepList,
                            finalAnswer = p.optString("finalAnswer", ""),
                            difficulty = p.optString("difficulty", "Medium"),
                            isSolved = p.optBoolean("isSolved", false),
                            userDraftAnswer = p.optString("userDraftAnswer", "")
                        )
                    )
                }
            }

            val deckId = obj.optString("id", java.util.UUID.randomUUID().toString())
            if (deckId.startsWith("starter_")) {
                continue
            }

            result.add(
                FlashcardDeck(
                    id = deckId,
                    title = obj.optString("title", "Notes Deck"),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    pagesCount = obj.optInt("pagesCount", 1),
                    subject = parsedSubject,
                    cards = cardsList,
                    quiz = quizList,
                    practiceProblems = probList,
                    isCloudSynced = obj.optBoolean("isCloudSynced", false),
                    extractedSummaryText = obj.optString("extractedSummaryText", "")
                )
            )
        }
        return result
    }

    private fun createStarterDecks(): List<FlashcardDeck> {
        return emptyList()
    }
}
