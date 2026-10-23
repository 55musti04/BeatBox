package com.blackstack.beatbox.ui.player

import com.blackstack.beatbox.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    player: MainActivity,
    a: AudioFile,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    animDir: Int,
    strings: Strings,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onAdd: () -> Unit,
    currentPlaylist: List<AudioFile> = emptyList(),
    onSelectFromQueue: ((AudioFile) -> Unit)? = null,
    onOpenEQ: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    visualizerLevel: Float = 0f,
    visualizerWaveform: FloatArray = FloatArray(0)
) {
    val context = LocalContext.current
    var pos by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }

    // Volume Control using AudioManager
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember(audioManager) { mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) }

    // Create LyricsViewModel instance with error handling
    val lyricsViewModel: LyricsViewModel = viewModel(
        key = "lyrics_viewmodel",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return (try {
                    val ctx = player.applicationContext
                    val database = LyricsDatabase.getDatabase(ctx)
                    val lyricsProvider = LyricsProvider(
                        context = ctx,
                        lyricsCache = database.lyricsCacheDao()
                    )
                    LyricsViewModel(lyricsProvider)
                } catch (e: Exception) {
                    android.util.Log.e("FullScreenPlayer", "Error creating LyricsViewModel", e)
                    LyricsViewModel(null)
                }) as T
            }
        }
    )

    // Set up position callback when lyrics screen is shown
    LaunchedEffect(showLyrics) {
        if (showLyrics) {
            player.musicService?.setLyricsPositionCallback { position ->
                lyricsViewModel.updatePlaybackPosition(position)
            }
        } else {
            player.musicService?.removeLyricsPositionCallback()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.musicService?.removeLyricsPositionCallback()
        }
    }

    // Extract dominant and vibrant colors from album art
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val defaultBackgroundColor = MaterialTheme.colorScheme.background

    var dominantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }
    var vibrantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }

    LaunchedEffect(a.id, defaultPrimaryColor) {
        val primaryArgb = defaultPrimaryColor.toArgb()
        withContext(Dispatchers.IO) {
            try {
                AlbumArtCache.bitmapCache.get(a.id)?.let { bitmap ->
                    val palette = Palette.from(bitmap).generate()
                    withContext(Dispatchers.Main) {
                        dominantColor = Color(palette.getDominantColor(primaryArgb))
                        vibrantColor = Color(
                            palette.getVibrantColor(
                                palette.getLightVibrantColor(
                                    palette.getDarkVibrantColor(primaryArgb)
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FullScreenPlayer", "Error extracting colors", e)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        withContext(Dispatchers.Default) {
            while (isPlaying) {
                if (!drag) {
                    val currentPos = player.getCurrentPosition().toFloat()
                    withContext(Dispatchers.Main) {
                        pos = currentPos
                    }
                }
                delay(100)
            }
        }
    }

    // Pulse animation for album art glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.55f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Animated color transition
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dominant_color"
    )

    val animatedVibrant by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "vibrant_color"
    )

    val dynamicGradient = remember(animatedDominant, animatedVibrant) {
        Brush.verticalGradient(
            colors = listOf(
                animatedDominant.copy(alpha = 0.85f),
                animatedVibrant.copy(alpha = 0.6f),
                defaultBackgroundColor
            ),
            startY = 0f,
            endY = 2200f
        )
    }

    // Outer Box: Fully Opaque, Consumes Clicks to Prevent Touch Pass-Through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume clicks to block touch pass-through to background screen */ }
            .background(Color(0xFF0C0D15)) // 100% Opaque Solid Dark Background
            .background(dynamicGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse, modifier = Modifier.iconButtonShadow()) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = strings.get("now_playing_badge"),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onOpenEQ != null) {
                        IconButton(onClick = onOpenEQ, modifier = Modifier.iconButtonShadow()) {
                            Icon(
                                Icons.Rounded.Equalizer,
                                contentDescription = "Equalizer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (onOpenSleepTimer != null) {
                        IconButton(onClick = onOpenSleepTimer, modifier = Modifier.iconButtonShadow()) {
                            Icon(
                                Icons.Rounded.Nightlight,
                                contentDescription = "Sleep Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(onClick = onAdd, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Album Art Area with Swipe Gestures and Glowing Pulse Effect
            var totalDragX by remember { mutableFloatStateOf(0f) }
            var totalDragY by remember { mutableFloatStateOf(0f) }

            AnimatedContent(
                targetState = a,
                transitionSpec = {
                    val duration = 380
                    val enter = if (animDir > 0) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 0.9f, animationSpec = tween(duration))
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 1.1f, animationSpec = tween(duration))
                    }
                    val exit = if (animDir > 0) {
                        slideOutHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(duration))
                    } else {
                        slideOutHorizontally(animationSpec = tween(duration)) { it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(duration))
                    }
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "song_transition"
            ) { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.pointerInput(c.id) {
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDragEnd = {
                                    if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 100f) {
                                        if (totalDragX < 0) onNext() else onPrev()
                                    } else if (totalDragY > 140f) {
                                        onCollapse()
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                }
                            )
                        }
                    ) {
                        // Ambient Glowing Aura
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(330.dp)
                                    .scale(pulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                animatedVibrant.copy(alpha = pulseAlpha),
                                                animatedDominant.copy(alpha = pulseAlpha * 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                        }

                        // Album Art Card
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            elevation = CardDefaults.cardElevation(16.dp),
                            modifier = Modifier
                                .size(260.dp)
                                .cardShadow()
                        ) {
                            AlbumArtImage(c, Modifier.fillMaxSize(), true)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        c.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        c.artist,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // Quality / Duration Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${formatTime(c.duration)} • HQ AUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
                            )
                        }
                    }
                }
            }

            // Sleek Floating Action Toolbar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFav, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFF4081) else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showLyrics = true },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.Rounded.Subtitles,
                            contentDescription = strings.get("lyrics"),
                            tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueueSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showVolumeControl = !showVolumeControl },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            if (currentVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Volume",
                            tint = if (showVolumeControl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSongInfoDialog = true },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${a.title} - ${a.artist}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🎵 BeatBox'ta dinliyorum:\n\"${a.title}\" - ${a.artist}\n\n#BeatBox #Music"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, strings.get("share_song")))
                        },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Inline Volume Bar
            AnimatedVisibility(
                visible = showVolumeControl,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (currentVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { vol ->
                                currentVolume = vol.toInt()
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${((currentVolume.toFloat() / maxVolume) * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Real-Time Audio Visualizer Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(18.dp))
            }

            // Seek Bar Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Text(formatTime(pos.toLong()), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(" / ${formatTime(a.duration)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                    }

                    // Speed Selector
                    Box {
                        TextButton(onClick = { speedMenu = true }, modifier = Modifier.height(30.dp)) {
                            Text(
                                "${currentSpeed}x",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s}x", fontWeight = if (s == currentSpeed) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        currentSpeed = s
                                        player.setPlaybackSpeed(s)
                                        speedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Slider(
                    value = pos,
                    onValueChange = {
                        drag = true
                        pos = it
                    },
                    onValueChangeFinished = {
                        player.seekTo(pos.toInt())
                        drag = false
                    },
                    valueRange = 0f..(a.duration.toFloat().takeIf { it > 0 } ?: 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.2f)
                    )
                )
            }

            // Main Playback Controls Capsule
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onShuffle, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(24.dp),
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }

                    IconButton(onClick = onPrev, modifier = Modifier.size(48.dp).iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp).iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onRepeat, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            modifier = Modifier.size(24.dp),
                            tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
        }

        // Lyrics Screen Overlay with smooth slide-up & fade animation
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(250, easing = FastOutLinearInEasing)) { it } + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            LyricsScreen(
                audio = a,
                currentPositionMs = pos.toLong(),
                isPlaying = isPlaying,
                viewModel = lyricsViewModel,
                strings = strings,
                onSeekTo = { ms -> player.seekTo(ms.toInt()) },
                onDismiss = { showLyrics = false }
            )
        }

        // Queue Bottom Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Oynatma Sırası",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(currentPlaylist) { song ->
                            SongItem(
                                a = song,
                                strings = strings,
                                isCurrent = song.id == a.id,
                                isPlaying = isPlaying && song.id == a.id,
                                isFavorite = false,
                                onFavClick = {},
                                onDelete = {},
                                onClick = {
                                    onSelectFromQueue?.invoke(song)
                                    showQueueSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Song Info Dialog
        if (showSongInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSongInfoDialog = false },
                title = { Text(text = a.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sanatçı: ${a.artist}")
                        Text("Süre: ${formatTime(a.duration)}")
                        Text("URI: ${a.uri}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSongInfoDialog = false }) {
                        Text("Tamam")
                    }
                }
            )
        }
    }
}
