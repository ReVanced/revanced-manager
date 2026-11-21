import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_tts/flutter_tts.dart';

import '../models/navigation.dart';

/// Text-to-speech as navigation needs it. The Android implementation (flutter_tts) speaks
/// Indonesian with navigation audio attributes and transient-may-duck focus, so music and
/// video duck under a prompt instead of pausing.
abstract interface class SpeechEngine {
  /// Prepares the engine; false when no Indonesian voice is available.
  Future<bool> init();

  /// Speaks [text], interrupting whatever is being spoken.
  Future<void> speak(String text);

  Future<void> stop();

  Future<void> dispose();
}

/// Speaks nothing: tests, and builds without a speech engine.
class SilentSpeech implements SpeechEngine {
  const SilentSpeech();

  @override
  Future<bool> init() async => false;

  @override
  Future<void> speak(String text) async {}

  @override
  Future<void> stop() async {}

  @override
  Future<void> dispose() async {}
}

/// Speaks the navigation controller's voice cues unless muted.
///
/// The controller decides when a cue is due and writes its text; this class only speaks
/// it. A newer cue interrupts an older one still being spoken, because the newer one is
/// always closer to the maneuver.
class VoiceGuidance {
  VoiceGuidance({
    required Stream<VoiceCue> cues,
    SpeechEngine engine = const SilentSpeech(),
    bool muted = false,
  })  : _engine = engine,
        muted = ValueNotifier(muted) {
    _ready = _init();
    _subscription = cues.listen(_onCue);
    this.muted.addListener(_onMuted);
  }

  final SpeechEngine _engine;
  late final Future<bool> _ready;
  late final StreamSubscription<VoiceCue> _subscription;

  /// True while prompts are silenced; the guidance bar's toggle flips it.
  final ValueNotifier<bool> muted;

  void toggleMute() => muted.value = !muted.value;

  Future<bool> _init() async {
    try {
      return await _engine.init();
    } catch (e) {
      debugPrint('Voice guidance unavailable: $e');
      return false;
    }
  }

  Future<void> _onCue(VoiceCue cue) async {
    if (muted.value) return;
    if (!await _ready) return;
    // Muted while the engine was still starting.
    if (muted.value) return;
    try {
      await _engine.speak(cue.text);
    } catch (e) {
      debugPrint('Voice prompt failed: $e');
    }
  }

  void _onMuted() {
    if (muted.value) _engine.stop().catchError((Object _) {});
  }

  Future<void> dispose() async {
    await _subscription.cancel();
    muted.dispose();
    try {
      await _engine.dispose();
    } catch (_) {}
  }
}

/// The device's speech engine.
SpeechEngine createSpeechEngine() => FlutterTtsSpeech();

/// Android's text-to-speech through flutter_tts, in Indonesian.
///
/// Prompts play with the navigation-guidance usage, and each one holds
/// transient-may-duck audio focus (flutter_tts requests it with `focus: true` and
/// abandons it when the utterance ends or is stopped). Our own playback ducks under it:
/// the audio session is configured as music without pause-when-ducked, so Android lowers
/// just_audio itself, and ExoPlayer in video_player ducks movie content on that loss.
class FlutterTtsSpeech implements SpeechEngine {
  FlutterTtsSpeech([FlutterTts? tts]) : _tts = tts ?? FlutterTts();

  static const language = 'id-ID';

  /// Installed on most Android phones and has Indonesian; Samsung's own engine, the
  /// default on the A6+, may not.
  static const googleEngine = 'com.google.android.tts';

  final FlutterTts _tts;

  @override
  Future<bool> init() async {
    final engine = await _tts.getDefaultEngine;
    var indonesian = await _tts.isLanguageAvailable(language) == true;
    debugPrint('Voice: engine $engine, $language available: $indonesian');
    if (!indonesian && engine != googleEngine) {
      final engines = await _tts.getEngines;
      if (engines is List && engines.contains(googleEngine)) {
        // Resolves once the new engine is bound and initialised.
        await _tts.setEngine(googleEngine);
        indonesian = await _tts.isLanguageAvailable(language) == true;
        debugPrint('Voice: engine $googleEngine, $language available: $indonesian');
      }
    }
    if (!indonesian) {
      // Stay silent rather than read Indonesian text with another language's voice.
      debugPrint('Voice: no $language voice, prompts stay silent');
      return false;
    }
    await _tts.setLanguage(language);
    await _tts.setAudioAttributesForNavigation();
    // speak() returns at once; a newer prompt flushes an older one (queue mode 0).
    await _tts.awaitSpeakCompletion(false);
    await _tts.setQueueMode(0);
    debugPrint('Voice: ready, default voice ${await _tts.getDefaultVoice}');
    return true;
  }

  @override
  Future<void> speak(String text) async {
    debugPrint('Voice: $text');
    await _tts.speak(text, focus: true);
  }

  @override
  Future<void> stop() async {
    await _tts.stop();
  }

  @override
  Future<void> dispose() async {
    await _tts.stop();
  }
}
