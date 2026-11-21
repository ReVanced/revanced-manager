import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/voice_guidance.dart';
import 'package:youtube_car_headunit/widgets/navigation/nav_format.dart';

class _RecordingSpeech implements SpeechEngine {
  _RecordingSpeech({this.available = true});

  final bool available;
  final List<String> spoken = [];
  int stops = 0;
  final Completer<void> ready = Completer();

  @override
  Future<bool> init() async {
    await ready.future;
    return available;
  }

  @override
  Future<void> speak(String text) async => spoken.add(text);

  @override
  Future<void> stop() async => stops++;

  @override
  Future<void> dispose() async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('FlutterTtsSpeech', () {
    const channel = MethodChannel('flutter_tts');
    late List<MethodCall> calls;

    /// [indonesian] per engine: whether it has an id-ID voice.
    void mockEngine(Map<String, bool> indonesian, {String engine = 'samsung'}) {
      calls = [];
      var current = engine;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        switch (call.method) {
          case 'getDefaultEngine':
            return engine;
          case 'getEngines':
            return indonesian.keys.toList();
          case 'setEngine':
            current = call.arguments as String;
            return 1;
          case 'isLanguageAvailable':
            return indonesian[current] ?? false;
          case 'getDefaultVoice':
            return {'name': 'id-id-x-ide-local', 'locale': 'id-ID'};
          default:
            return 1;
        }
      });
      addTearDown(() => TestDefaultBinaryMessengerBinding
          .instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null));
    }

    List<String> methods() => [for (final c in calls) c.method];

    test('sets Indonesian with navigation audio before speaking', () async {
      mockEngine({'samsung': true});
      final speech = FlutterTtsSpeech();

      expect(await speech.init(), isTrue);
      await speech.speak('Belok kiri');

      expect(methods(), [
        'getDefaultEngine',
        'isLanguageAvailable',
        'setLanguage',
        'setAudioAttributesForNavigation',
        'awaitSpeakCompletion',
        'setQueueMode',
        'getDefaultVoice',
        'speak',
      ]);
      expect(calls[1].arguments, 'id-ID');
      expect(calls[2].arguments, 'id-ID');
    });

    test('falls back to Google TTS when the default engine lacks Indonesian',
        () async {
      mockEngine({'samsung': false, FlutterTtsSpeech.googleEngine: true});

      expect(await FlutterTtsSpeech().init(), isTrue);
      final setEngine = calls.singleWhere((c) => c.method == 'setEngine');
      expect(setEngine.arguments, FlutterTtsSpeech.googleEngine);
      expect(methods().indexOf('setLanguage'),
          greaterThan(methods().indexOf('setEngine')));
    });

    test('reports no voice when no engine has Indonesian', () async {
      mockEngine({'samsung': false});
      expect(await FlutterTtsSpeech().init(), isFalse);
      expect(methods(), isNot(contains('setLanguage')));
    });
  });

  group('VoiceGuidance', () {
    test('speaks cue text once the engine is ready, nothing while muted', () async {
      final cues = StreamController<VoiceCue>.broadcast();
      final speech = _RecordingSpeech();
      final voice = VoiceGuidance(cues: cues.stream, engine: speech);
      addTearDown(voice.dispose);

      cues.add(const VoiceCue(VoiceCueKind.depart, 'Mulai perjalanan'));
      await pumpEventQueue();
      expect(speech.spoken, isEmpty, reason: 'engine still starting');

      speech.ready.complete();
      await pumpEventQueue();
      expect(speech.spoken, ['Mulai perjalanan']);

      voice.toggleMute();
      expect(voice.muted.value, isTrue);
      expect(speech.stops, 1, reason: 'muting cuts the prompt being spoken');
      cues.add(const VoiceCue(VoiceCueKind.approach, 'Dalam 300 meter, belok kiri'));
      await pumpEventQueue();
      expect(speech.spoken, ['Mulai perjalanan']);

      voice.toggleMute();
      cues.add(const VoiceCue(VoiceCueKind.maneuver, 'Belok kiri'));
      await pumpEventQueue();
      expect(speech.spoken, ['Mulai perjalanan', 'Belok kiri']);
      await cues.close();
    });

    test('stays silent without an Indonesian voice', () async {
      final cues = StreamController<VoiceCue>.broadcast();
      final speech = _RecordingSpeech(available: false)..ready.complete();
      final voice = VoiceGuidance(cues: cues.stream, engine: speech);
      addTearDown(voice.dispose);

      cues.add(const VoiceCue(VoiceCueKind.arrive, 'Anda telah tiba di tujuan'));
      await pumpEventQueue();
      expect(speech.spoken, isEmpty);
      await cues.close();
    });
  });

  test('distances on screen snap to round steps with a decimal comma', () {
    expect(formatDistance(0), '0 m');
    expect(formatDistance(43), '40 m');
    expect(formatDistance(96), '100 m');
    expect(formatDistance(337), '350 m');
    expect(formatDistance(980), '1,0 km');
    expect(formatDistance(1234), '1,2 km');
    expect(formatDistance(12400), '12 km');
  });

  test('travel time rounds up to whole minutes', () {
    expect(formatTravelTime(const Duration(seconds: 20)), '1 mnt');
    expect(formatTravelTime(const Duration(minutes: 7, seconds: 1)), '8 mnt');
    expect(formatTravelTime(const Duration(hours: 1, minutes: 5)), '1 j 05 mnt');
  });
}
