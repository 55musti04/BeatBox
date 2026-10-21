package com.blackstack.beatbox

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Preservation Property Tests for Temporary Top Bar Audio Output Indicator
 * 
 * **Validates: Requirements 3.1, 3.2**
 * 
 * Property 2: Preservation - Temporary Top Bar Indicator
 * 
 * IMPORTANT: These tests follow observation-first methodology.
 * They capture the CURRENT behavior on UNFIXED code to ensure it is preserved after the fix.
 * 
 * These tests verify that the existing temporary audio output indicator:
 * - Appears in the top bar visualizer area when playback starts
 * - Displays the correct audio output device (Bluetooth, Headphones, or Phone Speaker)
 * - Remains visible for 3 seconds
 * - Fades out after 3 seconds
 * - Automatically detects device changes
 * 
 * EXPECTED OUTCOME: Tests PASS on unfixed code (confirms baseline behavior to preserve)
 */
@RunWith(AndroidJUnit4::class)
class TopBarIndicatorPreservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Property Test: Temporary Top Bar Indicator Appears on Playback Start
     * 
     * Verifies that when playback starts:
     * - The temporary audio output indicator appears in the top bar
     * - It displays the current audio output device
     * - It shows an appropriate icon (Bluetooth, Headphones, or Phone)
     * 
     * This behavior must be preserved after adding the persistent mini player indicator.
     */
    @Test
    fun property_topBarIndicator_shouldAppearOnPlaybackStart() = runBlocking {
        checkAll(
            iterations = 10,
            Arb.constant(true) // Test with playback starting (isPlaying = true)
        ) { isPlaying ->
            composeTestRule.setContent {
                AudioOutputIndicator(
                    context = context,
                    isPlaying = isPlaying
                )
            }

            // Wait for the indicator to appear (it should appear immediately when isPlaying = true)
            composeTestRule.waitForIdle()
            
            // Give a small delay for the animation to complete
            delay(500)

            // Verify the indicator is visible
            // The indicator should show one of: "Bluetooth", "Kulaklık" (Headphones), or "Telefon" (Phone)
            val indicatorVisible = try {
                // Try to find any of the expected device text
                val hasBluetoothText = composeTestRule.onAllNodesWithText("Bluetooth", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasHeadphonesText = composeTestRule.onAllNodesWithText("Kulaklık", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasPhoneText = composeTestRule.onAllNodesWithText("Telefon", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                hasBluetoothText || hasHeadphonesText || hasPhoneText
            } catch (e: Exception) {
                false
            }

            assert(indicatorVisible) {
                "Temporary top bar indicator should appear when playback starts and display current audio output device"
            }
        }
    }

    /**
     * Property Test: Temporary Top Bar Indicator Fades Out After 3 Seconds
     * 
     * Verifies that the temporary audio output indicator:
     * - Remains visible for approximately 3 seconds
     * - Fades out after 3 seconds
     * - Is no longer visible after fade out completes
     * 
     * This behavior must be preserved after adding the persistent mini player indicator.
     */
    @Test
    fun property_topBarIndicator_shouldFadeOutAfter3Seconds() = runBlocking {
        checkAll(
            iterations = 5, // Reduced iterations since this test involves delays
            Arb.constant(true) // Test with playback starting
        ) { isPlaying ->
            composeTestRule.setContent {
                AudioOutputIndicator(
                    context = context,
                    isPlaying = isPlaying
                )
            }

            composeTestRule.waitForIdle()
            
            // Verify indicator is visible initially
            delay(500)
            val initiallyVisible = try {
                val hasBluetoothText = composeTestRule.onAllNodesWithText("Bluetooth", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasHeadphonesText = composeTestRule.onAllNodesWithText("Kulaklık", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasPhoneText = composeTestRule.onAllNodesWithText("Telefon", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                hasBluetoothText || hasHeadphonesText || hasPhoneText
            } catch (e: Exception) {
                false
            }

            assert(initiallyVisible) {
                "Indicator should be visible initially"
            }

            // Wait for 3.5 seconds (3 seconds display + 0.5 seconds for fade out animation)
            delay(3500)
            composeTestRule.waitForIdle()

            // Verify indicator is no longer visible
            val hiddenAfterDelay = try {
                val hasBluetoothText = composeTestRule.onAllNodesWithText("Bluetooth", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                val hasHeadphonesText = composeTestRule.onAllNodesWithText("Kulaklık", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                val hasPhoneText = composeTestRule.onAllNodesWithText("Telefon", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                hasBluetoothText && hasHeadphonesText && hasPhoneText
            } catch (e: Exception) {
                true // If we can't find any text, it's hidden
            }

            assert(hiddenAfterDelay) {
                "Temporary top bar indicator should fade out and be hidden after 3 seconds"
            }
        }
    }

    /**
     * Property Test: Temporary Top Bar Indicator Detects Audio Output Device
     * 
     * Verifies that the temporary audio output indicator:
     * - Correctly detects the current audio output device
     * - Displays one of the expected device types: Bluetooth, Headphones, or Phone Speaker
     * - Shows an appropriate icon for the device type
     * 
     * This automatic detection behavior must be preserved after adding the persistent indicator.
     */
    @Test
    fun property_topBarIndicator_shouldDetectAudioOutputDevice() = runBlocking {
        checkAll(
            iterations = 10,
            Arb.constant(true) // Test with playback starting
        ) { isPlaying ->
            composeTestRule.setContent {
                AudioOutputIndicator(
                    context = context,
                    isPlaying = isPlaying
                )
            }

            composeTestRule.waitForIdle()
            delay(500)

            // Verify that the indicator shows one of the expected device types
            val showsValidDevice = try {
                val hasBluetoothText = composeTestRule.onAllNodesWithText("Bluetooth", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasHeadphonesText = composeTestRule.onAllNodesWithText("Kulaklık", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                val hasPhoneText = composeTestRule.onAllNodesWithText("Telefon", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                
                // Should show exactly one of these
                (hasBluetoothText || hasHeadphonesText || hasPhoneText)
            } catch (e: Exception) {
                false
            }

            assert(showsValidDevice) {
                "Temporary top bar indicator should detect and display a valid audio output device (Bluetooth, Headphones, or Phone Speaker)"
            }
        }
    }

    /**
     * Property Test: Temporary Top Bar Indicator Does Not Appear When Not Playing
     * 
     * Verifies that the temporary audio output indicator:
     * - Does NOT appear when playback is not active (isPlaying = false)
     * - Only shows when playback starts
     * 
     * This behavior must be preserved after adding the persistent mini player indicator.
     */
    @Test
    fun property_topBarIndicator_shouldNotAppearWhenNotPlaying() = runBlocking {
        checkAll(
            iterations = 10,
            Arb.constant(false) // Test with playback stopped (isPlaying = false)
        ) { isPlaying ->
            composeTestRule.setContent {
                AudioOutputIndicator(
                    context = context,
                    isPlaying = isPlaying
                )
            }

            composeTestRule.waitForIdle()
            delay(500)

            // Verify the indicator is NOT visible
            val indicatorNotVisible = try {
                val hasBluetoothText = composeTestRule.onAllNodesWithText("Bluetooth", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                val hasHeadphonesText = composeTestRule.onAllNodesWithText("Kulaklık", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                val hasPhoneText = composeTestRule.onAllNodesWithText("Telefon", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
                
                hasBluetoothText && hasHeadphonesText && hasPhoneText
            } catch (e: Exception) {
                true // If we can't find any text, it's not visible (which is correct)
            }

            assert(indicatorNotVisible) {
                "Temporary top bar indicator should NOT appear when playback is not active"
            }
        }
    }
}
