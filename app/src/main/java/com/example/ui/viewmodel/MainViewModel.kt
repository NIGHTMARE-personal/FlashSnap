package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiFlashcardService
import com.example.data.model.CapturedPage
import com.example.data.model.Flashcard
import com.example.data.model.FlashcardDeck
import com.example.data.model.GameLevelInfo
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizRecord
import com.example.data.model.SubjectPerformanceSummary
import com.example.data.model.UserProfile
import com.example.data.notification.QuizReminderScheduler
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.FlashcardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    data object Camera : Screen()
    data object Pages : Screen()
    data object Decks : Screen()
    data class Study(val deckId: String, val initialMode: String = "CARDS") : Screen()
    data class Quiz(val deckId: String) : Screen()
    data class QuizResult(val deckId: String) : Screen()
    data object QuizSelect : Screen()
    data object QuizDashboard : Screen()
    data object Settings : Screen()

    companion object {
        // Compatibility alias for legacy references
        val Scan: Screen = Camera
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlashcardRepository(application.applicationContext)
    private val geminiService = GeminiFlashcardService()
    val quizReminderScheduler: QuizReminderScheduler = QuizReminderScheduler.getInstance(application.applicationContext)

    val decks: StateFlow<List<FlashcardDeck>> = repository.decks
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val quizHistory: StateFlow<List<QuizRecord>> = repository.quizHistory
    val syncStatus: StateFlow<CloudSyncStatus> = repository.syncStatus
    val syncAccountId: StateFlow<String> = repository.syncAccountId
    val lastBackupTime: StateFlow<Long?> = repository.lastBackupTime
    val syncMessage: StateFlow<String?> = repository.syncMessage

    val isReminderScheduled: StateFlow<Boolean> = quizReminderScheduler.isScheduled
    val reminderStatus: StateFlow<String?> = quizReminderScheduler.lastNotificationStatus
    val reminderTimeLabel: StateFlow<String> = quizReminderScheduler.scheduledTimeLabel

    init {
        // Auto-schedule daily reminder if user enabled it
        val profile = userProfile.value
        if (profile.reminderEnabled) {
            quizReminderScheduler.scheduleDailyReminder(
                hour = profile.reminderHour,
                minute = profile.reminderMinute,
                timeLabel = profile.typicalStudyTimeLabel
            )
        }
    }

    // Default screen is full-screen Camera as requested!
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Camera)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Page Capture & Scan State
    private val _capturedPages = MutableStateFlow<List<CapturedPage>>(emptyList())
    val capturedPages: StateFlow<List<CapturedPage>> = _capturedPages.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStepText = MutableStateFlow("")
    val analysisStepText: StateFlow<String> = _analysisStepText.asStateFlow()

    // Active Study Deck State
    private val _activeDeck = MutableStateFlow<FlashcardDeck?>(null)
    val activeDeck: StateFlow<FlashcardDeck?> = _activeDeck.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    // Active Quiz State (Negative Marking Game System)
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    private val _quizCorrectCount = MutableStateFlow(0)
    val quizCorrectCount: StateFlow<Int> = _quizCorrectCount.asStateFlow()

    private val _quizWrongCount = MutableStateFlow(0)
    val quizWrongCount: StateFlow<Int> = _quizWrongCount.asStateFlow()

    private val _quizCurrentStreak = MutableStateFlow(0)
    val quizCurrentStreak: StateFlow<Int> = _quizCurrentStreak.asStateFlow()

    private val _quizNetPoints = MutableStateFlow(0)
    val quizNetPoints: StateFlow<Int> = _quizNetPoints.asStateFlow()

    private val _lastPointChange = MutableStateFlow<Int?>(null) // +15 or -10
    val lastPointChange: StateFlow<Int?> = _lastPointChange.asStateFlow()

    // Level up dialog trigger
    private val _celebratedLevel = MutableStateFlow<GameLevelInfo?>(null)
    val celebratedLevel: StateFlow<GameLevelInfo?> = _celebratedLevel.asStateFlow()

    // Dual language support: "EN" (English) or "HI" (Hindi / हिन्दी)
    private val _studyLanguage = MutableStateFlow(userProfile.value.studyLanguage.ifBlank { "EN" })
    val studyLanguage: StateFlow<String> = _studyLanguage.asStateFlow()

    fun setStudyLanguage(lang: String) {
        val clean = if (lang.equals("HI", ignoreCase = true) || lang.equals("Hindi", ignoreCase = true)) "HI" else "EN"
        _studyLanguage.value = clean
        repository.updateProfile(userProfile.value.copy(studyLanguage = clean))
    }

    fun toggleStudyLanguage() {
        setStudyLanguage(if (_studyLanguage.value == "EN") "HI" else "EN")
    }

    fun setDailyQuizReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0, timeLabel: String = "8:00 PM") {
        val current = userProfile.value
        val updated = current.copy(
            reminderEnabled = enabled,
            reminderHour = hour,
            reminderMinute = minute,
            typicalStudyTimeLabel = timeLabel
        )
        repository.updateProfile(updated)
        if (enabled) {
            quizReminderScheduler.scheduleDailyReminder(hour, minute, timeLabel)
        } else {
            quizReminderScheduler.cancelReminder()
        }
    }

    fun sendTestQuizReminder(): Boolean {
        return quizReminderScheduler.sendTestReminderNotification(userProfile.value.typicalStudyTimeLabel)
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Study) {
            val deck = decks.value.find { it.id == screen.deckId }
            _activeDeck.value = deck
            _currentCardIndex.value = 0
            _isCardFlipped.value = false
        } else if (screen is Screen.Quiz) {
            val rawDeck = decks.value.find { it.id == screen.deckId }
            val readyDeck = rawDeck?.let { ensureQuizQuestions(it) }
            _activeDeck.value = readyDeck
            startQuiz()
        }
    }

    private fun ensureQuizQuestions(deck: FlashcardDeck): FlashcardDeck {
        if (deck.quiz.isNotEmpty()) return deck
        val cards = deck.cards
        if (cards.isEmpty()) return deck

        val generatedQuestions = cards.mapIndexed { idx, card ->
            val otherCards = cards.filter { it.id != card.id }.shuffled()
            val distractors = otherCards.take(2).map { it.back }.toMutableList()
            while (distractors.size < 2) {
                distractors.add("Alternative definition concept #${distractors.size + 1}")
            }
            val options = (distractors + card.back).shuffled()
            val correctIdx = options.indexOf(card.back).coerceAtLeast(0)

            val optionsHi = options.map { com.example.data.util.LanguageHelper.autoTranslateToHindi(it) }
            val cardFrontHi = card.frontHindi.ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(card.front) }
            val cardBackHi = card.backHindi.ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(card.back) }

            QuizQuestion(
                id = "quiz_${card.id}_$idx",
                question = "What is the key principle or definition of: \"${card.front}\"?",
                questionHindi = "मुख्य सिद्धांत या परिभाषा क्या है: \"$cardFrontHi\"?",
                options = options,
                optionsHindi = optionsHi,
                correctIndex = correctIdx,
                explanation = "Key definition: ${card.back}",
                explanationHindi = "मुख्य परिभाषा: $cardBackHi",
                xpValue = 15,
                penaltyXp = 10
            )
        }

        val updated = deck.copy(quiz = generatedQuestions)
        repository.saveDeck(updated)
        return updated
    }

    fun updateProblemSolved(deckId: String, problemId: String, isSolved: Boolean) {
        repository.updateProblemSolved(deckId, problemId, isSolved)
    }

    fun updateProblemDraft(deckId: String, problemId: String, draft: String) {
        repository.updateProblemDraft(deckId, problemId, draft)
    }

    fun ensureDeckHasProblems(deck: FlashcardDeck) {
        if (deck.practiceProblems.isEmpty()) {
            val fallback = geminiService.generateFallbackDeck(
                notesText = deck.extractedSummaryText.ifBlank { deck.cards.joinToString("\n") { "${it.front}: ${it.back}" } },
                pageCount = deck.pagesCount,
                titleHint = deck.title,
                targetSubject = deck.subject
            )
            if (fallback.practiceProblems.isNotEmpty()) {
                val updated = deck.copy(practiceProblems = fallback.practiceProblems)
                repository.saveDeck(updated)
            }
        }
    }

    // --- Active Session Subject Label ---
    private val _activeSessionSubject = MutableStateFlow<String>("")
    val activeSessionSubject: StateFlow<String> = _activeSessionSubject.asStateFlow()

    fun setActiveSessionSubject(subject: String) {
        _activeSessionSubject.value = subject.trim()
    }

    // --- Page Capture & Scan Operations ---
    fun addCapturedPage(
        bitmap: Bitmap,
        noteSnippet: String = "",
        initialSubject: String = ""
    ) {
        val current = _capturedPages.value.toMutableList()
        if (current.size < 20) {
            val pageNum = current.size + 1
            val sessionSubject = initialSubject.ifBlank { _activeSessionSubject.value }
            val detected = if (sessionSubject.isNotBlank()) {
                sessionSubject
            } else {
                com.example.data.model.SubjectClassifier.detectSubject(noteSnippet)
            }
            val newPage = CapturedPage(
                pageNumber = pageNum,
                bitmap = bitmap,
                noteSnippet = noteSnippet,
                extractedText = noteSnippet,
                autoDetectedSubject = detected,
                manualLabel = if (sessionSubject.isNotBlank()) sessionSubject else ""
            )
            current.add(newPage)
            _capturedPages.value = current

            // Perform background text extraction if noteSnippet is blank
            if (noteSnippet.isBlank()) {
                viewModelScope.launch {
                    try {
                        val ocrText = geminiService.extractTextFromBitmap(bitmap)
                        val refinedSubject = com.example.data.model.SubjectClassifier.detectSubject(ocrText)
                        _capturedPages.value = _capturedPages.value.map { p ->
                            if (p.id == newPage.id) {
                                p.copy(
                                    extractedText = ocrText,
                                    autoDetectedSubject = if (p.manualLabel.isNotBlank()) p.manualLabel else refinedSubject
                                )
                            } else p
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "OCR extraction non-fatal: ${e.message}")
                    }
                }
            }
        }
    }

    fun updatePageManualLabel(pageId: String, newLabel: String) {
        _capturedPages.value = _capturedPages.value.map { page ->
            if (page.id == pageId) {
                page.copy(manualLabel = newLabel.trim())
            } else page
        }
    }

    fun updatePageExtractedText(pageId: String, newText: String) {
        _capturedPages.value = _capturedPages.value.map { page ->
            if (page.id == pageId) {
                val autoSubject = if (page.manualLabel.isBlank()) {
                    com.example.data.model.SubjectClassifier.detectSubject(newText)
                } else page.autoDetectedSubject
                page.copy(extractedText = newText, autoDetectedSubject = autoSubject)
            } else page
        }
    }

    fun removeCapturedPage(index: Int) {
        val current = _capturedPages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            // Re-index page numbers
            _capturedPages.value = current.mapIndexed { idx, page ->
                page.copy(pageNumber = idx + 1)
            }
        }
    }

    fun clearCapturedPages() {
        _capturedPages.value = emptyList()
    }

    fun analyzeAndGenerateDeck(
        titleHint: String = "",
        additionalNotes: String = "",
        separateBySubject: Boolean = false
    ) {
        viewModelScope.launch {
            val pages = _capturedPages.value
            if (pages.isEmpty()) return@launch
            _isAnalyzing.value = true

            val steps = listOf(
                "Scanning page image(s) for handwritten ink & text...",
                "Deciphering cursive handwriting & formulas...",
                "Detecting subjects (Physics, Math, Chemistry, Biology)...",
                "Synthesizing 3D interactive flashcards with subject tags...",
                "Generating game quiz with negative marking rules..."
            )

            launch {
                for (step in steps) {
                    _analysisStepText.value = step
                    delay(550)
                }
            }

            val currentProf = userProfile.value
            val previousLevel = currentProf.levelInfo.level

            if (separateBySubject) {
                // Group pages by effectiveSubject (manualLabel > autoDetectedSubject)
                val grouped = pages.groupBy { it.effectiveSubject }
                val createdDecks = mutableListOf<FlashcardDeck>()

                for ((subj, subjPages) in grouped) {
                    val bitmaps = subjPages.mapNotNull { it.bitmap }
                    val aggregatedNotes = (subjPages.map { it.extractedText.ifBlank { it.noteSnippet } } + additionalNotes)
                        .filter { it.isNotBlank() }
                        .joinToString("\n\n")

                    val generatedDeck = geminiService.analyzeNotesAndGenerateDeck(
                        pages = bitmaps,
                        notesText = aggregatedNotes,
                        deckTitleHint = if (titleHint.isNotBlank() && grouped.size == 1) titleHint else "$subj: Scanned Notes (${subjPages.size} pages)"
                    ).copy(
                        subject = subj,
                        pagesCount = subjPages.size,
                        cards = geminiService.analyzeNotesAndGenerateDeck(
                            pages = bitmaps,
                            notesText = aggregatedNotes,
                            deckTitleHint = "$subj Notes"
                        ).cards.mapIndexed { idx, c ->
                            c.copy(subject = subj, sourcePageNumber = subjPages.getOrNull(idx % subjPages.size)?.pageNumber ?: 1)
                        }
                    )

                    repository.saveDeck(generatedDeck)
                    createdDecks.add(generatedDeck)
                }

                // Award XP for scanning all pages
                val updatedProf = currentProf.copy(
                    xp = currentProf.xp + (pages.size * 25),
                    streakDays = maxOf(1, currentProf.streakDays)
                )
                repository.updateProfile(updatedProf)

                if (updatedProf.levelInfo.level > previousLevel) {
                    _celebratedLevel.value = updatedProf.levelInfo
                }

                _isAnalyzing.value = false
                clearCapturedPages()

                // If multiple decks were created, navigate to Decks screen; otherwise open single deck
                if (createdDecks.size > 1) {
                    navigateTo(Screen.Decks)
                } else if (createdDecks.isNotEmpty()) {
                    navigateTo(Screen.Study(createdDecks.first().id))
                }
            } else {
                // Generate single combined deck
                val bitmaps = pages.mapNotNull { it.bitmap }
                val aggregatedNotes = (pages.map { "Page ${it.pageNumber} (${it.effectiveSubject}): " + it.extractedText.ifBlank { it.noteSnippet } } + additionalNotes)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")

                val primarySubject = pages.groupingBy { it.effectiveSubject }.eachCount().maxByOrNull { it.value }?.key ?: "General"

                val newDeck = geminiService.analyzeNotesAndGenerateDeck(
                    pages = bitmaps,
                    notesText = aggregatedNotes,
                    deckTitleHint = titleHint
                ).copy(
                    subject = primarySubject,
                    pagesCount = pages.size,
                    cards = geminiService.analyzeNotesAndGenerateDeck(
                        pages = bitmaps,
                        notesText = aggregatedNotes,
                        deckTitleHint = titleHint
                    ).cards.mapIndexed { idx, c ->
                        val matchedPage = pages.getOrNull(idx % pages.size)
                        c.copy(
                            subject = matchedPage?.effectiveSubject ?: primarySubject,
                            sourcePageNumber = matchedPage?.pageNumber ?: 1
                        )
                    }
                )

                // Save to repo & Firebase
                repository.saveDeck(newDeck)

                // Award XP for scanning pages
                val updatedProf = currentProf.copy(
                    xp = currentProf.xp + (pages.size * 25),
                    streakDays = maxOf(1, currentProf.streakDays)
                )
                repository.updateProfile(updatedProf)

                if (updatedProf.levelInfo.level > previousLevel) {
                    _celebratedLevel.value = updatedProf.levelInfo
                }

                _isAnalyzing.value = false
                clearCapturedPages()

                // Open the new deck in study view!
                navigateTo(Screen.Study(newDeck.id))
            }
        }
    }

    // --- Study Mode Operations ---
    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun nextCard() {
        val deck = _activeDeck.value ?: return
        if (_currentCardIndex.value < deck.cards.size - 1) {
            _currentCardIndex.value++
            _isCardFlipped.value = false
        }
    }

    fun previousCard() {
        if (_currentCardIndex.value > 0) {
            _currentCardIndex.value--
            _isCardFlipped.value = false
        }
    }

    fun markCardMastery(isMastered: Boolean) {
        val deck = _activeDeck.value ?: return
        val card = deck.cards.getOrNull(_currentCardIndex.value) ?: return

        val prevLevel = userProfile.value.levelInfo.level
        repository.updateDeckCardMastery(deck.id, card.id, isMastered)

        if (userProfile.value.levelInfo.level > prevLevel) {
            _celebratedLevel.value = userProfile.value.levelInfo
        }

        // Auto advance
        if (_currentCardIndex.value < deck.cards.size - 1) {
            _currentCardIndex.value++
            _isCardFlipped.value = false
        }
    }

    // --- Quiz Mode with Negative Marking Operations ---
    fun startQuiz() {
        _currentQuestionIndex.value = 0
        _selectedOptionIndex.value = null
        _isAnswerSubmitted.value = false
        _quizCorrectCount.value = 0
        _quizWrongCount.value = 0
        _quizCurrentStreak.value = 0
        _quizNetPoints.value = 0
        _lastPointChange.value = null
    }

    fun selectQuizOption(optionIndex: Int) {
        if (!_isAnswerSubmitted.value) {
            _selectedOptionIndex.value = optionIndex
        }
    }

    fun submitCurrentQuizAnswer() {
        val deck = _activeDeck.value ?: return
        val question = deck.quiz.getOrNull(_currentQuestionIndex.value) ?: return
        val selected = _selectedOptionIndex.value ?: return
        if (_isAnswerSubmitted.value) return

        _isAnswerSubmitted.value = true
        val isCorrect = (selected == question.correctIndex)

        recordQuizAnswerResult(isCorrect = isCorrect, isFinalAttempt = true)
    }

    fun recordQuizAnswerResult(isCorrect: Boolean, isFinalAttempt: Boolean = true) {
        val deck = _activeDeck.value ?: return
        val question = deck.quiz.getOrNull(_currentQuestionIndex.value) ?: return

        if (isCorrect) {
            val streakBonus = if (_quizCurrentStreak.value >= 2) 5 else 0
            val gain = question.xpValue + streakBonus
            _quizCorrectCount.value++
            _quizCurrentStreak.value++
            _quizNetPoints.value += gain
            _lastPointChange.value = gain
        } else if (isFinalAttempt) {
            // Negative Marking Penalty on final failure
            val penalty = question.penaltyXp
            _quizWrongCount.value++
            _quizCurrentStreak.value = 0
            _quizNetPoints.value -= penalty
            _lastPointChange.value = -penalty
        }
    }

    fun advanceToNextQuizQuestion() {
        val deck = _activeDeck.value ?: return
        _lastPointChange.value = null

        if (_currentQuestionIndex.value < deck.quiz.size - 1) {
            _currentQuestionIndex.value++
            _selectedOptionIndex.value = null
            _isAnswerSubmitted.value = false
        } else {
            // Finished Quiz! Record to Profile & History
            val prevLevel = userProfile.value.levelInfo.level
            repository.recordQuizResult(
                deckId = deck.id,
                deckTitle = deck.title,
                subject = deck.subject,
                correctCount = _quizCorrectCount.value,
                wrongCount = _quizWrongCount.value,
                netPoints = _quizNetPoints.value
            )

            if (userProfile.value.levelInfo.level > prevLevel) {
                _celebratedLevel.value = userProfile.value.levelInfo
            }

            navigateTo(Screen.QuizResult(deck.id))
        }
    }

    fun getSubjectPerformanceSummaries(): List<SubjectPerformanceSummary> {
        return repository.getSubjectPerformanceSummaries()
    }

    fun clearQuizHistory() {
        repository.clearQuizHistory()
    }

    fun deleteDeck(deckId: String) {
        repository.deleteDeck(deckId)
        if (_activeDeck.value?.id == deckId) {
            navigateTo(Screen.Decks)
        }
    }

    fun syncCloudNow() {
        repository.syncWithCloud()
    }

    fun setSyncAccountId(accountId: String) {
        repository.setSyncAccountId(accountId)
    }

    fun backupAllToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        repository.backupAllToCloud(onComplete)
    }

    fun restoreFromCloud(targetAccountId: String? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        repository.restoreFromCloud(targetAccountId, onComplete)
    }

    fun clearSyncMessage() {
        repository.clearSyncMessage()
    }

    fun dismissLevelUpCelebration() {
        _celebratedLevel.value = null
    }

    fun updateProfileDetails(
        name: String,
        studyField: String,
        studyGoal: String,
        avatarEmoji: String,
        studySpaceWorkspaceId: String,
        studySpaceSyncEnabled: Boolean,
        websiteUrl: String = ""
    ) {
        val current = userProfile.value
        val updated = current.copy(
            name = name,
            studyField = studyField,
            studyGoal = studyGoal,
            avatarEmoji = avatarEmoji,
            studySpaceWorkspaceId = studySpaceWorkspaceId,
            studySpaceSyncEnabled = studySpaceSyncEnabled,
            websiteUrl = websiteUrl
        )
        repository.updateProfile(updated)
    }

    fun updateStudyEngineSettings(
        flipSpeedMs: Int,
        hapticsEnabled: Boolean,
        soundEffectsEnabled: Boolean,
        autoAdvance: Boolean,
        spacedRepetitionAlgo: String,
        negativeMarkingSeverity: Int,
        questionTimerSeconds: Int,
        formulaFontStyle: String,
        dailyTargetCards: Int,
        highContrastCards: Boolean
    ) {
        val current = userProfile.value
        val updated = current.copy(
            cardFlipSpeedMs = flipSpeedMs,
            hapticFeedbackEnabled = hapticsEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            autoAdvanceMastered = autoAdvance,
            spacedRepetitionAlgo = spacedRepetitionAlgo,
            negativeMarkingSeverity = negativeMarkingSeverity,
            questionTimerSeconds = questionTimerSeconds,
            formulaFontStyle = formulaFontStyle,
            dailyTargetCards = dailyTargetCards,
            highContrastCards = highContrastCards
        )
        repository.updateProfile(updated)
    }

    fun resetMasteryProgress() {
        val currentDecks = decks.value.map { deck ->
            deck.copy(cards = deck.cards.map { it.copy(isMastered = false, reviewCount = 0) })
        }
        currentDecks.forEach { repository.saveDeck(it) }
        repository.updateProfile(userProfile.value.copy(totalCardsMastered = 0))
    }

    fun syncWithStudySpace(workspaceId: String? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val id = workspaceId ?: userProfile.value.studySpaceWorkspaceId
        repository.setSyncAccountId(id)
        repository.backupAllToCloud(onComplete)
    }

    fun importFromStudySpace(workspaceId: String? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val id = workspaceId ?: userProfile.value.studySpaceWorkspaceId
        repository.restoreFromCloud(id, onComplete)
    }

    fun syncWithWebsite(customUrl: String? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        repository.syncWithWebsite(customUrl, onComplete)
    }

    fun exportLocalBackupJson(): String {
        return repository.exportLocalBackupJson()
    }

    fun restoreFromLocalJson(json: String): Pair<Boolean, String> {
        return repository.restoreFromLocalJson(json)
    }
}
