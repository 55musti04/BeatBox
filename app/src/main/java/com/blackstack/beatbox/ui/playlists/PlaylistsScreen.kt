package com.blackstack.beatbox.ui.playlists

import com.blackstack.beatbox.*
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Çalma listeleri sekmesi.
 *
 * Liste üzerindeki tüm değişiklikler [onPlaylistsChange] ile yukarı bildirilir;
 * ekran kendi başına kalıcı veri yazmaz.
 */
@Composable
fun PlaylistsScreen(
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    strings: Strings,
    ctx: Context,
    onPlaylistOpen: (Playlist) -> Unit,
    onPlaylistDelete: (Playlist) -> Unit,
    onPlaylistEdit: (Playlist) -> Unit,
    onPlaylistsChange: (List<Playlist>) -> Unit
) {
    val artistGroups = remember(allAudioList, strings) { groupSongsByArtist(allAudioList, strings.get("unknown_artist")) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            // Auto-create artist playlists button card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        var addedCount = 0
                        val mutablePlaylists = playlists.toMutableList()
                        for ((artistName, artistSongs) in artistGroups) {
                            if (artistSongs.isNotEmpty() && !mutablePlaylists.any { it.isArtistPlaylist && it.artistName.equals(artistName, true) }) {
                                mutablePlaylists.add(
                                     Playlist(
                                        name = "${strings.get("artist")}: $artistName",
                                        songIds = artistSongs.map { it.id }.toMutableList(),
                                        isArtistPlaylist = true,
                                        artistName = artistName
                                    )
                                )
                                addedCount++
                            }
                        }
                        if (addedCount > 0) {
                            onPlaylistsChange(mutablePlaylists)
                            Toast.makeText(ctx, strings.get("artist_playlists_created"), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, strings.get("all_artist_playlists_exist"), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(0.2f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            strings.get("create_artist_playlists"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Text(
                            "${artistGroups.size} ${strings.get("artists_found")}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (artistGroups.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        strings.get("artist_playlists"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(artistGroups.entries.toList()) { (artistName, songs) ->
                            ArtistCard(
                                artistName = artistName,
                                songCount = songs.size,
                                strings = strings,
                                onClick = {
                                    val existing = playlists.find { it.isArtistPlaylist && it.artistName.equals(artistName, true) }
                                    if (existing != null) {
                                        onPlaylistOpen(existing)
                                    } else {
                                        val newPl = Playlist(
                                            name = "${strings.get("artist")}: $artistName",
                                            songIds = songs.map { it.id }.toMutableList(),
                                            isArtistPlaylist = true,
                                            artistName = artistName
                                        )
                                        onPlaylistsChange(playlists + newPl)
                                        onPlaylistOpen(newPl)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            // Favorites card with gradient
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val favSongs = allAudioList.filter { favoriteIds.contains(it.id.toString()) }
                        if (favSongs.isNotEmpty()) {
                            onPlaylistOpen(Playlist(name = strings.get("favorites"), isSmart = true, songIds = favSongs.map { it.id }.toMutableList()))
                        } else {
                            Toast.makeText(ctx, strings.get("empty_playlists"), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF4081).copy(0.15f)
                )
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFF4081).copy(0.3f),
                                        Color(0xFFFF4081).copy(0.1f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Favorite,
                            null,
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            strings.get("favorites"),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4081),
                            fontSize = 18.sp
                        )
                        Text(
                            "${favoriteIds.size} ${strings.get("songs_count")}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        )
                    }
                }
            }
        }
        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            strings.get("empty_playlists"),
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        items(playlists) { playlist ->
            PlaylistItem(
                playlist,
                { onPlaylistOpen(playlist) },
                { onPlaylistDelete(playlist) },
                { onPlaylistEdit(playlist) },
                strings = strings
            )
        }
    }
}
