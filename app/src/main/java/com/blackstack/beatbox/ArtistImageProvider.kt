package com.blackstack.beatbox

import androidx.core.content.edit
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val ARTIST_PHOTO_CACHE_PREF = "artist_photo_cache_v1"

object ArtistImageProvider {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Limit concurrent artist photo downloads to 4
    private val downloadSemaphore = Semaphore(4)

    // Memory cache for decoded artist bitmaps
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory for cache

    val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    /**
     * Gets cached artist image URL or fetches online from Deezer API / iTunes API
     */
    suspend fun getArtistImageUrl(context: Context, artistName: String): String? {
        val cleanName = artistName.trim().lowercase()
        if (cleanName.isBlank() || cleanName == "unknown artist" || cleanName == "<unknown>") return null

        val prefs = context.getSharedPreferences(ARTIST_PHOTO_CACHE_PREF, Context.MODE_PRIVATE)
        val cachedUrl = prefs.getString(cleanName, null)
        if (cachedUrl != null) {
            return if (cachedUrl == "NONE") null else cachedUrl
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Try Deezer API
                val encodedName = URLEncoder.encode(artistName, "UTF-8")
                val deezerUrl = "https://api.deezer.com/search/artist?q=$encodedName"
                val deezerReq = Request.Builder().url(deezerUrl).build()

                httpClient.newCall(deezerReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val data = json.optJSONArray("data")
                            if (data != null && data.length() > 0) {
                                val firstArtist = data.getJSONObject(0)
                                val img = firstArtist.optString("picture_big", "").ifEmpty {
                                    firstArtist.optString("picture_medium", "")
                                }
                                if (img.isNotBlank()) {
                                    prefs.edit { putString(cleanName, img) }
                                    return@withContext img
                                }
                            }
                        }
                    }
                }

                // 2. Fallback to iTunes Search API
                val itunesUrl = "https://itunes.apple.com/search?term=$encodedName&entity=song&limit=1"
                val itunesReq = Request.Builder().url(itunesUrl).build()

                httpClient.newCall(itunesReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val obj = results.getJSONObject(0)
                                val artwork100 = obj.optString("artworkUrl100", "")
                                if (artwork100.isNotBlank()) {
                                    val highResUrl = artwork100.replace("100x100bb", "600x600bb")
                                    prefs.edit { putString(cleanName, highResUrl) }
                                    return@withContext highResUrl
                                }
                            }
                        }
                    }
                }

                // No image found, cache "NONE" to prevent re-querying
                prefs.edit { putString(cleanName, "NONE") }
                null
            } catch (e: Exception) {
                Log.w("ArtistImageProvider", "Error fetching artist image for $artistName: ${e.message}")
                null
            }
        }
    }

    /**
     * Downloads and decodes artist image bitmap with caching
     */
    suspend fun loadArtistBitmap(context: Context, artistName: String, isHQ: Boolean = false): Bitmap? {
        val cacheKey = "${artistName.trim().lowercase()}_${if (isHQ) "hq" else "thumb"}"
        bitmapCache.get(cacheKey)?.let { return it }

        val imageUrl = getArtistImageUrl(context, artistName) ?: return null

        return downloadSemaphore.withPermit {
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(imageUrl).build()
                    httpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = if (isHQ) 1 else 2
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                if (bitmap != null) {
                                    bitmapCache.put(cacheKey, bitmap)
                                    return@withContext bitmap
                                }
                            }
                        }
                    }
                    null
                } catch (e: Exception) {
                    Log.w("ArtistImageProvider", "Failed to download bitmap from $imageUrl: ${e.message}")
                    null
                }
            }
        }
    }
}

/**
     * Helper to generate a vibrant gradient background from artist name
     */
private fun artistGradient(artistName: String): Brush {
    val h = artistName.hashCode()
    val r1 = ((h and 0xFF0000) shr 16) % 200 + 40
    val g1 = ((h and 0x00FF00) shr 8) % 200 + 40
    val b1 = (h and 0x0000FF) % 200 + 40

    val c1 = Color(r1, g1, b1)
    val c2 = Color((r1 + 60) % 255, (g1 + 40) % 255, (b1 + 80) % 255)

    return Brush.linearGradient(listOf(c1, c2))
}

@Composable
fun ArtistImage(
    artistName: String,
    modifier: Modifier = Modifier,
    isHQ: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop
) {
    val ctx = LocalContext.current
    var bitmap by remember(artistName) {
        val key = "${artistName.trim().lowercase()}_${if (isHQ) "hq" else "thumb"}"
        mutableStateOf<Bitmap?>(ArtistImageProvider.bitmapCache.get(key))
    }
    var isLoading by remember(artistName) { mutableStateOf(bitmap == null) }

    LaunchedEffect(artistName) {
        if (bitmap == null) {
            isLoading = true
            val loadedBmp = ArtistImageProvider.loadArtistBitmap(ctx, artistName, isHQ)
            bitmap = loadedBmp
            isLoading = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = artistName,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder gradient with initial letter
            val initial = artistName.trim().take(1).uppercase().ifEmpty { "?" }
            val bgGradient = remember(artistName) { artistGradient(artistName) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = initial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
