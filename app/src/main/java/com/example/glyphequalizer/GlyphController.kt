package com.example.glyphequalizer

import android.content.Context
import android.util.Log

/**
 * Thin wrapper around the Nothing Glyph Developer Kit (GDK).
 *
 * IMPORTANT: The exact GDK class/method names below are NOT verified against Nothing's
 * current public SDK for the Phone 4a's Glyph Bar. Method names are written to match the
 * *shape* of Nothing's known GDK pattern (register -> open session -> set channel
 * brightness -> close), based on their publicly documented GDK for older Glyph Matrix
 * devices. Before running this on a real 4a:
 *   1. Pull the actual GDK AAR/repo (search "Nothing Glyph Developer Kit GitHub").
 *   2. Replace the TODO-marked calls below with the real method signatures.
 *   3. Confirm the Phone 4a's 7 zones are addressable individually (vs. only as one bar) -
 *      the public teasers describe 6 square zones + 1 red status LED, so treat 6 as the
 *      controllable equalizer zone count, not 7, until confirmed on-device.
 */
interface GlyphController {
    fun connect(context: Context): Boolean
    fun setZoneBrightness(zone: Int, brightness: Int)
    fun setAllZones(brightness: IntArray)
    fun turnOffAll()
    fun disconnect()
}

class NothingGlyphController : GlyphController {

    private val tag = "GlyphController"
    private var connected = false

    // TODO: replace with real GDK object, e.g. `private var glyphManager: GlyphManager? = null`
    // once the actual SDK is pulled in via build.gradle.

    override fun connect(context: Context): Boolean {
        return try {
            // TODO: real GDK init sequence looks roughly like:
            //   GlyphManager.getInstance(context)
            //   glyphManager.init(callback)
            //   glyphManager.register(Common.getDevice()) // confirm Phone 4a device constant
            //   glyphManager.openSession()
            Log.w(tag, "GDK not wired up yet - stub connect() returning true for dev/testing")
            connected = true
            connected
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Glyph service", e)
            connected = false
            connected
        }
    }

    override fun setZoneBrightness(zone: Int, brightness: Int) {
        if (!connected) return
        // TODO: real call likely something like:
        //   val builder = GlyphFrame.Builder()
        //   builder.buildChannel(zone, brightness)
        //   glyphManager.toggle(builder.build())
        Log.d(tag, "STUB setZoneBrightness(zone=$zone, brightness=$brightness)")
    }

    override fun setAllZones(brightness: IntArray) {
        if (!connected) return
        // Prefer batching all zones into a single GlyphFrame per update rather than
        // calling setZoneBrightness in a loop, once the real GDK builder API is wired in -
        // batched updates avoid visible per-zone lag across the bar.
        for (zone in brightness.indices) {
            setZoneBrightness(zone, brightness[zone])
        }
    }

    override fun turnOffAll() {
        if (!connected) return
        setAllZones(IntArray(6)) // 6 controllable equalizer zones; the 7th LED is the
        // fixed red recording/notification indicator and should not be driven by music.
    }

    override fun disconnect() {
        // TODO: real teardown likely: glyphManager.closeSession(); glyphManager.unInit()
        Log.d(tag, "STUB disconnect()")
        connected = false
    }
}
