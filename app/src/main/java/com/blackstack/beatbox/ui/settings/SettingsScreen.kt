package com.blackstack.beatbox.ui.settings

import com.blackstack.beatbox.*
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Ayarlar ekranı.
 *
 * Tema ve hakkında diyalogları bu ekranın yerel durumudur; kullanıcı profili,
 * klasör seçimi ve istatistikler ise uygulama kabuğundan yönetilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    strings: Strings,
    userProfile: UserProfile,
    avatarIcons: List<ImageVector>,
    currentTheme: AppTheme,
    currentLang: AppLanguage,
    onThemeChange: (AppTheme) -> Unit,
    onLangChange: (AppLanguage) -> Unit,
    tempoViz: Boolean,
    onTempoVizChange: (Boolean) -> Unit,
    prefs: SharedPreferences,
    ctx: Context,
    player: MainActivity,
    micLauncher: ActivityResultLauncher<String>,
    onProfileClick: () -> Unit,
    onOpenFolderDialog: () -> Unit,
    onOpenWrappedDialog: () -> Unit,
    onOpenHistoryDialog: () -> Unit,
    onClose: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding()) {
            TopAppBar(
                title = { Text(strings.get("settings"), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { onClose() }, modifier = Modifier.iconButtonShadow()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(Color.Transparent)
            )
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onProfileClick()
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
                                onTempoVizChange(true)
                                prefs.edit { putBoolean("tempo_visualization", true) }
                            }
                        } else {
                            onTempoVizChange(false)
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
                            onTempoVizChange(false)
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
                item { SettingsClickItem(Icons.Rounded.Analytics, strings.get("wrapped_title"), strings.get("wrapped_desc")) { onOpenWrappedDialog() } }
                item { SettingsClickItem(Icons.Rounded.History, strings.get("listening_history"), strings.get("history_desc")) { onOpenHistoryDialog() } }
                item {
                    SettingsClickItem(
                        Icons.Rounded.CleaningServices,
                        strings.get("clear_cache"),
                        "RAM ve görsel önbelleği"
                    ) {
                        ArtistImageProvider.bitmapCache.evictAll()
                        AlbumArtCache.bitmapCache.evictAll()
                        Toast.makeText(ctx, strings.get("cache_cleared"), Toast.LENGTH_SHORT).show()
                    }
                }
                item { Spacer(Modifier.height(16.dp)); SettingsHeader(strings.get("general")) }
                item { SettingsClickItem(Icons.Rounded.Folder, strings.get("folders")) { onOpenFolderDialog() } }
                item {
                    var autoMetadata by remember {
                        mutableStateOf(prefs.getBoolean("auto_song_metadata", true))
                    }
                    SettingsToggleItem(
                        Icons.Rounded.AutoAwesome,
                        strings.get("auto_song_metadata"),
                        autoMetadata
                    ) { enabled ->
                        autoMetadata = enabled
                        prefs.edit { putBoolean("auto_song_metadata", enabled) }
                    }
                }
                item { SettingsClickItem(Icons.Rounded.Info, strings.get("about"), "BeatBox v3.0") { showAboutDialog = true } }
            }
        }
    }

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
}
