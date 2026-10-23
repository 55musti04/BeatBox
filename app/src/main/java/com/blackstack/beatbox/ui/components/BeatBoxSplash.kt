package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AppTheme
import com.blackstack.beatbox.R
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashCyan = Color(0xFF22D3EE)
private val SplashViolet = Color(0xFFA855F7)
private val SplashPink = Color(0xFFF472B6)

/**
 * Aurora Glass açılış ekranı.
 *
 * 1.2 saniyelik tek bir ritim: logo hafifçe "vurur", arkasındaki aurora
 * bloom açılır ve alttaki ince çizgi dolar. Reduced-motion açıksa tüm
 * dekoratif animasyonlar atlanır; süre korunur.
 */
@Composable
fun SplashScreen(
    theme: AppTheme,
    onFinish: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.84f) }
    val contentAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val bloom = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            launch {
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(420, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1.07f,
                    animationSpec = tween(230, easing = BeatBoxMotion.ExpressiveEasing)
                )
                logoScale.animateTo(0.97f, tween(130))
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                bloom.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(900, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1_050, easing = BeatBoxMotion.ExpressiveEasing)
                )
            }
        }
        delay(1_200)
        onFinish()
    }

    AuroraBackground(theme = theme, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Yumuşak aurora bloom'u; logo arkasında genişleyerek açılır.
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .graphicsLayer {
                        val value = bloom.value
                        scaleX = 0.55f + value * 0.65f
                        scaleY = 0.55f + value * 0.65f
                        alpha = value
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SplashCyan.copy(alpha = 0.34f),
                                SplashViolet.copy(alpha = 0.22f),
                                SplashPink.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = (1f - contentAlpha.value) * 20.dp.toPx()
                }
            ) {
                Surface(
                    modifier = Modifier
                        .size(138.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .shadow(28.dp, RoundedCornerShape(40.dp)),
                    shape = RoundedCornerShape(40.dp),
                    color = Color.Black.copy(alpha = 0.40f),
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(SplashCyan, SplashViolet, SplashPink)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.beatbox_logo),
                            contentDescription = "BeatBox",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(30.dp))

                Text(
                    text = "BeatBox",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(Color.White, Color(0xFFC9F7FF), Color.White)
                        ),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "HİSSET • DİNLE • YAŞA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplashCyan.copy(alpha = 0.82f),
                    letterSpacing = 2.4.sp
                )

                Spacer(Modifier.height(46.dp))

                Box(
                    modifier = Modifier
                        .width(168.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = progress.value
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SplashCyan, SplashViolet, SplashPink)
                                )
                            )
                    )
                }
            }
        }
    }
}
