package com.blackstack.beatbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AlbumArtImage
import com.blackstack.beatbox.ArtistImage
import com.blackstack.beatbox.AudioFile

/**
 * Çalma listeleri için ortak kapak görselleri.
 *
 * Kullanıcı listeleri şarkı kapaklarından mozaik üretir; sanatçı listeleri
 * halka biçiminde sanatçı görselini, akıllı favoriler ise pembe aurora
 * gradyanını kullanır.
 */

@Composable
fun PlaylistMosaicCover(
    songs: List<AudioFile>,
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp
) {
    val shape = RoundedCornerShape(corner)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    // Aynı albümden gelen şarkılar mozaikte tekrar etmesin.
    val covers = remember(songs) {
        songs.distinctBy { if (it.albumId != 0L) it.albumId else it.id }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.24f), secondary.copy(alpha = 0.12f))
                )
            )
            .clearAndSetSemantics { }
    ) {
        when {
            covers.isEmpty() -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(26.dp)
                )
            }

            covers.size == 1 -> {
                AlbumArtImage(covers.first(), Modifier.fillMaxSize(), isHQ = true)
            }

            covers.size == 2 -> {
                Row(Modifier.fillMaxSize()) {
                    AlbumArtImage(
                        covers[0],
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    AlbumArtImage(
                        covers[1],
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            covers.size == 3 -> {
                Row(Modifier.fillMaxSize()) {
                    AlbumArtImage(
                        covers[0],
                        Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    Column(Modifier.weight(1f)) {
                        AlbumArtImage(
                            covers[1],
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        AlbumArtImage(
                            covers[2],
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        AlbumArtImage(
                            covers[0],
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AlbumArtImage(
                            covers[1],
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(Modifier.weight(1f)) {
                        AlbumArtImage(
                            covers[2],
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AlbumArtImage(
                            covers[3],
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistArtistCover(
    artistName: String,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(primary, secondary, tertiary, primary)))
            .padding(2.dp)
            .clip(CircleShape)
            .clearAndSetSemantics { }
    ) {
        ArtistImage(
            artistName = artistName,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            isHQ = true
        )
    }
}

@Composable
fun PlaylistSmartCover(
    modifier: Modifier = Modifier
) {
    val pink = Color(0xFFFF4D8D)
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        pink.copy(alpha = 0.85f),
                        pink.copy(alpha = 0.42f),
                        primary.copy(alpha = 0.32f)
                    )
                )
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}
