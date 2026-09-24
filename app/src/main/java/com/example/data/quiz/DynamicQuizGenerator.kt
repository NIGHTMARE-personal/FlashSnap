package com.example.data.quiz

import com.example.data.model.Flashcard
import com.example.data.model.QuizQuestion
import com.example.data.util.LanguageHelper

object DynamicQuizGenerator {

    /**
     * Dynamically generates multiple-choice quiz questions directly from flashcard content.
     * Guarantees 0 meta questions about page numbers and tests actual concepts.
     */
    fun generateFromFlashcards(
        cards: List<Flashcard>,
        subject: String = "General",
        targetCount: Int = 10
    ): List<QuizQuestion> {
        val validCards = cards.filter { card ->
            val front = card.front.trim()
            val back = card.back.trim()
            front.isNotBlank() && back.isNotBlank() &&
            !front.contains(Regex("(?i)page\\s*\\d+")) &&
            !back.contains(Regex("(?i)page\\s*\\d+"))
        }

        if (validCards.isEmpty()) {
            return emptyList()
        }

        val questions = mutableListOf<QuizQuestion>()
        val pool = validCards.shuffled()

        for ((index, card) in pool.withIndex()) {
            if (questions.size >= targetCount) break

            val otherCards = validCards.filter { it.id != card.id }.shuffled()
            val questionType = index % 3

            val question = when (questionType) {
                0 -> buildDefinitionQuestion(card, otherCards, index, subject)
                1 -> buildConceptPromptQuestion(card, otherCards, index, subject)
                else -> buildTermMatchingQuestion(card, otherCards, index, subject)
            }

            if (question != null) {
                questions.add(question)
            }
        }

        return questions
    }

    private fun buildDefinitionQuestion(
        card: Flashcard,
        otherCards: List<Flashcard>,
        index: Int,
        subject: String
    ): QuizQuestion {
        val cleanFront = cleanPromptText(card.front)
        val cleanBack = card.back.trim()
        val frontHi = if (card.frontHindi.isNotBlank()) cleanPromptText(card.frontHindi) else LanguageHelper.autoTranslateToHindi(cleanFront)
        val backHi = if (card.backHindi.isNotBlank()) card.backHindi.trim() else LanguageHelper.autoTranslateToHindi(cleanBack)

        val questionText = "Which statement correctly defines: \"$cleanFront\"?"
        val questionHindi = "निम्नलिखित में से कौन सा \"$frontHi\" को सही रूप से परिभाषित करता है?"

        val distractors = otherCards.map { it.back.trim() }
            .filter { it.isNotBlank() && it != cleanBack }
            .take(2)
            .toMutableList()

        while (distractors.size < 2) {
            val fallback = when (distractors.size) {
                0 -> "Opposite state or inverse condition of the principle"
                else -> "Unrelated auxiliary property in $subject"
            }
            distractors.add(fallback)
        }

        val optionsList = (distractors + cleanBack).shuffled()
        val correctIdx = optionsList.indexOf(cleanBack).coerceAtLeast(0)

        val optionsHi = optionsList.map { opt ->
            if (opt == cleanBack) backHi
            else LanguageHelper.autoTranslateToHindi(opt)
        }

        val xp = if (card.difficulty.equals("Hard", ignoreCase = true)) 20 else 15

        return QuizQuestion(
            id = "dyn_quiz_def_${card.id}_$index",
            question = questionText,
            questionHindi = questionHindi,
            options = optionsList,
            optionsHindi = optionsHi,
            correctIndex = correctIdx,
            explanation = "Core Concept: $cleanBack",
            explanationHindi = "मुख्य व्याख्या: $backHi",
            xpValue = xp,
            penaltyXp = 10
        )
    }

    private fun buildConceptPromptQuestion(
        card: Flashcard,
        otherCards: List<Flashcard>,
        index: Int,
        subject: String
    ): QuizQuestion {
        val cleanFront = cleanPromptText(card.front)
        val cleanBack = card.back.trim()
        val frontHi = if (card.frontHindi.isNotBlank()) cleanPromptText(card.frontHindi) else LanguageHelper.autoTranslateToHindi(cleanFront)
        val backHi = if (card.backHindi.isNotBlank()) card.backHindi.trim() else LanguageHelper.autoTranslateToHindi(cleanBack)

        val isDirectQuestion = cleanFront.endsWith("?") || cleanFront.startsWith("What", ignoreCase = true) ||
                cleanFront.startsWith("Which", ignoreCase = true) || cleanFront.startsWith("How", ignoreCase = true) ||
                cleanFront.startsWith("Define", ignoreCase = true) || cleanFront.startsWith("State", ignoreCase = true)

        val questionText = if (isDirectQuestion) {
            cleanFront
        } else {
            "In $subject, what is the core principle of: \"$cleanFront\"?"
        }

        val questionHindi = if (isDirectQuestion) {
            frontHi
        } else {
            "$subject में, \"$frontHi\" का मुख्य सिद्धांत क्या है?"
        }

        val distractors = otherCards.map { it.back.trim() }
            .filter { it.isNotBlank() && it != cleanBack }
            .take(2)
            .toMutableList()

        while (distractors.size < 2) {
            distractors.add("Alternative hypothetical outcome #${distractors.size + 1}")
        }

        val optionsList = (distractors + cleanBack).shuffled()
        val correctIdx = optionsList.indexOf(cleanBack).coerceAtLeast(0)

        val optionsHi = optionsList.map { opt ->
            if (opt == cleanBack) backHi
            else LanguageHelper.autoTranslateToHindi(opt)
        }

        val xp = if (card.difficulty.equals("Hard", ignoreCase = true)) 20 else 15

        return QuizQuestion(
            id = "dyn_quiz_prompt_${card.id}_$index",
            question = questionText,
            questionHindi = questionHindi,
            options = optionsList,
            optionsHindi = optionsHi,
            correctIndex = correctIdx,
            explanation = "Verified answer: $cleanBack",
            explanationHindi = "सत्यापित उत्तर: $backHi",
            xpValue = xp,
            penaltyXp = 10
        )
    }

    private fun buildTermMatchingQuestion(
        card: Flashcard,
        otherCards: List<Flashcard>,
        index: Int,
        subject: String
    ): QuizQuestion {
        val term = card.keyTerm.trim().ifBlank {
            val front = cleanPromptText(card.front)
            if (front.length < 40) front else front.take(35) + "..."
        }
        val termHi = card.keyTermHindi.trim().ifBlank { LanguageHelper.autoTranslateToHindi(term) }

        val snippet = if (card.back.length > 120) card.back.take(117) + "..." else card.back
        val snippetHi = if (card.backHindi.isNotBlank()) {
            if (card.backHindi.length > 120) card.backHindi.take(117) + "..." else card.backHindi
        } else {
            LanguageHelper.autoTranslateToHindi(snippet)
        }

        val questionText = "Which concept or term matches the description: \"$snippet\"?"
        val questionHindi = "कौन सी अवधारणा या पद इस विवरण से मेल खाता है: \"$snippetHi\"?"

        val otherTerms = otherCards.map {
            it.keyTerm.trim().ifBlank { cleanPromptText(it.front).take(35) }
        }.filter { it.isNotBlank() && !it.equals(term, ignoreCase = true) }
            .distinct()
            .take(2)
            .toMutableList()

        while (otherTerms.size < 2) {
            otherTerms.add("Related Topic Term #${otherTerms.size + 1}")
        }

        val optionsList = (otherTerms + term).shuffled()
        val correctIdx = optionsList.indexOf(term).coerceAtLeast(0)

        val optionsHi = optionsList.map { opt ->
            if (opt == term) termHi
            else LanguageHelper.autoTranslateToHindi(opt)
        }

        val xp = if (card.difficulty.equals("Hard", ignoreCase = true)) 20 else 15

        return QuizQuestion(
            id = "dyn_quiz_term_${card.id}_$index",
            question = questionText,
            questionHindi = questionHindi,
            options = optionsList,
            optionsHindi = optionsHi,
            correctIndex = correctIdx,
            explanation = "Concept: $term — ${card.back}",
            explanationHindi = "अवधारणा: $termHi — ${card.backHindi.ifBlank { card.back }}",
            xpValue = xp,
            penaltyXp = 10
        )
    }

    private fun cleanPromptText(text: String): String {
        return text.trim()
            .replace(Regex("(?i)page\\s*\\d+[:\\-\\s]*"), "")
            .replace(Regex("(?i)what(?:'s| is) in page\\s*\\d+\\??"), "What is the key principle here?")
            .replace(Regex("(?i)page\\s*\\d+"), "the notes")
            .trim()
            .ifBlank { "Core Concept" }
    }
}
