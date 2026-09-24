package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.notification.QuizReminderScheduler
import com.example.ui.components.AppBottomBar
import com.example.ui.components.CloudSyncBackupDialog
import com.example.ui.components.LevelUpCelebrationDialog
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.FullScreenCameraScreen
import com.example.ui.screens.PagesScreen
import com.example.ui.screens.QuizDashboardScreen
import com.example.ui.screens.QuizResultScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.QuizSelectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FlashSnapApp()
            }
        }
    }
}

@Composable
fun FlashSnapApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val capturedPages by viewModel.capturedPages.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisStepText by viewModel.analysisStepText.collectAsState()

    val activeDeck by viewModel.activeDeck.collectAsState()
    val currentCardIndex by viewModel.currentCardIndex.collectAsState()
    val isCardFlipped by viewModel.isCardFlipped.collectAsState()

    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsState()
    val isAnswerSubmitted by viewModel.isAnswerSubmitted.collectAsState()
    val quizCorrectCount by viewModel.quizCorrectCount.collectAsState()
    val quizWrongCount by viewModel.quizWrongCount.collectAsState()
    val quizCurrentStreak by viewModel.quizCurrentStreak.collectAsState()
    val quizNetPoints by viewModel.quizNetPoints.collectAsState()
    val lastPointChange by viewModel.lastPointChange.collectAsState()

    val celebratedLevel by viewModel.celebratedLevel.collectAsState()
    val syncAccountId by viewModel.syncAccountId.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    val isReminderScheduled by viewModel.isReminderScheduled.collectAsState()
    val reminderStatus by viewModel.reminderStatus.collectAsState()
    val reminderTimeLabel by viewModel.reminderTimeLabel.collectAsState()
    val activeSessionSubject by viewModel.activeSessionSubject.collectAsState()
    val quizHistory by viewModel.quizHistory.collectAsState()

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        if (activity?.intent?.getBooleanExtra(QuizReminderScheduler.EXTRA_START_QUIZ, false) == true) {
            viewModel.navigateTo(Screen.QuizSelect)
        }
    }

    var showCloudSyncDialog by remember { mutableStateOf(false) }

    // Navigation back press handler
    BackHandler(enabled = currentScreen !is Screen.Camera) {
        when (currentScreen) {
            is Screen.Pages -> viewModel.navigateTo(Screen.Camera)
            is Screen.Decks -> viewModel.navigateTo(Screen.Camera)
            is Screen.QuizSelect -> viewModel.navigateTo(Screen.Camera)
            is Screen.QuizDashboard -> viewModel.navigateTo(Screen.QuizSelect)
            is Screen.Settings -> viewModel.navigateTo(Screen.Camera)
            is Screen.Study -> viewModel.navigateTo(Screen.Decks)
            is Screen.Quiz -> {
                val deckId = (currentScreen as Screen.Quiz).deckId
                viewModel.navigateTo(Screen.Study(deckId))
            }
            is Screen.QuizResult -> viewModel.navigateTo(Screen.Decks)
            Screen.Camera -> { /* Handled by OS */ }
        }
    }

    // Determine if bottom navigation tabs should be shown
    val showBottomBar = currentScreen is Screen.Camera ||
                        currentScreen is Screen.Pages ||
                        currentScreen is Screen.Decks ||
                        currentScreen is Screen.QuizSelect ||
                        currentScreen is Screen.QuizDashboard ||
                        currentScreen is Screen.Settings

    var cameraShutterTrigger by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentScreen = currentScreen,
                    onTabSelected = { targetScreen -> viewModel.navigateTo(targetScreen) },
                    onCameraShutterClick = {
                        cameraShutterTrigger?.invoke()
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) +
                        slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                            initialOffsetX = { fullWidth -> fullWidth / 8 }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(160)) +
                            slideOutHorizontally(
                                animationSpec = tween(160),
                                targetOffsetX = { fullWidth -> -fullWidth / 8 }
                            ))
                },
                label = "screen_navigation_transition"
            ) { screen ->
                when (screen) {
                    // Default View: Full-Screen Live Camera Viewfinder
                    is Screen.Camera -> {
                        FullScreenCameraScreen(
                            capturedPages = capturedPages,
                            onPageCaptured = { bmp -> viewModel.addCapturedPage(bmp) },
                            onRemovePage = { idx -> viewModel.removeCapturedPage(idx) },
                            activeSessionSubject = activeSessionSubject,
                            onSelectSessionSubject = { subject -> viewModel.setActiveSessionSubject(subject) },
                            onGoToPages = { viewModel.navigateTo(Screen.Pages) },
                            onRegisterShutter = { shutterFn ->
                                cameraShutterTrigger = shutterFn
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scanned Pages & Deck Generation Screen
                    is Screen.Pages -> {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                            PagesScreen(
                                capturedPages = capturedPages,
                                isAnalyzing = isAnalyzing,
                                analysisStepText = analysisStepText,
                                onAddPageBitmap = { bmp -> viewModel.addCapturedPage(bmp) },
                                onRemovePage = { idx -> viewModel.removeCapturedPage(idx) },
                                onUpdatePageManualLabel = { pageId, label -> viewModel.updatePageManualLabel(pageId, label) },
                                onUpdatePageExtractedText = { pageId, text -> viewModel.updatePageExtractedText(pageId, text) },
                                onAnalyze = { hint, notes, separate -> viewModel.analyzeAndGenerateDeck(hint, notes, separate) },
                                onGoToCamera = { viewModel.navigateTo(Screen.Camera) }
                            )
                        }
                    }

                    // Dedicated Decks & Flashcards Screen
                    is Screen.Decks -> {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                            DecksScreen(
                                decks = decks,
                                profile = userProfile,
                                onDeckClick = { deckId -> viewModel.navigateTo(Screen.Study(deckId = deckId, initialMode = "CARDS")) },
                                onSolveProblems = { deckId -> viewModel.navigateTo(Screen.Study(deckId = deckId, initialMode = "SOLVE")) },
                                onStartQuiz = { deckId -> viewModel.navigateTo(Screen.Quiz(deckId)) },
                                onDeleteDeck = { deckId -> viewModel.deleteDeck(deckId) },
                                onGoToCamera = { viewModel.navigateTo(Screen.Camera) },
                                onMarkCardMastery = { deckId, cardId, isMastered ->
                                    viewModel.markSpecificCardMastery(deckId, cardId, isMastered)
                                }
                            )
                        }
                    }

                    // Quiz Mode Selection Screen (Tab 4)
                    is Screen.QuizSelect -> {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                            QuizSelectScreen(
                                decks = decks,
                                profile = userProfile,
                                quizHistory = quizHistory,
                                isReminderScheduled = isReminderScheduled,
                                reminderStatus = reminderStatus,
                                reminderTimeLabel = reminderTimeLabel,
                                onSetReminder = { enabled, hour, minute, label ->
                                    viewModel.setDailyQuizReminder(enabled, hour, minute, label)
                                },
                                onSendTestReminder = {
                                    viewModel.sendTestQuizReminder()
                                },
                                onStartQuiz = { deckId -> viewModel.navigateTo(Screen.Quiz(deckId)) },
                                onStudyDeck = { deckId -> viewModel.navigateTo(Screen.Study(deckId = deckId, initialMode = "CARDS")) },
                                onGoToPages = { viewModel.navigateTo(Screen.Pages) }
                            )
                        }
                    }

                    // Dedicated Quiz Performance Dashboard Screen
                    is Screen.QuizDashboard -> {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                            QuizDashboardScreen(
                                quizHistory = quizHistory,
                                decks = decks,
                                profile = userProfile,
                                onBack = { viewModel.navigateTo(Screen.QuizSelect) },
                                onStartQuiz = { deckId -> viewModel.navigateTo(Screen.Quiz(deckId)) },
                                onStudyDeck = { deckId -> viewModel.navigateTo(Screen.Study(deckId = deckId, initialMode = "CARDS")) },
                                onClearHistory = { viewModel.clearQuizHistory() }
                            )
                        }
                    }

                    // Dedicated Profile & Study Space Settings Screen
                    is Screen.Settings -> {
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                            SettingsScreen(
                                profile = userProfile,
                                syncStatus = syncStatus,
                                syncAccountId = syncAccountId,
                                onUpdateProfile = { name, field, goal, emoji, spaceId, syncEnabled, webUrl ->
                                    viewModel.updateProfileDetails(name, field, goal, emoji, spaceId, syncEnabled, webUrl)
                                },
                                onSyncWithStudySpace = { spaceId, onResult ->
                                    viewModel.syncWithStudySpace(spaceId, onResult)
                                },
                                onImportFromStudySpace = { spaceId, onResult ->
                                    viewModel.importFromStudySpace(spaceId, onResult)
                                },
                                onSyncWithWebsite = { webUrl, onResult ->
                                    viewModel.syncWithWebsite(webUrl, onResult)
                                },
                                onUpdateStudySettings = { flipSpeed, haptics, sounds, autoAdvance, algo, penalty, timer, font, target, highContrast ->
                                    viewModel.updateStudyEngineSettings(
                                        flipSpeed, haptics, sounds, autoAdvance, algo, penalty, timer, font, target, highContrast
                                    )
                                },
                                onResetMastery = {
                                    viewModel.resetMasteryProgress()
                                },
                                isReminderScheduled = isReminderScheduled,
                                reminderTimeLabel = reminderTimeLabel,
                                reminderStatus = reminderStatus,
                                onSetReminder = { enabled, hour, minute, label ->
                                    viewModel.setDailyQuizReminder(enabled, hour, minute, label)
                                },
                                onSendTestReminder = {
                                    viewModel.sendTestQuizReminder()
                                }
                            )
                        }
                    }

                    // Clean & Organized Flashcard Study Screen & Problem Solver
                    is Screen.Study -> {
                        LaunchedEffect(screen.deckId) {
                            activeDeck?.let { viewModel.ensureDeckHasProblems(it) }
                        }
                        StudyScreen(
                            deck = activeDeck,
                            currentIndex = currentCardIndex,
                            isFlipped = isCardFlipped,
                            onFlip = { viewModel.flipCard() },
                            onNext = { viewModel.nextCard() },
                            onPrevious = { viewModel.previousCard() },
                            onMarkMastery = { isMastered -> viewModel.markCardMastery(isMastered) },
                            onMarkCardMastery = { cardId, isMastered ->
                                activeDeck?.let { viewModel.markSpecificCardMastery(it.id, cardId, isMastered) }
                            },
                            onStartQuiz = {
                                activeDeck?.let { viewModel.navigateTo(Screen.Quiz(it.id)) }
                            },
                            onBack = { viewModel.navigateTo(Screen.Decks) },
                            studyLanguage = userProfile.studyLanguage,
                            onToggleLanguage = { viewModel.toggleStudyLanguage() },
                            initialMode = if (screen.initialMode == "SOLVE") com.example.ui.screens.StudyMode.QUESTIONS_TO_SOLVE else com.example.ui.screens.StudyMode.CARD_FLIP,
                            onProblemSolved = { problemId, isSolved ->
                                activeDeck?.let { viewModel.updateProblemSolved(it.id, problemId, isSolved) }
                            },
                            onProblemDraftChange = { problemId, draft ->
                                activeDeck?.let { viewModel.updateProblemDraft(it.id, problemId, draft) }
                            },
                            onGenerateMoreProblems = {
                                activeDeck?.let { viewModel.ensureDeckHasProblems(it) }
                            }
                        )
                    }

                    // Quiz Mode
                    is Screen.Quiz -> {
                        QuizScreen(
                            deck = activeDeck,
                            currentIndex = currentQuestionIndex,
                            selectedOption = selectedOptionIndex,
                            isAnswerSubmitted = isAnswerSubmitted,
                            currentStreak = quizCurrentStreak,
                            netPoints = quizNetPoints,
                            lastPointChange = lastPointChange,
                            onSelectOption = { optIdx -> viewModel.selectQuizOption(optIdx) },
                            onSubmitAnswer = { viewModel.submitCurrentQuizAnswer() },
                            onNextQuestion = { viewModel.advanceToNextQuizQuestion() },
                            onRecordResult = { isCorrect, isFinal -> viewModel.recordQuizAnswerResult(isCorrect, isFinal) },
                            onExitQuiz = {
                                activeDeck?.let { viewModel.navigateTo(Screen.Study(it.id)) }
                                    ?: viewModel.navigateTo(Screen.Decks)
                            },
                            studyLanguage = userProfile.studyLanguage,
                            onToggleLanguage = { viewModel.toggleStudyLanguage() },
                            profile = userProfile,
                            onRegenerateDynamicQuiz = {
                                activeDeck?.let { viewModel.regenerateQuizFromFlashcards(it.id) }
                            }
                        )
                    }

                    // Quiz Result Mode
                    is Screen.QuizResult -> {
                        QuizResultScreen(
                            deck = activeDeck,
                            profile = userProfile,
                            correctCount = quizCorrectCount,
                            wrongCount = quizWrongCount,
                            netPoints = quizNetPoints,
                            onRetakeQuiz = {
                                activeDeck?.let { viewModel.navigateTo(Screen.Quiz(it.id)) }
                            },
                            onStudyDeck = {
                                activeDeck?.let { viewModel.navigateTo(Screen.Study(it.id)) }
                            },
                            onHome = { viewModel.navigateTo(Screen.Decks) },
                            onViewDashboard = { viewModel.navigateTo(Screen.QuizDashboard) }
                        )
                    }
                }
            }

            // Cloud Sync & Multi-Device Backup Dialog
            if (showCloudSyncDialog) {
                CloudSyncBackupDialog(
                    currentAccountId = syncAccountId,
                    lastBackupTime = lastBackupTime,
                    syncStatus = syncStatus,
                    syncMessage = syncMessage,
                    deckCount = decks.size,
                    totalCardsCount = decks.sumOf { it.cards.size },
                    onBackupNow = { viewModel.backupAllToCloud() },
                    onRestoreCloud = { targetId -> viewModel.restoreFromCloud(targetId) },
                    onUpdateAccountId = { newId -> viewModel.setSyncAccountId(newId) },
                    onDismiss = {
                        viewModel.clearSyncMessage()
                        showCloudSyncDialog = false
                    }
                )
            }

            // Level Up Celebration Modal
            celebratedLevel?.let { levelInfo ->
                LevelUpCelebrationDialog(
                    levelInfo = levelInfo,
                    onDismiss = { viewModel.dismissLevelUpCelebration() }
                )
            }
        }
    }
}
