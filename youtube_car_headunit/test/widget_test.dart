import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:youtube_car_headunit/main.dart';
import 'package:youtube_car_headunit/models/car_video.dart';
import 'package:youtube_car_headunit/services/car_audio_handler.dart';
import 'package:youtube_car_headunit/services/settings_store.dart';
import 'package:youtube_car_headunit/services/video_source.dart';

class _Source implements VideoSource {
  _Source({this.error});

  final Object? error;

  @override
  Future<VideoPage> search(String query) async {
    if (error != null) throw error!;
    return const VideoPage([
      CarVideo(
        id: 'a',
        title: 'Lagu Perjalanan',
        uploader: 'Artis',
        thumbnail: '',
        duration: Duration(hours: 1, minutes: 5),
      ),
      CarVideo(
        id: 'b',
        title: 'Berita Live',
        uploader: 'TV',
        thumbnail: '',
        duration: Duration.zero,
        isLive: true,
      ),
    ]);
  }

  @override
  Future<VideoPage> nextPage(VideoPage page) async => const VideoPage([]);
}

Future<void> pumpApp(WidgetTester tester, VideoSource source) async {
  // The dashboard is designed for a landscape head unit.
  tester.view.physicalSize = const Size(2220, 1080);
  tester.view.devicePixelRatio = 2.625;
  addTearDown(tester.view.reset);
  SharedPreferences.setMockInitialValues({});
  final settings = await SettingsStore.load();
  await tester.pumpWidget(
    YouTubeCarApp(
      audioHandler: CarAudioHandler(),
      settings: settings,
      videoSource: source,
      platformServices: false,
    ),
  );
  await tester.pump();
}

/// Disposes the app so no stream or timer outlives the test.
Future<void> unmount(WidgetTester tester) => tester.pumpWidget(const SizedBox());

void main() {
  testWidgets('opens on the dashboard with the four menus', (tester) async {
    await pumpApp(tester, _Source());

    for (final label in ['Home', 'Maps', 'YouTube', 'Musik']) {
      expect(find.bySemanticsLabel(label), findsOneWidget);
    }
    expect(find.text('Belum ada yang diputar'), findsOneWidget);
    expect(find.text('Navigasi'), findsOneWidget);

    await unmount(tester);
  });

  testWidgets('YouTube menu shows the grid with hour durations and LIVE',
      (tester) async {
    await pumpApp(tester, _Source());

    await tester.tap(find.bySemanticsLabel('YouTube'));
    await tester.pumpAndSettle();

    expect(find.text('Lagu Perjalanan'), findsOneWidget);
    expect(find.text('1:05:00'), findsOneWidget);
    expect(find.text('LIVE'), findsOneWidget);

    await unmount(tester);
  });

  testWidgets('Back returns from a menu to the dashboard', (tester) async {
    await pumpApp(tester, _Source());

    await tester.tap(find.bySemanticsLabel('YouTube'));
    await tester.pumpAndSettle();
    expect(find.text('Lagu Perjalanan'), findsOneWidget);

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(find.text('Belum ada yang diputar'), findsOneWidget);
    // The YouTube menu stays mounted (state kept) but is no longer shown.
    expect(find.text('Lagu Perjalanan'), findsNothing);

    await unmount(tester);
  });

  testWidgets('a failed search shows its message and a retry button',
      (tester) async {
    await pumpApp(
      tester,
      _Source(error: const YouTubeServiceException('Tidak ada koneksi internet.')),
    );

    await tester.tap(find.bySemanticsLabel('YouTube'));
    await tester.pumpAndSettle();

    expect(find.text('Tidak ada koneksi internet.'), findsOneWidget);
    expect(find.text('Coba lagi'), findsOneWidget);

    await unmount(tester);
  });
}
