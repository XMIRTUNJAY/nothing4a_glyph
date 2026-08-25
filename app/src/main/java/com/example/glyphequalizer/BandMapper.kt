package com.example.glyphequalizer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Converts raw FFT bytes from android.media.audiofx.Visualizer into 6 brightness
 * values (0-255), one per Phone (4a) Glyph Bar zone, roughly bass -> treble.
 *
 * Visualizer.getFft() returns interleaved real/imaginary pairs. We compute magnitude
 * per bin, group bins into 6 logarithmically-spaced bands (bass gets more bins than
 * treble, matching how music energy is actually distributed), then smooth the result
 * so the LEDs don't flicker on every single audio frame.
 */
class BandMapper(
    private val zoneCount: Int = 6,
    private val smoothingFactor: Float = 0.55f, // 0 = no smoothing, 1 = frozen
    private val sensitivity: Float = 1.0f        // user-adjustable gain, 0.2 - 3.0
) {

    // Holds the previous frame's brightness per zone, for smoothing.
    private val previousLevels = FloatArray(zoneCount)

    /**
     * @param fft raw output of Visualizer.getFft() for the current audio frame
     * @return brightness values 0-255 for each of the 6 Phone (4a) Glyph zones
     */
    fun mapFftToZones(fft: ByteArray): IntArray {
        val bandCount = zoneCount
        val magnitudes = computeMagnitudes(fft)
        val bandRanges = logSpacedBandRanges(magnitudes.size, bandCount)

        val result = IntArray(bandCount)
        for (band in 0 until bandCount) {
            val (start, end) = bandRanges[band]
            var sum = 0f
            for (i in start until end) sum += magnitudes[i]
            val avg = if (end > start) sum / (end - start) else 0f

            // Normalize (magnitudes from getFft() are roughly 0-128 in practice, but this
            // varies by device/stream loudness, so we clamp defensively) and apply gain.
            val normalized = min(1f, (avg / 128f) * sensitivity)
            val target = normalized * 255f

            // Exponential smoothing so zones ease up/down instead of flickering.
            val smoothed = previousLevels[band] * smoothingFactor +
                target * (1 - smoothingFactor)
            previousLevels[band] = smoothed

            result[band] = smoothed.toInt().coerceIn(0, 255)
        }
        return result
    }

    private fun computeMagnitudes(fft: ByteArray): FloatArray {
        // fft[0] = DC (real), fft[1] = Nyquist (real). From index 2 onward it's
        // interleaved (re, im) pairs per Visualizer docs.
        val binCount = (fft.size - 2) / 2
        val magnitudes = FloatArray(binCount)
        for (i in 0 until binCount) {
            val re = fft[2 * i + 2].toFloat()
            val im = fft[2 * i + 3].toFloat()
            magnitudes[i] = kotlin.math.sqrt(re * re + im * im)
        }
        return magnitudes
    }

    /**
     * Bass carries more perceptual weight in visualizers, so give lower bands more bins
     * (log spacing) rather than splitting the spectrum evenly.
     */
    private fun logSpacedBandRanges(binCount: Int, bands: Int): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        val logMax = kotlin.math.ln(binCount.toDouble())
        var prevIndex = 0
        for (b in 1..bands) {
            val fraction = b.toDouble() / bands
            val idx = kotlin.math.exp(logMax * fraction).toInt().coerceIn(prevIndex + 1, binCount)
            ranges.add(prevIndex to idx)
            prevIndex = idx
        }
        return ranges
    }

    fun updateSensitivity(newSensitivity: Float) {
        // Exposed for the UI slider; caller should recreate BandMapper with new value,
        // this class is intentionally immutable-ish for the audio-thread hot path.
    }
}
