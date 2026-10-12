package com.anant.gecko.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.anant.gecko.MainActivity
import com.anant.gecko.R
import com.anant.gecko.audio.AudioMemory
import com.anant.gecko.audio.WavFileWriter
import com.anant.gecko.data.GeckoPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Foreground mic service with an in-memory ring buffer ("time travel").
 *
 * Battery path:
 * - Blocking [AudioRecord.read] in ~80 ms slices (HAL sleep, no timer wakeups)
 * - [AudioRecord.PERFORMANCE_MODE_POWER_SAVING] when the device supports it
 * - Default 8 kHz speech rate
 * - No UI state publishes while the app is backgrounded
 */
class GeckoRecordingService : Service() {

    enum class Mode { Idle, Listening, Recording }

    data class UiState(
        val listeningEnabled: Boolean = false,
        val mode: Mode = Mode.Idle,
        val memorizedSeconds: Float = 0f,
        val totalMemorySeconds: Float = 0f,
        val recordedSeconds: Float = 0f,
        val sampleRate: Int = GeckoPreferences.DEFAULT_SAMPLE_RATE,
        val memoryBytes: Long = 0L,
        val lastSavedFile: File? = null,
        val lastSavedRuntime: Float = 0f,
        val busy: Boolean = false,
        val error: String? = null,
    )

    inner class LocalBinder : Binder() {
        fun getService(): GeckoRecordingService = this@GeckoRecordingService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefs: GeckoPreferences

    @Volatile private var sampleRate = GeckoPreferences.DEFAULT_SAMPLE_RATE
    @Volatile private var fillRate = 2 * sampleRate
    @Volatile private var mode = Mode.Idle
    @Volatile private var listeningPref = true
    @Volatile private var configuredMemoryBytes = 0L

    private val audioMemory = AudioMemory()
    private val writerLock = Any()
    private var audioRecord: AudioRecord? = null
    private var wavWriter: WavFileWriter? = null
    private var wavFile: File? = null

    private val captureActive = AtomicBoolean(false)
    private var captureThread: Thread? = null
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val controlLock = Object()
    private var pendingControl: (() -> Unit)? = null

    /** >0 while an Activity is bound — capture skips StateFlow publishes otherwise. */
    private val uiClients = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setUiAttached(attached: Boolean) {
        if (attached) {
            uiClients.incrementAndGet()
            publishState()
        } else {
            uiClients.updateAndGet { (it - 1).coerceAtLeast(0) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = GeckoPreferences(this)
        ensureChannel()

        runBlocking {
            sampleRate = prefs.getSampleRate()
            fillRate = 2 * sampleRate
            listeningPref = prefs.isListeningEnabled()
            configuredMemoryBytes = prefs.getMemoryBytes()
        }
        // Bind-only creation must not start capture — without startForegroundService the
        // process dies on Activity unbind (swipe-away). Real starts go through onStartCommand.
        publishState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LISTENING -> stopListening()
            ACTION_TOGGLE_LISTEN -> {
                if (listeningPref && mode != Mode.Idle) stopListening() else startListening()
            }
            ACTION_START_LISTENING -> startListening()
            else -> {
                // Sticky / task-removed redelivery — resume if we still want mic capture.
                if (listeningPref && hasMicPermission()) {
                    beginListeningSession()
                } else if (mode != Mode.Idle && hasMicPermission()) {
                    promoteForeground()
                }
            }
        }
        // Started + sticky so swipe-away unbind does not destroy the service.
        return if (listeningPref) START_STICKY else START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (listeningPref && hasMicPermission()) {
            ContextCompat.startForegroundService(
                applicationContext,
                startListeningIntent(applicationContext),
            )
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopRecordingInternal()
        stopCapture()
        scope.cancel()
        super.onDestroy()
    }

    fun startListening() {
        if (!hasMicPermission()) {
            Log.w(TAG, "startListening blocked — RECORD_AUDIO not granted")
            publishState()
            return
        }
        scope.launch {
            prefs.setListeningEnabled(true)
            listeningPref = true
            sampleRate = prefs.getSampleRate()
            fillRate = 2 * sampleRate
            configuredMemoryBytes = prefs.getMemoryBytes()
            // Must be a *started* FGS — bind alone dies when the Activity unbinds on swipe.
            ContextCompat.startForegroundService(
                this@GeckoRecordingService,
                startIntent(this@GeckoRecordingService),
            )
            beginListeningSession()
        }
    }

    private fun beginListeningSession() {
        if (!hasMicPermission()) {
            mode = Mode.Idle
            publishState()
            return
        }
        if (mode == Mode.Listening || mode == Mode.Recording) {
            promoteForeground()
            return
        }
        mode = Mode.Listening
        if (!promoteForeground()) {
            mode = Mode.Idle
            publishState()
            return
        }
        startCapture(configuredMemoryBytes)
        publishState()
    }

    fun stopListening() {
        if (mode == Mode.Recording) return
        scope.launch {
            prefs.setListeningEnabled(false)
            listeningPref = false
            stopCapture()
            mode = Mode.Idle
            ServiceCompat.stopForeground(
                this@GeckoRecordingService,
                ServiceCompat.STOP_FOREGROUND_REMOVE,
            )
            stopSelf()
            publishState()
        }
    }

    fun setMemoryBytes(bytes: Long) {
        scope.launch {
            prefs.setMemoryBytes(bytes)
            configuredMemoryBytes = bytes
            if (listeningPref && mode != Mode.Idle) {
                runOnCaptureControl { audioMemory.allocate(bytes) }
            }
            publishState()
        }
    }

    fun setSampleRate(rate: Int) {
        if (mode == Mode.Recording) return
        scope.launch {
            prefs.setSampleRate(rate)
            val wasListening = mode == Mode.Listening
            if (wasListening) {
                stopCapture()
                sampleRate = rate
                fillRate = 2 * rate
                mode = Mode.Listening
                startCapture(configuredMemoryBytes)
            } else {
                sampleRate = rate
                fillRate = 2 * rate
            }
            publishState()
        }
    }

    fun dumpRecording(seconds: Float, fileName: String?, onDone: ((File, Float) -> Unit)?) {
        if (mode != Mode.Listening) return
        _uiState.value = _uiState.value.copy(busy = true)
        scope.launch(Dispatchers.IO) {
            try {
                // Drain + snapshot while capture is paused inside the control callback
                // so the ring isn't overwritten mid-save.
                lateinit var file: File
                runOnCaptureControl {
                    drainHardwareBuffer()
                    file = writeHistoryToTemp(seconds, fileName)
                }
                val runtime = (file.length() - 44).coerceAtLeast(0) * bytesToSeconds()
                val exported = exportToMediaStore(file)
                file.delete()
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    lastSavedFile = exported,
                    lastSavedRuntime = runtime,
                    error = null,
                )
                onDone?.invoke(exported, runtime)
                notifySaved(exported)
                publishState()
            } catch (e: Exception) {
                Log.e(TAG, "dump failed", e)
                _uiState.value = _uiState.value.copy(busy = false, error = e.message)
                toast(getString(R.string.error_save, e.message ?: ""))
            }
        }
    }

    fun startRecording(prependSeconds: Float) {
        if (mode == Mode.Recording) return
        if (mode == Mode.Idle) {
            startListening()
        }
        mode = Mode.Recording
        promoteForeground()
        scope.launch(Dispatchers.IO) {
            try {
                runOnCaptureControl {
                    drainHardwareBuffer()
                    synchronized(writerLock) {
                        val temp = createTempWav(null)
                        wavFile = temp
                        val writer = WavFileWriter(sampleRate, temp)
                        writePrepend(prependSeconds, writer)
                        wavWriter = writer
                    }
                }
                publishState()
            } catch (e: Exception) {
                Log.e(TAG, "startRecording failed", e)
                mode = Mode.Listening
                toast(getString(R.string.error_save, e.message ?: ""))
                publishState()
            }
        }
    }

    fun stopRecording(onDone: ((File, Float) -> Unit)?) {
        if (mode != Mode.Recording) return
        mode = Mode.Listening
        scope.launch(Dispatchers.IO) {
            try {
                var runtime = 0f
                var exported: File? = null
                runOnCaptureControl {
                    drainHardwareBuffer()
                    synchronized(writerLock) {
                        val writer = wavWriter
                        val temp = wavFile
                        wavWriter = null
                        wavFile = null
                        writer?.close()
                        if (writer != null && temp != null) {
                            runtime = writer.totalSampleBytesWritten() * bytesToSeconds()
                            exported = exportToMediaStore(temp)
                            temp.delete()
                        }
                    }
                }
                exported?.let {
                    _uiState.value = _uiState.value.copy(
                        lastSavedFile = it,
                        lastSavedRuntime = runtime,
                    )
                    onDone?.invoke(it, runtime)
                    notifySaved(it)
                }
                if (!prefs.isListeningEnabled()) {
                    stopListening()
                } else {
                    promoteForeground()
                    publishState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "stopRecording failed", e)
                toast(getString(R.string.error_save, e.message ?: ""))
                publishState()
            }
        }
    }

    fun clearSavedPrompt() {
        _uiState.value = _uiState.value.copy(lastSavedFile = null, lastSavedRuntime = 0f)
    }

    private fun stopRecordingInternal() {
        if (mode != Mode.Recording) return
        mode = Mode.Listening
        runOnCaptureControl {
            synchronized(writerLock) {
                try {
                    wavWriter?.close()
                } catch (_: Exception) {
                }
                wavWriter = null
                wavFile = null
            }
        }
    }

    private fun startCapture(memoryBytes: Long) {
        stopCapture()
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        // Larger HW buffer → HAL batches more → fewer wakeups; drain() still empties it on save
        val bufferSize = (minBuf * 6).coerceAtLeast(sampleRate * 2)

        val record = createPowerSavingRecorder(bufferSize)
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            mode = Mode.Idle
            toast(getString(R.string.error_mic))
            publishState()
            return
        }
        audioRecord = record
        audioMemory.allocate(memoryBytes)
        record.startRecording()
        captureActive.set(true)

        captureThread = thread(name = "gecko-capture", isDaemon = true) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val maxSlice = (sampleRate * BYTES_PER_SAMPLE * SLICE_MS / 1000).coerceAtLeast(320)
            var slices = 0
            while (captureActive.get()) {
                // Dump/stop between slices (≤ SLICE_MS control latency)
                val control = synchronized(controlLock) { pendingControl }
                if (control != null) {
                    try {
                        control()
                    } catch (e: Exception) {
                        Log.e(TAG, "capture control failed", e)
                    } finally {
                        synchronized(controlLock) {
                            if (pendingControl === control) pendingControl = null
                            controlLock.notifyAll()
                        }
                    }
                    continue
                }

                try {
                    audioMemory.fill { array, offset, count ->
                        val rec = audioRecord ?: return@fill 0
                        val toRead = min(count, maxSlice)
                        val read = rec.read(array, offset, toRead) // blocking
                        when {
                            read > 0 -> {
                                synchronized(writerLock) {
                                    wavWriter?.write(array, offset, read)
                                }
                                read
                            }
                            read == 0 -> 0
                            else -> {
                                Log.e(TAG, "AudioRecord error: $read — retrying")
                                try {
                                    Thread.sleep(40)
                                } catch (_: InterruptedException) {
                                    captureActive.set(false)
                                }
                                0
                            }
                        }
                    }
                    // UI meter only — never while backgrounded
                    if (uiClients.get() > 0 && ++slices % SLICES_PER_UI_TICK == 0) {
                        mainHandler.post { publishState() }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "capture loop", e)
                    try {
                        Thread.sleep(40)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun createPowerSavingRecorder(bufferSize: Int): AudioRecord? {
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        return try {
            val builder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
            // setPerformanceMode existed on API 29–35; compile SDK 36+ stubs omit it,
            // but many devices still honor it at runtime.
            applyPowerSavingMode(builder)
            builder.build()
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord.Builder failed, falling back", e)
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            } catch (e2: Exception) {
                Log.e(TAG, "AudioRecord create failed", e2)
                null
            }
        }
    }

    private fun applyPowerSavingMode(builder: AudioRecord.Builder) {
        try {
            val mode = AudioRecord::class.java.getField("PERFORMANCE_MODE_POWER_SAVING").getInt(null)
            val method = AudioRecord.Builder::class.java.getMethod(
                "setPerformanceMode",
                Int::class.javaPrimitiveType,
            )
            method.invoke(builder, mode)
            Log.d(TAG, "AudioRecord PERFORMANCE_MODE_POWER_SAVING enabled")
        } catch (_: Exception) {
            // Stub/device without the API — larger buffer + 8 kHz still help
        }
    }

    private fun stopCapture() {
        captureActive.set(false)
        val record = audioRecord
        try {
            record?.stop()
        } catch (_: Exception) {
        }
        captureThread?.join(1500)
        captureThread = null
        try {
            record?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        audioMemory.allocate(0)
        synchronized(controlLock) {
            pendingControl = null
            controlLock.notifyAll()
        }
    }

    /** Pull leftover PCM from the HW buffer into the ring before save/export. */
    private fun drainHardwareBuffer() {
        val rec = audioRecord ?: return
        var guard = 0
        while (guard++ < 64) {
            var got = 0
            audioMemory.fill { array, offset, count ->
                val read = rec.read(array, offset, count, AudioRecord.READ_NON_BLOCKING)
                if (read > 0) {
                    synchronized(writerLock) {
                        wavWriter?.write(array, offset, read)
                    }
                    got = read
                    read
                } else {
                    0
                }
            }
            if (got <= 0) break
        }
    }

    private fun runOnCaptureControl(block: () -> Unit) {
        if (!captureActive.get() || captureThread?.isAlive != true) {
            block()
            return
        }
        if (Thread.currentThread() === captureThread) {
            block()
            return
        }
        synchronized(controlLock) {
            pendingControl = block
            val deadline = System.currentTimeMillis() + 3000
            while (pendingControl != null && System.currentTimeMillis() < deadline) {
                try {
                    controlLock.wait(50)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (pendingControl != null) {
                Log.w(TAG, "control timed out; running inline")
                pendingControl = null
                block()
            }
        }
    }

    private fun writePrepend(seconds: Float, writer: WavFileWriter) {
        val prependBytes = (seconds * fillRate).toInt()
        val available = audioMemory.countFilled()
        val skip = (available - prependBytes).coerceAtLeast(0)
        if (skip < available) {
            audioMemory.read(skip) { array, offset, count ->
                writer.write(array, offset, count)
                0
            }
        }
    }

    private fun writeHistoryToTemp(seconds: Float, fileName: String?): File {
        val temp = createTempWav(fileName)
        WavFileWriter(sampleRate, temp).use { writer ->
            writePrepend(seconds, writer)
        }
        return temp
    }

    private fun createTempWav(preferredName: String?): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val base = preferredName?.takeIf { it.isNotBlank() }?.let { sanitize(it) }
            ?: "Gecko_$stamp"
        val dir = File(cacheDir, "recordings").also { it.mkdirs() }
        return File(dir, "$base.wav")
    }

    private fun exportToMediaStore(source: File): File {
        val name = source.name
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Gecko")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(source).use { it.copyTo(out) }
        } ?: throw IOException("Cannot open MediaStore stream")
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        val shared = File(cacheDir, "shared").also { it.mkdirs() }
        val outFile = File(shared, name)
        source.copyTo(outFile, overwrite = true)
        return outFile
    }

    private fun bytesToSeconds(): Float = 1f / fillRate

    private fun publishState() {
        if (uiClients.get() == 0) return
        val stats = audioMemory.getStats(fillRate)
        val memorized = (if (stats.overwriting) stats.total else stats.filled + stats.estimation) *
            bytesToSeconds()
        val total = stats.total * bytesToSeconds()
        var recorded = 0
        synchronized(writerLock) {
            wavWriter?.let { recorded += it.totalSampleBytesWritten() }
        }
        recorded += stats.estimation
        val next = _uiState.value.copy(
            listeningEnabled = listeningPref,
            mode = mode,
            memorizedSeconds = memorized,
            totalMemorySeconds = total,
            recordedSeconds = recorded * bytesToSeconds(),
            sampleRate = sampleRate,
            memoryBytes = audioMemory.allocatedMemorySize.takeIf { it > 0 } ?: configuredMemoryBytes,
        )
        // Avoid recomposition churn when meters barely move
        val prev = _uiState.value
        if (prev.listeningEnabled == next.listeningEnabled &&
            prev.mode == next.mode &&
            prev.sampleRate == next.sampleRate &&
            prev.busy == next.busy &&
            prev.lastSavedFile == next.lastSavedFile &&
            kotlin.math.abs(prev.memorizedSeconds - next.memorizedSeconds) < 0.25f &&
            kotlin.math.abs(prev.recordedSeconds - next.recordedSeconds) < 0.25f &&
            kotlin.math.abs(prev.totalMemorySeconds - next.totalMemorySeconds) < 0.25f
        ) {
            return
        }
        _uiState.value = next
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** @return false if the OS rejected mic FGS (missing permission / ineligible state). */
    private fun promoteForeground(): Boolean {
        if (!hasMicPermission()) return false
        return try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Mic foreground service rejected", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Mic foreground service rejected", e)
            false
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = when (mode) {
            Mode.Recording -> getString(R.string.notif_recording)
            Mode.Listening -> getString(R.string.notif_listening)
            Mode.Idle -> getString(R.string.app_name)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_body))
            .setSmallIcon(R.drawable.ic_notif_gecko)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun notifySaved(file: File) {
        val open = PendingIntent.getActivity(
            this,
            file.name.hashCode(),
            Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_FILE, file.absolutePath),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, CHANNEL_SAVED)
            .setContentTitle(getString(R.string.recording_saved))
            .setContentText(file.name)
            .setSmallIcon(R.drawable.ic_notif_saved)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(file.name.hashCode(), n)
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_listening),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SAVED,
                getString(R.string.channel_saved),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun toast(msg: String) {
        android.os.Handler(mainLooper).post {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)

    companion object {
        private const val TAG = "GeckoService"
        private const val BYTES_PER_SAMPLE = 2
        /** Longer slices = fewer sched wakeups; save still drains HW buffer first. */
        private const val SLICE_MS = 80
        /** ~2 s UI meter refresh while the app is open (80 ms × 25). */
        private const val SLICES_PER_UI_TICK = 25
        const val CHANNEL_ID = "gecko_listening"
        const val CHANNEL_SAVED = "gecko_saved"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP_LISTENING = "com.anant.gecko.STOP_LISTENING"
        const val ACTION_TOGGLE_LISTEN = "com.anant.gecko.TOGGLE_LISTEN"
        const val ACTION_START_LISTENING = "com.anant.gecko.START_LISTENING"
        const val EXTRA_OPEN_FILE = "open_file"

        fun startIntent(context: android.content.Context): Intent =
            Intent(context, GeckoRecordingService::class.java)

        fun startListeningIntent(context: android.content.Context): Intent =
            Intent(context, GeckoRecordingService::class.java).setAction(ACTION_START_LISTENING)
    }
}
