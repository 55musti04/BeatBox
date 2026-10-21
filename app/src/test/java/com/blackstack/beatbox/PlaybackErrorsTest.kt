package com.blackstack.beatbox

import java.io.EOFException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorsTest {
    @Test
    fun causeChainKeepsNestedFailures() {
        val rootCause = EOFException("stream ended")
        val sourceCause = IOException("read failed", rootCause)
        val error = IllegalStateException("playback failed", sourceCause)

        assertEquals(listOf(error, sourceCause, rootCause), error.causeChain())
    }

    @Test
    fun offlineWarningReplacesProviderErrors() {
        assertEquals(
            listOf("İnternet bağlantısı yok"),
            playbackErrorMessages(
                isOnline = false,
                offlineMessage = "İnternet bağlantısı yok",
                guidance = null,
                rawErrorMessages = listOf("Şarkı bulunamadı"),
            ),
        )
    }

    @Test
    fun onlineMessagesKeepGuidanceAndRemoveDuplicates() {
        assertEquals(
            listOf("Tekrar deneyin", "Sunucu yanıt vermedi"),
            playbackErrorMessages(
                isOnline = true,
                offlineMessage = "İnternet bağlantısı yok",
                guidance = "Tekrar deneyin",
                rawErrorMessages = listOf("Sunucu yanıt vermedi", "Tekrar deneyin"),
            ),
        )
    }
}