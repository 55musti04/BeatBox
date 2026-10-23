package com.blackstack.beatbox.ui.components

import com.blackstack.beatbox.*
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blackstack.beatbox.ui.home.HomeScreen
import com.blackstack.beatbox.ui.library.LibraryScreen
import com.blackstack.beatbox.ui.playlists.PlaylistsScreen

/**
 * Sekme içerikleri (Ana Sayfa, Çalma Listeleri, Kütüphane) ve sekme geçişi.
 *
 * Ekranlar yalnızca sunum yapar; çalma durumu ve kalıcı yazma işlemleri
 * [onCurrentAudioChange], [onCurrentPlaylistChange] ve [onPlaylistsChange]
 * üzerinden uygulama kabuğuna bildirilir.
 */
@Composable
fun BeatBoxTabContent(
    selectedTab: Int,
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    currentAudio: AudioFile?,
    currentPlaylist: List<AudioFile>,
    isPlaying: Boolean,
    searchQuery: String,
    sortOption: SortOption,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    strings: Strings,
    ctx: Context,
    prefs: SharedPreferences,
    player: MainActivity,
    overlays: BeatBoxOverlayState,
    onCurrentPlaylistChange: (List<AudioFile>) -> Unit,
    onCurrentAudioChange: (AudioFile) -> Unit,
    onFavoriteIdsChange: (Set<String>) -> Unit,
    onPlaylistsChange: (List<Playlist>) -> Unit
) {
    AnimatedContent(
        targetState = selectedTab,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            val duration = 320
            (slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth * direction },
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(duration / 2))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth * direction },
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(duration / 2))) using
                    SizeTransform(clip = false)
        },
        label = "tab_transition"
    ) { tab ->
        when (tab) {
            // Tab 0 = Home (Ana Sayfa)
            0 -> {
                HomeScreen(
                    allAudioList = allAudioList,
                    favoriteIds = favoriteIds,
                    playlists = playlists,
                    currentAudioId = currentAudio?.id,
                    currentAudio = currentAudio,
                    isPlaying = isPlaying,
                    onPlaySong = { song ->
                        onCurrentPlaylistChange(allAudioList)
                        onCurrentAudioChange(song)
                        player.playAudio(song)
                        updateSongStats(prefs, song)
                    },
                    onShuffleAll = {
                        if (allAudioList.isNotEmpty()) {
                            val shuffled = allAudioList.shuffled()
                            onCurrentPlaylistChange(shuffled)
                            onCurrentAudioChange(shuffled.first())
                            player.playAudio(shuffled.first())
                            updateSongStats(prefs, shuffled.first())
                        }
                    },
                    onPlayArtist = { songs ->
                        if (songs.isNotEmpty()) {
                            onCurrentPlaylistChange(songs)
                            onCurrentAudioChange(songs.first())
                            player.playAudio(songs.first())
                            updateSongStats(prefs, songs.first())
                        }
                    },
                    onOpenPlayer = { overlays.showPlayer = true },
                    onPlayPause = {
                        currentAudio?.let { audio ->
                            if (isPlaying) player.pauseAudio(audio) else player.resumeAudio(audio)
                        }
                    },
                    progress = {
                        val duration = player.getDuration()
                        if (duration > 0) {
                            player.getCurrentPosition().toFloat() / duration
                        } else {
                            0f
                        }
                    },
                    strings = strings,
                    ctx = ctx,
                    visualizerLevel = visualizerLevel,
                    visualizerWaveform = visualizerWaveform
                )
            }
            // Tab 1 = Playlists (Ã‡alma Listeleri)
            1 -> {
                PlaylistsScreen(
                    allAudioList = allAudioList,
                    favoriteIds = favoriteIds,
                    playlists = playlists,
                    strings = strings,
                    ctx = ctx,
                    onPlaylistOpen = { overlays.playlistDetail = it },
                    onPlaylistDelete = { p -> onPlaylistsChange(playlists.filter { it.id != p.id }) },
                    onPlaylistEdit = { overlays.editPlaylist = it },
                    onPlaylistsChange = { onPlaylistsChange(it) }
                )
            }
            // Tab 2 = Library (KÃ¼tÃ¼phane)
            2 -> {
                LibraryScreen(
                    allAudioList = allAudioList,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    strings = strings,
                    currentAudioId = currentAudio?.id,
                    isPlaying = isPlaying,
                    favoriteIds = favoriteIds,
                    onFavClick = { a -> player.toggleFavorite(a.id); onFavoriteIdsChange(player.getFavorites()) },
                    onDeleteSong = { overlays.songToDelete = it; overlays.showDelete = true },
                    onSongClick = { a, list ->
                        if (currentPlaylist != list) onCurrentPlaylistChange(list)
                        onCurrentAudioChange(a)
                        player.playAudio(a)
                        updateSongStats(prefs, a)
                    }
                )
            }
        }
    }
}
