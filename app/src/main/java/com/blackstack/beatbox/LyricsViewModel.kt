package com.blackstack.beatbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing lyrics state and playback synchronization
 */
class LyricsViewModel(
    private val lyricsProvider: LyricsProvider
) : ViewModel() {
    
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()
    
    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()
    
    /**
     * Loads lyrics for the specified audio file
     * 
     * @param audio The audio file to load lyrics for
     */
    fun loadLyrics(audio: AudioFile) {
        viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            lyricsProvider.getLyrics(audio).fold(
                onSuccess = { lyrics ->
                    _lyricsState.value = LyricsState.Success(lyrics)
                },
                onFailure = { error ->
                    _lyricsState.value = LyricsState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
    
    /**
     * Updates the current playback position and finds the corresponding lyrics line
     * 
     * @param positionMs Current playback position in milliseconds
     */
    fun updatePlaybackPosition(positionMs: Long) {
        val state = _lyricsState.value
        if (state is LyricsState.Success) {
            val newIndex = findCurrentLineIndex(state.lyrics.lines, positionMs)
            if (newIndex != _currentLineIndex.value) {
                _currentLineIndex.value = newIndex
            }
        }
    }
    
    /**
     * Retries loading lyrics after an error
     * 
     * @param audio The audio file to retry loading lyrics for
     */
    fun retry(audio: AudioFile) {
        loadLyrics(audio)
    }
    
    /**
     * Finds the index of the current lyrics line using binary search
     * Returns the highest index where the line's timestamp is <= playback position
     * 
     * @param lines List of synced lyrics lines
     * @param positionMs Current playback position in milliseconds
     * @return Index of the current line
     */
    private fun findCurrentLineIndex(lines: List<SyncedLyricsLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return 0
        
        var left = 0
        var right = lines.size - 1
        var result = 0
        
        while (left <= right) {
            val mid = (left + right) / 2
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                left = mid + 1
            } else {
                right = mid - 1
            }
        }
        
        return result
    }
}

/**
 * Sealed class representing the state of lyrics loading
 */
sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Success(val lyrics: SyncedLyrics) : LyricsState()
    data class Error(val message: String) : LyricsState()
}
