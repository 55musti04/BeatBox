package com.blackstack.beatbox.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.groupSongsByArtist

/**
 * Ana sayfa: selamlama, devam eden çalma sahnesi ve kişisel raflar.
 *
 * Ekran yalnızca sunum yapar. Çalma durumu, ilerleme ve gezinme eylemleri
 * [com.blackstack.beatbox.ui.components.BeatBoxTabContent] tarafından beslenir.
 */
@Composable
fun HomeScreen(
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    currentAudioId: Long?,
    currentAudio: AudioFile?,
    isPlaying: Boolean,
    onPlaySong: (AudioFile) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayArtist: (List<AudioFile>) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    progress: () -> Float,
    strings: Strings,
    ctx: Context,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray
) {
    val prefs = remember(ctx) {
        ctx.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
    }
    val listState = rememberLazyListState()

    // "Senin için": en çok dinlenenler; henüz veri yoksa en yeni parçalar.
    val topSongs = remember(allAudioList, currentAudioId, prefs) {
        allAudioList
            .sortedWith(
                compareByDescending<AudioFile> { prefs.getInt("stats_count_${it.id}", 0) }
                    .thenByDescending { it.dateAdded }
            )
            .take(10)
    }

    val recentlyAdded = remember(allAudioList) {
        allAudioList.sortedByDescending { it.dateAdded }.take(6)
    }

    // Dinleme geçmişi kimlik listesi olarak tutulur; ekran tarafında çözülür.
    val recentlyPlayed = remember(allAudioList, currentAudioId, prefs) {
        val byId = allAudioList.associateBy { it.id }
        prefs.getString("stats_history", "")
            .orEmpty()
            .split(',')
            .asSequence()
            .mapNotNull { it.trim().toLongOrNull() }
            .mapNotNull { byId[it] }
            .distinctBy { it.id }
            .take(8)
            .toList()
    }

    val artists = remember(allAudioList, strings) {
        groupSongsByArtist(allAudioList, strings.get("unknown_artist"))
            .entries
            .sortedByDescending { it.value.size }
            .take(12)
    }

    val totalMinutes = remember(prefs, currentAudioId) {
        prefs.getLong("stats_total_minutes", 0L)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "greeting") {
            HomeGreeting(
                strings = strings,
                songCount = allAudioList.size,
                playlistCount = playlists.size,
                onShuffleAll = onShuffleAll,
                listState = listState
            )
        }

        item(key = "hero") {
            HomeHero(
                currentAudio = currentAudio,
                isPlaying = isPlaying,
                isFavorite = currentAudio?.let { favoriteIds.contains(it.id.toString()) } == true,
                progress = progress,
                onPlayPause = onPlayPause,
                onOpenPlayer = onOpenPlayer,
                onShuffleAll = onShuffleAll,
                songCount = allAudioList.size,
                strings = strings
            )
        }

        if (topSongs.isNotEmpty()) {
            item(key = "for_you") {
                HomeForYouSection(
                    songs = topSongs,
                    currentAudioId = currentAudioId,
                    isPlaying = isPlaying,
                    visualizerLevel = visualizerLevel,
                    visualizerWaveform = visualizerWaveform,
                    strings = strings,
                    onPlaySong = onPlaySong
                )
            }
        }

        if (recentlyPlayed.size >= 2) {
            item(key = "recently_played") {
                HomeRecentlyPlayedSection(
                    songs = recentlyPlayed,
                    currentAudioId = currentAudioId,
                    strings = strings,
                    onPlaySong = onPlaySong
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently_added") {
                HomeRecentlyAddedSection(
                    songs = recentlyAdded,
                    currentAudioId = currentAudioId,
                    strings = strings,
                    onPlaySong = onPlaySong
                )
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "artists") {
                HomeArtistsSection(
                    artists = artists,
                    strings = strings,
                    onPlayArtist = onPlayArtist
                )
            }
        }

        if (totalMinutes > 0L) {
            item(key = "stats") {
                HomeListeningStat(totalMinutes = totalMinutes, strings = strings)
            }
        }

        if (allAudioList.isEmpty()) {
            item(key = "empty") {
                HomeEmptyLibrary(strings = strings)
            }
        }
    }
}

/**
 * Saate göre selamlama anahtarını döndürür. Saf fonksiyon; birim testi vardır.
 */
internal fun greetingKeyForHour(hour: Int): String = when (hour) {
    in 5..11 -> "good_morning"
    in 12..17 -> "good_afternoon"
    else -> "good_evening"
}
