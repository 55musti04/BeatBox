package com.blackstack.beatbox

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Enhanced lyrics screen with beautiful animations and effects
 */
@Composable
fun LyricsScreen(
    audio: AudioFile,
    currentPositionMs: Long,
    isPlaying: Boolean,
    viewModel: LyricsViewModel,
    strings: Strings,
    onDismiss: () -> Unit
) {
    val lyricsState by viewModel.lyricsState.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()
    
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    // Load lyrics when audio changes
    LaunchedEffect(audio.id) {
        viewModel.loadLyrics(audio)
    }
    
    // Update playback position
    LaunchedEffect(currentPositionMs) {
        viewModel.updatePlaybackPosition(currentPositionMs)
    }
    
    // Animated gradient colors
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0B2E).copy(alpha = 0.95f + gradientOffset * 0.05f),
                        Color(0xFF2D1B4E).copy(alpha = 0.9f + gradientOffset * 0.1f),
                        Color(0xFF4A2C6D),
                        Color(0xFF6B3FA0).copy(alpha = 0.8f),
                        Color(0xFF1A0B2E)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100) {
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
        // Blurred album art background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.15f }
        ) {
            AlbumArtImage(
                audio,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
            )
        }
        
        // Animated particles/stars effect
        AnimatedStarsBackground()
        
        when (val state = lyricsState) {
            is LyricsState.Loading -> EnhancedLoadingView(strings)
            is LyricsState.Success -> EnhancedLyricsContent(
                lyrics = state.lyrics,
                currentLineIndex = currentLineIndex,
                audio = audio,
                currentPositionMs = currentPositionMs,
                strings = strings
            )
            is LyricsState.Error -> EnhancedErrorView(
                message = state.message,
                strings = strings,
                onRetry = { viewModel.retry(audio) }
            )
            is LyricsState.Idle -> {}
        }
        
        // Drag indicator at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Enhanced close button with glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .blur(12.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        CircleShape
                    )
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF2D1B4E),
                                Color(0xFF1A0B2E)
                            )
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Animated stars/particles background effect
 */
@Composable
private fun AnimatedStarsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    
    repeat(15) { index ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = (index * 50f),
            targetValue = (index * 50f + 800f),
            animationSpec = infiniteRepeatable(
                animation = tween((3000 + index * 200), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "star$index"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000 + index * 100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "starAlpha$index"
        )
        
        Box(
            modifier = Modifier
                .offset(x = (index * 25 % 350).dp, y = (offsetY % 800).dp)
                .size((2 + index % 3).dp)
                .background(Color.White.copy(alpha = alpha), CircleShape)
        )
    }
}

/**
 * Enhanced loading view with pulsing animation
 */
@Composable
private fun EnhancedLoadingView(strings: Strings) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingScale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing circle background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .blur(20.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        CircleShape
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = (-120).dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 5.dp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = strings.get("loading_lyrics"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Enhanced error view with beautiful animations
 */
@Composable
private fun EnhancedErrorView(
    message: String,
    strings: Strings,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated error icon with glow
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .blur(30.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                
                // Icon container
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(12.dp, CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF4A2C6D),
                                    Color(0xFF2D1B4E)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = strings.get("no_lyrics_found"),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 15.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Glowing retry button
            Box {
                // Button glow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(16.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                )
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(60.dp)
                        .widthIn(min = 180.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            strings.get("retry"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced lyrics content with progress bar and beautiful animations
 */
@Composable
private fun EnhancedLyricsContent(
    lyrics: SyncedLyrics,
    currentLineIndex: Int,
    audio: AudioFile,
    currentPositionMs: Long,
    strings: Strings
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll with smooth animation
    LaunchedEffect(currentLineIndex) {
        if (lyrics.lines.isNotEmpty() && currentLineIndex in lyrics.lines.indices) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -250
            )
        }
    }
    
    // Calculate progress
    val progress = if (audio.duration > 0) {
        (currentPositionMs.toFloat() / audio.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Enhanced header with glassmorphism effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            // Glassmorphism background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = audio.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 26.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = audio.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Progress bar
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    ),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
        
        // Lyrics list with enhanced styling
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 180.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(lyrics.lines.size) { index ->
                EnhancedLyricsLine(
                    line = lyrics.lines[index],
                    isCurrent = index == currentLineIndex,
                    isPast = index < currentLineIndex
                )
            }
        }
    }
}

/**
 * Enhanced lyrics line with glow effect and smooth animations
 */
@Composable
private fun EnhancedLyricsLine(
    line: SyncedLyricsLine,
    isCurrent: Boolean,
    isPast: Boolean
) {
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Alpha animation
    val alpha by animateFloatAsState(
        targetValue = when {
            isCurrent -> 1f
            isPast -> 0.35f
            else -> 0.55f
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    // Glow animation for current line
    val glowAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 0.6f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for current line
        if (isCurrent) {
            Text(
                text = line.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                modifier = Modifier
                    .blur(20.dp)
                    .graphicsLayer {
                        scaleX = scale * 1.1f
                        scaleY = scale * 1.1f
                        this.alpha = glowAlpha
                    }
            )
        }
        
        // Main text
        Text(
            text = line.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isCurrent) FontWeight.ExtraBold else if (isPast) FontWeight.Normal else FontWeight.Medium,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White
            },
            textAlign = TextAlign.Center,
            fontSize = if (isCurrent) 24.sp else 19.sp,
            lineHeight = if (isCurrent) 32.sp else 26.sp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        )
    }
}
