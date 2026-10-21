package com.blackstack.beatbox

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Utility object for reading local LRC files from the filesystem
 * 
 * Handles reading LRC files that are stored alongside audio files,
 * with the same base filename but .lrc extension.
 */
object LocalFileReader {
    /**
     * Reads an LRC file from the same directory as the audio file
     * 
     * Converts the audio file URI to a file path, replaces the extension
     * with .lrc, and attempts to read the file content.
     * 
     * @param audioUri The URI of the audio file
     * @return The LRC file content as a string, or null if the file doesn't exist or can't be read
     */
    suspend fun readLrcFile(context: Context, audioUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            candidatePaths(context, audioUri).firstNotNullOfOrNull { path ->
                val lrcFile = File(path)
                if (lrcFile.exists() && lrcFile.canRead()) lrcFile.readText() else null
            }
        } catch (_: IOException) {
            // Return null on any IOException (file not found, permission denied, etc.)
            null
        } catch (_: SecurityException) {
            // Return null on security exceptions (permission issues)
            null
        }
    }
    
    /**
     * Constructs the LRC file path from an audio file URI
     * 
     * Takes the audio file path and replaces the file extension with .lrc
     * 
     * @param audioUri The URI of the audio file
     * @return The path to the corresponding LRC file
     */
    private fun candidatePaths(context: Context, audioUri: Uri): List<String> {
        if (audioUri.scheme.equals("file", ignoreCase = true)) {
            return listOf(constructLrcPath(audioUri.path ?: throw IllegalArgumentException("Invalid URI: no path")))
        }

        if (!audioUri.scheme.equals("content", ignoreCase = true)) return emptyList()

        val paths = mutableListOf<String>()
        val projection = buildList {
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
                add(MediaStore.Audio.Media.DISPLAY_NAME)
            }
        }.toTypedArray()

        context.contentResolver.query(audioUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                @Suppress("DEPRECATION")
                val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                if ((dataIndex >= 0) && !cursor.isNull(dataIndex)) {
                    paths += constructLrcPath(cursor.getString(dataIndex))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    val displayNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    if (relativePathIndex >= 0 && displayNameIndex >= 0) {
                        val relativePath = cursor.getString(relativePathIndex)
                        val displayName = cursor.getString(displayNameIndex)
                        if (!relativePath.isNullOrBlank() && !displayName.isNullOrBlank()) {
                            val audioPath = File(
                                Environment.getExternalStorageDirectory(),
                                relativePath.trimStart('/') + displayName,
                            ).path
                            paths += constructLrcPath(audioPath)
                        }
                    }
                }
            }
        }

        return paths.distinct()
    }

    private fun constructLrcPath(audioPath: String): String {
        val lastDotIndex = audioPath.lastIndexOf('.')
        
        return if (lastDotIndex > 0) {
            // Replace extension with .lrc
            audioPath.substring(0, lastDotIndex) + ".lrc"
        } else {
            // No extension found, just append .lrc
            "$audioPath.lrc"
        }
    }
}
