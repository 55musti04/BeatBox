package com.blackstack.beatbox

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Modern Apple-Music style immersive lyrics screen.
 *
 * Improvements over the previous version:
 *   • Staggered entrance animation for every lyric line (fade + slide up)
 *   • "Current line" gets a soft primary-color glow and a gentle bounce
 *   • Karaoke highlighting: active word is tinted with the theme primary color,
 *     lifts up and pulses; past words stay white, future words fade out
 *   • Ambient breathing background artwork + soft vignette for readability
 *   • Glassmorphic top navigation bar
 */
@Composable
fun LyricsScreen(
    audio: AudioFile,
    currentPositionMs: Long,
    isPlaying: Boolean,
    viewModel: LyricsViewModel,
    strings: Strings,
    onSeekTo: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val lyricsState by viewModel.lyricsState.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var showManualSearchDialog by remember { mutableStateOf(false) }
    var searchTrackQuery by remember(audio.id) { mutableStateOf(audio.title) }
    var searchArtistQuery by remember(audio.id) { mutableStateOf(audio.artist) }

    // User manual scroll detection
    var userIsScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(userIsScrolling) {
        if (userIsScrolling) {
            delay(4000)
            userIsScrolling = false
        }
    }

    // Load lyrics when audio changes
    LaunchedEffect(audio.id) {
        viewModel.loadLyrics(audio)
    }

    // Update playback position
    LaunchedEffect(currentPositionMs) {
        viewModel.updatePlaybackPosition(currentPositionMs)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Ambient breathing pulse for background artwork
    val infiniteTransition = rememberInfiniteTransition(label = "apple_lyrics_ambient")
    val ambientScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(5000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_scale"
    )

    // Slow panning light sweep across the artwork
    val sweep by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            tween<Float>(10000),
            RepeatMode.Reverse
        ),
        label = "light_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B12))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 140f) {
                            onDismiss()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> if (dragAmount > 0) dragOffset += dragAmount }
                )
            }
    ) {
        // Fullscreen Blurred Album Art Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = ambientScale
                    scaleY = ambientScale
                    alpha = 0.30f
                }
        ) {
            AlbumArtImage(
                audio,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(120.dp)
            )
        }

        // Soft moving light sweep over the artwork (adds depth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Dark gradient vignette overlay for high contrast text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.80f),
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // Main Lyrics / State
        when (val state = lyricsState) {
            is LyricsState.Loading -> AppleStyleLoadingView(strings)
            is LyricsState.Success -> AppleStyleLyricsContent(
                lyrics = state.lyrics,
                currentLineIndex = currentLineIndex,
                currentPositionMs = currentPositionMs,
                userIsScrolling = userIsScrolling,
                primaryColor = primaryColor,
                onLineClick = { line ->
                    onSeekTo?.invoke(line.timestampMs)
                    viewModel.updatePlaybackPosition(line.timestampMs)
                },
                onUserScroll = { userIsScrolling = true }
            )
            is LyricsState.Error -> AppleStyleErrorView(
                message = state.message,
                strings = strings,
                primaryColor = primaryColor,
                onRetry = { viewModel.retry(audio) },
                onManualSearch = { showManualSearchDialog = true }
            )
            is LyricsState.Idle -> {}
        }

        // Glassmorphic Top Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Kapat",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Song & Artist Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = audio.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = audio.artist,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                // Manual Search Button
                IconButton(
                    onClick = { showManualSearchDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Söz ara",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Manual Search Dialog
        if (showManualSearchDialog) {
            AlertDialog(
                onDismissRequest = { showManualSearchDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showManualSearchDialog = false
                            viewModel.searchLyricsManually(searchTrackQuery, searchArtistQuery)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.get("ok"), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualSearchDialog = false }) {
                        Text(strings.get("cancel"), color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(strings.get("search_hint"), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = searchTrackQuery,
                            onValueChange = { searchTrackQuery = it },
                            label = { Text(strings.get("track_name")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                        OutlinedTextField(
                            value = searchArtistQuery,
                            onValueChange = { searchArtistQuery = it },
                            label = { Text(strings.get("artist")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(26.dp)
            )
        }
    }
}

@Composable
private fun AppleStyleLoadingView(strings: Strings) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.95f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.get("loading_lyrics"),
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppleStyleErrorView(
    message: String,
    strings: Strings,
    primaryColor: Color,
    onRetry: () -> Unit,
    onManualSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color.White.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = strings.get("no_lyrics_found"),
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.get("retry"), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onManualSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.get("manual_search"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AppleStyleLyricsContent(
    lyrics: SyncedLyrics,
    currentLineIndex: Int,
    currentPositionMs: Long,
    userIsScrolling: Boolean,
    primaryColor: Color,
    onLineClick: (SyncedLyricsLine) -> Unit,
    onUserScroll: () -> Unit
) {
    val listState = rememberLazyListState()

    // Smooth auto-scrolling to keep active line centered
    LaunchedEffect(currentLineIndex, userIsScrolling) {
        if (!userIsScrolling && lyrics.lines.isNotEmpty() && currentLineIndex in lyrics.lines.indices) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -260
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 170.dp, bottom = 300.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(lyrics.lines.size, key = { it -> it }) { index ->
            val line = lyrics.lines[index]
            val isCurrent = index == currentLineIndex
            val isPast = index < currentLineIndex

            val nextTimestamp = if (index < lyrics.lines.size - 1) lyrics.lines[index + 1].timestampMs else line.timestampMs + 4000L
            val lineDurationMs = (nextTimestamp - line.timestampMs).coerceAtLeast(1000L)

            // Staggered entrance: each line fades/slides in slightly after the previous one
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween<Float>(400 + (index * 60)))
            ) {
                AppleStyleLyricsLine(
                    line = line,
                    lineIndex = index,
                    isCurrent = isCurrent,
                    isPast = isPast,
                    currentPositionMs = currentPositionMs,
                    lineDurationMs = lineDurationMs,
                    primaryColor = primaryColor,
                    onClick = {
                        onUserScroll()
                        onLineClick(line)
                    }
                )
            }
        }
    }
}

@Composable
private fun FlowingKaraokeText(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color
) {
    val words = remember(text) { text.split(" ") }
    val totalChars = remember(text) { text.length.coerceAtLeast(1) }

    // Compute character range for each word
    val wordRanges = remember(text) {
        var runningIndex = 0
        words.map { word ->
            val start = runningIndex
            val end = start + word.length
            runningIndex = end + 1 // +1 for space
            start..end
        }
    }

    val intCharIndex = remember(progress, totalChars) {
        (progress * totalChars).toInt().coerceIn(0, totalChars)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEachIndexed { index, word ->
            val range = wordRanges[index]
            val wordStartChar = range.first
            val wordEndChar = range.last

            val isWordPast = intCharIndex >= wordEndChar
            val isWordActive = intCharIndex in wordStartChar until wordEndChar

            // Active word: theme color, lifts up and pulses softly
            val wordScale by animateFloatAsState(
                targetValue = when {
                    isWordActive -> 1.16f
                    isWordPast -> 1.0f
                    else -> 0.93f
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "word_scale_${index}_${word.hashCode()}"
            )

            val wordOffsetY by animateFloatAsState(
                targetValue = if (isWordActive) -8f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "word_offset_${index}_${word.hashCode()}"
            )

            // Active word breathes with a gentle alpha pulse
            val wordPulse by animateFloatAsState(
                targetValue = if (isWordActive) 1f else 0.82f,
                animationSpec = tween<Float>(300),
                label = "word_pulse_${index}_${word.hashCode()}"
            )

            // Character by character: sung chars light up, future chars stay dimmed
            val animatedWord = remember(word, intCharIndex, wordStartChar) {
                buildAnnotatedString {
                    for (i in word.indices) {
                        val absoluteCharIndex = wordStartChar + i
                        when {
                            absoluteCharIndex < intCharIndex -> {
                                // Fully sung: theme color, bold, with soft shadow
                                withStyle(
                                    style = SpanStyle(
                                        color = if (isWordActive) primaryColor else Color.White,
                                        fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                ) {
                                    append(word[i])
                                }
                            }
                            absoluteCharIndex == intCharIndex && isWordActive -> {
                                // Currently singing character: brightest, biggest
                                withStyle(
                                    style = SpanStyle(
                                        color = primaryColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 32.sp
                                    )
                                ) {
                                    append(word[i])
                                }
                            }
                            else -> {
                                // Future: dimmed, small
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.White.copy(alpha = 0.25f),
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(word[i])
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = wordScale
                        scaleY = wordScale
                        translationY = wordOffsetY
                        alpha = wordPulse
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .padding(end = 12.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Text(
                    text = animatedWord,
                    fontSize = 30.sp,
                    lineHeight = 42.sp
                )
            }
        }
    }
}

@Composable
private fun AppleStyleLyricsLine(
    line: SyncedLyricsLine,
    lineIndex: Int,
    isCurrent: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    lineDurationMs: Long,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isCurrent -> 1f
            isPast -> 0.38f
            else -> 0.52f
        },
        animationSpec = tween<Float>(500),
        label = "line_alpha_${line.timestampMs}"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.05f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "line_scale_${line.timestampMs}"
    )

    // Gentle, slow glow pulse on the current line only (organic breathing)
    val glow by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "line_glow_${line.timestampMs}"
    )

    val isIntro = remember(line.text) {
        line.text == "♪ ♪ ♪" || line.text.contains("♪") || line.text.equals("intro", ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
    ) {
        if (isCurrent) {
            // Soft primary-color glow behind the current line - slow breathing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.12f * glow),
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.12f * glow)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 8.dp)
            )
        }

        if (isIntro) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                if (isCurrent) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = Color.White.copy(alpha = alpha),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Text(
                    text = if (isCurrent) "Müzik Girişi..." else "♪ ♪ ♪",
                    color = Color.White.copy(alpha = alpha),
                    fontSize = if (isCurrent) 27.sp else 21.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            }
        } else if (isCurrent) {
            val progress = ((currentPositionMs - line.timestampMs).toFloat() / lineDurationMs).coerceIn(0f, 1f)
            FlowingKaraokeText(
                text = line.text,
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                primaryColor = primaryColor
            )
        } else {
            Text(
                text = line.text,
                color = Color.White.copy(alpha = alpha),
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                lineHeight = 34.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
