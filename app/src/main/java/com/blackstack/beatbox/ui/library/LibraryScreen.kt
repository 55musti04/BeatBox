package com.blackstack.beatbox.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.SortOption
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.groupSongsByArtist
import com.blackstack.beatbox.ui.components.BeatBoxSongRow
import kotlinx.coroutines.launch

/**
 * Kütüphane filtreleri. "Hepsi" alfabetik bölümlere ayrılır, "Şarkılar"
 * düz liste gösterir; diğerleri albüm, sanatçı ve klasör gruplarını açar.
 */
internal enum class LibraryFilter { ALL, SONGS, FAVORITES, ALBUMS, ARTISTS, FOLDERS }

internal data class LibraryAlbum(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<AudioFile>
)

internal data class LibraryFolder(
    val name: String,
    val songs: List<AudioFile>
)

private const val LIST_HEADER_ITEMS = 2

/**
 * Kütüphane sekmesi.
 *
 * Arama ve sıralama dışarıdan gelir; ekran filtreleme, A-Z gezinme, seçim
 * ve sunumdan sorumludur. Kalıcı yazma işlemleri geri çağrılarla kabuğa
 * bildirilir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    allAudioList: List<AudioFile>,
    searchQuery: String,
    sortOption: SortOption,
    strings: Strings,
    currentAudioId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onFavClick: (AudioFile) -> Unit,
    onAddFavorites: (List<AudioFile>) -> Unit,
    onAddToQueue: (List<AudioFile>) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val stickyOffsetPx = with(LocalDensity.current) { (-58).dp.roundToPx() }

    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val search = searchQuery.trim()
    val searched = remember(allAudioList, search) {
        if (search.isBlank()) {
            allAudioList
        } else {
            allAudioList.filter { song ->
                song.title.contains(search, ignoreCase = true) ||
                    song.artist.contains(search, ignoreCase = true) ||
                    song.folderName.contains(search, ignoreCase = true)
            }
        }
    }

    val songsSorted = remember(searched, sortOption) {
        when (sortOption) {
            SortOption.NAME -> sortedByTitle(searched)
            SortOption.DATE -> searched.sortedByDescending { it.dateAdded }
        }
    }

    val favoriteSongs = remember(songsSorted, favoriteIds) {
        songsSorted.filter { favoriteIds.contains(it.id.toString()) }
    }

    // "Hepsi" görünümü her zaman A-Z sıralıdır; harf başlıkları buna dayanır.
    val alphabetical = remember(songsSorted) { sortedByTitle(songsSorted) }
    val songGroups = remember(alphabetical) {
        alphabetical.groupBy { libraryLetter(it.title) }
    }

    val albums = remember(songsSorted, strings) {
        songsSorted
            .groupBy { song ->
                if (song.albumId > 0) "album-${song.albumId}" else "folder-${song.folderName}"
            }
            .map { (key, songs) ->
                val first = songs.first()
                LibraryAlbum(
                    key = key,
                    title = first.albumName
                        ?.takeIf { it.isNotBlank() }
                        ?: first.folderName.ifBlank { strings.get("unknown_album") },
                    artist = first.albumArtist
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                        ?: first.artist.takeIf { !it.equals("<unknown>", true) }
                        ?: strings.get("unknown_artist"),
                    songs = sortedByTitle(songs)
                )
            }
            .sortedWith(
                compareBy<LibraryAlbum> { librarySortKey(it.title) }
                    .thenBy { it.title }
            )
    }

    val artists = remember(songsSorted, strings) {
        groupSongsByArtist(songsSorted, strings.get("unknown_artist"))
            .entries
            .sortedBy { librarySortKey(it.key) }
    }

    val folders = remember(songsSorted) {
        songsSorted
            .groupBy { it.folderName.ifBlank { "/" } }
            .map { (name, songs) -> LibraryFolder(name, sortedByTitle(songs)) }
            .sortedBy { librarySortKey(it.name) }
    }

    val totalMinutes = remember(allAudioList) {
        allAudioList.sumOf { it.duration } / 60_000L
    }

    LaunchedEffect(search, filter, sortOption) {
        selection = emptySet()
    }

    val selectionMode = selection.isNotEmpty()
    val selectedSongs = remember(selection, songsSorted) {
        songsSorted.filter { selection.contains(it.id) }
    }

    val displaySongs = when (filter) {
        LibraryFilter.ALL -> alphabetical
        LibraryFilter.SONGS -> songsSorted
        LibraryFilter.FAVORITES -> favoriteSongs
        LibraryFilter.ALBUMS,
        LibraryFilter.ARTISTS,
        LibraryFilter.FOLDERS -> emptyList()
    }

    // A-Z kaydırıcısının hedef öğe indeksleri. İlk iki öğe hero ve sticky
    // filtre çubuğudur; harf gruplarında araya başlık öğesi girer.
    val letterAnchors = remember(filter, alphabetical, songsSorted, favoriteSongs) {
        val anchors = linkedMapOf<Char, Int>()
        when (filter) {
            LibraryFilter.ALL -> {
                var index = LIST_HEADER_ITEMS
                songGroups.forEach { (letter, songs) ->
                    anchors[letter] = index
                    index += 1 + songs.size
                }
            }

            LibraryFilter.SONGS -> songsSorted.forEachIndexed { index, song ->
                anchors.putIfAbsent(libraryLetter(song.title), index + LIST_HEADER_ITEMS)
            }

            LibraryFilter.FAVORITES -> favoriteSongs.forEachIndexed { index, song ->
                anchors.putIfAbsent(libraryLetter(song.title), index + LIST_HEADER_ITEMS)
            }

            else -> Unit
        }
        anchors
    }

    fun toggleSelection(song: AudioFile) {
        selection = if (selection.contains(song.id)) {
            selection - song.id
        } else {
            selection + song.id
        }
    }

    fun playSelection() {
        if (selectedSongs.isNotEmpty()) {
            onSongClick(selectedSongs.first(), selectedSongs)
            selection = emptySet()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "library_hero") {
                LibraryHero(
                    songCount = allAudioList.size,
                    favoriteCount = favoriteIds.size,
                    totalMinutes = totalMinutes,
                    strings = strings,
                    onShuffle = {
                        if (songsSorted.isNotEmpty()) {
                            val shuffled = songsSorted.shuffled()
                            onSongClick(shuffled.first(), shuffled)
                        }
                    }
                )
            }

            stickyHeader(key = "library_filters") {
                LibraryFilterBar(
                    filter = filter,
                    onFilterChange = { filter = it },
                    compact = compact,
                    onToggleDensity = { compact = !compact },
                    strings = strings,
                    selectionCount = selection.size,
                    onClearSelection = { selection = emptySet() },
                    onPlaySelected = { playSelection() },
                    onFavoriteSelected = { onAddFavorites(selectedSongs) },
                    onQueueSelected = { onAddToQueue(selectedSongs) }
                )
            }

            when (filter) {
                LibraryFilter.ALL -> {
                    if (alphabetical.isEmpty()) {
                        item(key = "empty") {
                            LibraryEmpty(
                                title = emptyTitle(allAudioList, filter, strings),
                                icon = emptyIcon(allAudioList, filter)
                            )
                        }
                    } else {
                        songGroups.forEach { (letter, songs) ->
                            item(key = "letter-$letter") {
                                LibraryLetterHeader(letter)
                            }
                            items(songs, key = { "song-${it.id}" }) { song ->
                                LibrarySongRow(
                                    song = song,
                                    list = displaySongs,
                                    strings = strings,
                                    currentAudioId = currentAudioId,
                                    isPlaying = isPlaying,
                                    favoriteIds = favoriteIds,
                                    selection = selection,
                                    selectionMode = selectionMode,
                                    compact = compact,
                                    onFavClick = onFavClick,
                                    onDeleteSong = onDeleteSong,
                                    onSongClick = onSongClick,
                                    onToggleSelection = ::toggleSelection,
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (selection.isEmpty()) {
                                            selection = setOf(song.id)
                                        } else {
                                            toggleSelection(song)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                LibraryFilter.SONGS,
                LibraryFilter.FAVORITES -> {
                    if (displaySongs.isEmpty()) {
                        item(key = "empty") {
                            LibraryEmpty(
                                title = emptyTitle(allAudioList, filter, strings),
                                icon = emptyIcon(allAudioList, filter)
                            )
                        }
                    } else {
                        items(displaySongs, key = { "song-${it.id}" }) { song ->
                            LibrarySongRow(
                                song = song,
                                list = displaySongs,
                                strings = strings,
                                currentAudioId = currentAudioId,
                                isPlaying = isPlaying,
                                favoriteIds = favoriteIds,
                                selection = selection,
                                selectionMode = selectionMode,
                                compact = compact,
                                onFavClick = onFavClick,
                                onDeleteSong = onDeleteSong,
                                onSongClick = onSongClick,
                                onToggleSelection = ::toggleSelection,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (selection.isEmpty()) {
                                        selection = setOf(song.id)
                                    } else {
                                        toggleSelection(song)
                                    }
                                }
                            )
                        }
                    }
                }

                LibraryFilter.ALBUMS -> {
                    if (albums.isEmpty()) {
                        item(key = "empty") {
                            LibraryEmpty(
                                title = emptyTitle(allAudioList, filter, strings),
                                icon = emptyIcon(allAudioList, filter)
                            )
                        }
                    } else {
                        items(albums, key = { it.key }) { album ->
                            LibraryAlbumRow(
                                album = album,
                                strings = strings,
                                onClick = {
                                    onSongClick(album.songs.first(), album.songs)
                                }
                            )
                        }
                    }
                }

                LibraryFilter.ARTISTS -> {
                    if (artists.isEmpty()) {
                        item(key = "empty") {
                            LibraryEmpty(
                                title = emptyTitle(allAudioList, filter, strings),
                                icon = emptyIcon(allAudioList, filter)
                            )
                        }
                    } else {
                        items(artists, key = { "artist-${it.key}" }) { entry ->
                            LibraryArtistRow(
                                name = entry.key,
                                songs = entry.value,
                                strings = strings,
                                onClick = {
                                    onSongClick(entry.value.first(), entry.value)
                                }
                            )
                        }
                    }
                }

                LibraryFilter.FOLDERS -> {
                    if (folders.isEmpty()) {
                        item(key = "empty") {
                            LibraryEmpty(
                                title = emptyTitle(allAudioList, filter, strings),
                                icon = emptyIcon(allAudioList, filter)
                            )
                        }
                    } else {
                        items(folders, key = { "folder-row-${it.name}" }) { folder ->
                            LibraryFolderRow(
                                folder = folder,
                                strings = strings,
                                onClick = {
                                    onSongClick(folder.songs.first(), folder.songs)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (letterAnchors.isNotEmpty() && !selectionMode) {
            AlphabetScrubber(
                letters = letterAnchors.keys.toList(),
                onLetterSelected = { letter ->
                    letterAnchors[letter]?.let { index ->
                        scope.launch {
                            listState.scrollToItem(index, stickyOffsetPx)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 1.dp)
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: AudioFile,
    list: List<AudioFile>,
    strings: Strings,
    currentAudioId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    selection: Set<Long>,
    selectionMode: Boolean,
    compact: Boolean,
    onFavClick: (AudioFile) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit,
    onToggleSelection: (AudioFile) -> Unit,
    onLongPress: () -> Unit
) {
    BeatBoxSongRow(
        audio = song,
        strings = strings,
        isCurrent = currentAudioId == song.id,
        isPlaying = isPlaying,
        isFavorite = favoriteIds.contains(song.id.toString()),
        isSelected = selection.contains(song.id),
        selectionMode = selectionMode,
        compact = compact,
        onFavClick = { onFavClick(song) },
        onDelete = { onDeleteSong(song) },
        onRowClick = {
            if (selectionMode) onToggleSelection(song) else onSongClick(song, list)
        },
        onRowLongClick = onLongPress
    )
}

private fun emptyTitle(
    allAudioList: List<AudioFile>,
    filter: LibraryFilter,
    strings: Strings
): String = when {
    allAudioList.isEmpty() -> strings.get("empty_library")
    filter == LibraryFilter.FAVORITES -> strings.get("no_favorites")
    else -> strings.get("no_results")
}

private fun emptyIcon(
    allAudioList: List<AudioFile>,
    filter: LibraryFilter
) = if (allAudioList.isEmpty() || filter == LibraryFilter.FAVORITES) {
    Icons.Rounded.MusicOff
} else {
    Icons.Rounded.SearchOff
}
