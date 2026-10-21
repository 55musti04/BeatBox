package com.blackstack.beatbox

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bug Condition Exploration Test for Audio Output Device Selector
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 * 
 * This test encodes the EXPECTED behavior (persistent audio output indicator in mini player bar).
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * When this test passes after implementation, it confirms the bug is fixed.
 * 
 * Property 1: Fault Condition - Persistent Audio Output Indicator Missing
 * 
 * For any state where audio is playing or paused, the MiniPlayerBar SHALL display:
 * - A persistent audio output indicator button showing current device
 * - The indicator SHALL remain visible without fading out
 * - The indicator SHALL be clickable to open device selection interface
 * 
 * Expected counterexamples on UNFIXED code:
 * - Mini player bar does not contain audio output indicator button
 * - No clickable UI element for device selection in mini player bar
 * - Only temporary top bar indicator exists (fades after 3 seconds)
 */
@RunWith(AndroidJUnit4::class)
class AudioOutputIndicatorBugTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Property-Based Test: Persistent Audio Output Indicator in Mini Player Bar
     * 
     * This test verifies that during active playback, the mini player bar displays
     * a persistent audio output indicator button that:
     * 1. Shows the current audio output device (Bluetooth, Headphones, or Phone Speaker)
     * 2. Remains visible without fading out
     * 3. Is clickable to open device selection interface
     * 
     * Scoped to concrete failing case: mini player bar during active playback
     */
    @Test
    fun property_miniPlayerBar_shouldDisplayPersistentAudioOutputIndicator_duringPlayback() = runBlocking {
        // Property-based test scoped to the bug condition
        // We test with a constant playback state since the bug manifests during any playback
        checkAll(
            iterations = 10,
            Arb.constant(true) // isPlaying = true (active playback state)
        ) { isPlaying ->
            // Create a mock audio file for testing
            val mockAudio = createMockAudioFile()
            
            // Render the MiniPlayerBar component
            composeTestRule.setContent {
                MiniPlayerBar(
                    a = mockAudio,
                    isPlaying = isPlaying,
                    onClick = {},
                    onPlayPause = {},
                    onNext = {},
                    onPrev = {}
                )
            }

            // Wait for composition to settle
            composeTestRule.waitForIdle()

            // ASSERTION 1: Verify persistent audio output indicator button exists
            // Expected to FAIL on unfixed code: No audio output indicator in mini player bar
            val audioOutputIndicatorExists = try {
                composeTestRule.onNodeWithContentDescription(
                    "Audio Output Device",
                    useUnmergedTree = true
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            // ASSERTION 2: Verify the indicator is clickable
            // Expected to FAIL on unfixed code: No clickable audio device selector
            val indicatorIsClickable = if (audioOutputIndicatorExists) {
                try {
                    composeTestRule.onNodeWithContentDescription(
                        "Audio Output Device",
                        useUnmergedTree = true
                    ).assertHasClickAction()
                    true
                } catch (e: AssertionError) {
                    false
                }
            } else {
                false
            }

            // ASSERTION 3: Verify indicator shows device information
            // Expected to FAIL on unfixed code: No device information displayed
            val showsDeviceInfo = if (audioOutputIndicatorExists) {
                try {
                    // Check for any of the expected device types
                    val hasBluetoothIcon = composeTestRule.onAllNodesWithContentDescription(
                        "Bluetooth",
                        useUnmergedTree = true
                    ).fetchSemanticsNodes().isNotEmpty()
                    
                    val hasHeadphonesIcon = composeTestRule.onAllNodesWithContentDescription(
                        "Headphones",
                        useUnmergedTree = true
                    ).fetchSemanticsNodes().isNotEmpty()
                    
                    val hasPhoneIcon = composeTestRule.onAllNodesWithContentDescription(
                        "Phone Speaker",
                        useUnmergedTree = true
                    ).fetchSemanticsNodes().isNotEmpty()
                    
                    hasBluetoothIcon || hasHeadphonesIcon || hasPhoneIcon
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }

            // FINAL ASSERTION: All conditions must be true for the bug to be fixed
            // This assertion will FAIL on unfixed code, confirming the bug exists
            assert(audioOutputIndicatorExists && indicatorIsClickable && showsDeviceInfo) {
                """
                Bug Condition Detected: Persistent Audio Output Indicator Missing
                
                Expected behavior (not found in unfixed code):
                - Persistent audio output indicator button in mini player bar
                - Indicator shows current device (Bluetooth, Headphones, or Phone Speaker)
                - Indicator is clickable to open device selection interface
                
                Actual behavior (counterexample):
                - Audio output indicator exists: $audioOutputIndicatorExists
                - Indicator is clickable: $indicatorIsClickable
                - Shows device information: $showsDeviceInfo
                
                Root cause: MiniPlayerBar does not include persistent audio output indicator.
                Only temporary top bar indicator exists (fades after 3 seconds).
                
                This failure confirms the bug exists as described in requirements 2.1, 2.2, 2.3, 2.4.
                """.trimIndent()
            }
        }
    }

    /**
     * Helper function to create a mock AudioFile for testing
     */
    private fun createMockAudioFile(): AudioFile {
        return AudioFile(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            duration = 180000L,
            uri = android.net.Uri.parse("content://media/external/audio/media/1"),
            albumId = 1L,
            folderName = "Music",
            dateAdded = System.currentTimeMillis()
        )
    }
}
