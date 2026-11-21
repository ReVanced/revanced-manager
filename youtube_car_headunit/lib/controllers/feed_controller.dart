import 'package:flutter/foundation.dart';

import '../models/car_video.dart';
import '../services/video_source.dart';
import '../utils/frame_log.dart';

/// The video grid: trending, search, paging and errors.
class FeedController extends ChangeNotifier {
  FeedController(this._source);

  static const String trendingQuery = 'Musik Indonesia trending';

  /// Paging stops here to bound memory on low-end phones.
  static const int maxItems = 120;

  final VideoSource _source;

  List<CarVideo> _videos = const [];
  VideoPage? _lastPage;
  String _query = trendingQuery;
  bool _loading = false;
  bool _loadingMore = false;
  String? _error;
  String? _loadMoreError;
  int _generation = 0;
  bool _disposed = false;

  List<CarVideo> get videos => _videos;

  String get query => _query;

  bool get isTrending => _query == trendingQuery;

  bool get isLoading => _loading;

  bool get isLoadingMore => _loadingMore;

  /// Why the last search failed; the grid shows it with a retry button.
  String? get error => _error;

  /// Why loading the next page failed; shown as a retry tile under the grid.
  String? get loadMoreError => _loadMoreError;

  bool get hasMore =>
      (_lastPage?.hasMore ?? false) &&
      _loadMoreError == null &&
      _videos.length < maxItems;

  Future<void> loadTrending() => search(trendingQuery);

  Future<void> search(String query) async {
    final q = query.trim().isEmpty ? trendingQuery : query.trim();
    final generation = ++_generation;
    _query = q;
    _loading = true;
    _loadingMore = false;
    _error = null;
    _loadMoreError = null;
    _notify();
    try {
      final page = await _source.search(q);
      if (generation != _generation) return;
      _videos = List.unmodifiable(_dedupe(const [], page.videos));
      _lastPage = page;
    } catch (e) {
      if (generation != _generation) return;
      _videos = const [];
      _lastPage = null;
      _error = e is YouTubeServiceException ? e.message : 'Gagal memuat video.';
    } finally {
      if (generation == _generation) {
        _loading = false;
        _notify();
      }
    }
  }

  Future<void> loadMore() async {
    final page = _lastPage;
    if (page == null || !hasMore || _loading || _loadingMore) return;
    final generation = _generation;
    _loadingMore = true;
    _notify();
    try {
      final watch = kFrameLog ? (Stopwatch()..start()) : null;
      final next = await _source.nextPage(page);
      if (watch != null) debugPrint('PAGE loaded in ${watch.elapsedMilliseconds} ms');
      if (generation != _generation) return;
      _videos = List.unmodifiable(_dedupe(_videos, next.videos));
      _lastPage = next;
    } catch (e) {
      if (generation != _generation) return;
      _loadMoreError =
          e is YouTubeServiceException ? e.message : 'Gagal memuat lagi.';
    } finally {
      if (generation == _generation) {
        _loadingMore = false;
        _notify();
      }
    }
  }

  Future<void> retryLoadMore() async {
    _loadMoreError = null;
    await loadMore();
  }

  Future<void> retry() => search(_query);

  /// YouTube repeats some videos across pages; keep the first occurrence.
  static List<CarVideo> _dedupe(List<CarVideo> existing, List<CarVideo> added) {
    final seen = existing.map((v) => v.id).toSet();
    return [...existing, ...added.where((v) => seen.add(v.id))];
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
