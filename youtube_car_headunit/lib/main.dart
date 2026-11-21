import 'dart:io';

import 'package:audio_service/audio_service.dart';
import 'package:audio_session/audio_session.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import 'controllers/drive_simulator.dart';
import 'controllers/feed_controller.dart';
import 'controllers/location_controller.dart';
import 'controllers/navigation_controller.dart';
import 'controllers/playback_controller.dart';
import 'screens/headunit_screen.dart';
import 'services/car_audio_handler.dart';
import 'services/recent_destinations.dart';
import 'services/settings_store.dart';
import 'services/video_source.dart';
import 'services/voice_guidance.dart';
import 'services/youtube_worker.dart';
import 'theme/car_theme.dart';
import 'utils/frame_log.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  startFrameLog();
  HttpOverrides.global = _CarHttpOverrides();

  // Force landscape for the head unit.
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);

  applyCarSystemUi();

  // Keep the screen awake while driving.
  try {
    await WakelockPlus.enable();
  } catch (e) {
    debugPrint('Wakelock error: $e');
  }

  // Music focus: navigation prompts and calls duck or pause us, and we pause others.
  try {
    final session = await AudioSession.instance;
    await session.configure(const AudioSessionConfiguration.music());
  } catch (e) {
    debugPrint('AudioSession configure failed: $e');
  }

  final settings = await SettingsStore.load();
  final navigationStore = await NavigationStore.load();
  final audioHandler = await _initAudioService();
  runApp(
    YouTubeCarApp(
      audioHandler: audioHandler,
      settings: settings,
      navigationStore: navigationStore,
    ),
  );
}

Future<CarAudioHandler> _initAudioService() async {
  try {
    return await AudioService.init(
      builder: CarAudioHandler.new,
      config: const AudioServiceConfig(
        androidNotificationChannelId:
            'com.carheadunit.youtube_car_headunit.playback',
        androidNotificationChannelName: 'Pemutaran',
        // Staying in the foreground while paused lets a steering-wheel "play" resume from
        // the background; Android 12+ refuses to restart a foreground service from there.
        androidStopForegroundOnPause: false,
      ),
    );
  } catch (e) {
    // Without a session the app still plays; only system media controls are missing.
    debugPrint('AudioService.init failed: $e');
    return CarAudioHandler();
  }
}

class YouTubeCarApp extends StatefulWidget {
  const YouTubeCarApp({
    super.key,
    required this.audioHandler,
    required this.settings,
    this.videoSource,
    this.navigationStore,
    this.platformServices = true,
  });

  final CarAudioHandler audioHandler;
  final SettingsStore settings;

  /// Recent destinations, route provider and avoid-tolls choice; tests run without one.
  final NavigationStore? navigationStore;

  /// Overrides where the grid gets videos from (tests); defaults to YouTube.
  final VideoSource? videoSource;

  /// False in widget tests: no location plugin and no network for map tiles.
  final bool platformServices;

  @override
  State<YouTubeCarApp> createState() => _YouTubeCarAppState();
}

class _YouTubeCarAppState extends State<YouTubeCarApp> {
  late final YouTubeWorker _youtube = YouTubeWorker();
  late final FeedController _feed =
      FeedController(widget.videoSource ?? _youtube);
  late final PlaybackController _playback = PlaybackController(
    youtube: _youtube,
    handler: widget.audioHandler,
    settings: widget.settings,
  );
  late final LocationController _location =
      LocationController(enabled: widget.platformServices);
  late final NavigationController _navigation = NavigationController(
    location: _location,
    store: widget.navigationStore,
  );

  /// Dev builds with --dart-define=SIMULATE_DRIVE=true drive every started route, so
  /// guidance can be checked on the phone without moving; compiled out otherwise.
  DriveSimulator? _simulator;

  @override
  void initState() {
    super.initState();
    if (kSimulateDrive) {
      _simulator = DriveSimulator(location: _location, navigation: _navigation)
        ..attach();
    }
  }

  @override
  void dispose() {
    _simulator?.dispose();
    _navigation.dispose();
    _location.dispose();
    _playback.dispose();
    _feed.dispose();
    _youtube.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'YouTube Car',
      debugShowCheckedModeBanner: false,
      theme: CarTheme.darkTheme,
      home: HeadunitScreen(
        playback: _playback,
        feed: _feed,
        settings: widget.settings,
        location: _location,
        navigation: _navigation,
        // Widget tests have no TTS engine.
        speech: widget.platformServices ? null : const SilentSpeech(),
        tilesEnabled: widget.platformServices,
      ),
    );
  }
}

/// Thumbnails and map tiles load through Dart's HttpClient on the UI isolate, and its TLS
/// handshakes run on that isolate's thread. Unlimited, a grid fling opened a connection
/// (and a handshake) per thumbnail; six per host are reused instead, as browsers do.
/// Overrides are per isolate, so YouTubeWorker's requests are not limited.
class _CarHttpOverrides extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) =>
      super.createHttpClient(context)..maxConnectionsPerHost = 6;
}
