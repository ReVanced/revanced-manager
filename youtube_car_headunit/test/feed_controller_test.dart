import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/controllers/feed_controller.dart';
import 'package:youtube_car_headunit/models/car_video.dart';
import 'package:youtube_car_headunit/services/video_source.dart';

CarVideo video(String id) => CarVideo(
      id: id,
      title: 'Video $id',
      uploader: 'Uploader',
      thumbnail: '',
      duration: const Duration(minutes: 3),
    );

/// Serves pages of ids; `pages[query]` lists the pages for that query.
class FakeSource implements VideoSource {
  FakeSource(this.pages);

  final Map<String, List<List<String>>> pages;
  Object? searchError;
  Object? nextPageError;
  final List<String> searched = [];

  @override
  Future<VideoPage> search(String query) async {
    searched.add(query);
    if (searchError != null) throw searchError!;
    return _page(query, 0);
  }

  @override
  Future<VideoPage> nextPage(VideoPage page) async {
    if (nextPageError != null) throw nextPageError!;
    final (query, index) = page.cursor! as (String, int);
    return _page(query, index + 1);
  }

  VideoPage _page(String query, int index) {
    final list = pages[query] ?? const [];
    if (index >= list.length) return const VideoPage([]);
    return VideoPage(
      list[index].map(video).toList(),
      cursor: index + 1 < list.length ? (query, index) : null,
    );
  }
}

void main() {
  test('trending loads into videos', () async {
    final source = FakeSource({
      FeedController.trendingQuery: [
        ['a', 'b'],
      ],
    });
    final feed = FeedController(source);
    await feed.loadTrending();
    expect(feed.videos.map((v) => v.id), ['a', 'b']);
    expect(feed.isTrending, isTrue);
    expect(feed.error, isNull);
    expect(feed.hasMore, isFalse);
  });

  test('empty query means trending', () async {
    final source = FakeSource({});
    final feed = FeedController(source);
    await feed.search('   ');
    expect(source.searched, [FeedController.trendingQuery]);
  });

  test('a failed search shows its message, not a fake fallback list', () async {
    final source = FakeSource({})
      ..searchError = const YouTubeServiceException('Tidak ada koneksi.');
    final feed = FeedController(source);
    await feed.search('lofi');
    expect(feed.videos, isEmpty);
    expect(feed.error, 'Tidak ada koneksi.');
    expect(feed.isLoading, isFalse);

    source.searchError = null;
    source.pages['lofi'] = [
      ['x'],
    ];
    await feed.retry();
    expect(feed.error, isNull);
    expect(feed.videos.single.id, 'x');
  });

  test('loadMore appends the next page and drops repeats', () async {
    final source = FakeSource({
      'musik': [
        ['a', 'b'],
        ['b', 'c'],
      ],
    });
    final feed = FeedController(source);
    await feed.search('musik');
    expect(feed.hasMore, isTrue);
    await feed.loadMore();
    expect(feed.videos.map((v) => v.id), ['a', 'b', 'c']);
    expect(feed.hasMore, isFalse);
  });

  test('a failed page keeps the list and offers a retry', () async {
    final source = FakeSource({
      'musik': [
        ['a'],
        ['b'],
      ],
    });
    final feed = FeedController(source);
    await feed.search('musik');
    source.nextPageError = const YouTubeServiceException('Gagal.');
    await feed.loadMore();
    expect(feed.videos.map((v) => v.id), ['a']);
    expect(feed.loadMoreError, 'Gagal.');
    expect(feed.hasMore, isFalse);

    source.nextPageError = null;
    await feed.retryLoadMore();
    expect(feed.videos.map((v) => v.id), ['a', 'b']);
    expect(feed.loadMoreError, isNull);
  });

  test('a newer search wins over a slower older one', () async {
    final source = _SlowSource();
    final feed = FeedController(source);
    final slow = feed.search('slow');
    await feed.search('fast');
    source.gate.complete();
    await slow;
    expect(feed.query, 'fast');
    expect(feed.videos.single.id, 'fast');
    expect(feed.isLoading, isFalse);
  });
}

/// Holds the 'slow' search until [gate] completes.
class _SlowSource implements VideoSource {
  final Completer<void> gate = Completer<void>();

  @override
  Future<VideoPage> search(String query) async {
    if (query == 'slow') await gate.future;
    return VideoPage([video(query)]);
  }

  @override
  Future<VideoPage> nextPage(VideoPage page) async => const VideoPage([]);
}
