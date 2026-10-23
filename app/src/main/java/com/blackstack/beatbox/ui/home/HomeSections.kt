package com.blackstack.beatbox.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AlbumArtImage
import com.blackstack.beatbox.ArtistImage
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.MusicVisualizer
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import java.util.Calendar

// ---------------------------------------------------------------------------
// Selamlama
// ---------------------------------------------------------------------------

@Composable
internal fun HomeGreeting(
    strings: Strings,
    songCount: Int,
    playlistCount: Int,
    onShuffleAll: () -> Unit,
    listState: LazyListState
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = strings.get(greetingKeyForHour(hour))
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                val offset = if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset
                } else {
                    360
                }
                translationY = -offset * 0.10f
                alpha = (1f - offset / 720f).coerceIn(0.45f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "BEATBOX",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp,
                color = primary.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = greeting,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = strings.get("home_subtitle"),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeCountPill("$songCount ${strings.get("songs")}")
                HomeCountPill("$playlistCount ${strings.get("playlists")}")
            }
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.26f),
                            secondary.copy(alpha = 0.16f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clearAndSetSemantics {
                    contentDescription = strings.get("shuffle")
                    role = Role.Button
                    onClick {
                        onShuffleAll()
                        true
                    }
                }
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShuffleAll()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeCountPill(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

// ---------------------------------------------------------------------------
// Hero: devam eden çalma veya keşfet daveti
// ---------------------------------------------------------------------------

@Composable
internal fun HomeHero(
    currentAudio: AudioFile?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    onShuffleAll: () -> Unit,
    songCount: Int,
    strings: Strings
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current
    val reducedMotion = LocalReducedMotion.current
    val audio = currentAudio
    val playing = audio != null && isPlaying

    var rawProgress by remember(audio?.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(audio?.id, isPlaying) {
        if (audio == null) {
            rawProgress = 0f
            return@LaunchedEffect
        }
        while (true) {
            rawProgress = progress().coerceIn(0f, 1f)
            delay(if (isPlaying) 700 else 2_000)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (reducedMotion) tween(0) else tween(420),
        label = "home_hero_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.42f),
                spotColor = secondary.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.30f),
                        secondary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        primary.copy(alpha = 0.26f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = shape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (audio != null) onOpenPlayer() else onShuffleAll()
            }
            .padding(16.dp)
    ) {
        // Sağ üstteki yumuşak aurora lekesi, karta derinlik verir.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = (-40).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (audio != null) {
                    AlbumArtImage(audio, Modifier.fillMaxSize(), isHQ = true)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primary.copy(alpha = 0.82f),
                                        secondary.copy(alpha = 0.72f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (audio != null) {
                        if (playing) strings.get("now_playing_badge") else strings.get("continue_listening")
                    } else {
                        strings.get("welcome")
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = primary.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = audio?.title ?: strings.get("app_name"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = audio?.artist ?: "$songCount ${strings.get("songs")}",
                    fontSize = 12.sp,
                    color = onSurface.copy(alpha = 0.66f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (audio != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(primary, secondary)))
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${strings.get("shuffle")} · ${strings.get("start")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(primary, secondary.copy(alpha = 0.90f))
                        )
                    )
                    .clearAndSetSemantics {
                        contentDescription = if (playing) strings.get("pause") else strings.get("play")
                        role = Role.Button
                        onClick {
                            if (audio != null) onPlayPause() else onShuffleAll()
                            true
                        }
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (audio != null) onPlayPause() else onShuffleAll()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bölüm başlıkları ve raflar
// ---------------------------------------------------------------------------

@Composable
private fun HomeSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun HomeForYouSection(
    songs: List<AudioFile>,
    currentAudioId: Long?,
    isPlaying: Boolean,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    strings: Strings,
    onPlaySong: (AudioFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            title = strings.get("for_you"),
            tint = MaterialTheme.colorScheme.primary
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                HomeTopSongCard(
                    song = song,
                    rank = index + 1,
                    isCurrent = song.id == currentAudioId,
                    isPlaying = isPlaying,
                    visualizerLevel = visualizerLevel,
                    visualizerWaveform = visualizerWaveform,
                    onClick = { onPlaySong(song) }
                )
            }
        }
    }
}

@Composable
private fun HomeTopSongCard(
    song: AudioFile,
    rank: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.955f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BeatBoxMotion.Snappy,
        label = "home_top_song_press"
    )
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .width(142.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.35f),
                spotColor = primary.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AlbumArtImage(song, Modifier.fillMaxSize(), isHQ = true)
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.48f)),
                    contentAlignment = Alignment.Center
                ) {
                    MusicVisualizer(
                        isPlaying = true,
                        level = visualizerLevel,
                        waveform = visualizerWaveform,
                        modifier = Modifier.height(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) primary else onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun HomeRecentlyPlayedSection(
    songs: List<AudioFile>,
    currentAudioId: Long?,
    strings: Strings,
    onPlaySong: (AudioFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(
            icon = Icons.Rounded.History,
            title = strings.get("recently_played"),
            tint = MaterialTheme.colorScheme.secondary
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                HomeWideSongCard(
                    song = song,
                    isCurrent = song.id == currentAudioId,
                    onClick = { onPlaySong(song) }
                )
            }
        }
    }
}

@Composable
private fun HomeWideSongCard(
    song: AudioFile,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.965f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BeatBoxMotion.Snappy,
        label = "home_recent_press"
    )
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .width(236.dp)
            .height(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            a = song,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            isHQ = true
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) primary else onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 10.5.sp,
                color = onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = primary.copy(alpha = 0.78f),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
internal fun HomeRecentlyAddedSection(
    songs: List<AudioFile>,
    currentAudioId: Long?,
    strings: Strings,
    onPlaySong: (AudioFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(
            icon = Icons.Rounded.NewReleases,
            title = strings.get("recently_added"),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            songs.chunked(2).forEach { rowSongs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSongs.forEach { song ->
                        HomeAddedCard(
                            song = song,
                            isCurrent = song.id == currentAudioId,
                            onClick = { onPlaySong(song) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSongs.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAddedCard(
    song: AudioFile,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.96f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BeatBoxMotion.Snappy,
        label = "home_added_press"
    )
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .height(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            a = song,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            isHQ = true
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) primary else onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 10.5.sp,
                color = onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primary)
            )
        }
    }
}

@Composable
internal fun HomeArtistsSection(
    artists: List<Map.Entry<String, List<AudioFile>>>,
    strings: Strings,
    onPlayArtist: (List<AudioFile>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(
            icon = Icons.Rounded.Person,
            title = strings.get("artist"),
            tint = MaterialTheme.colorScheme.primary
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(artists, key = { it.key }) { entry ->
                HomeArtistBubble(
                    artistName = entry.key,
                    songCount = entry.value.size,
                    strings = strings,
                    onClick = { onPlayArtist(entry.value) }
                )
            }
        }
    }
}

@Composable
private fun HomeArtistBubble(
    artistName: String,
    songCount: Int,
    strings: Strings,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.95f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BeatBoxMotion.Snappy,
        label = "home_artist_press"
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .width(88.dp)
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
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(primary, secondary, tertiary, primary)))
                .padding(2.5.dp)
                .clip(CircleShape)
        ) {
            ArtistImage(
                artistName = artistName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clearAndSetSemantics { },
                isHQ = false
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = artistName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$songCount ${strings.get("songs")}",
            fontSize = 10.sp,
            color = onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HomeListeningStat(
    totalMinutes: Long,
    strings: Strings
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.16f),
                        secondary.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.get("total_listen"),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$totalMinutes ${strings.get("minutes")}",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun HomeEmptyLibrary(strings: Strings) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicOff,
                contentDescription = null,
                tint = primary.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = strings.get("empty_library"),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            textAlign = TextAlign.Center
        )
    }
}
