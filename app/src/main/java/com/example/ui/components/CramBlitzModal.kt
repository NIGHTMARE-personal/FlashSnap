package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Flashcard
import com.example.ui.theme.SuccessSage
import com.example.util.SoundFeedbackManager
import kotlinx.coroutines.delay

enum class CramState {
    CONFIG,
    ACTIVE,
    SUMMARY
}

@Composable
fun CramBlitzModal(
    availableCards: List<Flashcard>,
    onDismiss: () -> Unit,
    onCompleteBlitz: (masteredCount: Int, xpEarned: Int) -> Unit
) {
    val context = LocalContext.current
    var cramState by remember { mutableStateOf(CramState.CONFIG) }
    var selectedDurationMinutes by remember { mutableIntStateOf(3) }
    var secondsRemaining by remember { mutableIntStateOf(180) }

    // Queue of cards to cram (prioritizes unmastered & hard)
    var cramQueue by remember {
        mutableStateOf(
            availableCards.filter { !it.isMastered || it.difficulty == "Hard" }.ifEmpty { availableCards }.shuffled()
        )
    }

    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var cardsReviewedCount by remember { mutableIntStateOf(0) }
    var cardsMasteredCount by remember { mutableIntStateOf(0) }
    var currentStreak by remember { mutableIntStateOf(0) }
    var highestStreak by remember { mutableIntStateOf(0) }
    var xpEarned by remember { mutableIntStateOf(0) }

    // Countdown Timer during ACTIVE state
    LaunchedEffect(cramState) {
        if (cramState == CramState.ACTIVE) {
            secondsRemaining = selectedDurationMinutes * 60
            while (secondsRemaining > 0 && cramState == CramState.ACTIVE) {
                delay(1000)
                secondsRemaining--
            }
            if (cramState == CramState.ACTIVE) {
                SoundFeedbackManager.getInstance(context).playCelebrationSound()
                cramState = CramState.SUMMARY
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (cramState == CramState.SUMMARY) {
                onCompleteBlitz(cardsMasteredCount, xpEarned)
            }
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("cram_blitz_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            when (cramState) {
                CramState.CONFIG -> {
                    CramConfigView(
                        availableCount = cramQueue.size,
                        selectedDuration = selectedDurationMinutes,
                        onDurationSelect = { selectedDurationMinutes = it },
                        onStart = {
                            SoundFeedbackManager.getInstance(context).playShutterSound()
                            cramState = CramState.ACTIVE
                        },
                        onClose = onDismiss
                    )
                }

                CramState.ACTIVE -> {
                    if (cramQueue.isEmpty() || currentCardIndex >= cramQueue.size) {
                        // Queue completed before timer ran out
                        LaunchedEffect(Unit) {
                            SoundFeedbackManager.getInstance(context).playCelebrationSound()
                            cramState = CramState.SUMMARY
                        }
                    } else {
                        val activeCard = cramQueue[currentCardIndex]
                        CramActiveSessionView(
                            card = activeCard,
                            currentIndex = currentCardIndex,
                            totalCards = cramQueue.size,
                            secondsRemaining = secondsRemaining,
                            totalSeconds = selectedDurationMinutes * 60,
                            isFlipped = isFlipped,
                            streak = currentStreak,
                            xpEarned = xpEarned,
                            onFlip = {
                                SoundFeedbackManager.getInstance(context).playCardFlipSound()
                                isFlipped = !isFlipped
                            },
                            onReviewAgain = {
                                SoundFeedbackManager.getInstance(context).playWrongSound()
                                cardsReviewedCount++
                                currentStreak = 0
                                isFlipped = false
                                // Move card to the end of the cram queue for another pass
                                cramQueue = cramQueue + activeCard
                                currentCardIndex++
                            },
                            onGotIt = {
                                SoundFeedbackManager.getInstance(context).playCorrectSound()
                                cardsReviewedCount++
                                cardsMasteredCount++
                                currentStreak++
                                if (currentStreak > highestStreak) highestStreak = currentStreak
                                val bonus = if (currentStreak >= 3) 25 else 15
                                xpEarned += bonus
                                isFlipped = false
                                currentCardIndex++
                            },
                            onAbort = {
                                SoundFeedbackManager.getInstance(context).playCelebrationSound()
                                cramState = CramState.SUMMARY
                            }
                        )
                    }
                }

                CramState.SUMMARY -> {
                    CramSummaryView(
                        cardsReviewed = cardsReviewedCount,
                        cardsMastered = cardsMasteredCount,
                        highestStreak = highestStreak,
                        xpEarned = xpEarned,
                        onFinish = {
                            onCompleteBlitz(cardsMasteredCount, xpEarned)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CramConfigView(
    availableCount: Int,
    selectedDuration: Int,
    onDurationSelect: (Int) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFC0793D).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = Color(0xFFC0793D),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Smart Exam Cram Mode",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Rapid-fire blitz focusing on high-difficulty and unmastered concepts before your test.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$availableCount Cards in Cram Queue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Filtered for maximum revision efficiency",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SELECT BLITZ TIMER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(2, 3, 5).forEach { mins ->
                val isSelected = selectedDuration == mins
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDurationSelect(mins) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$mins MIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (mins == 2) "Sprint" else if (mins == 3) "Standard" else "Deep Focus",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("cram_blitz_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0793D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Begin Cram Blitz", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CramActiveSessionView(
    card: Flashcard,
    currentIndex: Int,
    totalCards: Int,
    secondsRemaining: Int,
    totalSeconds: Int,
    isFlipped: Boolean,
    streak: Int,
    xpEarned: Int,
    onFlip: () -> Unit,
    onReviewAgain: () -> Unit,
    onGotIt: () -> Unit,
    onAbort: () -> Unit
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val timerProgress = (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "cram_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Exit, Timer, and Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAbort) {
                Icon(Icons.Default.Close, contentDescription = "End Session")
            }

            // Countdown Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (secondsRemaining <= 30) Color(0xFFFF5252).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, if (secondsRemaining <= 30) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (secondsRemaining <= 30) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormatted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (secondsRemaining <= 30) Color(0xFFFF5252) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Streak Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${streak}x",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFFF7043)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timer Progress Bar
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (secondsRemaining <= 30) Color(0xFFFF5252) else Color(0xFFC0793D),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card Index & Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Card ${currentIndex + 1} of $totalCards",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+$xpEarned XP",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = SuccessSage
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Flippable Cram Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = flipRotation
                    cameraDistance = 12f * density
                }
                .clickable { onFlip() }
                .testTag("cram_card_surface"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (flipRotation <= 90f) {
                    // FRONT (Question)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = card.tag.ifBlank { "Concept" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = card.front,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap card to reveal answer",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // BACK (Answer)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessSage.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ANSWER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessSage,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = card.back,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Rate your recall below",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Bottom Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReviewAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cram_review_again_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE57373))
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Review Again", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onGotIt,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cram_got_it_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessSage),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Got It!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CramSummaryView(
    cardsReviewed: Int,
    cardsMastered: Int,
    highestStreak: Int,
    xpEarned: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SuccessSage.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = SuccessSage,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Cram Blitz Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Great study momentum before your exam!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                title = "Reviewed",
                value = "$cardsReviewed",
                subtitle = "Cards"
            )
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                title = "Mastered",
                value = "$cardsMastered",
                subtitle = "Retained"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                title = "Streak",
                value = "${highestStreak}x",
                subtitle = "Max Chain"
            )
            SummaryStatCard(
                modifier = Modifier.weight(1f),
                title = "XP Gained",
                value = "+$xpEarned",
                subtitle = "Blitz Bonus",
                highlight = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("cram_finish_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) Color(0xFFC0793D).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (highlight) Color(0xFFC0793D) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color(0xFFC0793D) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
