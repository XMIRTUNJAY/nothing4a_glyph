# Glyph Equalizer — Nothing Phone (4a) Pro

A music-reactive equalizer for the Nothing Phone (4a) Pro's Glyph Matrix. Splits live audio
into 6 frequency bands and maps them to the Nothing Phone (4a) Pro's 13x13 Glyph Matrix columns.

## Status

**Audio pipeline: complete and functional.**
FFT capture, band-splitting, smoothing, and sensitivity control are all implemented
and don't depend on anything Nothing-specific — this part will work as-is.

**Glyph hardware binding: wired to the official Nothing Glyph Matrix SDK.**
`GlyphController.kt` now uses `GlyphMatrixManager`, registers `Glyph.DEVICE_25111p`, and sends Phone (4a) Pro 13x13 matrix frames from the live FFT levels with `setAppMatrixFrame`.

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

The app expects the official AAR from Nothing's Glyph Matrix Developer Kit at:

```
app/libs/glyph-matrix-sdk-2.0.aar
```

Download it from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` and keep the manifest `NothingKey` meta-data set. Debug builds use `android:value="test"`; on-device debug mode can be enabled with:

```
adb shell settings put global nt_glyph_interface_debug_enable 1
```

## Build and device testing

For Android Studio, import/open the repository root, not just the `app/` directory. Use JDK 17 or 21 in Android Studio (`File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`). The project cannot complete an Android Studio/Gradle build until the official AAR exists at `app/libs/glyph-matrix-sdk-2.0.aar`.

To test on a Nothing Phone (4a) Pro:

```
adb shell settings put global nt_glyph_interface_debug_enable 1
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open the app in the foreground, grant the audio permission, start playback in any music app, and toggle Glyph Equalizer on.

If you want a local installable APK from the command line, run:

```
./scripts/download_nothing_sdk.sh
JAVA_HOME=/path/to/jdk17 ./scripts/build_debug_apk.sh
```

The script verifies that `app/libs/glyph-matrix-sdk-2.0.aar` exists before invoking Gradle and prints the generated `app/build/outputs/apk/debug/app-debug.apk` path when the build succeeds.

## Known limitations

- Glyph must be enabled in system settings (Settings > Glyph Interface) for anything
  to light up, regardless of what this app does.
- The visualizer auto-stops below 15% battery (configurable in
  `GlyphVisualizerService.LOW_BATTERY_CUTOFF_PERCENT`).
- Color is not adjustable — the Glyph Matrix LEDs are monochrome, so only brightness/pattern is controllable.
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
