# Glyph Equalizer — Nothing Phone (4a)

A music-reactive equalizer for the Nothing Phone (4a)'s Glyph Bar. Splits live audio
into 6 frequency bands and maps them to the Nothing Phone (4a) Glyph Bar's A1-A6 channels.

## Status

**Audio pipeline: complete and functional.**
FFT capture, band-splitting, smoothing, and sensitivity control are all implemented
and don't depend on anything Nothing-specific — this part will work as-is.

**Glyph hardware binding: wired to the official Nothing Glyph SDK.**
`GlyphController.kt` now uses `GlyphManager`, registers `Glyph.DEVICE_25111`, opens a Glyph session, and toggles the Phone (4a) A1-A6 channel indexes from the live FFT levels. The SDK exposes channel on/off frames rather than per-channel analog brightness, so the equalizer maps brightness to active zones and pulse timing.

## Permissions explained

- **RECORD_AUDIO**: Required by Android's `Visualizer` API even though this app never
  records or stores audio — it only reads playback data from the system audio mix
  (`Visualizer(0)`). This is explained to the user in-app.
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_MEDIA_PLAYBACK**: Required to keep the
  visualizer running reliably while the screen is off or another app is in the
  foreground.
- **BIND_NOTIFICATION_LISTENER_SERVICE**: Only needed if you extend this to detect
  *which* app is playing via `MediaSessionManager`. Not currently used by the
  `Visualizer(0)` global-mix approach, but declared for future use. This permission
  can't be requested via a runtime dialog — users must enable it manually under
  Settings > Apps > Special access > Notification access.

## Nothing SDK setup

The app expects the official AAR from Nothing's Glyph Developer Kit at:

```
app/libs/glyph-matrix-sdk-2.0.aar
```

Download it from `Nothing-Developer-Programme/Glyph-Developer-Kit` and keep the manifest `NothingKey` meta-data set. Debug builds use `android:value="test"`; on-device debug mode can be enabled with:

```
adb shell settings put global nt_glyph_interface_debug_enable 1
```

## Known limitations

- Glyph must be enabled in system settings (Settings > Glyph Interface) for anything
  to light up, regardless of what this app does.
- The visualizer auto-stops below 15% battery (configurable in
  `GlyphVisualizerService.LOW_BATTERY_CUTOFF_PERCENT`).
- Color is not adjustable — the Glyph Bar's LEDs are white-only by hardware design
  (plus one fixed red status LED), so only brightness/pattern is controllable.
- `captureSize` is set to the device's max supported value; if this proves too
  CPU-heavy on the Snapdragon 7s Gen 4, drop to a mid-range fixed size (e.g. 1024)
  instead of `getCaptureSizeRange()[1]`.

## Project structure

```
app/src/main/java/com/example/glyphequalizer/
  MainActivity.kt            - toggle + sensitivity UI
  GlyphVisualizerService.kt  - foreground service, owns the Visualizer lifecycle
  BandMapper.kt               - FFT -> 6-band brightness math for Phone (4a)
  GlyphController.kt          - official Nothing Glyph SDK wrapper
app/src/main/res/layout/activity_main.xml
app/src/main/AndroidManifest.xml
app/build.gradle.kts
```
