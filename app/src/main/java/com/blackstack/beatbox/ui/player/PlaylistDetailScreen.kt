package com.blackstack.beatbox.ui.player

import com.blackstack.beatbox.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tek bir çalma listesinin şarkı listesi.
 *
 * Şarkı seçimi ve listeyi çalma davranışı dışarıdan yönetilir; ekran yalnızca
 * [playlist] içeriğini gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allAudioList: List<AudioFile>,
    strings: Strings,
    currentAudioId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (List<AudioFile>) -> Unit,
    onFavClick: (AudioFile) -> Unit,
    onDelete: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit
) {
    val pl = playlist
    val songs = allAudioList.filter { pl.songIds.contains(it.id) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            TopAppBar(
                title = { Text(pl.name) },
                navigationIcon = { IconButton(onClick = { onBack() }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        onPlayAll(songs)
                    }, modifier = Modifier.iconButtonShadow()) {
                        Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
            )

            if (pl.isArtistPlaylist && !pl.artistName.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .cardShadow(),
                            contentAlignment = Alignment.Center
                        ) {
                            ArtistImage(
                                artistName = pl.artistName,
                                modifier = Modifier.fillMaxSize(),
                                isHQ = true
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = pl.artistName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${songs.size} ${strings.get("songs")} â€¢ ${strings.get("artist_playlist_tag")}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }

            if (songs.isEmpty()) {
                EmptyState(Icons.Rounded.MusicNote, strings.get("empty_library"))
            } else {
                LazyColumn {
                    items(songs) { a ->
                        SongItem(
                            a = a,
                            strings = strings,
                            isCurrent = currentAudioId == a.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(a.id.toString()),
                            onFavClick = { onFavClick(a) },
                            onDelete = { onDelete(a) },
                            onClick = { onSongClick(a, songs) }
                        )
                    }
                }
            }
        }
    }
}
