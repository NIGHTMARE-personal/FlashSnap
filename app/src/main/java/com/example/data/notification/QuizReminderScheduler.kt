package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class QuizReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "QuizReminderScheduler"
        const val CHANNEL_ID = "daily_quiz_reminder_channel"
        const val NOTIFICATION_ID = 2024
        const val EXTRA_START_QUIZ = "extra_start_quiz"

        @Volatile
        private var instance: QuizReminderScheduler? = null

        fun getInstance(context: Context): QuizReminderScheduler {
            return instance ?: synchronized(this) {
                instance ?: QuizReminderScheduler(context.applicationContext).also { instance = it }
            }
        }
    }

    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reminderJob: Job? = null

    private val _isScheduled = MutableStateFlow(false)
    val isScheduled: StateFlow<Boolean> = _isScheduled.asStateFlow()

    private val _scheduledTimeLabel = MutableStateFlow("8:00 PM")
    val scheduledTimeLabel: StateFlow<String> = _scheduledTimeLabel.asStateFlow()

    private val _lastNotificationStatus = MutableStateFlow<String?>(null)
    val lastNotificationStatus: StateFlow<String?> = _lastNotificationStatus.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Quiz Reminders"
            val descriptionText = "Reminds you to complete your daily retention quiz based on your typical study time."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Schedules a daily quiz reminder using Kotlin Coroutines.
     * Computes the initial delay until the target study hour/minute, delays,
     * fires notification, and repeats every 24 hours.
     */
    fun scheduleDailyReminder(hour: Int = 20, minute: Int = 0, timeLabel: String = "8:00 PM") {
        reminderJob?.cancel()

        _scheduledTimeLabel.value = timeLabel
        _isScheduled.value = true

        reminderJob = schedulerScope.launch {
            try {
                val initialDelayMs = calculateDelayUntil(hour, minute)
                val hoursUntil = initialDelayMs / (1000 * 60 * 60)
                val minutesUntil = (initialDelayMs / (1000 * 60)) % 60
                Log.d(TAG, "Scheduled quiz reminder in ${hoursUntil}h ${minutesUntil}m for $timeLabel")
                _lastNotificationStatus.value = "Active: Next reminder in ${hoursUntil}h ${minutesUntil}m ($timeLabel)"

                // Coroutine wait for target study time
                delay(initialDelayMs)

                while (isActive) {
                    postReminderNotification(
                        title = "🎯 Daily Quiz Reminder",
                        body = "It's your typical study time ($timeLabel)! Complete today's quiz to earn +15 XP and keep your streak alive 🔥"
                    )

                    // Daily repetition loop
                    delay(24 * 60 * 60 * 1000L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reminder coroutine cancelled or error: ${e.message}")
            }
        }
    }

    fun cancelReminder() {
        reminderJob?.cancel()
        reminderJob = null
        _isScheduled.value = false
        _lastNotificationStatus.value = "Reminders paused"
        Log.d(TAG, "Quiz reminder cancelled")
    }

    /**
     * Immediately triggers a test reminder so users can verify notification delivery.
     */
    fun sendTestReminderNotification(typicalTime: String = "8:00 PM"): Boolean {
        return postReminderNotification(
            title = "🎯 Daily Quiz Reminder (Test)",
            body = "Great timing! Your daily quiz is ready ($typicalTime study routine). Tap to practice and gain +15 XP with negative marking!"
        )
    }

    private fun postReminderNotification(title: String, body: String): Boolean {
        if (!hasNotificationPermission()) {
            _lastNotificationStatus.value = "Permission required to post notifications"
            Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
            return false
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_QUIZ, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID, notification)
            _lastNotificationStatus.value = "Notification sent successfully at ${getCurrentTimeFormatted()}"
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification: ${e.message}")
            _lastNotificationStatus.value = "Notification blocked by system permissions"
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification: ${e.message}")
            _lastNotificationStatus.value = "Error: ${e.message}"
            return false
        }
    }

    private fun calculateDelayUntil(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If target time already passed today, schedule for tomorrow
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun getCurrentTimeFormatted(): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    /**
     * Compute typical study time from activity timestamps or hour presets
     */
    fun computeTypicalStudyTime(studyHours: List<Int>): Pair<Int, String> {
        if (studyHours.isEmpty()) {
            return Pair(20, "8:00 PM") // Default evening study time
        }
        val mostFrequentHour = studyHours.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 20
        val label = when (mostFrequentHour) {
            in 6..11 -> "$mostFrequentHour:00 AM"
            12 -> "12:00 PM"
            in 13..23 -> "${mostFrequentHour - 12}:00 PM"
            else -> "12:00 AM"
        }
        return Pair(mostFrequentHour, label)
    }
}
