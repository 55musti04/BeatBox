package com.blackstack.beatbox

/**
 * Parser for LRC (Lyrics) file format
 * 
 * Supports parsing LRC format with timestamps [mm:ss.xx] or [mm:ss.xxx]
 * and metadata tags like [ti:], [ar:], [al:], [by:], [offset:]
 */
object LRCParser {
    
    // Regex patterns for parsing
    private val TIMESTAMP_PATTERN = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")
    private val METADATA_PATTERN = Regex("""^\[(ti|ar|al|by|offset):(.+)\]$""")
    
    /**
     * Parses LRC format string into SyncedLyrics
     * 
     * @param lrcContent The LRC file content as a string
     * @return Result containing SyncedLyrics on success, or error on failure
     */
    fun parse(lrcContent: String): Result<SyncedLyrics> {
        return try {
            val lines = mutableListOf<SyncedLyricsLine>()
            val metadataMap = mutableMapOf<String, String>()
            
            // Parse each line
            lrcContent.lines().forEach { line ->
                val trimmedLine = line.trim()
                
                // Try to parse as timestamp line
                val timestampMatch = TIMESTAMP_PATTERN.matchEntire(trimmedLine)
                if (timestampMatch != null) {
                    val timestamp = parseTimestamp(timestampMatch)
                    if (timestamp != null) {
                        val text = timestampMatch.groupValues[4].trim()
                        lines.add(SyncedLyricsLine(timestamp, text))
                    }
                    // If timestamp parsing fails, skip the line (malformed)
                } else {
                    // Try to parse as metadata tag
                    val metadataMatch = METADATA_PATTERN.matchEntire(trimmedLine)
                    if (metadataMatch != null) {
                        val tag = metadataMatch.groupValues[1]
                        val value = metadataMatch.groupValues[2].trim()
                        metadataMap[tag] = value
                    }
                    // If neither matches, skip the line
                }
            }
            
            // Sort lines by timestamp
            val sortedLines = lines.sortedBy { it.timestampMs }
            
            // Build metadata object
            val metadata = LyricsMetadata(
                title = metadataMap["ti"],
                artist = metadataMap["ar"],
                album = metadataMap["al"],
                by = metadataMap["by"],
                offset = metadataMap["offset"]?.toLongOrNull() ?: 0L
            )
            
            // Apply offset to all timestamps if present
            val adjustedLines = if (metadata.offset != 0L) {
                sortedLines.map { line ->
                    SyncedLyricsLine(
                        timestampMs = line.timestampMs + metadata.offset,
                        text = line.text
                    )
                }
            } else {
                sortedLines
            }
            
            Result.success(SyncedLyrics(adjustedLines, metadata))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Formats SyncedLyrics back to LRC string format
     * 
     * @param lyrics The SyncedLyrics object to format
     * @return LRC format string
     */
    fun format(lyrics: SyncedLyrics): String {
        val builder = StringBuilder()
        
        // Write metadata tags
        lyrics.metadata.title?.let { builder.appendLine("[ti:$it]") }
        lyrics.metadata.artist?.let { builder.appendLine("[ar:$it]") }
        lyrics.metadata.album?.let { builder.appendLine("[al:$it]") }
        lyrics.metadata.by?.let { builder.appendLine("[by:$it]") }
        if (lyrics.metadata.offset != 0L) {
            builder.appendLine("[offset:${lyrics.metadata.offset}]")
        }
        
        // Add blank line after metadata if present
        if (builder.isNotEmpty()) {
            builder.appendLine()
        }
        
        // Write lyrics lines
        lyrics.lines.forEach { line ->
            val timestamp = formatTimestamp(line.timestampMs)
            builder.appendLine("[$timestamp]${line.text}")
        }
        
        return builder.toString()
    }
    
    /**
     * Parses a timestamp from regex match groups
     * Format: [mm:ss.xx] or [mm:ss.xxx]
     * 
     * @param match The regex match result
     * @return Timestamp in milliseconds, or null if invalid
     */
    private fun parseTimestamp(match: MatchResult): Long? {
        return try {
            val minutes = match.groupValues[1].toInt()
            val seconds = match.groupValues[2].toInt()
            val fraction = match.groupValues[3]
            
            // Handle both .xx (centiseconds) and .xxx (milliseconds) formats
            val milliseconds = if (fraction.length == 2) {
                fraction.toInt() * 10  // centiseconds to milliseconds
            } else {
                fraction.toInt()  // already milliseconds
            }
            
            // Convert to total milliseconds
            (minutes * 60 + seconds) * 1000L + milliseconds
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Formats a timestamp in milliseconds to LRC format [mm:ss.xx]
     * 
     * @param timestampMs Timestamp in milliseconds
     * @return Formatted timestamp string (mm:ss.xx)
     */
    private fun formatTimestamp(timestampMs: Long): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (timestampMs % 1000) / 10
        
        return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
