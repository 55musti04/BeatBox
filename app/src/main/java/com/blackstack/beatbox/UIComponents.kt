package com.blackstack.beatbox

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════
// UI COMPONENTS
// ═══════════════════════════════════════════

// Unified cache and concurrency control for album art loading (prevents ANR and memory leaks)
internal object AlbumArtCache {
    // Unified cache shared with MediaPlaybackService to prevent duplicate bitmap storage and OOM
    val bitmapCache = com.blackstack.beatbox.bitmapCache
}

// Gradient Theme Support
@Composable
fun GradientBackground(
    theme: AppTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Cache gradient brush to avoid recreating on every recomposition
    val gradientBrush = remember(theme) {
        if (theme.isGradient && theme.gradientColors.isNotEmpty()) {
            Brush.verticalGradient(
                colors = theme.gradientColors,
                startY = 0f,
                endY = 3000f // Use fixed value instead of POSITIVE_INFINITY for better performance
            )
        } else null
    }
    
    if (gradientBrush != null) {
        Box(
            modifier = modifier
                .graphicsLayer { } // Hardware acceleration
                .background(gradientBrush)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

@Composable
fun GradientCard(
    theme: AppTheme,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    // Cache gradient brush
    val gradientBrush = remember(theme) {
        if (theme.isGradient && theme.gradientColors.isNotEmpty()) {
            Brush.linearGradient(
                colors = theme.gradientColors.map { it.copy(alpha = 0.1f) },
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f) // Use fixed value for better performance
            )
        } else null
    }
    
    if (gradientBrush != null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { } // Hardware acceleration
                    .background(gradientBrush)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
        ) {
            content()
        }
    }
}

// Glassmorphic Card Container
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.03f)
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Box(content = content)
    }
}

// Explicit Content Badge
@Composable
fun ExplicitBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "E",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// Shadow effects for buttons
fun Modifier.buttonShadow() = this.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(12.dp),
    ambientColor = Color.Black.copy(alpha = 0.1f),
    spotColor = Color.Black.copy(alpha = 0.1f)
)

fun Modifier.iconButtonShadow() = this.shadow(
    elevation = 2.dp,
    shape = CircleShape,
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.08f)
)

fun Modifier.cardShadow() = this.shadow(
    elevation = 6.dp,
    shape = RoundedCornerShape(28.dp),
    ambientColor = Color.Black.copy(alpha = 0.12f),
    spotColor = Color.Black.copy(alpha = 0.12f)
)

fun Modifier.fabShadow() = this.shadow(
    elevation = 8.dp,
    shape = RoundedCornerShape(28.dp),
    ambientColor = Color.Black.copy(alpha = 0.15f),
    spotColor = Color.Black.copy(alpha = 0.15f)
)

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_angle"
    )

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(1f, tween(500))
        }
        launch {
            progress.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
        }
        delay(1900)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06070D),
                        Color(0xFF101428),
                        Color(0xFF06070D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Rotated Light Aura
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {
                    scaleX = pulseGlow
                    scaleY = pulseGlow
                    rotationZ = rotateAngle
                }
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.35f),
                            Color(0xFF00E5FF).copy(alpha = 0.25f),
                            Color(0xFFFF2E93).copy(alpha = 0.35f),
                            Color(0xFF9C27B0).copy(alpha = 0.35f)
                        )
                    ),
                    CircleShape
                )
                .blur(90.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
        ) {
            // Glassmorphic Glowing Logo Card
            Surface(
                modifier = Modifier
                    .size(150.dp)
                    .shadow(24.dp, RoundedCornerShape(36.dp)),
                shape = RoundedCornerShape(36.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF9C27B0),
                            Color(0xFFFF2E93)
                        )
                    )
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beatbox_logo),
                        contentDescription = "BeatBox Logo",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name with Gradient Glow
            Text(
                text = "BeatBox",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "HİSSET • DİNLE • YAŞA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Precision Loading Line
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF9C27B0),
                                    Color(0xFFFF2E93)
                                )
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(strings: Strings, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = strings.get("onboarding_title_1"),
            description = strings.get("onboarding_desc_1"),
            icon = Icons.Rounded.GraphicEq,
            accentColor = Color(0xFF9C27B0)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_2"),
            description = strings.get("onboarding_desc_2"),
            icon = Icons.Rounded.Subtitles,
            accentColor = Color(0xFF00E5FF)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_3"),
            description = strings.get("onboarding_desc_3"),
            icon = Icons.Rounded.Palette,
            accentColor = Color(0xFFFF2E93)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_4"),
            description = strings.get("onboarding_desc_4"),
            icon = Icons.Rounded.Security,
            accentColor = Color(0xFF00E5FF)
        ),
        OnboardingPage(
            title = strings.get("onboarding_title_5"),
            description = strings.get("onboarding_desc_5"),
            icon = Icons.Rounded.MusicNote,
            accentColor = Color(0xFFFFD60A)
        )
    )

    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    // Ambient pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val currentPage = pages[pagerState.currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
    ) {
        // Animated Ambient Background Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            currentPage.accentColor.copy(alpha = 0.35f),
                            Color(0xFF121420),
                            Color(0xFF0B0C10)
                        )
                    )
                )
        )

        // Center Pulsing Orb
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .scale(glowScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            currentPage.accentColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glassmorphic Icon Card
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.5.dp, page.accentColor.copy(alpha = 0.6f)),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .size(160.dp)
                        .cardShadow()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            page.accentColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = page.accentColor,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                Text(
                    text = page.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Bottom Controls Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Smooth Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (active) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (active)
                                    currentPage.accentColor
                                else
                                    Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = strings.get("skip"),
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(60.dp))
                }

                // Next / Get Started Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentPage.accentColor
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .animateContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pages.size - 1) "Müziğe Başla" else "Devam Et",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun UserAgreementPage(strings: Strings, color: Color, onShowFull: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Description,
                null,
                Modifier.size(60.dp),
                tint = color
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            strings.get("user_agreement"),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Agreement summary card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(0.1f)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    strings.get("user_agreement_short"),
                    fontSize = 14.sp,
                    color = Color.White.copy(0.9f),
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Read full agreement button
        TextButton(
            onClick = onShowFull,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Article,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    strings.get("read_full_agreement"),
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UserAgreementDialog(strings: Strings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    strings.get("user_agreement"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        strings.get("user_agreement_full"),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.get("ok"))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}


@Composable 
fun SettingsHeader(t: String) { 
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            t, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable 
fun SettingsToggleItem(icon: ImageVector, t: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onChange(!checked) }
            .cardShadow(), 
        shape = RoundedCornerShape(20.dp), 
        color = if (checked) 
            MaterialTheme.colorScheme.primaryContainer.copy(0.3f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        border = if (checked) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(0.3f)) 
        else null
    ) {
        Row(
            Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (checked) 
                            MaterialTheme.colorScheme.primary.copy(0.15f) 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = if (checked) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                t, 
                Modifier.weight(1f), 
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                color = if (checked) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked, 
                onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(0.5f)
                )
            )
        }
    }
}

@Composable 
fun SettingsClickItem(icon: ImageVector, t: String, sub: String? = null, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .cardShadow(), 
        shape = RoundedCornerShape(20.dp), 
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
    ) {
        Row(
            Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { 
                Text(t, fontWeight = FontWeight.Medium)
                if (sub != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sub, 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.primary.copy(0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight, 
                null, 
                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable fun AnimatedEqualizer(isPlaying: Boolean, color: Color) {
    val inf = rememberInfiniteTransition(label = "")
    val a = (0..2).map { index ->
        inf.animateFloat(0.2f, 1f, infiniteRepeatable(tween(400 + index * 150, 50, LinearEasing), RepeatMode.Reverse), label = "eq_$index")
    }
    Canvas(Modifier.size(20.dp)) {
        val w = size.width / 5
        a.forEachIndexed { i, v ->
            val targetValue = if (isPlaying) v.value else 0.2f
            val h = size.height * targetValue
            drawRoundRect(color, Offset(i * w * 2, size.height - h), Size(w, h), CornerRadius(2.dp.toPx()))
        }
    }
}

@Composable fun EmptyState(icon: ImageVector, t: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(64.dp), Color.Gray); Spacer(Modifier.height(16.dp)); Text(t, color = Color.Gray) } }
}

@Composable fun PermissionScreen(strings: Strings, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp)); Text("İzin Gerekli"); Spacer(Modifier.height(16.dp))
            Button(onClick, modifier = Modifier.buttonShadow()) { Text("İzin Ver") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun SongItem(
    a: AudioFile,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    isFavorite: Boolean,
    onFavClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onShowArtistSongs: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val strings = Strings(AppLanguage.TR)
    
    // Modern Glassmorphic card design
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isCurrent) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(0.3f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(0.02f)
                        )
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .shadow(
                elevation = if (isCurrent) 10.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.08f),
                spotColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.08f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isCurrent) 
            MaterialTheme.colorScheme.primaryContainer.copy(0.9f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(0.88f)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with glow effect when playing
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isCurrent) 6.dp else 2.dp)
                ) {
                    AlbumArtImage(a, Modifier.fillMaxSize(), isHQ = true)
                }
                
                // Playing indicator overlay / Equalizer animation
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizer(isPlaying = true, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Song info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        a.title,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (a.isExplicit) {
                        Spacer(Modifier.width(6.dp))
                        ExplicitBadge()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    a.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDuration(a.duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
                    fontWeight = FontWeight.Normal
                )
            }
            
            // Favorite button with animation
            IconButton(
                onClick = onFavClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isFavorite) 
                            Color(0xFFFF4081).copy(0.15f) 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get("same_artist_songs"),
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onShowArtistSongs(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Person,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get(if (isFavorite) "remove_from_favorites" else "add_to_favorites"),
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onFavClick(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite,
                                null,
                                tint = if (!isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                    )
                    if (a.isOnline) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "İndir",
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = { onDownload(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Download,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get("delete"),
                                color = Color.Red,
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

// Helper function to format duration
private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

@Composable fun PlaylistItem(p: Playlist, onClick: () -> Unit, onDel: () -> Unit, onEdit: () -> Unit, strings: Strings? = null) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }.cardShadow(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (p.isArtistPlaylist && !p.artistName.isNullOrBlank()) {
                    ArtistImage(
                        artistName = p.artistName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                val songLabel = strings?.get("songs") ?: "Songs"
                Text("${p.songIds.size} $songLabel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            if (!p.isArtistPlaylist) {
                IconButton(onClick = onEdit, modifier = Modifier.iconButtonShadow()) { Icon(Icons.Default.Edit, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface) }
            }
            IconButton(onClick = onDel, modifier = Modifier.iconButtonShadow()) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = Color.Red) }
        }
    }
}

@Composable fun ArtistCard(
    artistName: String,
    songCount: Int,
    onClick: () -> Unit,
    strings: Strings? = null
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
            .cardShadow(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                ArtistImage(
                    artistName = artistName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = artistName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            val songLabel = strings?.get("songs") ?: "Songs"
            Text(
                text = "$songCount $songLabel",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable fun MiniPlayerBar(
    a: AudioFile?, 
    isPlaying: Boolean, 
    onClick: () -> Unit, 
    onPlayPause: () -> Unit, 
    onNext: () -> Unit, 
    onPrev: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(78.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (a != null) {
                            if (offsetX > 60f) {
                                onPrev()
                            } else if (offsetX < -60f) {
                                onNext()
                            }
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
            .clickable { onClick() }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(0.4f),
                        MaterialTheme.colorScheme.onSurface.copy(0.08f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val song = a
            if (song != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AlbumArtImage(song, Modifier.fillMaxSize(), isHQ = true)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                song.title,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (song.isExplicit) {
                                Spacer(Modifier.width(6.dp))
                                ExplicitBadge()
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            song.artist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "BeatBox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Müzik çalınmıyor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onPrev,
                enabled = a != null,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(if (a != null) 0.6f else 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    null,
                    tint = if (a != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (a != null) {
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(0.82f)
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                        },
                        CircleShape
                    )
                    .clickable(enabled = a != null, onClick = onPlayPause)
                    .shadow(if (a != null) 6.dp else 0.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = if (a != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onNext,
                enabled = a != null,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(if (a != null) 0.6f else 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    null,
                    tint = if (a != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable fun AlbumArtImage(a: AudioFile, modifier: Modifier = Modifier, isHQ: Boolean = false) {
    val ctx = LocalContext.current
    var bmp by remember(a.id) { mutableStateOf<Bitmap?>(AlbumArtCache.bitmapCache.get(a.id)) }
    var isLoading by remember(a.id) { mutableStateOf(false) }
    
    LaunchedEffect(a.id) {
        if (bmp == null) {
            // Check memory cache first
            AlbumArtCache.bitmapCache.get(a.id)?.let {
                bmp = it
                return@LaunchedEffect
            }
            
            isLoading = true
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap? = null

                // 1. Fast system MediaStore album art URI FIRST (2ms execution!)
                if (a.albumId > 0) {
                    try {
                        val uri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), 
                            a.albumId
                        )
                        bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ctx.contentResolver.loadThumbnail(
                                uri, 
                                android.util.Size(
                                    if (isHQ) 256 else 96,
                                    if (isHQ) 256 else 96
                                ), 
                                null
                            )
                        } else {
                            @Suppress("DEPRECATION") 
                            MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                        }
                    } catch (_: Exception) {}
                }

                // 2. Secondary Fallback: Embedded album art via file descriptor (only if MediaStore URI is missing)
                if (bitmap == null) {
                    try {
                        ctx.contentResolver.openFileDescriptor(a.uri, "r")?.use { pfd ->
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(pfd.fileDescriptor)
                            val art = retriever.embeddedPicture
                            if (art != null) {
                                val options = BitmapFactory.Options().apply {
                                    inSampleSize = if (isHQ) 2 else 4
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                            }
                            retriever.release()
                        }
                    } catch (_: Exception) {}
                }

                if (bitmap != null) {
                    AlbumArtCache.bitmapCache.put(a.id, bitmap)
                }
                
                withContext(Dispatchers.Main) {
                    bmp = bitmap
                    isLoading = false
                }
            }
        }
    }
    
    bmp?.let { validBitmap ->
        Box(modifier) {
            Image(
                bitmap = validBitmap.asImageBitmap(), 
                contentDescription = null, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize()
            )
        }
    } ?: run {
        // Use cached gradient background when no album art is available
        val gradient = remember(a.id) { gradientForSong(a, ctx) }
        Box(
            modifier = modifier.background(gradient), 
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

// Generates a pleasant gradient based on song title/artist to use when album art is missing
private fun colorFromString(seed: String): Color {
    val h = seed.hashCode()
    val r = ((h shr 16) and 0xFF)
    val g = ((h shr 8) and 0xFF)
    val b = (h and 0xFF)
    val a = 0xFF
    return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
}

// LRU Cache for gradient calculations to improve performance and prevent memory issues
private val gradientCache = object : LinkedHashMap<String, Brush>(
    100, // Initial capacity
    0.75f, // Load factor
    true // Access order (LRU)
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, Brush>): Boolean {
        return size > 100 // Remove oldest entry when size exceeds 100
    }
}

private fun gradientForSong(a: AudioFile, ctx: Context): Brush {
    val cacheKey = "${a.id}_${a.title}_${a.artist}"
    
    // Return cached gradient if available
    gradientCache[cacheKey]?.let { return it }
    
    // Simple gradient based on song info (no user preference)
    val c1 = colorFromString(a.title + a.artist)
    val c2 = colorFromString(a.title + a.artist + "grad2")
    val gradient = Brush.linearGradient(listOf(c1, c2))
    
    // Cache the gradient (LRU will automatically remove oldest entry if needed)
    gradientCache[cacheKey] = gradient
    
    return gradient
}

@Composable fun DynamicThemePlayer(a: AudioFile, currentTheme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = getThemeColors(currentTheme)) { content() }
}

fun formatTime(ms: Long): String = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) % 60)

fun loadUserProfile(prefs: SharedPreferences): UserProfile {
    val isLoggedIn = prefs.getBoolean("user_is_logged_in", false)
    if (!isLoggedIn) {
        return UserProfile(isLoggedIn = false)
    }
    return UserProfile(
        id = prefs.getString("user_id", "user_1") ?: "user_1",
        displayName = prefs.getString("user_display_name", "BeatBox Dinleyicisi") ?: "BeatBox Dinleyicisi",
        email = prefs.getString("user_email", "") ?: "",
        phoneNumber = prefs.getString("user_phone", "") ?: "",
        bio = prefs.getString("user_bio", "Müzik ruhun gıdasıdır ✨") ?: "Müzik ruhun gıdasıdır ✨",
        avatarId = prefs.getInt("user_avatar_id", 0),
        joinedDate = prefs.getLong("user_joined_date", System.currentTimeMillis()),
        isLoggedIn = true,
        authProvider = try {
            AuthProvider.valueOf(prefs.getString("user_auth_provider", "EMAIL") ?: "EMAIL")
        } catch (e: Exception) { AuthProvider.EMAIL },
        isPremium = prefs.getBoolean("user_is_premium", true)
    )
}

fun saveUserProfile(prefs: SharedPreferences, user: UserProfile) {
    prefs.edit()
        .putBoolean("user_is_logged_in", user.isLoggedIn)
        .putString("user_id", user.id)
        .putString("user_display_name", user.displayName)
        .putString("user_email", user.email)
        .putString("user_phone", user.phoneNumber)
        .putString("user_bio", user.bio)
        .putInt("user_avatar_id", user.avatarId)
        .putLong("user_joined_date", user.joinedDate)
        .putString("user_auth_provider", user.authProvider.name)
        .putBoolean("user_is_premium", user.isPremium)
        .apply()
}

fun updateSongStats(prefs: SharedPreferences, a: AudioFile) {
    val recentHistory = prefs.getString("stats_history", "") ?: ""
    val historyList = recentHistory.split(",").filter { it.isNotBlank() }.toMutableList()
    historyList.remove(a.id.toString())
    historyList.add(0, a.id.toString())
    val newHistory = historyList.take(50).joinToString(",")

    prefs.edit()
        .putInt("stats_count_${a.id}", prefs.getInt("stats_count_${a.id}", 0) + 1)
        .putLong("stats_last_${a.id}", System.currentTimeMillis())
        .putString("stats_history", newHistory)
        .apply()
}

fun loadAudioFilesAndFolders(ctx: Context, minSec: Int, selected: Set<String>): Pair<List<AudioFile>, List<String>> {
    val list = mutableListOf<AudioFile>()
    val folders = mutableSetOf<String>()
    
    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        
        // Add duration filter to SQL query to reduce processing
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf((minSec * 1000).toString())
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        
        ctx.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val count = cursor.count
            android.util.Log.d("AudioLoader", "Loading $count audio files")
            
            while (cursor.moveToNext()) {
                try {
                    val path = cursor.getString(5) ?: continue
                    val folderName = File(path).parentFile?.name ?: "Unknown"
                    folders.add(folderName)
                    
                    // Skip if folder filtering is active and this folder is not selected
                    if (selected.isNotEmpty() && !selected.contains(folderName)) {
                        continue
                    }
                    
                    val id = cursor.getLong(0)
                    val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    val audioFile = AudioFile(
                        id = id,
                        title = cursor.getString(1) ?: "Unknown",
                        artist = cursor.getString(2) ?: "Unknown",
                        duration = cursor.getLong(3),
                        uri = audioUri,
                        albumId = cursor.getLong(4),
                        folderName = folderName,
                        dateAdded = cursor.getLong(6)
                    )
                    
                    list.add(audioFile)
                } catch (e: Exception) {
                    // Log individual file errors but continue processing
                    android.util.Log.w("AudioLoader", "Error processing audio file", e)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AudioLoader", "Error loading audio files", e)
        // Return empty lists on critical error
        return Pair(emptyList(), emptyList())
    }
    
    android.util.Log.d("AudioLoader", "Loaded ${list.size} audio files from ${folders.size} folders")
    return Pair(list, folders.sorted())
}

fun getThemeColors(t: AppTheme): ColorScheme = when (t) {
    AppTheme.DYNAMIC -> darkColorScheme(
        primary = Color(0xFF00E5FF), onPrimary = Color.Black,
        primaryContainer = Color(0xFF003840), onPrimaryContainer = Color(0xFF80F2FF),
        secondary = Color(0xFF9C27B0), onSecondary = Color.White,
        background = Color(0xFF0C0D15), onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF141622), onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1E2235), onSurfaceVariant = Color(0xFFD0D0D0)
    )
    AppTheme.AMOLED -> darkColorScheme(
        primary = Color(0xFF00E5FF), onPrimary = Color.Black,
        primaryContainer = Color(0xFF003840), onPrimaryContainer = Color(0xFF80F2FF),
        secondary = Color(0xFFFF2E93), onSecondary = Color.White,
        background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF09090D), onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF14141A), onSurfaceVariant = Color(0xFFD0D0D0)
    )
    AppTheme.NEON_CYBER -> darkColorScheme(
        primary = Color(0xFF00F5D4), onPrimary = Color.Black,
        primaryContainer = Color(0xFF00473E), onPrimaryContainer = Color(0xFF99FCEF),
        secondary = Color(0xFF7B2CBF), onSecondary = Color.White,
        background = Color(0xFF180030), onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF27004D), onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF3B0073), onSurfaceVariant = Color(0xFFE0AAFF)
    )
    AppTheme.ROSE_GOLD -> darkColorScheme(
        primary = Color(0xFFF2CC8F), onPrimary = Color.Black,
        primaryContainer = Color(0xFF473720), onPrimaryContainer = Color(0xFFFAF0CA),
        secondary = Color(0xFFFF758F), onSecondary = Color.Black,
        background = Color(0xFF21171C), onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF33242B), onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF47333C), onSurfaceVariant = Color(0xFFF5D0E0)
    )
    else -> darkColorScheme( // DARK (Midnight)
        primary = Color(0xFF9C27B0), onPrimary = Color.White,
        primaryContainer = Color(0xFF3B004D), onPrimaryContainer = Color(0xFFF2C8FF),
        secondary = Color(0xFF00E5FF), onSecondary = Color.Black,
        background = Color(0xFF0C0A1A), onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF171333), onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF241D4D), onSurfaceVariant = Color(0xFFE2D0FF)
    )
}

fun getAudioOutputDeviceName(context: Context): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        ?: return "Telefon"

    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    for (device in devices) {
        when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> return "Bluetooth"

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> return "Kulaklık"

            else -> {}
        }
    }

    @Suppress("DEPRECATION")
    if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
        return "Bluetooth"
    }

    @Suppress("DEPRECATION")
    if (audioManager.isWiredHeadsetOn) {
        return "Kulaklık"
    }

    return "Telefon"
}

@Composable
fun AudioOutputIndicator(
    context: Context = LocalContext.current,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentDevice by remember { mutableStateOf("Telefon") }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            currentDevice = getAudioOutputDeviceName(context)
            isVisible = true
            delay(3000)
            isVisible = false
        } else {
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        val icon = when (currentDevice) {
            "Bluetooth" -> Icons.Rounded.Bluetooth
            "Kulaklık" -> Icons.Rounded.Headphones
            else -> Icons.Rounded.PhoneAndroid
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = currentDevice,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentDevice,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
