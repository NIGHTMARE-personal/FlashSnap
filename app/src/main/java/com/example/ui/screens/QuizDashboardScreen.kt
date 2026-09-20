package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardDeck
import com.example.data.model.QuizRecord
import com.example.data.model.SubjectMasteryStatus
import com.example.data.model.SubjectPerformanceSummary
import com.example.data.model.UserProfile
import com.example.ui.theme.GoldXP
import com.example.ui.theme.PenaltyWarmCocoa
import com.example.ui.theme.PenaltyWarmCocoaContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSage
import com.example.ui.theme.SuccessSageContainer
import com.example.ui.theme.WarmGoldXP
import java.util.Date

enum class QuizTimeframe(val label: String, val days: Int) {
    ALL_TIME("All Time", 0),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDashboardScreen(
    quizHistory: List<QuizRecord>,
    decks: List<FlashcardDeck>,
    profile: UserProfile,
    onBack: (() -> Unit)? = null,
    onStartQuiz: (deckId: String) -> Unit,
    onStudyDeck: (deckId: String) -> Unit,
    onClearHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(QuizTimeframe.ALL_TIME) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var selectedRecordIdForTooltip by remember { mutableStateOf<String?>(null) }

    // Filter history based on timeframe
    val timeframeFilteredHistory = remember(quizHistory, selectedTimeframe) {
        if (selectedTimeframe.days == 0) {
            quizHistory
        } else {
            val cutoff = System.currentTimeMillis() - (selectedTimeframe.days.toLong() * 86400000L)
            quizHistory.filter { it.timestamp >= cutoff }
        }
    }

    // Filter history based on selected subject chip
    val filteredHistory = remember(timeframeFilteredHistory, selectedSubjectFilter) {
        if (selectedSubjectFilter == "All") {
            timeframeFilteredHistory
        } else {
            timeframeFilteredHistory.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
        }
    }

    // Chronological history for trend visualization (oldest to newest)
    val chronologicalHistory = remember(filteredHistory) {
        filteredHistory.sortedBy { it.timestamp }
    }

    // High level metrics
    val totalQuizzes = filteredHistory.size
    val totalQuestionsAnswered = filteredHistory.sumOf { it.totalQuestions }
    val totalCorrect = filteredHistory.sumOf { it.correctCount }
    val totalMistakes = filteredHistory.sumOf { it.wrongCount }
    val overallAccuracy = if (totalQuestionsAnswered > 0) {
        ((totalCorrect.toFloat() / totalQuestionsAnswered) * 100).toInt()
    } else 0
    val totalNetXp = filteredHistory.sumOf { it.netPoints }

    // Subjects present in history
    val availableSubjects = remember(quizHistory) {
        listOf("All") + quizHistory.map { it.subject.ifBlank { "General" } }.distinct().sorted()
    }

    // Subject Performance Summaries (grouped & analyzed)
    val subjectSummaries = remember(timeframeFilteredHistory, decks) {
        calculateSubjectSummaries(timeframeFilteredHistory, decks)
    }

    // Weak subjects that critically need review (< 70% accuracy)
    val subjectsNeedingReview = remember(subjectSummaries) {
        subjectSummaries.filter { it.needsReview }
    }

    // Progress trend comparison: compare earlier half of quizzes vs recent half
    val trendImprovementText = remember(chronologicalHistory) {
        if (chronologicalHistory.size >= 4) {
            val mid = chronologicalHistory.size / 2
            val firstHalf = chronologicalHistory.take(mid)
            val secondHalf = chronologicalHistory.drop(mid)
            val firstAvg = if (firstHalf.isNotEmpty()) firstHalf.map { it.accuracyPercentage }.average() else 0.0
            val secondAvg = if (secondHalf.isNotEmpty()) secondHalf.map { it.accuracyPercentage }.average() else 0.0
            val diff = (secondAvg - firstAvg).toInt()
            if (diff > 0) "+$diff% accuracy gain" else if (diff < 0) "$diff% accuracy change" else "Consistent accuracy"
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ══════════════════════════════════════════════════════════════
        // 1. TOP APP BAR
        // ══════════════════════════════════════════════════════════════
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Quiz Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Progress Trends & Subject Diagnostics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("quiz_dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            },
            actions = {
                // Streak & Mastery chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = GoldXP,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.streakDays}d Streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // ══════════════════════════════════════════════════════════════
        // 2. TIMEFRAME SELECTOR PILLS
        // ══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuizTimeframe.values().forEach { timeframe ->
                val isSelected = selectedTimeframe == timeframe
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { selectedTimeframe = timeframe }
                        .testTag("timeframe_pill_${timeframe.name.lowercase()}")
                ) {
                    Text(
                        text = timeframe.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // 3. MAIN DASHBOARD CONTENT LIST
        // ══════════════════════════════════════════════════════════════
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ──────────────────────────────────────────────────────────
            // A. KEY PERFORMANCE SCORECARD (METRICS GRID)
            // ──────────────────────────────────────────────────────────
            item {
                PerformanceScorecard(
                    totalQuizzes = totalQuizzes,
                    overallAccuracy = overallAccuracy,
                    totalNetXp = totalNetXp,
                    subjectsNeedingReviewCount = subjectsNeedingReview.size,
                    trendImprovementText = trendImprovementText
                )
            }

            // ──────────────────────────────────────────────────────────
            // B. CRITICAL ALERT: SUBJECTS NEEDING REVIEW (< 70% Accuracy)
            // ──────────────────────────────────────────────────────────
            item {
                SubjectsNeedingReviewSection(
                    weakSubjects = subjectsNeedingReview,
                    onStartPracticeQuiz = onStartQuiz,
                    onStudyFlashcards = onStudyDeck
                )
            }

            // ──────────────────────────────────────────────────────────
            // C. PROGRESS OVER TIME TREND CHART
            // ──────────────────────────────────────────────────────────
            item {
                ProgressOverTimeCard(
                    chronologicalHistory = chronologicalHistory,
                    selectedRecordId = selectedRecordIdForTooltip,
                    onSelectRecord = { id -> selectedRecordIdForTooltip = id }
                )
            }

            // ──────────────────────────────────────────────────────────
            // D. SUBJECT MASTERY MATRIX (All Subjects Breakdown)
            // ──────────────────────────────────────────────────────────
            item {
                Text(
                    text = "Subject Mastery Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Accuracy scores and question-by-question mastery per subject",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (subjectSummaries.isEmpty()) {
                item {
                    EmptySubjectSummaryCard()
                }
            } else {
                items(subjectSummaries) { summary ->
                    SubjectMasteryCard(
                        summary = summary,
                        onPracticeQuiz = { deckId ->
                            if (!deckId.isNullOrBlank()) onStartQuiz(deckId)
                        },
                        onStudyDeck = { deckId ->
                            if (!deckId.isNullOrBlank()) onStudyDeck(deckId)
                        }
                    )
                }
            }

            // ──────────────────────────────────────────────────────────
            // E. PAST QUIZ ATTEMPTS AUDIT TRAIL (CHRONOLOGICAL LOG)
            // ──────────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Past Quiz Log (${filteredHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Chronological record of recent exam scores & penalties",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subject filter chip row for quiz history log
            if (availableSubjects.size > 2) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSubjects.forEach { subject ->
                            FilterChip(
                                selected = selectedSubjectFilter == subject,
                                onClick = { selectedSubjectFilter = subject },
                                label = { Text(subject) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (filteredHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No quizzes found for this timeframe/subject filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredHistory, key = { it.id }) { record ->
                    QuizHistoryRowItem(
                        record = record,
                        isHighlighted = record.id == selectedRecordIdForTooltip,
                        onRetake = {
                            if (record.deckId.isNotBlank()) onStartQuiz(record.deckId)
                        }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// COMPONENT: Performance Scorecard (KPI Grid)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun PerformanceScorecard(
    totalQuizzes: Int,
    overallAccuracy: Int,
    totalNetXp: Int,
    subjectsNeedingReviewCount: Int,
    trendImprovementText: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Overall Quiz Performance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (overallAccuracy >= 80) "Exceptional Retention"
                            else if (overallAccuracy >= 70) "Solid Working Knowledge"
                            else "Requires Active Revision",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overallAccuracy >= 80) SuccessSage
                            else if (overallAccuracy >= 70) WarmGoldXP
                            else PenaltyWarmCocoa
                        )
                    }
                }

                if (trendImprovementText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessSageContainer
                    ) {
                        Text(
                            text = trendImprovementText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessSage,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Grid Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Overall Accuracy %
                StatCard(
                    label = "ACCURACY",
                    value = "$overallAccuracy%",
                    subtext = if (overallAccuracy >= 70) "Above 70% threshold" else "Below target standard",
                    accentColor = if (overallAccuracy >= 70) SuccessSage else PenaltyWarmCocoa,
                    modifier = Modifier.weight(1f)
                )

                // Stat 2: Total Quizzes Taken
                StatCard(
                    label = "QUIZZES",
                    value = "$totalQuizzes",
                    subtext = "Tests completed",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 3: Net XP Earned (+15 / -10)
                StatCard(
                    label = "NET XP EARNED",
                    value = if (totalNetXp >= 0) "+$totalNetXp" else "$totalNetXp",
                    subtext = "Penalties deducted",
                    accentColor = if (totalNetXp >= 0) GoldXP else PenaltyWarmCocoa,
                    modifier = Modifier.weight(1f)
                )

                // Stat 4: Needs Review Count
                StatCard(
                    label = "ATTENTION NEEDED",
                    value = if (subjectsNeedingReviewCount > 0) "$subjectsNeedingReviewCount Subject${if (subjectsNeedingReviewCount > 1) "s" else ""}" else "None",
                    subtext = if (subjectsNeedingReviewCount > 0) "Accuracy < 70%" else "All subjects on track!",
                    accentColor = if (subjectsNeedingReviewCount > 0) PenaltyWarmCocoa else SuccessSage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// COMPONENT: Subjects Needing Review Critical Section
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun SubjectsNeedingReviewSection(
    weakSubjects: List<SubjectPerformanceSummary>,
    onStartPracticeQuiz: (deckId: String) -> Unit,
    onStudyFlashcards: (deckId: String) -> Unit
) {
    if (weakSubjects.isEmpty()) {
        // High Mastery Celebration Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SuccessSage.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessSageContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SuccessSage),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "All Subjects On Track! 🎯",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SuccessSage
                    )
                    Text(
                        text = "Every tested subject is currently performing at or above the 70% retention benchmark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Critical Review Required Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PenaltyWarmCocoa.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PenaltyWarmCocoaContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PenaltyWarmCocoa),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Warning",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Subjects Needing Immediate Review",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PenaltyWarmCocoa
                        )
                        Text(
                            text = "${weakSubjects.size} topic${if (weakSubjects.size > 1) "s" else ""} scored below 70% accuracy threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                weakSubjects.forEach { weak ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(1.dp, PenaltyWarmCocoa.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = getSubjectEmoji(weak.subject),
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = weak.subject,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PenaltyWarmCocoaContainer
                                ) {
                                    Text(
                                        text = "${weak.averageAccuracy}% Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PenaltyWarmCocoa,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = weak.weakTopicsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dual Action Buttons: Practice Quiz & Flashcards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val targetDeckId = weak.recommendedDeckId
                                Button(
                                    onClick = {
                                        if (!targetDeckId.isNullOrBlank()) onStartPracticeQuiz(targetDeckId)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PenaltyWarmCocoa,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("practice_quiz_btn_${weak.subject.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Targeted Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!targetDeckId.isNullOrBlank()) onStudyFlashcards(targetDeckId)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("study_flashcards_btn_${weak.subject.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Study Cards", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// COMPONENT: Progress Over Time Trend Canvas Chart
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ProgressOverTimeCard(
    chronologicalHistory: List<QuizRecord>,
    selectedRecordId: String?,
    onSelectRecord: (String) -> Unit
) {
    val selectedRecord = remember(chronologicalHistory, selectedRecordId) {
        chronologicalHistory.firstOrNull { it.id == selectedRecordId } ?: chronologicalHistory.lastOrNull()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accuracy Progress Over Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Scores plotted chronologically with 70% threshold benchmark",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${chronologicalHistory.size} Quizzes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chronologicalHistory.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Take 2 or more quizzes to see your visual progress curve over time!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Interactive Compose Canvas Line & Area Chart
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val greenAccent = SuccessSage
                val redAccent = PenaltyWarmCocoa

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 20f
                    val paddingTop = 10f
                    val usableHeight = height - paddingTop - paddingBottom

                    // Draw 4 Horizontal Gridlines (100%, 75%, 50%, 25%)
                    val levels = listOf(100f, 75f, 50f, 25f)
                    levels.forEach { level ->
                        val y = paddingTop + usableHeight * (1f - (level / 100f))
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw 70% Target Benchmark Dashed Guide Line
                    val benchmarkY = paddingTop + usableHeight * (1f - 0.70f)
                    drawLine(
                        color = WarmGoldXP.copy(alpha = 0.6f),
                        start = Offset(0f, benchmarkY),
                        end = Offset(width, benchmarkY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )

                    // Map history points to coordinates
                    val count = chronologicalHistory.size
                    val stepX = if (count > 1) width / (count - 1) else width

                    val points = chronologicalHistory.mapIndexed { idx, item ->
                        val x = idx * stepX
                        val normalizedAcc = (item.accuracyPercentage.toFloat() / 100f).coerceIn(0f, 1f)
                        val y = paddingTop + usableHeight * (1f - normalizedAcc)
                        Offset(x, y)
                    }

                    // Draw Gradient Fill under line
                    val areaPath = Path().apply {
                        moveTo(points.first().x, height - paddingBottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    // Draw Connected Trend Line
                    val linePath = Path().apply {
                        points.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y)
                            else {
                                val prev = points[i - 1]
                                val cx = (prev.x + pt.x) / 2f
                                cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                            }
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw individual data nodes
                    points.forEachIndexed { idx, pt ->
                        val item = chronologicalHistory[idx]
                        val isSelected = item.id == selectedRecord?.id
                        val nodeColor = if (item.accuracyPercentage >= 80) greenAccent
                        else if (item.accuracyPercentage >= 70) WarmGoldXP
                        else redAccent

                        // Node Outer Halo
                        drawCircle(
                            color = if (isSelected) primaryColor.copy(alpha = 0.4f) else nodeColor.copy(alpha = 0.25f),
                            radius = if (isSelected) 10.dp.toPx() else 6.dp.toPx(),
                            center = pt
                        )

                        // Node Solid Center
                        drawCircle(
                            color = if (isSelected) primaryColor else nodeColor,
                            radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )

                        // White Core
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Interactive Selected Point Tooltip Card
                if (selectedRecord != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = getSubjectEmoji(selectedRecord.subject),
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedRecord.deckTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatTimestamp(selectedRecord.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${selectedRecord.accuracyPercentage}% Accuracy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedRecord.accuracyPercentage >= 70) SuccessSage else PenaltyWarmCocoa
                                )
                                Text(
                                    text = "${selectedRecord.correctCount}/${selectedRecord.totalQuestions} Correct (${if (selectedRecord.netPoints >= 0) "+${selectedRecord.netPoints}" else "${selectedRecord.netPoints}"} XP)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// COMPONENT: Subject Mastery Card (Matrix Row)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun SubjectMasteryCard(
    summary: SubjectPerformanceSummary,
    onPracticeQuiz: (deckId: String?) -> Unit,
    onStudyDeck: (deckId: String?) -> Unit
) {
    val isNeedsReview = summary.masteryStatus == SubjectMasteryStatus.NEEDS_REVIEW
    val isMastered = summary.masteryStatus == SubjectMasteryStatus.MASTERED

    val cardBorderColor = if (isNeedsReview) PenaltyWarmCocoa.copy(alpha = 0.45f)
    else if (isMastered) SuccessSage.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNeedsReview) PenaltyWarmCocoaContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Subject, Emoji & Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = getSubjectEmoji(summary.subject), fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${summary.totalQuizzes} quiz${if (summary.totalQuizzes > 1) "zes" else ""} taken",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mastery Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (summary.masteryStatus) {
                        SubjectMasteryStatus.NEEDS_REVIEW -> PenaltyWarmCocoaContainer
                        SubjectMasteryStatus.DEVELOPING -> MaterialTheme.colorScheme.surfaceVariant
                        SubjectMasteryStatus.MASTERED -> SuccessSageContainer
                    }
                ) {
                    Text(
                        text = when (summary.masteryStatus) {
                            SubjectMasteryStatus.NEEDS_REVIEW -> "Needs Review ⚠️"
                            SubjectMasteryStatus.DEVELOPING -> "Developing 📈"
                            SubjectMasteryStatus.MASTERED -> "Mastered 🌟"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (summary.masteryStatus) {
                            SubjectMasteryStatus.NEEDS_REVIEW -> PenaltyWarmCocoa
                            SubjectMasteryStatus.DEVELOPING -> MaterialTheme.colorScheme.onSurfaceVariant
                            SubjectMasteryStatus.MASTERED -> SuccessSage
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accuracy progress bar & percentage label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mastery Accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${summary.averageAccuracy}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (summary.averageAccuracy >= 70) SuccessSage else PenaltyWarmCocoa
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Two-tone progress bar
            val progress = (summary.averageAccuracy / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (summary.averageAccuracy >= 70) SuccessSage else PenaltyWarmCocoa,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Questions breakdown: Correct vs Mistakes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${summary.correctCount} Correct • ${summary.wrongCount} Mistakes",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Net XP: ${if (summary.netPoints >= 0) "+${summary.netPoints}" else "${summary.netPoints}"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.netPoints >= 0) SuccessSage else PenaltyWarmCocoa
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summary.weakTopicsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            // Quick action footer button if a deck is recommended
            if (!summary.recommendedDeckId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPracticeQuiz(summary.recommendedDeckId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Practice Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onStudyDeck(summary.recommendedDeckId) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Review Cards", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// COMPONENT: Past Quiz Row Item (Chronological Log)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun QuizHistoryRowItem(
    record: QuizRecord,
    isHighlighted: Boolean,
    onRetake: () -> Unit
) {
    val isPassing = record.accuracyPercentage >= 70
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject Emoji icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isPassing) SuccessSageContainer else PenaltyWarmCocoaContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = getSubjectEmoji(record.subject), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.deckTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = record.subject,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTimestamp(record.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (record.durationSeconds > 0) {
                        Text(
                            text = "• ${record.durationSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${record.correctCount}/${record.totalQuestions} Correct (${record.accuracyPercentage}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPassing) SuccessSage else PenaltyWarmCocoa
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Net XP Badge & Retake Button
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (record.netPoints >= 0) SuccessSageContainer else PenaltyWarmCocoaContainer
                ) {
                    Text(
                        text = if (record.netPoints >= 0) "+${record.netPoints} XP" else "${record.netPoints} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (record.netPoints >= 0) SuccessSage else PenaltyWarmCocoa,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                IconButton(
                    onClick = onRetake,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Retake Quiz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySubjectSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No quiz history yet. Take your first quiz in the Quiz Arena to build your subject mastery dashboard!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═════════════════════════════════════════════════════════════════════

private fun calculateSubjectSummaries(
    history: List<QuizRecord>,
    decks: List<FlashcardDeck>
): List<SubjectPerformanceSummary> {
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

        val matchingDeck = decks.firstOrNull { it.subject.equals(subject, ignoreCase = true) }
            ?: records.firstOrNull()?.let { r -> decks.firstOrNull { it.id == r.deckId } }

        val weakTopicsSummary = when (masteryStatus) {
            SubjectMasteryStatus.NEEDS_REVIEW ->
                "Recent accuracy is $avgAccuracy% with $totalWrong mistakes. Prioritize reviewing core definitions & formulas."
            SubjectMasteryStatus.DEVELOPING ->
                "Good progress at $avgAccuracy%. Practice occasional missed cards to reach permanent mastery."
            SubjectMasteryStatus.MASTERED ->
                "Outstanding retention ($avgAccuracy% accuracy)! All key concepts securely mastered."
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

    // Sort: NEEDS_REVIEW first, then lowest accuracy
    return summaries.sortedWith(
        compareBy(
            { it.masteryStatus.ordinal },
            { it.averageAccuracy }
        )
    )
}

private fun getSubjectEmoji(subject: String): String {
    val lower = subject.lowercase()
    return when {
        lower.contains("chem") -> "🧪"
        lower.contains("phys") -> "⚡"
        lower.contains("math") || lower.contains("calc") -> "📐"
        lower.contains("bio") -> "🧬"
        lower.contains("hist") -> "📜"
        lower.contains("geo") -> "🌍"
        lower.contains("lit") || lower.contains("eng") -> "📖"
        else -> "📚"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneHour = 3600000L
    val oneDay = 86400000L

    return when {
        diff < 60000L -> "Just now"
        diff < oneHour -> "${diff / 60000L}m ago"
        diff < oneDay -> "${diff / oneHour}h ago"
        diff < oneDay * 2 -> "Yesterday"
        diff < oneDay * 7 -> "${diff / oneDay}d ago"
        else -> DateFormat.format("MMM d, yyyy", Date(timestamp)).toString()
    }
}
