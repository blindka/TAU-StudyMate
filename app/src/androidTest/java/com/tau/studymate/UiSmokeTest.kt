package com.tau.studymate

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Opens every tab in English and in Hebrew; fails on a crash or a missing tab title. */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun setLanguage(tag: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cmd locale set-app-locales com.tau.studymate --locales $tag").close()
        Thread.sleep(1500)
    }

    private fun walkTabs(degree: String, apply: String, tabs: List<String>) {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            // First launch shows onboarding
            // First launch shows onboarding: pick a catalog degree, then apply (the field starts empty)
            if (compose.onAllNodesWithText(apply).fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodesWithText(degree).onFirst().performClick()
                compose.onAllNodesWithText(apply).onFirst().performScrollTo().performClick()
                compose.waitForIdle()
            }
            for (tab in tabs) {
                compose.onAllNodesWithText(tab).onFirst().performClick()
                compose.waitForIdle()
            }
        }
    }

    @Test fun english() {
        setLanguage("en")
        walkTabs("Communication & Labor Studies", "Apply", listOf("Tasks", "Courses", "Readings", "Focus", "Today"))
    }

    @Test fun hebrew() {
        setLanguage("he")
        walkTabs("תקשורת ולימודי עבודה", "החל והתאם קורסים", listOf("מטלות", "קורסים", "קריאה", "פוקוס", "היום שלי"))
    }

    @After fun resetLanguage() = setLanguage("\"\"")
}
