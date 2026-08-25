package com.example.glyphequalizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val RECORD_AUDIO_REQUEST_CODE = 42
    private var sensitivity = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggle = findViewById<Switch>(R.id.toggleVisualizer)
        val sensitivitySeekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        val sensitivityLabel = findViewById<TextView>(R.id.sensitivityLabel)
        val permissionNote = findViewById<TextView>(R.id.permissionNote)

        permissionNote.text = "This app needs the microphone permission only because " +
            "Android requires it for reading system audio playback data (Visualizer API). " +
            "It does not record or store any audio."

        // SeekBar 0-100 maps to sensitivity 0.2 - 3.0
        sensitivitySeekBar.progress = 40 // corresponds to ~1.0x
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivity = 0.2f + (progress / 100f) * 2.8f
                sensitivityLabel.text = "Sensitivity: %.1fx".format(sensitivity)
                if (toggle.isChecked) restartServiceWithNewSensitivity()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasRecordAudioPermission()) {
                    startVisualizerService()
                } else {
                    requestRecordAudioPermission()
                    toggle.isChecked = false
                }
            } else {
                stopVisualizerService()
            }
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            findViewById<Switch>(R.id.toggleVisualizer).isChecked = true
        }
    }

    private fun startVisualizerService() {
        val intent = Intent(this, GlyphVisualizerService::class.java).apply {
            action = GlyphVisualizerService.ACTION_START
            putExtra(GlyphVisualizerService.EXTRA_SENSITIVITY, sensitivity)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun restartServiceWithNewSensitivity() {
        // Simplest approach: just re-send start with updated sensitivity; service
        // recreates its BandMapper. Swap for a Binder+direct-call approach later if
        // start/stop churn turns out to be too jumpy in practice.
        startVisualizerService()
    }

    private fun stopVisualizerService() {
        val intent = Intent(this, GlyphVisualizerService::class.java).apply {
            action = GlyphVisualizerService.ACTION_STOP
        }
        startService(intent)
    }
}
