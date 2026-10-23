package com.blackstack.beatbox.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist

/**
 * Uygulama kabuğunun üstünde açılan ekran ve diyalogların görünürlük durumu.
 *
 * Tüm alanlar tek bir yerde tutulur; böylece `BeatBoxApp` yalnızca veri ve
 * eylemleri yönetir, görünürlük ayrıntılarıyla boğuşmaz.
 */
@Stable
class BeatBoxOverlayState {
    var showSettings by mutableStateOf(false)
    var showEQ by mutableStateOf(false)
    var showSleepTimer by mutableStateOf(false)
    var showPlayer by mutableStateOf(false)
    var playlistDetail by mutableStateOf<Playlist?>(null)
    var showCreatePlaylist by mutableStateOf(false)
    var editPlaylist by mutableStateOf<Playlist?>(null)
    var showAddToPlaylist by mutableStateOf(false)
    var showFolder by mutableStateOf(false)
    var showDelete by mutableStateOf(false)
    var songToDelete by mutableStateOf<AudioFile?>(null)
    var showWrapped by mutableStateOf(false)
    var showHistory by mutableStateOf(false)
    var showAuth by mutableStateOf(false)
    var showProfile by mutableStateOf(false)

    /** Açık bir katman varsa geri tuşu önce onu kapatır. */
    fun closeTopmost(): Boolean = when {
        showPlayer -> { showPlayer = false; true }
        playlistDetail != null -> { playlistDetail = null; true }
        showSettings -> { showSettings = false; true }
        else -> false
    }
}
