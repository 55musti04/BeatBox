package com.blackstack.beatbox

import androidx.compose.animation.core.*
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
 * Modern theme-synchronized Apple-Music style immersive lyrics screen with
 * real-time character-by-character lighting & animated intro spinners.
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

    // Reset userIsScrolling after 4 seconds
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
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 130f) {
                            onDismiss()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) {
                            dragOffset += dragAmount
                        }
                    }
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
                    alpha = 0.35f
                }
        ) {
            AlbumArtImage(
                audio,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(110.dp)
            )
        }

        // Dark gradient vignette overlay for high contrast text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f)
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Song & Artist Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = audio.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = audio.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                // Manual Search Button
                IconButton(
                    onClick = { showManualSearchDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
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
                        shape = RoundedCornerShape(12.dp)
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
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
                color = Color.White.copy(alpha = 0.9f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = strings.get("loading_lyrics"),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
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
                modifier = Modifier.size(54.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.get("no_lyrics_found"),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.get("retry"), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onManualSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
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
    onLineClick: (SyncedLyricsLine) -> Unit,
    onUserScroll: () -> Unit
) {
    val listState = rememberLazyListState()

    // Smooth auto-scrolling to keep active line centered
    LaunchedEffect(currentLineIndex, userIsScrolling) {
        if (!userIsScrolling && lyrics.lines.isNotEmpty() && currentLineIndex in lyrics.lines.indices) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -300
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 180.dp, bottom = 320.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(lyrics.lines.size) { index ->
            val line = lyrics.lines[index]
            val isCurrent = index == currentLineIndex
            val isPast = index < currentLineIndex

            val nextTimestamp = if (index < lyrics.lines.size - 1) lyrics.lines[index + 1].timestampMs else line.timestampMs + 4000L
            val lineDurationMs = (nextTimestamp - line.timestampMs).coerceAtLeast(1000L)

            AppleStyleLyricsLine(
                line = line,
                isCurrent = isCurrent,
                isPast = isPast,
                currentPositionMs = currentPositionMs,
                lineDurationMs = lineDurationMs,
                onClick = {
                    onUserScroll()
                    onLineClick(line)
                }
            )
        }
    }
}

@Composable
private fun FlowingKaraokeText(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier
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

    // Row used instead of FlowRow: FlowRow's newer overloads (with
    // FlowRowOverflow) are not reliably present across foundation-layout
    // versions, which caused a NoSuchMethodError on release builds. Row is
    // stable since Compose 1.0 and never crashes at runtime.
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

            // Smooth spring scale for active word
            val wordScale by animateFloatAsState(
                targetValue = when {
                    isWordActive -> 1.12f
                    isWordPast -> 1.0f
                    else -> 0.96f
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "word_scale_$index"
            )

            // Floating upward pop for active word (-4.dp)
            val wordOffsetY by animateFloatAsState(
                targetValue = if (isWordActive) -4f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "word_offset_$index"
            )

            // Character by character annotated string inside word (Cached on integer character change only!)
            val annotatedWord = remember(word, intCharIndex) {
                buildAnnotatedString {
                    for (i in word.indices) {
                        val absoluteCharIndex = wordStartChar + i
                        when {
                            absoluteCharIndex <= intCharIndex -> {
                                // Sung & Active character (Parıldayan Saf Beyaz)
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                ) {
                                    append(word[i])
                                }
                            }
                            else -> {
                                // Future character (Yarı Şeffaf Mat Beyaz)
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontWeight = FontWeight.SemiBold
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
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .padding(end = 10.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = annotatedWord,
                    fontSize = 28.sp,
                    lineHeight = 38.sp
                )
            }
        }
    }
}

@Composable
private fun AppleStyleLyricsLine(
    line: SyncedLyricsLine,
    isCurrent: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    lineDurationMs: Long,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isCurrent -> 1f
            isPast -> 0.35f
            else -> 0.45f
        },
        animationSpec = tween(400),
        label = "apple_line_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.04f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "apple_line_scale"
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
        if (isIntro) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                if (isCurrent) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White.copy(alpha = alpha),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    text = if (isCurrent) "Müzik Girişi..." else "♪ ♪ ♪",
                    color = Color.White.copy(alpha = alpha),
                    fontSize = if (isCurrent) 26.sp else 20.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            }
        } else if (isCurrent) {
            val progress = ((currentPositionMs - line.timestampMs).toFloat() / lineDurationMs).coerceIn(0f, 1f)
            FlowingKaraokeText(
                text = line.text,
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = line.text,
                color = Color.White.copy(alpha = alpha),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                lineHeight = 30.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
