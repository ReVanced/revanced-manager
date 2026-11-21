import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:youtube_car_headunit/models/car_video.dart';
import 'package:youtube_car_headunit/services/settings_store.dart';

void main() {
  test('defaults when nothing is stored', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await SettingsStore.load();
    expect(store.quality, isNull);
    expect(store.autoplay, isTrue);
    expect(store.lastVideo, isNull);
    expect(store.lastPosition, Duration.zero);
  });

  test('quality, autoplay and last video survive a reload', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await SettingsStore.load();
    await store.setQuality('720p');
    await store.setAutoplay(false);
    await store.setLastVideo(
      const CarVideo(
        id: 'abc',
        title: 'Lagu',
        uploader: 'Artis',
        thumbnail: 'https://i.ytimg.com/vi/abc/mqdefault.jpg',
        duration: Duration(minutes: 4, seconds: 2),
        isLive: true,
      ),
    );
    await store.setLastPosition(const Duration(seconds: 95));

    final reloaded = await SettingsStore.load();
    expect(reloaded.quality, '720p');
    expect(reloaded.autoplay, isFalse);
    final video = reloaded.lastVideo!;
    expect(video.id, 'abc');
    expect(video.title, 'Lagu');
    expect(video.uploader, 'Artis');
    expect(video.duration, const Duration(minutes: 4, seconds: 2));
    expect(video.isLive, isTrue);
    expect(reloaded.lastPosition, const Duration(seconds: 95));
  });

  test('a corrupt last video is ignored instead of crashing startup', () async {
    SharedPreferences.setMockInitialValues({'last_video': '{not json'});
    final store = await SettingsStore.load();
    expect(store.lastVideo, isNull);
  });
}
