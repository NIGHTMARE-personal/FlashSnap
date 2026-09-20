package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.CloudSyncStatus
import com.example.ui.theme.SuccessSage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    syncStatus: CloudSyncStatus,
    syncAccountId: String,
    onUpdateProfile: (
        name: String,
        studyField: String,
        studyGoal: String,
        avatarEmoji: String,
        studySpaceWorkspaceId: String,
        studySpaceSyncEnabled: Boolean,
        websiteUrl: String
    ) -> Unit,
    onSyncWithStudySpace: (String, (Boolean, String) -> Unit) -> Unit,
    onImportFromStudySpace: (String, (Boolean, String) -> Unit) -> Unit,
    onSyncWithWebsite: ((String?, (Boolean, String) -> Unit) -> Unit)? = null,
    onUpdateStudySettings: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onResetMastery: () -> Unit = {},
    isReminderScheduled: Boolean = false,
    reminderTimeLabel: String = "8:00 PM",
    reminderStatus: String? = null,
    onSetReminder: ((Boolean, Int, Int, String) -> Unit)? = null,
    onSendTestReminder: (() -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSetReminder?.invoke(true, profile.reminderHour, profile.reminderMinute, reminderTimeLabel)
            Toast.makeText(context, "Notification permission granted! Reminder active.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var studyField by remember(profile.studyField) { mutableStateOf(profile.studyField) }
    var studyGoal by remember(profile.studyGoal) { mutableStateOf(profile.studyGoal) }
    var selectedEmoji by remember(profile.avatarEmoji) { mutableStateOf(profile.avatarEmoji) }
    var workspaceId by remember(profile.studySpaceWorkspaceId) { mutableStateOf(profile.studySpaceWorkspaceId) }
    var syncEnabled by remember(profile.studySpaceSyncEnabled) { mutableStateOf(profile.studySpaceSyncEnabled) }
    var websiteUrl by remember(profile.websiteUrl) { mutableStateOf(profile.websiteUrl) }
    var showWebsiteGuideDialog by remember { mutableStateOf(false) }

    // Enhanced Study Settings State
    var flipSpeedMs by remember(profile.cardFlipSpeedMs) { mutableStateOf(profile.cardFlipSpeedMs) }
    var hapticsEnabled by remember(profile.hapticFeedbackEnabled) { mutableStateOf(profile.hapticFeedbackEnabled) }
    var soundEffectsEnabled by remember(profile.soundEffectsEnabled) { mutableStateOf(profile.soundEffectsEnabled) }
    var autoAdvance by remember(profile.autoAdvanceMastered) { mutableStateOf(profile.autoAdvanceMastered) }
    var spacedRepetitionAlgo by remember(profile.spacedRepetitionAlgo) { mutableStateOf(profile.spacedRepetitionAlgo) }
    var negativeMarkingSeverity by remember(profile.negativeMarkingSeverity) { mutableStateOf(profile.negativeMarkingSeverity) }
    var questionTimerSeconds by remember(profile.questionTimerSeconds) { mutableStateOf(profile.questionTimerSeconds) }
    var formulaFontStyle by remember(profile.formulaFontStyle) { mutableStateOf(profile.formulaFontStyle) }
    var dailyTargetCards by remember(profile.dailyTargetCards) { mutableStateOf(profile.dailyTargetCards) }
    var highContrastCards by remember(profile.highContrastCards) { mutableStateOf(profile.highContrastCards) }
    var showResetDialog by remember { mutableStateOf(false) }

    var saveFeedback by remember { mutableStateOf<String?>(null) }
    var studySettingsFeedback by remember { mutableStateOf<String?>(null) }
    var syncActionFeedback by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    val emojis = listOf("🎓", "🔬", "📚", "💡", "⚡", "🌟", "✍️", "🩺", "🧠", "🎯")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Settings & Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize Profile • Linked with Study Space",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Website & Firebase Cloud Sync Hub
            WebsiteAndCloudHubCard(
                workspaceId = workspaceId,
                onWorkspaceIdChange = { workspaceId = it },
                websiteUrl = websiteUrl,
                onWebsiteUrlChange = { websiteUrl = it },
                syncEnabled = syncEnabled,
                onSyncEnabledChange = { syncEnabled = it },
                syncStatus = syncStatus,
                isOperating = isOperating,
                syncFeedback = syncActionFeedback,
                onSyncNow = {
                    isOperating = true
                    syncActionFeedback = "Syncing with website & Firebase Cloud..."
                    if (onSyncWithWebsite != null) {
                        onSyncWithWebsite(websiteUrl) { success, msg ->
                            isOperating = false
                            syncActionFeedback = msg
                            coroutineScope.launch {
                                delay(4000)
                                syncActionFeedback = null
                            }
                        }
                    } else {
                        onSyncWithStudySpace(workspaceId) { success, msg ->
                            isOperating = false
                            syncActionFeedback = msg
                            coroutineScope.launch {
                                delay(4000)
                                syncActionFeedback = null
                            }
                        }
                    }
                },
                onImportNow = {
                    isOperating = true
                    syncActionFeedback = "Checking website data on Firebase..."
                    onImportFromStudySpace(workspaceId) { success, msg ->
                        isOperating = false
                        syncActionFeedback = msg
                        coroutineScope.launch {
                            delay(4000)
                            syncActionFeedback = null
                        }
                    }
                },
                onOpenWebsite = {
                    try {
                        val validUrl = if (websiteUrl.startsWith("http://") || websiteUrl.startsWith("https://")) {
                            websiteUrl
                        } else {
                            "https://$websiteUrl"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onShowGuide = {
                    showWebsiteGuideDialog = true
                }
            )

            // Profile Adjustment Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("profile_adjust_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = selectedEmoji, fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "Adjust Profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Personalize your study identity",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Level ${profile.levelInfo.level}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Emoji Avatar Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Choose Avatar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(emojis) { emoji ->
                                val isSelected = emoji == selectedEmoji
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedEmoji = emoji }
                                        .testTag("avatar_emoji_$emoji"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Name input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    )

                    // Study Field input
                    OutlinedTextField(
                        value = studyField,
                        onValueChange = { studyField = it },
                        label = { Text("Field of Study / Major") },
                        placeholder = { Text("e.g. Biology, History, Computer Science") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_field_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    )

                    // Daily Study Goal input
                    OutlinedTextField(
                        value = studyGoal,
                        onValueChange = { studyGoal = it },
                        label = { Text("Daily Study Goal") },
                        placeholder = { Text("e.g. 10 cards per day") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_goal_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    )

                    // Save Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onUpdateProfile(
                                    name,
                                    studyField,
                                    studyGoal,
                                    selectedEmoji,
                                    workspaceId,
                                    syncEnabled,
                                    websiteUrl
                                )
                                saveFeedback = "Profile & Website connection saved!"
                                coroutineScope.launch {
                                    delay(3000)
                                    saveFeedback = null
                                }
                            }
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Profile Changes",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Save Feedback Message
                    AnimatedVisibility(visible = saveFeedback != null) {
                        saveFeedback?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Study Engine & Spaced Repetition Card
            StudyEnginePreferencesCard(
                flipSpeedMs = flipSpeedMs,
                onFlipSpeedChange = {
                    flipSpeedMs = it
                    onUpdateStudySettings(
                        it, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                    studySettingsFeedback = "Flip speed updated"
                    coroutineScope.launch { delay(2500); studySettingsFeedback = null }
                },
                hapticsEnabled = hapticsEnabled,
                onHapticsChange = {
                    hapticsEnabled = it
                    onUpdateStudySettings(
                        flipSpeedMs, it, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                },
                soundEffectsEnabled = soundEffectsEnabled,
                onSoundEffectsChange = {
                    soundEffectsEnabled = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, it, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                },
                autoAdvance = autoAdvance,
                onAutoAdvanceChange = {
                    autoAdvance = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, it,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                },
                spacedRepetitionAlgo = spacedRepetitionAlgo,
                onAlgoChange = {
                    spacedRepetitionAlgo = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        it, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                }
            )

            // 2. Quiz Arena Preferences Card
            QuizArenaPreferencesCard(
                negativeMarkingSeverity = negativeMarkingSeverity,
                onNegativeMarkingChange = {
                    negativeMarkingSeverity = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, it, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                },
                questionTimerSeconds = questionTimerSeconds,
                onQuestionTimerChange = {
                    questionTimerSeconds = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, it,
                        formulaFontStyle, dailyTargetCards, highContrastCards
                    )
                }
            )

            // 3. Daily Quiz Notification & Study Reminders Card
            if (onSetReminder != null) {
                DailyQuizReminderSettingsCard(
                    isScheduled = isReminderScheduled,
                    scheduledTimeLabel = reminderTimeLabel,
                    statusText = reminderStatus,
                    onToggleReminder = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onSetReminder(enabled, profile.reminderHour, profile.reminderMinute, reminderTimeLabel)
                        }
                    },
                    onSelectTime = { hour, minute, label ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onSetReminder(true, hour, minute, label)
                        Toast.makeText(context, "Reminder set for $label daily!", Toast.LENGTH_SHORT).show()
                    },
                    onTestNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val sent = onSendTestReminder?.invoke() ?: false
                        if (sent) {
                            Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Notification permission required", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 4. Card Aesthetics & Typography Card
            DisplayAestheticsPreferencesCard(
                formulaFontStyle = formulaFontStyle,
                onFormulaFontChange = {
                    formulaFontStyle = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        it, dailyTargetCards, highContrastCards
                    )
                },
                dailyTargetCards = dailyTargetCards,
                onDailyTargetChange = {
                    dailyTargetCards = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, it, highContrastCards
                    )
                },
                highContrastCards = highContrastCards,
                onHighContrastChange = {
                    highContrastCards = it
                    onUpdateStudySettings(
                        flipSpeedMs, hapticsEnabled, soundEffectsEnabled, autoAdvance,
                        spacedRepetitionAlgo, negativeMarkingSeverity, questionTimerSeconds,
                        formulaFontStyle, dailyTargetCards, it
                    )
                }
            )

            // 4. Data Management & Reset Card
            DataResetManagementCard(
                onResetClick = { showResetDialog = true }
            )

            // Feedback toast for study settings updates
            AnimatedVisibility(visible = studySettingsFeedback != null) {
                studySettingsFeedback?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Learning Stats Summary
            LearningStatsCard(profile = profile)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Alert for Resetting Deck Mastery
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Reset All Deck Mastery?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will mark all flashcards across your decks as unmastered (0 review count), allowing you to review your entire catalog fresh. Your total XP and study streak will NOT be lost.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetMastery()
                        saveFeedback = "All card mastery progress has been reset!"
                        coroutineScope.launch {
                            delay(3500)
                            saveFeedback = null
                        }
                    }
                ) {
                    Text(
                        text = "Reset Cards",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWebsiteGuideDialog) {
        WebsiteIntegrationGuideDialog(
            websiteUrl = websiteUrl,
            onDismiss = { showWebsiteGuideDialog = false }
        )
    }
}

@Composable
private fun WebsiteAndCloudHubCard(
    workspaceId: String,
    onWorkspaceIdChange: (String) -> Unit,
    websiteUrl: String,
    onWebsiteUrlChange: (String) -> Unit,
    syncEnabled: Boolean,
    onSyncEnabledChange: (Boolean) -> Unit,
    syncStatus: CloudSyncStatus,
    isOperating: Boolean,
    syncFeedback: String?,
    onSyncNow: () -> Unit,
    onImportNow: () -> Unit,
    onOpenWebsite: () -> Unit,
    onShowGuide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .testTag("website_cloud_hub_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Website & Cloud Sync Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "study-space-a6417 • Realtime DB & Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessSage)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Sync",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Dual-Storage Active: All flashcards and stats are stored both in permanent local internal disk files and synchronized to Firebase Cloud, ensuring zero data loss during app updates or offline use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }

            // Website URL input with open browser quick action
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = onWebsiteUrlChange,
                label = { Text("Your Website URL") },
                placeholder = { Text("https://your-website.com") },
                trailingIcon = {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onOpenWebsite() }
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open Website in Browser",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_url_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            )

            // Workspace ID field
            OutlinedTextField(
                value = workspaceId,
                onValueChange = onWorkspaceIdChange,
                label = { Text("Study Space Workspace / Account ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_space_workspace_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            )

            // Auto-sync switch row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Automatic Cloud Sync",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keep decks synchronized with website and Study Space",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Sync Buttons Row 1: Push to Web & Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isOperating) { onSyncNow() }
                        .testTag("sync_website_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOperating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Push to Website",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isOperating) { onImportNow() }
                        .testTag("import_website_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import Cards",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Sync Buttons Row 2: Open in Browser & Integration Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenWebsite() }
                        .testTag("open_website_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Website",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowGuide() }
                        .testTag("website_guide_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Website Code Guide",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            AnimatedVisibility(visible = syncFeedback != null) {
                syncFeedback?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
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
private fun WebsiteIntegrationGuideDialog(
    websiteUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val sampleHtmlSnippet = """
<!-- FlashSnap & Study Space Website Integration -->
<script type="module">
  import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
  import { getFirestore, collection, onSnapshot } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";
  import { getDatabase, ref, onValue } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

  const firebaseConfig = {
    projectId: "study-space-a6417",
    databaseURL: "https://study-space-a6417-default-rtdb.firebaseio.com",
    storageBucket: "study-space-a6417.firebasestorage.app",
    appId: "1:997715794034:android:39579454febe86012e8aeb"
  };

  const app = initializeApp(firebaseConfig);
  const db = getFirestore(app);
  const rtdb = getDatabase(app);

  // 1. Real-time Firestore sync:
  onSnapshot(collection(db, "flashcard_decks"), (snapshot) => {
    snapshot.forEach((doc) => {
      console.log("Deck from app:", doc.data().title);
    });
  });

  // 2. Realtime Database mirror:
  onValue(ref(rtdb, "website_sync/decks"), (snapshot) => {
    console.log("Website sync snapshot:", snapshot.val());
  });
</script>
""".trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Website Integration Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Your Android app is configured to sync with project study-space-a6417. You can embed and read your flashcards in any web application:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Firebase Project: study-space-a6417",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Realtime Database: https://study-space-a6417-default-rtdb.firebaseio.com",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "• Target Website: $websiteUrl",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Web Embed / JavaScript Snippet:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sampleHtmlSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(sampleHtmlSnippet))
                            Toast.makeText(context, "Website snippet copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Web Snippet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun LearningStatsCard(profile: UserProfile) {
    val levelInfo = profile.levelInfo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${profile.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rank: ${levelInfo.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${levelInfo.currentXpInLevel} / ${levelInfo.neededXpForLevel} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { levelInfo.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(title = "Streak", value = "${profile.streakDays} Days")
                StatItem(title = "Quizzes", value = "${profile.totalQuizzesTaken}")
                StatItem(title = "Mastered", value = "${profile.totalCardsMastered}")
                StatItem(title = "Net Score", value = "${profile.netQuizPoints} pts")
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Study Engine & Spaced Repetition configuration card
 */
@Composable
private fun StudyEnginePreferencesCard(
    flipSpeedMs: Int,
    onFlipSpeedChange: (Int) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    soundEffectsEnabled: Boolean,
    onSoundEffectsChange: (Boolean) -> Unit,
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    spacedRepetitionAlgo: String,
    onAlgoChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .testTag("study_engine_preferences_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Study Engine & Repetition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fine-tune 3D flips, pacing, and feedback",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card Flip Speed
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Card Flip Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("Snappy (250ms)" to 250, "Balanced (380ms)" to 380, "Smooth (600ms)" to 600),
                    selectedValue = flipSpeedMs,
                    onSelect = onFlipSpeedChange
                )
            }

            // Repetition Algorithm
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Spaced Repetition Schedule",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("Leitner 3-Box" to "Leitner 3-Box", "SM-2 Spaced" to "SM-2 Spaced"),
                    selectedValue = spacedRepetitionAlgo,
                    onSelect = onAlgoChange
                )
            }

            // Toggles
            SettingToggleRow(
                icon = Icons.Default.Vibration,
                title = "Haptic Tactile Feedback",
                subtitle = "Subtle vibration pulse on card flip & quiz submissions",
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChange,
                testTag = "toggle_haptics"
            )

            SettingToggleRow(
                icon = Icons.Default.VolumeUp,
                title = "Audio Chimes & Cues",
                subtitle = "Sound cue for level-up streaks and answer reveals",
                checked = soundEffectsEnabled,
                onCheckedChange = onSoundEffectsChange,
                testTag = "toggle_sounds"
            )

            SettingToggleRow(
                icon = Icons.Default.AutoAwesome,
                title = "Auto-Advance on Mastered",
                subtitle = "Progress to next card immediately after marking mastery",
                checked = autoAdvance,
                onCheckedChange = onAutoAdvanceChange,
                testTag = "toggle_auto_advance"
            )
        }
    }
}

/**
 * Quiz Arena & Penalty Rules configuration card
 */
@Composable
private fun QuizArenaPreferencesCard(
    negativeMarkingSeverity: Int,
    onNegativeMarkingChange: (Int) -> Unit,
    questionTimerSeconds: Int,
    onQuestionTimerChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .testTag("quiz_arena_preferences_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Quiz Arena & Scoring Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Adjust test pressure, timers, and penalties",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Negative Marking Severity
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Incorrect Answer Penalty",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("Lenient (-5 XP)" to 5, "Standard (-10 XP)" to 10, "Strict Exam (-20 XP)" to 20),
                    selectedValue = negativeMarkingSeverity,
                    onSelect = onNegativeMarkingChange
                )
            }

            // Question Timer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Timer Per Question",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("Zen (Untimed)" to 0, "45 Seconds" to 45, "Blitz (20s)" to 20),
                    selectedValue = questionTimerSeconds,
                    onSelect = onQuestionTimerChange
                )
            }
        }
    }
}

/**
 * Card Typography & Aesthetics configuration card
 */
@Composable
private fun DisplayAestheticsPreferencesCard(
    formulaFontStyle: String,
    onFormulaFontChange: (String) -> Unit,
    dailyTargetCards: Int,
    onDailyTargetChange: (Int) -> Unit,
    highContrastCards: Boolean,
    onHighContrastChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .testTag("display_aesthetics_preferences_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Typography & Layout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paper texture and notation typography",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Formula Font Style
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LaTeX & Code Notation Font",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("JetBrains Mono" to "JetBrains Mono", "Academic Serif" to "Academic Serif", "Clean Sans" to "Clean Sans"),
                    selectedValue = formulaFontStyle,
                    onSelect = onFormulaFontChange
                )
            }

            // Daily Target Cards
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Daily Cards Target",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingSegmentSelector(
                    options = listOf("10 Cards" to 10, "20 Cards" to 20, "30 Cards" to 30, "50 Cards" to 50),
                    selectedValue = dailyTargetCards,
                    onSelect = onDailyTargetChange
                )
            }

            // High Contrast Cards
            SettingToggleRow(
                icon = Icons.Default.Contrast,
                title = "High-Contrast Paper Surface",
                subtitle = "Pure optic white card faces for maximum readability in direct sunlight",
                checked = highContrastCards,
                onCheckedChange = onHighContrastChange,
                testTag = "toggle_high_contrast"
            )
        }
    }
}

/**
 * Data Management & Reset Card
 */
@Composable
private fun DataResetManagementCard(
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .testTag("data_management_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Catalog resets and card state",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset Card Mastery",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clears mastered stamps on all cards to review afresh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onResetClick() }
                        .testTag("reset_deck_mastery_button")
                ) {
                    Text(
                        text = "Reset Cards",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reusable Setting Toggle Row
 */
@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Reusable Segment Selector Chip Bar
 */
@Composable
private fun <T> SettingSegmentSelector(
    options: List<Pair<String, T>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = selectedValue == value
            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                              else androidx.compose.ui.graphics.Color.Transparent,
                label = "segment_bg_anim"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segment_text_anim"
            )

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = animatedBgColor,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelect(value) }
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = animatedTextColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyQuizReminderSettingsCard(
    isScheduled: Boolean,
    scheduledTimeLabel: String,
    statusText: String?,
    onToggleReminder: (Boolean) -> Unit,
    onSelectTime: (hour: Int, minute: Int, label: String) -> Unit,
    onTestNotification: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_daily_quiz_reminder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isScheduled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isScheduled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScheduled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Quiz Reminder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isScheduled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isScheduled) "ACTIVE" else "OFF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isScheduled) "Scheduled at $scheduledTimeLabel daily"
                            else "Receive a study prompt at your typical study time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isScheduled,
                    onCheckedChange = onToggleReminder,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("settings_quiz_reminder_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "TYPICAL STUDY TIME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            val timePresets = listOf(
                Triple(8, 30, "8:30 AM"),
                Triple(14, 0, "2:00 PM"),
                Triple(19, 30, "7:30 PM"),
                Triple(20, 0, "8:00 PM"),
                Triple(21, 30, "9:30 PM")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timePresets.forEach { (hour, minute, label) ->
                    val isCurrent = scheduledTimeLabel.startsWith(label)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTime(hour, minute, label) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTestNotification() }
                        .testTag("settings_test_notification_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test Notification",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• $statusText",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
