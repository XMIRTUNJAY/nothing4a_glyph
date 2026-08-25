package com.example.glyphequalizer

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Controls the Nothing Phone (4a) Pro Glyph Matrix through Nothing's official SDK. */
interface GlyphController {
    fun connect(context: Context): Boolean
    fun setZoneBrightness(zone: Int, brightness: Int)
    fun setAllZones(brightness: IntArray)
    fun turnOffAll()
    fun disconnect()
}

class NothingGlyphController : GlyphController {

    private val tag = "GlyphController"
    private var glyphMatrixManager: GlyphMatrixManager? = null
    private var callback: GlyphMatrixManager.Callback? = null
    private var connected = false
    private val currentLevels = IntArray(EQUALIZER_BAND_COUNT)

    override fun connect(context: Context): Boolean {
        if (connected) return true

        val manager = GlyphMatrixManager.getInstance(context.applicationContext)
        val latch = CountDownLatch(1)
        glyphMatrixManager = manager
        callback = object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                try {
                    manager.register(Glyph.DEVICE_25111p)
                    connected = true
                    Log.i(tag, "Glyph Matrix SDK connected for Nothing Phone (4a) Pro")
                } catch (e: Exception) {
                    connected = false
                    Log.e(tag, "Failed to register Glyph Matrix SDK", e)
                } finally {
                    latch.countDown()
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                connected = false
                latch.countDown()
                Log.w(tag, "Glyph Matrix SDK service disconnected")
            }
        }

        return try {
            manager.init(callback)
            if (!latch.await(SERVICE_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.e(tag, "Timed out waiting for Glyph Matrix SDK service")
                false
            } else {
                connected
            }
        } catch (e: Exception) {
            connected = false
            Log.e(tag, "Failed to initialize Glyph Matrix SDK", e)
            false
        }
    }

    override fun setZoneBrightness(zone: Int, brightness: Int) {
        if (zone !in currentLevels.indices) return
        currentLevels[zone] = brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        setAllZones(currentLevels)
    }

    override fun setAllZones(brightness: IntArray) {
        if (!connected) return

        brightness.take(EQUALIZER_BAND_COUNT).forEachIndexed { index, level ->
            currentLevels[index] = level.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        }

        try {
            glyphMatrixManager?.setAppMatrixFrame(buildEqualizerMatrix(currentLevels))
        } catch (e: Exception) {
            Log.e(tag, "Failed to send Glyph Matrix frame", e)
        }
    }

    override fun turnOffAll() {
        if (!connected) return
        try {
            currentLevels.fill(MIN_BRIGHTNESS)
            glyphMatrixManager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.e(tag, "Failed to close Glyph Matrix frame", e)
        }
    }

    override fun disconnect() {
        try {
            if (connected) glyphMatrixManager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.e(tag, "Failed to close Glyph Matrix on disconnect", e)
        } finally {
            connected = false
            glyphMatrixManager?.unInit()
            glyphMatrixManager = null
            callback = null
        }
    }

    private fun buildEqualizerMatrix(levels: IntArray): IntArray {
        val frame = IntArray(MATRIX_SIDE * MATRIX_SIDE)
        levels.take(EQUALIZER_BAND_COUNT).forEachIndexed { band, level ->
            val startX = BAND_COLUMN_RANGES[band].first
            val endX = BAND_COLUMN_RANGES[band].last
            val litRows = ((level.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS) / MAX_BRIGHTNESS.toFloat()) * MATRIX_SIDE)
                .toInt()
                .coerceIn(0, MATRIX_SIDE)

            for (x in startX..endX) {
                for (y in MATRIX_SIDE - litRows until MATRIX_SIDE) {
                    frame[y * MATRIX_SIDE + x] = level.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
                }
            }
        }
        return frame
    }

    private companion object {
        private const val MATRIX_SIDE = 13
        private const val EQUALIZER_BAND_COUNT = 6
        private const val MIN_BRIGHTNESS = 0
        private const val MAX_BRIGHTNESS = 255
        private const val SERVICE_CONNECT_TIMEOUT_MS = 2_000L
        private val BAND_COLUMN_RANGES = listOf(
            0..1,
            2..3,
            4..5,
            6..7,
            8..9,
            10..12,
        )
    }
}
