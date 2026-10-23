package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.graphicsLayer
import com.blackstack.beatbox.AppTheme
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlin.random.Random

/**
 * Aurora Glass tuvali.
 *
 * Seçili temanın dikey gradyanı üzerine albümden türeyen birincil/ikincil
 * renklerden iki yumuşak ışık lekesi ve çok hafif bir grain ekler. Işık
 * lekeleri yavaşça sürüklenir; reduced-motion açıkken statik kalır.
 *
 * Eski `GradientBackground` ile aynı kullanım sözleşmesine sahiptir:
 * [modifier], [content] alanını sarar.
 */
@Composable
fun AuroraBackground(
    theme: AppTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val baseBrush = remember(theme) {
        if (theme.gradientColors.isNotEmpty()) {
            Brush.verticalGradient(
                colors = theme.gradientColors,
                startY = 0f,
                endY = 3000f
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color(0xFF07080F), Color(0xFF0D1020), Color(0xFF07080F)),
                startY = 0f,
                endY = 3000f
            )
        }
    }

    val drift: State<Float> = if (reducedMotion) {
        remember { mutableStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "aurora_drift")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(26_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_drift_value"
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(baseBrush)
    ) {
        val glowSize = maxWidth * 1.35f
        val travel = maxWidth * 0.10f

        // Üst-sol: birincil renk.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = -maxWidth * 0.55f, y = -maxWidth * 0.48f)
                .size(glowSize)
                .graphicsLayer {
                    translationX = travel.toPx() * drift.value
                    translationY = travel.toPx() * drift.value * 0.55f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.30f),
                            primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Alt-sağ: ikincil renk.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = maxWidth * 0.42f, y = maxWidth * 0.40f)
                .size(glowSize)
                .graphicsLayer {
                    translationX = -travel.toPx() * drift.value * 0.8f
                    translationY = -travel.toPx() * drift.value * 0.6f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.24f),
                            secondary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Orta-alt: üçüncül renk, kompozisyonu dengeler.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -maxWidth * 0.35f, y = maxWidth * 0.72f)
                .size(maxWidth * 1.1f)
                .graphicsLayer {
                    translationX = travel.toPx() * drift.value * 0.45f
                    translationY = -travel.toPx() * drift.value * 0.30f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tertiary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Grain: deterministik noktalar, animasyona katılmaz.
        val grain = remember {
            val random = Random(0xBE47)
            List(1_100) { Offset(random.nextFloat(), random.nextFloat()) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val points = grain.map { Offset(it.x * size.width, it.y * size.height) }
                    onDrawBehind {
                        drawPoints(
                            points = points,
                            pointMode = PointMode.Points,
                            color = Color.White.copy(alpha = 0.028f),
                            strokeWidth = 1f
                        )
                    }
                }
        )

        // Kenarlarda hafif karartma; ortadaki içerik daha okunaklı olur.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        0.18f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f)
                    )
                )
        )

        content()
    }
}
