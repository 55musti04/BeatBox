package com.blackstack.beatbox

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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
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
    var isSleepTimerRunning = false
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
                        prefs.edit().putStringSet("favorite_ids", f).apply()
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
        
        // Disable StrictMode for better performance (prevents ANR from disk/network operations)
        try {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .permitDiskReads()
                    .permitDiskWrites()
                    .permitNetwork()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .build()
            )
        } catch (e: Exception) {
            // Ignore if StrictMode configuration fails
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        prefs = getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        Intent(this, MediaPlaybackService::class.java).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(it) else startService(it)
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        // Start sleep timer with proper lifecycle management
        startSleepTimer()
        
        setContent { BeatBoxTheme() }
    }

    private fun startSleepTimer() {
        sleepTimerJob?.cancel() // Cancel any existing timer
        sleepTimerJob = lifecycleScope.launch(Dispatchers.Default) { // Use Default dispatcher instead of Main
            while (isActive) {
                delay(1000) // 1 second interval
                
                // Only process if timer is running
                if (isSleepTimerRunning && (sleepTimerMinutes > 0 || sleepTimerSeconds > 0)) {
                    // Update timer values
                    if (sleepTimerSeconds > 0) {
                        sleepTimerSeconds--
                    } else if (sleepTimerMinutes > 0) {
                        sleepTimerMinutes--
                        sleepTimerSeconds = 59
                    }
                    
                    // Check if timer reached zero
                    if (sleepTimerMinutes <= 0 && sleepTimerSeconds <= 0) {
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
    private fun BeatBoxTheme() {
        var visualizerLevel by remember { mutableFloatStateOf(0f) }
        var visualizerWaveform by remember { mutableStateOf(FloatArray(15) { 0f }) }
        
        // Optimized polling for smooth animation without lag (100ms = 10 FPS)
        // Use Dispatchers.Default to avoid blocking main thread
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) { // ✅ Background thread
                while (isActive) {
                    try {
                        val level = musicService?.getCurrentLevel() ?: 0f
                        val waveform = musicService?.getCurrentWaveform() ?: FloatArray(15) { 0f }
                        
                        // Update state on main thread
                        withContext(Dispatchers.Main) {
                            visualizerLevel = level
                            visualizerWaveform = waveform
                        }
                    } catch (e: Exception) {
                        // Ignore errors silently
                    }
                    delay(100) // Reduced from 60ms to 100ms for better battery life
                }
            }
        }

        var currentTheme by remember { 
            mutableStateOf(
                try {
                    AppTheme.valueOf(prefs.getString("app_theme", "DARK") ?: "DARK")
                } catch (e: Exception) {
                    AppTheme.DARK
                }
            )
        }
        
        // Detect system language and set as default
        val systemLang = remember {
            val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resources.configuration.locales[0].language
            } else {
                @Suppress("DEPRECATION")
                resources.configuration.locale.language
            }
            
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
        MaterialTheme(colorScheme = getThemeColors(currentTheme)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BeatBoxApp(this@MainActivity, currentTheme, s, lang, visualizerLevel, visualizerWaveform,
                    { currentTheme = it; prefs.edit().putString("app_theme", it.name).apply() },
                    { lang = it; prefs.edit().putString("app_lang", it.name).apply() })
            }
        }
    }

    fun playAudio(audio: AudioFile) { musicService?.play(audio, prefs.getBoolean("crossfade_enabled", true)) }
    fun pauseAudio(audio: AudioFile?) { musicService?.pause() }
    fun resumeAudio(audio: AudioFile?) { musicService?.resume() }
    fun seekTo(position: Int) { musicService?.seekTo(position) }
    fun getCurrentPosition(): Int = musicService?.getCurrentPosition() ?: 0
    fun isPlaying(): Boolean = musicService?.isPlaying() == true
    fun setEQPreset(preset: EQPreset) { musicService?.applyPreset(preset) }
    fun getCurrentEQPreset(): EQPreset = musicService?.currentPreset ?: EQPreset.NONE
    fun setPlaybackSpeed(speed: Float) { musicService?.setSpeed(speed) }
    fun toggleFavorite(audioId: Long) {
        val favs = getFavorites().toMutableSet()
        if (favs.contains(audioId.toString())) favs.remove(audioId.toString()) else favs.add(audioId.toString())
        prefs.edit().putStringSet("favorite_ids", favs).apply()
    }
    fun getFavorites(): Set<String> = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
    fun isFavorite(audioId: Long): Boolean = getFavorites().contains(audioId.toString())
    fun saveSelectedFolders(folders: Set<String>) { prefs.edit().putStringSet("selected_folders", folders).apply() }
    fun loadSelectedFolders(): Set<String> = prefs.getStringSet("selected_folders", emptySet()) ?: emptySet()
    fun saveMinDurationSetting(seconds: Int) { prefs.edit().putInt("min_duration", seconds).apply() }
    fun loadMinDurationSetting(): Int = prefs.getInt("min_duration", 30)
    fun savePlaylists(playlists: List<Playlist>) {
        try {
            // Use a simple JSON-like format for better reliability
            val playlistsJson = playlists.joinToString("|||") { playlist ->
                val songIdsStr = playlist.songIds.joinToString(",")
                "${playlist.id}::${playlist.name}::${playlist.isSmart}::$songIdsStr"
            }
            prefs.edit().putString("playlists_v2", playlistsJson).apply()
        } catch (e: Exception) {
            android.util.Log.e("PlaylistSave", "Error saving playlists", e)
        }
    }
    
    fun loadPlaylists(): List<Playlist> {
        return try {
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
                        Playlist(name, id, songIds, isSmart)
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
