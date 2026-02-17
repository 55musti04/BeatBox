package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Unit tests for LRCParser
 */
class LRCParserTest : StringSpec({
    
    "parse valid LRC with timestamps and metadata" {
        val lrcContent = """
            [ti:Test Song]
            [ar:Test Artist]
            [al:Test Album]
            
            [00:12.00]First line of lyrics
            [00:17.20]Second line of lyrics
            [00:21.10]Third line of lyrics
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        // Check metadata
        lyrics.metadata.title shouldBe "Test Song"
        lyrics.metadata.artist shouldBe "Test Artist"
        lyrics.metadata.album shouldBe "Test Album"
        
        // Check lines
        lyrics.lines.size shouldBe 3
        lyrics.lines[0].timestampMs shouldBe 12000L
        lyrics.lines[0].text shouldBe "First line of lyrics"
        lyrics.lines[1].timestampMs shouldBe 17200L
        lyrics.lines[1].text shouldBe "Second line of lyrics"
        lyrics.lines[2].timestampMs shouldBe 21100L
        lyrics.lines[2].text shouldBe "Third line of lyrics"
    }
    
    "parse LRC with milliseconds format" {
        val lrcContent = """
            [00:12.000]First line
            [00:17.250]Second line
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        lyrics.lines.size shouldBe 2
        lyrics.lines[0].timestampMs shouldBe 12000L
        lyrics.lines[1].timestampMs shouldBe 17250L
    }
    
    "parse LRC with timestamps out of order" {
        val lrcContent = """
            [00:21.10]Third line
            [00:12.00]First line
            [00:17.20]Second line
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        // Should be sorted by timestamp
        lyrics.lines.size shouldBe 3
        lyrics.lines[0].timestampMs shouldBe 12000L
        lyrics.lines[0].text shouldBe "First line"
        lyrics.lines[1].timestampMs shouldBe 17200L
        lyrics.lines[1].text shouldBe "Second line"
        lyrics.lines[2].timestampMs shouldBe 21100L
        lyrics.lines[2].text shouldBe "Third line"
    }
    
    "parse LRC with malformed lines skips them gracefully" {
        val lrcContent = """
            [00:12.00]Valid line
            [invalid timestamp]Should be skipped
            Not a timestamp at all
            [00:17.20]Another valid line
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        // Should only have 2 valid lines
        lyrics.lines.size shouldBe 2
        lyrics.lines[0].text shouldBe "Valid line"
        lyrics.lines[1].text shouldBe "Another valid line"
    }
    
    "parse LRC with offset applies offset to timestamps" {
        val lrcContent = """
            [offset:500]
            [00:12.00]First line
            [00:17.20]Second line
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        lyrics.metadata.offset shouldBe 500L
        // Timestamps should have offset applied
        lyrics.lines[0].timestampMs shouldBe 12500L
        lyrics.lines[1].timestampMs shouldBe 17700L
    }
    
    "parse empty LRC returns empty lyrics" {
        val lrcContent = ""
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        lyrics.lines.size shouldBe 0
    }
    
    "parse LRC with only metadata returns empty lines" {
        val lrcContent = """
            [ti:Test Song]
            [ar:Test Artist]
        """.trimIndent()
        
        val result = LRCParser.parse(lrcContent)
        
        result.isSuccess shouldBe true
        val lyrics = result.getOrNull().shouldNotBeNull()
        
        lyrics.metadata.title shouldBe "Test Song"
        lyrics.metadata.artist shouldBe "Test Artist"
        lyrics.lines.size shouldBe 0
    }
    
    "format creates valid LRC string" {
        val lyrics = SyncedLyrics(
            lines = listOf(
                SyncedLyricsLine(12000L, "First line"),
                SyncedLyricsLine(17200L, "Second line"),
                SyncedLyricsLine(21100L, "Third line")
            ),
            metadata = LyricsMetadata(
                title = "Test Song",
                artist = "Test Artist",
                album = "Test Album"
            )
        )
        
        val lrcString = LRCParser.format(lyrics)
        
        lrcString.contains("[ti:Test Song]") shouldBe true
        lrcString.contains("[ar:Test Artist]") shouldBe true
        lrcString.contains("[al:Test Album]") shouldBe true
        lrcString.contains("[00:12.00]First line") shouldBe true
        lrcString.contains("[00:17.20]Second line") shouldBe true
        lrcString.contains("[00:21.10]Third line") shouldBe true
    }
    
    "round trip parse and format preserves data" {
        val originalLrc = """
            [ti:Test Song]
            [ar:Test Artist]
            
            [00:12.00]First line
            [00:17.20]Second line
        """.trimIndent()
        
        val parsed = LRCParser.parse(originalLrc).getOrNull().shouldNotBeNull()
        val formatted = LRCParser.format(parsed)
        val reparsed = LRCParser.parse(formatted).getOrNull().shouldNotBeNull()
        
        // Check that data is preserved
        parsed.metadata.title shouldBe reparsed.metadata.title
        parsed.metadata.artist shouldBe reparsed.metadata.artist
        parsed.lines.size shouldBe reparsed.lines.size
        
        for (i in parsed.lines.indices) {
            parsed.lines[i].timestampMs shouldBe reparsed.lines[i].timestampMs
            parsed.lines[i].text shouldBe reparsed.lines[i].text
        }
    }
})
