package com.blackstack.beatbox.ui.components

import com.blackstack.beatbox.*
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Uygulamanın üst çubuğu, alt navigasyonu, mini oynatıcısı ve FAB'ı.
 *
 * Görsel yapı önceki `BeatBoxApp` gövdesiyle birebir aynıdır; yalnızca durum
 * ve eylemler parametre olarak dışarıdan verilir. [content] Scaffold'un içerik
 * alanını (izin ekranı, sekmeler) alır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatBoxScaffold(
    player: MainActivity,
    strings: Strings,
    ctx: Context,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    speechLauncher: ActivityResultLauncher<Intent>,
    tempoViz: Boolean,
    isPlaying: Boolean,
    visualizerLevel: Float,
    visualizerWaveform: FloatArray,
    userProfile: UserProfile,
    avatarIcons: List<ImageVector>,
    currentAudio: AudioFile?,
    showPlayer: Boolean,
    showFab: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenPlayer: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenEQ: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var showSortMenu by remember { mutableStateOf(false) }

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
                                onClick = onOpenEQ,
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
                                onClick = onOpenSleepTimer,
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
                                onClick = onProfileClick,
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
                                onClick = onOpenSettings,
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
                                    text = String.format(locale, "%02d:%02d", player.sleepTimerMinutes, player.sleepTimerSeconds),
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
                        onValueChange = onSearchQueryChange,
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
                            DropdownMenuItem(text = { Text(strings.get("sort_date")) }, onClick = { onSortOptionChange(SortOption.DATE); showSortMenu = false })
                            DropdownMenuItem(text = { Text(strings.get("sort_name")) }, onClick = { onSortOptionChange(SortOption.NAME); showSortMenu = false })
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
                .navigationBarsPadding() // System navigation bar iÃ§in padding
        ) {
            // Mini player bar above navigation
            if (!showPlayer) {
                MiniPlayerBar(currentAudio, isPlaying, { if (currentAudio != null) onOpenPlayer() }, onPlayPause, onNext, onPrev)
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
                        onClick = { onTabSelected(0) },
                        icon = Icons.Rounded.Home,
                        label = strings.get("home"),
                        modifier = Modifier.weight(1f)
                    )
                    ModernBottomNavigationItem(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = strings.get("playlists"),
                        modifier = Modifier.weight(1f)
                    )
                    ModernBottomNavigationItem(
                        selected = selectedTab == 2,
                        onClick = { onTabSelected(2) },
                        icon = Icons.Rounded.LibraryMusic,
                        label = strings.get("library"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = onCreatePlaylist,
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fabShadow()
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        content = content
    )
}
