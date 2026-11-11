package com.tau.studymate.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the startup path and a visit to every tab. */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "com.tau.studymate") {
        pressHome()
        startActivityAndWait()
        // First launch shows onboarding; accept the default degree if it's there
        device.findObject(By.text("החל והתאם קורסים"))?.click()
        for (tab in listOf("מטלות", "קורסים", "קריאה", "פוקוס", "היום שלי")) {
            device.wait(Until.findObject(By.text(tab)), 5_000)?.click()
            device.waitForIdle()
        }
    }
}
