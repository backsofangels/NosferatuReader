package com.nosferatu.launcher.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull
import com.nosferatu.launcher.ui.MainActivity

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsScreen_launches() {
        assertNotNull(composeTestRule.activity)
        // TODO: navigate to settings and assert toggles persist
    }
}
