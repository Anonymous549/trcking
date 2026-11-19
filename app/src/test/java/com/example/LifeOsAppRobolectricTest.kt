package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LifeOsAppRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `bottom navigation traverses all 5 core tabs and opens modules hub`() {
        composeRule.waitForIdle()

        // Dashboard loads with Today's date, welcome message, completion % & Quick Actions grid
        composeRule.onNodeWithTag("dashboard_date").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_welcome").assertIsDisplayed()
        composeRule.onNodeWithText("Today's Rhythm").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_habit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_task").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_expense").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_journal").performScrollTo().assertIsDisplayed()

        // Navigate to Habits
        composeRule.onNodeWithTag("nav_habits").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Habits & Discipline").assertIsDisplayed()

        // Navigate to Focus (Tasks / Study / Routine)
        composeRule.onNodeWithTag("nav_focus").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Focus, Study & Routine").assertIsDisplayed()

        // Navigate to Analytics
        composeRule.onNodeWithTag("nav_analytics").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Personal Analytics").assertIsDisplayed()

        // Navigate to Modules Hub and open Personal Finance
        composeRule.onNodeWithTag("nav_modules").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Life Modules").assertIsDisplayed()

        composeRule.onNodeWithTag("module_finance").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Personal Finance").assertIsDisplayed()
    }
}
