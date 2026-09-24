package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CapturedPage
import java.io.InputStream

enum class PageSortType(val displayName: String) {
    PAGE_NUMBER("Page #"),
    SUBJECT("Subject"),
    MANUAL_FIRST("Manual 🏷️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
    capturedPages: List<CapturedPage>,
    isAnalyzing: Boolean,
    analysisStepText: String,
    onAddPageBitmap: (Bitmap) -> Unit,
    onRemovePage: (Int) -> Unit,
    onUpdatePageManualLabel: (pageId: String, label: String) -> Unit,
    onUpdatePageExtractedText: (pageId: String, text: String) -> Unit,
    onAnalyze: (titleHint: String, notes: String, separateBySubject: Boolean) -> Unit,
    onGoToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var titleHint by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // Dialog state for editing a page's manual label
    var editingPageForLabel by remember { mutableStateOf<CapturedPage?>(null) }
    // Dialog state for inspecting/editing a page's extracted OCR text
    var viewingPageText by remember { mutableStateOf<CapturedPage?>(null) }

    // Sorting & Filtering State ("short it out / sort it out")
    var sortType by remember { mutableStateOf(PageSortType.PAGE_NUMBER) }
    var activeSubjectFilter by remember { mutableStateOf("All") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    onAddPageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Breakdown counts by effective subject
    val subjectCounts = remember(capturedPages) {
        capturedPages.groupingBy { it.effectiveSubject }.eachCount()
    }
    val distinctSubjectCount = subjectCounts.size

    // Sorted and filtered pages
    val displayedPages = remember(capturedPages, sortType, activeSubjectFilter) {
        val filtered = if (activeSubjectFilter == "All") {
            capturedPages
        } else {
            capturedPages.filter { it.effectiveSubject.equals(activeSubjectFilter, ignoreCase = true) }
        }
        when (sortType) {
            PageSortType.PAGE_NUMBER -> filtered.sortedBy { it.pageNumber }
            PageSortType.SUBJECT -> filtered.sortedBy { it.effectiveSubject }
            PageSortType.MANUAL_FIRST -> filtered.sortedWith(
                compareByDescending<CapturedPage> { it.isManuallyLabeled }.thenBy { it.pageNumber }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Study Pages & Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${capturedPages.size} of 20 pages captured",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact Filter & Sort Bar (Shortened and sorted out)
            if (capturedPages.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: Subject Filters & Sort Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter Pills: All + Subjects
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = activeSubjectFilter == "All",
                                    onClick = { activeSubjectFilter = "All" },
                                    label = { Text("All (${capturedPages.size})", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )

                                subjectCounts.forEach { (subject, count) ->
                                    val icon = when (subject) {
                                        "Physics" -> "⚡"
                                        "Math" -> "📐"
                                        "Chemistry" -> "🧪"
                                        "Biology" -> "🧬"
                                        else -> "📚"
                                    }
                                    FilterChip(
                                        selected = activeSubjectFilter.equals(subject, ignoreCase = true),
                                        onClick = {
                                            activeSubjectFilter = if (activeSubjectFilter.equals(subject, ignoreCase = true)) "All" else subject
                                        },
                                        label = { Text("$icon $subject ($count)", fontSize = 11.sp) },
                                        modifier = Modifier.height(28.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Sort Toggle Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        sortType = when (sortType) {
                                            PageSortType.PAGE_NUMBER -> PageSortType.SUBJECT
                                            PageSortType.SUBJECT -> PageSortType.MANUAL_FIRST
                                            PageSortType.MANUAL_FIRST -> PageSortType.PAGE_NUMBER
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sort,
                                        contentDescription = "Sort pages",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = sortType.displayName,
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

            // 2. Compact Page Cards Reel
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAGES (${displayedPages.size} shown)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    if (capturedPages.isNotEmpty()) {
                        Text(
                            text = "Tap label to override • OCR to edit text",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(displayedPages, key = { _, page -> page.id }) { _, page ->
                        val originalIndex = capturedPages.indexOfFirst { it.id == page.id }
                        CompactCapturedPageCard(
                            page = page,
                            onRemove = {
                                if (originalIndex >= 0) onRemovePage(originalIndex)
                            },
                            onEditLabel = { editingPageForLabel = page },
                            onViewText = { viewingPageText = page }
                        )
                    }

                    // Add Page Slot if under 20
                    if (capturedPages.size < 20) {
                        item {
                            CompactAddPageCard(
                                nextNumber = capturedPages.size + 1,
                                onCameraClick = onGoToCamera,
                                onGalleryClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. Deck Customization & Smart Generation
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Deck Customization & Flashcard Generation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = titleHint,
                            onValueChange = { titleHint = it },
                            label = { Text("Deck Title Hint (Optional)", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deck_title_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = additionalNotes,
                            onValueChange = { additionalNotes = it },
                            label = { Text("Additional Focus / Exam Notes (Optional)", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deck_notes_input"),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 1,
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Smart Generation: Separate into Subject Decks (if multiple subjects)
                        if (distinctSubjectCount > 1) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = capturedPages.isNotEmpty() && !isAnalyzing) {
                                        onAnalyze(titleHint, additionalNotes, true)
                                    }
                                    .testTag("generate_separated_decks_button"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (capturedPages.isNotEmpty() && !isAnalyzing)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Layers,
                                        contentDescription = null,
                                        tint = if (capturedPages.isNotEmpty() && !isAnalyzing) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Separate into $distinctSubjectCount Subject Decks (Physics, Math, Chem)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (capturedPages.isNotEmpty() && !isAnalyzing) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Combined Deck Generation Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = capturedPages.isNotEmpty() && !isAnalyzing) {
                                    onAnalyze(titleHint, additionalNotes, false)
                                }
                                .testTag("generate_flashcards_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (distinctSubjectCount > 1)
                                MaterialTheme.colorScheme.surfaceVariant
                            else if (capturedPages.isNotEmpty() && !isAnalyzing)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Synthesizing with Gemini AI...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (distinctSubjectCount <= 1 && capturedPages.isNotEmpty()) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (capturedPages.isEmpty()) "Scan or Add a Page First"
                                               else if (distinctSubjectCount > 1) "Or Generate 1 Combined Deck (${capturedPages.size} pages)"
                                               else "Generate Flashcard Deck (${capturedPages.size} pages)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (distinctSubjectCount <= 1 && capturedPages.isNotEmpty()) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Manual Subject Label (Override Auto-detector)
    editingPageForLabel?.let { page ->
        var customLabelInput by remember { mutableStateOf(page.manualLabel) }
        val currentEffective = page.effectiveSubject

        AlertDialog(
            onDismissRequest = { editingPageForLabel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Label Override - Page ${page.pageNumber}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Auto-detected: \"${page.autoDetectedSubject}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Manual label takes precedence over auto-detection:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Physics", "Math", "Chemistry", "Biology", "History").forEach { subj ->
                            FilterChip(
                                selected = customLabelInput == subj || (customLabelInput.isBlank() && currentEffective == subj),
                                onClick = { customLabelInput = subj },
                                label = { Text(subj, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customLabelInput,
                        onValueChange = { customLabelInput = it },
                        label = { Text("Custom Subject Label") },
                        placeholder = { Text("e.g. Organic Chemistry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePageManualLabel(page.id, customLabelInput)
                        editingPageForLabel = null
                    }
                ) {
                    Text("Apply Label")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onUpdatePageManualLabel(page.id, "")
                        editingPageForLabel = null
                    }
                ) {
                    Text("Reset to Auto")
                }
            }
        )
    }

    // Modal: View / Edit Extracted Text from Image
    viewingPageText?.let { page ->
        var editedText by remember { mutableStateOf(page.extractedText.ifBlank { page.noteSnippet }) }

        AlertDialog(
            onDismissRequest = { viewingPageText = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extracted Text - Page ${page.pageNumber}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Text transcribed via Google ML Kit on-device OCR (+ Gemini AI):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePageExtractedText(page.id, editedText)
                        viewingPageText = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingPageText = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Compact Captured Page Card (130dp x 175dp) - Sleek, short, and organized
 */
@Composable
private fun CompactCapturedPageCard(
    page: CapturedPage,
    onRemove: () -> Unit,
    onEditLabel: () -> Unit,
    onViewText: () -> Unit
) {
    val subject = page.effectiveSubject
    val isManual = page.isManuallyLabeled
    val subjectIcon = when (subject) {
        "Physics" -> "⚡"
        "Math" -> "📐"
        "Chemistry" -> "🧪"
        "Biology" -> "🧬"
        else -> "📚"
    }

    Card(
        modifier = Modifier
            .width(132.dp)
            .height(178.dp)
            .border(
                1.dp,
                if (isManual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            page.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${page.pageNumber}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDE7DC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Top Left: Subject Chip (tap to edit manual label)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isManual) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                        else Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onEditLabel() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$subjectIcon $subject",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (isManual) Icons.Filled.Edit else Icons.Filled.Label,
                        contentDescription = "Edit label",
                        tint = if (isManual) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }

            // Top Right: Remove Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove page",
                    tint = Color.White,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(2.dp)
                )
            }

            // Bottom Overlay: Page # & OCR Text button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${page.pageNumber}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onViewText() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TextFields,
                            contentDescription = "View OCR Text",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "OCR",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Add Page Slot Card
 */
@Composable
private fun CompactAddPageCard(
    nextNumber: Int,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .height(178.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$nextNumber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCameraClick() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Camera",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGalleryClick() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Gallery",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
