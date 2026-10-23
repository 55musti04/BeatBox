package com.blackstack.beatbox

import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.blackstack.beatbox.ui.theme.BeatBoxTheme
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sqrt
import kotlin.math.abs

class MainActivity : ComponentActivity(), SensorEventListener {
    var musicService: MediaPlaybackService? = null
    private var serviceBound = false
    // Thread-safe state management
    var currentAudio = mutableStateOf<AudioFile?>(null)
    var isPlaying = mutableStateOf(false)

    // Constants for shake detection
    private companion object {
        private const val SHAKE_THRESHOLD = 51f
        private const val SHAKE_DELAY_MS = 3000L
        private const val SLEEP_TIMER_INTERVAL_MS = 60000L
        private const val FADE_STEP = 0.05f
        private const val FADE_DELAY_MS = 1500L
        private const val DELETE_REQUEST_CODE = 1001
    }
    
    var onSongComplete: (() -> Unit)? = null
    var uiActionPlayNext: (() -> Unit)? = null
    var uiActionPlayPrev: (() -> Unit)? = null
    var uiActionPlayPause: (() -> Unit)? = null
    lateinit var prefs: SharedPreferences
    var sleepTimerMinutes by mutableIntStateOf(0)
    var sleepTimerSeconds by mutableIntStateOf(0)
    @Volatile var isSleepTimerRunning = false
    private var fadeJob: Job? = null
    private var sleepTimerJob: Job? = null
    private lateinit var sensorManager: SensorManager
    private var acceleration = 10f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var lastShakeTime = 0L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MediaPlaybackService.MusicBinder).getService()
            serviceBound = true
            // Sync Initial State
            currentAudio.value = musicService?.currentAudio
            isPlaying.value = musicService?.isPlaying() == true

            musicService?.onSongComplete = { onSongComplete?.invoke() }
            musicService?.onPrevRequested = { uiActionPlayPrev?.invoke() }
            musicService?.onPlaybackStateChanged = { isPlaying.value = it }
            musicService?.onAudioChanged = { currentAudio.value = it }
        }
        override fun onServiceDisconnected(name: ComponentName?) { serviceBound = false; isPlaying.value = false }
    }

    fun deleteSong(audio: AudioFile): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+) - Use MediaStore API with user permission
                val uris = listOf(audio.uri)
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                
                // This will show system dialog to user for confirmation
                try {
                    startIntentSenderForResult(
                        pendingIntent.intentSender,
                        DELETE_REQUEST_CODE,
                        null, 0, 0, 0, null
                    )
                    // Return true - actual deletion happens in onActivityResult
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                // Android 10 and below - Direct deletion
                val rows = contentResolver.delete(audio.uri, null, null)
                if (rows > 0) {
                    // Remove from favorites if exists
                    val f = getFavorites().toMutableSet()
                    if (f.contains(audio.id.toString())) {
                        f.remove(audio.id.toString())
                        prefs.edit { putStringSet("favorite_ids", f) }
                    }
                    true
                } else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val msg = throwable.message ?: ""
            if (msg.contains("ACTION_HOVER_EXIT") || (msg.contains("MotionEvent") && msg.contains("hover"))) {
                Log.e("UncaughtException", "Suppressed Compose hover exit framework bug", throwable)
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        
        // StrictMode: In debug builds, log VM leaks without hiding ANRs by permitting network on main thread
        if (BuildConfig.DEBUG) {
            try {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .build()
                )
            } catch (e: Exception) {
                // Ignore if StrictMode configuration fails
            }
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // Enable High Refresh Rate (90Hz / 120Hz)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else @Suppress("DEPRECATION") windowManager.defaultDisplay
                val modes = currentDisplay?.supportedModes
                val bestMode = modes?.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = bestMode.modeId
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to configure high refresh rate", e)
        }

        prefs = getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("keep_screen_on", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        Intent(this, MediaPlaybackService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        // Start sleep timer with proper lifecycle management
        startSleepTimer()
        
        setContent { BeatBoxRoot() }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit { putBoolean("keep_screen_on", enabled) }
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun startSleepTimer() {
        sleepTimerJob?.cancel() // Cancel any existing timer
        sleepTimerJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000) // 1 second interval
                
                if (isSleepTimerRunning) {
                    var timerFinished = false
                    withContext(Dispatchers.Main) {
                        if (sleepTimerMinutes > 0 || sleepTimerSeconds > 0) {
                            if (sleepTimerSeconds > 0) {
                                sleepTimerSeconds--
                            } else if (sleepTimerMinutes > 0) {
                                sleepTimerMinutes--
                                sleepTimerSeconds = 59
                            }
                            if (sleepTimerMinutes <= 0 && sleepTimerSeconds <= 0) {
                                timerFinished = true
                            }
                        }
                    }
                    
                    if (timerFinished) {
                        fadeJob?.cancel()
                        fadeJob = launch(Dispatchers.Main) { // Fade on Main thread for UI updates
                            var v = 1f
                            while (v > 0 && isActive) { 
                                v -= FADE_STEP
                                musicService?.setVolume(v.coerceAtLeast(0f))
                                delay(FADE_DELAY_MS) 
                            }
                            if (isActive) {
                                pauseAudio(null)
                                musicService?.setVolume(1f)
                                isSleepTimerRunning = false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && 
            event.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM &&
            prefs.getBoolean("shake_to_shuffle", true)) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt(x * x + y * y + z * z)
            acceleration = acceleration * 0.9f + abs(currentAcceleration - lastAcceleration)
            if (acceleration > SHAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > SHAKE_DELAY_MS) {
                    lastShakeTime = now
                    uiActionPlayNext?.invoke()
                    lifecycleScope.launch(Dispatchers.Main) {
                        val l = try { 
                            AppLanguage.valueOf(prefs.getString("app_lang", "TR") ?: "TR") 
                        } catch(e: Exception) { 
                            AppLanguage.TR 
                        }
                        Toast.makeText(this@MainActivity, Strings(l).get("shake_toast"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    private fun BeatBoxRoot() {
        var visualizerLevel by remember { mutableFloatStateOf(0f) }
        var visualizerWaveform by remember { mutableStateOf(FloatArray(15) { 0f }) }
        
        // Optimized polling: Only polls when playing, saving CPU & eliminating background lag
        LaunchedEffect(isPlaying.value) {
            if (isPlaying.value) {
                withContext(Dispatchers.Default) {
                    while (isActive && isPlaying.value) {
                        try {
                            val level = musicService?.getCurrentLevel() ?: 0f
                            val waveform = musicService?.getCurrentWaveform() ?: FloatArray(15) { 0f }
                            withContext(Dispatchers.Main) {
                                visualizerLevel = level
                                visualizerWaveform = waveform
                            }
                        } catch (e: Exception) {
                            // Ignore errors silently
                        }
                        delay(120) // ~8 FPS, ultra smooth & zero lag
                    }
                }
            } else {
                visualizerLevel = 0f
                visualizerWaveform = FloatArray(15) { 0f }
            }
        }

        var currentTheme by remember { 
            mutableStateOf(
                try {
                    AppTheme.valueOf(prefs.getString("app_theme", "DYNAMIC") ?: "DYNAMIC")
                } catch (e: Exception) {
                    AppTheme.DYNAMIC
                }
            )
        }
        
        // Detect system language and set as default
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val systemLang = remember(configuration) {
            val systemLocale = configuration.locales[0].language
            
            // Map system language to AppLanguage
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
                else -> AppLanguage.EN // Default to English if language not supported
            }
        }
        
        var lang by remember { 
            mutableStateOf(
                try { 
                    val savedLang = prefs.getString("app_lang", null)
                    if (savedLang != null) {
                        AppLanguage.valueOf(savedLang)
                    } else {
                        // First launch - use system language
                        systemLang
                    }
                } catch (e: Exception) { 
                    systemLang
                }
            )
        }
        val s = remember(lang) { Strings(lang) }
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as android.app.Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !currentTheme.isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !currentTheme.isDark
            }
        }
        val currAudio by currentAudio
        var dynamicPrimary by remember { mutableStateOf(Color(0xFF00E5FF)) }

        LaunchedEffect(currAudio?.id) {
            val audio = currAudio
            if (audio != null) {
                withContext(Dispatchers.IO) {
                    try {
                        AlbumArtCache.bitmapCache.get(audio.id)?.let { bmp ->
                            val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                            val dom = palette.getDominantColor("#00E5FF".toColorInt())
                            withContext(Dispatchers.Main) {
                                dynamicPrimary = Color(dom)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BeatBoxTheme(
            theme = currentTheme,
            dynamicPrimary = if (currentTheme == AppTheme.DYNAMIC) dynamicPrimary else null
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BeatBoxApp(this@MainActivity, currentTheme, s, lang, visualizerLevel, visualizerWaveform,
                    { currentTheme = it; prefs.edit { putString("app_theme", it.name) } },
                    { lang = it; prefs.edit { putString("app_lang", it.name) } })
            }
        }
    }

    fun playAudio(audio: AudioFile) { 
        android.util.Log.d("MainActivity", "playAudio called for: ${audio.title}")
        android.util.Log.d("MainActivity", "musicService is null: ${musicService == null}")
        android.util.Log.d("MainActivity", "serviceBound: $serviceBound")
        musicService?.play(audio, prefs.getBoolean("crossfade_enabled", true)) 
    }
    fun pauseAudio(audio: AudioFile?) { musicService?.pause() }
    fun resumeAudio(audio: AudioFile?) { musicService?.resume() }
    fun seekTo(position: Int) { musicService?.seekTo(position) }
    fun getCurrentPosition(): Int = musicService?.getCurrentPosition() ?: 0
    fun getDuration(): Int = musicService?.getDuration() ?: 0
    fun isPlaying(): Boolean = musicService?.isPlaying() == true
    fun setEQPreset(preset: EQPreset) { musicService?.applyPreset(preset) }
    fun getCurrentEQPreset(): EQPreset = musicService?.currentPreset ?: EQPreset.NONE
    fun setPlaybackSpeed(speed: Float) { musicService?.setSpeed(speed) }
    fun loadOnlineTracks(): List<AudioFile> = OnlineAudioManager.loadOnlineTracks(this)
    fun saveOnlineTrack(title: String, artist: String, url: String, artworkUrl: String?, durationMs: Long = 0L): AudioFile =
        OnlineAudioManager.saveOnlineTrack(this, title, artist, url, artworkUrl, durationMs)
    fun downloadTrack(audioFile: AudioFile, onResult: (Boolean, String) -> Unit): Unit =
        run { OnlineAudioManager.downloadTrack(this, audioFile, lifecycleScope, onResult) }
    fun toggleFavorite(audioId: Long) {
        val favs = getFavorites().toMutableSet()
        if (favs.contains(audioId.toString())) favs.remove(audioId.toString()) else favs.add(audioId.toString())
        prefs.edit { putStringSet("favorite_ids", favs) }
    }
    fun getFavorites(): Set<String> = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    fun isFavorite(audioId: Long): Boolean = getFavorites().contains(audioId.toString())
    fun saveSelectedFolders(folders: Set<String>) { prefs.edit { putStringSet("selected_folders", folders) } }
    fun loadSelectedFolders(): Set<String> = prefs.getStringSet("selected_folders", emptySet()) ?: emptySet()
    fun saveMinDurationSetting(seconds: Int) { prefs.edit { putInt("min_duration", seconds) } }
    fun loadMinDurationSetting(): Int = prefs.getInt("min_duration", 30)
    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (p in playlists) {
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("isSmart", p.isSmart)
                obj.put("isArtistPlaylist", p.isArtistPlaylist)
                if (p.artistName != null) {
                    obj.put("artistName", p.artistName)
                }
                val songIdsArray = org.json.JSONArray()
                for (id in p.songIds) {
                    songIdsArray.put(id)
                }
                obj.put("songIds", songIdsArray)
                jsonArray.put(obj)
            }
            prefs.edit { putString("playlists_v3", jsonArray.toString(), ) }
        } catch (e: Exception) {
            android.util.Log.e("PlaylistSave", "Error saving playlists", e)
        }
    }
    
    fun loadPlaylists(): List<Playlist> {
        return try {
            val v3Str = prefs.getString("playlists_v3", null)
            if (!v3Str.isNullOrEmpty()) {
                val jsonArray = org.json.JSONArray(v3Str)
                val list = mutableListOf<Playlist>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    val name = obj.optString("name", "")
                    val isSmart = obj.optBoolean("isSmart", false)
                    val isArtistPlaylist = obj.optBoolean("isArtistPlaylist", false)
                    val artistName = if (obj.has("artistName")) obj.optString("artistName").ifBlank { null } else null
                    val songIdsArray = obj.optJSONArray("songIds")
                    val songIds = mutableListOf<Long>()
                    if (songIdsArray != null) {
                        for (j in 0 until songIdsArray.length()) {
                            songIds.add(songIdsArray.getLong(j))
                        }
                    }
                    list.add(Playlist(name, id, songIds, isSmart, isArtistPlaylist, artistName))
                }
                return list
            }

            // Fallback to legacy playlists_v2 delimiter format
            val playlistsStr = prefs.getString("playlists_v2", "") ?: ""
            if (playlistsStr.isEmpty()) return emptyList()
            
            playlistsStr.split("|||").mapNotNull { playlistStr ->
                try {
                    val parts = playlistStr.split("::")
                    if (parts.size >= 4) {
                        val id = parts[0]
                        val name = parts[1]
                        val isSmart = parts[2].toBooleanStrictOrNull() ?: false
                        val songIds = if (parts[3].isEmpty()) {
                            mutableListOf()
                        } else {
                            parts[3].split(",").mapNotNull { idStr ->
                                idStr.toLongOrNull()
                            }.toMutableList()
                        }
                        val isArtistPlaylist = parts.getOrNull(4)?.toBooleanStrictOrNull() ?: false
                        val artistName = parts.getOrNull(5)?.ifBlank { null }
                        Playlist(name, id, songIds, isSmart, isArtistPlaylist, artistName)
                    } else {
                        android.util.Log.w("PlaylistLoad", "Invalid playlist format: $playlistStr")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PlaylistLoad", "Error parsing playlist: $playlistStr", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaylistLoad", "Error loading playlists", e)
            emptyList()
        }
    }
    override fun onPause() {
        super.onPause()
        // Pause visualizer to save battery when app is in background
        musicService?.pauseVisualizer()
    }
    
    override fun onResume() {
        super.onResume()
        // Resume visualizer when app comes to foreground
        musicService?.resumeVisualizer()
        
        if (prefs.getBoolean("keep_screen_on", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // File was successfully deleted by user
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Şarkı silindi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // User cancelled the deletion
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Silme işlemi iptal edildi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines
        fadeJob?.cancel()
        sleepTimerJob?.cancel()
        
        // Clear service callbacks to avoid leaking the Activity
        try {
            musicService?.onSongComplete = null
            musicService?.onPlaybackStateChanged = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Unbind service safely
        if (serviceBound) { 
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            serviceBound = false 
        }
        
        // Unregister sensor listener
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this).generate()
    val dominant = palette.getVibrantColor(palette.getDominantColor(android.graphics.Color.DKGRAY))
    return Color(dominant)
}
