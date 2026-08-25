package com.example.glyphequalizer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.audiofx.Visualizer
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log

class GlyphVisualizerService : Service() {

    companion object {
        private const val TAG = "GlyphVisualizerService"
        private const val CHANNEL_ID = "glyph_visualizer_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CAPTURE_INTERVAL_MS = 33 // ~30fps, matches note in build prompt
        const val ACTION_START = "com.example.glyphequalizer.START"
        const val ACTION_STOP = "com.example.glyphequalizer.STOP"
        const val EXTRA_SENSITIVITY = "sensitivity"
        const val LOW_BATTERY_CUTOFF_PERCENT = 15
    }

    private var visualizer: Visualizer? = null
    private var bandMapper = BandMapper()
    private val glyphController: GlyphController = NothingGlyphController()
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVisualizing()
                stopSelf()
            }
            else -> {
                val sensitivity = intent?.getFloatExtra(EXTRA_SENSITIVITY, 1.0f) ?: 1.0f
                startForeground(NOTIFICATION_ID, buildNotification())
                startVisualizing(sensitivity)
            }
        }
        return START_STICKY
    }

    private fun startVisualizing(sensitivity: Float) {
        if (running) return

        if (isBatteryTooLow()) {
            Log.w(TAG, "Battery below ${LOW_BATTERY_CUTOFF_PERCENT}% - refusing to start visualizer")
            stopSelf()
            return
        }

        if (!glyphController.connect(this)) {
            Log.e(TAG, "Could not connect to Glyph hardware - stopping service")
            stopSelf()
            return
        }

        bandMapper = BandMapper(sensitivity = sensitivity)

        try {
            // Session ID 0 = attach to the system's global output mix rather than one app,
            // so this reacts to whatever app is currently playing audio.
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // max resolution available
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        // Not used - we rely on FFT for band-based visualization.
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        fft ?: return
                        val zoneLevels = bandMapper.mapFftToZones(fft)
                        glyphController.setAllZones(zoneLevels)
                    }
                }, (1000 / CAPTURE_INTERVAL_MS) * 1000, false, true)
                enabled = true
            }
            running = true
            Log.i(TAG, "Visualizer started")
        } catch (e: Exception) {
            // Visualizer(0) can throw if RECORD_AUDIO isn't granted or no audio session
            // exists yet - caller should ensure permission is granted before starting.
            Log.e(TAG, "Failed to start Visualizer", e)
            stopSelf()
        }
    }

    private fun stopVisualizing() {
        running = false
        visualizer?.apply {
            enabled = false
            release()
        }
        visualizer = null
        glyphController.turnOffAll()
        glyphController.disconnect()
        Log.i(TAG, "Visualizer stopped, Glyph released")
    }

    private fun isBatteryTooLow(): Boolean {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level in 0 until LOW_BATTERY_CUTOFF_PERCENT
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Glyph Visualizer", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Equalizer active")
            .setContentText("Syncing Glyph Bar to playing audio")
            .setSmallIcon(android.R.drawable.ic_media_play) // TODO: replace with app icon
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVisualizing()
        super.onDestroy()
    }
}
