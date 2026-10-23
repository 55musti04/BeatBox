package com.blackstack.beatbox.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.components.BeatBoxSongRow
import com.blackstack.beatbox.ui.components.PlaylistArtistCover
import com.blackstack.beatbox.ui.components.PlaylistMosaicCover
import com.blackstack.beatbox.ui.components.PlaylistSmartCover

/**
 * Tek bir çalma listesinin şarkı listesi.
 *
 * Kapak, başlık ve toplu çalma aksiyonları Aurora Glass hiyerarşisinde
 * sunulur; satırlar kütüphaneyle aynı [BeatBoxSongRow] bileşenini kullanır.
 * Şarkı seçimi ve listeyi çalma davranışı dışarıdan yönetilir.
 */
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
    val songs = remember(pl.songIds, allAudioList) {
        allAudioList.filter { pl.songIds.contains(it.id) }
    }
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Üstte albümden türeyen aurora ışığı; taban tamamen opak kalır,
        // böylece alttaki sekme içeriği detay ekranına sızmaz.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )

        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.get("back"),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 132.dp)
            ) {
                item(key = "playlist_header") {
                    PlaylistDetailHeader(
                        playlist = pl,
                        songs = songs,
                        strings = strings,
                        onPlayAll = onPlayAll
                    )
                }

                if (songs.isEmpty()) {
                    item(key = "playlist_empty") {
                        PlaylistDetailEmpty(strings)
                    }
                } else {
                    items(songs, key = { it.id }) { song ->
                        BeatBoxSongRow(
                            audio = song,
                            strings = strings,
                            isCurrent = currentAudioId == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id.toString()),
                            isSelected = false,
                            selectionMode = false,
                            compact = false,
                            onFavClick = { onFavClick(song) },
                            onDelete = { onDelete(song) },
                            onRowClick = { onSongClick(song, songs) },
                            onRowLongClick = { onFavClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailHeader(
    playlist: Playlist,
    songs: List<AudioFile>,
    strings: Strings,
    onPlayAll: (List<AudioFile>) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coverShape = if (playlist.isArtistPlaylist && !playlist.artistName.isNullOrBlank()) {
            CircleShape
        } else {
            RoundedCornerShape(28.dp)
        }

        Box(
            modifier = Modifier
                .size(132.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = coverShape,
                    ambientColor = primary.copy(alpha = 0.45f),
                    spotColor = secondary.copy(alpha = 0.28f)
                )
        ) {
            when {
                playlist.isSmart -> PlaylistSmartCover(Modifier.fillMaxSize())
                playlist.isArtistPlaylist && !playlist.artistName.isNullOrBlank() ->
                    PlaylistArtistCover(
                        artistName = playlist.artistName,
                        modifier = Modifier.fillMaxSize()
                    )

                else -> PlaylistMosaicCover(
                    songs = songs,
                    modifier = Modifier.fillMaxSize(),
                    corner = 28.dp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = when {
                playlist.isSmart -> strings.get("smart_playlist")
                playlist.isArtistPlaylist -> strings.get("artist_playlist_tag")
                else -> strings.get("playlists")
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
            color = primary.copy(alpha = 0.9f),
            maxLines = 1
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = playlist.name,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "${songs.size} ${strings.get("songs")}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            maxLines = 1
        )

        Spacer(Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(percent = 50),
                        ambientColor = primary.copy(alpha = 0.45f)
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(primary, secondary.copy(alpha = 0.92f))
                        )
                    )
                    .clickable {
                        if (songs.isNotEmpty()) onPlayAll(songs)
                    }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = strings.get("play_all"),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = {
                    if (songs.isNotEmpty()) onPlayAll(songs.shuffled())
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = strings.get("shuffle"),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun PlaylistDetailEmpty(strings: Strings) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(primary.copy(alpha = 0.16f), secondary.copy(alpha = 0.07f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = primary.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = strings.get("playlist_empty"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
