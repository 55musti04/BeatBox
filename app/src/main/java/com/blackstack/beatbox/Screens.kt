package com.blackstack.beatbox

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.blackstack.beatbox.ui.components.AuroraBackground
import com.blackstack.beatbox.ui.components.BeatBoxOverlayState
import com.blackstack.beatbox.ui.components.BeatBoxOverlays
import com.blackstack.beatbox.ui.components.BeatBoxScaffold
import com.blackstack.beatbox.ui.components.BeatBoxSkeleton
import com.blackstack.beatbox.ui.components.BeatBoxTabContent
import com.blackstack.beatbox.ui.components.SplashScreen
import com.blackstack.beatbox.ui.components.nextSongInQueue
import com.blackstack.beatbox.ui.components.previousSongInQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val overlays = remember { BeatBoxOverlayState() }
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(prefs.getBoolean("first_launch", true)) }

    if (showSplash) { SplashScreen(currentTheme) { showSplash = false }; return }
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
            val (loaded, folders) = withContext(Dispatchers.IO) {
                loadAudioFilesAndFolders(ctx, minDurationSeconds, selectedFolders)
            }
            // Daha önce çözülen sanatçı ve kapakları hemen uygula; ardından
            // eksik kalanları arka planda çevrim içi tamamla.
            val enriched = SongMetadataRepository.applyCached(ctx, loaded)
            allAudioList = enriched
            availableFolders = folders
            isLoading = false
            SongMetadataRepository.enrichUnknownArtists(ctx, enriched) { id, artist, artworkPath ->
                allAudioList = SongMetadataRepository.applyResolved(allAudioList, id, artist, artworkPath)
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

    // Kitaplık veya çevrim içi bilgiler güncellendiğinde çalan şarkıyı ve
    // kuyruğu aynı zenginleştirilmiş meta verilerle tazele.
    LaunchedEffect(allAudioList) {
        val byId = allAudioList.associateBy { it.id }
        currentAudio = currentAudio?.let { byId[it.id] ?: it }
        if (currentPlaylist.isNotEmpty()) currentPlaylist = currentPlaylist.map { byId[it.id] ?: it }
    }

    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var currentEQPreset by remember { mutableStateOf(player.getCurrentEQPreset()) }
    var animationDirection by remember { mutableIntStateOf(1) }
    var sortOption by remember { mutableStateOf(SortOption.DATE) }
    var favoriteIds by remember { mutableStateOf(player.getFavorites()) }
    var userProfile by remember { mutableStateOf(loadUserProfile(prefs)) }

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

    val playNext: () -> Unit = {
        val listToUse = if (currentPlaylist.isNotEmpty()) currentPlaylist else allAudioList
        nextSongInQueue(listToUse, currentAudio?.id, isShuffle)?.let { next ->
            animationDirection = 1
            currentAudio = next
            player.playAudio(next)
        }
    }

    val playPrev: () -> Unit = {
        val listToUse = if (currentPlaylist.isNotEmpty()) currentPlaylist else allAudioList
        previousSongInQueue(listToUse, currentAudio?.id, isShuffle)?.let { prev ->
            animationDirection = -1
            currentAudio = prev
            player.playAudio(prev)
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

    BackHandler(overlays.showPlayer || overlays.playlistDetail != null || overlays.showSettings) {
        if (overlays.showPlayer) overlays.showPlayer = false
        else if (overlays.playlistDetail != null) overlays.playlistDetail = null
        else if (overlays.showSettings) overlays.showSettings = false
    }

    var compactTopBarVisible by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(currentTheme, Modifier.fillMaxSize()) {
            // Tam ekran katman açıkken alttaki scaffold TalkBack tarafından
            // odaklanmaz; aksi halde görünmeyen üst çubuk okunabilir.
            Box(
                modifier = if (
                    overlays.playlistDetail != null ||
                    overlays.showPlayer ||
                    overlays.showSettings
                ) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier
                }
            ) {
                BeatBoxScaffold(
                    player = player,
                    strings = strings,
                    ctx = ctx,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    sortOption = sortOption,
                    onSortOptionChange = { sortOption = it },
                    speechLauncher = speechLauncher,
                    tempoViz = tempoViz,
                    isPlaying = isPlaying,
                    visualizerLevel = visualizerLevel,
                    visualizerWaveform = visualizerWaveform,
                    userProfile = userProfile,
                    avatarIcons = avatarIcons,
                    currentAudio = currentAudio,
                    showPlayer = overlays.showPlayer,
                    showFab = selectedTab == 1 && !overlays.showPlayer && overlays.playlistDetail == null,
                    onPlayPause = togglePlayPause,
                    onNext = playNext,
                    onPrev = playPrev,
                    onOpenPlayer = { overlays.showPlayer = true },
                    onCreatePlaylist = { overlays.showCreatePlaylist = true },
                    onOpenEQ = { overlays.showEQ = true },
                    onOpenSleepTimer = { overlays.showSleepTimer = true },
                    onProfileClick = { if (userProfile.isLoggedIn) overlays.showProfile = true else overlays.showAuth = true },
                    onOpenSettings = { overlays.showSettings = true },
                    transparentTopBar = selectedTab == 0,
                    showCompactTopBar = selectedTab == 0 && compactTopBarVisible,
                    onCompactProfileClick = { if (userProfile.isLoggedIn) overlays.showProfile = true else overlays.showAuth = true },
                    onCompactSettingsClick = { overlays.showSettings = true }
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
                            BeatBoxSkeleton(Modifier.fillMaxSize())
                        } else {
                            BeatBoxTabContent(
                                selectedTab = selectedTab,
                                allAudioList = allAudioList,
                                favoriteIds = favoriteIds,
                                playlists = playlists,
                                currentAudio = currentAudio,
                                currentPlaylist = currentPlaylist,
                                isPlaying = isPlaying,
                                searchQuery = searchQuery,
                                sortOption = sortOption,
                                visualizerLevel = visualizerLevel,
                                visualizerWaveform = visualizerWaveform,
                                strings = strings,
                                ctx = ctx,
                                prefs = prefs,
                                player = player,
                                overlays = overlays,
                                onCurrentPlaylistChange = { currentPlaylist = it },
                                onCurrentAudioChange = { currentAudio = it },
                                onFavoriteIdsChange = { favoriteIds = it },
                                onPlaylistsChange = { list ->
                                    playlists = list
                                    player.savePlaylists(list)
                                },
                                onHomeCompactBarVisibleChange = { compactTopBarVisible = it }
                            )
                        }
                    }
                }
            }

        BeatBoxOverlays(
            overlays = overlays,
            player = player,
            strings = strings,
            ctx = ctx,
            prefs = prefs,
            allAudioList = allAudioList,
            playlists = playlists,
            selectedFolders = selectedFolders,
            availableFolders = availableFolders,
            currentTheme = currentTheme,
            currentLang = currentLang,
            onThemeChange = onThemeChange,
            onLangChange = onLangChange,
            tempoViz = tempoViz,
            onTempoVizChange = { tempoViz = it },
            micLauncher = micLauncher,
            userProfile = userProfile,
            onUserProfileChange = { userProfile = it },
            avatarIcons = avatarIcons,
            currentAudio = currentAudio,
            currentPlaylist = currentPlaylist,
            isPlaying = isPlaying,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            animationDirection = animationDirection,
            favoriteIds = favoriteIds,
            currentEQPreset = currentEQPreset,
            onEQPresetChange = { currentEQPreset = it },
            visualizerLevel = visualizerLevel,
            visualizerWaveform = visualizerWaveform,
            onPlayPause = togglePlayPause,
            onNext = playNext,
            onPrev = playPrev,
            onShuffleToggle = {
                isShuffle = !isShuffle
                val msg = if (isShuffle) strings.get("shuffle_on") else strings.get("shuffle_off")
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            },
            onRepeatCycle = {
                repeatMode = (repeatMode + 1) % 3
                val msg = when (repeatMode) {
                    1 -> strings.get("repeat_all")
                    2 -> strings.get("repeat_one")
                    else -> strings.get("repeat_off")
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            },
            onSelectFromQueue = { song ->
                currentAudio = song
                player.playAudio(song)
                updateSongStats(prefs, song)
            },
            onToggleFavorite = { a -> player.toggleFavorite(a.id); favoriteIds = player.getFavorites() },
            onPlaylistsChange = { list -> playlists = list; player.savePlaylists(list) },
            onAddToPlaylist = { playlist, song ->
                playlist.songIds.add(song.id)
                player.savePlaylists(playlists)
            },
            onSelectedFoldersChange = { folders ->
                selectedFolders = folders
                player.saveSelectedFolders(folders)
            },
            onReloadLibrary = { reloadLibrary() },
            onDeleteSong = { song ->
                if (player.deleteSong(song)) {
                    allAudioList = allAudioList.filter { it.id != song.id }
                    favoriteIds = player.getFavorites()
                }
            },
            onPlaySong = { song, list ->
                if (currentPlaylist != list) currentPlaylist = list
                currentAudio = song
                player.playAudio(song)
            },
            onPlayPlaylist = { songs ->
                currentPlaylist = songs
                currentAudio = songs.first()
                player.playAudio(songs.first())
                updateSongStats(prefs, songs.first())
            },
            onUpdateSongStats = { updateSongStats(prefs, it) }
        )
    }
}
}

