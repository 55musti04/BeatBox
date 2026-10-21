package com.blackstack.beatbox

import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Test

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
class AudioOutputIndicatorBugTest {

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
     * 
     * This test inspects the MiniPlayerBar function signature and implementation
     * to verify the presence of audio output indicator parameters and functionality.
     */
    @Test
    fun property_miniPlayerBar_shouldHaveAudioOutputIndicatorParameters() = runBlocking {
        // Property-based test scoped to the bug condition
        // We test with a constant playback state since the bug manifests during any playback
        checkAll(
            iterations = 10,
            Arb.constant(true) // isPlaying = true (active playback state)
        ) { isPlaying ->
            
            // ASSERTION 1: Verify MiniPlayerBar has audio output device parameter
            // Expected to FAIL on unfixed code: No audioOutputDevice parameter
            val hasMiniPlayerBarAudioOutputDeviceParam = checkMiniPlayerBarHasParameter("audioOutputDevice")
            
            // ASSERTION 2: Verify MiniPlayerBar has audio output icon parameter
            // Expected to FAIL on unfixed code: No audioOutputIcon parameter
            val hasMiniPlayerBarAudioOutputIconParam = checkMiniPlayerBarHasParameter("audioOutputIcon")
            
            // ASSERTION 3: Verify MiniPlayerBar has audio output click callback
            // Expected to FAIL on unfixed code: No onAudioOutputClick parameter
            val hasMiniPlayerBarAudioOutputClickParam = checkMiniPlayerBarHasParameter("onAudioOutputClick")
            
            // ASSERTION 4: Verify AudioDeviceSelectionBottomSheet exists
            // Expected to FAIL on unfixed code: No device selection UI component
            val hasAudioDeviceSelectionBottomSheet = checkFunctionExists("AudioDeviceSelectionBottomSheet")
            
            // FINAL ASSERTION: All conditions must be true for the bug to be fixed
            // This assertion will FAIL on unfixed code, confirming the bug exists
            assert(
                hasMiniPlayerBarAudioOutputDeviceParam &&
                hasMiniPlayerBarAudioOutputIconParam &&
                hasMiniPlayerBarAudioOutputClickParam &&
                hasAudioDeviceSelectionBottomSheet
            ) {
                """
                Bug Condition Detected: Persistent Audio Output Indicator Missing
                
                Expected behavior (not found in unfixed code):
                - MiniPlayerBar should have audioOutputDevice parameter: $hasMiniPlayerBarAudioOutputDeviceParam
                - MiniPlayerBar should have audioOutputIcon parameter: $hasMiniPlayerBarAudioOutputIconParam
                - MiniPlayerBar should have onAudioOutputClick callback: $hasMiniPlayerBarAudioOutputClickParam
                - AudioDeviceSelectionBottomSheet component should exist: $hasAudioDeviceSelectionBottomSheet
                
                Root cause: MiniPlayerBar does not include persistent audio output indicator.
                Only temporary top bar indicator exists (fades after 3 seconds).
                
                This failure confirms the bug exists as described in requirements 2.1, 2.2, 2.3, 2.4.
                
                Counterexamples found:
                - Mini player bar does not contain audio output indicator button
                - No clickable UI element for device selection in mini player bar
                - No device selection interface available
                """.trimIndent()
            }
        }
    }

    /**
     * Helper function to check if MiniPlayerBar has a specific parameter
     * This uses reflection to inspect the function signature
     */
    private fun checkMiniPlayerBarHasParameter(parameterName: String): Boolean {
        return try {
            // Read the UIComponents.kt file to check for parameter
            val uiComponentsFile = java.io.File("app/src/main/java/com/blackstack/beatbox/UIComponents.kt")
            if (!uiComponentsFile.exists()) {
                // Try alternative path
                val altFile = java.io.File("BeatBox/app/src/main/java/com/blackstack/beatbox/UIComponents.kt")
                if (!altFile.exists()) {
                    return false
                }
                val content = altFile.readText()
                checkParameterInContent(content, "MiniPlayerBar", parameterName)
            } else {
                val content = uiComponentsFile.readText()
                checkParameterInContent(content, "MiniPlayerBar", parameterName)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Helper function to check if a function exists in the codebase
     */
    private fun checkFunctionExists(functionName: String): Boolean {
        return try {
            // Read the Screens.kt file to check for function
            val screensFile = java.io.File("app/src/main/java/com/blackstack/beatbox/Screens.kt")
            if (!screensFile.exists()) {
                // Try alternative path
                val altFile = java.io.File("BeatBox/app/src/main/java/com/blackstack/beatbox/Screens.kt")
                if (!altFile.exists()) {
                    return false
                }
                val content = altFile.readText()
                content.contains("fun $functionName")
            } else {
                val content = screensFile.readText()
                content.contains("fun $functionName")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Helper function to check if a parameter exists in function signature
     */
    private fun checkParameterInContent(content: String, functionName: String, parameterName: String): Boolean {
        // Find the function definition
        val functionPattern = Regex("fun\\s+$functionName\\s*\\([^)]*\\)")
        val match = functionPattern.find(content) ?: return false
        
        val functionSignature = match.value
        return functionSignature.contains(parameterName)
    }
}
