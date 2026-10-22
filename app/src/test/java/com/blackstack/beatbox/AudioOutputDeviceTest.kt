package com.blackstack.beatbox

import android.media.AudioDeviceInfo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the audio output device classification.
 *
 * The mini player and the top bar indicator show which device the audio is
 * routed to. The mapping from an [AudioDeviceInfo] type to a user facing
 * category is pure logic, so it can be verified without an Android Context.
 */
class AudioOutputDeviceTest : StringSpec({

    "bluetooth devices are reported as bluetooth" {
        audioOutputKindFor(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) shouldBe AudioOutputKind.BLUETOOTH
        audioOutputKindFor(AudioDeviceInfo.TYPE_BLUETOOTH_SCO) shouldBe AudioOutputKind.BLUETOOTH
        audioOutputKindFor(AudioDeviceInfo.TYPE_BLE_HEADSET) shouldBe AudioOutputKind.BLUETOOTH
        audioOutputKindFor(AudioDeviceInfo.TYPE_BLE_SPEAKER) shouldBe AudioOutputKind.BLUETOOTH
    }

    "wired and usb headsets are reported as headphones" {
        audioOutputKindFor(AudioDeviceInfo.TYPE_WIRED_HEADSET) shouldBe AudioOutputKind.HEADPHONES
        audioOutputKindFor(AudioDeviceInfo.TYPE_WIRED_HEADPHONES) shouldBe AudioOutputKind.HEADPHONES
        audioOutputKindFor(AudioDeviceInfo.TYPE_USB_HEADSET) shouldBe AudioOutputKind.HEADPHONES
        audioOutputKindFor(AudioDeviceInfo.TYPE_USB_DEVICE) shouldBe AudioOutputKind.HEADPHONES
    }

    "the built-in speaker and unknown types are not classified" {
        audioOutputKindFor(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) shouldBe AudioOutputKind.NONE
        audioOutputKindFor(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) shouldBe AudioOutputKind.NONE
        audioOutputKindFor(Int.MAX_VALUE) shouldBe AudioOutputKind.NONE
    }
})
