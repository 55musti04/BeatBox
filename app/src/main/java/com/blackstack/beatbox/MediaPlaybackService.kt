package com.blackstack.beatbox

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.*

// Optimize bitmap cache size to prevent OutOfMemoryError (shared across app)
val bitmapCache = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()) {
    override fun sizeOf(key: Long, bitmap: Bitmap): Int = (bitmap.byteCount / 1024).coerceAtLeast(1)
}


class MediaPlaybackService : Service(), AudioManager.OnAudioFocusChangeListener {

    inner class MusicBinder : Binder() { fun getService() = this@MediaPlaybackService }
    private val binder = MusicBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private lateinit var mediaSession: MediaSessionCompat
    private var audioFocusRequest: AudioFocusRequest? = null

    var currentAudio: AudioFile? = null
    private var resumeOnFocusGain = false
    private var wasPlayingBeforeMute = false

    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onSongComplete: (() -> Unit)? = null
    var onPrevRequested: (() -> Unit)? = null
    var onAudioChanged: ((AudioFile?) -> Unit)? = null

    private var lyricsPositionCallback: ((Long) -> Unit)? = null
    private var positionUpdateJob: Job? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val channelId = "BeatBoxChannel"
    private val notificationId = 101

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null
    
    // Real-time audio visualization data
    @Volatile
    private var currentLevel: Float = 0f
    @Volatile
    private var currentWaveform: FloatArray = FloatArray(15) { 0f }
    
    fun getCurrentLevel(): Float = currentLevel
    fun getCurrentWaveform(): FloatArray = currentWaveform.copyOf()
    
    var currentPreset: EQPreset = EQPreset.NONE
    
    // Notification action states
    var isRepeatEnabled: Boolean = false
    var isShuffleEnabled: Boolean = false
    var isFavorite: Boolean = false
    var currentSpeedIndex: Int = 2 // 0=0.5x, 1=0.75x, 2=1x, 3=1.25x, 4=1.5x, 5=2x
    var currentEQIndex: Int = 0 // 0=NONE, 1=BASS_BOOST, 2=VOCAL, 3=TREBLE, 4=LOUDNESS

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                when (intent.getIntExtra("state", -1)) {
                    1 -> if (currentAudio != null && !isPlaying()) resume()
                    0 -> pause()
                }
            }
        }
    }

    private var noisyRegistered = false
    private var headsetRegistered = false
    private var volumeObserver: ContentObserver? = null
    private var lastVolume = -1
    var playbackSpeed: Float = 1.0f
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "BeatBox").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null, this@MediaPlaybackService, MediaButtonReceiver::class.java)
            val mediaButtonPendingIntent = PendingIntent.getBroadcast(this@MediaPlaybackService, 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE)
            setMediaButtonReceiver(mediaButtonPendingIntent)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resume() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { onSongComplete?.invoke() }
                override fun onSkipToPrevious() { onPrevRequested?.invoke() }
                override fun onStop() {
                    pause()
                    try { ServiceCompat.stopForeground(this@MediaPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
                    stopSelf()
                }
                override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            })
            isActive = true
        }

        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val v = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (v == 0 && lastVolume > 0 && isPlaying()) { 
                    wasPlayingBeforeMute = true
                    pause() 
                } else if (v > 0 && lastVolume == 0 && wasPlayingBeforeMute) { 
                    wasPlayingBeforeMute = false
                    resume() 
                }
                lastVolume = v
            }
        }
        try {
            contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, 
                true, 
                volumeObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register headset receiver safely
        if (!headsetRegistered) {
            val f = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(headsetReceiver, f, RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(headsetReceiver, f)
                }
                headsetRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startStatsTracking()
    }

    private fun startStatsTracking() {
        serviceScope.launch(Dispatchers.Default) {
            var actualSecs = 0
            while (isActive) {
                if (mediaPlayer?.isPlaying == true) {
                    delay(1000)
                    actualSecs++
                    if (actualSecs >= 5) {
                        try {
                            val prefs = getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
                            val totalSecs = prefs.getLong("stats_total_seconds", 0L) + actualSecs
                            prefs.edit()
                                .putLong("stats_total_seconds", totalSecs)
                                .putLong("stats_total_minutes", totalSecs / 60L)
                                .apply()
                            actualSecs = 0
                        } catch (e: Exception) {
                            android.util.Log.w("BeatBox", "Error saving stats: ${e.message}")
                        }
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Intent.ACTION_MEDIA_BUTTON == intent?.action) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            return START_STICKY
        }

        if (currentAudio != null && isPlaying()) {
            startForeground(notificationId, buildNotification(currentAudio!!, true))
        } else if (currentAudio != null) {
            updateNotification(currentAudio!!, false)
        }
        
        when (intent?.action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> onSongComplete?.invoke()
            ACTION_PREV -> onPrevRequested?.invoke()
            ACTION_STOP -> {
                pause()
                try { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
                stopSelf()
            }
            ACTION_REPEAT -> {
                isRepeatEnabled = !isRepeatEnabled
                currentAudio?.let { updateNotification(it, isPlaying()) }
            }
            ACTION_SHUFFLE -> {
                isShuffleEnabled = !isShuffleEnabled
                currentAudio?.let { updateNotification(it, isPlaying()) }
            }
            ACTION_FAVORITE -> {
                isFavorite = !isFavorite
                currentAudio?.let { updateNotification(it, isPlaying()) }
            }
            ACTION_SPEED -> {
                currentSpeedIndex = (currentSpeedIndex + 1) % 6
                val speeds = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                setSpeed(speeds[currentSpeedIndex])
                currentAudio?.let { updateNotification(it, isPlaying()) }
            }
            ACTION_EQ -> {
                currentEQIndex = (currentEQIndex + 1) % 5
                val presets = arrayOf(EQPreset.NONE, EQPreset.BASS_BOOST, EQPreset.VOCAL, EQPreset.TREBLE, EQPreset.LOUDNESS)
                applyPreset(presets[currentEQIndex])
                currentAudio?.let { updateNotification(it, isPlaying()) }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isPlaying()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val restartServiceIntent = Intent(applicationContext, this.javaClass).also { it.setPackage(packageName) }
        val restartServicePendingIntent = PendingIntent.getService(applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }

    private fun requestAudioFocus(): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this).build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else @Suppress("DEPRECATION") audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else @Suppress("DEPRECATION") audioManager.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(f: Int) {
        when (f) {
            AudioManager.AUDIOFOCUS_GAIN -> { if (resumeOnFocusGain) { resumeOnFocusGain = false; resume() }; try { mediaPlayer?.setVolume(1f, 1f) } catch (_: Exception) {} }
            AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocusGain = false; pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { resumeOnFocusGain = isPlaying(); pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (_: Exception) {} }
        }
    }

    fun play(audio: AudioFile, crossfade: Boolean = false) {
        android.util.Log.d("MediaPlaybackService", "play() called for: ${audio.title}, crossfade: $crossfade")
        
        if (!requestAudioFocus()) {
            android.util.Log.e("MediaPlaybackService", "Failed to get audio focus!")
            return
        }
        
        android.util.Log.d("MediaPlaybackService", "Audio focus granted")
        
        try {
            // Start foreground immediately to avoid ANR
            val isNewAudio = currentAudio?.uri != audio.uri
            
            if (isNewAudio) {
                currentAudio = audio
                // Start foreground with loading notification
                startForeground(notificationId, buildNotification(audio, false))
            }
            
            if (isNewAudio) {
                // Cancel any ongoing crossfade
                stopCrossfadeMonitoring()
                
                if (crossfade && mediaPlayer?.isPlaying == true) {
                    val old = mediaPlayer
                    serviceScope.launch(Dispatchers.IO) {
                        var v = 1f
                        while (v > 0 && isActive) { 
                            v -= 0.05f
                            try { 
                                old?.setVolume(v.coerceAtLeast(0f), v.coerceAtLeast(0f)) 
                            } catch (e: Exception) {
                                android.util.Log.w("BeatBox", "Crossfade volume error: ${e.message}")
                            }
                            delay(100) 
                        }
                        try { 
                            old?.release()
                            android.util.Log.d("BeatBox", "Old player released after crossfade")
                        } catch (e: Exception) {
                            android.util.Log.e("BeatBox", "Error releasing old player: ${e.message}")
                        }
                    }
                } else { 
                    releaseEQ()
                    try { 
                        mediaPlayer?.release()
                        android.util.Log.d("BeatBox", "Player released (no crossfade)")
                    } catch (e: Exception) {
                        android.util.Log.e("BeatBox", "Error releasing player: ${e.message}")
                    }
                }
                
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                    if (audio.uri.scheme.equals("http", ignoreCase = true) || audio.uri.scheme.equals("https", ignoreCase = true)) {
                        setDataSource(audio.uri.toString())
                    } else {
                        setDataSource(applicationContext, audio.uri)
                    }
                    
                    // Use prepareAsync instead of prepare to avoid blocking
                    setOnPreparedListener { mp ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try { 
                                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed) 
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        if (crossfade) {
                            mp.setVolume(0f, 0f)
                            mp.start()
                            serviceScope.launch(Dispatchers.IO) {
                                var v = 0f
                                while (v < 1f && isActive) { 
                                    v += 0.04f
                                    try { 
                                        mp.setVolume(v.coerceAtMost(1f), v.coerceAtMost(1f)) 
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    delay(100) 
                                }
                            }
                        } else {
                            mp.start()
                        }
                        
                        // Update online audio duration with actual MediaPlayer duration
                        if (audio.isOnline) {
                            val mpDur = try { mp.duration.toLong() } catch (_: Exception) { 0L }
                            if (mpDur > 0L && mpDur != audio.duration) {
                                val updated = audio.copy(duration = mpDur)
                                currentAudio = updated
                                onAudioChanged?.invoke(updated)
                            }
                        }

                        // Setup after preparation
                        setupEQ()
                        applyPreset(currentPreset)
                        updateMediaSession(audio, true)
                        updateNotification(audio, true)
                        onPlaybackStateChanged?.invoke(true)
                        onAudioChanged?.invoke(audio)
                        
                        // Load album art AFTER player is prepared and playing
                        loadArtAsync(audio, true)
                        
                        // Start automatic crossfade monitoring
                        startCrossfadeMonitoring(mp)
                    }
                    
                    // Set completion listener
                    setOnCompletionListener { completedPlayer ->
                        if (mediaPlayer === completedPlayer) {
                            onSongComplete?.invoke()
                        }
                    }
                    
                    // Set error listener
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("BeatBox", "MediaPlayer error: what=$what, extra=$extra")
                        
                        // If error -38, try to recover by skipping to next
                        if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK || what == -38) {
                            android.util.Log.e("BeatBox", "Error -38 detected, attempting recovery...")
                            serviceScope.launch(Dispatchers.Main) {
                                delay(100) // Small delay
                                onSongComplete?.invoke() // Skip to next
                            }
                            return@setOnErrorListener true // Error handled
                        }
                        
                        false // Let system handle other errors
                    }
                    
                    prepareAsync() // Non-blocking preparation
                }
                
                // ✅ Album art loading moved to onPrepared callback
            } else { 
                mediaPlayer?.let { player ->
                    if (player.isPlaying) { 
                        pause()
                        return 
                    } else {
                        try { 
                            player.start()
                            updateMediaSession(audio, true)
                            updateNotification(audio, true)
                            onPlaybackStateChanged?.invoke(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } ?: return
            }

            if (!noisyRegistered) {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(noisyReceiver, filter, RECEIVER_NOT_EXPORTED)
                    } else {
                        registerReceiver(noisyReceiver, filter)
                    }
                    noisyRegistered = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) { 
            android.util.Log.e("BeatBox", "Play error", e)
            e.printStackTrace() 
        }
    }

    private fun loadArtAsync(audio: AudioFile, playing: Boolean) {
        if (bitmapCache.get(audio.id) != null) return
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Add timeout to prevent indefinite blocking
                withTimeout(6000L) { // 6 second timeout
                    val bmp = loadAlbumArt(audio)
                    if (bmp != null) {
                        bitmapCache.put(audio.id, bmp)
                        withContext(Dispatchers.Main) { 
                            if (currentAudio?.id == audio.id) { 
                                updateMediaSession(audio, playing)
                                updateNotification(audio, playing) 
                            } 
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.w("BeatBox", "Album art loading timed out for ${audio.title}")
            } catch (e: Exception) {
                android.util.Log.e("BeatBox", "Error loading album art for ${audio.title}: ${e.message}")
            }
        }
    }

    fun setSpeed(speed: Float) {
        // Clamp speed to safe range
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        playbackSpeed = clampedSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { 
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(clampedSpeed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupEQ() {
        try {
            val s = mediaPlayer?.audioSessionId ?: return
            android.util.Log.d("BeatBox", "setupEQ - audioSessionId: $s")
            equalizer = Equalizer(0, s).apply { enabled = true }
            bassBoost = BassBoost(0, s).apply { enabled = false }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) loudnessEnhancer = LoudnessEnhancer(s).apply { enabled = false }
            // Setup visualizer on background thread to prevent ANR
            serviceScope.launch(Dispatchers.IO) {
                setupVisualizer(s)
            }
        } catch (e: Exception) {
            android.util.Log.e("BeatBox", "setupEQ failed", e)
            e.printStackTrace()
        }
    }

    private fun setupVisualizer(sessionId: Int) {
        try {
            // Check RECORD_AUDIO permission
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            
            if (!hasPermission) {
                android.util.Log.e("BeatBox", "RECORD_AUDIO permission not granted! Visualizer cannot work.")
                return
            }
            
            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                // Use larger capture size for better frequency resolution
                captureSize = Visualizer.getCaptureSizeRange()[1] // Use max size
                android.util.Log.d("BeatBox", "Visualizer setup - captureSize: $captureSize, sessionId: $sessionId")
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform == null || waveform.isEmpty()) return
                            
                            // Calculate overall level (RMS)
                            var sum = 0f
                            for (b in waveform) {
                                val v = (b.toInt() and 0xFF) - 128
                                sum += v * v
                            }
                            val mean = sum / waveform.size
                            val rms = kotlin.math.sqrt(mean)
                            val normalized = (rms / 64f).coerceIn(0f, 1f)
                            currentLevel = (currentLevel * 0.7f) + (normalized * 0.3f)
                            
                            // Calculate per-bar levels for visualizer (15 bars)
                            val barCount = 15
                            val samplesPerBar = waveform.size / barCount
                            val newWaveform = FloatArray(barCount)
                            
                            for (i in 0 until barCount) {
                                val startIdx = i * samplesPerBar
                                val endIdx = kotlin.math.min(startIdx + samplesPerBar, waveform.size)
                                var barSum = 0f
                                
                                for (j in startIdx until endIdx) {
                                    val v = (waveform[j].toInt() and 0xFF) - 128
                                    barSum += kotlin.math.abs(v).toFloat()
                                }
                                
                                val barMean = barSum / (endIdx - startIdx)
                                // Reduce sensitivity - only strong sounds trigger bars
                                val barNormalized = (barMean / 100f).coerceIn(0f, 1f)
                                
                                // Apply threshold - ignore very quiet sounds
                                val thresholded = if (barNormalized < 0.15f) 0f else (barNormalized - 0.15f) / 0.85f
                                
                                // Heavy smoothing for ultra-smooth animation (0.7/0.3)
                                newWaveform[i] = (currentWaveform[i] * 0.7f) + (thresholded * 0.3f)
                            }
                            
                            currentWaveform = newWaveform
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Şimdilik FFT kullanılmıyor
                        }
                    },
                    Visualizer.getMaxCaptureRate(), // Max capture rate for smoother animation
                    true,
                    false
                )
                enabled = true
                android.util.Log.d("BeatBox", "Visualizer enabled successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("BeatBox", "Visualizer setup failed", e)
            e.printStackTrace()
            currentLevel = 0f
        }
    }

    private fun releaseEQ() {
        try { 
            equalizer?.let { eq ->
                try { eq.enabled = false } catch (_: Exception) {}
                try { eq.release() } catch (_: Exception) {}
            }
            bassBoost?.let { bb ->
                try { bb.enabled = false } catch (_: Exception) {}
                try { bb.release() } catch (_: Exception) {}
            }
            loudnessEnhancer?.let { le ->
                try { le.enabled = false } catch (_: Exception) {}
                try { le.release() } catch (_: Exception) {}
            }
            visualizer?.let { v ->
                try { v.enabled = false } catch (_: Exception) {}
                try { v.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            loudnessEnhancer = null
            visualizer = null
            currentLevel = 0f
        }
    }

    fun applyPreset(p: EQPreset) {
        currentPreset = p
        try {
            bassBoost?.enabled = false; loudnessEnhancer?.enabled = false
            equalizer?.let { eq ->
                val mid = ((eq.bandLevelRange[0] + eq.bandLevelRange[1]) / 2).toShort()
                for (i in 0 until eq.numberOfBands) eq.setBandLevel(i.toShort(), mid)
            }
            when (p) {
                EQPreset.BASS_BOOST -> {
                    bassBoost?.enabled = true; bassBoost?.setStrength(800.toShort())
                    equalizer?.let { if (it.numberOfBands >= 2) { it.setBandLevel(0, it.bandLevelRange[1]); it.setBandLevel(1, (it.bandLevelRange[1] * 0.7).toInt().toShort()) } }
                }
                EQPreset.VOCAL -> equalizer?.let { if (it.numberOfBands >= 5) { it.setBandLevel(2, it.bandLevelRange[1]); it.setBandLevel(3, (it.bandLevelRange[1] * 0.8).toInt().toShort()) } }
                EQPreset.TREBLE -> equalizer?.let { val n = it.numberOfBands; if (n >= 5) { it.setBandLevel((n - 1).toShort(), it.bandLevelRange[1]); it.setBandLevel((n - 2).toShort(), (it.bandLevelRange[1] * 0.7).toInt().toShort()) } }
                EQPreset.LOUDNESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { loudnessEnhancer?.enabled = true; loudnessEnhancer?.setTargetGain(800) }
                else -> {}
            }
        } catch (_: Exception) {}
    }

    private fun startCrossfadeMonitoring(player: MediaPlayer) {
        // Cancel any existing monitoring job
        monitoringJob?.cancel()
        
        // Run on background thread to prevent ANR
        monitoringJob = serviceScope.launch(Dispatchers.IO) {
            // ✅ Wait for MediaPlayer to be fully ready before starting monitoring
            // This prevents Error -38 on some devices
            delay(500)
            
            var crossfadeTriggered = false
            
            // Get crossfade setting
            val prefs = getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
            val crossfadeEnabled = prefs.getBoolean("crossfade_enabled", true)
            
            // Get duration once at start using metadata fallback
            val duration = try {
                player.duration
            } catch (e: IllegalStateException) {
                android.util.Log.v("BeatBox", "Using metadata duration for crossfade monitoring")
                currentAudio?.duration?.toInt() ?: 0
            }
            
            // If no valid duration, exit monitoring
            if (duration <= 3000) {
                android.util.Log.v("BeatBox", "Song too short or no duration, skipping crossfade")
                return@launch
            }
            
            while (isActive) {
                try {
                    // Check if player is still playing
                    if (!player.isPlaying || crossfadeTriggered) break
                    
                    // State-safe getCurrentPosition (safer than getDuration)
                    val currentPos = try {
                        player.currentPosition
                    } catch (e: IllegalStateException) {
                        android.util.Log.v("BeatBox", "Player not ready for currentPosition in crossfade")
                        break
                    }
                    
                    // Start crossfade 3 seconds before song ends
                    if (currentPos > 0 && (duration - currentPos) in 1..3100) {
                        if (!crossfadeTriggered && crossfadeEnabled) {
                            crossfadeTriggered = true
                            withContext(Dispatchers.Main) {
                                if (mediaPlayer === player) {
                                    onSongComplete?.invoke()
                                }
                            }
                        }
                        break
                    }
                } catch (e: Exception) {
                    // Player might be released or in invalid state, exit loop
                    android.util.Log.e("BeatBox", "Crossfade monitoring error: ${e.message}", e)
                    break
                }
                delay(200) // Check every 200ms
            }
        }
    }

    private fun stopCrossfadeMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun pause() {
        stopCrossfadeMonitoring()
        try { mediaPlayer?.pause(); onPlaybackStateChanged?.invoke(false); currentAudio?.let { updateMediaSession(it, false); updateNotification(it, false) } } catch (_: Exception) {}
    }

    fun resume() {
        if (requestAudioFocus()) { try { mediaPlayer?.start(); onPlaybackStateChanged?.invoke(true); currentAudio?.let { updateMediaSession(it, true); updateNotification(it, true) } } catch (_: Exception) {} }
    }

    fun seekTo(pos: Int) { 
        try { 
            mediaPlayer?.let { player ->
                // Only seek if player is in valid state
                if (player.isPlaying || currentAudio != null) {
                    val actualDur = try { player.duration } catch (_: Exception) { currentAudio?.duration?.toInt() ?: 0 }
                    val maxSeek = if (actualDur > 1000) actualDur - 600 else actualDur
                    val safePos = if (maxSeek > 0) pos.coerceIn(0, maxSeek) else pos.coerceAtLeast(0)
                    player.seekTo(safePos)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("BeatBox", "seekTo failed: ${e.message}")
        } 
    }
    
    fun getCurrentPosition(): Int = try { 
        mediaPlayer?.let { player ->
            // Check if player is in valid state before getting position
            if (player.isPlaying || currentAudio != null) {
                player.currentPosition
            } else {
                0
            }
        } ?: 0
    } catch (e: Exception) { 
        android.util.Log.w("BeatBox", "getCurrentPosition failed: ${e.message}")
        0 
    }
    
    fun getDuration(): Int = try {
        mediaPlayer?.let { player ->
            try {
                // Try to get duration from MediaPlayer
                val d = player.duration
                if (d > 0) {
                    return d
                }
            } catch (e: IllegalStateException) {
                // Player not in valid state, fall back to metadata
                android.util.Log.v("BeatBox", "MediaPlayer not ready for getDuration, using metadata")
            }
        }
        // Fallback to cached duration from metadata
        currentAudio?.duration?.toInt() ?: 0
    } catch (e: Exception) {
        android.util.Log.w("BeatBox", "getDuration failed: ${e.message}")
        currentAudio?.duration?.toInt() ?: 0
    }
    fun isPlaying(): Boolean = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }
    fun setVolume(v: Float) { try { mediaPlayer?.setVolume(v, v) } catch (_: Exception) {} }
    
    fun setLyricsPositionCallback(callback: (Long) -> Unit) {
        lyricsPositionCallback = callback
        startPositionUpdates()
    }
    
    fun removeLyricsPositionCallback() {
        lyricsPositionCallback = null
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    private fun startPositionUpdates() {
        // Cancel existing job if any
        positionUpdateJob?.cancel()
        
        // Start new position update coroutine
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                if (isPlaying()) {
                    val position = getCurrentPosition().toLong()
                    lyricsPositionCallback?.invoke(position)
                }
                delay(100) // Update every 100ms for smooth sync
            }
        }
    }
    
    fun pauseVisualizer() {
        serviceScope.launch(Dispatchers.Main) {
            try {
                visualizer?.enabled = false
                android.util.Log.d("BeatBox", "Visualizer paused for battery saving")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun resumeVisualizer() {
        serviceScope.launch(Dispatchers.Main) {
            try {
                // Always enable if visualizer exists, regardless of playing state
                // The visualizer will just show zero values when not playing
                visualizer?.enabled = true
                android.util.Log.d("BeatBox", "Visualizer resumed")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMediaSession(audio: AudioFile, playing: Boolean) {
        try {
            val albumArt = bitmapCache.get(audio.id)
            if (albumArt == null) {
                loadArtAsync(audio, playing)
            }
            
            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, audio.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, audio.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, audio.duration)
            
            if (albumArt != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            }
            
            mediaSession.setMetadata(metadataBuilder.build())
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                .setState(if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, getCurrentPosition().toLong(), if (playing) playbackSpeed else 0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP)
                .build())
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Music Playback", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(audio: AudioFile, playing: Boolean): Notification {
        // Only use cached album art for notification - don't block to load it
        val albumArt = bitmapCache.get(audio.id)
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(audio.title)
            .setContentText(audio.artist)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setLargeIcon(albumArt)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 
                    0, 
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(false)
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    actionIntent(ACTION_PREV)
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "Pause" else "Play",
                    actionIntent(if (playing) ACTION_PAUSE else ACTION_PLAY)
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    actionIntent(ACTION_NEXT)
                ).build()
            )
            .setDeleteIntent(actionIntent(ACTION_STOP))
            .setOngoing(playing)
            .build()
    }

    private fun loadAlbumArt(audio: AudioFile): Bitmap? {
        return try {
            // First try: Load from album art URI
            val uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), audio.albumId)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION") 
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            
            // Cache the loaded bitmap
            if (bitmap != null) {
                bitmapCache.put(audio.id, bitmap)
            }
            
            bitmap
        } catch (e: Exception) { 
            android.util.Log.w("BeatBox", "Failed to load album art from URI for ${audio.title}, trying embedded art: ${e.message}")
            
            // Second try: Extract embedded album art from audio file with timeout
            var retriever: MediaMetadataRetriever? = null
            try {
                // Use withTimeout to prevent ANR from slow metadata extraction
                val timeoutMillis = 5000L // 5 second timeout
                val startTime = System.currentTimeMillis()
                
                retriever = MediaMetadataRetriever()
                
                // Try to set data source - this can take a long time for some files
                try {
                    if (audio.uri.scheme.equals("http", ignoreCase = true) || audio.uri.scheme.equals("https", ignoreCase = true)) {
                        retriever.setDataSource(audio.uri.toString(), HashMap())
                    } else {
                        // For file URIs, use file descriptor for better performance
                        contentResolver.openFileDescriptor(audio.uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("BeatBox", "Failed to set data source for ${audio.title}: ${e.message}")
                    return null
                }
                
                // Check if we've already exceeded timeout during setDataSource
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    android.util.Log.w("BeatBox", "Timeout during setDataSource for ${audio.title}")
                    return null
                }
                
                // Extract album art - this can also be slow
                val art = retriever.embeddedPicture
                
                // Check timeout again
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    android.util.Log.w("BeatBox", "Timeout during embeddedPicture extraction for ${audio.title}")
                    return null
                }
                
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    if (bitmap != null) {
                        bitmapCache.put(audio.id, bitmap)
                        android.util.Log.d("BeatBox", "Successfully loaded embedded album art for ${audio.title} in ${System.currentTimeMillis() - startTime}ms")
                    }
                    bitmap
                } else {
                    android.util.Log.w("BeatBox", "No embedded album art found for ${audio.title}")
                    null
                }
            } catch (e2: Exception) {
                android.util.Log.e("BeatBox", "Failed to load embedded album art for ${audio.title}: ${e2.message}")
                null
            } finally {
                // CRITICAL: Always release retriever in finally block
                try {
                    retriever?.release()
                } catch (e: Exception) {
                    android.util.Log.e("BeatBox", "Error releasing MediaMetadataRetriever: ${e.message}")
                }
            }
        }
    }

    private fun updateNotification(audio: AudioFile, playing: Boolean) {
        try {
            val notification = buildNotification(audio, playing)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (playing) {
                startForeground(notificationId, notification)
            } else {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e("BeatBox", "Error updating notification: ${e.message}")
        }
    }

    private fun actionIntent(action: String) = PendingIntent.getService(this, action.hashCode(), Intent(this, MediaPlaybackService::class.java).apply { this.action = action }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel monitoring job
        stopCrossfadeMonitoring()
        
        // Cancel position update job
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        lyricsPositionCallback = null
        
        // Abandon audio focus
        abandonAudioFocus()
        
        // Release equalizer resources
        releaseEQ()

        // Cancel all coroutines after asynchronous effect cleanup was scheduled
        serviceScope.cancel()
        
        // Unregister receivers safely
        if (noisyRegistered) {
            try { 
                unregisterReceiver(noisyReceiver)
                noisyRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (headsetRegistered) {
            try { 
                unregisterReceiver(headsetReceiver)
                headsetRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Unregister content observer
        volumeObserver?.let { observer ->
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Release media player
        try { 
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Release media session
        try { 
            mediaSession.release() 
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_PLAY = "com.blackstack.beatbox.PLAY"
        const val ACTION_PAUSE = "com.blackstack.beatbox.PAUSE"
        const val ACTION_NEXT = "com.blackstack.beatbox.NEXT"
        const val ACTION_PREV = "com.blackstack.beatbox.PREV"
        const val ACTION_STOP = "com.blackstack.beatbox.STOP"
        const val ACTION_REPEAT = "com.blackstack.beatbox.REPEAT"
        const val ACTION_SHUFFLE = "com.blackstack.beatbox.SHUFFLE"
        const val ACTION_FAVORITE = "com.blackstack.beatbox.FAVORITE"
        const val ACTION_SPEED = "com.blackstack.beatbox.SPEED"
        const val ACTION_EQ = "com.blackstack.beatbox.EQ"
    }
}
