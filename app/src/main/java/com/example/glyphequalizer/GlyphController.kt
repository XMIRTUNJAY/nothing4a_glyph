package com.example.glyphequalizer

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Controls the Nothing Phone (4a) Glyph bar through the official Glyph SDK. */
interface GlyphController {
    fun connect(context: Context): Boolean
    fun setZoneBrightness(zone: Int, brightness: Int)
    fun setAllZones(brightness: IntArray)
    fun turnOffAll()
    fun disconnect()
}

class NothingGlyphController : GlyphController {

    private val tag = "GlyphController"
    private var glyphManager: GlyphManager? = null
    private var callback: GlyphManager.Callback? = null
    private var connected = false
    private var sessionOpen = false
    private val currentLevels = IntArray(PHONE_4A_ZONE_COUNT)

    override fun connect(context: Context): Boolean {
        if (connected && sessionOpen) return true

        val manager = GlyphManager.getInstance(context.applicationContext)
        val latch = CountDownLatch(1)
        glyphManager = manager
        callback = object : GlyphManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                try {
                    val device = phone4aTargetDevice()
                    if (device == null) {
                        Log.e(tag, "This build is intended for Nothing Phone (4a); unsupported device")
                        connected = false
                    } else {
                        connected = manager.register(device)
                        if (connected) {
                            manager.openSession()
                            sessionOpen = true
                            Log.i(tag, "Glyph SDK session opened for Nothing Phone (4a)")
                        } else {
                            Log.e(tag, "Glyph SDK register() rejected this application")
                        }
                    }
                } catch (e: GlyphException) {
                    Log.e(tag, "Failed to register/open Glyph SDK session", e)
                    connected = false
                    sessionOpen = false
                } finally {
                    latch.countDown()
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                sessionOpen = false
                connected = false
                latch.countDown()
                Log.w(tag, "Glyph SDK service disconnected")
            }
        }

        return try {
            manager.init(callback)
            if (!latch.await(SERVICE_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.e(tag, "Timed out waiting for Glyph SDK service")
                false
            } else {
                connected && sessionOpen
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Glyph SDK", e)
            connected = false
            sessionOpen = false
            false
        }
    }

    override fun setZoneBrightness(zone: Int, brightness: Int) {
        if (zone !in currentLevels.indices) return
        currentLevels[zone] = brightness
        setAllZones(currentLevels)
    }

    override fun setAllZones(brightness: IntArray) {
        if (!connected || !sessionOpen) return

        brightness.take(PHONE_4A_ZONE_COUNT).forEachIndexed { index, level ->
            currentLevels[index] = level.coerceIn(0, MAX_BRIGHTNESS)
        }
        val frame = buildPhone4aFrame(currentLevels)
        try {
            if (frame == null) {
                currentLevels.fill(0)
                glyphManager?.turnOff()
            } else {
                glyphManager?.toggle(frame)
            }
        } catch (e: GlyphException) {
            Log.e(tag, "Failed to update Glyph channels", e)
        }
    }

    override fun turnOffAll() {
        if (!connected || !sessionOpen) return
        try {
            currentLevels.fill(0)
            glyphManager?.turnOff()
        } catch (e: GlyphException) {
            Log.e(tag, "Failed to turn off Glyph channels", e)
        }
    }

    override fun disconnect() {
        try {
            if (sessionOpen) glyphManager?.closeSession()
        } catch (e: GlyphException) {
            Log.e(tag, "Failed to close Glyph SDK session", e)
        } finally {
            sessionOpen = false
            connected = false
            glyphManager?.unInit()
            glyphManager = null
            callback = null
        }
    }

    private fun buildPhone4aFrame(brightness: IntArray): GlyphFrame? {
        val manager = glyphManager ?: return null
        val normalized = brightness.take(PHONE_4A_ZONE_COUNT).map { it.coerceIn(0, MAX_BRIGHTNESS) }
        val activeChannels = normalized.withIndex().filter { it.value > OFF_THRESHOLD }
        if (activeChannels.isEmpty()) return null

        val builder = manager.getGlyphFrameBuilder()
        activeChannels.forEach { (index, _) -> builder.buildChannel(index) }
        val strongest = normalized.maxOrNull() ?: 0
        val period = MIN_PERIOD_MS +
            ((MAX_BRIGHTNESS - strongest) / MAX_BRIGHTNESS.toFloat() * PERIOD_RANGE_MS).roundToInt()
        return builder
            .buildPeriod(period)
            .buildCycles(1)
            .buildInterval(MIN_INTERVAL_MS)
            .build()
    }

    private fun phone4aTargetDevice(): String? = when {
        Common.is25111() -> Glyph.DEVICE_25111
        else -> null
    }

    private companion object {
        private const val PHONE_4A_ZONE_COUNT = 6
        private const val MAX_BRIGHTNESS = 255
        private const val OFF_THRESHOLD = 8
        private const val SERVICE_CONNECT_TIMEOUT_MS = 2_000L
        private const val MIN_PERIOD_MS = 33
        private const val PERIOD_RANGE_MS = 120
        private const val MIN_INTERVAL_MS = 0
    }
}
