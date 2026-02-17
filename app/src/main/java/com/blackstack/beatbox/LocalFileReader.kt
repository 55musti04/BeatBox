package com.blackstack.beatbox

import android.net.Uri
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
    suspend fun readLrcFile(audioUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val lrcPath = constructLrcPath(audioUri)
            val lrcFile = File(lrcPath)
            
            if (lrcFile.exists() && lrcFile.canRead()) {
                lrcFile.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            // Return null on any IOException (file not found, permission denied, etc.)
            null
        } catch (e: SecurityException) {
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
    private fun constructLrcPath(audioUri: Uri): String {
        val audioPath = audioUri.path ?: throw IllegalArgumentException("Invalid URI: no path")
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
