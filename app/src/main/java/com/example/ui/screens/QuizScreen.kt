package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardDeck
import com.example.data.model.QuizQuestion
import com.example.data.util.LanguageHelper
import com.example.ui.components.FloatingXpBadge
import com.example.ui.components.PulsingStreakFlame
import com.example.ui.theme.PenaltyRed
import com.example.ui.theme.SuccessEmerald
import com.example.util.SoundFeedbackManager
import kotlin.math.roundToInt

enum class QuizQuestionState {
    IDLE,              // Waiting for user to select an option and click CHECK
    WRONG_RETRY,       // Wrong attempt (1 or 2): shows red sheet, "TRY AGAIN" button returns to question
    FAILED_CANCELLED,  // 3rd wrong attempt: out of lives! Shows explanation, "GOT IT" button cancels/skips
    CORRECT            // Answered correctly: shows green celebration sheet, "CONTINUE" button advances
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    deck: FlashcardDeck?,
    currentIndex: Int,
    selectedOption: Int?,
    isAnswerSubmitted: Boolean,
    currentStreak: Int,
    netPoints: Int,
    lastPointChange: Int?,
    onSelectOption: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onExitQuiz: () -> Unit,
    onRecordResult: (isCorrect: Boolean, isFinalAttempt: Boolean) -> Unit = { _, _ -> },
    studyLanguage: String = "EN",
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (deck == null || deck.quiz.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quiz available for this deck.")
        }
        return
    }

    val currentRawQuestion = deck.quiz.getOrNull(currentIndex) ?: deck.quiz.first()

    val currentOptions = remember(currentRawQuestion, studyLanguage) {
        LanguageHelper.getOptions(currentRawQuestion, studyLanguage)
    }

    // Ensure around 3 options as requested: 1 correct + 2 distractors
    val (threeOptions, correctIndexInThree) = remember(currentRawQuestion, currentOptions) {
        if (currentOptions.size <= 3) {
            currentOptions to currentRawQuestion.correctIndex.coerceIn(0, currentOptions.lastIndex)
        } else {
            val correctText = currentOptions.getOrElse(currentRawQuestion.correctIndex) { currentOptions.first() }
            val otherOptions = currentOptions.filterIndexed { i, _ -> i != currentRawQuestion.correctIndex }.shuffled()
            val chosenDistractors = otherOptions.take(2)
            val combined = (chosenDistractors + correctText).shuffled()
            combined to combined.indexOf(correctText).coerceAtLeast(0)
        }
    }

    // Per-question state for 3 attempts (Duolingo / Flipkart mechanics)
    var attemptsUsed by remember(currentIndex) { mutableIntStateOf(0) }
    val context = LocalContext.current
    var disabledOptions by remember(currentIndex) { mutableStateOf(setOf<Int>()) }
    var localSelectedOption by remember(currentIndex) { mutableStateOf<Int?>(null) }
    var questionState by remember(currentIndex) { mutableStateOf(QuizQuestionState.IDLE) }
    var lastWrongOptionIndex by remember(currentIndex) { mutableIntStateOf(-1) }

    val heartsRemaining = (3 - attemptsUsed).coerceAtLeast(0)
    val progress = (currentIndex + 1).toFloat() / deck.quiz.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp) // Leave space for Duolingo fixed bottom sheet
        ) {
            // 1. Duolingo Style Header: Exit (X) + Smooth Progress Bar + Language Toggle + 3 Hearts (❤️❤️❤️)
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Smooth Duolingo Capsule Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = SuccessEmerald,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Language Toggle Pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onToggleLanguage() }
                                .testTag("quiz_language_toggle"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (studyLanguage == "HI") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (studyLanguage == "HI") "🇮🇳 HI" else "🇬🇧 EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (studyLanguage == "HI") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 3 Hearts (Lives / Attempts)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF4B4B).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("quiz_hearts_container")
                        ) {
                            repeat(3) { heartIdx ->
                                val isAlive = heartIdx < heartsRemaining
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = if (isAlive) Color(0xFFFF4B4B) else Color.Gray.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(horizontal = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$heartsRemaining",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (heartsRemaining > 0) Color(0xFFFF4B4B) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExitQuiz,
                        modifier = Modifier.testTag("quiz_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit Quiz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secondary bar: XP Score + Streak Flame
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Net points pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (netPoints >= 0) SuccessEmerald.copy(alpha = 0.14f)
                                else PenaltyRed.copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (netPoints >= 0) "+$netPoints XP" else "$netPoints XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (netPoints >= 0) SuccessEmerald else PenaltyRed
                        )
                    }

                    // Pulsing Streak Flame
                    if (currentStreak >= 2) {
                        PulsingStreakFlame(streak = currentStreak)
                    }
                }

                // Question Transition
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "duolingo_card_transition"
                ) { _ ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 2. Duolingo Question Card with 3D Bottom Border
                        DuolingoQuestionCard(
                            questionNumber = currentIndex + 1,
                            totalQuestions = deck.quiz.size,
                            questionText = LanguageHelper.getQuestion(currentRawQuestion, studyLanguage),
                            secondaryQuestionText = if (studyLanguage == "HI" && currentRawQuestion.question != LanguageHelper.getQuestion(currentRawQuestion, studyLanguage)) currentRawQuestion.question else null,
                            xpValue = currentRawQuestion.xpValue,
                            subject = deck.subject,
                            studyLanguage = studyLanguage
                        )

                        // 3. Three Interactive Options (Duolingo 3D Button Style + Flipkart Shake)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            threeOptions.forEachIndexed { optIndex, optionText ->
                                val isSelected = localSelectedOption == optIndex
                                val isCorrect = optIndex == correctIndexInThree
                                val isDisabled = disabledOptions.contains(optIndex)
                                val isShaking = optIndex == lastWrongOptionIndex && questionState != QuizQuestionState.CORRECT

                                DuolingoOptionCard(
                                    optionIndex = optIndex,
                                    optionText = optionText,
                                    isSelected = isSelected,
                                    isDisabled = isDisabled,
                                    isCorrect = isCorrect,
                                    isShaking = isShaking,
                                    questionState = questionState,
                                    onSelect = {
                                        if (questionState == QuizQuestionState.IDLE && !isDisabled) {
                                            SoundFeedbackManager.getInstance(context).playCardFlipSound()
                                            localSelectedOption = optIndex
                                            onSelectOption(optIndex)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Fixed Duolingo Bottom Action Sheet (State-Driven: Check / Try Again / Continue)
        DuolingoActionSheet(
            questionState = questionState,
            selectedOption = localSelectedOption,
            attemptsUsed = attemptsUsed,
            currentQuestion = currentRawQuestion,
            correctAnswerText = threeOptions.getOrElse(correctIndexInThree) { "" },
            studyLanguage = studyLanguage,
            onCheck = {
                if (localSelectedOption != null) {
                    val isCorrect = (localSelectedOption == correctIndexInThree)
                    if (isCorrect) {
                        SoundFeedbackManager.getInstance(context).playCorrectSound()
                        questionState = QuizQuestionState.CORRECT
                        onRecordResult(true, true)
                    } else {
                        SoundFeedbackManager.getInstance(context).playWrongSound()
                        attemptsUsed++
                        lastWrongOptionIndex = localSelectedOption!!
                        disabledOptions = disabledOptions + localSelectedOption!!

                        if (attemptsUsed < 3) {
                            questionState = QuizQuestionState.WRONG_RETRY
                        } else {
                            questionState = QuizQuestionState.FAILED_CANCELLED
                            onRecordResult(false, true)
                        }
                    }
                }
            },
            onTryAgain = {
                // "If I just got wrong, then it just go back. Second time, it show me."
                questionState = QuizQuestionState.IDLE
                localSelectedOption = null
                lastWrongOptionIndex = -1
            },
            onContinueNext = {
                if (currentIndex >= deck.quiz.lastIndex) {
                    SoundFeedbackManager.getInstance(context).playCelebrationSound()
                }
                onNextQuestion()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        // Floating XP feedback toast
        FloatingXpBadge(
            points = lastPointChange,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        )
    }
}

/**
 * Duolingo-style Question Card with 3D bottom border and subject badge
 */
@Composable
private fun DuolingoQuestionCard(
    questionNumber: Int,
    totalQuestions: Int,
    questionText: String,
    secondaryQuestionText: String? = null,
    xpValue: Int,
    subject: String,
    studyLanguage: String = "EN"
) {
    val subjectIcon = when {
        subject.contains("Physics", ignoreCase = true) -> "⚡"
        subject.contains("Math", ignoreCase = true) -> "📐"
        subject.contains("Chem", ignoreCase = true) -> "🧪"
        subject.contains("Bio", ignoreCase = true) -> "🧬"
        else -> "📚"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (studyLanguage == "HI") "🇮🇳 $subjectIcon प्रश्न $questionNumber / $totalQuestions" else "$subjectIcon ${subject.uppercase()} • Q$questionNumber OF $totalQuestions",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "+$xpValue XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = SuccessEmerald
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = questionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 25.sp
            )

            if (!secondaryQuestionText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "EN: $secondaryQuestionText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Duolingo 3D Option Card with Flipkart Shake Animation on Wrong Answer
 */
@Composable
private fun DuolingoOptionCard(
    optionIndex: Int,
    optionText: String,
    isSelected: Boolean,
    isDisabled: Boolean,
    isCorrect: Boolean,
    isShaking: Boolean,
    questionState: QuizQuestionState,
    onSelect: () -> Unit
) {
    val optionLetters = listOf("A", "B", "C")
    val letter = optionLetters.getOrElse(optionIndex) { "${optionIndex + 1}" }

    // Flipkart horizontal shake vibration
    val shakeOffset = remember { Animatable(0f) }
    val bounceScale = remember { Animatable(1f) }

    LaunchedEffect(isShaking) {
        if (isShaking) {
            // Multi-frequency horizontal shake
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 480
                    0f at 0
                    (-16f) at 45
                    16f at 90
                    (-12f) at 135
                    12f at 180
                    (-8f) at 225
                    8f at 270
                    (-4f) at 315
                    4f at 360
                    0f at 480
                }
            )
        }
    }

    LaunchedEffect(questionState) {
        if (questionState == QuizQuestionState.CORRECT && isCorrect) {
            // Spring bounce for correct answer
            bounceScale.animateTo(1.05f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            bounceScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
        }
    }

    // Dynamic border & container colors
    val isAnswerRevealed = questionState == QuizQuestionState.CORRECT || questionState == QuizQuestionState.FAILED_CANCELLED

    val borderColor by animateColorAsState(
        targetValue = when {
            isAnswerRevealed && isCorrect -> SuccessEmerald
            (isShaking || isDisabled) -> PenaltyRed
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        label = "duolingo_border_color"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isAnswerRevealed && isCorrect -> SuccessEmerald.copy(alpha = 0.14f)
            (isShaking || isDisabled) -> PenaltyRed.copy(alpha = 0.12f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "duolingo_container_color"
    )

    // 3D Bottom drop shadow simulation like Duolingo buttons
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .scale(bounceScale.value)
            .shadow(
                elevation = if (isSelected) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .border(
                width = if (isSelected || (isAnswerRevealed && isCorrect) || isShaking) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isDisabled && questionState == QuizQuestionState.IDLE) {
                onSelect()
            }
            .testTag("quiz_option_$optionIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isAnswerRevealed && isCorrect -> SuccessEmerald
                            (isShaking || isDisabled) -> PenaltyRed
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isAnswerRevealed && isCorrect -> "✓"
                        (isShaking || isDisabled) -> "✕"
                        else -> letter
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = when {
                        isSelected || (isAnswerRevealed && isCorrect) || isShaking || isDisabled -> Color.White
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || (isAnswerRevealed && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                color = if (isDisabled && !isAnswerRevealed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                lineHeight = 21.sp
            )

            // Trailing icon indicator
            if (isAnswerRevealed && isCorrect) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Correct",
                    tint = SuccessEmerald,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isShaking || isDisabled) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Wrong",
                    tint = PenaltyRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Duolingo Bottom Action Sheet:
 * Fixed bottom container displaying:
 * - Neutral: Big 3D "CHECK" button
 * - Wrong (Attempt 1 or 2): Red banner + "TRY AGAIN" button
 * - Wrong (Attempt 3): Dark red banner + Explanation + "GOT IT" cancel button
 * - Correct: Green banner + Explanation + "CONTINUE" button
 */
@Composable
private fun DuolingoActionSheet(
    questionState: QuizQuestionState,
    selectedOption: Int?,
    attemptsUsed: Int,
    currentQuestion: QuizQuestion,
    correctAnswerText: String,
    studyLanguage: String = "EN",
    onCheck: () -> Unit,
    onTryAgain: () -> Unit,
    onContinueNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val explanationText = LanguageHelper.getExplanation(currentQuestion, studyLanguage)
    val isWrongRetry = questionState == QuizQuestionState.WRONG_RETRY
    val isFailedCancelled = questionState == QuizQuestionState.FAILED_CANCELLED
    val isCorrect = questionState == QuizQuestionState.CORRECT

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = when {
                    isCorrect -> SuccessEmerald.copy(alpha = 0.35f)
                    isWrongRetry || isFailedCancelled -> PenaltyRed.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            ),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = when {
            isCorrect -> Color(0xFFE8F5E9)
            isWrongRetry || isFailedCancelled -> Color(0xFFFFEBEE)
            else -> MaterialTheme.colorScheme.surface
        },
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Feedback text when answered
            when (questionState) {
                QuizQuestionState.WRONG_RETRY -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PenaltyRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (studyLanguage == "HI") "गलत उत्तर! (प्रयास $attemptsUsed / 3)" else "Incorrect! (Attempt $attemptsUsed of 3)",
                                fontWeight = FontWeight.Black,
                                color = PenaltyRed,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (studyLanguage == "HI") "आपके पास ${3 - attemptsUsed} मौके शेष हैं। पुनः प्रयास करें!" else "You have ${3 - attemptsUsed} chances remaining. Keep trying!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC62828),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                QuizQuestionState.FAILED_CANCELLED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PenaltyRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (studyLanguage == "HI") "सारे प्रयास समाप्त! (प्रयास 3 / 3)" else "Out of attempts! (Attempt 3 of 3)",
                                fontWeight = FontWeight.Black,
                                color = PenaltyRed,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (studyLanguage == "HI") "सही उत्तर: $correctAnswerText" else "Correct solution: $correctAnswerText",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB71C1C),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = explanationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF424242),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                QuizQuestionState.CORRECT -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SuccessEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (studyLanguage == "HI") "शानदार! +${currentQuestion.xpValue} XP" else "Nicely done! +${currentQuestion.xpValue} XP",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32),
                                fontSize = 15.sp
                            )
                            Text(
                                text = explanationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1B5E20),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                QuizQuestionState.IDLE -> {
                    // Sits clean and spacious
                }
            }

            // Big 3D Duolingo Action Button
            when (questionState) {
                QuizQuestionState.IDLE -> {
                    DuolingoBigButton(
                        text = if (studyLanguage == "HI") "जांचें (CHECK)" else "CHECK",
                        isEnabled = selectedOption != null,
                        color = MaterialTheme.colorScheme.primary,
                        shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        testTag = "submit_answer_button",
                        onClick = onCheck
                    )
                }

                QuizQuestionState.WRONG_RETRY -> {
                    DuolingoBigButton(
                        text = if (studyLanguage == "HI") "पुनः प्रयास करें ↻" else "TRY AGAIN ↻",
                        isEnabled = true,
                        color = PenaltyRed,
                        shadowColor = Color(0xFFB71C1C),
                        testTag = "try_again_button",
                        onClick = onTryAgain
                    )
                }

                QuizQuestionState.FAILED_CANCELLED -> {
                    DuolingoBigButton(
                        text = if (studyLanguage == "HI") "समझ गया →" else "GOT IT →",
                        isEnabled = true,
                        color = Color(0xFF757575),
                        shadowColor = Color(0xFF424242),
                        testTag = "cancel_question_button",
                        onClick = onContinueNext
                    )
                }

                QuizQuestionState.CORRECT -> {
                    DuolingoBigButton(
                        text = if (studyLanguage == "HI") "आगे बढ़ें →" else "CONTINUE →",
                        isEnabled = true,
                        color = SuccessEmerald,
                        shadowColor = Color(0xFF2E7D32),
                        testTag = "next_question_button",
                        onClick = onContinueNext
                    )
                }
            }
        }
    }
}

/**
 * Duolingo-style 3D Button with bottom thickness
 */
@Composable
private fun DuolingoBigButton(
    text: String,
    isEnabled: Boolean,
    color: Color,
    shadowColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY = if (isPressed) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .offset(y = offsetY)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isEnabled) color else Color.Gray.copy(alpha = 0.35f))
            .border(
                width = 2.dp,
                color = if (isEnabled) shadowColor else Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )
    }
}
