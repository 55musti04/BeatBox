package com.blackstack.beatbox.ui.playlists

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.groupSongsByArtist
import com.blackstack.beatbox.ui.dialogs.DeletePlaylistDialog

/**
 * Çalma listeleri sekmesi.
 *
 * Akıllı favoriler, sanatçı listeleri ve kullanıcı listeleri tek bir editorial
 * hiyerarşide sunulur. Liste değişiklikleri [onPlaylistsChange] ile yukarı
 * bildirilir; ekran kalıcı veri yazmaz.
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
    onPlaylistsChange: (List<Playlist>) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val artistGroups = remember(allAudioList, strings) {
        groupSongsByArtist(allAudioList, strings.get("unknown_artist"))
    }
    val songsById = remember(allAudioList) { allAudioList.associateBy { it.id } }
    val favoriteSongs = remember(allAudioList, favoriteIds) {
        allAudioList.filter { favoriteIds.contains(it.id.toString()) }
    }
    val userPlaylists = remember(playlists) { playlists.filter { !it.isArtistPlaylist } }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val heroSongs = remember(userPlaylists, playlists, allAudioList) {
        val first = (userPlaylists + playlists).firstOrNull { it.songIds.isNotEmpty() }
        val cover = first?.songIds?.mapNotNull { songsById[it] }.orEmpty()
        if (cover.isNotEmpty()) cover.take(4) else allAudioList.take(4)
    }

    fun createAllArtistPlaylists() {
        var addedCount = 0
        val mutablePlaylists = playlists.toMutableList()
        for ((artistName, artistSongs) in artistGroups) {
            val exists = mutablePlaylists.any {
                it.isArtistPlaylist && it.artistName.equals(artistName, true)
            }
            if (artistSongs.isNotEmpty() && !exists) {
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

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "playlists_hero") {
            PlaylistsHero(
                songCount = allAudioList.size,
                artistCount = artistGroups.size,
                coverSongs = heroSongs,
                strings = strings
            )
        }

        item(key = "smart_header") {
            PlaylistSectionHeader(
                title = strings.get("smart_lists"),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item(key = "smart_favorites") {
            SmartFavoritesCard(
                songCount = favoriteSongs.size,
                strings = strings,
                onClick = {
                    if (favoriteSongs.isNotEmpty()) {
                        onPlaylistOpen(
                            Playlist(
                                name = strings.get("favorites"),
                                isSmart = true,
                                songIds = favoriteSongs.map { it.id }.toMutableList()
                            )
                        )
                    } else {
                        Toast.makeText(ctx, strings.get("empty_playlists"), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (artistGroups.isNotEmpty()) {
            item(key = "artists_header") {
                PlaylistSectionHeader(
                    title = strings.get("artist_playlists"),
                    count = artistGroups.size,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item(key = "artists_row") {
                LazyRow(
                    // Sağdaki FAB'ın son halkayı örtmemesi için satır sonunda
                    // FAB genişliği kadar boşluk bırak.
                    contentPadding = PaddingValues(start = 20.dp, end = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(artistGroups.entries.toList(), key = { it.key }) { entry ->
                        ArtistPlaylistCard(
                            artistName = entry.key,
                            songCount = entry.value.size,
                            strings = strings,
                            onClick = {
                                val existing = playlists.find {
                                    it.isArtistPlaylist && it.artistName.equals(entry.key, true)
                                }
                                if (existing != null) {
                                    onPlaylistOpen(existing)
                                } else {
                                    val newPlaylist = Playlist(
                                        name = "${strings.get("artist")}: ${entry.key}",
                                        songIds = entry.value.map { it.id }.toMutableList(),
                                        isArtistPlaylist = true,
                                        artistName = entry.key
                                    )
                                    onPlaylistsChange(playlists + newPlaylist)
                                    onPlaylistOpen(newPlaylist)
                                }
                            }
                        )
                    }
                }
            }

            item(key = "artists_create_all") {
                CreateAllArtistPlaylistsRow(
                    artistCount = artistGroups.size,
                    strings = strings,
                    onClick = { createAllArtistPlaylists() }
                )
            }
        }

        item(key = "your_playlists_header") {
            PlaylistSectionHeader(
                title = strings.get("your_playlists"),
                count = userPlaylists.size,
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        if (userPlaylists.isEmpty()) {
            item(key = "playlists_empty") {
                EmptyPlaylists(
                    strings = strings,
                    onCreatePlaylist = onCreatePlaylist
                )
            }
        } else {
            items(userPlaylists, key = { it.id }) { playlist ->
                val coverSongs = remember(playlist.songIds, songsById) {
                    playlist.songIds.mapNotNull { songsById[it] }
                }
                PlaylistCard(
                    playlist = playlist,
                    coverSongs = coverSongs,
                    strings = strings,
                    onClick = { onPlaylistOpen(playlist) },
                    onEdit = { onPlaylistEdit(playlist) },
                    onDelete = { playlistToDelete = playlist }
                )
            }
        }
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            strings = strings,
            playlist = playlist,
            onConfirm = {
                onPlaylistDelete(playlist)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }
}
