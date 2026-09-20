package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.FlashcardDeck
import com.example.ui.theme.SuccessSage
import com.example.util.SoundFeedbackManager

data class LeitnerBoxData(
    val boxIndex: Int,
    val title: String,
    val subtitle: String,
    val intervalDays: String,
    val accentColor: Color,
    val cards: List<Flashcard>
)

@Composable
fun LeitnerBoxVisualizer(
    decks: List<FlashcardDeck>,
    selectedBox: Int?,
    onBoxSelected: (Int?) -> Unit,
    onReviewBoxCards: (List<Flashcard>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }

    val allCards = remember(decks) { decks.flatMap { it.cards } }
    val totalCards = allCards.size

    val boxesData = remember(allCards) {
        listOf(
            LeitnerBoxData(
                boxIndex = 1,
                title = "Box 1",
                subtitle = "Daily Blitz",
                intervalDays = "Everyday",
                accentColor = Color(0xFFE57373), // Coral Red
                cards = allCards.filter { it.leitnerBox == 1 }
            ),
            LeitnerBoxData(
                boxIndex = 2,
                title = "Box 2",
                subtitle = "Short Recall",
                intervalDays = "3 Days",
                accentColor = Color(0xFFFFB74D), // Amber
                cards = allCards.filter { it.leitnerBox == 2 }
            ),
            LeitnerBoxData(
                boxIndex = 3,
                title = "Box 3",
                subtitle = "Weekly Check",
                intervalDays = "7 Days",
                accentColor = Color(0xFFFFF176), // Gold Yellow
                cards = allCards.filter { it.leitnerBox == 3 }
            ),
            LeitnerBoxData(
                boxIndex = 4,
                title = "Box 4",
                subtitle = "Fortnight",
                intervalDays = "14 Days",
                accentColor = Color(0xFF81C784), // Light Sage
                cards = allCards.filter { it.leitnerBox == 4 }
            ),
            LeitnerBoxData(
                boxIndex = 5,
                title = "Box 5",
                subtitle = "Mastered",
                intervalDays = "30 Days",
                accentColor = SuccessSage,
                cards = allCards.filter { it.leitnerBox == 5 }
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .testTag("leitner_box_visualizer_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Title & Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LEITNER SPACED REPETITION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "5-Box Retention Distribution ($totalCards cards)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Hide ▴" else "View ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Cumulative Retention Multi-colored Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (totalCards > 0) {
                            boxesData.forEach { box ->
                                val weight = box.cards.size.toFloat() / totalCards.toFloat()
                                if (weight > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .height(6.dp)
                                            .background(box.accentColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrollable 5-Box Archival Shelves
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        boxesData.forEach { box ->
                            val isSelected = selectedBox == box.boxIndex
                            val animatedBorderColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                label = "leitner_box_border"
                            )
                            val animatedBgColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                label = "leitner_box_bg"
                            )

                            Surface(
                                modifier = Modifier
                                    .width(108.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        SoundFeedbackManager.getInstance(context).playCardFlipSound()
                                        onBoxSelected(if (isSelected) null else box.boxIndex)
                                    }
                                    .testTag("leitner_box_${box.boxIndex}"),
                                shape = RoundedCornerShape(10.dp),
                                color = animatedBgColor,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, animatedBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(box.accentColor)
                                        )
                                        Text(
                                            text = "${box.cards.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = box.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = box.subtitle,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = box.intervalDays,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Selection Bar / Quick Action
                    if (selectedBox != null) {
                        val activeBoxData = boxesData.firstOrNull { it.boxIndex == selectedBox }
                        if (activeBoxData != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Filtering: ${activeBoxData.title} (${activeBoxData.subtitle})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${activeBoxData.cards.size} cards due for review (${activeBoxData.intervalDays} interval)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }

                                    if (activeBoxData.cards.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                SoundFeedbackManager.getInstance(context).playCardFlipSound()
                                                onReviewBoxCards(activeBoxData.cards)
                                            },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
