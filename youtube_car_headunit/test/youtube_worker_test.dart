import 'dart:isolate';

import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/models/car_video.dart';
import 'package:youtube_car_headunit/services/video_source.dart';
import 'package:youtube_car_headunit/services/youtube_service.dart';
import 'package:youtube_car_headunit/services/youtube_worker.dart';

CarVideo _video(String id) => CarVideo(
      id: id,
      title: 'Video $id',
      uploader: 'Channel',
      thumbnail: 'https://i.ytimg.com/vi/$id/mqdefault.jpg',
      duration: const Duration(minutes: 3),
    );

/// Runs inside the worker isolate. Its cursors are ReceivePorts, which cannot be sent
/// between isolates: a cursor leaking out of the worker would fail the test.
class _FakeApi implements YouTubeApi {
  final List<ReceivePort> _ports = [];

  @override
  Future<VideoPage> search(String query) async {
    switch (query) {
      case 'error':
        throw const YouTubeServiceException('Tidak ada koneksi internet.');
      case 'crash':
        throw StateError('parser bug');
      case 'exit':
        Isolate.current.kill(priority: Isolate.immediate);
    }
    return VideoPage([_video('$query-1')], cursor: _cursor());
  }

  @override
  Future<VideoPage> nextPage(VideoPage page) async {
    // The first cursor handed out leads to page 2, the second to page 3 (the last).
    final index = _ports.indexOf(page.cursor! as ReceivePort) + 2;
    return VideoPage(
      [_video('p$index')],
      cursor: index < 3 ? _cursor() : null,
    );
  }

  ReceivePort _cursor() {
    final port = ReceivePort();
    _ports.add(port);
    return port;
  }

  @override
  Future<ResolvedStreams> resolveStreams(CarVideo video, StreamQuality quality) async =>
      ResolvedStreams(
        label: quality.label,
        videoUri: Uri.parse('https://example.com/${video.id}.mp4'),
      );

  @override
  Future<List<SponsorSegment>> getSponsorSegments(String videoId) async =>
      const [SponsorSegment(category: 'sponsor', start: 1, end: 5)];

  @override
  void dispose() {}
}

YouTubeApi _createFake() => _FakeApi();

void main() {
  late YouTubeWorker worker;

  setUp(() => worker = YouTubeWorker(create: _createFake));
  tearDown(() => worker.dispose());

  test('search and paging run in the worker; cursors stay there', () async {
    final first = await worker.search('lagu');
    expect(first.videos.single.id, 'lagu-1');
    expect(first.cursor, isA<int>());

    final second = await worker.nextPage(first);
    expect(second.videos.single.id, 'p2');
    final third = await worker.nextPage(second);
    expect(third.videos.single.id, 'p3');
    expect(third.hasMore, isFalse);

    // Retrying a page (after a failed load) uses the same cursor again.
    expect((await worker.nextPage(first)).videos.single.id, 'p2');
  });

  test('streams and sponsor segments come back typed', () async {
    final streams = await worker.resolveStreams(_video('abc'), StreamQuality.p720);
    expect(streams.label, '720p');
    expect(streams.videoUri, Uri.parse('https://example.com/abc.mp4'));

    final segments = await worker.getSponsorSegments('abc');
    expect(segments.single.end, 5);
  });

  test('failures arrive as messages for the driver', () async {
    await expectLater(
      worker.search('error'),
      throwsA(isA<YouTubeServiceException>()
          .having((e) => e.message, 'message', 'Tidak ada koneksi internet.')),
    );
    await expectLater(
      worker.search('crash'),
      throwsA(isA<YouTubeServiceException>().having(
          (e) => e.message, 'message', 'Gagal memuat dari YouTube. Coba lagi.')),
    );
    // The worker survives failed tasks.
    expect((await worker.search('lagi')).videos.single.id, 'lagi-1');
  });

  test('a worker that dies fails its request and is started again', () async {
    await expectLater(
      worker.search('exit'),
      throwsA(isA<YouTubeServiceException>()),
    );
    expect((await worker.search('lagu')).videos.single.id, 'lagu-1');
  });
}
