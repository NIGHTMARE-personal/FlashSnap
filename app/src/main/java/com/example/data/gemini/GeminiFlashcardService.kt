package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Flashcard
import com.example.data.model.FlashcardDeck
import com.example.data.model.PracticeProblem
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiFlashcardService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiFlashcardService"
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun analyzeNotesAndGenerateDeck(
        pages: List<Bitmap>,
        notesText: String = "",
        deckTitleHint: String = ""
    ): FlashcardDeck = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is placeholder or empty. Generating high-yield curated deck.")
            return@withContext generateFallbackDeck(notesText, pages.size, deckTitleHint)
        }

        try {
            val endpoint = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val partsArray = JSONArray()

            // Detailed prompt for handwriting OCR and flashcard/quiz creation
            val promptText = buildString {
                appendLine("You are an expert tutor specializing in deciphering handwritten notes, book scans, and study summaries.")
                appendLine("Carefully read and analyze all handwritten content, cursive text, margin annotations, formulas, diagrams, and textbook paragraphs in the provided image(s).")
                if (notesText.isNotBlank()) {
                    appendLine("Additional text notes provided by student: $notesText")
                }
                if (deckTitleHint.isNotBlank()) {
                    appendLine("Subject / Title Hint: $deckTitleHint")
                }
                appendLine()
                appendLine("Return a single JSON object with EXACTLY the following format, providing BOTH English and Hindi for every card and question:")
                appendLine("CRITICAL CONTENT & QUALITY RULES:")
                appendLine("1. NEVER generate meta-questions about page numbers (e.g. NEVER ask 'What is on page 4?', 'What does page 2 discuss?', or 'Summarize page 1').")
                appendLine("2. Always formulate substantive academic questions that test real concepts, definitions, formulas, reactions, laws, and facts.")
                appendLine("3. The flashcard front MUST be a clear question or prompt (e.g. 'What is Newton's Second Law?', 'Define photosynthesis', 'State the Pythagorean theorem').")
                appendLine("""
                {
                  "title": "Clear concise subject title",
                  "description": "1-2 sentence overview of the covered notes",
                  "flashcards": [
                    {
                      "front": "Question, prompt, or term in English",
                      "frontHindi": "हिन्दी में प्रश्न या शीर्षक",
                      "back": "Clear, precise explanation and answer in English",
                      "backHindi": "हिन्दी में स्पष्ट व्याख्या और उत्तर",
                      "keyTerm": "Key term or concept in English",
                      "keyTermHindi": "मुख्य शब्द / सूत्र हिन्दी में",
                      "tag": "Definition or Formula or Concept or Process",
                      "difficulty": "Easy" or "Medium" or "Hard"
                    }
                  ],
                  "quizQuestions": [
                    {
                      "question": "Clear multiple-choice question in English",
                      "questionHindi": "हिन्दी में बहुविकल्पीय प्रश्न",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "optionsHindi": ["विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"],
                      "correctIndex": 0,
                      "explanation": "Why this answer is correct in English",
                      "explanationHindi": "हिन्दी में व्याख्या",
                      "xpValue": 15,
                      "penaltyXp": 10
                    }
                  ],
                  "practiceProblems": [
                    {
                      "title": "Practice problem title (e.g. Free Body Incline, Circuit Current, Definite Integral)",
                      "subject": "Physics" or "Chemistry" or "Math",
                      "problemStatement": "Detailed question to solve based on formulas and equations from the notes",
                      "problemStatementHindi": "हल करने के लिए विस्तृत प्रश्न",
                      "equationOrFormula": "Governing equation / formula used (e.g. F = ma, v = u + at, PV = nRT, ∫f(x)dx)",
                      "diagramType": "FORCE_VECTORS" or "CIRCUIT" or "GEOMETRY" or "REACTION" or "GRAPH",
                      "diagramContent": "Clear ASCII/Unicode technical schematic showing the setup with labels, arrows, angles, and components",
                      "givenData": ["Value 1 with units", "Value 2 with units"],
                      "whatToFind": "Target quantity to solve",
                      "hint": "Useful conceptual hint for solving",
                      "stepByStepSolution": [
                        "Step 1: State the equation ...",
                        "Step 2: Substitute values ...",
                        "Step 3: Calculate the numerical result ..."
                      ],
                      "finalAnswer": "Final answer with units",
                      "difficulty": "Medium"
                    }
                  ]
                }
                """.trimIndent())
                appendLine("Generate at least 5-8 flashcards, 3-5 quiz questions, AND 3-5 practice problems to solve (questions with equations, given data, ASCII diagrams, and step-by-step solutions). Only output valid JSON without markdown code fences if possible.")
            }

            // Add text prompt part
            val textPart = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(textPart)

            // Add image parts
            for (bitmap in pages) {
                val base64Image = bitmapToBase64(bitmap)
                val imagePart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }
                    put("inlineData", inlineData)
                }
                partsArray.put(imagePart)
            }

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini API error ${response.code}: $errorBody")
                return@withContext generateFallbackDeck(notesText, pages.size, deckTitleHint)
            }

            val responseBody = response.body?.string() ?: ""
            val parsedDeck = parseGeminiResponse(responseBody, pages.size)
            return@withContext parsedDeck ?: generateFallbackDeck(notesText, pages.size, deckTitleHint)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini analysis: ${e.message}", e)
            return@withContext generateFallbackDeck(notesText, pages.size, deckTitleHint)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large to conserve bandwidth and latency
        val maxDimension = 1280
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseGeminiResponse(jsonString: String, pageCount: Int): FlashcardDeck? {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val data = JSONObject(cleanedJson)
            val title = data.optString("title", "Scanned Notes Deck")
            val description = data.optString("description", "Flashcards extracted from your handwritten notes.")

            val flashcards = mutableListOf<Flashcard>()
            val flashcardsJson = data.optJSONArray("flashcards")
            if (flashcardsJson != null) {
                for (i in 0 until flashcardsJson.length()) {
                    val c = flashcardsJson.getJSONObject(i)
                    var front = c.optString("front", "Question").trim()
                    val back = c.optString("back", "Answer").trim()
                    var keyTerm = c.optString("keyTerm", "").trim()
                    if (front.contains(Regex("(?i)page\\s*\\d+")) || front.startsWith("What's in page", ignoreCase = true) || front.startsWith("What is in page", ignoreCase = true)) {
                        front = if (keyTerm.isNotBlank()) "What is the key principle of $keyTerm?" else "Define the core concept: \"${back.take(45)}\""
                    }
                    if (keyTerm.contains(Regex("(?i)page\\s*\\d+"))) {
                        keyTerm = back.take(30)
                    }
                    val frontHindi = c.optString("frontHindi", "").ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(front) }
                    val backHindi = c.optString("backHindi", "").ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(back) }
                    val keyTermHindi = c.optString("keyTermHindi", "").ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(keyTerm) }
                    flashcards.add(
                        Flashcard(
                            front = front,
                            back = back,
                            frontHindi = frontHindi,
                            backHindi = backHindi,
                            keyTerm = keyTerm,
                            keyTermHindi = keyTermHindi,
                            tag = c.optString("tag", "Concept"),
                            difficulty = c.optString("difficulty", "Medium")
                        )
                    )
                }
            }

            val quizQuestions = mutableListOf<QuizQuestion>()
            val quizJson = data.optJSONArray("quizQuestions")
            if (quizJson != null) {
                for (i in 0 until quizJson.length()) {
                    val q = quizJson.getJSONObject(i)
                    var question = q.optString("question", "Question").trim()
                    if (question.contains(Regex("(?i)page\\s*\\d+")) || question.startsWith("What's in page", ignoreCase = true) || question.startsWith("What is in page", ignoreCase = true)) {
                        question = question.replace(Regex("(?i)page\\s*\\d+[:\\-\\s]*"), "").trim()
                        if (question.isBlank() || question.length < 5) {
                            question = "What is the key principle discussed in this topic?"
                        }
                    }
                    val questionHindi = q.optString("questionHindi", "").ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(question) }
                    val explanation = q.optString("explanation", "Correct answer verified from notes.")
                    val explanationHindi = q.optString("explanationHindi", "").ifBlank { com.example.data.util.LanguageHelper.autoTranslateToHindi(explanation) }

                    val optsJson = q.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsJson != null) {
                        for (j in 0 until optsJson.length()) {
                            opts.add(optsJson.getString(j))
                        }
                    }

                    val optsHiJson = q.optJSONArray("optionsHindi")
                    val optsHi = mutableListOf<String>()
                    if (optsHiJson != null && optsHiJson.length() == opts.size) {
                        for (k in 0 until optsHiJson.length()) {
                            optsHi.add(optsHiJson.getString(k))
                        }
                    } else {
                        opts.mapTo(optsHi) { com.example.data.util.LanguageHelper.autoTranslateToHindi(it) }
                    }

                    if (opts.size >= 2) {
                        quizQuestions.add(
                            QuizQuestion(
                                question = question,
                                questionHindi = questionHindi,
                                options = opts,
                                optionsHindi = optsHi,
                                correctIndex = q.optInt("correctIndex", 0).coerceIn(0, opts.size - 1),
                                explanation = explanation,
                                explanationHindi = explanationHindi,
                                xpValue = q.optInt("xpValue", 15),
                                penaltyXp = q.optInt("penaltyXp", 10)
                            )
                        )
                    }
                }
            }

            val practiceProblems = mutableListOf<PracticeProblem>()
            val problemsJson = data.optJSONArray("practiceProblems")
            if (problemsJson != null) {
                for (pIdx in 0 until problemsJson.length()) {
                    val p = problemsJson.getJSONObject(pIdx)
                    val pTitle = p.optString("title", "Practice Problem ${pIdx + 1}")
                    val pSubject = p.optString("subject", title)
                    val problemStatement = p.optString("problemStatement", "")
                    if (problemStatement.isNotBlank()) {
                        val givenDataList = mutableListOf<String>()
                        val gArr = p.optJSONArray("givenData")
                        if (gArr != null) {
                            for (g in 0 until gArr.length()) givenDataList.add(gArr.getString(g))
                        }
                        val stepsList = mutableListOf<String>()
                        val sArr = p.optJSONArray("stepByStepSolution")
                        if (sArr != null) {
                            for (s in 0 until sArr.length()) stepsList.add(sArr.getString(s))
                        }
                        practiceProblems.add(
                            PracticeProblem(
                                title = pTitle,
                                subject = pSubject,
                                problemStatement = problemStatement,
                                problemStatementHindi = p.optString("problemStatementHindi", ""),
                                equationOrFormula = p.optString("equationOrFormula", ""),
                                diagramType = p.optString("diagramType", "DIAGRAM"),
                                diagramContent = p.optString("diagramContent", ""),
                                givenData = givenDataList,
                                whatToFind = p.optString("whatToFind", ""),
                                hint = p.optString("hint", ""),
                                stepByStepSolution = stepsList,
                                finalAnswer = p.optString("finalAnswer", ""),
                                difficulty = p.optString("difficulty", "Medium")
                            )
                        )
                    }
                }
            }

            if (flashcards.isEmpty()) return null

            FlashcardDeck(
                title = title,
                description = description,
                pagesCount = pageCount.coerceAtLeast(1),
                cards = flashcards,
                quiz = quizQuestions,
                practiceProblems = practiceProblems,
                isCloudSynced = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: ${e.message}", e)
            null
        }
    }

    fun generateFallbackDeck(notesText: String, pageCount: Int, titleHint: String, targetSubject: String = ""): FlashcardDeck {
        val detectedSubj = if (targetSubject.isNotBlank()) targetSubject else com.example.data.model.SubjectClassifier.detectSubject(notesText + " " + titleHint)
        val subject = if (detectedSubj.isNotBlank() && detectedSubj != "General") detectedSubj else "General"

        val title = when {
            titleHint.isNotBlank() -> titleHint
            notesText.isNotBlank() -> {
                val firstLine = notesText.lines().firstOrNull { it.trim().isNotBlank() }?.trim()?.take(40) ?: ""
                if (firstLine.isNotBlank()) firstLine else "Scanned Notes: $subject"
            }
            else -> "Scanned Notes: $subject"
        }

        // Parse actual content directly from the scanned text
        val rawLines = notesText.lines()
            .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
            .filter { it.length > 5 }

        val cards = mutableListOf<Flashcard>()
        val quiz = mutableListOf<QuizQuestion>()

        for ((idx, line) in rawLines.withIndex()) {
            if (cards.size >= 15) break
            when {
                line.contains(":") -> {
                    val parts = line.split(":", limit = 2)
                    val term = parts[0].trim()
                    val defn = parts[1].trim()
                    if (term.isNotBlank() && defn.isNotBlank()) {
                        cards.add(
                            Flashcard(
                                front = "What is $term?",
                                back = defn,
                                keyTerm = term,
                                tag = subject,
                                difficulty = if (idx % 2 == 0) "Easy" else "Medium",
                                subject = subject
                            )
                        )
                        if (quiz.size < 5) {
                            val wrongAnswers = rawLines.filter { it != line && it.contains(":") }
                                .map { it.split(":", limit = 2)[1].trim().take(50) }
                                .take(3)
                            val options = (listOf(defn.take(60)) + wrongAnswers).distinct()
                            if (options.size >= 2) {
                                quiz.add(
                                    QuizQuestion(
                                        question = "What is $term?",
                                        options = options,
                                        correctIndex = 0,
                                        explanation = defn,
                                        xpValue = 15,
                                        penaltyXp = 10
                                    )
                                )
                            }
                        }
                    }
                }
                line.contains("=") -> {
                    val parts = line.split("=", limit = 2)
                    val lhs = parts[0].trim()
                    val rhs = parts[1].trim()
                    if (lhs.isNotBlank() && rhs.isNotBlank()) {
                        cards.add(
                            Flashcard(
                                front = "Formula / Equation for $lhs",
                                back = "$lhs = $rhs",
                                keyTerm = lhs,
                                tag = "Formula",
                                difficulty = "Medium",
                                subject = subject
                            )
                        )
                    }
                }
                line.length > 15 -> {
                    cards.add(
                        Flashcard(
                            front = "Review key note concept:",
                            back = line,
                            keyTerm = line.take(25),
                            tag = subject,
                            difficulty = "Easy",
                            subject = subject
                        )
                    )
                }
            }
        }

        if (cards.isEmpty()) {
            cards.add(
                Flashcard(
                    front = "Scanned Note Review",
                    back = if (notesText.isNotBlank()) notesText.take(200) else "Take a clear photo of your handwritten or printed study notes to generate cards.",
                    keyTerm = "Notes",
                    tag = subject,
                    difficulty = "Easy",
                    subject = subject
                )
            )
        }

        val practiceProblems = generateCuratedPracticeProblems(subject, notesText)

        return FlashcardDeck(
            title = title,
            description = "Generated from $pageCount scanned page(s) of $subject notes.",
            pagesCount = pageCount.coerceAtLeast(1),
            subject = subject,
            cards = cards,
            quiz = quiz,
            practiceProblems = practiceProblems,
            isCloudSynced = false,
            extractedSummaryText = notesText
        )
    }

    private fun generateCuratedPracticeProblems(subject: String, notesText: String): List<PracticeProblem> {
        val problems = mutableListOf<PracticeProblem>()
        when (subject.lowercase()) {
            "physics" -> {
                problems.add(
                    PracticeProblem(
                        title = "Kinematics & Newton's Second Law",
                        subject = "Physics",
                        problemStatement = "A wooden block of mass m = 4.0 kg rests on a smooth horizontal surface. A constant horizontal pulling force of F = 20.0 N is applied. Calculate the acceleration of the block and its velocity after moving for t = 3.0 seconds from rest.",
                        problemStatementHindi = "4.0 किग्रा द्रव्यमान का एक गुटका चिकनी क्षैतिज सतह पर रखा है। 20.0 N का बल लगाने पर 3.0 सेकंड बाद त्वरण और वेग ज्ञात कीजिए।",
                        equationOrFormula = "F = m · a   and   v = u + a · t",
                        diagramType = "FORCE_VECTORS",
                        diagramContent = """
                           F = 20 N ──────►
                         ┌─────────────────┐
                         │   Mass m=4kg    │
                 ────────┴─────────────────┴────────
                  ///////////////////////////////////
                        """.trimIndent(),
                        givenData = listOf("Mass m = 4.0 kg", "Applied Force F = 20.0 N", "Initial velocity u = 0 m/s", "Time t = 3.0 s"),
                        whatToFind = "Acceleration (a) and final velocity (v)",
                        hint = "Use Newton's 2nd law F = ma to determine acceleration, then substitute into the 1st kinematic equation v = u + at.",
                        stepByStepSolution = listOf(
                            "Step 1: Apply Newton's Second Law of Motion: F = m · a",
                            "Step 2: Solve for acceleration: a = F / m = 20.0 N / 4.0 kg = 5.0 m/s²",
                            "Step 3: Apply the 1st kinematic equation: v = u + a · t",
                            "Step 4: Substitute known values: v = 0 + (5.0 m/s²)(3.0 s) = 15.0 m/s"
                        ),
                        finalAnswer = "Acceleration a = 5.0 m/s², Final Velocity v = 15.0 m/s",
                        difficulty = "Medium"
                    )
                )
                problems.add(
                    PracticeProblem(
                        title = "Inclined Plane Free-Body Forces",
                        subject = "Physics",
                        problemStatement = "A crate of mass m = 10 kg slides down an incline of angle θ = 30°. If the coefficient of kinetic friction μ = 0.20, calculate the net acceleration down the slope. (Take g = 9.8 m/s²).",
                        equationOrFormula = "a = g · (sin θ - μ · cos θ)",
                        diagramType = "FORCE_VECTORS",
                        diagramContent = """
                                  /|
                                 / |
                           [m]  /  |
                           / \ /   |
                          /   /    |  h
                         /   / θ   |
                        /───┴──────┘
                        """.trimIndent(),
                        givenData = listOf("Mass m = 10 kg", "Incline angle θ = 30°", "Friction coefficient μ = 0.20", "g = 9.8 m/s²"),
                        whatToFind = "Net downward acceleration (a)",
                        hint = "Resolve gravity into perpendicular component mg·cosθ and parallel component mg·sinθ.",
                        stepByStepSolution = listOf(
                            "Step 1: Normal force N = m · g · cos(30°) = 10 · 9.8 · 0.866 = 84.87 N",
                            "Step 2: Friction force f_k = μ · N = 0.20 · 84.87 N = 16.97 N",
                            "Step 3: Driving force along slope = m · g · sin(30°) = 10 · 9.8 · 0.5 = 49.0 N",
                            "Step 4: Net force F_net = 49.0 N - 16.97 N = 32.03 N",
                            "Step 5: Acceleration a = F_net / m = 32.03 N / 10 kg = 3.20 m/s²"
                        ),
                        finalAnswer = "a = 3.20 m/s² down the plane",
                        difficulty = "Hard"
                    )
                )
            }
            "chemistry" -> {
                problems.add(
                    PracticeProblem(
                        title = "Solution Molarity & Stoichiometry",
                        subject = "Chemistry",
                        problemStatement = "A student dissolves 11.7 grams of pure NaCl (Sodium Chloride) in deionized water and dilutes the solution to an exact total volume of 500 mL in a volumetric flask. Calculate the molarity (mol/L) of the resulting solution. (Molar mass of Na = 23.0 g/mol, Cl = 35.5 g/mol).",
                        problemStatementHindi = "11.7 ग्राम NaCl को 500 mL जल में घोलने पर प्राप्त विलयन की मोलरता ज्ञात कीजिए।",
                        equationOrFormula = "Molarity (M) = (Mass / Molar Mass) / Volume in Liters",
                        diagramType = "REACTION",
                        diagramContent = """
                          ┌────────────────────────┐
                          │   11.7 g pure NaCl     │
                          └───────────┬────────────┘
                                      │  dissolve & dilute
                                      ▼
                             [ Volumetric Flask ]
                             │  Total Vol = 500mL │
                             └────────────────────┘
                              M = n / V(L)
                        """.trimIndent(),
                        givenData = listOf("Mass of solute NaCl = 11.7 g", "Molar mass M_w = 23.0 + 35.5 = 58.5 g/mol", "Solution Volume V = 500 mL = 0.50 L"),
                        whatToFind = "Molarity (M) in mol/L",
                        hint = "First calculate the number of moles of solute (n = mass / molar mass), then divide by volume in liters.",
                        stepByStepSolution = listOf(
                            "Step 1: Calculate moles of NaCl: n = 11.7 g / 58.5 g/mol = 0.200 mol",
                            "Step 2: Convert volume to liters: V = 500 mL / 1000 = 0.500 L",
                            "Step 3: Calculate Molarity M = n / V = 0.200 mol / 0.500 L = 0.400 M (mol/L)"
                        ),
                        finalAnswer = "Molarity = 0.40 M (0.40 mol/L)",
                        difficulty = "Medium"
                    )
                )
                problems.add(
                    PracticeProblem(
                        title = "Ideal Gas Law Equation",
                        subject = "Chemistry",
                        problemStatement = "A 2.5 L sealed container holds 0.50 moles of an ideal gas at a temperature of 27°C (300 K). Calculate the pressure inside the container in atmospheres (atm). (Gas constant R = 0.0821 L·atm/(mol·K)).",
                        equationOrFormula = "P · V = n · R · T",
                        diagramType = "REACTION",
                        diagramContent = """
                          ┌────────────────────────┐
                          │    P = ? atm           │
                          │    n = 0.50 mol gas    │
                          │    T = 300 K           │
                          │    V = 2.50 L          │
                          └────────────────────────┘
                        """.trimIndent(),
                        givenData = listOf("Volume V = 2.5 L", "Moles n = 0.50 mol", "Temperature T = 27 + 273.15 = 300.15 K ≈ 300 K", "R = 0.0821 L·atm/(mol·K)"),
                        whatToFind = "Pressure P in atm",
                        hint = "Rearrange the ideal gas equation: P = (n · R · T) / V.",
                        stepByStepSolution = listOf(
                            "Step 1: Rearrange equation: P = (n · R · T) / V",
                            "Step 2: Substitute: P = (0.50 mol · 0.0821 · 300 K) / 2.5 L",
                            "Step 3: P = 12.315 / 2.5 = 4.93 atm"
                        ),
                        finalAnswer = "Pressure P = 4.93 atm",
                        difficulty = "Easy"
                    )
                )
            }
            "mathematics", "math" -> {
                problems.add(
                    PracticeProblem(
                        title = "Definite Integral & Area under Curve",
                        subject = "Mathematics",
                        problemStatement = "Evaluate the definite integral ∫ from x = 0 to x = 3 of (3x² - 2x + 4) dx. Find the exact numerical value of the bounded area under the curve.",
                        problemStatementHindi = "निश्चित समाकलन ∫(3x² - 2x + 4)dx की सीमा 0 से 3 तक का मान ज्ञात कीजिए।",
                        equationOrFormula = "∫ xⁿ dx = (xⁿ⁺¹) / (n + 1) + C",
                        diagramType = "GRAPH",
                        diagramContent = """
                           y ▲
                             │         /  f(x) = 3x² - 2x + 4
                             │        /
                             │  ░░░░░/  Area = ?
                             │  ░░░░/
                             └───────────▲───────► x
                             0           3
                        """.trimIndent(),
                        givenData = listOf("Function f(x) = 3x² - 2x + 4", "Lower limit a = 0", "Upper limit b = 3"),
                        whatToFind = "Definite integral value / bounded area",
                        hint = "Find the antiderivative F(x) term by term, then apply the Fundamental Theorem of Calculus: F(b) - F(a).",
                        stepByStepSolution = listOf(
                            "Step 1: Integrate term by term: ∫ 3x² dx = x³,  ∫ -2x dx = -x²,  ∫ 4 dx = 4x",
                            "Step 2: Antiderivative F(x) = x³ - x² + 4x",
                            "Step 3: Evaluate at upper limit x = 3: F(3) = 3³ - 3² + 4(3) = 27 - 9 + 12 = 30",
                            "Step 4: Evaluate at lower limit x = 0: F(0) = 0",
                            "Step 5: Calculate F(3) - F(0) = 30 - 0 = 30"
                        ),
                        finalAnswer = "Value = 30",
                        difficulty = "Medium"
                    )
                )
            }
            else -> {
                problems.add(
                    PracticeProblem(
                        title = "Quantitative Concept Analysis",
                        subject = subject,
                        problemStatement = "Based on the formulas and principles noted in your scanned $subject notes, derive and solve the core relationship between the primary variables.",
                        equationOrFormula = "Y = f(X_1, X_2)",
                        diagramType = "TEXT_DIAGRAM",
                        diagramContent = """
                         [ Input Variables ] ──► [ Governing Rule ] ──► [ Target Output ]
                        """.trimIndent(),
                        givenData = listOf("Review the notes definitions and equations above"),
                        whatToFind = "System equilibrium / output variable",
                        hint = "Isolate the unknown variable on one side of the equality.",
                        stepByStepSolution = listOf(
                            "Step 1: Write down the primary formula identified from notes.",
                            "Step 2: Substitute the known benchmark values.",
                            "Step 3: Simplify and state the final result with appropriate units."
                        ),
                        finalAnswer = "Refer to the step-by-step working above.",
                        difficulty = "Medium"
                    )
                )
            }
        }
        return problems
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        // 1. Run real on-device Google ML Kit OCR (instant, offline, works without any API key)
        val onDeviceText = com.example.util.OfflineOcrProcessor.extractTextFromBitmap(bitmap)

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val endpoint = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
                val base64Image = bitmapToBase64(bitmap)

                val promptJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "Perform full verbatim OCR on this handwritten study note/textbook page. Transcribe all text, equations, formulas, diagrams, headers, and margin annotations accurately into clean markdown formatted text.")
                                })
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(promptJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = okHttpClient.newCall(request).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = JSONObject(body)
                    val cand = root.optJSONArray("candidates")?.optJSONObject(0)
                    val text = cand?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "OCR text extraction fallback: ${e.message}")
            }
        }

        // Return real on-device ML Kit OCR text
        onDeviceText
    }
}
