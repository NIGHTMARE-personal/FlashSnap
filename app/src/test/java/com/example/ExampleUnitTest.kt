package com.example

import com.example.data.model.CapturedPage
import com.example.data.model.SubjectClassifier
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSubjectClassifier_detectsCorrectSubjects() {
        val physicsSnippet = "Newton's Second Law: F = m * a. Velocity and acceleration of projectile under gravity."
        val mathSnippet = "Differential calculus: derivative dy/dx of polynomials, integrals and matrix multiplication."
        val chemistrySnippet = "Periodic table: valence electrons, covalent bonding, exothermic reaction enthalpy change."
        val bioSnippet = "Cellular biology: mitochondria ATP production, DNA replication and mitosis phases."

        assertEquals("Physics", SubjectClassifier.detectSubject(physicsSnippet))
        assertEquals("Math", SubjectClassifier.detectSubject(mathSnippet))
        assertEquals("Chemistry", SubjectClassifier.detectSubject(chemistrySnippet))
        assertEquals("Biology", SubjectClassifier.detectSubject(bioSnippet))
    }

    @Test
    fun testCapturedPage_manualLabelOverridesAutoDetection() {
        // Auto-detected as Physics
        val page = CapturedPage(
            pageNumber = 1,
            autoDetectedSubject = "Physics",
            manualLabel = ""
        )
        assertEquals("Physics", page.effectiveSubject)
        assertFalse(page.isManuallyLabeled)

        // Now user overrides with manual label
        val labeledPage = page.copy(manualLabel = "Applied Mathematics")
        assertEquals("Applied Mathematics", labeledPage.effectiveSubject)
        assertTrue(labeledPage.isManuallyLabeled)
        assertTrue(labeledPage.hasManualOverride)

        // Reset manual label back to empty -> falls back to auto-detected
        val resetPage = labeledPage.copy(manualLabel = "")
        assertEquals("Physics", resetPage.effectiveSubject)
        assertFalse(resetPage.isManuallyLabeled)
    }

    @Test
    fun testMultiSubject10PagesScenario() {
        // User scenario: "3 is physics, four is from math, and others from chemistry or bio. It should know how to separate which one is which"
        val pages = mutableListOf<CapturedPage>()
        for (i in 1..3) {
            pages.add(CapturedPage(pageNumber = i, autoDetectedSubject = "Physics"))
        }
        for (i in 4..7) {
            pages.add(CapturedPage(pageNumber = i, autoDetectedSubject = "Math"))
        }
        for (i in 8..10) {
            pages.add(CapturedPage(pageNumber = i, autoDetectedSubject = "Chemistry"))
        }

        // Grouping by effective subject
        val grouped = pages.groupBy { it.effectiveSubject }
        assertEquals(3, grouped.keys.size)
        assertEquals(3, grouped["Physics"]?.size)
        assertEquals(4, grouped["Math"]?.size)
        assertEquals(3, grouped["Chemistry"]?.size)

        // Test manual label override on page 8 to "Physical Chemistry"
        val overriddenPage8 = pages[7].copy(manualLabel = "Physical Chemistry")
        pages[7] = overriddenPage8

        val regrouped = pages.groupBy { it.effectiveSubject }
        assertEquals(4, regrouped.keys.size)
        assertEquals(1, regrouped["Physical Chemistry"]?.size)
        assertEquals(2, regrouped["Chemistry"]?.size)
    }
}
