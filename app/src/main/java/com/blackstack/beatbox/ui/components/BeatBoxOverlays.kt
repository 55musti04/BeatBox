package com.blackstack.beatbox.ui.components

import com.blackstack.beatbox.*
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.blackstack.beatbox.ui.dialogs.AddToPlaylistDialog
import com.blackstack.beatbox.ui.dialogs.CreatePlaylistDialog
import com.blackstack.beatbox.ui.dialogs.DeleteSongDialog
import com.blackstack.beatbox.ui.dialogs.EQDialog
import com.blackstack.beatbox.ui.dialogs.EditPlaylistDialog
import com.blackstack.beatbox.ui.dialogs.FolderDialog
import com.blackstack.beatbox.ui.dialogs.HistoryDialog
import com.blackstack.beatbox.ui.dialogs.SleepTimerDialog
import com.blackstack.beatbox.ui.dialogs.WrappedDialog
import com.blackstack.beatbox.ui.player.FullScreenPlayer
import com.blackstack.beatbox.ui.player.PlaylistDetailScreen
import com.blackstack.beatbox.ui.settings.SettingsScreen

/**
 * Scaffold'un üzerine açılan tüm ekran ve diyaloglar.
 *
 * Görünürlük [overlays] içinde tutulur; veri ve eylemler uygulama kabuğundan
 * parametre olarak gelir. Böylece `BeatBoxApp` yalnızca durum makinesi olarak
 * kalır ve bu dosya bağımsız olarak geliştirilebilir.
 */
@Composable
fun BeatBoxOverlays(
    overlays: BeatBoxOverlayState,
    player: MainActivity,
    strings: Strings,
    ctx: Context,
    prefs: SharedPreferences,
    allAudioList: List<AudioFile>,
    playlists: List<Playlist>,
    selectedFolders: Set<String>,
    availableFolders: List<String>,
    currentTheme: AppTheme,
    currentLang: AppLanguage,
    onThemeChange: (AppTheme) -> Unit,
    onLangChange: (AppLanguage) -> Unit,
    tempoViz: Boolean,
    onTempoVizChange: (Boolean) -> Unit,
    micLauncher: ActivityResultLauncher<String>,
    userProfile: UserProfile,
    onUserProfileChange: (UserProfile) -> Unit,
    avatarIcons: List<ImageVector>,
    currentAudio: AudioFile?,
    currentPlaylist: List<AudioFile>,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    animationDirection: Int,
    favoriteIds: Set<String>,
    currentEQPreset: EQPreset,
    onEQPresetChange: (EQPreset) -> Unit,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatCycle: () -> Unit,
    onSelectFromQueue: (AudioFile) -> Unit,
    onToggleFavorite: (AudioFile) -> Unit,
    onPlaylistsChange: (List<Playlist>) -> Unit,
    onAddToPlaylist: (Playlist, AudioFile) -> Unit,
    onSelectedFoldersChange: (Set<String>) -> Unit,
    onReloadLibrary: () -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onPlaySong: (AudioFile, List<AudioFile>) -> Unit,
    onPlayPlaylist: (List<AudioFile>) -> Unit,
    onUpdateSongStats: (AudioFile) -> Unit
) {
        // Settings Screen
        AnimatedVisibility(overlays.showSettings, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
            SettingsScreen(
                strings = strings,
                userProfile = userProfile,
                avatarIcons = avatarIcons,
                currentTheme = currentTheme,
                currentLang = currentLang,
                onThemeChange = onThemeChange,
                onLangChange = onLangChange,
                tempoViz = tempoViz,
                onTempoVizChange = onTempoVizChange,
                prefs = prefs,
                ctx = ctx,
                player = player,
                micLauncher = micLauncher,
                onProfileClick = { if (userProfile.isLoggedIn) overlays.showProfile = true else overlays.showAuth = true },
                onOpenFolderDialog = { overlays.showFolder = true },
                onOpenWrappedDialog = { overlays.showWrapped = true },
                onOpenHistoryDialog = { overlays.showHistory = true },
                onClose = { overlays.showSettings = false }
            )
        }

        // EQ Menu Dialog - Modern Design
        if (overlays.showEQ) {
            EQDialog(
                strings = strings,
                currentEQPreset = currentEQPreset,
                visualizerWaveform = visualizerWaveform,
                onPresetSelected = { preset ->
                    player.setEQPreset(preset)
                    onEQPresetChange(preset)
                },
                onDismiss = { overlays.showEQ = false }
            )
        }

        // Playlist View
        overlays.playlistDetail?.let { pl ->
            PlaylistDetailScreen(
                playlist = pl,
                allAudioList = allAudioList,
                strings = strings,
                currentAudioId = currentAudio?.id,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                onBack = { overlays.playlistDetail = null },
                onPlayAll = { songs ->
                    onPlayPlaylist(songs)
                    overlays.showPlayer = true
                },
                onFavClick = { a -> onToggleFavorite(a) },
                onDelete = { overlays.songToDelete = it; overlays.showDelete = true },
                onSongClick = { a, songs -> onPlaySong(a, songs) }
            )
        }

        // Full Screen Player
        AnimatedVisibility(
            overlays.showPlayer,
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
                        onToggleFav = { onToggleFavorite(a) },
                        onCollapse = { overlays.showPlayer = false },
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrev = onPrev,
                        onShuffle = onShuffleToggle,
                        onRepeat = onRepeatCycle,
                        onAdd = { overlays.showAddToPlaylist = true },
                        currentPlaylist = currentPlaylist,
                        onSelectFromQueue = onSelectFromQueue,
                        onOpenEQ = { overlays.showEQ = true },
                        onOpenSleepTimer = { overlays.showSleepTimer = true },
                        visualizerLevel = visualizerLevel,
                        visualizerWaveform = visualizerWaveform
                    )
                }
            }
        }

        // Create Playlist Dialog - Modern & Sweet Design
        if (overlays.showCreatePlaylist) {
            CreatePlaylistDialog(
                strings = strings,
                onCreate = { name -> onPlaylistsChange(playlists + Playlist(name = name)) },
                onDismiss = { overlays.showCreatePlaylist = false }
            )
        }

        // Edit Playlist Dialog
        overlays.editPlaylist?.let { playlist ->
            EditPlaylistDialog(
                strings = strings,
                playlist = playlist,
                onRename = { name ->
                    onPlaylistsChange(
                        playlists.map { p -> if (p.id == playlist.id) p.copy(name = name) else p }
                    )
                },
                onDismiss = { overlays.editPlaylist = null }
            )
        }

        // Add to Playlist Dialog
        if (overlays.showAddToPlaylist && currentAudio != null) {
            AddToPlaylistDialog(
                strings = strings,
                playlists = playlists,
                song = currentAudio!!,
                onAdd = { playlist, song -> onAddToPlaylist(playlist, song) },
                onDismiss = { overlays.showAddToPlaylist = false }
            )
        }

        // Folder Dialog - Modern & Sweet Design
        if (overlays.showFolder) {
            FolderDialog(
                strings = strings,
                selectedFolders = selectedFolders,
                availableFolders = availableFolders,
                onToggleFolder = onSelectedFoldersChange,
                onConfirm = {
                    overlays.showFolder = false
                    onReloadLibrary()
                },
                onDismiss = { overlays.showFolder = false }
            )
        }

        // Sleep Timer Dialog - Redesigned with blur effect
        if (overlays.showSleepTimer) {
            SleepTimerDialog(
                strings = strings,
                onStart = { minutes ->
                    player.sleepTimerMinutes = minutes
                    player.sleepTimerSeconds = 0
                    player.isSleepTimerRunning = true
                },
                onDismiss = { overlays.showSleepTimer = false }
            )
        }

        // Delete Song Dialog
        if (overlays.showDelete && overlays.songToDelete != null) {
            DeleteSongDialog(
                strings = strings,
                song = overlays.songToDelete!!,
                onConfirm = {
                    overlays.songToDelete?.let(onDeleteSong)
                    overlays.showDelete = false
                    overlays.songToDelete = null
                },
                onDismiss = { overlays.showDelete = false; overlays.songToDelete = null }
            )
        }

        // BeatBox Wrapped Dialog
        if (overlays.showWrapped) {
            WrappedDialog(
                strings = strings,
                ctx = ctx,
                prefs = prefs,
                allAudioList = allAudioList,
                onDismiss = { overlays.showWrapped = false }
            )
        }

        // Listening History Dialog
        if (overlays.showHistory) {
            HistoryDialog(
                strings = strings,
                prefs = prefs,
                allAudioList = allAudioList,
                onSongClick = { song, list ->
                    onPlaySong(song, list)
                    onUpdateSongStats(song)
                },
                onDismiss = { overlays.showHistory = false }
            )
        }

        // User Auth Dialog
        if (overlays.showAuth) {
            AuthDialog(
                strings = strings,
                onDismiss = { overlays.showAuth = false },
                onLoginSuccess = { newUser ->
                    onUserProfileChange(newUser)
                    saveUserProfile(prefs, newUser)
                    overlays.showAuth = false
                    Toast.makeText(ctx, "HoÅŸ geldiniz, ${newUser.displayName}!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // User Profile Dialog
        if (overlays.showProfile) {
            ProfileDialog(
                user = userProfile,
                avatarIcons = avatarIcons,
                onDismiss = { overlays.showProfile = false },
                onSaveProfile = { updatedUser ->
                    onUserProfileChange(updatedUser)
                    saveUserProfile(prefs, updatedUser)
                    Toast.makeText(ctx, "Profil gÃ¼ncellendi", Toast.LENGTH_SHORT).show()
                },
                onLogout = {
                    val loggedOut = UserProfile(isLoggedIn = false)
                    onUserProfileChange(loggedOut)
                    saveUserProfile(prefs, loggedOut)
                    Toast.makeText(ctx, "Oturum kapatÄ±ldÄ±", Toast.LENGTH_SHORT).show()
                }
            )
        }
}
