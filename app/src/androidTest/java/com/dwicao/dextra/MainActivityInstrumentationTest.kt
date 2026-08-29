package com.dwicao.dextra

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun browserControlsRenderOnActivityStart() {
        composeRule.onNodeWithTag("browser_surface").assertIsDisplayed()
    }

    @Test
    fun browserSurfaceSurvivesActivityRecreation() {
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("browser_surface").assertIsDisplayed()
    }
}
