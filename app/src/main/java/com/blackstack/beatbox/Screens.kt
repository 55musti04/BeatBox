package com.blackstack.beatbox

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.math.abs
import kotlin.random.Random

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
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            else
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }
    // Tempo visualization toggle state (tempo-based visualizations).
    // Declared before the launchers because the microphone launcher callback
    // has to be able to turn it back on.
    var tempoViz by remember {
        mutableStateOf(
            prefs.getBoolean("tempo_visualization", true) &&
            !prefs.getBoolean("performance_mode", false) // Disable if performance mode is on
        )
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        // Only the audio library permission gates the app. Denying the
        // notification permission must not lock the user out of their music.
        val audioGranted = perms.entries.any { (permission, granted) ->
            granted && (permission == Manifest.permission.READ_MEDIA_AUDIO ||
                permission == Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (audioGranted) hasPermission = true
    }

    // The microphone is only used by the waveform visualiser, so it is
    // requested on demand instead of being bundled into the startup request.
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            prefs.edit { putBoolean("tempo_visualization", true) }
            tempoViz = true
        }
    }

    if (showOnboarding) {
        OnboardingScreen(strings) {
            showOnboarding = false
            prefs.edit { putBoolean("first_launch", false) }
            if (!hasPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
                } else {
                    permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
        return
    }

    var allAudioList by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var availableFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf(player.loadSelectedFolders()) }
    var minDurationSeconds by remember { mutableIntStateOf(player.loadMinDurationSetting()) }
    var isLoading by remember { mutableStateOf(true) }

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
    var showWrappedDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(loadUserProfile(prefs)) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val avatarIcons = listOf(
        Icons.Rounded.Person,
        Icons.Rounded.Face,
        Icons.Rounded.Psychology,
        Icons.Rounded.Mood,
        Icons.Rounded.Star,
        Icons.Rounded.MusicNote
    )

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
            }
        }
    }

    fun refreshFavorites() { favoriteIds = player.getFavorites() }

    val playNext: () -> Unit = {
        val listToUse = if (currentPlaylist.isNotEmpty()) currentPlaylist else allAudioList
        if (listToUse.isNotEmpty()) {
            animationDirection = 1
            val n = if (isShuffle) {
                val available = listToUse.filter { it.id != currentAudio?.id }
                if (available.isNotEmpty()) available.random() else listToUse.random()
            } else {
                val i = listToUse.indexOfFirst { it.id == currentAudio?.id }
                if (i >= 0 && i < listToUse.size - 1) {
                    listToUse[i + 1]
                } else {
                    listToUse.first()
                }
            }
            currentAudio = n
            player.playAudio(n)
        }
    }

    val playPrev: () -> Unit = {
        val listToUse = if (currentPlaylist.isNotEmpty()) currentPlaylist else allAudioList
        if (listToUse.isNotEmpty()) {
            animationDirection = -1
            val p = if (isShuffle) {
                val available = listToUse.filter { it.id != currentAudio?.id }
                if (available.isNotEmpty()) available.random() else listToUse.random()
            } else {
                val i = listToUse.indexOfFirst { it.id == currentAudio?.id }
                if (i > 0) {
                    listToUse[i - 1]
                } else {
                    listToUse.last()
                }
            }
            currentAudio = p
            player.playAudio(p)
        }
    }

    val togglePlayPause: () -> Unit = { currentAudio?.let { if (isPlaying) player.pauseAudio(it) else player.resumeAudio(it) } }

    player.onSongComplete = {
        animationDirection = 1
        if (repeatMode == 2) {
            // Repeat One: replay current track from start
            currentAudio?.let { player.playAudio(it) }
        } else {
            playNext()
        }
    }
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
                                .height(64.dp)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                ),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            shadowElevation = 12.dp,
                            tonalElevation = 4.dp
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
                                        onClick = { showEQMenu = true },
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
                                        onClick = { showSleepTimerDialog = true },
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
                                
                                // Right side - Refresh, Profile and Settings
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (userProfile.isLoggedIn) showProfileDialog = true else showAuthDialog = true
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (userProfile.isLoggedIn) MaterialTheme.colorScheme.primary.copy(0.25f) else MaterialTheme.colorScheme.primary.copy(0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            avatarIcons.getOrElse(userProfile.avatarId) { Icons.Rounded.Person },
                                            contentDescription = "User Profile",
                                            tint = if (userProfile.isLoggedIn) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showSettingsScreen = true },
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
                                            text = String.format(java.util.Locale.getDefault(), "%02d:%02d", player.sleepTimerMinutes, player.sleepTimerSeconds),
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(strings.get("search_hint")) },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, strings.get("search_hint"))
                                            }
                                            speechLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Sesli arama desteklenmiyor", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Rounded.Mic,
                                            contentDescription = "Voice Search",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.3f)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Box {
                                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = MaterialTheme.colorScheme.onSurface) }
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
                    if (!showFullScreenPlayer) {
                        MiniPlayerBar(currentAudio, isPlaying, { if (currentAudio != null) showFullScreenPlayer = true }, togglePlayPause, playNext, playPrev)
                    }
                    // Bottom Navigation Bar - Modern & Sweet Theme-Adaptive Design
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        shadowElevation = 16.dp,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernBottomNavigationItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = Icons.Rounded.Home,
                                label = strings.get("home"),
                                modifier = Modifier.weight(1f)
                            )
                            ModernBottomNavigationItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                label = strings.get("playlists"),
                                modifier = Modifier.weight(1f)
                            )
                            ModernBottomNavigationItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = Icons.Rounded.LibraryMusic,
                                label = strings.get("library"),
                                modifier = Modifier.weight(1f)
                            )
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
                            permLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
                        else
                            permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
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
                                    currentAudio = currentAudio,
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
                                val artistGroups = remember(allAudioList, strings) { groupSongsByArtist(allAudioList, strings.get("unknown_artist")) }
                                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    item {
                                        // Auto-create artist playlists button card
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    var addedCount = 0
                                                    val mutablePlaylists = playlists.toMutableList()
                                                    for ((artistName, artistSongs) in artistGroups) {
                                                        if (artistSongs.isNotEmpty() && !mutablePlaylists.any { it.isArtistPlaylist && it.artistName.equals(artistName, true) }) {
                                                            mutablePlaylists.add(
                                                                 Playlist(
                                                                    name = "${strings.get("artist")}: $artistName",
                                                                    songIds = artistSongs.map { it.id }.toMutableList(),
                                                                    isArtistPlaylist = true,
                                                                    artistName = artistName
                                                                )
                                                            )
                                                            addedCount++
                                                        }
                                                    }
                                                    if (addedCount > 0) {
                                                        playlists = mutablePlaylists
                                                        player.savePlaylists(playlists)
                                                        Toast.makeText(ctx, strings.get("artist_playlists_created"), Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(ctx, strings.get("all_artist_playlists_exist"), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(0.2f), RoundedCornerShape(14.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        strings.get("create_artist_playlists"),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        "${artistGroups.size} ${strings.get("artists_found")}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                                    )
                                                }
                                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    if (artistGroups.isNotEmpty()) {
                                        item {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    strings.get("artist_playlists"),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    items(artistGroups.entries.toList()) { (artistName, songs) ->
                                                        ArtistCard(
                                                            artistName = artistName,
                                                            songCount = songs.size,
                                                            strings = strings,
                                                            onClick = {
                                                                val existing = playlists.find { it.isArtistPlaylist && it.artistName.equals(artistName, true) }
                                                                if (existing != null) {
                                                                    selectedPlaylistForView = existing
                                                                } else {
                                                                    val newPl = Playlist(
                                                                        name = "${strings.get("artist")}: $artistName",
                                                                        songIds = songs.map { it.id }.toMutableList(),
                                                                        isArtistPlaylist = true,
                                                                        artistName = artistName
                                                                    )
                                                                    playlists = playlists + newPl
                                                                    player.savePlaylists(playlists)
                                                                    selectedPlaylistForView = newPl
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

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
                                            { playlistToEdit = playlist },
                                            strings = strings
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
                                                a = a,
                                                strings = strings,
                                                isCurrent = currentAudio?.id == a.id,
                                                isPlaying = isPlaying,
                                                isFavorite = favoriteIds.contains(a.id.toString()),
                                                onFavClick = { player.toggleFavorite(a.id); refreshFavorites() },
                                                onDelete = { songToDelete = a; showDeleteDialog = true },
                                                onClick = {
                                                    if (currentPlaylist != sorted) currentPlaylist = sorted
                                                    currentAudio = a
                                                    player.playAudio(a)
                                                    updateSongStats(prefs, a)
                                                }
                                            )
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
                        navigationIcon = { IconButton(onClick = { showSettingsScreen = false }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
                    )
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (userProfile.isLoggedIn) showProfileDialog = true else showAuthDialog = true
                                    }
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(0.25f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            avatarIcons.getOrElse(userProfile.avatarId) { Icons.Rounded.Person },
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            userProfile.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            if (userProfile.isLoggedIn)
                                                (if (userProfile.email.isNotBlank()) userProfile.email else userProfile.bio)
                                            else
                                                strings.get("login_prompt"),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            if (userProfile.isLoggedIn) strings.get("edit") else strings.get("login"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item { SettingsHeader(strings.get("appearance")) }
                        item { SettingsClickItem(Icons.Rounded.Palette, strings.get("theme"), strings.getThemeLabel(currentTheme)) { showThemeDialog = true } }
                        item {
                            var m by remember { mutableStateOf(false) }
                            
                            // Get system language outside of dialog
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val systemLang = remember(configuration) {
                                val systemLocale = configuration.locales[0].language
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
                                            items(AppLanguage.entries) { l ->
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
                        // Tempo Visualization Toggle
                        item {
                            SettingsToggleItem(Icons.Rounded.Tune, strings.get("tempo_visualization"), tempoViz) { v ->
                                if (v) {
                                    // The waveform visualiser reads the live
                                    // microphone input, so the permission is
                                    // asked for at the moment the user opts in.
                                    val needsMic = ContextCompat.checkSelfPermission(
                                        ctx, Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                                    if (needsMic) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        tempoViz = true
                                        prefs.edit { putBoolean("tempo_visualization", true) }
                                    }
                                } else {
                                    tempoViz = false
                                    prefs.edit { putBoolean("tempo_visualization", false) }
                                }
                            }
                        }
                        // Performance Mode Toggle
                        item { 
                            var performanceMode by remember { mutableStateOf(prefs.getBoolean("performance_mode", false)) }
                            SettingsToggleItem(Icons.Rounded.Speed, strings.get("performance_mode"), performanceMode) { v -> 
                                performanceMode = v
                                prefs.edit { putBoolean("performance_mode", v) }
                                if (v) {
                                    tempoViz = false
                                    prefs.edit { putBoolean("tempo_visualization", false) }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("audio")) }
                        item { var s by remember { mutableStateOf(prefs.getBoolean("shake_to_shuffle", true)) }; SettingsToggleItem(Icons.Rounded.Shuffle, strings.get("shake_to_shuffle"), s) { s = it; prefs.edit { putBoolean("shake_to_shuffle", it) } } }
                        item { var c by remember { mutableStateOf(prefs.getBoolean("crossfade_enabled", true)) }; SettingsToggleItem(Icons.Rounded.SwapHoriz, strings.get("crossfade"), c) { c = it; prefs.edit { putBoolean("crossfade_enabled", it) } } }
                        item {
                            var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
                            SettingsToggleItem(Icons.Rounded.ScreenLockLandscape, strings.get("keep_screen_on"), keepScreenOn) { v ->
                                keepScreenOn = v
                                player.setKeepScreenOn(v)
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("personalization")) }
                        item { SettingsClickItem(Icons.Rounded.Analytics, strings.get("wrapped_title"), strings.get("wrapped_desc")) { showWrappedDialog = true } }
                        item { SettingsClickItem(Icons.Rounded.History, strings.get("listening_history"), strings.get("history_desc")) { showHistoryDialog = true } }
                        item {
                            SettingsClickItem(
                                Icons.Rounded.CleaningServices,
                                strings.get("clear_cache"),
                                "RAM & Görsel Önbelleği"
                            ) {
                                ArtistImageProvider.bitmapCache.evictAll()
                                AlbumArtCache.bitmapCache.evictAll()
                                Toast.makeText(ctx, strings.get("cache_cleared"), Toast.LENGTH_SHORT).show()
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("general")) }
                        item { SettingsClickItem(Icons.Rounded.Folder, strings.get("folders")) { showFolderDialog = true } }
                        item { SettingsClickItem(Icons.Rounded.Info, strings.get("about"), "BeatBox v3.0") { showAboutDialog = true } }
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
                        Text(strings.get("ok"), fontWeight = FontWeight.SemiBold) 
                    } 
                },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Equalizer, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(strings.get("equalizer"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                strings.get("eq_subtitle"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Live spectrum visualizer preview inside EQ dialog
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val waveform = visualizerWaveform
                                val barCount = if (waveform.isNotEmpty()) minOf(12, waveform.size) else 12
                                for (i in 0 until barCount) {
                                    val heightFraction = if (waveform.isNotEmpty() && i < waveform.size) waveform[i].coerceIn(0.1f, 1f) else 0.2f
                                    Box(
                                        Modifier
                                            .width(4.dp)
                                            .fillMaxHeight(heightFraction)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        EQPreset.entries.forEach { p ->
                            val isSelected = currentEQPreset == p
                            val presetIcon = when(p) {
                                EQPreset.NONE -> Icons.Rounded.MusicNote
                                EQPreset.BASS_BOOST -> Icons.Rounded.GraphicEq
                                EQPreset.VOCAL -> Icons.Rounded.Mic
                                EQPreset.TREBLE -> Icons.AutoMirrored.Rounded.TrendingUp
                                EQPreset.LOUDNESS -> Icons.AutoMirrored.Rounded.VolumeUp
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
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) 
                                    presetColor.copy(alpha = 0.15f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = if (isSelected) 
                                    BorderStroke(2.dp, presetColor) 
                                else null
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(42.dp)
                                            .background(
                                                if (isSelected) presetColor.copy(alpha = 0.25f) else Color.Transparent,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            presetIcon,
                                            null,
                                            tint = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            strings.getEQPresetLabel(p),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            strings.getEQPresetDesc(p),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            null,
                                            tint = presetColor,
                                            modifier = Modifier.size(22.dp)
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
                                            strings.getThemeLabel(t),
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
            val pl = selectedPlaylistForView!!
            val songs = allAudioList.filter { pl.songIds.contains(it.id) }
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.statusBarsPadding()) {
                    TopAppBar(
                        title = { Text(pl.name) },
                        navigationIcon = { IconButton(onClick = { selectedPlaylistForView = null }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                        actions = {
                            IconButton(onClick = {
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
                    
                    if (pl.isArtistPlaylist && !pl.artistName.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .cardShadow(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ArtistImage(
                                        artistName = pl.artistName,
                                        modifier = Modifier.fillMaxSize(),
                                        isHQ = true
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = pl.artistName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${songs.size} ${strings.get("songs")} • ${strings.get("artist_playlist_tag")}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                            }
                        }
                    }

                    if (songs.isEmpty()) {
                        EmptyState(Icons.Rounded.MusicNote, strings.get("empty_library"))
                    } else {
                        LazyColumn {
                            items(songs) { a ->
                                SongItem(
                                    a = a,
                                    strings = strings,
                                    isCurrent = currentAudio?.id == a.id,
                                    isPlaying = isPlaying,
                                    isFavorite = favoriteIds.contains(a.id.toString()),
                                    onFavClick = { player.toggleFavorite(a.id); refreshFavorites() },
                                    onDelete = { songToDelete = a; showDeleteDialog = true },
                                    onClick = {
                                        currentPlaylist = songs
                                        currentAudio = a
                                        player.playAudio(a)
                                    }
                                )
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
                        player = player,
                        a = a,
                        isPlaying = isPlaying,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        animDir = animationDirection,
                        strings = strings,
                        isFav = favoriteIds.contains(a.id.toString()),
                        onToggleFav = { player.toggleFavorite(a.id); refreshFavorites() },
                        onCollapse = { showFullScreenPlayer = false },
                        onPlayPause = togglePlayPause,
                        onNext = playNext,
                        onPrev = playPrev,
                        onShuffle = {
                            isShuffle = !isShuffle
                            val msg = if (isShuffle) strings.get("shuffle_on") else strings.get("shuffle_off")
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        },
                        onRepeat = {
                            repeatMode = (repeatMode + 1) % 3
                            val msg = when (repeatMode) {
                                1 -> strings.get("repeat_all")
                                2 -> strings.get("repeat_one")
                                else -> strings.get("repeat_off")
                            }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                        },
                        onAdd = { showAddToPlaylistDialog = true },
                        currentPlaylist = currentPlaylist,
                        onSelectFromQueue = { song ->
                            currentAudio = song
                            player.playAudio(song)
                            updateSongStats(prefs, song)
                        },
                        onOpenEQ = { showEQMenu = true },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        visualizerLevel = visualizerLevel,
                        visualizerWaveform = visualizerWaveform
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
                            strings.get("new_playlist_subtitle"),
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
                            val target = playlistToEdit!!
                            playlists = playlists.map { p ->
                                if (p.id == target.id) p.copy(name = n) else p
                            }
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
                                    text = String.format(java.util.Locale.getDefault(), "%02d:00", m),
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

        // BeatBox Wrapped Dialog
        if (showWrappedDialog) {
            val totalMinutes = prefs.getLong("stats_total_minutes", 0L)
            val topSongs = allAudioList.sortedByDescending { prefs.getInt("stats_count_${it.id}", 0) }.take(5)
            val topSong = topSongs.firstOrNull()
            val topArtist = topSongs.groupBy { it.artist }.maxByOrNull { it.value.size }?.key ?: (topSong?.artist ?: "Bilinmiyor")

            AlertDialog(
                onDismissRequest = { showWrappedDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val shareText = "🔥 My BeatBox Wrapped 🎵\n\n⏱️ Total Listening: $totalMinutes minutes\n🏆 Top Song: \"${topSong?.title ?: "N/A"}\" by ${topSong?.artist ?: "N/A"}\n🎤 Top Artist: $topArtist\n\nListen on BeatBox!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "BeatBox Wrapped")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Müzik Özetini Paylaş"))
                        },
                        modifier = Modifier.buttonShadow()
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Özeti Paylaş", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWrappedDialog = false }) {
                        Text(strings.get("cancel"))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("BeatBox Wrapped 🎵", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(strings.get("total_listen"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                    Text("$totalMinutes ${strings.get("minutes")}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (topSong != null) {
                            Column {
                                Text(strings.get("most_played_song"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("\"${topSong.title}\" - ${topSong.artist}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("${prefs.getInt("stats_count_${topSong.id}", 0)}x", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (topSongs.isNotEmpty()) {
                            Column {
                                Text(strings.get("top_5_songs"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp))
                                topSongs.forEachIndexed { index, song ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        Text(song.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                        Text("${prefs.getInt("stats_count_${song.id}", 0)}x", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Listening History Dialog
        if (showHistoryDialog) {
            val historyRaw = prefs.getString("stats_history", "") ?: ""
            val historyIds = historyRaw.split(",").filter { it.isNotBlank() }
            val historySongs = historyIds.mapNotNull { idStr -> allAudioList.find { it.id.toString() == idStr } }

            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                confirmButton = {
                    if (historySongs.isNotEmpty()) {
                        TextButton(onClick = {
                            prefs.edit { remove("stats_history") }
                            showHistoryDialog = false
                        }) {
                            Text("Geçmişi Temizle", color = Color.Red)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text(strings.get("cancel"))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Dinleme Geçmişi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    if (historySongs.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Henüz dinleme geçmişi yok", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(historySongs, key = { it.id }) { song ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        currentPlaylist = historySongs
                                        currentAudio = song
                                        player.playAudio(song)
                                        updateSongStats(prefs, song)
                                        showHistoryDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                                ) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AlbumArtImage(song, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                            Text(song.artist, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        }
                                        Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // User Auth Dialog
        if (showAuthDialog) {
            AuthDialog(
                strings = strings,
                onDismiss = { showAuthDialog = false },
                onLoginSuccess = { newUser ->
                    userProfile = newUser
                    saveUserProfile(prefs, newUser)
                    showAuthDialog = false
                    Toast.makeText(ctx, "Hoş geldiniz, ${newUser.displayName}!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // User Profile Dialog
        if (showProfileDialog) {
            ProfileDialog(
                user = userProfile,
                avatarIcons = avatarIcons,
                onDismiss = { showProfileDialog = false },
                onSaveProfile = { updatedUser ->
                    userProfile = updatedUser
                    saveUserProfile(prefs, updatedUser)
                    Toast.makeText(ctx, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                },
                onLogout = {
                    val loggedOut = UserProfile(isLoggedIn = false)
                    userProfile = loggedOut
                    saveUserProfile(prefs, loggedOut)
                    Toast.makeText(ctx, "Oturum kapatıldı", Toast.LENGTH_SHORT).show()
                }
            )
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
    onAdd: () -> Unit,
    currentPlaylist: List<AudioFile> = emptyList(),
    onSelectFromQueue: ((AudioFile) -> Unit)? = null,
    onOpenEQ: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    visualizerLevel: Float = 0f,
    visualizerWaveform: FloatArray = FloatArray(0)
) {
    val context = LocalContext.current
    var pos by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }

    // Volume Control using AudioManager
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember(audioManager) { mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) }

    // Create LyricsViewModel instance with error handling
    val lyricsViewModel = remember {
        try {
            val ctx = player.applicationContext
            val database = LyricsDatabase.getDatabase(ctx)
            val lyricsProvider = LyricsProvider(
                context = ctx,
                lyricsCache = database.lyricsCacheDao()
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

    DisposableEffect(Unit) {
        onDispose {
            player.musicService?.removeLyricsPositionCallback()
        }
    }

    // Extract dominant and vibrant colors from album art
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val defaultBackgroundColor = MaterialTheme.colorScheme.background

    var dominantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }
    var vibrantColor by remember(a.id) { mutableStateOf(defaultPrimaryColor) }

    LaunchedEffect(a.id, defaultPrimaryColor) {
        val primaryArgb = defaultPrimaryColor.toArgb()
        withContext(Dispatchers.IO) {
            try {
                AlbumArtCache.bitmapCache.get(a.id)?.let { bitmap ->
                    val palette = Palette.from(bitmap).generate()
                    withContext(Dispatchers.Main) {
                        dominantColor = Color(palette.getDominantColor(primaryArgb))
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
        withContext(Dispatchers.Default) {
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

    // Pulse animation for album art glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.55f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

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

    val dynamicGradient = remember(animatedDominant, animatedVibrant) {
        Brush.verticalGradient(
            colors = listOf(
                animatedDominant.copy(alpha = 0.85f),
                animatedVibrant.copy(alpha = 0.6f),
                defaultBackgroundColor
            ),
            startY = 0f,
            endY = 2200f
        )
    }

    // Outer Box: Fully Opaque, Consumes Clicks to Prevent Touch Pass-Through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consume clicks to block touch pass-through to background screen */ }
            .background(Color(0xFF0C0D15)) // 100% Opaque Solid Dark Background
            .background(dynamicGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse, modifier = Modifier.iconButtonShadow()) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = strings.get("now_playing_badge"),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onOpenEQ != null) {
                        IconButton(onClick = onOpenEQ, modifier = Modifier.iconButtonShadow()) {
                            Icon(
                                Icons.Rounded.Equalizer,
                                contentDescription = "Equalizer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (onOpenSleepTimer != null) {
                        IconButton(onClick = onOpenSleepTimer, modifier = Modifier.iconButtonShadow()) {
                            Icon(
                                Icons.Rounded.Nightlight,
                                contentDescription = "Sleep Timer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(onClick = onAdd, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Album Art Area with Swipe Gestures and Glowing Pulse Effect
            var totalDragX by remember { mutableFloatStateOf(0f) }
            var totalDragY by remember { mutableFloatStateOf(0f) }

            AnimatedContent(
                targetState = a,
                transitionSpec = {
                    val duration = 380
                    val enter = if (animDir > 0) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 0.9f, animationSpec = tween(duration))
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 1.1f, animationSpec = tween(duration))
                    }
                    val exit = if (animDir > 0) {
                        slideOutHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(duration))
                    } else {
                        slideOutHorizontally(animationSpec = tween(duration)) { it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(duration))
                    }
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "song_transition"
            ) { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.pointerInput(c.id) {
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDragEnd = {
                                    if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 100f) {
                                        if (totalDragX < 0) onNext() else onPrev()
                                    } else if (totalDragY > 140f) {
                                        onCollapse()
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                }
                            )
                        }
                    ) {
                        // Ambient Glowing Aura
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(330.dp)
                                    .scale(pulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                animatedVibrant.copy(alpha = pulseAlpha),
                                                animatedDominant.copy(alpha = pulseAlpha * 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                        }

                        // Album Art Card (Increased size: 290.dp for immersive full-screen view)
                        Card(
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(20.dp),
                            modifier = Modifier
                                .size(290.dp)
                                .cardShadow()
                        ) {
                            AlbumArtImage(c, Modifier.fillMaxSize(), true)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        c.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        c.artist,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    // Quality / Duration Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${formatTime(c.duration)} ÔÇó HQ AUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
                            )
                        }
                    }
                }
            }

            // Sleek Floating Action Toolbar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFav, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFF4081) else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (lyricsViewModel != null) {
                        IconButton(
                            onClick = { showLyrics = true },
                            modifier = Modifier.iconButtonShadow()
                        ) {
                            Icon(
                                Icons.Rounded.Subtitles,
                                contentDescription = strings.get("lyrics"),
                                tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueueSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showVolumeControl = !showVolumeControl },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            if (currentVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Volume",
                            tint = if (showVolumeControl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSongInfoDialog = true },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${a.title} - ${a.artist}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "­şÄÁ BeatBox'ta dinliyorum:\n\"${a.title}\" - ${a.artist}\n\n#BeatBox #Music"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, strings.get("share_song")))
                        },
                        modifier = Modifier.iconButtonShadow()
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Inline Volume Bar
            AnimatedVisibility(
                visible = showVolumeControl,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (currentVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { vol ->
                                currentVolume = vol.toInt()
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${((currentVolume.toFloat() / maxVolume) * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Real-Time Audio Visualizer Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                MusicVisualizer(isPlaying, visualizerLevel, visualizerWaveform, modifier = Modifier.height(18.dp))
            }

            // Seek Bar Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Text(formatTime(pos.toLong()), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(" / ${formatTime(a.duration)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                    }

                    // Speed Selector
                    Box {
                        TextButton(onClick = { speedMenu = true }, modifier = Modifier.height(30.dp)) {
                            Text(
                                "${currentSpeed}x",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s}x", fontWeight = if (s == currentSpeed) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        currentSpeed = s
                                        player.setPlaybackSpeed(s)
                                        speedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Slider(
                    value = pos,
                    onValueChange = {
                        drag = true
                        pos = it
                    },
                    onValueChangeFinished = {
                        player.seekTo(pos.toInt())
                        drag = false
                    },
                    valueRange = 0f..(a.duration.toFloat().takeIf { it > 0 } ?: 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.2f)
                    )
                )
            }

            // Main Playback Controls Capsule
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.12f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onShuffle, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(24.dp),
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }

                    IconButton(onClick = onPrev, modifier = Modifier.size(48.dp).iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp).iconButtonShadow()) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onRepeat, modifier = Modifier.iconButtonShadow()) {
                        Icon(
                            if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            modifier = Modifier.size(24.dp),
                            tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
            }
        }

        // Lyrics Screen Overlay with smooth slide-up & fade animation
        AnimatedVisibility(
            visible = showLyrics && lyricsViewModel != null,
            enter = slideInVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(250, easing = FastOutLinearInEasing)) { it } + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (lyricsViewModel != null) {
                LyricsScreen(
                    audio = a,
                    currentPositionMs = pos.toLong(),
                    isPlaying = isPlaying,
                    viewModel = lyricsViewModel,
                    strings = strings,
                    onSeekTo = { ms -> player.seekTo(ms.toInt()) },
                    onDismiss = { showLyrics = false }
                )
            }
        }

        // Queue / Up Next Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.get("now_playing_queue"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentPlaylist.size} ${strings.get("songs")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                            }
                        }

                        IconButton(onClick = { showQueueSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (currentPlaylist.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                strings.get("empty_library"),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(currentPlaylist, key = { it.id }) { item ->
                                val isCurrent = item.id == a.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectFromQueue?.invoke(item)
                                            showQueueSheet = false
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent)
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.4f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AlbumArtImage(item, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.title,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                item.artist,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCurrent && isPlaying) {
                                            MusicVisualizer(true, visualizerLevel, visualizerWaveform, modifier = Modifier.height(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Song Info Dialog
        if (showSongInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSongInfoDialog = false },
                confirmButton = {
                    Button(onClick = { showSongInfoDialog = false }, modifier = Modifier.buttonShadow()) {
                        Text(strings.get("ok"))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(strings.get("song_info"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoDetailRow(strings.get("track_name"), a.title)
                        InfoDetailRow(strings.get("artist"), a.artist)
                        InfoDetailRow(strings.get("duration"), formatTime(a.duration))
                        if (a.folderName.isNotBlank()) {
                            InfoDetailRow(strings.get("folder"), a.folderName)
                        }
                    }
                }
            )
        }
    }
}

}
@Composable
private fun InfoDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

