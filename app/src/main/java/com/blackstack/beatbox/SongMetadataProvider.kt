package com.blackstack.beatbox

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Deezer (öncelikli) ve iTunes (yedek) üzerinden şarkı sanatçısı ve albüm
 * kapağı arar. API anahtarı gerektirmez; sonuçlar [SongMetadataMatcher] ile
 * yerel şarkıya karşı puanlanır.
 */
object SongMetadataProvider {

    private const val USER_AGENT = "BeatBox/3.0 (Android)"
    private const val DEEZER_LIMIT = 8
    private const val ITUNES_LIMIT = 8

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val downloadSemaphore = Semaphore(3)
    private val keyLocks = mutableMapOf<String, Mutex>()
    private val keyLocksGuard = Mutex()

    /**
     * Arama başarısızsa [Result.failure], hiç sonuç yoksa boş liste döner.
     * Bu ayrım, ağ hatasının "bulunamadı" olarak önbelleğe yazılmasını önler.
     */
    suspend fun search(title: String, artist: String?): Result<List<SongMetadataCandidate>> =
        withContext(Dispatchers.IO) {
            val query = buildQuery(title, artist)
            if (query.isBlank()) return@withContext Result.success(emptyList())

            var lastError: Exception? = null

            try {
                val deezer = searchDeezer(query)
                if (deezer.isNotEmpty()) return@withContext Result.success(deezer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w("SongMetadata", "Deezer araması başarısız: ${e.message}")
            }

            try {
                return@withContext Result.success(searchItunes(query))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w("SongMetadata", "iTunes araması başarısız: ${e.message}")
            }

            Result.failure(lastError ?: IllegalStateException("Şarkı bilgisi araması başarısız"))
        }

    private fun buildQuery(title: String, artist: String?): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist?.trim().orEmpty()
        if (cleanTitle.isBlank()) return cleanArtist
        if (cleanArtist.isBlank() || SongMetadataMatcher.isUnknownArtist(cleanArtist)) {
            return cleanTitle
        }
        return if (cleanTitle.lowercase().contains(cleanArtist.lowercase())) {
            cleanTitle
        } else {
            "$cleanArtist $cleanTitle"
        }
    }

    private fun searchDeezer(query: String): List<SongMetadataCandidate> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.deezer.com/search?q=$encoded&limit=$DEEZER_LIMIT"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Deezer HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()
            val json = JSONObject(body)
            if (json.has("error")) error("Deezer API hatası")
            val data = json.optJSONArray("data") ?: return emptyList()

            return buildList {
                for (index in 0 until data.length()) {
                    val item = data.optJSONObject(index) ?: continue
                    val album = item.optJSONObject("album")
                    val artist = item.optJSONObject("artist")
                    val title = item.optString("title").trim()
                    val artistName = artist?.optString("name")?.trim().orEmpty()
                    if (title.isBlank() || artistName.isBlank()) continue

                    val cover = album?.optString("cover_big").orEmpty().ifBlank {
                        album?.optString("cover_medium").orEmpty()
                    }.ifBlank {
                        album?.optString("cover_xl").orEmpty()
                    }

                    add(
                        SongMetadataCandidate(
                            title = title,
                            artist = artistName,
                            durationSeconds = item.optInt("duration"),
                            artworkUrl = cover.ifBlank { null },
                            source = "deezer"
                        )
                    )
                }
            }
        }
    }

    private fun searchItunes(query: String): List<SongMetadataCandidate> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$ITUNES_LIMIT"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("iTunes HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()

            return buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val title = item.optString("trackName").trim()
                    val artistName = item.optString("artistName").trim()
                    if (title.isBlank() || artistName.isBlank()) continue

                    val artwork = item.optString("artworkUrl100")
                        .replace("100x100bb", "600x600bb")
                        .ifBlank { null }

                    add(
                        SongMetadataCandidate(
                            title = title,
                            artist = artistName,
                            durationSeconds = (item.optLong("trackTimeMillis") / 1000L).toInt(),
                            artworkUrl = artwork,
                            source = "itunes"
                        )
                    )
                }
            }
        }
    }

    /**
     * Kapak görselini indirip uygulamanın özel dizinine kaydeder. Aynı anahtar
     * için daha önce indirilmiş geçerli bir dosya varsa yeniden indirmez.
     */
    suspend fun downloadArtwork(context: Context, url: String, cacheKey: String): String? {
        // Aynı şarkı için sanatçı zenginleştirmesi ve AlbumArtImage aynı anda
        // indirme başlatabilir; geçici dosya yarışını anahtar bazlı kilitle önle.
        val keyLock = keyLocksGuard.withLock {
            keyLocks.getOrPut(cacheKey) { Mutex() }
        }
        return keyLock.withLock {
            downloadSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    try {
                        val directory = File(context.filesDir, "song_art").apply { mkdirs() }
                        val target = File(directory, "$cacheKey.jpg")
                        if (target.exists() && target.length() > 0L) {
                            return@withContext target.absolutePath
                        }

                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", USER_AGENT)
                            .build()

                        httpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) return@withContext null
                            val bytes = response.body?.bytes() ?: return@withContext null
                            if (bytes.isEmpty()) return@withContext null

                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                                return@withContext null
                            }

                            val temporary = File(directory, "$cacheKey.tmp")
                            temporary.writeBytes(bytes)
                            if (!temporary.renameTo(target)) {
                                temporary.copyTo(target, overwrite = true)
                                temporary.delete()
                            }
                            target.absolutePath
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("SongMetadata", "Kapak indirilemedi: ${e.message}")
                        null
                    }
                }
            }
        }
    }
}
