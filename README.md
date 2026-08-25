# Glyph Equalizer — Nothing Phone (4a)

A music-reactive equalizer for the Nothing Phone (4a)'s Glyph Bar. Splits live audio
into 7 frequency bands and maps them to the Glyph Bar's 6 controllable white LED zones
(the 7th LED is a fixed red status light and is intentionally left alone).

## Status

**Audio pipeline: complete and functional.**
FFT capture, band-splitting, smoothing, and sensitivity control are all implemented
and don't depend on anything Nothing-specific — this part will work as-is.

**Glyph hardware binding: stubbed, not yet wired to the real SDK.**
`GlyphController.kt` contains a `NothingGlyphController` class with TODO-marked stub
methods instead of real Nothing GDK calls. This was done deliberately rather than
guessing at method signatures, since the Phone 4a's Glyph Bar API may differ from
older Glyph Matrix/strip SDK examples found online.

## What you need to do before this runs for real

1. **Find the current Nothing Glyph Developer Kit.**
   Search "Nothing Glyph Developer Kit GitHub" or check Nothing's developer site.
   Confirm whether it's a Maven dependency or a local `.aar` file.

2. **Confirm the Phone 4a device constant / zone count.**
   Public teasers describe 6 white square zones + 1 red LED, but the SDK may address
   them differently (e.g. as one combined "channel 0-5" or with a device-specific
   enum). Update `GlyphController.turnOffAll()` and `setAllZones()` if the real
   zone count or indexing differs from what's assumed here.

3. **Replace the TODO blocks in `GlyphController.kt`** with real GDK calls:
   - `connect()` → real init/register/openSession sequence
   - `setZoneBrightness()` / `setAllZones()` → real GlyphFrame builder + toggle call
   - `disconnect()` → real closeSession/unInit calls

4. **Add the dependency** in `app/build.gradle.kts` where marked.

Everything else (UI, service lifecycle, permission handling, battery cutoff,
FFT-to-band math) should not need changes once the SDK is wired in.

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
  BandMapper.kt               - FFT -> 7-band brightness math (device-agnostic, done)
  GlyphController.kt          - Glyph hardware wrapper (STUBBED, needs real SDK)
app/src/main/res/layout/activity_main.xml
app/src/main/AndroidManifest.xml
app/build.gradle.kts
```
