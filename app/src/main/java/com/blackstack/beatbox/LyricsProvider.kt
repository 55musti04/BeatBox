package com.blackstack.beatbox

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider for retrieving synchronized lyrics from multiple sources
 * 
 * Checks sources in priority order:
 * 1. Local LRC file (same directory as audio file)
 * 2. Cache (Room database)
 * 3. Online API (LRCLIB)
 * 
 * All operations are executed on Dispatchers.IO for background processing.
 */
class LyricsProvider(
    private val context: Context,
    private val lyricsCache: LyricsCacheDao,
    private val lrcLibApi: LRCLIBApi
) {
    /**
     * Retrieves lyrics for a song, checking sources in priority order
     * 
     * @param audio The audio file to get lyrics for
     * @return Result containing SyncedLyrics on success, or error on failure
     */
    suspend fun getLyrics(audio: AudioFile): Result<SyncedLyrics> = withContext(Dispatchers.IO) {
        try {
            // Priority 1: Check for local LRC file
            checkLocalFile(audio)?.let { lyrics ->
                return@withContext Result.success(lyrics)
            }
            
            // Priority 2: Check cache
            checkCache(audio.id)?.let { lyrics ->
                return@withContext Result.success(lyrics)
            }
            
            // Priority 3: Fetch from API
            fetchFromApi(audio)?.let { lyrics ->
                // Cache the fetched lyrics
                cacheLyrics(audio.id, lyrics, "api")
                return@withContext Result.success(lyrics)
            }
            
            // All sources failed
            Result.failure(Exception("No lyrics found from any source"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks for a local LRC file alongside the audio file
     * 
     * @param audio The audio file to check for
     * @return SyncedLyrics if local file exists and is valid, null otherwise
     */
    private suspend fun checkLocalFile(audio: AudioFile): SyncedLyrics? {
        return try {
            val lrcContent = LocalFileReader.readLrcFile(audio.uri) ?: return null
            val parseResult = LRCParser.parse(lrcContent)
            parseResult.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks the cache for previously fetched lyrics
     * 
     * @param songId The unique identifier of the song
     * @return SyncedLyrics if cached lyrics exist and are valid, null otherwise
     */
    private suspend fun checkCache(songId: Long): SyncedLyrics? {
        return try {
            val cachedLyrics = lyricsCache.getLyrics(songId) ?: return null
            val parseResult = LRCParser.parse(cachedLyrics.lrcContent)
            parseResult.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetches lyrics from the LRCLIB API
     * 
     * @param audio The audio file to fetch lyrics for
     * @return SyncedLyrics if API fetch succeeds and response is valid, null otherwise
     */
    private suspend fun fetchFromApi(audio: AudioFile): SyncedLyrics? {
        return try {
            val response = lrcLibApi.searchLyrics(
                trackName = audio.title,
                artistName = audio.artist,
                duration = (audio.duration / 1000).toInt() // Convert ms to seconds
            )
            
            // Prefer synced lyrics over plain lyrics
            val lrcContent = response.syncedLyrics ?: return null
            
            val parseResult = LRCParser.parse(lrcContent)
            parseResult.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Caches lyrics in the local database
     * 
     * @param songId The unique identifier of the song
     * @param lyrics The lyrics to cache
     * @param source The source of the lyrics ("local" or "api")
     */
    private suspend fun cacheLyrics(songId: Long, lyrics: SyncedLyrics, source: String) {
        try {
            val lrcContent = LRCParser.format(lyrics)
            val cachedLyrics = CachedLyrics(
                songId = songId,
                lrcContent = lrcContent,
                source = source,
                fetchedAt = System.currentTimeMillis()
            )
            lyricsCache.insertLyrics(cachedLyrics)
        } catch (e: Exception) {
            // Log error but don't fail the operation
            // Caching is not critical to the lyrics retrieval flow
        }
    }
}
