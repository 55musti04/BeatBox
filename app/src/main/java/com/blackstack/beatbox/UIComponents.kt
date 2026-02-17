package com.blackstack.beatbox

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════
// UI COMPONENTS
// ═══════════════════════════════════════════

// Cache and concurrency control for album art loading (prevents ANR and memory leaks)
internal object AlbumArtCache {
    // Semaphore to limit concurrent album art loading (reduced to 2 for better ANR prevention)
    val loadingSemaphore = Semaphore(2)
    
    // LRU Cache for bitmaps
    val bitmapCache = object : android.util.LruCache<Long, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt() // Use 1/8 of available memory
    ) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.byteCount
        }
    }
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
    var startAnimation by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    // Animated music notes
    val infiniteTransition = rememberInfiniteTransition(label = "splash_music_notes")
    val note1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "note1"
    )
    val note2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "note2"
    )
    val note3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "note3"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        launch { 
            scale.animateTo(
                1.1f, 
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
            )
        }
        launch { contentAlpha.animateTo(1f, tween(1000)) }
        launch { rotation.animateTo(360f, tween(1500, easing = FastOutSlowInEasing)) }
        launch {
            delay(1000)
            textAlpha.animateTo(1f, tween(800))
        }
        delay(2800)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460),
                        Color(0xFF000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            // Outer glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6200EA).copy(alpha = 0.15f * contentAlpha.value),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = 400f
                ),
                center = Offset(centerX, centerY)
            )
            
            // Middle glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBB86FC).copy(alpha = 0.2f * contentAlpha.value),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = 300f
                ),
                center = Offset(centerX, centerY)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Floating music notes around logo
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Note 1 - Top Left
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = (-60).dp, y = (-60 + note1Y).dp)
                        .graphicsLayer { 
                            alpha = contentAlpha.value * 0.6f
                            rotationZ = rotation.value * 0.5f
                        },
                    tint = Color(0xFFBB86FC)
                )
                
                // Note 2 - Top Right
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .offset(x = 60.dp, y = (-50 + note2Y).dp)
                        .graphicsLayer { 
                            alpha = contentAlpha.value * 0.7f
                            rotationZ = -rotation.value * 0.3f
                        },
                    tint = Color(0xFF03DAC6)
                )
                
                // Note 3 - Bottom
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = 0.dp, y = (70 + note3Y).dp)
                        .graphicsLayer { 
                            alpha = contentAlpha.value * 0.5f
                            rotationZ = rotation.value * 0.4f
                        },
                    tint = Color(0xFFFF4081)
                )
                
                // Logo with enhanced glow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            alpha = contentAlpha.value
                            scaleX = scale.value
                            scaleY = scale.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Multi-layer glow effect
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        
                        // Outer glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6200EA).copy(alpha = 0.4f),
                                    Color(0xFF6200EA).copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2f
                            ),
                            center = center
                        )
                        
                        // Inner glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFBB86FC).copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 3f
                            ),
                            center = center
                        )
                    }
                    
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.beatbox_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .graphicsLayer {
                                rotationZ = rotation.value * 0.1f
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // App name with gradient
            Text(
                "BeatBox",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBB86FC),
                            Color(0xFF6200EA),
                            Color(0xFF03DAC6)
                        )
                    )
                ),
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tagline with shimmer effect
            Text(
                "Crystal Clear Music Experience",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 1.2.sp,
                modifier = Modifier.graphicsLayer { 
                    alpha = textAlpha.value 
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Loading indicator
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .graphicsLayer { alpha = textAlpha.value }
            ) {
                val loadingProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "loading"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(loadingProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFBB86FC),
                                    Color(0xFF6200EA)
                                )
                            )
                        )
                )
            }
        }

        // Bottom branding with fade in
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .graphicsLayer { alpha = contentAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF4081)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Made with Love",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "by BlackStack Labs",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(strings: Strings, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage("welcome", "welcome_desc", Icons.Rounded.MusicNote, Color(0xFFBB86FC)),
        OnboardingPage("shake_to_shuffle", "shake_desc", Icons.Rounded.Shuffle, Color(0xFF03DAC6)),
        OnboardingPage("earphone_intelligence", "earphone_desc", Icons.Rounded.Headset, Color(0xFFFF4081)),
        OnboardingPage("user_agreement", "user_agreement_short", Icons.Rounded.Description, Color(0xFFFFB300)),
        OnboardingPage("ready", "ready_desc", Icons.Rounded.CheckCircle, Color(0xFF4CAF50))
    )
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()
    var showFullAgreement by remember { mutableStateOf(false) }
    
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1238),
                        Color(0xFF1A237E),
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { i ->
            val p = pages[i]
            
            // Special layout for user agreement page
            if (p.titleKey == "user_agreement") {
                UserAgreementPage(
                    strings = strings,
                    color = p.color,
                    onShowFull = { showFullAgreement = true }
                )
            } else {
                // Normal onboarding page
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated icon with glow effect
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        p.color.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            p.icon,
                            null,
                            Modifier.size(80.dp),
                            tint = p.color
                        )
                    }
                    
                    Spacer(Modifier.height(48.dp))
                    
                    Text(
                        strings.get(p.titleKey),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        strings.get(p.descKey),
                        fontSize = 16.sp,
                        color = Color.White.copy(0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
        
        // Bottom section with indicators and button
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        Modifier
                            .size(
                                width = if (pagerState.currentPage == index) 32.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.White.copy(alpha = 0.3f)
                            )
                            .animateContentSize()
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button (except on last page)
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.buttonShadow()
                    ) {
                        Text(
                            strings.get("skip"),
                            color = Color.White.copy(0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                
                // Next/Start button
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
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .then(
                            if (pagerState.currentPage == pages.size - 1)
                                Modifier.fillMaxWidth(0.8f)
                            else
                                Modifier.width(120.dp)
                        )
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (pagerState.currentPage == pages.size - 1)
                                strings.get("start")
                            else
                                strings.get("next"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        if (pagerState.currentPage < pages.size - 1) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Full User Agreement Dialog
    if (showFullAgreement) {
        UserAgreementDialog(
            strings = strings,
            onDismiss = { showFullAgreement = false }
        )
    }
}

data class OnboardingPage(val titleKey: String, val descKey: String, val icon: ImageVector, val color: Color)

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
                    Icons.Rounded.Article,
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
@Composable fun SongItem(a: AudioFile, isCurrent: Boolean, isPlaying: Boolean = false, isFavorite: Boolean, onFavClick: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val strings = Strings(AppLanguage.TR) // TODO: Get from context
    
    // Modern card design with gradient and shadow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .shadow(
                elevation = if (isCurrent) 12.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.1f),
                spotColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) 
            MaterialTheme.colorScheme.primaryContainer.copy(0.4f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
        border = if (isCurrent) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(0.5f)) 
        else null
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with glow effect when playing
            Box {
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isCurrent) 8.dp else 4.dp)
                ) {
                    AlbumArtImage(a, Modifier.fillMaxSize(), isHQ = true)
                }
                
                // Playing indicator overlay
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(0.3f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Song info
            Column(Modifier.weight(1f)) {
                Text(
                    a.title,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    a.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDuration(a.duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    fontWeight = FontWeight.Normal
                )
            }
            
            // Animated equalizer when playing
            if (isCurrent && isPlaying) {
                AnimatedEqualizer(isPlaying, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            
            // Favorite button with animation
            IconButton(
                onClick = onFavClick,
                modifier = Modifier
                    .size(44.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                DropdownMenu(showMenu, { showMenu = false }) {
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

@Composable fun PlaylistItem(p: Playlist, onClick: () -> Unit, onDel: () -> Unit, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }.cardShadow(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), RoundedCornerShape(16.dp)), Alignment.Center) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text("${p.songIds.size} Şarkı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) }
            IconButton(onEdit, modifier = Modifier.iconButtonShadow()) { Icon(Icons.Default.Edit, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onDel, modifier = Modifier.iconButtonShadow()) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp), tint = Color.Red) }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable fun MiniPlayerBar(a: AudioFile, isPlaying: Boolean, onClick: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(80.dp)
            .clickable { onClick() }
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = a,
                transitionSpec = {
                    val direction = if (targetState.id > initialState.id) 1 else -1
                    (slideInHorizontally { direction * it / 3 } + fadeIn(tween(250))) togetherWith
                            (slideOutHorizontally { -direction * it / 3 } + fadeOut(tween(250)))
                },
                label = "mini_player_song",
                modifier = Modifier.weight(1f)
            ) { song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AlbumArtImage(song, Modifier.fillMaxSize(), isHQ = true)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            song.artist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(0.8f)
                            )
                        ),
                        CircleShape
                    )
                    .clickable(onClick = onPlayPause)
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable fun AlbumArtImage(a: AudioFile, modifier: Modifier = Modifier, isHQ: Boolean = false) {
    val ctx = LocalContext.current
    var bmp by remember(a.id) { mutableStateOf<Bitmap?>(AlbumArtCache.bitmapCache.get(a.id)) }
    var isLoading by remember(a.id) { mutableStateOf(false) }
    
    // Load album art if not already cached - with aggressive debouncing and concurrency control
    LaunchedEffect(a.id) {
        if (bmp == null && !isLoading) {
            isLoading = true
            // Aggressive debounce to prevent ANR (increased from 200ms to 300ms)
            delay(300)
            
            // Use semaphore to limit concurrent bitmap loading (max 2 at a time)
            AlbumArtCache.loadingSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    try {
                        // Check cache again in case it was loaded by another component
                        AlbumArtCache.bitmapCache.get(a.id)?.let { 
                            withContext(Dispatchers.Main) {
                                bmp = it
                                isLoading = false
                            }
                            return@withContext
                        }
                        
                        // Try embedded album art first (faster and more reliable)
                        var bitmap: Bitmap? = null
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(ctx, a.uri)
                            val art = retriever.embeddedPicture
                            if (art != null) {
                                // Decode with smaller size to prevent OOM
                                val options = BitmapFactory.Options().apply {
                                    // More aggressive downsampling for non-HQ images
                                    inSampleSize = if (isHQ) 2 else 4
                                    inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                                }
                                bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                            }
                            retriever.release()
                        } catch (e: Exception) {
                            android.util.Log.w("AlbumArt", "Failed to load embedded album art: ${e.message}")
                        }
                        
                        // Fallback to external album art if embedded failed
                        if (bitmap == null) {
                            try {
                                val uri = ContentUris.withAppendedId(
                                    Uri.parse("content://media/external/audio/albumart"), 
                                    a.albumId
                                )
                                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    ctx.contentResolver.loadThumbnail(
                                        uri, 
                                        android.util.Size(
                                            if (isHQ) 200 else 80,
                                            if (isHQ) 200 else 80
                                        ), 
                                        null
                                    )
                                } else {
                                    @Suppress("DEPRECATION") 
                                    MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("AlbumArt", "Failed to load external album art: ${e.message}")
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                AlbumArtCache.bitmapCache.put(a.id, bitmap)
                                bmp = bitmap
                            }
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AlbumArt", "Critical error loading album art", e)
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
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

fun updateSongStats(prefs: SharedPreferences, a: AudioFile) {
    prefs.edit().putInt("stats_count_${a.id}", prefs.getInt("stats_count_${a.id}", 0) + 1).putLong("stats_total_time", prefs.getLong("stats_total_time", 0L) + a.duration / 1000).apply()
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
    AppTheme.LIGHT -> lightColorScheme(
        primary = Color(0xFF6200EE), onPrimary = Color.White,
        background = Color(0xFFF5F5F5), onBackground = Color.Black,
        surface = Color(0xFFFFFFFF), onSurface = Color.Black,
        surfaceVariant = Color(0xFFE0E0E0), onSurfaceVariant = Color.Black
    )
    AppTheme.PURPLE -> darkColorScheme(
        primary = Color(0xFF6200EA), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.PINK -> darkColorScheme(
        primary = Color(0xFFE91E63), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.BLUE -> darkColorScheme(
        primary = Color(0xFF2196F3), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.TEAL -> darkColorScheme(
        primary = Color(0xFF009688), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.GREEN -> darkColorScheme(
        primary = Color(0xFF4CAF50), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.ORANGE -> darkColorScheme(
        primary = Color(0xFFFF9800), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.RED -> darkColorScheme(
        primary = Color(0xFFF44336), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    AppTheme.INDIGO -> darkColorScheme(
        primary = Color(0xFF3F51B5), onPrimary = Color.White,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
    else -> darkColorScheme( // DARK (default)
        primary = Color(0xFFBB86FC), onPrimary = Color.Black,
        background = Color(0xFF121212), onBackground = Color.White,
        surface = Color(0xFF1E1E1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color.White
    )
}
