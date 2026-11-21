import 'dart:async';
import 'dart:isolate';

import 'package:flutter/foundation.dart';

import '../models/car_video.dart';
import 'video_source.dart';
import 'youtube_service.dart';

typedef YouTubeApiFactory = YouTubeApi Function();

typedef _Task = Future<Object?> Function(_WorkerState state);

/// [YouTubeService] running in a background isolate.
///
/// youtube_explode_dart decodes YouTube's large JSON responses synchronously. On the
/// main isolate that froze the UI on the Galaxy A6+ for ~0.5 s at the first search,
/// ~0.85 s when a video starts and ~0.17 s per grid page; here the decoding, and the TLS
/// handshakes of these requests, run on the worker's own thread. The isolate starts with
/// the first request and is started again if it dies.
///
/// Search cursors stay in the worker, because youtube_explode's result lists hold the
/// HTTP client and cannot cross isolates; the UI side gets an id in [VideoPage.cursor].
class YouTubeWorker implements YouTubeApi {
  YouTubeWorker({YouTubeApiFactory create = YouTubeService.new}) : _create = create;

  static const _failed = 'Gagal memuat dari YouTube. Coba lagi.';

  final YouTubeApiFactory _create;
  final Map<int, Completer<Object?>> _pending = {};
  int _nextId = 0;
  Future<SendPort>? _port;
  Isolate? _isolate;
  ReceivePort? _replies;
  ReceivePort? _exits;
  bool _disposed = false;

  @override
  Future<VideoPage> search(String query) => _run(_searchTask(query));

  @override
  Future<VideoPage> nextPage(VideoPage page) {
    final cursor = page.cursor;
    if (cursor is! int) return Future.value(const VideoPage([]));
    return _run(_nextPageTask(cursor));
  }

  @override
  Future<ResolvedStreams> resolveStreams(CarVideo video, StreamQuality quality) =>
      _run(_resolveTask(video, quality));

  @override
  Future<List<SponsorSegment>> getSponsorSegments(String videoId) =>
      _run(_sponsorTask(videoId));

  // Tasks are built in static methods so each closure captures only its arguments; a
  // closure that captured `this` would drag ports and completers into the message.
  static _Task _searchTask(String query) => (state) => state.search(query);

  static _Task _nextPageTask(int cursor) => (state) => state.nextPage(cursor);

  static _Task _resolveTask(CarVideo video, StreamQuality quality) =>
      (state) => state.api.resolveStreams(video, quality);

  static _Task _sponsorTask(String videoId) =>
      (state) => state.api.getSponsorSegments(videoId);

  Future<T> _run<T>(_Task task) async {
    if (_disposed) throw const YouTubeServiceException(_failed);
    final port = await _start();
    final id = _nextId++;
    final reply = Completer<Object?>();
    _pending[id] = reply;
    port.send((id, task));
    return await reply.future as T;
  }

  Future<SendPort> _start() => _port ??= _spawn();

  Future<SendPort> _spawn() async {
    final replies = ReceivePort();
    final exits = ReceivePort();
    _replies = replies;
    _exits = exits;
    final ready = Completer<SendPort>();
    replies.listen((message) {
      if (message is SendPort) {
        ready.complete(message);
        return;
      }
      final (id, ok, value) = message as (int, bool, Object?);
      final reply = _pending.remove(id);
      if (reply == null) return;
      if (ok) {
        reply.complete(value);
      } else {
        reply.completeError(YouTubeServiceException(value as String));
      }
    });
    exits.listen((_) => _onWorkerExit(ready));
    try {
      _isolate = await Isolate.spawn(
        _workerMain,
        (replies.sendPort, _create),
        onExit: exits.sendPort,
        debugName: 'youtube',
      );
    } catch (e) {
      debugPrint('YouTube worker failed to start: $e');
      _reset();
      throw const YouTubeServiceException(_failed);
    }
    return ready.future;
  }

  /// A dead worker fails what it was doing; the next request starts a new one.
  void _onWorkerExit(Completer<SendPort> ready) {
    if (!_disposed) debugPrint('YouTube worker exited');
    if (!ready.isCompleted) {
      ready.completeError(const YouTubeServiceException(_failed));
    }
    _reset();
  }

  void _reset() {
    for (final reply in _pending.values) {
      reply.completeError(const YouTubeServiceException(_failed));
    }
    _pending.clear();
    _replies?.close();
    _exits?.close();
    _replies = null;
    _exits = null;
    _isolate = null;
    _port = null;
  }

  @override
  void dispose() {
    _disposed = true;
    _isolate?.kill(priority: Isolate.immediate);
    _reset();
  }
}

/// The worker's side: the real [YouTubeApi] and the search cursors it handed out.
class _WorkerState {
  _WorkerState(this.api);

  final YouTubeApi api;
  final Map<int, Object> _cursors = {};
  int _nextCursor = 0;

  Future<VideoPage> search(String query) async {
    // A new search replaces the grid, so older cursors can go.
    _cursors.clear();
    return _export(await api.search(query));
  }

  /// Cursors stay until the next search, so retrying a failed page works.
  Future<VideoPage> nextPage(int id) async {
    final cursor = _cursors[id];
    if (cursor == null) return const VideoPage([]);
    return _export(await api.nextPage(VideoPage(const [], cursor: cursor)));
  }

  VideoPage _export(VideoPage page) {
    final cursor = page.cursor;
    if (cursor == null) return VideoPage(page.videos);
    final id = _nextCursor++;
    _cursors[id] = cursor;
    return VideoPage(page.videos, cursor: id);
  }
}

void _workerMain((SendPort, YouTubeApiFactory) args) {
  final (replies, create) = args;
  final state = _WorkerState(create());
  final inbox = ReceivePort();
  replies.send(inbox.sendPort);
  inbox.listen((message) async {
    final (id, task) = message as (int, _Task);
    try {
      replies.send((id, true, await task(state)));
    } on YouTubeServiceException catch (e) {
      replies.send((id, false, e.message));
    } catch (e) {
      debugPrint('YouTube worker task failed: $e');
      replies.send((id, false, YouTubeWorker._failed));
    }
  });
}
