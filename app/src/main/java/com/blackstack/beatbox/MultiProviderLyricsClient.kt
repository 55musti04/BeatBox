package com.blackstack.beatbox

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/*
 * Every JSON field below is annotated with @SerializedName on purpose.
 * Gson resolves fields by their *name* via reflection, and R8 renames fields
 * unless they are kept. Without the annotations the release build silently
 * deserializes every response to nulls (verified in the built APK: the field
 * names were absent from classes.dex). Keep the annotation and the value in
 * sync with the upstream JSON key.
 */

/**
 * ============================================
 * BetterLyrics API (Boidu Dev - Free TTML Lyrics)
 * ============================================
 */
data class TTMLResponse(
    @SerializedName("ttml") val ttml: String?
)

interface BetterLyricsApi {
    @GET("getLyrics")
    suspend fun getLyrics(
        @Query("s") title: String,
        @Query("a") artist: String,
        @Query("d") duration: Int? = null,
        @Query("al") album: String? = null
    ): TTMLResponse
}

/**
 * ============================================
 * LRCLIB API (Primary - Free, No API Key)
 * ============================================
 */
data class LRCLIBResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null
)

interface LRCLIBApi {
    @GET("get")
    suspend fun getExactLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LRCLIBResponse

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LRCLIBResponse>

    @GET("search")
    suspend fun searchLyricsByFields(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): List<LRCLIBResponse>
}

/**
 * ============================================
 * Lyrics.ovh API (Backup - Free, No API Key)
 * ============================================
 */
data class LyricsOvhResponse(
    @SerializedName("lyrics") val lyrics: String?
)

interface LyricsOvhApi {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsOvhResponse
}

/**
 * ============================================
 * Musixmatch API (Unofficial - Free, No API Key)
 * ============================================
 */
data class MusixmatchSearchResponse(
    @SerializedName("message") val message: MusixmatchMessage?
)

data class MusixmatchMessage(
    @SerializedName("body") val body: MusixmatchBody?
)

data class MusixmatchBody(
    @SerializedName("track_list") val track_list: List<MusixmatchTrackWrapper>?
)

data class MusixmatchTrackWrapper(
    @SerializedName("track") val track: MusixmatchTrack?
)

data class MusixmatchTrack(
    @SerializedName("track_id") val track_id: Long?,
    @SerializedName("track_name") val track_name: String?,
    @SerializedName("artist_name") val artist_name: String?,
    @SerializedName("has_lyrics") val has_lyrics: Int?
)

data class MusixmatchLyricsResponse(
    @SerializedName("message") val message: MusixmatchLyricsMessage?
)

data class MusixmatchLyricsMessage(
    @SerializedName("body") val body: MusixmatchLyricsBody?
)

data class MusixmatchLyricsBody(
    @SerializedName("lyrics") val lyrics: MusixmatchLyrics?
)

data class MusixmatchLyrics(
    @SerializedName("lyrics_body") val lyrics_body: String?
)

interface MusixmatchApi {
    @GET("track.search")
    suspend fun searchTrack(
        @Query("q_track") track: String,
        @Query("q_artist") artist: String,
        @Query("page_size") pageSize: Int = 5,
        @Query("s_track_rating") sortByRating: String = "desc",
        @Query("apikey") apiKey: String = BuildConfig.MUSIXMATCH_API_KEY
    ): MusixmatchSearchResponse
    
    @GET("track.lyrics.get")
    suspend fun getLyrics(
        @Query("track_id") trackId: Long,
        @Query("apikey") apiKey: String = BuildConfig.MUSIXMATCH_API_KEY
    ): MusixmatchLyricsResponse
}

/**
 * ============================================
 * Netease Cloud Music API (Chinese - Free)
 * ============================================
 */
data class NeteaseSearchResponse(
    @SerializedName("result") val result: NeteaseResult?
)

data class NeteaseResult(
    @SerializedName("songs") val songs: List<NeteaseSong>?
)

data class NeteaseSong(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("artists") val artists: List<NeteaseArtist>?
)

data class NeteaseArtist(
    @SerializedName("name") val name: String?
)

data class NeteaseLyricsResponse(
    @SerializedName("lrc") val lrc: NeteaseLrc?
)

data class NeteaseLrc(
    @SerializedName("lyric") val lyric: String?
)

interface NeteaseApi {
    @GET("search")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1
    ): NeteaseSearchResponse
    
    @GET("lyric")
    suspend fun getLyrics(
        @Query("id") id: Long
    ): NeteaseLyricsResponse
}

/**
 * ============================================
 * Genius API (Free - No API Key for search)
 * ============================================
 */
data class GeniusSearchResponse(
    @SerializedName("response") val response: GeniusResponseData?
)

data class GeniusResponseData(
    @SerializedName("hits") val hits: List<GeniusHit>?
)

data class GeniusHit(
    @SerializedName("result") val result: GeniusResult?
)

data class GeniusResult(
    @SerializedName("url") val url: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("primary_artist") val primary_artist: GeniusArtist?
)

data class GeniusArtist(
    @SerializedName("name") val name: String?
)

interface GeniusApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String
    ): GeniusSearchResponse
}

/**
 * ============================================
 * Multi-Provider Lyrics Client
 * ============================================
 * Attempts to fetch lyrics from multiple sources with fallback strategy
 */
object MultiProviderLyricsClient {
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val READ_TIMEOUT_SECONDS = 12L

    /**
     * Shared OkHttp client for all providers
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "BeatBox/3.0 (Android Music Player)")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .apply {
                // BASIC level logs the full request line, which for Musixmatch
                // contains the api key query parameter. Keep it out of release
                // builds so the key never reaches logcat.
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    /**
     * LRCLIB API - Primary provider (synced lyrics)
     */
    val lrclibApi: LRCLIBApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LRCLIBApi::class.java)
    }

    /**
     * Lyrics.ovh API - Secondary provider (plain lyrics)
     */
    val lyricsOvhApi: LyricsOvhApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.lyrics.ovh/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsOvhApi::class.java)
    }
    
    /**
     * BetterLyrics API - TTML lyrics provider
     */
    val betterLyricsApi: BetterLyricsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lyrics-api.boidu.dev/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BetterLyricsApi::class.java)
    }

    /**
     * Musixmatch API - Tertiary provider (best lyrics database, works great for Turkish music)
     */
    val musixmatchApi: MusixmatchApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.musixmatch.com/ws/1.1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusixmatchApi::class.java)
    }
    
    /**
     * Netease Cloud Music API - Fourth provider (synced lyrics, great for Chinese/Asian music)
     */
    val neteaseApi: NeteaseApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://music.163.com/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NeteaseApi::class.java)
    }
    
    /**
     * Genius API - Fourth provider (web scraping for lyrics, good coverage)
     */
    val geniusApi: GeniusApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.genius.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeniusApi::class.java)
    }

    // Cache failed searches to avoid spamming APIs
    private val failedSearchCache = ConcurrentHashMap<String, Long>()
    private val failureCacheTimeMs = 60000L // 1 minute
    
    /**
     * Try all providers in order until one succeeds
     * @return Pair of (lyrics text, source name)
     */
    suspend fun fetchLyricsFromAllProviders(
        trackName: String,
        artistName: String,
        duration: Long,
        skipCache: Boolean = false
    ): Pair<String?, String>? {
        val cacheKey = "$trackName-$artistName".lowercase().trim()
        if (!skipCache) {
            val lastFailure = failedSearchCache[cacheKey]
            if (lastFailure != null && System.currentTimeMillis() - lastFailure < failureCacheTimeMs) {
                Log.d("MultiProvider", "Skipping search - failed recently (${System.currentTimeMillis() - lastFailure}ms ago)")
                return null
            }
        }
        
        android.util.Log.d("MultiProvider", "=== Trying all providers for: $trackName - $artistName ===")
        
        // Provider 1: LRCLIB (Best - has synced lyrics)
        try {
            Log.d("MultiProvider", "Trying LRCLIB exact match...")
            val durationSec = (duration / 1000).toInt().takeIf { it > 0 }
            
            // 1a. Try getExactLyrics WITH duration
            var exactResponse = if (durationSec != null && artistName.isNotBlank()) {
                try {
                    lrclibApi.getExactLyrics(
                        trackName = trackName,
                        artistName = artistName,
                        duration = durationSec
                    )
                } catch (e: Exception) { null }
            } else null

            // 1b. If failed, try getExactLyrics WITHOUT duration
            if (exactResponse?.syncedLyrics.isNullOrBlank() && exactResponse?.plainLyrics.isNullOrBlank() && artistName.isNotBlank()) {
                exactResponse = try {
                    lrclibApi.getExactLyrics(
                        trackName = trackName,
                        artistName = artistName,
                        duration = null
                    )
                } catch (e: Exception) { null }
            }

            exactResponse?.syncedLyrics?.takeIf { it.isNotBlank() }?.let {
                Log.d("MultiProvider", "✓ Found synced lyrics from LRCLIB (exact match)")
                failedSearchCache.remove(cacheKey)
                return Pair(it, "LRCLIB")
            }
            exactResponse?.plainLyrics?.takeIf { it.isNotBlank() }?.let {
                Log.d("MultiProvider", "✓ Found plain lyrics from LRCLIB (exact match)")
                failedSearchCache.remove(cacheKey)
                return Pair(it, "LRCLIB")
            }

            // 1c. LRCLIB Field Search (track_name & artist_name)
            if (artistName.isNotBlank()) {
                Log.d("MultiProvider", "Trying LRCLIB field search...")
                val fieldResults = try {
                    lrclibApi.searchLyricsByFields(trackName = trackName, artistName = artistName)
                } catch (e: Exception) { emptyList() }

                for (item in fieldResults) {
                    item.syncedLyrics?.takeIf { it.isNotBlank() }?.let {
                        Log.d("MultiProvider", "✓ Found synced lyrics from LRCLIB field search")
                        failedSearchCache.remove(cacheKey)
                        return Pair(it, "LRCLIB")
                    }
                    item.plainLyrics?.takeIf { it.isNotBlank() }?.let {
                        Log.d("MultiProvider", "✓ Found plain lyrics from LRCLIB field search")
                        failedSearchCache.remove(cacheKey)
                        return Pair(it, "LRCLIB")
                    }
                }
            }

            // 1d. LRCLIB General Query Search
            val queryStr = if (artistName.isNotBlank()) "$trackName $artistName".trim() else trackName.trim()
            Log.d("MultiProvider", "Trying LRCLIB query search: '$queryStr'")
            val searchResults = try {
                lrclibApi.searchLyrics(query = queryStr)
            } catch (e: Exception) { emptyList() }

            for (item in searchResults) {
                item.syncedLyrics?.takeIf { it.isNotBlank() }?.let {
                    Log.d("MultiProvider", "✓ Found synced lyrics from LRCLIB query search")
                    failedSearchCache.remove(cacheKey)
                    return Pair(it, "LRCLIB")
                }
                item.plainLyrics?.takeIf { it.isNotBlank() }?.let {
                    Log.d("MultiProvider", "✓ Found plain lyrics from LRCLIB query search")
                    failedSearchCache.remove(cacheKey)
                    return Pair(it, "LRCLIB")
                }
            }

            // 1e. Track-only Query Search
            if (artistName.isNotBlank()) {
                Log.d("MultiProvider", "Trying LRCLIB query search with track alone: '$trackName'")
                val trackOnlyResults = try {
                    lrclibApi.searchLyrics(query = trackName)
                } catch (e: Exception) { emptyList() }

                for (item in trackOnlyResults) {
                    item.syncedLyrics?.takeIf { it.isNotBlank() }?.let {
                        Log.d("MultiProvider", "✓ Found synced lyrics from LRCLIB track-only search")
                        failedSearchCache.remove(cacheKey)
                        return Pair(it, "LRCLIB")
                    }
                    item.plainLyrics?.takeIf { it.isNotBlank() }?.let {
                        Log.d("MultiProvider", "✓ Found plain lyrics from LRCLIB track-only search")
                        failedSearchCache.remove(cacheKey)
                        return Pair(it, "LRCLIB")
                    }
                }
            }
            android.util.Log.d("MultiProvider", "✗ LRCLIB returned no lyrics")
        } catch (e: Exception) {
            Log.e("MultiProvider", "Error in LRCLIB search", e)
        }

        // Provider 1.5: BetterLyrics (TTML API)
        try {
            Log.d("MultiProvider", "Trying BetterLyrics API...")
            val durationSec = (duration / 1000).toInt().takeIf { it > 0 }
            val ttmlResp = betterLyricsApi.getLyrics(
                title = trackName,
                artist = artistName,
                duration = durationSec
            )
            val ttmlText = ttmlResp.ttml?.trim()?.takeIf { it.isNotEmpty() }
            if (ttmlText != null) {
                val parsed = TTMLParser.parseTTML(ttmlText)
                if (parsed.isNotEmpty()) {
                    val lrc = TTMLParser.toLRC(parsed)
                    Log.d("MultiProvider", "✓ Found synchronized lyrics from BetterLyrics (TTML)")
                    failedSearchCache.remove(cacheKey)
                    return Pair(lrc, "BetterLyrics")
                }
            }
        } catch (e: Exception) {
            Log.d("MultiProvider", "BetterLyrics failed: ${e.message}")
        }
        
        // Provider 2: Lyrics.ovh
        try {
            android.util.Log.d("MultiProvider", "Trying Lyrics.ovh...")
            val cleanTitleForOvh = cleanTitle(trackName)
            val cleanArtistForOvh = cleanArtist(artistName)
            val response = lyricsOvhApi.getLyrics(
                artist = cleanArtistForOvh,
                title = cleanTitleForOvh
            )
            
            response.lyrics?.takeIf { it.isNotBlank() }?.let {
                Log.d("MultiProvider", "✓ Found lyrics from Lyrics.ovh")
                failedSearchCache.remove(cacheKey)
                return Pair(it, "Lyrics.ovh")
            }
            android.util.Log.d("MultiProvider", "✗ Lyrics.ovh returned no lyrics")
        } catch (e: Exception) {
            android.util.Log.e("MultiProvider", "✗ Lyrics.ovh failed: ${e.message}")
        }
        
        // Provider 3: Musixmatch (Best for Turkish music and popular songs)
        try {
            android.util.Log.d("MultiProvider", "Trying Musixmatch...")
            val searchResponse = musixmatchApi.searchTrack(
                track = trackName,
                artist = artistName
            )
            
            val track = searchResponse.message?.body?.track_list?.firstOrNull()?.track
            if (track?.track_id != null && track.has_lyrics == 1) {
                val lyricsResponse = musixmatchApi.getLyrics(track.track_id)
                lyricsResponse.message?.body?.lyrics?.lyrics_body?.takeIf { it.isNotBlank() }?.let { lyrics ->
                    Log.d("MultiProvider", "✓ Found lyrics from Musixmatch")
                    failedSearchCache.remove(cacheKey)
                    return Pair(lyrics, "Musixmatch")
                }
            }
            android.util.Log.d("MultiProvider", "✗ Musixmatch returned no lyrics")
        } catch (e: Exception) {
            android.util.Log.e("MultiProvider", "✗ Musixmatch failed: ${e.message}")
        }
        
        // Provider 4: Netease Cloud Music (Great for Asian music)
        try {
            android.util.Log.d("MultiProvider", "Trying Netease Cloud Music...")
            val searchResponse = neteaseApi.search("$trackName $artistName")
            
            val songId = searchResponse.result?.songs?.firstOrNull()?.id
            if (songId != null) {
                val lyricsResponse = neteaseApi.getLyrics(songId)
                lyricsResponse.lrc?.lyric?.takeIf { it.isNotBlank() }?.let {
                    Log.d("MultiProvider", "✓ Found synced lyrics from Netease")
                    failedSearchCache.remove(cacheKey)
                    return Pair(it, "Netease Cloud Music")
                }
            }
            android.util.Log.d("MultiProvider", "✗ Netease returned no lyrics")
        } catch (e: Exception) {
            android.util.Log.e("MultiProvider", "✗ Netease failed: ${e.message}")
        }
        
        // All providers failed - cache failure only if not skipping cache
        if (!skipCache) {
            failedSearchCache[cacheKey] = System.currentTimeMillis()
        }
        Log.e("MultiProvider", "✗ All providers failed for: $trackName - $artistName")
        return null
    }
    
    /**
     * Try fetching with different name variations
     */
    suspend fun fetchWithVariations(
        originalTrack: String,
        originalArtist: String,
        duration: Long
    ): Pair<String?, String>? {
        // Try 1: Original names
        fetchLyricsFromAllProviders(originalTrack, originalArtist, duration, skipCache = true)?.let {
            return it
        }
        
        // Try 2: Cleaned names
        val cleanedTrack = cleanTitle(originalTrack)
        val cleanedArtist = cleanArtist(originalArtist)
        if (cleanedTrack != originalTrack || cleanedArtist != originalArtist) {
            fetchLyricsFromAllProviders(cleanedTrack, cleanedArtist, duration, skipCache = true)?.let {
                return it
            }
        }
        
        // Try 3: Smart parsed
        val (smartTrack, smartArtist) = smartParseTitleAndArtist(originalTrack, originalArtist)
        if (smartTrack != cleanedTrack || smartArtist != cleanedArtist) {
            fetchLyricsFromAllProviders(smartTrack, smartArtist, duration, skipCache = true)?.let {
                return it
            }
        }
        
        // Try 4: Without duration
        fetchLyricsFromAllProviders(smartTrack, smartArtist, 0, skipCache = true)?.let {
            return it
        }
        
        return null
    }
    
    /**
     * Clean title - remove video tags, file extensions, and track numbers
     */
    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        
        // Remove file extensions (.mp3, .flac, .wav, .m4a, .aac, .ogg)
        cleaned = cleaned.replace(Regex("""\.(mp3|flac|wav|m4a|aac|ogg|wma|opus)$""", RegexOption.IGNORE_CASE), "")

        // Remove leading track numbers (e.g. "01. ", "01 - ", "1 - ")
        cleaned = cleaned.replace(Regex("""^\d+[\s.-]+"""), "")

        val patternsToRemove = listOf(
            Regex("""\s*[\(\[](Official\s+)?(Video|Audio|Music\s+Video|Lyric\s+Video|MV|Remix|Cover)[\)\]]\s*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*[\(\[](HD|HQ|4K|1080p|720p)[\)\]]\s*$""", RegexOption.IGNORE_CASE)
        )
        
        patternsToRemove.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "")
        }
        
        cleaned = cleaned.replace(Regex("""\s*[-|]\s*$"""), "")
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        return cleaned
    }
    
    /**
     * Clean artist name
     */
    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        
        // Clear unknown artist placeholders
        val unknownPlaceholders = listOf("<unknown>", "unknown", "<bilinmeyen sanatçı>", "bilinmeyen sanatçı", "unknown artist", "<unknown artist>")
        if (unknownPlaceholders.any { cleaned.lowercase() == it }) {
            return ""
        }

        val suffixPatterns = listOf(
            Regex("""\s*-\s*(VEVO|Topic)\s*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*[\(\[](VEVO|Topic)[\)\]]\s*$""", RegexOption.IGNORE_CASE)
        )
        
        suffixPatterns.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "")
        }
        
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        return cleaned
    }
    
    /**
     * Smart parse title and artist from metadata
     */
    private fun smartParseTitleAndArtist(title: String, artist: String): Pair<String, String> {
        val cleanedArt = cleanArtist(artist)
        val cleanedTit = cleanTitle(title)

        val isUnknownArtist = cleanedArt.isEmpty() || 
                           cleanedArt.contains("VEVO", ignoreCase = true) || 
                           cleanedArt.contains("Topic", ignoreCase = true) ||
                           cleanedArt.contains("Official", ignoreCase = true)
        
        if ((isUnknownArtist || cleanedArt.isEmpty()) && cleanedTit.contains(" - ")) {
            val parts = cleanedTit.split(" - ").map { it.trim() }
            if (parts.size >= 2) {
                return Pair(cleanTitle(parts[1]), cleanArtist(parts[0]))
            }
        }
        
        return Pair(cleanedTit, cleanedArt)
    }
}
