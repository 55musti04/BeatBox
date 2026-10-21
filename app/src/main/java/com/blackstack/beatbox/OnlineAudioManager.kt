package com.blackstack.beatbox

import android.content.Context
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.Normalizer
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

private const val ONLINE_TRACKS_PREF_KEY = "online_tracks_v1"

object OnlineAudioManager {
    private val httpClient = OkHttpClient()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun loadOnlineTracks(context: Context): List<AudioFile> {
        val prefs = context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        val raw = prefs.getString(ONLINE_TRACKS_PREF_KEY, "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split("|||").mapNotNull { row ->
            val parts = row.split("::")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val artist = parts[2]
            val url = parts[3]
            val artworkUrl = parts.getOrNull(4)?.ifBlank { null }
            val duration = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            AudioFile(
                id = id,
                title = title,
                artist = artist,
                duration = duration,
                uri = Uri.parse(url),
                albumId = -1L,
                folderName = "Online",
                dateAdded = System.currentTimeMillis(),
                isOnline = true,
                artworkUrl = artworkUrl
            )
        }
    }

    fun saveOnlineTrack(
        context: Context,
        title: String,
        artist: String,
        url: String,
        artworkUrl: String?,
        durationMs: Long = 0L
    ): AudioFile {
        val prefs = context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        val current = prefs.getString(ONLINE_TRACKS_PREF_KEY, "") ?: ""
        val id = -System.currentTimeMillis()
        val row = "$id::$title::$artist::$url::${artworkUrl.orEmpty()}::$durationMs"
        val next = if (current.isBlank()) row else "$current|||$row"
        prefs.edit().putString(ONLINE_TRACKS_PREF_KEY, next).apply()
        return AudioFile(
            id = id,
            title = title,
            artist = artist,
            duration = durationMs,
            uri = Uri.parse(url),
            albumId = -1L,
            folderName = "Online",
            dateAdded = System.currentTimeMillis(),
            isOnline = true,
            artworkUrl = artworkUrl
        )
    }

    fun downloadTrack(
        context: Context,
        audioFile: AudioFile,
        scope: CoroutineScope = managerScope,
        onResult: (Boolean, String) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            try {
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: throw IOException("Music klasoru olusturulamadi")
                val beatBoxDir = File(downloadDir, "BeatBox").apply { mkdirs() }
                val safeName = sanitizeFileName("${audioFile.artist} - ${audioFile.title}")
                val outputFile = File(beatBoxDir, "$safeName.mp3")

                downloadToFile(audioFile.uri.toString(), outputFile)
                embedMetadataAndArtwork(outputFile, audioFile)
                val savedName = saveToPublicMusic(context, outputFile, "$safeName.mp3")
                outputFile.delete()
                withContext(Dispatchers.Main) {
                    onResult(true, "$savedName indirildi")
                }
            } catch (e: Exception) {
                android.util.Log.e("OnlineAudioManager", "Download failed", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Indirme basarisiz: ${e.message ?: "Bilinmeyen hata"}")
                }
            }
        }
    }

    private fun downloadToFile(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to download file")
            val body = response.body ?: error("Empty body")
            destination.outputStream().use { out ->
                body.byteStream().copyTo(out)
            }
        }
    }

    private fun embedMetadataAndArtwork(file: File, audioFile: AudioFile) {
        runCatching {
            val taggedFile = AudioFileIO.read(file)
            val tag = taggedFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, audioFile.title)
            tag.setField(FieldKey.ARTIST, audioFile.artist)
            tag.setField(FieldKey.ALBUM, "BeatBox Downloads")
            if (!audioFile.artworkUrl.isNullOrBlank()) {
                val imageBytes = downloadBytes(audioFile.artworkUrl)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    val tempCover = File(file.parentFile, "${file.nameWithoutExtension}_cover.jpg")
                    FileOutputStream(tempCover).use { it.write(imageBytes) }
                    val artwork = ArtworkFactory.createArtworkFromFile(tempCover)
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                    tempCover.delete()
                }
            }
            AudioFileIO.write(taggedFile)
        }.onFailure {
            android.util.Log.w("OnlineAudioManager", "Metadata tagging failed: ${it.message}")
        }
    }

    private fun downloadBytes(url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.bytes()
        }
    }

    private fun scanIntoMediaLibrary(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("audio/mpeg"),
            null
        )
    }

    private fun saveToPublicMusic(context: Context, sourceFile: File, fileName: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/BeatBox")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values)
            ?: throw IOException("Music/BeatBox klasorune kaydedilemedi")

        resolver.openOutputStream(itemUri)?.use { out ->
            sourceFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IOException("Dosya cikis akisi acilamadi")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            resolver.update(itemUri, doneValues, null, null)
        }

        return fileName
    }

    private fun sanitizeFileName(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val ascii = normalized.replace("[^\\p{ASCII}]".toRegex(), "")
        val clean = ascii.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
        return clean.takeIf { it.isNotBlank() }?.take(80) ?: "track_${System.currentTimeMillis()}"
    }
}
