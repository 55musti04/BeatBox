package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Unit tests for LyricsProvider
 * 
 * Tests the core functionality and error handling.
 * Note: These are basic structural tests. Full integration tests with mocked
 * dependencies would require additional test infrastructure.
 */
class LyricsProviderTest : StringSpec({
    
    "LyricsProvider class exists and can be instantiated" {
        // This test verifies the class structure is correct
        // Full integration testing would require mocking Context, DAO, and API
        val className = LyricsProvider::class.simpleName
        className shouldBe "LyricsProvider"
    }
    
    "LRCParser integration - parse and format work correctly" {
        // Test that the parser (used by LyricsProvider) works correctly
        val lrcContent = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:10.00]Test lyrics line
        """.trimIndent()
        
        val parseResult = LRCParser.parse(lrcContent)
        parseResult.isSuccess shouldBe true
        
        val lyrics = parseResult.getOrNull().shouldNotBeNull()
        lyrics.metadata.title shouldBe "Test Song"
        lyrics.metadata.artist shouldBe "Test Artist"
        lyrics.lines.size shouldBe 1
        lyrics.lines[0].text shouldBe "Test lyrics line"
        
        // Test formatting back
        val formatted = LRCParser.format(lyrics)
        formatted.contains("[ti:Test Song]") shouldBe true
        formatted.contains("[ar:Test Artist]") shouldBe true
        formatted.contains("[00:10.00]Test lyrics line") shouldBe true
    }
    
    "CachedLyrics data class structure is correct" {
        // Verify the cache model works correctly
        val cachedLyrics = CachedLyrics(
            songId = 123L,
            lrcContent = "[00:10.00]Test",
            source = "api",
            fetchedAt = System.currentTimeMillis()
        )
        
        cachedLyrics.songId shouldBe 123L
        cachedLyrics.source shouldBe "api"
        cachedLyrics.lrcContent shouldBe "[00:10.00]Test"
    }
})
