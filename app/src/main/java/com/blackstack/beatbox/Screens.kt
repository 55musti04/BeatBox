package com.blackstack.beatbox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.abs
import kotlin.random.Random

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
    
    if (!tempoVizEnabled) {
        // Modern static bars with gradient effect
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            repeat(15) { index ->
                val staticHeight = when {
                    index < 4 -> 28.dp
                    index < 8 -> 24.dp
                    index < 12 -> 20.dp
                    else -> 16.dp
                }
                Box(
                    Modifier
                        .padding(horizontal = 1.5.dp)
                        .width(4.dp)
                        .height(if (isPlaying) staticHeight else 12.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        return
    }
    
    // Reduce bar count in performance mode
    val barCount = if (performanceMode) 10 else 15
    
    // Only use real waveform data - no fallback animation
    val hasRealData = isPlaying && waveform.any { it > 0.01f }

    Row(
        modifier = modifier.graphicsLayer { }, // Hardware acceleration
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(barCount) { index ->
            // Only use real audio data - no synthetic animation
            val targetAmplitude = if (hasRealData && index < waveform.size) {
                (waveform[index] * 2.5f).coerceIn(0f, 1f)
            } else {
                0f // Silent when no data
            }
            
            // Animate amplitude changes smoothly with spring animation
            val amplitude by animateFloatAsState(
                targetValue = targetAmplitude,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bar_amplitude_$index"
            )
            
            // Dynamic height mapping with center emphasis
            val centerDistance = abs(index - barCount / 2f) / (barCount / 2f)
            val baseHeight = 8f
            val maxHeight = (45f - centerDistance * 20f).coerceAtLeast(25f)
            
            val height = if (isPlaying && hasRealData) {
                // Smooth, balanced animation based on real audio
                val smoothAmp = amplitude.coerceIn(0f, 1f)
                (baseHeight + smoothAmp * (maxHeight - baseHeight)).dp
            } else {
                10.dp // Minimal height when not playing or no data
            }
            
            // Dynamic alpha with smooth pulsing
            val alpha = if (isPlaying && hasRealData) {
                (0.5f + amplitude * 0.5f).coerceIn(0.5f, 1f)
            } else {
                0.3f
            }
            
            // Gradient bars for modern, sweet look
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(5.dp)
                    .height(height)
                    .graphicsLayer { } // Hardware acceleration for each bar
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.9f),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.6f),
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f)
                            )
                        ),
                        RoundedCornerShape(3.dp) // More rounded for softer look
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BeatBoxApp(
    player: MainActivity,
    currentTheme: AppTheme,
    strings: Strings,
    currentLang: AppLanguage,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    onThemeChange: (AppTheme) -> Unit,
    onLangChange: (AppLanguage) -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(prefs.getBoolean("first_launch", true)) }

    if (showSplash) { SplashScreen { showSplash = false }; return }
    if (showOnboarding) { OnboardingScreen(strings) { showOnboarding = false; prefs.edit().putBoolean("first_launch", false).apply() }; return }

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            else
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms -> hasPermission = perms.all { it.value } }

    var allAudioList by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var availableFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf(player.loadSelectedFolders()) }
    var minDurationSeconds by remember { mutableIntStateOf(player.loadMinDurationSetting()) }
    var isLoading by remember { mutableStateOf(true) }
    // Tempo visualization toggle state (tempo-based visualizations)
    var tempoViz by remember { 
        mutableStateOf(
            prefs.getBoolean("tempo_visualization", true) && 
            !prefs.getBoolean("performance_mode", false) // Disable if performance mode is on
        ) 
    }

    fun reloadLibrary() {
        if (!hasPermission) return
        isLoading = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val (l, f) = loadAudioFilesAndFolders(ctx, minDurationSeconds, selectedFolders)
                withContext(Dispatchers.Main) {
                    allAudioList = l
                    availableFolders = f
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(hasPermission, selectedFolders, minDurationSeconds) {
        if (hasPermission) {
            reloadLibrary()
        } else isLoading = false
    }

    var playlists by remember { mutableStateOf(player.loadPlaylists()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Playlists, 2=Library
    var currentPlaylist by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var currentAudio by player.currentAudio
    var isPlaying by player.isPlaying

    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showFullScreenPlayer by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEQMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<AudioFile?>(null) }
    var selectedPlaylistForView by remember { mutableStateOf<Playlist?>(null) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var currentEQPreset by remember { mutableStateOf(player.getCurrentEQPreset()) }
    var animationDirection by remember { mutableIntStateOf(1) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var favoriteIds by remember { mutableStateOf(player.getFavorites()) }

    fun refreshFavorites() { favoriteIds = player.getFavorites() }

    val playNext: () -> Unit = {
        if (currentPlaylist.isNotEmpty()) {
            animationDirection = 1
            val n = if (isShuffle) currentPlaylist.random() else {
                val i = currentPlaylist.indexOfFirst { it.id == currentAudio?.id }
                if (i >= 0 && i < currentPlaylist.size - 1) currentPlaylist[i + 1] else if (repeatMode == 1) currentPlaylist.first() else currentPlaylist.last()
            }
            currentAudio = n; player.playAudio(n)
        }
    }

    val playPrev: () -> Unit = {
        if (currentPlaylist.isNotEmpty()) {
            animationDirection = -1
            val p = if (isShuffle) currentPlaylist.random() else {
                val i = currentPlaylist.indexOfFirst { it.id == currentAudio?.id }
                if (i > 0) currentPlaylist[i - 1] else if (repeatMode == 1) currentPlaylist.last() else currentPlaylist.first()
            }
            currentAudio = p; player.playAudio(p)
        }
    }

    val togglePlayPause: () -> Unit = { currentAudio?.let { if (isPlaying) player.pauseAudio(it) else player.resumeAudio(it) } }

    player.onSongComplete = { animationDirection = 1; if (repeatMode == 2) currentAudio?.let { player.playAudio(it) } else playNext() }
    player.uiActionPlayNext = playNext; player.uiActionPlayPrev = playPrev; player.uiActionPlayPause = togglePlayPause

    BackHandler(showFullScreenPlayer || selectedPlaylistForView != null || showSettingsScreen) {
        if (showFullScreenPlayer) showFullScreenPlayer = false
        else if (selectedPlaylistForView != null) selectedPlaylistForView = null
        else if (showSettingsScreen) showSettingsScreen = false
    }

    Box(Modifier.fillMaxSize()) {
        GradientBackground(currentTheme, Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                Column(Modifier.statusBarsPadding()) {
                    // Floating Oval Bar with Icons
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            shadowElevation = 12.dp,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left side - EQ and Sleep Timer
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        { showEQMenu = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Rounded.Equalizer,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        { showSleepTimerDialog = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Rounded.Nightlight,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                // Center - Visualizer
                                Box(
                                    Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tempoViz) {
                                        MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(30.dp))
                                    }
                                }
                                
                                // Right side - Refresh and Settings
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        { reloadLibrary() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        { showSettingsScreen = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Rounded.Settings,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Sleep Timer Indicator - Below TopBar
                    if (player.isSleepTimerRunning && (player.sleepTimerMinutes > 0 || player.sleepTimerSeconds > 0)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(0.2f),
                                            MaterialTheme.colorScheme.primary.copy(0.1f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                            .shadow(4.dp, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Nightlight,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = String.format("%02d:%02d", player.sleepTimerMinutes, player.sleepTimerSeconds),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "Sleep Timer Active",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { 
                                        player.isSleepTimerRunning = false
                                        player.sleepTimerMinutes = 0
                                        player.sleepTimerSeconds = 0
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error.copy(0.15f))
                                        .shadow(4.dp, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Search bar only for Library tab (tab 2)
                    if (selectedTab == 2) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                searchQuery, { searchQuery = it },
                                Modifier.weight(1f),
                                placeholder = { Text(strings.get("search_hint")) },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                shape = RoundedCornerShape(28.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            )
                            Spacer(Modifier.width(8.dp))
                            Box {
                                IconButton({ showSortMenu = true }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = MaterialTheme.colorScheme.onSurface) }
                                DropdownMenu(showSortMenu, { showSortMenu = false }) {
                                    DropdownMenuItem(text = { Text(strings.get("sort_date")) }, onClick = { sortOption = SortOption.DATE; showSortMenu = false })
                                    DropdownMenuItem(text = { Text(strings.get("sort_name")) }, onClick = { sortOption = SortOption.NAME; showSortMenu = false })
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // System navigation bar için padding
                ) {
                    // Mini player bar above navigation
                    if (currentAudio != null && !showFullScreenPlayer) {
                        MiniPlayerBar(currentAudio!!, isPlaying, { showFullScreenPlayer = true }, togglePlayPause, playNext, playPrev)
                    }
                    // Bottom Navigation Bar - Modern & Sweet Design
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.Black.copy(alpha = 0.9f),
                        shadowElevation = 16.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedTab = 0 }
                                    .background(
                                        if (selectedTab == 0) MaterialTheme.colorScheme.primary.copy(0.15f) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = strings.get("home"),
                                    tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            // Playlists Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedTab = 1 }
                                    .background(
                                        if (selectedTab == 1) MaterialTheme.colorScheme.primary.copy(0.15f) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = strings.get("playlists"),
                                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            // Library Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedTab = 2 }
                                    .background(
                                        if (selectedTab == 2) MaterialTheme.colorScheme.primary.copy(0.15f) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = strings.get("library"),
                                    tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.White.copy(0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = { if (selectedTab == 1 && !showFullScreenPlayer && selectedPlaylistForView == null) FloatingActionButton({ showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(28.dp), modifier = Modifier.fabShadow()) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary) } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!hasPermission) {
                    PermissionScreen(strings) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            permLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO))
                        else
                            permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO))
                    }
                } else if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            val duration = 350
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth * direction },
                                animationSpec = tween(duration, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(duration / 2))) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -fullWidth * direction },
                                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(duration / 2)))
                        },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            // Tab 0 = Home (Ana Sayfa)
                            0 -> {
                                HomeScreen(
                                    allAudioList = allAudioList,
                                    favoriteIds = favoriteIds,
                                    playlists = playlists,
                                    currentAudioId = currentAudio?.id,
                                    isPlaying = isPlaying,
                                    onPlaylistClick = { selectedPlaylistForView = it },
                                    onPlaylistDelete = { id -> playlists = playlists.filter { p -> p.id != id }; player.savePlaylists(playlists) },
                                    onPlaylistEdit = { playlistToEdit = it },
                                    onPlaySong = { song ->
                                        currentPlaylist = allAudioList
                                        currentAudio = song
                                        player.playAudio(song)
                                        updateSongStats(prefs, song)
                                    },
                                    onShuffleAll = {
                                        if (allAudioList.isNotEmpty()) {
                                            currentPlaylist = allAudioList.shuffled()
                                            currentAudio = currentPlaylist.first()
                                            player.playAudio(currentPlaylist.first())
                                            updateSongStats(prefs, currentPlaylist.first())
                                        }
                                    },
                                    strings = strings,
                                    ctx = ctx,
                                    visualizerLevel = visualizerLevel,
                                    visualizerWaveform = visualizerWaveform
                                )
                            }
                            // Tab 1 = Playlists (Çalma Listeleri)
                            1 -> {
                                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    item {
                                        // Favorites card with gradient
                                        Card(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val favSongs = allAudioList.filter { favoriteIds.contains(it.id.toString()) }
                                                    if (favSongs.isNotEmpty()) {
                                                        selectedPlaylistForView = Playlist(name = strings.get("favorites"), isSmart = true, songIds = favSongs.map { it.id }.toMutableList())
                                                    } else {
                                                        Toast.makeText(ctx, strings.get("empty_playlists"), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .shadow(12.dp, RoundedCornerShape(24.dp)),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFF4081).copy(0.15f)
                                            )
                                        ) {
                                            Row(
                                                Modifier.padding(20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(64.dp)
                                                        .background(
                                                            Brush.radialGradient(
                                                                listOf(
                                                                    Color(0xFFFF4081).copy(0.3f),
                                                                    Color(0xFFFF4081).copy(0.1f)
                                                                )
                                                            ),
                                                            RoundedCornerShape(20.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Favorite,
                                                        null,
                                                        tint = Color(0xFFFF4081),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(20.dp))
                                                Column {
                                                    Text(
                                                        strings.get("favorites"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFF4081),
                                                        fontSize = 18.sp
                                                    )
                                                    Text(
                                                        "${favoriteIds.size} ${strings.get("songs_count")}",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (playlists.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.AutoMirrored.Rounded.QueueMusic,
                                                        null,
                                                        modifier = Modifier.size(64.dp),
                                                        tint = Color.Gray.copy(0.5f)
                                                    )
                                                    Spacer(Modifier.height(16.dp))
                                                    Text(
                                                        strings.get("empty_playlists"),
                                                        color = Color.Gray,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(playlists) { playlist ->
                                        PlaylistItem(
                                            playlist,
                                            { selectedPlaylistForView = playlist },
                                            { playlists = playlists.filter { p -> p.id != playlist.id }; player.savePlaylists(playlists) },
                                            { playlistToEdit = playlist }
                                        )
                                    }
                                }
                            }
                            // Tab 2 = Library (Kütüphane)
                            2 -> {
                                val filtered = allAudioList.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
                                val sorted = if (sortOption == SortOption.NAME) filtered.sortedBy { it.title } else filtered
                                if (sorted.isEmpty()) {
                                    EmptyState(Icons.Rounded.MusicOff, strings.get("empty_library"))
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(0.dp) // No extra spacing, cards have their own padding
                                    ) {
                                        items(sorted, key = { it.id }) { a ->
                                            SongItem(
                                                a, currentAudio?.id == a.id, isPlaying, favoriteIds.contains(a.id.toString()),
                                                onFavClick = { player.toggleFavorite(a.id); refreshFavorites() },
                                                onDelete = { songToDelete = a; showDeleteDialog = true }
                                            ) {
                                                if (currentPlaylist != sorted) currentPlaylist = sorted
                                                currentAudio = a
                                                player.playAudio(a)
                                                updateSongStats(prefs, a)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Screen
        AnimatedVisibility(showSettingsScreen, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.statusBarsPadding()) {
                    TopAppBar(
                        title = { Text(strings.get("settings"), fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton({ showSettingsScreen = false }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
                    )
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        item { SettingsHeader(strings.get("appearance")) }
                        item { SettingsClickItem(Icons.Rounded.Palette, strings.get("theme"), if (currentLang == AppLanguage.TR) currentTheme.labelTR else currentTheme.label) { showThemeDialog = true } }
                        item {
                            var m by remember { mutableStateOf(false) }
                            
                            // Get system language outside of dialog
                            val systemLang = remember {
                                val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    ctx.resources.configuration.locales[0].language
                                } else {
                                    @Suppress("DEPRECATION")
                                    ctx.resources.configuration.locale.language
                                }
                                when (systemLocale.lowercase()) {
                                    "tr" -> AppLanguage.TR
                                    "en" -> AppLanguage.EN
                                    "es" -> AppLanguage.ES
                                    "fr" -> AppLanguage.FR
                                    "de" -> AppLanguage.DE
                                    "it" -> AppLanguage.IT
                                    "pt" -> AppLanguage.PT
                                    "ru" -> AppLanguage.RU
                                    "ja" -> AppLanguage.JA
                                    "zh" -> AppLanguage.ZH
                                    "ar" -> AppLanguage.AR
                                    "ko" -> AppLanguage.KO
                                    else -> AppLanguage.EN
                                }
                            }
                            
                            SettingsClickItem(Icons.Rounded.Language, strings.get("language"), currentLang.label) { m = true }
                            if (m) {
                                AlertDialog(
                                    onDismissRequest = { m = false },
                                    confirmButton = { 
                                        Button(
                                            { m = false }, 
                                            modifier = Modifier.buttonShadow(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) { 
                                            Text(strings.get("ok")) 
                                        } 
                                    },
                                    title = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Language, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(strings.get("language"), fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    text = {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(AppLanguage.values()) { l ->
                                                val isSelected = currentLang == l
                                                val isSystemDefault = l == systemLang
                                                
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onLangChange(l); m = false }
                                                        .cardShadow(),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isSelected) 
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                                    else 
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    border = if (isSelected) 
                                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                                    else null
                                                ) {
                                                    Row(
                                                        Modifier.padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Language flag/icon placeholder
                                                        Box(
                                                            Modifier
                                                                .size(48.dp)
                                                                .background(
                                                                    if (isSelected) 
                                                                        MaterialTheme.colorScheme.primary.copy(0.2f) 
                                                                    else 
                                                                        Color.Transparent,
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                l.code.uppercase(),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = if (isSelected) 
                                                                    MaterialTheme.colorScheme.primary 
                                                                else 
                                                                    MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                                            )
                                                        }
                                                        Spacer(Modifier.width(16.dp))
                                                        Column(Modifier.weight(1f)) {
                                                            Text(
                                                                l.label,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) 
                                                                    MaterialTheme.colorScheme.primary 
                                                                else 
                                                                    MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (isSystemDefault) {
                                                                Spacer(Modifier.height(4.dp))
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        Icons.Rounded.PhoneAndroid,
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                    Spacer(Modifier.width(4.dp))
                                                                    Text(
                                                                        strings.get("system_default"),
                                                                        fontSize = 11.sp,
                                                                        color = MaterialTheme.colorScheme.primary.copy(0.7f),
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (isSelected) {
                                                            Icon(
                                                                Icons.Rounded.CheckCircle,
                                                                null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        // Tempo Visualization Toggle (t8 part of UI improvements)
                        item { SettingsToggleItem(Icons.Rounded.Tune, strings.get("tempo_visualization"), tempoViz) { v -> tempoViz = v; prefs.edit().putBoolean("tempo_visualization", v).apply() } }
                        // Performance Mode Toggle
                        item { 
                            var performanceMode by remember { mutableStateOf(prefs.getBoolean("performance_mode", false)) }
                            SettingsToggleItem(Icons.Rounded.Speed, strings.get("performance_mode"), performanceMode) { v -> 
                                performanceMode = v
                                prefs.edit().putBoolean("performance_mode", v).apply()
                                // When performance mode is enabled, disable tempo visualization
                                if (v) {
                                    tempoViz = false
                                    prefs.edit().putBoolean("tempo_visualization", false).apply()
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("audio")) }
                        item { var s by remember { mutableStateOf(prefs.getBoolean("shake_to_shuffle", true)) }; SettingsToggleItem(Icons.Rounded.Shuffle, strings.get("shake_to_shuffle"), s) { s = it; prefs.edit().putBoolean("shake_to_shuffle", it).apply() } }
                        item { var c by remember { mutableStateOf(prefs.getBoolean("crossfade_enabled", true)) }; SettingsToggleItem(Icons.Rounded.SwapHoriz, strings.get("crossfade"), c) { c = it; prefs.edit().putBoolean("crossfade_enabled", it).apply() } }
                        item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("general")) }
                        item { SettingsClickItem(Icons.Rounded.Folder, strings.get("folders")) { showFolderDialog = true } }
                        item { SettingsClickItem(Icons.Rounded.Info, strings.get("about"), "BeatBox v2.5") { showAboutDialog = true } }
                    }
                }
            }
        }

        // EQ Menu Dialog - Modern Design
        if (showEQMenu) {
            AlertDialog(
                onDismissRequest = { showEQMenu = false },
                confirmButton = { 
                    Button(
                        { showEQMenu = false }, 
                        modifier = Modifier.buttonShadow(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { 
                        Text(strings.get("ok")) 
                    } 
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Equalizer, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(strings.get("equalizer"), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EQPreset.values().forEach { p ->
                            val isSelected = currentEQPreset == p
                            val presetIcon = when(p) {
                                EQPreset.NONE -> Icons.Rounded.MusicNote
                                EQPreset.BASS_BOOST -> Icons.Rounded.GraphicEq
                                EQPreset.VOCAL -> Icons.Rounded.Mic
                                EQPreset.TREBLE -> Icons.Rounded.TrendingUp
                                EQPreset.LOUDNESS -> Icons.Rounded.VolumeUp
                            }
                            val presetColor = when(p) {
                                EQPreset.NONE -> Color(0xFF9E9E9E)
                                EQPreset.BASS_BOOST -> Color(0xFFFF5722)
                                EQPreset.VOCAL -> Color(0xFF2196F3)
                                EQPreset.TREBLE -> Color(0xFF4CAF50)
                                EQPreset.LOUDNESS -> Color(0xFFFF9800)
                            }
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { player.setEQPreset(p); currentEQPreset = p }
                                    .cardShadow(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) 
                                    presetColor.copy(alpha = 0.15f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) 
                                    BorderStroke(2.dp, presetColor) 
                                else null
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .background(
                                                if (isSelected) presetColor.copy(alpha = 0.2f) else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            presetIcon,
                                            null,
                                            tint = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        if (currentLang == AppLanguage.TR) p.labelTR else p.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            null,
                                            tint = presetColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Theme Dialog - Modern Design
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                confirmButton = { 
                    Button(
                        { showThemeDialog = false }, 
                        modifier = Modifier.buttonShadow(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { 
                        Text(strings.get("ok")) 
                    } 
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Palette, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(strings.get("theme"), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AppTheme.values()) { t ->
                            val isSelected = currentTheme == t
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeChange(t); showThemeDialog = false }
                                    .cardShadow(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) 
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                else null
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Gradient or solid color preview with larger size
                                    if (t.isGradient && t.gradientColors.isNotEmpty()) {
                                        Box(
                                            Modifier
                                                .size(56.dp)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = t.gradientColors,
                                                        start = Offset(0f, 0f),
                                                        end = Offset(56f, 56f)
                                                    ),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    2.dp, 
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f), 
                                                    RoundedCornerShape(12.dp)
                                                )
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(56.dp)
                                                .background(t.previewColor, RoundedCornerShape(12.dp))
                                                .border(
                                                    2.dp, 
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f), 
                                                    RoundedCornerShape(12.dp)
                                                )
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (currentLang == AppLanguage.TR) t.labelTR else t.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (t.isGradient) {
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.Gradient,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    strings.get("gradient"), 
                                                    fontSize = 11.sp, 
                                                    color = MaterialTheme.colorScheme.primary.copy(0.7f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            var showUserAgreementFromAbout by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                confirmButton = { Button({ showAboutDialog = false }, modifier = Modifier.buttonShadow()) { Text(strings.get("ok")) } },
                title = { Text(strings.get("about")) },
                text = { 
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) { 
                        Text(
                            strings.get("about_desc"), 
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // User Agreement Button
                        TextButton(
                            onClick = { showUserAgreementFromAbout = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Description,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.get("user_agreement"),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } 
                }
            )
            
            // Show User Agreement Dialog from About
            if (showUserAgreementFromAbout) {
                UserAgreementDialog(
                    strings = strings,
                    onDismiss = { showUserAgreementFromAbout = false }
                )
            }
        }

        // Playlist View
        if (selectedPlaylistForView != null) {
            val songs = allAudioList.filter { selectedPlaylistForView!!.songIds.contains(it.id) }
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.statusBarsPadding()) {
                    TopAppBar(
                        title = { Text(selectedPlaylistForView!!.name) },
                        navigationIcon = { IconButton({ selectedPlaylistForView = null }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        actions = {
                            IconButton({
                                if (songs.isNotEmpty()) {
                                    currentPlaylist = songs
                                    currentAudio = songs.first()
                                    player.playAudio(songs.first())
                                    showFullScreenPlayer = true
                                }
                            }, modifier = Modifier.iconButtonShadow()) {
                                Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
                    )
                    if (songs.isEmpty()) {
                        EmptyState(Icons.Rounded.MusicNote, strings.get("empty_library"))
                    } else {
                        LazyColumn {
                            items(songs) { a ->
                                SongItem(
                                    a, currentAudio?.id == a.id, isPlaying, favoriteIds.contains(a.id.toString()),
                                    onFavClick = { player.toggleFavorite(a.id); refreshFavorites() },
                                    onDelete = { songToDelete = a; showDeleteDialog = true }
                                ) {
                                    currentPlaylist = songs
                                    currentAudio = a
                                    player.playAudio(a)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Player
        AnimatedVisibility(
            showFullScreenPlayer,
            enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut(animationSpec = tween(200))
        ) {
            currentAudio?.let { a ->
                DynamicThemePlayer(a, currentTheme) {
                    FullScreenPlayer(
                        player, a, isPlaying, isShuffle, repeatMode, animationDirection, strings,
                        favoriteIds.contains(a.id.toString()),
                        { player.toggleFavorite(a.id); refreshFavorites() },
                        { showFullScreenPlayer = false },
                        togglePlayPause, playNext, playPrev,
                        { isShuffle = !isShuffle },
                        { repeatMode = (repeatMode + 1) % 3 },
                        { showAddToPlaylistDialog = true }
                    )
                }
            }
        }

        // Create Playlist Dialog - Modern & Sweet Design
        if (showCreateDialog) {
            var n by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            if (n.isNotBlank()) {
                                playlists = playlists + Playlist(name = n.trim())
                                player.savePlaylists(playlists)
                                showCreateDialog = false
                            }
                        },
                        modifier = Modifier.buttonShadow(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.get("ok"), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateDialog = false },
                        modifier = Modifier.buttonShadow()
                    ) {
                        Text(
                            strings.get("cancel"),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            strings.get("new_playlist"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = n,
                            onValueChange = { n = it },
                            label = { Text(strings.get("playlist_name")) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Edit,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary.copy(0.7f)
                                )
                            },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Yeni bir çalma listesi oluştur",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Edit Playlist Dialog
        if (playlistToEdit != null) {
            var n by remember { mutableStateOf(playlistToEdit!!.name) }
            AlertDialog(
                onDismissRequest = { playlistToEdit = null },
                confirmButton = {
                    Button({
                        if (n.isNotBlank()) {
                            playlistToEdit!!.name = n
                            player.savePlaylists(playlists)
                            playlistToEdit = null
                        }
                    }, modifier = Modifier.buttonShadow()) { Text(strings.get("ok")) }
                },
                dismissButton = { TextButton({ playlistToEdit = null }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
                title = { Text(strings.get("update")) },
                text = { OutlinedTextField(n, { n = it }, label = { Text(strings.get("playlist_name")) }, shape = RoundedCornerShape(28.dp)) }
            )
        }

        // Add to Playlist Dialog
        if (showAddToPlaylistDialog && currentAudio != null) {
            AlertDialog(
                onDismissRequest = { showAddToPlaylistDialog = false },
                confirmButton = { TextButton({ showAddToPlaylistDialog = false }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
                title = { Text(strings.get("add_to_playlist")) },
                text = {
                    Column {
                        playlists.forEach { p ->
                            val isAdded = p.songIds.contains(currentAudio!!.id)
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    if (!isAdded) {
                                        p.songIds.add(currentAudio!!.id)
                                        player.savePlaylists(playlists)
                                    }
                                    showAddToPlaylistDialog = false
                                }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Text(p.name)
                                Spacer(Modifier.weight(1f))
                                if (isAdded) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            )
        }

        // Folder Dialog - Modern & Sweet Design
        if (showFolderDialog) {
            AlertDialog(
                onDismissRequest = { showFolderDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showFolderDialog = false
                            // Reload audio files with new folder selection
                            isLoading = true
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val (l, f) = loadAudioFilesAndFolders(ctx, minDurationSeconds, selectedFolders)
                                    withContext(Dispatchers.Main) {
                                        allAudioList = l
                                        availableFolders = f
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.buttonShadow(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.get("ok"), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFolderDialog = false },
                        modifier = Modifier.buttonShadow()
                    ) {
                        Text(
                            strings.get("cancel"),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                strings.get("folders"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "${selectedFolders.size} klasör seçili",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary.copy(0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                text = {
                    Column {
                        Text(
                            "Müzik klasörlerini seç",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableFolders) { f ->
                                val isSelected = selectedFolders.contains(f)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val n = selectedFolders.toMutableSet()
                                            if (n.contains(f)) n.remove(f) else n.add(f)
                                            selectedFolders = n
                                            player.saveSelectedFolders(n)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                                    border = if (isSelected)
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(0.5f))
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Icon(
                                            Icons.Rounded.Folder,
                                            null,
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            f,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Sleep Timer Dialog - Redesigned with blur effect
        if (showSleepTimerDialog) {
            var m by remember { mutableIntStateOf(15) }
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                confirmButton = {
                    Button(
                        onClick = { 
                            player.sleepTimerMinutes = m
                            player.sleepTimerSeconds = 0
                            player.isSleepTimerRunning = true
                            showSleepTimerDialog = false 
                        },
                        modifier = Modifier
                            .buttonShadow()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(Color.Transparent)
                    ) {
                        Icon(Icons.Rounded.Nightlight, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.get("start"), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { 
                    TextButton(
                        onClick = { showSleepTimerDialog = false },
                        modifier = Modifier.buttonShadow()
                    ) { 
                        Text(strings.get("cancel")) 
                    } 
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Nightlight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(strings.get("sleep_timer"), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Beautiful time display with seconds
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%02d:00", m),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${strings.get("minutes")}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        // Slider with custom styling
                        Column {
                            Slider(
                                value = m.toFloat(),
                                onValueChange = { m = it.toInt() },
                                valueRange = 5f..120f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("5 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Text("120 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                    }
                }
            )
        }

        // Delete Song Dialog
        if (showDeleteDialog && songToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; songToDelete = null },
                confirmButton = {
                    Button({
                        if (player.deleteSong(songToDelete!!)) {
                            allAudioList = allAudioList.filter { it.id != songToDelete!!.id }
                            refreshFavorites()
                        }
                        showDeleteDialog = false
                        songToDelete = null
                    }, modifier = Modifier.buttonShadow()) {
                        Text(strings.get("delete"), color = Color.Red)
                    }
                },
                dismissButton = { TextButton({ showDeleteDialog = false; songToDelete = null }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
                title = { Text(strings.get("delete_song")) },
                text = { Text("\"${songToDelete!!.title}\" ${strings.get("delete_confirm")}") }
            )
        }
        }
    }
}

@Composable
fun HomeScreen(
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    currentAudioId: Long?,
    isPlaying: Boolean,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistDelete: (String) -> Unit,
    onPlaylistEdit: (Playlist) -> Unit,
    onPlaySong: (AudioFile) -> Unit,
    onShuffleAll: () -> Unit,
    strings: Strings,
    ctx: Context,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray
) {
    val prefs = ctx.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
    val topSongs = allAudioList.sortedByDescending { prefs.getInt("stats_count_${it.id}", 0) }.take(9)
    val recentlyAdded = allAudioList.sortedByDescending { it.dateAdded }.take(5)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            // Modern header with gradient
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.TrendingUp,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        strings.get("most_played"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                topSongs.take(5).forEach { song ->
                    val isCurrent = song.id == currentAudioId
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            Modifier
                                .size(85.dp)
                                .clickable { onPlaySong(song) }
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Box {
                                AlbumArtImage(song, Modifier.fillMaxSize(), isHQ = true)
                                if (isCurrent && isPlaying) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.6f),
                                                        Color.Black.copy(alpha = 0.3f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(18.dp))
                                    }
                                } else if (isCurrent) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                        Text(
                            song.title,
                            maxLines = 1,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            song.artist,
                            fontSize = 10.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.7f) else Color.Gray,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                topSongs.drop(5).take(4).forEach { song ->
                    val isCurrent = song.id == currentAudioId
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            Modifier
                                .size(85.dp)
                                .clickable { onPlaySong(song) }
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Box {
                                AlbumArtImage(song, Modifier.fillMaxSize(), isHQ = true)
                                if (isCurrent && isPlaying) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.6f),
                                                        Color.Black.copy(alpha = 0.3f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(18.dp))
                                    }
                                } else if (isCurrent) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                        Text(
                            song.title,
                            maxLines = 1,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            song.artist,
                            fontSize = 10.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.7f) else Color.Gray,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        Modifier
                            .size(85.dp)
                            .clickable { onShuffleAll() }
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(0.3f),
                                    MaterialTheme.colorScheme.primary.copy(0.1f)
                                )
                            ).let { MaterialTheme.colorScheme.primaryContainer }
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Icon(Icons.Rounded.Shuffle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(strings.get("shuffle"), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Text(strings.get("all"), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
        item {
            // Modern header for recently added
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.NewReleases,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        strings.get("recently_added"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        items(recentlyAdded) { song ->
            val isCurrent = song.id == currentAudioId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaySong(song) }
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isCurrent) 
                    MaterialTheme.colorScheme.primaryContainer.copy(0.4f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                shadowElevation = if (isCurrent) 4.dp else 2.dp
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        AlbumArtImage(song, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), isHQ = true)
                        if (isCurrent && isPlaying) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Black.copy(alpha = 0.3f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                MusicVisualizer(true, visualizerLevel, visualizerWaveform, Modifier.height(16.dp))
                            }
                        } else if (isCurrent) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        Text(
                            song.artist,
                            fontSize = 13.sp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.7f) else Color.Gray
                        )
                    }
                    if (isCurrent && isPlaying) {
                        MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(28.dp).padding(end = 8.dp))
                    }
                }
            }
        }
    }
}



@Composable
fun FullScreenPlayer(
    player: MainActivity,
    a: AudioFile,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    animDir: Int,
    strings: Strings,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onAdd: () -> Unit
) {
    var pos by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showLyrics by remember { mutableStateOf(false) }
    
    // Create LyricsViewModel instance with error handling
    val lyricsViewModel = remember {
        try {
            val context = player.applicationContext
            val database = LyricsDatabase.getDatabase(context)
            val lyricsProvider = LyricsProvider(
                context = context,
                lyricsCache = database.lyricsCacheDao(),
                lrcLibApi = LRCLIBApiClient.api
            )
            LyricsViewModel(lyricsProvider)
        } catch (e: Exception) {
            android.util.Log.e("FullScreenPlayer", "Error creating LyricsViewModel", e)
            null
        }
    }
    
    // Set up position callback when lyrics screen is shown
    LaunchedEffect(showLyrics) {
        if (showLyrics && lyricsViewModel != null) {
            player.musicService?.setLyricsPositionCallback { position ->
                lyricsViewModel.updatePlaybackPosition(position)
            }
        } else {
            player.musicService?.removeLyricsPositionCallback()
        }
    }
    
    // Clean up callback when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            player.musicService?.removeLyricsPositionCallback()
        }
    }
    
    // Extract dominant color from album art
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val defaultBackgroundColor = MaterialTheme.colorScheme.background
    
    var dominantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }
    var vibrantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }
    
    LaunchedEffect(a.id, defaultPrimaryColor) {
        val primaryArgb = defaultPrimaryColor.toArgb()
        withContext(Dispatchers.IO) {
            try {
                // Get bitmap from cache
                AlbumArtCache.bitmapCache.get(a.id)?.let { bitmap ->
                    // Extract colors using Palette
                    val palette = Palette.from(bitmap).generate()
                    
                    withContext(Dispatchers.Main) {
                        // Get dominant color with fallback
                        dominantColor = Color(palette.getDominantColor(primaryArgb))
                        
                        // Get vibrant color with fallback
                        vibrantColor = Color(
                            palette.getVibrantColor(
                                palette.getLightVibrantColor(
                                    palette.getDarkVibrantColor(primaryArgb)
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FullScreenPlayer", "Error extracting colors", e)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        withContext(Dispatchers.Default) { // ✅ Background thread
            while (isPlaying) {
                if (!drag) {
                    val currentPos = player.getCurrentPosition().toFloat()
                    withContext(Dispatchers.Main) {
                        pos = currentPos
                    }
                }
                delay(100)
            }
        }
    }
    
    // Animated color transition
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dominant_color"
    )
    
    val animatedVibrant by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "vibrant_color"
    )
    
    // Create dynamic gradient background
    val dynamicGradient = remember(animatedDominant, animatedVibrant) {
        Brush.verticalGradient(
            colors = listOf(
                animatedDominant.copy(alpha = 0.8f),
                animatedVibrant.copy(alpha = 0.6f),
                defaultBackgroundColor
            ),
            startY = 0f,
            endY = 2000f
        )
    }

    Box(Modifier.fillMaxSize().background(dynamicGradient)) {
        Column(Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onCollapse, modifier = Modifier.iconButtonShadow()) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onBackground) }
                Text(strings.get("app_name"), fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.7f))
                IconButton(onAdd, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onBackground) }
            }
            Spacer(Modifier.height(40.dp))
            AnimatedContent(
                targetState = a,
                transitionSpec = {
                    val duration = 380
                    val enter = if (animDir > 0) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(
                                    initialScale = 0.9f,
                                    animationSpec = tween(duration)
                                )
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(
                                    initialScale = 1.1f,
                                    animationSpec = tween(duration)
                                )
                    }
                    val exit = if (animDir > 0) {
                        slideOutHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(
                                    targetScale = 1.05f,
                                    animationSpec = tween(duration)
                                )
                    } else {
                        slideOutHorizontally(animationSpec = tween(duration)) { it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(
                                    targetScale = 0.95f,
                                    animationSpec = tween(duration)
                                )
                    }
                    (enter togetherWith exit).using(
                        SizeTransform(clip = false)
                    )
                },
                label = "song_transition"
            ) { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(20.dp),
                        modifier = Modifier.size(320.dp).cardShadow()
                    ) {
                        AlbumArtImage(c, Modifier.fillMaxSize(), true)
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(
                        c.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        c.artist,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                        maxLines = 1
                    )
                }
            }
            IconButton(onToggleFav, Modifier.padding(top = 10.dp).iconButtonShadow()) {
                Icon(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = if (isFav) Color(0xFFFF4081) else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Lyrics button (only show if viewModel is available)
            if (lyricsViewModel != null) {
                IconButton(
                    onClick = { showLyrics = true },
                    modifier = Modifier.padding(top = 10.dp).iconButtonShadow()
                ) {
                    Icon(
                        Icons.Rounded.Subtitles,
                        contentDescription = strings.get("lyrics"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            Box(Modifier.align(Alignment.End)) {
                TextButton({ speedMenu = true }, modifier = Modifier.buttonShadow()) {
                    Text("${currentSpeed}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(speedMenu, { speedMenu = false }) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s}x") },
                            onClick = { currentSpeed = s; player.setPlaybackSpeed(s); speedMenu = false }
                        )
                    }
                }
            }
            Slider(
                pos,
                { drag = true; pos = it },
                onValueChangeFinished = { player.seekTo(pos.toInt()); drag = false },
                valueRange = 0f..(a.duration.toFloat().takeIf { it > 0 } ?: 1f),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(formatTime(pos.toLong()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(formatTime(a.duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.1f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onShuffle, modifier = Modifier.iconButtonShadow()) {
                        Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(28.dp), tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                    IconButton(onPrev, Modifier.size(64.dp).iconButtonShadow()) {
                        Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onNext, Modifier.size(64.dp).iconButtonShadow()) {
                        Icon(Icons.Rounded.SkipNext, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onRepeat, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            null,
                            modifier = Modifier.size(28.dp),
                            tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
        
        // Show lyrics screen as modal overlay
        if (showLyrics && lyricsViewModel != null) {
            LyricsScreen(
                audio = a,
                currentPositionMs = pos.toLong(),
                isPlaying = isPlaying,
                viewModel = lyricsViewModel,
                strings = strings,
                onDismiss = { showLyrics = false }
            )
        }
    }
}

