package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.FlashcardDeck
import com.example.data.util.LanguageHelper
import com.example.ui.components.MasteryBurstEffect
import com.example.ui.theme.SuccessSage
import com.example.util.FlashcardPdfExporter
import com.example.util.SoundFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class StudyMode {
    CARD_FLIP,
    QUESTIONS_TO_SOLVE,
    ALL_CARDS_LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    deck: FlashcardDeck?,
    currentIndex: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onMarkMastery: (Boolean) -> Unit,
    onStartQuiz: () -> Unit,
    onBack: () -> Unit,
    studyLanguage: String = "EN",
    onToggleLanguage: () -> Unit = {},
    initialMode: StudyMode = StudyMode.CARD_FLIP,
    onProblemSolved: (problemId: String, isSolved: Boolean) -> Unit = { _, _ -> },
    onProblemDraftChange: (problemId: String, draft: String) -> Unit = { _, _ -> },
    onGenerateMoreProblems: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (deck == null || deck.cards.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No flashcards found in this deck.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Go Back",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var studyMode by remember(initialMode) { mutableStateOf(initialMode) }
    val currentCard = deck.cards.getOrNull(currentIndex) ?: deck.cards.first()
    val totalCards = deck.cards.size
    val progress = ((currentIndex + 1).toFloat() / totalCards.toFloat()).coerceIn(0f, 1f)
    val masteredCount = remember(deck) { deck.cards.count { it.isMastered } }

    // 3D Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "card_flip_3d"
    )
    val density = LocalDensity.current.density
    var showSparkle by remember { mutableStateOf(false) }

    LaunchedEffect(showSparkle) {
        if (showSparkle) {
            delay(1400)
            showSparkle = false
        }
    }

    // Swipe gesture physics state
    val dragOffsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Reset drag offset when card index changes
    LaunchedEffect(currentIndex) {
        dragOffsetX.snapTo(0f)
    }

    // Dynamic rotation based on horizontal drag (-10° to +10°)
    val dragRotation = (dragOffsetX.value / 45f).coerceIn(-12f, 12f)
    // Swipe opacity cues
    val swipeRightAlpha = (dragOffsetX.value / 200f).coerceIn(0f, 0.95f)
    val swipeLeftAlpha = (-dragOffsetX.value / 200f).coerceIn(0f, 0.95f)

    // Completion dialog flag
    var showCompletionModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sleek Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "$totalCards Cards • $masteredCount Mastered (${((masteredCount.toFloat() / totalCards) * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // Export Printable Duplex PDF Button
                IconButton(
                    onClick = {
                        FlashcardPdfExporter.exportDeckToPdf(context, deck)?.let { file ->
                            FlashcardPdfExporter.sharePdf(context, file, deck.title)
                        }
                    },
                    modifier = Modifier.testTag("study_export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Export Duplex PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Language Switcher Toggle Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onToggleLanguage() }
                        .testTag("study_language_toggle"),
                    shape = RoundedCornerShape(20.dp),
                    color = if (studyLanguage == "HI") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (studyLanguage == "HI") "🇮🇳 हिन्दी" else "🇬🇧 EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (studyLanguage == "HI") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (deck.quiz.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStartQuiz() }
                            .testTag("start_quiz_top_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Quiz (${deck.quiz.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 2. Animated Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // 3. View Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = studyMode == StudyMode.CARD_FLIP,
                    onClick = { studyMode = StudyMode.CARD_FLIP },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Card Flip", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = studyMode == StudyMode.QUESTIONS_TO_SOLVE,
                    onClick = { studyMode = StudyMode.QUESTIONS_TO_SOLVE },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Solve Problems (${deck.practiceProblems.size})", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = studyMode == StudyMode.ALL_CARDS_LIST,
                    onClick = { studyMode = StudyMode.ALL_CARDS_LIST },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("All Cards ($totalCards)", fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Quick gesture guidance pill
            if (studyMode == StudyMode.CARD_FLIP) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Swipe ↔ to rate",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        when (studyMode) {
            StudyMode.CARD_FLIP -> {
                // CARD FLIP VIEW - Interactive 3D Card with Swipe-to-Rate
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card ${currentIndex + 1} of $totalCards",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (currentCard.isMastered) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = SuccessSage,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Mastered",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessSage
                                    )
                                }
                            }
                        }

                        // Difficulty badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = currentCard.difficulty.replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // The Interactive 3D Swipable Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                        .graphicsLayer {
                            rotationZ = dragRotation
                            rotationY = rotation
                            cameraDistance = 16f * density
                        }
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    coroutineScope.launch {
                                        if (dragOffsetX.value > 180f) {
                                            // Swiped right -> Mastered!
                                            SoundFeedbackManager.getInstance(context).playCorrectSound()
                                            showSparkle = true
                                            dragOffsetX.animateTo(
                                                600f,
                                                spring(stiffness = Spring.StiffnessMedium)
                                            )
                                            onMarkMastery(true)
                                            if (currentIndex < totalCards - 1) {
                                                onNext()
                                            } else {
                                                SoundFeedbackManager.getInstance(context).playCelebrationSound()
                                                showCompletionModal = true
                                            }
                                            dragOffsetX.snapTo(0f)
                                        } else if (dragOffsetX.value < -180f) {
                                            // Swiped left -> Still learning!
                                            SoundFeedbackManager.getInstance(context).playWrongSound()
                                            dragOffsetX.animateTo(
                                                -600f,
                                                spring(stiffness = Spring.StiffnessMedium)
                                            )
                                            onMarkMastery(false)
                                            if (currentIndex < totalCards - 1) {
                                                onNext()
                                            } else {
                                                SoundFeedbackManager.getInstance(context).playCelebrationSound()
                                                showCompletionModal = true
                                            }
                                            dragOffsetX.snapTo(0f)
                                        } else {
                                            // Snap back to center
                                            dragOffsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    coroutineScope.launch {
                                        dragOffsetX.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                    }
                                }
                            )
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            SoundFeedbackManager.getInstance(context).playCardFlipSound()
                            onFlip()
                        }
                        .testTag("flashcard_3d_container"),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        CleanFlashcardFront(
                            card = currentCard,
                            studyLanguage = studyLanguage,
                            onFlip = onFlip,
                            onToggleLanguage = onToggleLanguage
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                        ) {
                            CleanFlashcardBack(
                                card = currentCard,
                                studyLanguage = studyLanguage,
                                onFlip = onFlip,
                                onToggleLanguage = onToggleLanguage
                            )
                        }
                    }

                    // Swipe Right Visual Cue (Green "MASTERED ✓")
                    if (swipeRightAlpha > 0.05f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(18.dp)
                                .graphicsLayer { alpha = swipeRightAlpha },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.92f),
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MASTERED",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Swipe Left Visual Cue (Amber/Red "STILL LEARNING ✕")
                    if (swipeLeftAlpha > 0.05f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(18.dp)
                                .graphicsLayer { alpha = swipeLeftAlpha },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFC62828).copy(alpha = 0.92f),
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STILL LEARNING",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    MasteryBurstEffect(
                        visible = showSparkle,
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Navigation & Mastery Action Bar (Responsive, weighted, navigation-safe)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Card Button
                        IconButton(
                            onClick = onPrevious,
                            enabled = currentIndex > 0,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("prev_card_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Card",
                                tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Still Learning button
                        TactileActionButton(
                            label = if (studyLanguage == "HI") "दोहराएं" else "Learning",
                            subLabel = if (studyLanguage == "HI") "Learning" else null,
                            icon = Icons.Filled.Close,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            testTag = "needs_review_button",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundFeedbackManager.getInstance(context).playWrongSound()
                                onMarkMastery(false)
                                if (currentIndex < totalCards - 1) onNext()
                                else {
                                    SoundFeedbackManager.getInstance(context).playCelebrationSound()
                                    showCompletionModal = true
                                }
                            }
                        )

                        // Center 3D Flip button
                        TactileActionButton(
                            label = if (isFlipped) (if (studyLanguage == "HI") "प्रश्न" else "Question")
                                    else (if (studyLanguage == "HI") "उत्तर देखें" else "Reveal"),
                            subLabel = if (isFlipped) "Flip Back" else "Flip Card",
                            icon = Icons.Filled.Flip,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTag = "flip_card_button",
                            modifier = Modifier.weight(1.15f),
                            onClick = {
                                SoundFeedbackManager.getInstance(context).playCardFlipSound()
                                onFlip()
                            }
                        )

                        // Mastered button with emerald green accent
                        TactileActionButton(
                            label = if (studyLanguage == "HI") "याद है" else "Mastered",
                            subLabel = if (studyLanguage == "HI") "Mastered" else null,
                            icon = Icons.Filled.Check,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            textColor = SuccessSage,
                            testTag = "mastered_card_button",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                SoundFeedbackManager.getInstance(context).playCorrectSound()
                                showSparkle = true
                                onMarkMastery(true)
                                if (currentIndex < totalCards - 1) onNext()
                                else {
                                    SoundFeedbackManager.getInstance(context).playCelebrationSound()
                                    showCompletionModal = true
                                }
                            }
                        )

                        // Next Card Button
                        IconButton(
                            onClick = {
                                if (currentIndex < totalCards - 1) onNext()
                                else showCompletionModal = true
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("next_card_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Card",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
        StudyMode.QUESTIONS_TO_SOLVE -> {
                com.example.ui.components.PracticeProblemSolver(
                    problems = deck.practiceProblems,
                    subject = deck.subject,
                    studyLanguage = studyLanguage,
                    onProblemSolved = onProblemSolved,
                    onProblemDraftChange = onProblemDraftChange,
                    onGenerateMore = onGenerateMoreProblems,
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudyMode.ALL_CARDS_LIST -> {
                // ALL CARDS ORGANIZED LIST VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(deck.cards, key = { _, card -> card.id }) { index, card ->
                        CleanCardListItem(
                            index = index + 1,
                            card = card,
                            studyLanguage = studyLanguage
                        )
                    }
                }
            }
        }
    }

    // Session Complete Celebration Modal
    if (showCompletionModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCompletionModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deck Session Finished! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You've reviewed all $totalCards cards in ${deck.title}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Mastery Score: $masteredCount of $totalCards (${((masteredCount.toFloat() / totalCards) * 100).toInt()}%)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (masteredCount.toFloat() / totalCards).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SuccessSage
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (deck.quiz.isNotEmpty()) {
                    androidx.compose.material3.Button(
                        onClick = {
                            showCompletionModal = false
                            onStartQuiz()
                        }
                    ) {
                        Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Deck Quiz")
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showCompletionModal = false
                        onBack()
                    }
                ) {
                    Text("Back to Decks")
                }
            }
        )
    }
}

/**
 * Tactile 3D Action Button with Push Scale Feedback
 */
@Composable
private fun TactileActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    textColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "tactile_btn_scale"
    )

    Surface(
        modifier = modifier
            .scale(animatedScale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (!subLabel.isNullOrBlank()) {
                    Text(
                        text = subLabel,
                        fontWeight = FontWeight.Normal,
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanFlashcardFront(
    card: Flashcard,
    studyLanguage: String = "EN",
    onFlip: () -> Unit,
    onToggleLanguage: () -> Unit = {}
) {
    val frontText = LanguageHelper.getFront(card, studyLanguage)
    val keyTermText = LanguageHelper.getKeyTerm(card, studyLanguage)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top tag & Language Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (studyLanguage == "HI") "🇮🇳 हिन्दी प्रश्न" else card.tag.ifBlank { "Concept Question" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onToggleLanguage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (studyLanguage == "HI") "Side 1 • हिन्दी ⇄ EN" else "Side 1 • Question ⇄ HI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Question / Concept Prompt
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = frontText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )

                // Secondary English reference subtitle when in Hindi mode
                if (studyLanguage == "HI" && card.front.isNotBlank() && card.front != frontText) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "EN: ${card.front}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (keyTermText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (studyLanguage == "HI") "प्रमुख शब्द: $keyTermText" else "Key Term: $keyTermText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Flip Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (studyLanguage == "HI") "उत्तर देखने के लिए टैप करें" else "Tap card to flip • Swipe right to master",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CleanFlashcardBack(
    card: Flashcard,
    studyLanguage: String = "EN",
    onFlip: () -> Unit,
    onToggleLanguage: () -> Unit = {}
) {
    val backText = LanguageHelper.getBack(card, studyLanguage)
    val keyTermText = LanguageHelper.getKeyTerm(card, studyLanguage)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (studyLanguage == "HI") "Side 2 • उत्तर एवं व्याख्या" else "Side 2 • Answer & Notes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (keyTermText.isNotBlank()) {
                    Text(
                        text = keyTermText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Answer Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = backText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 23.sp
                )

                // Secondary English text when in Hindi mode
                if (studyLanguage == "HI" && card.back.isNotBlank() && card.back != backText) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "EN: ${card.back}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Flip Back Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (studyLanguage == "HI") "प्रश्न पर वापस जाने के लिए टैप करें" else "Tap to flip back to question",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CleanCardListItem(
    index: Int,
    card: Flashcard,
    studyLanguage: String = "EN"
) {
    val frontText = LanguageHelper.getFront(card, studyLanguage)
    val backText = LanguageHelper.getBack(card, studyLanguage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (studyLanguage == "HI") "अवधारणा $index" else card.tag.ifBlank { "Concept" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (card.isMastered) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = if (studyLanguage == "HI") "कंठस्थ ✓" else "Mastered",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessSage,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Question
            Text(
                text = frontText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Answer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = backText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
