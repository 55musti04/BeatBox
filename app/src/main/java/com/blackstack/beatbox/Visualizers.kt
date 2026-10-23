package com.blackstack.beatbox

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun EnhancedRealTimeVisualizer(
    isPlaying: Boolean,
    level: Float,
    waveform: FloatArray,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE) }
    val tempoVizEnabled = remember { prefs.getBoolean("tempo_visualization", true) }
    val performanceMode = remember { prefs.getBoolean("performance_mode", false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "tempo_fallback")
    val fallbackPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tempo_phase"
    )

    if (!tempoVizEnabled) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            repeat(12) { index ->
                val staticHeight = when {
                    index < 3 -> 24.dp
                    index < 6 -> 20.dp
                    index < 9 -> 16.dp
                    else -> 12.dp
                }
                Box(
                    Modifier
                        .padding(horizontal = 1.5.dp)
                        .width(4.dp)
                        .height(if (isPlaying) staticHeight else 8.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        return
    }
    
    val barCount = if (performanceMode) 10 else 14
    val hasRealData = isPlaying && waveform.any { it > 0.01f }

    Row(
        modifier = modifier.graphicsLayer { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(barCount) { index ->
            val targetAmplitude = if (isPlaying) {
                if (hasRealData && index < waveform.size) {
                    (waveform[index] * 2.8f).coerceIn(0f, 1f)
                } else {
                    val sineVal = sin(fallbackPhase + index * 0.45f)
                    val baseAmp = (sineVal + 1f) / 2f
                    (0.25f + baseAmp * 0.7f).coerceIn(0.15f, 1f)
                }
            } else {
                0.05f
            }
            
            val amplitude by animateFloatAsState(
                targetValue = targetAmplitude,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bar_amplitude_$index"
            )
            
            val centerDistance = abs(index - barCount / 2f) / (barCount / 2f)
            val baseHeight = 6f
            val maxHeight = (38f - centerDistance * 14f).coerceAtLeast(20f)
            
            val height = if (isPlaying) {
                (baseHeight + amplitude * (maxHeight - baseHeight)).dp
            } else {
                8.dp
            }
            
            val alpha = if (isPlaying) {
                (0.65f + amplitude * 0.35f).coerceIn(0.65f, 1f)
            } else {
                0.35f
            }
            
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(4.5.dp)
                    .height(height)
                    .graphicsLayer { }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.5f)
                            )
                        ),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
fun MusicVisualizer(
    isPlaying: Boolean,
    level: Float,
    waveform: FloatArray,
    modifier: Modifier = Modifier
) {
    // Real-time audio visualizer with actual waveform data
    EnhancedRealTimeVisualizer(isPlaying, level, waveform, modifier)
}
