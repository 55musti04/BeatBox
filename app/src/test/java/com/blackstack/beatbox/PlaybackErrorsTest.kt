package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.EOFException
import java.io.IOException

/**
 * Unit tests for the playback error helpers.
 */
class PlaybackErrorsTest : StringSpec({

    "causeChain keeps nested failures" {
        val rootCause = EOFException("stream ended")
        val sourceCause = IOException("read failed", rootCause)
        val error = IllegalStateException("playback failed", sourceCause)

        error.causeChain() shouldBe listOf(error, sourceCause, rootCause)
    }

    "causeChain of an error without cause is a single element" {
        val error = IllegalStateException("playback failed")

        error.causeChain() shouldBe listOf(error)
    }

    "offline warning replaces provider errors" {
        playbackErrorMessages(
            isOnline = false,
            offlineMessage = "İnternet bağlantısı yok",
            guidance = null,
            rawErrorMessages = listOf("Şarkı bulunamadı"),
        ) shouldBe listOf("İnternet bağlantısı yok")
    }

    "online messages keep guidance and remove duplicates" {
        playbackErrorMessages(
            isOnline = true,
            offlineMessage = "İnternet bağlantısı yok",
            guidance = "Tekrar deneyin",
            rawErrorMessages = listOf("Sunucu yanıt vermedi", "Tekrar deneyin"),
        ) shouldBe listOf("Tekrar deneyin", "Sunucu yanıt vermedi")
    }

    "no guidance and no errors produce an empty list" {
        playbackErrorMessages(
            isOnline = true,
            offlineMessage = "İnternet bağlantısı yok",
            guidance = null,
            rawErrorMessages = emptyList(),
        ) shouldBe emptyList()
    }
})
