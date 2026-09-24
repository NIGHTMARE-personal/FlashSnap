package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlashcardDeck
import com.example.data.model.UserProfile
import com.example.ui.components.CramBlitzModal
import com.example.ui.components.LeitnerBoxVisualizer
import com.example.ui.theme.SuccessSage
import com.example.util.FlashcardPdfExporter
import com.example.util.SoundFeedbackManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeckFilter {
    ALL,
    IN_PROGRESS,
    MASTERED
}

enum class DecksTab {
    ALL_DECKS,
    ALL_FLASHCARDS,
    TIMELINE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    decks: List<FlashcardDeck>,
    profile: UserProfile,
    onDeckClick: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onGoToCamera: () -> Unit,
    onSolveProblems: (String) -> Unit = { onDeckClick(it) },
    onMarkCardMastery: ((deckId: String, cardId: String, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DecksTab.ALL_DECKS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(DeckFilter.ALL) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    val context = LocalContext.current
    var showCramModal by remember { mutableStateOf(false) }
    var selectedLeitnerBox by remember { mutableStateOf<Int?>(null) }
    var showLeitnerBoxShelf by remember { mutableStateOf(false) }

    val subjectsInDecks = remember(decks) {
        listOf("All") + decks.map { it.subject }.distinct().filter { it.isNotBlank() && it != "All" }
    }

    // Extract unique content tags across all cards in all decks
    val allContentTags = remember(decks) {
        decks.flatMap { deck ->
            deck.cards.mapNotNull { card ->
                card.tag.trim().takeIf { it.isNotBlank() }
            }
        }.distinct().sorted()
    }

    val filteredDecks = remember(decks, searchQuery, selectedTagFilter, selectedFilter, selectedSubjectFilter, selectedLeitnerBox) {
        val query = searchQuery.trim().removePrefix("#")
        decks.filter { deck ->
            val matchesSubject = selectedSubjectFilter == "All" ||
                    deck.subject.equals(selectedSubjectFilter, ignoreCase = true)

            val matchesTag = selectedTagFilter == null ||
                    deck.cards.any { it.tag.equals(selectedTagFilter, ignoreCase = true) }

            val matchesLeitner = selectedLeitnerBox == null ||
                    deck.cards.any { it.leitnerBox == selectedLeitnerBox }

            val matchesSearch = query.isBlank() ||
                    deck.title.contains(query, ignoreCase = true) ||
                    deck.description.contains(query, ignoreCase = true) ||
                    deck.subject.contains(query, ignoreCase = true) ||
                    deck.cards.any { card ->
                        card.tag.contains(query, ignoreCase = true) ||
                        card.keyTerm.contains(query, ignoreCase = true) ||
                        card.front.contains(query, ignoreCase = true) ||
                        card.back.contains(query, ignoreCase = true)
                    }

            val totalCards = deck.cards.size
            val masteredCards = deck.cards.count { it.isMastered }
            val isMastered = totalCards > 0 && masteredCards == totalCards
            val isInProgress = totalCards > 0 && masteredCards < totalCards

            val matchesFilter = when (selectedFilter) {
                DeckFilter.ALL -> true
                DeckFilter.IN_PROGRESS -> isInProgress
                DeckFilter.MASTERED -> isMastered
            }

            matchesSubject && matchesTag && matchesSearch && matchesFilter && matchesLeitner
        }
    }

    val timelineDecks = remember(filteredDecks) {
        filteredDecks.sortedByDescending { it.createdAt }
    }

    val totalFlashcards = remember(decks) { decks.sumOf { it.cards.size } }
    val totalMastered = remember(decks) { decks.sumOf { d -> d.cards.count { it.isMastered } } }

    val allCardsWithDeck = remember(filteredDecks, searchQuery) {
        val query = searchQuery.trim().removePrefix("#")
        filteredDecks.flatMap { deck ->
            deck.cards.map { card -> card to deck }
        }.filter { (card, deck) ->
            if (query.isBlank()) true
            else card.front.contains(query, ignoreCase = true) ||
                 card.back.contains(query, ignoreCase = true) ||
                 card.tag.contains(query, ignoreCase = true) ||
                 card.keyTerm.contains(query, ignoreCase = true) ||
                 deck.title.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Clean Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Flashcard Decks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${decks.size} Decks • $totalFlashcards Total Flashcards",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Study Space Cloud",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Study Space",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Top Search Bar: Instant Filtering by Title and Content Tags
        DecksTopSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            availableTags = allContentTags,
            selectedTag = selectedTagFilter,
            onTagSelected = { tag ->
                selectedTagFilter = if (selectedTagFilter == tag) null else tag
            },
            matchingCount = filteredDecks.size,
            totalCount = decks.size,
            onClearAll = {
                searchQuery = ""
                selectedTagFilter = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // View Mode Switcher: All Decks vs Capture Timeline
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == DecksTab.ALL_DECKS,
                onClick = { selectedTab = DecksTab.ALL_DECKS },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Decks (${decks.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == DecksTab.ALL_FLASHCARDS,
                onClick = { selectedTab = DecksTab.ALL_FLASHCARDS },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flashcards (${allCardsWithDeck.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == DecksTab.TIMELINE,
                onClick = { selectedTab = DecksTab.TIMELINE },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == DecksTab.ALL_FLASHCARDS) {
                // ══════════════════════════════════════════════════════════════
                // ALL FLASHCARDS FEED VIEW: Scrollable List with Bounded Max Height Containers
                // ══════════════════════════════════════════════════════════════
                if (allCardsWithDeck.isEmpty()) {
                    item {
                        CleanEmptyDecksState(
                            hasDecks = decks.isNotEmpty(),
                            isFiltered = searchQuery.isNotBlank() || selectedTagFilter != null || selectedSubjectFilter != "All" || selectedFilter != DeckFilter.ALL,
                            onClearFilter = {
                                searchQuery = ""
                                selectedTagFilter = null
                                selectedSubjectFilter = "All"
                                selectedFilter = DeckFilter.ALL
                                selectedLeitnerBox = null
                            },
                            onGoToCamera = onGoToCamera
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FLASHCARDS FEED (${allCardsWithDeck.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Tap card to flip • Bounded height",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    itemsIndexed(allCardsWithDeck, key = { _, pair -> "${pair.second.id}_${pair.first.id}" }) { index, (card, deck) ->
                        ScrollableFlashcardItem(
                            index = index + 1,
                            card = card,
                            deckTitle = deck.title,
                            onToggleMastery = { isMastered ->
                                onMarkCardMastery?.invoke(deck.id, card.id, isMastered)
                            },
                            onClickStudy = { onDeckClick(deck.id) }
                        )
                    }
                }
            } else if (selectedTab == DecksTab.TIMELINE) {
                // ══════════════════════════════════════════════════════════════
                // TIMELINE VIEW: Chronological Scan Journal
                // ══════════════════════════════════════════════════════════════
                if (filteredDecks.isEmpty()) {
                    item {
                        CleanEmptyDecksState(
                            hasDecks = decks.isNotEmpty(),
                            isFiltered = searchQuery.isNotBlank() || selectedTagFilter != null || selectedSubjectFilter != "All" || selectedFilter != DeckFilter.ALL,
                            onClearFilter = {
                                searchQuery = ""
                                selectedTagFilter = null
                                selectedSubjectFilter = "All"
                                selectedFilter = DeckFilter.ALL
                            },
                            onGoToCamera = onGoToCamera
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CAPTURE TIMELINE (${timelineDecks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Chronological notebook scans",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Render chronological timeline nodes (sorted newest first)
                    items(timelineDecks, key = { "timeline_${it.id}" }) { deck ->
                        TimelineDeckItem(
                            deck = deck,
                            selectedTagFilter = selectedTagFilter,
                            onTagClick = { tag ->
                                selectedTagFilter = if (selectedTagFilter.equals(tag, ignoreCase = true)) null else tag
                            },
                            onStudyClick = { onDeckClick(deck.id) },
                            onSolveClick = { onSolveProblems(deck.id) },
                            onQuizClick = { onStartQuiz(deck.id) },
                            onDeleteClick = { onDeleteDeck(deck.id) }
                        )
                    }
                }
            } else {
                // ══════════════════════════════════════════════════════════════
                // ALL DECKS VIEW: Organized Overview, Filters, Leitner & Decks
                // ══════════════════════════════════════════════════════════════
                // 1. Sleek Overview Metric Strip
                item {
                    DecksOverviewRow(
                        deckCount = decks.size,
                        cardCount = totalFlashcards,
                        masteredCount = totalMastered,
                        streakDays = profile.streakDays
                    )
                }

                // 2. Smart Exam Cram Mode Prompt Card
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFC0793D).copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC0793D).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                SoundFeedbackManager.getInstance(context).playShutterSound()
                                showCramModal = true
                            }
                            .testTag("start_cram_blitz_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC0793D).copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = Color(0xFFC0793D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Smart Exam Cram Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Rapid-fire blitz on high-difficulty cards before exams",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFC0793D),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Start Blitz ⚡",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Subject and Completion Status Filter Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjectsInDecks.forEach { subj ->
                                val icon = when (subj) {
                                    "Physics" -> "⚡"
                                    "Math" -> "📐"
                                    "Chemistry" -> "🧪"
                                    "Biology" -> "🧬"
                                    "History" -> "📜"
                                    else -> "📚"
                                }
                                val isSelected = selectedSubjectFilter.equals(subj, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubjectFilter = subj },
                                    label = { Text(if (subj == "All") "All Subjects" else "$icon $subj", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == DeckFilter.ALL,
                                onClick = { selectedFilter = DeckFilter.ALL },
                                label = { Text("All (${decks.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == DeckFilter.IN_PROGRESS,
                                onClick = { selectedFilter = DeckFilter.IN_PROGRESS },
                                label = { Text("In Progress", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == DeckFilter.MASTERED,
                                onClick = { selectedFilter = DeckFilter.MASTERED },
                                label = { Text("Mastered", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // 4. Collapsible Leitner Spaced Repetition 5-Box Shelf
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLeitnerBoxShelf = !showLeitnerBoxShelf },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Leitner Spaced Repetition Shelf",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (showLeitnerBoxShelf) "Collapse" else if (selectedLeitnerBox != null) "Box $selectedLeitnerBox Active" else "View 5 Boxes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = if (showLeitnerBoxShelf) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showLeitnerBoxShelf) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    LeitnerBoxVisualizer(
                                        decks = decks,
                                        selectedBox = selectedLeitnerBox,
                                        onBoxSelected = { selectedLeitnerBox = it },
                                        onReviewBoxCards = { cards ->
                                            val matchingDeck = decks.firstOrNull { d -> d.cards.any { c -> cards.any { bc -> bc.id == c.id } } }
                                            matchingDeck?.let { onDeckClick(it.id) }
                                        },
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Decks List or Empty State
                if (filteredDecks.isEmpty()) {
                    item {
                        CleanEmptyDecksState(
                            hasDecks = decks.isNotEmpty(),
                            isFiltered = searchQuery.isNotBlank() || selectedTagFilter != null || selectedSubjectFilter != "All" || selectedFilter != DeckFilter.ALL,
                            onClearFilter = {
                                searchQuery = ""
                                selectedTagFilter = null
                                selectedSubjectFilter = "All"
                                selectedFilter = DeckFilter.ALL
                            },
                            onGoToCamera = onGoToCamera
                        )
                    }
                } else {
                    items(filteredDecks, key = { it.id }) { deck ->
                        CleanDeckItemCard(
                            deck = deck,
                            selectedTagFilter = selectedTagFilter,
                            searchQuery = searchQuery,
                            onTagClick = { tag ->
                                selectedTagFilter = if (selectedTagFilter.equals(tag, ignoreCase = true)) null else tag
                            },
                            onStudyClick = { onDeckClick(deck.id) },
                            onSolveClick = { onSolveProblems(deck.id) },
                            onQuizClick = { onStartQuiz(deck.id) },
                            onDeleteClick = { onDeleteDeck(deck.id) }
                        )
                    }
                }
            }
        }

        if (showCramModal) {
            val allCards = remember(decks) { decks.flatMap { it.cards } }
            CramBlitzModal(
                availableCards = allCards,
                onDismiss = { showCramModal = false },
                onCompleteBlitz = { _, _ ->
                    showCramModal = false
                }
            )
        }
    }
}

/**
 * Timeline Node Item showing chronological progression, subject badge, and extracted notes
 */
@Composable
private fun TimelineDeckItem(
    deck: FlashcardDeck,
    selectedTagFilter: String? = null,
    onTagClick: (String) -> Unit = {},
    onStudyClick: () -> Unit,
    onQuizClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSolveClick: () -> Unit = onStudyClick
) {
    val totalCards = deck.cards.size
    val masteredCards = deck.cards.count { it.isMastered }
    val progressRatio = if (totalCards > 0) masteredCards.toFloat() / totalCards.toFloat() else 0f

    val subjectIcon = when (deck.subject) {
        "Physics" -> "⚡"
        "Math" -> "📐"
        "Chemistry" -> "🧪"
        "Biology" -> "🧬"
        "History" -> "📜"
        else -> "📚"
    }

    val formattedDate = remember(deck.createdAt) {
        try {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            sdf.format(Date(deck.createdAt))
        } catch (e: Exception) {
            "Today"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_item_${deck.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Timeline stem and marker
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Node circle marker
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subjectIcon, fontSize = 14.sp)
            }
            // Vertical timeline line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        }

        // Timeline Content Card
        val timelineInteractionSource = remember { MutableInteractionSource() }
        val isTimelinePressed by timelineInteractionSource.collectIsPressedAsState()
        val timelineScale by animateFloatAsState(
            targetValue = if (isTimelinePressed) 0.98f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "timeline_card_scale"
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .scale(timelineScale)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(interactionSource = timelineInteractionSource, indication = null) { onStudyClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Subject Chip, Page Count & Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$subjectIcon ${deck.subject}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (deck.pagesCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${deck.pagesCount} pages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // Title
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Extracted Text Summary Snippet
                if (deck.extractedSummaryText.isNotBlank()) {
                    Text(
                        text = deck.extractedSummaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                // Content Tags
                val deckTags = remember(deck) {
                    deck.cards.mapNotNull { it.tag.trim().takeIf { t -> t.isNotBlank() } }.distinct()
                }
                if (deckTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        deckTags.take(4).forEach { tag ->
                            val isTagActive = selectedTagFilter.equals(tag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isTagActive) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isTagActive) null
                                        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .clickable { onTagClick(tag) }
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 9.sp,
                                    fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTagActive) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (masteredCards == totalCards && totalCards > 0) SuccessSage
                            else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalCards cards • $masteredCards mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStudyClick() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Study Cards",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSolveClick() }
                            .testTag("timeline_deck_solve_${deck.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (deck.practiceProblems.isNotEmpty()) "Solve (${deck.practiceProblems.size})" else "Solve",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuizClick() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quiz",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Duplex PDF Print Action
                    val itemContext = LocalContext.current
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundFeedbackManager.getInstance(itemContext).playShutterSound()
                                FlashcardPdfExporter.exportDeckToPdf(itemContext, deck)?.let { file ->
                                    FlashcardPdfExporter.sharePdf(itemContext, file, deck.title)
                                }
                            }
                            .testTag("timeline_deck_export_pdf_${deck.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Export Duplex PDF",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "PDF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecksOverviewRow(
    deckCount: Int,
    cardCount: Int,
    masteredCount: Int,
    streakDays: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricPill(
                icon = Icons.Default.Style,
                title = "$cardCount Cards",
                subtitle = "$deckCount Decks"
            )
            MetricPill(
                icon = Icons.Default.CheckCircle,
                title = "$masteredCount Mastered",
                subtitle = if (cardCount > 0) "${(masteredCount * 100) / cardCount}%" else "0%"
            )
            MetricPill(
                icon = Icons.Default.LocalFireDepartment,
                title = "$streakDays Day",
                subtitle = "Streak"
            )
        }
    }
}

@Composable
private fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun CleanDeckItemCard(
    deck: FlashcardDeck,
    selectedTagFilter: String? = null,
    searchQuery: String = "",
    onTagClick: (String) -> Unit = {},
    onStudyClick: () -> Unit,
    onQuizClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSolveClick: () -> Unit = onStudyClick
) {
    val totalCards = deck.cards.size
    val masteredCards = deck.cards.count { it.isMastered }
    val progressRatio = if (totalCards > 0) masteredCards.toFloat() / totalCards.toFloat() else 0f

    val subjectIcon = when (deck.subject) {
        "Physics" -> "⚡"
        "Math" -> "📐"
        "Chemistry" -> "🧪"
        "Biology" -> "🧬"
        "History" -> "📜"
        else -> "📚"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "deck_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onStudyClick() }
            .testTag("deck_card_${deck.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Subject, Title, Cards count pill, Synced badge, and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$subjectIcon ${deck.subject}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${deck.cards.size}c",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    if (deck.isCloudSynced) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_deck_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Deck",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Deck Content Tags
            val deckTags = remember(deck) {
                deck.cards.mapNotNull { it.tag.trim().takeIf { t -> t.isNotBlank() } }.distinct()
            }
            if (deckTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    deckTags.take(4).forEach { tag ->
                        val isTagActive = selectedTagFilter.equals(tag, ignoreCase = true) ||
                                (searchQuery.isNotBlank() && tag.contains(searchQuery.trim().removePrefix("#"), ignoreCase = true))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isTagActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isTagActive) null
                                    else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable { onTagClick(tag) }
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 9.sp,
                                fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTagActive) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Compact Progress bar & stats in single line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (masteredCards == totalCards && totalCards > 0) SuccessSage
                            else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$masteredCards/$totalCards mastered",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (masteredCards == totalCards && totalCards > 0) SuccessSage
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStudyClick() }
                        .testTag("study_deck_button_${deck.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Study Cards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSolveClick() }
                        .testTag("solve_deck_button_${deck.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (deck.practiceProblems.isNotEmpty()) "Solve (${deck.practiceProblems.size})" else "Solve",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onQuizClick() }
                        .testTag("quiz_deck_button_${deck.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Take Quiz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Duplex PDF Print Action
                val itemContext = LocalContext.current
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            SoundFeedbackManager.getInstance(itemContext).playShutterSound()
                            FlashcardPdfExporter.exportDeckToPdf(itemContext, deck)?.let { file ->
                                FlashcardPdfExporter.sharePdf(itemContext, file, deck.title)
                            }
                        }
                        .testTag("deck_export_pdf_${deck.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Export Duplex PDF",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanEmptyDecksState(
    hasDecks: Boolean,
    isFiltered: Boolean = false,
    onClearFilter: (() -> Unit)? = null,
    onGoToCamera: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFiltered) Icons.Default.Search else Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = if (isFiltered) "No matching decks found"
                       else if (hasDecks) "No decks found in this view"
                       else "No flashcard decks yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isFiltered) "Try adjusting or clearing your title and content tag filters."
                       else if (hasDecks) "Change your subject or status filter to see other decks."
                       else "Open the camera to scan handwritten lecture notes or textbook pages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFiltered && onClearFilter != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onClearFilter() }
                        .testTag("clear_filter_empty_state_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear Search & Filters",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onGoToCamera() }
                        .testTag("empty_state_scan_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan Notes with Camera",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, low-profile search bar at the top of Decks tab:
 * Sleek 36dp pill layout with search input, expandable tag chip tray, inline count, and clear button.
 */
@Composable
private fun DecksTopSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    availableTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    matchingCount: Int,
    totalCount: Int,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagsStrip by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Single Sleek Compact Search Pill
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (searchQuery.isNotEmpty() || selectedTag != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(15.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (selectedTag != null) "#$selectedTag active — type to search..." else "Search decks or #tags...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decks_top_search_input")
                    )
                }

                // Inline Match Count Pill when active
                if (searchQuery.isNotEmpty() || selectedTag != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "$matchingCount / $totalCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Tag Filter Expand/Collapse Button
                if (availableTags.isNotEmpty()) {
                    val isTagActive = selectedTag != null
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTagActive || showTagsStrip) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { showTagsStrip = !showTagsStrip }
                            .testTag("toggle_tags_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Tags Filter",
                            tint = if (isTagActive || showTagsStrip) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Clear Button
                if (searchQuery.isNotEmpty() || selectedTag != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onClearAll() }
                            .testTag("clear_deck_search_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Expandable Tag Filter Strip
        AnimatedVisibility(visible = showTagsStrip || selectedTag != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Tags Chip
                val isAllSelected = selectedTag == null
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTagSelected(null) }
                        .testTag("tag_pill_all")
                ) {
                    Text(
                        text = "All Tags",
                        fontSize = 10.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                availableTags.forEach { tag ->
                    val isSelected = selectedTag.equals(tag, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTagSelected(tag) }
                            .testTag("tag_pill_$tag")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
