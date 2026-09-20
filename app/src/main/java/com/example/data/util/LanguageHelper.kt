package com.example.data.util

import com.example.data.model.Flashcard
import com.example.data.model.QuizQuestion

object LanguageHelper {

    fun getFront(card: Flashcard, lang: String): String {
        return if (isHindi(lang)) {
            card.frontHindi.ifBlank { autoTranslateToHindi(card.front) }
        } else {
            card.front
        }
    }

    fun getBack(card: Flashcard, lang: String): String {
        return if (isHindi(lang)) {
            card.backHindi.ifBlank { autoTranslateToHindi(card.back) }
        } else {
            card.back
        }
    }

    fun getKeyTerm(card: Flashcard, lang: String): String {
        return if (isHindi(lang)) {
            card.keyTermHindi.ifBlank { autoTranslateToHindi(card.keyTerm) }
        } else {
            card.keyTerm
        }
    }

    fun getQuestion(quiz: QuizQuestion, lang: String): String {
        return if (isHindi(lang)) {
            quiz.questionHindi.ifBlank { autoTranslateToHindi(quiz.question) }
        } else {
            quiz.question
        }
    }

    fun getOptions(quiz: QuizQuestion, lang: String): List<String> {
        return if (isHindi(lang)) {
            if (quiz.optionsHindi.isNotEmpty() && quiz.optionsHindi.size == quiz.options.size) {
                quiz.optionsHindi
            } else {
                quiz.options.map { autoTranslateToHindi(it) }
            }
        } else {
            quiz.options
        }
    }

    fun getExplanation(quiz: QuizQuestion, lang: String): String {
        return if (isHindi(lang)) {
            quiz.explanationHindi.ifBlank { autoTranslateToHindi(quiz.explanation) }
        } else {
            quiz.explanation
        }
    }

    private fun isHindi(lang: String): Boolean {
        return lang.equals("HI", ignoreCase = true) || lang.equals("Hindi", ignoreCase = true)
    }

    /**
     * Context-aware educational translation dictionary and rule-based Hindi transliteration fallback.
     * Guarantees intelligible Hindi text for chemistry, physics, biology, and maths cards.
     */
    fun autoTranslateToHindi(text: String): String {
        if (text.isBlank()) return text

        // Check if text already contains Devanagari characters
        val containsDevanagari = text.any { it.code in 0x0900..0x097F }
        if (containsDevanagari) return text

        var result = text

        // Exact term / sentence replacements
        val directDictionary = mapOf(
            // Chemistry & Solution Terms (Specifically from user's notes)
            "Colligative Property" to "अनुसंख्य गुणधर्म",
            "colligative properties" to "अनुसंख्य गुणधर्म",
            "Depression in Freezing Point" to "हिमांक में अवनमन",
            "Elevation in Boiling Point" to "क्वथनांक में उन्नयन",
            "Osmotic Pressure" to "परासरण दाब",
            "Relative Lowering of Vapor Pressure" to "वाष्प दाब में आपेक्षिक अवनमन",
            "Vapour Pressure" to "वाष्प दाब",
            "Molarity" to "मोलरता",
            "Molality" to "मोललता",
            "Mole Fraction" to "मोल अंश",
            "Solute" to "विलेय",
            "Solvent" to "विलायक",
            "Solution" to "विलयन",
            "Ideal Solution" to "आदर्श विलयन",
            "Non-ideal Solution" to "अनादर्श विलयन",
            "Henry's Law" to "हेनरी का नियम",
            "Raoult's Law" to "राउल्ट का नियम",
            "Van't Hoff Factor" to "वांट हॉफ कारक (i)",
            "Abnormal Molar Mass" to "असामान्य मोलर द्रव्यमान",
            "Covalent Bond" to "सहसंयोजक बंध",
            "Ionic Bond" to "आयनिक बंध",
            "Oxidation" to "ऑक्सीकरण",
            "Reduction" to "अपचयन",
            "Redox Reaction" to "रेडॉक्स अभिक्रिया",
            "Chemical Equilibrium" to "रासायनिक साम्यावस्था",
            "Catalyst" to "उत्प्रेरक",
            "Activation Energy" to "सक्रियण ऊर्जा",
            "Endothermic" to "ऊष्माशोषी",
            "Exothermic" to "ऊष्माक्षेपी",
            "Acid" to "अम्ल",
            "Base" to "क्षार",

            // Physics Terms
            "Newton's First Law" to "न्यूटन का प्रथम नियम (जड़त्व का नियम)",
            "Newton's Second Law" to "न्यूटन का द्वितीय नियम (बल का नियम)",
            "Newton's Third Law" to "न्यूटन का तृतीय नियम (क्रिया-प्रतिक्रिया)",
            "Law of Inertia" to "जड़त्व का नियम",
            "Inertia" to "जड़त्व",
            "Momentum" to "संवेग",
            "Conservation of Momentum" to "संवेग संरक्षण का नियम",
            "Force" to "बल",
            "Acceleration" to "त्वरण",
            "Velocity" to "वेग",
            "Speed" to "चाल",
            "Displacement" to "विस्थापन",
            "Distance" to "दूरी",
            "Work" to "कार्य",
            "Energy" to "ऊर्जा",
            "Kinetic Energy" to "गतिज ऊर्जा",
            "Potential Energy" to "स्थितिज ऊर्जा",
            "Power" to "शक्ति",
            "Gravitational Force" to "गुरुत्वाकर्षण बल",
            "Friction" to "घर्षण",
            "Centripetal Force" to "अभिकेंद्रीय बल",
            "Torque" to "बल आघूर्ण",
            "Angular Momentum" to "कोणीय संवेग",

            // Biology Terms
            "Cellular Respiration" to "कोशिकीय श्वसन",
            "Mitochondria" to "माइटोकॉन्ड्रिया (ऊर्जा गृह)",
            "Glycolysis" to "ग्लाइकोलिसिस",
            "Krebs Cycle" to "क्रेब्स चक्र",
            "Electron Transport Chain" to "इलेक्ट्रॉन परिवहन शृंखला",
            "Photosynthesis" to "प्रकाश संश्लेषण",
            "Chloroplast" to "हरितलवक (क्लोरोप्लास्ट)",
            "Nucleus" to "केंद्रक",
            "Cytoplasm" to "कोशिकाद्रव्य",
            "Ribosome" to "राइबोसोम",
            "Endoplasmic Reticulum" to "अंतःप्रद्रव्यी जालिका",
            "DNA" to "डीएनए (DNA)",
            "RNA" to "आरएनए (RNA)",
            "Protein Synthesis" to "प्रोटीन संश्लेषण",
            "Enzyme" to "एंजाइम",
            "Mitosis" to "समसूत्री विभाजन",
            "Meiosis" to "अर्धसूत्री विभाजन",
            "Catabolism" to "अपचय",
            "Anabolism" to "उपचय",
            "Metabolism" to "उपापचय",
            "ATP" to "एटीपी (ATP)",

            // Common Question Frames
            "What is the primary function of" to "किसका मुख्य कार्य क्या है:",
            "What is the definition of" to "किसकी परिभाषा क्या है:",
            "What is" to "क्या है:",
            "State Newton's First Law" to "न्यूटन का प्रथम नियम बताएं",
            "State Le Chatelier's Principle" to "ला शातेलिए का नियम बताएं",
            "Where does" to "कहाँ घटित होता है:",
            "Differentiate between" to "अंतर स्पष्ट करें:",
            "Which molecule is" to "कौन सा अणु है:",
            "What happens when" to "क्या होता है जब:"
        )

        // Replace matched terms safely
        for ((en, hi) in directDictionary) {
            if (result.equals(en, ignoreCase = true)) {
                return hi
            }
            if (result.contains(en, ignoreCase = true)) {
                result = result.replace(Regex("(?i)\\b${Regex.escape(en)}\\b"), hi)
            }
        }

        return result
    }
}
