package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AlbumArtImage
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.ExplicitBadge
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Alt navigasyonun üstünde yüzen cam mini oynatıcı.
 *
 * - Albüm kapağı, başlık ve sanatçı
 * - Alt kenarda gerçek çalma ilerlemesini gösteren ince çizgi
 * - Oynat/duraklat ikonları arasında yumuşak morph
 * - Yatay kaydırma ile önceki/sonraki, yukarı kaydırma ile tam ekran
 */
@Composable
fun MiniPlayerBar(
    audio: AudioFile?,
    isPlaying: Boolean,
    progress: () -> Float,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSwipeUp: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var rawProgress by remember(audio?.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(audio?.id, isPlaying) {
        if (audio == null) {
            rawProgress = 0f
            return@LaunchedEffect
        }
        while (true) {
            rawProgress = progress().coerceIn(0f, 1f)
            delay(if (isPlaying) 500 else 1_500)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(380),
        label = "mini_player_progress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val enabled = audio != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .pointerInput(audio?.id) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = {
                        val x = dragOffset.x
                        val y = dragOffset.y
                        when {
                            abs(y) > abs(x) && y < -56f -> if (enabled) onSwipeUp()
                            x > 64f && enabled -> onPrev()
                            x < -64f && enabled -> onNext()
                        }
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                    }
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    primary.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.10f),
                    secondary.copy(alpha = 0.22f)
                )
            )
        )
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Yukarı kaydırma ipucu.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (audio != null) {
                    AlbumArtImage(
                        a = audio,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(15.dp)),
                        isHQ = true
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(primary.copy(alpha = 0.14f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(11.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = audio?.title ?: "BeatBox",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (audio?.isExplicit == true) {
                            Spacer(Modifier.width(6.dp))
                            ExplicitBadge()
                        }
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = audio?.artist ?: "Müzik çalınmıyor",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onPrev,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Önceki",
                        tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                        modifier = Modifier.size(23.dp)
                    )
                }

                val playing = isPlaying && enabled
                val playLabel = if (playing) "Duraklat" else "Oynat"
                val iconScale by animateFloatAsState(
                    targetValue = if (playing) 1f else 0.88f,
                    animationSpec = BeatBoxMotion.Snappy,
                    label = "mini_player_play_scale"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (enabled) {
                                Brush.linearGradient(listOf(primary, secondary.copy(alpha = 0.92f)))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            },
                            shape = CircleShape
                        )
                        .semantics(mergeDescendants = true) { contentDescription = playLabel }
                        .clickable(enabled = enabled, onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                        modifier = Modifier
                            .size(25.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = enabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Sonraki",
                        tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            // Çalma ilerlemesi: 2 dp'lik hairline.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedProgress
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(Brush.horizontalGradient(listOf(primary, secondary)))
                )
            }
        }
    }
}
