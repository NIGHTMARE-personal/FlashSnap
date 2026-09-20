package com.example.data.model

import android.graphics.Bitmap

data class Flashcard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val front: String,
    val back: String,
    val frontHindi: String = "",
    val backHindi: String = "",
    val keyTerm: String = "",
    val keyTermHindi: String = "",
    val tag: String = "Core Concept",
    val difficulty: String = "Medium",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0,
    val subject: String = "General",
    val sourcePageNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    val question: String get() = front
    val answer: String get() = back

    /**
     * Leitner 5-Box Spaced Repetition partition:
     * Box 1: Daily Blitz (0-1 reviews, not mastered)
     * Box 2: Every 3 Days (2-3 reviews)
     * Box 3: Weekly Review (4-5 reviews)
     * Box 4: Bi-Weekly (6-8 reviews)
     * Box 5: Mastered / Monthly (9+ reviews or marked mastered)
     */
    val leitnerBox: Int
        get() = when {
            isMastered || reviewCount >= 9 -> 5
            reviewCount >= 6 -> 4
            reviewCount >= 4 -> 3
            reviewCount >= 2 -> 2
            else -> 1
        }
}

data class LeitnerBoxStats(
    val boxIndex: Int,
    val title: String,
    val intervalLabel: String,
    val cardCount: Int,
    val iconName: String = "Style"
)

data class QuizRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val deckId: String,
    val deckTitle: String,
    val subject: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val netPoints: Int,
    val durationSeconds: Int = 0
) {
    val accuracyPercentage: Int
        get() = if (totalQuestions > 0) ((correctCount.toFloat() / totalQuestions) * 100).toInt() else 0

    val isPassing: Boolean
        get() = accuracyPercentage >= 70
}

enum class SubjectMasteryStatus {
    NEEDS_REVIEW, // < 70%
    DEVELOPING,   // 70% - 84%
    MASTERED      // >= 85%
}

data class SubjectPerformanceSummary(
    val subject: String,
    val totalQuizzes: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val averageAccuracy: Int,
    val netPoints: Int,
    val lastQuizTimestamp: Long,
    val masteryStatus: SubjectMasteryStatus,
    val weakTopicsSummary: String = "",
    val recommendedDeckId: String? = null,
    val recommendedDeckTitle: String? = null
) {
    val needsReview: Boolean
        get() = masteryStatus == SubjectMasteryStatus.NEEDS_REVIEW
}

data class QuizQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val questionHindi: String = "",
    val optionsHindi: List<String> = emptyList(),
    val explanationHindi: String = "",
    val xpValue: Int = 15,
    val penaltyXp: Int = 10
)

data class PracticeProblem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subject: String = "Physics",
    val problemStatement: String,
    val problemStatementHindi: String = "",
    val equationOrFormula: String = "",
    val diagramType: String = "DIAGRAM", // "CIRCUIT", "FORCE_VECTORS", "GEOMETRY", "REACTION", "GRAPH", "TEXT_DIAGRAM"
    val diagramContent: String = "",
    val givenData: List<String> = emptyList(),
    val whatToFind: String = "",
    val hint: String = "",
    val stepByStepSolution: List<String> = emptyList(),
    val finalAnswer: String = "",
    val difficulty: String = "Medium",
    val isSolved: Boolean = false,
    val userDraftAnswer: String = ""
)

data class FlashcardDeck(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pagesCount: Int = 1,
    val subject: String = "General",
    val cards: List<Flashcard> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val practiceProblems: List<PracticeProblem> = emptyList(),
    val isCloudSynced: Boolean = false,
    val extractedSummaryText: String = "",
    val rawScannedNoteContent: String = ""
)

data class StudyNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val subject: String = "General",
    val pagesCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val deckId: String? = null,
    val isCloudSynced: Boolean = false
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val iconEmoji: String = "🏆"
)

data class DailyStudyXp(
    val dayLabel: String, // "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    val dateLabel: String, // "Sep 14"
    val xpGained: Int,
    val cumulativeXp: Int,
    val isToday: Boolean = false,
    val quizzesCompleted: Int = 0
)

data class UserProfile(
    val name: String = "Student",
    val studyField: String = "",
    val studyGoal: String = "",
    val avatarEmoji: String = "🎓",
    val studySpaceWorkspaceId: String = "",
    val studySpaceSyncEnabled: Boolean = false,
    val websiteUrl: String = "",
    val xp: Int = 0,
    val streakDays: Int = 0,
    val totalQuizzesTaken: Int = 0,
    val totalCardsMastered: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val netQuizPoints: Int = 0,
    val totalStudyMinutes: Int = 0,
    val studyLanguage: String = "EN",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val typicalStudyTimeLabel: String = "8:00 PM",
    val cardFlipSpeedMs: Int = 380,
    val hapticFeedbackEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val autoAdvanceMastered: Boolean = true,
    val spacedRepetitionAlgo: String = "Leitner 3-Box",
    val negativeMarkingSeverity: Int = 10,
    val questionTimerSeconds: Int = 0,
    val formulaFontStyle: String = "JetBrains Mono",
    val dailyTargetCards: Int = 20,
    val highContrastCards: Boolean = false,
    val achievements: List<Achievement> = defaultAchievements()
) {
    val levelInfo: GameLevelInfo
        get() = GameLevelInfo.fromXp(xp)

    fun getWeeklyStudyProgress(): List<DailyStudyXp> {
        val calendar = java.util.Calendar.getInstance()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())

        val result = mutableListOf<DailyStudyXp>()
        if (xp <= 0) {
            for (i in 6 downTo 0) {
                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -i)
                }
                result.add(
                    DailyStudyXp(
                        dayLabel = dayFormat.format(cal.time),
                        dateLabel = dateFormat.format(cal.time),
                        xpGained = 0,
                        cumulativeXp = 0,
                        isToday = (i == 0),
                        quizzesCompleted = 0
                    )
                )
            }
            return result
        }

        val totalCurrentXp = xp
        val safeStreak = streakDays.coerceIn(0, 7)

        val weights = listOf(0.08f, 0.12f, 0.10f, 0.15f, 0.18f, 0.17f, 0.20f)
        var runningTotal = 0

        for (i in 6 downTo 0) {
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -i)
            }
            val dayName = dayFormat.format(cal.time)
            val dateStr = dateFormat.format(cal.time)
            val isToday = (i == 0)

            val dayWeightIndex = 6 - i
            val isActiveStreakDay = safeStreak > 0 && i < safeStreak
            val dailyXp = if (isActiveStreakDay) {
                (totalCurrentXp * weights[dayWeightIndex]).toInt()
            } else {
                0
            }
            runningTotal += dailyXp

            result.add(
                DailyStudyXp(
                    dayLabel = dayName,
                    dateLabel = dateStr,
                    xpGained = dailyXp,
                    cumulativeXp = runningTotal,
                    isToday = isToday,
                    quizzesCompleted = if (isActiveStreakDay) maxOf(1, totalQuizzesTaken / maxOf(1, safeStreak)) else 0
                )
            )
        }
        return result
    }

    companion object {
        fun defaultAchievements(): List<Achievement> = listOf(
            Achievement(
                id = "first_scan",
                title = "Handwriting Decoder",
                description = "Scan your first page of notes or textbook into flashcards",
                isUnlocked = false,
                unlockedAt = null,
                iconEmoji = "📸"
            ),
            Achievement(
                id = "quiz_rookie",
                title = "Quiz Challenger",
                description = "Complete a quiz with negative marking enabled",
                isUnlocked = false,
                iconEmoji = "🎯"
            ),
            Achievement(
                id = "streak_fire",
                title = "Streak Champion",
                description = "Maintain a 3-day study streak",
                isUnlocked = false,
                iconEmoji = "🔥"
            ),
            Achievement(
                id = "multi_page",
                title = "Bookworm Scribe",
                description = "Scan 2 or 3 pages at once into a comprehensive deck",
                isUnlocked = false,
                iconEmoji = "📚"
            ),
            Achievement(
                id = "scholar_level3",
                title = "Level 3 Achiever",
                description = "Earn enough XP to reach Level 3 Flashcard Scholar",
                isUnlocked = false,
                iconEmoji = "⭐"
            ),
            Achievement(
                id = "flawless_quiz",
                title = "Zero Penalties",
                description = "Finish a quiz without suffering any negative point deductions",
                isUnlocked = false,
                iconEmoji = "🛡️"
            )
        )
    }
}

data class GameLevelInfo(
    val level: Int,
    val title: String,
    val minXp: Int,
    val maxXp: Int,
    val currentXpInLevel: Int,
    val neededXpForLevel: Int,
    val progress: Float
) {
    companion object {
        fun fromXp(totalXp: Int): GameLevelInfo {
            val safeXp = maxOf(0, totalXp)
            val tiers = listOf(
                LevelTier(1, "Novice Scribe", 0, 150),
                LevelTier(2, "Curious Scholar", 150, 350),
                LevelTier(3, "Memory Apprentice", 350, 650),
                LevelTier(4, "Flashcard Adept", 650, 1050),
                LevelTier(5, "Quiz Master", 1050, 1600),
                LevelTier(6, "Grand Archon of Wisdom", 1600, 3000)
            )

            for (tier in tiers) {
                if (safeXp < tier.max) {
                    val inLevel = safeXp - tier.min
                    val span = tier.max - tier.min
                    val prog = (inLevel.toFloat() / span.toFloat()).coerceIn(0f, 1f)
                    return GameLevelInfo(
                        level = tier.level,
                        title = tier.title,
                        minXp = tier.min,
                        maxXp = tier.max,
                        currentXpInLevel = inLevel,
                        neededXpForLevel = span,
                        progress = prog
                    )
                }
            }

            // Max level reached
            return GameLevelInfo(
                level = 7,
                title = "Legendary Omniscient",
                minXp = 3000,
                maxXp = 10000,
                currentXpInLevel = safeXp - 3000,
                neededXpForLevel = 7000,
                progress = 1.0f
            )
        }
    }

    private data class LevelTier(val level: Int, val title: String, val min: Int, val max: Int)
}

data class CapturedPage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pageNumber: Int,
    val bitmap: Bitmap? = null,
    val noteSnippet: String = "",
    val extractedText: String = "",
    val autoDetectedSubject: String = "General",
    val manualLabel: String = "",
    val capturedAt: Long = System.currentTimeMillis()
) {
    val effectiveSubject: String
        get() = manualLabel.trim().ifBlank { autoDetectedSubject.trim().ifBlank { "General" } }

    val hasManualOverride: Boolean
        get() = manualLabel.trim().isNotBlank()

    val isManuallyLabeled: Boolean
        get() = hasManualOverride
}

object SubjectClassifier {
    val KNOWN_SUBJECTS = listOf("Physics", "Math", "Chemistry", "Biology", "History", "General")

    fun detectSubject(content: String): String {
        val lower = content.lowercase()

        val physicsScore = countOccurrences(lower, listOf(
            "physics", "newton", "velocity", "acceleration", "force", "inertia", "gravity",
            "momentum", "kinetic", "potential", "friction", "quantum", "thermodynamic", "ohm",
            "circuit", "current", "voltage", "optics", "lens", "refraction", "wave", "joule",
            "watt", "torque", "magnetism", "flux", "ke = ", "f = m", "p = m"
        ))

        val mathScore = countOccurrences(lower, listOf(
            "math", "calculus", "derivative", "integral", "limit", "algebra", "matrix", "vector",
            "equation", "polynomial", "trigonometry", "sine", "cosine", "tangent", "logarithm",
            "probability", "geometry", "theorem", "differentiation", "quadratic", "dx", "dy/dx",
            "summation", "graph", "hypotenuse"
        ))

        val chemistryScore = countOccurrences(lower, listOf(
            "chemistry", "reaction", "molecule", "acid", "base", "ph", "molar", "compound",
            "bond", "covalent", "ionic", "periodic", "valence", "organic", "polymer", "catalyst",
            "electron", "oxidation", "reduction", "stoichiometry", "titration", "solution",
            "solute", "solvent", "alkane", "enthalpy", "equilibrium", "h2o", "co2", "nacl"
        ))

        val biologyScore = countOccurrences(lower, listOf(
            "biology", "cell", "mitochondria", "dna", "rna", "protein", "organism", "respiration",
            "photosynthesis", "krebs", "genetics", "evolution", "bacteria", "virus", "enzyme",
            "chromosome", "chloroplast", "ribosome", "mitosis", "meiosis", "membrane", "glycolysis",
            "atp", "cytosol", "ecology", "ecosystem"
        ))

        val historyScore = countOccurrences(lower, listOf(
            "history", "war", "treaty", "revolution", "empire", "century", "ancient", "constitution",
            "monarchy", "parliament", "colonial", "civilization", "reign", "declaration"
        ))

        val scores = listOf(
            "Physics" to physicsScore,
            "Math" to mathScore,
            "Chemistry" to chemistryScore,
            "Biology" to biologyScore,
            "History" to historyScore
        )

        val best = scores.maxByOrNull { it.second }
        return if (best != null && best.second > 0) best.first else "General"
    }

    private fun countOccurrences(text: String, keywords: List<String>): Int {
        var count = 0
        for (kw in keywords) {
            if (text.contains(kw)) {
                count += 1
            }
        }
        return count
    }
}
