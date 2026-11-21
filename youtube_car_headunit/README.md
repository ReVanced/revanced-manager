# YouTube Car (youtube_car_headunit)

Flutter Android app that turns a phone in the car into a head unit: a dashboard with a
live map and what is playing, and four menus (Home, Maps, YouTube, Musik). YouTube plays
without ads, with SponsorBlock auto-skip, Audio Only, picture-in-picture, background
playback and control from steering-wheel or Bluetooth media buttons. Turn-by-turn
navigation runs in the app (search, route preview with alternatives, Indonesian voice,
rerouting, live traffic with Mapbox), with Google Maps as a hand-off. Tuned for mid-range
phones such as the Galaxy A6+ (Android 10).

## Screens

- **Home**: clock and date in the menu rail, a dark map following the car, the
  now-playing card (next item, seek bar, big controls) and quick actions (Trending, Live,
  Cari). **Navigasi** opens the destination search in Maps; while guiding, the map card
  shows the route and the next turn.
- **Maps**: full map with zoom and recenter, destination search, route preview and
  guidance (see Navigation); "Google Maps" hands off, with a playing video moved to PiP.
- **YouTube**: search, categories, grid with paging, compact inline player, quality sheet,
  fullscreen.
- **Musik**: large artwork, seek bar, big transport controls, Audio Only and autoplay
  switches, what plays next. Swipe left/right to skip.

Back closes fullscreen or a sheet, then returns to Home; on Home it sends the app to the
background and playback continues.

## Run

```sh
flutter pub get
flutter run --dart-define-from-file=mapbox.local.json   # debug on the connected device
flutter test           # unit + widget tests
flutter analyze
```

`mapbox.local.json` holds the Mapbox token (`{"MAPBOX_TOKEN": "pk..."}`) and is gitignored;
never commit it. Without it the app still runs and routes with OSRM only. Release builds
need the same flag: `flutter build apk --release --dart-define-from-file=mapbox.local.json`.

Requires Flutter 3.35 or newer. PiP needs Android 8.0+.

## Release build and signing

Release builds are signed with the key described in `android/key.properties`
(gitignored, never commit it or the keystore):

```properties
storePassword=<store password>
keyPassword=<key password>
keyAlias=upload
storeFile=/absolute/path/to/upload-keystore.jks
```

Create a keystore once with
`keytool -genkey -v -keystore ~/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`.
Without `key.properties`, `flutter build apk --release` still works but signs with the
debug key and Gradle prints a warning. An APK signed with one key cannot be installed over
one signed with another; uninstall first (this erases the app's settings).

## Navigation

- **Destination:** type a name or address (Photon search, results while typing, nearby
  places first), long-press the map for a point (named by reverse geocoding), or pick a
  recent destination.
- **Preview:** route line, distance, time and arrival for up to three routes (a TOL tag
  marks toll roads), "Hindari tol", the route provider (Mapbox or OSM), Mulai, Google Maps.
- **Guidance:** maneuver banner with the next turn after it, a turn tile in the menu rail
  in every menu, arrival time and remaining time/distance, Akhiri. North-up driving view
  with the car placed so the road ahead stays on screen; the compass button turns
  heading-up on for that drive (it costs about twice the GPU time on the A6+). Leaving the
  route recalculates it; the arrival card ends guidance. Guidance keeps running in the
  background behind a "Navigasi aktif" notification.
- **Providers:** Mapbox Directions (`driving-traffic`, live traffic, congestion colors,
  Indonesian instructions, ETA refreshed every 5 minutes) when a token is set; OSRM public
  servers otherwise or when Mapbox fails. Avoiding tolls works with Mapbox only; the
  public OSRM servers reject it. Search is Photon in both cases: Mapbox's terms tie its POI
  results to Mapbox maps and forbid storing them, which recent destinations need. Mapbox
  routes are kept in memory only (its terms forbid storing Navigation API results).
- **Voice:** Indonesian prompts through Google TTS when the default engine (Samsung TTS
  on the A6+) has no Indonesian voice; music and video duck under each prompt; the
  speaker button mutes.
- **Testing without driving:** a build with `--dart-define=SIMULATE_DRIVE=true` drives
  every started route at 40 km/h (`SIMULATE_DETOUR_S=<n>` leaves the route once after n
  seconds). Android 10 cannot take test locations from the shell.

## Map, location and rendering

- Map tiles are standard OpenStreetMap tiles shown dark on the device (no API key; the
  attribution stays visible). OSM's tile policy fits personal use: real User-Agent, no
  bulk download, no simulated retina.
- The location permission is asked only when the driver taps "Izinkan lokasi"; GPS stops
  while the app is in the background. Fixes are requested every second (Android's default
  is 5 s) and the car marker and camera glide from one fix to the next.
- The dark look is one color filter over the whole tile layer, not one per tile.
- Impeller is disabled in the manifest (Skia is used): on the A6+'s Adreno 506 Vulkan
  driver Impeller drew widget borders under modal sheets on top of the sheet.
- The phone's status bar is hidden (the rail shows the clock) and landscape is locked in
  the manifest; both are reapplied when Android recreates the activity, because the
  Flutter engine is cached by audio_service and `main()` does not run again.

## How it works

```
lib/
  main.dart                      AudioService + AudioSession init, app root
  controllers/
    playback_controller.dart     the single owner of playback
    feed_controller.dart         trending, search, paging, errors
    location_controller.dart     GPS permission, position stream, background tracking
    navigation_controller.dart   destination, routes, guidance, reroute, voice cues
    drive_simulator.dart         dev-only simulated drive (SIMULATE_DRIVE)
  models/                        CarVideo, navigation models (Place, NavRoute, ...)
  services/
    geocoding_service.dart       Photon search + reverse, nearby first
    routing_service.dart         Mapbox Directions / OSRM, fallback, rate limits
    route_instructions.dart      Indonesian maneuver text for OSRM routes
    polyline.dart, geo.dart      polyline6 decoding, distances and projection
    recent_destinations.dart     recents, route provider, avoid-tolls choice
    voice_guidance.dart          flutter_tts with Indonesian voice and ducking
    youtube_service.dart         search + stream resolution (youtube_explode_dart)
    youtube_worker.dart          runs youtube_service in a background isolate
    stream_selection.dart        HLS variant choice + one-variant master playlist
    car_audio_handler.dart       media-session bridge, owns the Audio Only player
    settings_store.dart          quality, autoplay, last video + position
    video_source.dart            VideoSource interface for the feed
    platform_bridge.dart         PiP and minimize channel (see MainActivity.kt)
  screens/                       shell (headunit_screen) and the four sections
  widgets/                       rail, map, now-playing, player, grid, seek bar
  widgets/navigation/            search panel, route preview, banner, guidance bar
  theme/car_theme.dart           design tokens and shared components
  utils/                         formatting, SponsorBlock tracker, frame log
```

**YouTube off the UI thread.** youtube_explode_dart decodes YouTube's large JSON responses
synchronously; on the A6+ that froze the UI for ~0.5 s at the first search, ~0.85 s per
video start and ~0.17 s per grid page. `YouTubeWorker` runs `YouTubeService` in a
background isolate and the app only receives plain results.

**Playback.** `PlaybackController` owns the queue, the current video, the active player
(video_player for video, just_audio for Audio Only, never both), SponsorBlock, autoplay,
quality and resume state. Every menu, Fullscreen, PiP and the media session all
go through it, so SponsorBlock and autoplay behave the same everywhere.

**Media session.** `AudioService.init` registers `CarAudioHandler`; `MainActivity` extends
`AudioServiceActivity` so the session survives the activity. Steering-wheel/Bluetooth
buttons, the notification and the lockscreen send play, pause, next, previous, seek and
stop to the controller. The service stays in the foreground while paused so a
steering-wheel "play" can resume from the background (Android 12+ forbids restarting it
there). Video keeps playing with the screen off (`allowBackgroundPlayback`).

**Streams** (measured with youtube_explode_dart 2.5.3 on 2026-11-21):

| Quality | Source | What plays |
|---|---|---|
| 360p | `android` client manifest (~0.7 s) | itag 18, the only muxed progressive stream |
| 480p / 720p | `ios` player response `hlsManifestUrl` (~0.5 s) | HLS: the variants are video-only, so the app builds a one-variant master (H.264 variant + its audio rendition) and hands it to ExoPlayer as a `data:` URI. Only some videos have an ios HLS master |
| Audio Only | `android` client manifest | best audio-only stream (opus/aac) |
| Live | `android` player response `hlsManifestUrl` | HLS, not seekable; ios and the web watch page omit the master for live |

The HLS masters are read from the player response directly (`VideoController`, an
internal youtube_explode_dart class) because `getManifest` HEAD-checks the first adaptive
stream, which YouTube often answers with 403 for the ios client, and then retries for about
15 s. Search uses `searchContent` and maps results itself, because 2.5.3's `search()`
throws on result pages that contain live videos.

If HD is not offered the app falls back to 360p and says so; if video fails it plays the
audio. SponsorBlock skips `sponsor`, `intro`, `outro`, `selfpromo` and `music_offtopic`,
each segment once per video.

## Measuring smoothness on the phone

- Frame pacing as the screen shows it: `adb shell dumpsys SurfaceFlinger --latency-clear`,
  do the gesture, then `adb shell "dumpsys SurfaceFlinger --latency '<layer>'"` with the
  app's `SurfaceView - com.carheadunit...` layer from `dumpsys SurfaceFlinger --list`.
  Gaps between present times in 16.7 ms steps are missed refreshes. The buffer holds the
  last 127 frames.
- Frame costs: build with `--dart-define=FRAME_LOG=true` and read `adb logcat` (tag
  `flutter`): build/raster times per batch, frames over 16.7 ms, and `STALL` lines when
  the UI isolate was busy for more than 50 ms. The code is compiled out without the flag.

## Known limits

- `lib/services/youtube_service.dart` imports an internal youtube_explode_dart class; recheck
  it when upgrading the package (3.x changes client defaults).
- YouTube rate-limits bursts of searches from one network; the app reports it and a retry
  later works.
- Stream extraction breaks whenever YouTube changes its player; updating
  youtube_explode_dart is the fix.
- HD depends on YouTube offering an ios HLS master for the video; otherwise 360p plays and
  the app says so.
- While a video plays in a menu that does not show it (Home, Maps), Flutter still redraws
  the screen for every video frame (~3.5 ms of GPU time each on the A6+). Audio saja
  avoids it on long drives.
- The first time a screen or sheet is drawn after the app starts, that frame can take
  50-100 ms (shader compilation, image and glyph uploads); later openings are smooth.
