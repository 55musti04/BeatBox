package com.blackstack.beatbox.ui.library

import com.blackstack.beatbox.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Kütüphane sekmesi.
 *
 * Filtreleme ve sıralama dışarıdan gelen [searchQuery] ve [sortOption] ile
 * yapılır; ekran yalnızca sunumdan sorumludur.
 */
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
    onDeleteSong: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit
) {
    val filtered = allAudioList.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
    val sorted = if (sortOption == SortOption.NAME) filtered.sortedBy { it.title } else filtered
    if (sorted.isEmpty()) {
        EmptyState(Icons.Rounded.MusicOff, strings.get("empty_library"))
    } else {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp) // No extra spacing, cards have their own padding
        ) {
            items(sorted, key = { it.id }) { a ->
                SongItem(
                    a = a,
                    strings = strings,
                    isCurrent = currentAudioId == a.id,
                    isPlaying = isPlaying,
                    isFavorite = favoriteIds.contains(a.id.toString()),
                    onFavClick = { onFavClick(a) },
                    onDelete = { onDeleteSong(a) },
                    onClick = { onSongClick(a, sorted) }
                )
            }
        }
    }
}
