package com.blackstack.beatbox.ui.playlists

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.components.PlaylistArtistCover
import com.blackstack.beatbox.ui.components.PlaylistMosaicCover
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

/**
 * Çalma listeleri sekmesinin kart bileşenleri.
 *
 * Ana ekran akışını [PlaylistsScreen] yönetir; bu dosya hero, akıllı liste,
 * sanatçı halkaları, kullanıcı listesi ve boş durum görsellerini barındırır.
 */

private val favoritesPink = Color(0xFFFF4D8D)

@Composable
internal fun PlaylistsHero(
    songCount: Int,
    artistCount: Int,
    coverSongs: List<AudioFile>,
    strings: Strings
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.36f),
                spotColor = secondary.copy(alpha = 0.26f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.30f),
                        secondary.copy(alpha = 0.15f),
                        surfaceVariant.copy(alpha = 0.74f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        primary.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-52).dp)
                .size(170.dp)
                .background(
                    Brush.radialGradient(
                        listOf(tertiary.copy(alpha = 0.26f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(Modifier.padding(20.dp)) {
            // Kapak üst satırda küçük bir rozet gibi durur; başlık tam
            // genişlikte kalır ve uzun dillerde kesilmez.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BEATBOX",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.6.sp,
                    color = primary.copy(alpha = 0.95f)
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .graphicsLayer { rotationZ = -5f }
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = primary.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    PlaylistMosaicCover(
                        songs = coverSongs,
                        modifier = Modifier.fillMaxSize(),
                        corner = 16.dp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.get("playlists"),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "$songCount ${strings.get("songs")} · $artistCount ${strings.get("artist")}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Bölüm başlıkları
// ---------------------------------------------------------------------------

@Composable
internal fun PlaylistSectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        if (count != null) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Akıllı favoriler
// ---------------------------------------------------------------------------

@Composable
internal fun SmartFavoritesCard(
    songCount: Int,
    strings: Strings,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.985f else 1f,
        animationSpec = if (reducedMotion) androidx.compose.animation.core.tween(0) else BeatBoxMotion.Snappy,
        label = "smart_card_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        favoritesPink.copy(alpha = 0.26f),
                        primary.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.035f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(favoritesPink.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f))
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(10.dp, CircleShape, ambientColor = favoritesPink.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(favoritesPink, favoritesPink.copy(alpha = 0.62f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.get("favorites"),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(favoritesPink.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.get("smart_playlist"),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = favoritesPink
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "$songCount ${strings.get("songs")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = favoritesPink.copy(alpha = 0.8f),
            modifier = Modifier.size(19.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Sanatçı listeleri
// ---------------------------------------------------------------------------

@Composable
internal fun ArtistPlaylistCard(
    artistName: String,
    songCount: Int,
    strings: Strings,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.96f else 1f,
        animationSpec = if (reducedMotion) androidx.compose.animation.core.tween(0) else BeatBoxMotion.Snappy,
        label = "artist_card_press"
    )

    Column(
        modifier = Modifier
            // 80 dp: 360 dp genişlikte üç halka FAB'ın solunda kalır.
            .width(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaylistArtistCover(
            artistName = artistName,
            modifier = Modifier
                .size(74.dp)
                .shadow(10.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = artistName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$songCount ${strings.get("songs")}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
            maxLines = 1
        )
    }
}

@Composable
internal fun CreateAllArtistPlaylistsRow(
    artistCount: Int,
    strings: Strings,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Sağdaki FAB'ın satırın okunu örtmemesi için boşluk bırakılır.
            .padding(start = 20.dp, end = 92.dp, top = 8.dp, bottom = 8.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.get("create_artist_playlists"),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$artistCount ${strings.get("artists_found")}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = primary.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Kullanıcı listeleri
// ---------------------------------------------------------------------------

@Composable
internal fun PlaylistCard(
    playlist: Playlist,
    coverSongs: List<AudioFile>,
    strings: Strings,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.985f else 1f,
        animationSpec = if (reducedMotion) androidx.compose.animation.core.tween(0) else BeatBoxMotion.Snappy,
        label = "playlist_card_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.025f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.075f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistMosaicCover(
            songs = coverSongs,
            modifier = Modifier.size(58.dp),
            corner = 15.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${playlist.songIds.size} ${strings.get("songs")}",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                maxLines = 1
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = strings.get("edit"),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = strings.get("delete"),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Boş durum
// ---------------------------------------------------------------------------

@Composable
internal fun EmptyPlaylists(
    strings: Strings,
    onCreatePlaylist: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = 158.dp, height = 118.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .offset(x = (-24).dp, y = 4.dp)
                    .graphicsLayer { rotationZ = -13f }
                    .clip(RoundedCornerShape(22.dp))
                    .background(primary.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
            )
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .offset(x = 24.dp, y = 2.dp)
                    .graphicsLayer { rotationZ = 12f }
                    .clip(RoundedCornerShape(22.dp))
                    .background(secondary.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { rotationZ = -3f }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = primary.copy(alpha = 0.42f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primary.copy(alpha = 0.32f), secondary.copy(alpha = 0.16f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.92f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = strings.get("empty_playlists"),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.get("empty_playlists_hint"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .shadow(10.dp, RoundedCornerShape(percent = 50), ambientColor = primary.copy(alpha = 0.4f))
                .background(Brush.linearGradient(listOf(primary, secondary.copy(alpha = 0.9f))))
                .clickable(onClick = onCreatePlaylist)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = strings.get("new_playlist"),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
