package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

/**
 * İlk kütüphane taraması sürerken gösterilen iskelet.
 *
 * Gerçek içerik yüklenince yerini listeye bırakır. Reduced-motion açıkken
 * parıltı durur; şekiller sabit kalır.
 */
@Composable
fun BeatBoxSkeleton(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val pulse: State<Float> = if (reducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton_pulse")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(950),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_pulse_value"
        )
    }

    val weak = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
    val strong = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.19f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.55f + pulse.value * 0.45f }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(strong)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(weak)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(118.dp)
                .height(13.dp)
                .clip(CircleShape)
                .background(weak)
        )

        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(strong)
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(weak)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.38f)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(weak)
                    )
                }
            }
        }
    }
}
