package com.dwicao.dextra

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
        composeRule.onNodeWithText("Search or enter web address").assertIsDisplayed()
    }

    @Test
    fun browserSurfaceSurvivesActivityRecreation() {
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Search or enter web address").assertIsDisplayed()
    }
}
