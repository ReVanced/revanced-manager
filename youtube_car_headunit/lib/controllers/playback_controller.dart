import 'dart:async';

import 'package:audio_service/audio_service.dart';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart' as ja;
import 'package:video_player/video_player.dart';

import '../models/car_video.dart';
import '../services/car_audio_handler.dart';
import '../services/platform_bridge.dart';
import '../services/settings_store.dart';
import '../services/video_source.dart';
import '../services/youtube_service.dart';
import '../utils/sponsor_skip.dart';

enum PlaybackMode { idle, video, audio }

@immutable
class PlaybackProgress {
  const PlaybackProgress(this.position, this.duration);

  static const zero = PlaybackProgress(Duration.zero, Duration.zero);

  final Duration position;

  /// Zero for live streams and while unknown; seek bars treat it as "not seekable".
  final Duration duration;

  @override
  bool operator ==(Object other) =>
      other is PlaybackProgress &&
      other.position == position &&
      other.duration == duration;

  @override
  int get hashCode => Object.hash(position, duration);
}

/// Owns playback: queue, current video, the active player, SponsorBlock, autoplay,
/// quality and resume state.
///
/// Every screen and the media session drive playback through this one object, so the
/// behavior is identical in every menu, Fullscreen and PiP. Only one decoder
/// runs at a time: the old player is released before a new one starts, and a generation
/// counter drops the results of selections that were superseded meanwhile.
class PlaybackController extends ChangeNotifier implements MediaCommands {
  PlaybackController({
    required StreamSource youtube,
    required CarAudioHandler handler,
    required SettingsStore settings,
    PlatformBridge? pip,
  })  : _youtube = youtube,
        _handler = handler,
        _settings = settings,
        _pip = pip ?? PlatformBridge(),
        _quality = StreamQuality.fromLabel(settings.quality),
        _autoplay = settings.autoplay {
    if (!_quality.isAudioOnly) _lastVideoQuality = _quality;
    _handler.commands = this;
    _subscriptions
      ..add(_audio.playerStateStream.listen(_onAudioState))
      ..add(
        _audio
            .createPositionStream(
              minPeriod: const Duration(milliseconds: 250),
              maxPeriod: const Duration(milliseconds: 500),
            )
            .listen(_onAudioPosition),
      );
  }

  static const _positionSaveInterval = Duration(seconds: 10);
  static const _sponsorNoticeDuration = Duration(seconds: 2);

  final StreamSource _youtube;
  final CarAudioHandler _handler;
  final SettingsStore _settings;
  final PlatformBridge _pip;
  final List<StreamSubscription<Object?>> _subscriptions = [];
  final SponsorSkipTracker _skipper = SponsorSkipTracker();
  final StreamController<String> _messages =
      StreamController<String>.broadcast();

  /// Position and duration, changed at most once per second so only seek bars rebuild.
  final ValueNotifier<PlaybackProgress> progress =
      ValueNotifier(PlaybackProgress.zero);

  /// Short "skipped sponsor" notice, cleared after a moment.
  final ValueNotifier<String?> sponsorNotice = ValueNotifier(null);

  List<CarVideo> _queue = const [];
  CarVideo? _current;
  VideoPlayerController? _video;
  PlaybackMode _mode = PlaybackMode.idle;
  ResolvedStreams? _streams;
  List<SponsorSegment> _segments = const [];
  StreamQuality _quality;

  /// Quality to return to when Audio Only is switched off.
  StreamQuality _lastVideoQuality = StreamQuality.p360;
  bool _autoplay;
  bool _loading = false;
  String? _error;
  int _generation = 0;
  bool _completionHandled = false;
  bool _lastVideoPlaying = false;
  bool _lastVideoBuffering = false;
  DateTime _lastPositionSave = DateTime.fromMillisecondsSinceEpoch(0);
  Timer? _sponsorNoticeTimer;
  bool _disposed = false;

  ja.AudioPlayer get _audio => _handler.player;

  CarVideo? get current => _current;

  /// The list next/previous walk through.
  List<CarVideo> get queue => _queue;

  /// The next [count] queue items after the current video, wrapping like [next] does.
  List<CarVideo> upNext(int count) {
    final current = _current;
    if (_queue.length < 2 || current == null) return const [];
    final index = _queue.indexWhere((v) => v.id == current.id);
    return [
      for (var i = 1; i <= count && i < _queue.length; i++)
        _queue[(index + i) % _queue.length],
    ];
  }

  VideoPlayerController? get videoController => _video;

  PlaybackMode get mode => _mode;

  StreamQuality get quality => _quality;

  bool get autoplay => _autoplay;

  bool get isLoading => _loading;

  /// Message of the last failure for the current video, null while things work.
  String? get error => _error;

  /// One-off messages for a snackbar (fallbacks, failures, quality changes).
  Stream<String> get messages => _messages.stream;

  bool get isLive => _current?.isLive ?? false;

  /// What is actually served (`720p`, `360p`, `LIVE`, `Audio`); the chosen quality while
  /// loading.
  String get servedLabel =>
      _loading ? _quality.label : (_streams?.label ?? _quality.label);

  bool get isAudioOnly =>
      _mode == PlaybackMode.audio || (_loading && _quality.isAudioOnly);

  bool get hasVideo =>
      _mode == PlaybackMode.video && (_video?.value.isInitialized ?? false);

  bool get canSeek =>
      !isLive &&
      _mode != PlaybackMode.idle &&
      progress.value.duration > Duration.zero;

  bool get isPlaying => switch (_mode) {
        PlaybackMode.video => _video?.value.isPlaying ?? false,
        PlaybackMode.audio => _audio.playing,
        PlaybackMode.idle => false,
      };

  Duration get position => switch (_mode) {
        PlaybackMode.video => _video?.value.position ?? Duration.zero,
        PlaybackMode.audio => _audio.position,
        PlaybackMode.idle => progress.value.position,
      };

  // ------------------------------------------------------------- selection

  /// Plays [video]; [queue] becomes the list next/previous walk through.
  Future<void> select(
    CarVideo video, {
    List<CarVideo>? queue,
    Duration? startAt,
  }) async {
    if (queue != null) _queue = List.unmodifiable(queue);
    final generation = ++_generation;
    final start = startAt != null && startAt > Duration.zero && !video.isLive
        ? startAt
        : null;

    final oldVideo = _detachPlayers();
    _current = video;
    _streams = null;
    _segments = const [];
    _skipper.reset();
    _completionHandled = false;
    _loading = true;
    _error = null;
    progress.value = PlaybackProgress(
      start ?? Duration.zero,
      video.isLive ? Duration.zero : video.duration,
    );
    _notify();
    _handler.setNowPlaying(video);
    _handler.publish(
      playing: false,
      processing: AudioProcessingState.loading,
      position: start ?? Duration.zero,
      canSeek: false,
    );
    unawaited(_settings.setLastVideo(video));
    unawaited(_settings.setLastPosition(start ?? Duration.zero));

    await _releasePlayers(oldVideo);
    if (_isStale(generation)) return;

    try {
      final results = await Future.wait<Object>([
        _youtube.resolveStreams(video, _quality),
        if (!video.isLive) _youtube.getSponsorSegments(video.id),
      ]);
      if (_isStale(generation)) return;
      final streams = results[0] as ResolvedStreams;
      _streams = streams;
      _segments = results.length > 1
          ? results[1] as List<SponsorSegment>
          : const <SponsorSegment>[];
      if (streams.downgraded) {
        _say('${_quality.label} tidak tersedia, memutar ${streams.label}');
      }

      if (!_quality.isAudioOnly) {
        final videoUri = streams.videoUri;
        if (videoUri != null &&
            await _startVideo(videoUri, streams.videoIsHls, generation, start)) {
          return;
        }
        if (_isStale(generation)) return;
        _say('Video gagal dimuat, memutar Audio Only');
      }

      final audioUri = streams.audioUri;
      if (audioUri == null) {
        throw const YouTubeServiceException(
          'Stream tidak tersedia. Coba video lain.',
        );
      }
      await _startAudio(
        audioUri,
        streams.audioIsHls,
        generation,
        start,
        fallback: streams.audioFallbackUri,
      );
    } on YouTubeServiceException catch (e) {
      _fail(generation, e.message);
    } catch (e) {
      debugPrint('Playback failed for ${video.id}: $e');
      _fail(generation, 'Gagal memuat video. Coba lagi.');
    }
  }

  Future<bool> _startVideo(
    Uri uri,
    bool isHls,
    int generation,
    Duration? start,
  ) async {
    final controller = VideoPlayerController.networkUrl(
      uri,
      formatHint: isHls ? VideoFormat.hls : null,
      httpHeaders: kStreamHeaders,
      // Keeps sound when the screen turns off or the driver switches apps without PiP;
      // the media-session foreground service keeps the process alive meanwhile.
      videoPlayerOptions: VideoPlayerOptions(allowBackgroundPlayback: true),
    );
    try {
      await controller.initialize();
      if (start != null) await controller.seekTo(start);
    } catch (e) {
      debugPrint('Video init failed: $e');
      await controller.dispose();
      return false;
    }
    if (_isStale(generation)) {
      await controller.dispose();
      return false;
    }
    // No await between the stale check and taking ownership: a newer select() must find
    // this controller in _video to release it.
    _video = controller;
    _mode = PlaybackMode.video;
    _loading = false;
    _lastVideoPlaying = false;
    _lastVideoBuffering = false;
    controller.addListener(_onVideoTick);
    _handler.setNowPlaying(_current!, duration: controller.value.duration);
    _pip.setAutoPip(true);
    _notify();
    unawaited(controller.play());
    return true;
  }

  Future<void> _startAudio(
    Uri uri,
    bool isHls,
    int generation,
    Duration? start, {
    Uri? fallback,
  }) async {
    final source =
        isHls ? ja.HlsAudioSource(uri) : ja.ProgressiveAudioSource(uri);
    try {
      await _audio.setAudioSource(source, initialPosition: start);
    } on ja.PlayerInterruptedException {
      return; // A newer selection stopped this load.
    } on ja.PlayerException catch (e) {
      if (fallback == null || _isStale(generation)) rethrow;
      debugPrint('Audio stream refused ($e), trying the fallback stream');
      try {
        await _audio.setAudioSource(
          ja.ProgressiveAudioSource(fallback),
          initialPosition: start,
        );
      } on ja.PlayerInterruptedException {
        return;
      }
    }
    if (_isStale(generation)) return;
    _mode = PlaybackMode.audio;
    _loading = false;
    final duration = _audio.duration ?? _current!.duration;
    _handler.setNowPlaying(_current!, duration: duration);
    progress.value = PlaybackProgress(
      start ?? Duration.zero,
      isLive ? Duration.zero : duration,
    );
    _notify();
    // just_audio's play() completes only when playback stops; never await it here.
    unawaited(_audio.play());
  }

  /// Detaches the players synchronously so no widget builds with a controller that is
  /// about to be disposed. Returns the video controller to release.
  VideoPlayerController? _detachPlayers() {
    final old = _video;
    old?.removeListener(_onVideoTick);
    _video = null;
    _mode = PlaybackMode.idle;
    _pip.setAutoPip(false);
    return old;
  }

  Future<void> _releasePlayers(VideoPlayerController? oldVideo) async {
    try {
      await oldVideo?.dispose();
    } catch (e) {
      debugPrint('Video dispose failed: $e');
    }
    try {
      await _audio.stop();
    } catch (e) {
      debugPrint('Audio stop failed: $e');
    }
  }

  void _fail(int generation, String message) {
    if (_isStale(generation)) return;
    unawaited(_releasePlayers(_detachPlayers()));
    _loading = false;
    _error = message;
    _handler.publish(
      playing: false,
      processing: AudioProcessingState.error,
      position: progress.value.position,
      canSeek: false,
    );
    _say(message);
    _notify();
  }

  // ------------------------------------------------------------- listeners

  void _onVideoTick() {
    final controller = _video;
    if (controller == null || _mode != PlaybackMode.video) return;
    final value = controller.value;
    if (value.hasError) {
      debugPrint('Video playback error: ${value.errorDescription}');
      _fail(_generation, 'Pemutaran terputus. Tekan Ulangi untuk lanjut.');
      return;
    }
    if (!value.isInitialized) return;
    _checkSponsor(value.position);
    _updateProgress(value.position, value.duration);
    if (value.isCompleted) _onCompleted();
    if (value.isPlaying != _lastVideoPlaying ||
        value.isBuffering != _lastVideoBuffering) {
      _lastVideoPlaying = value.isPlaying;
      _lastVideoBuffering = value.isBuffering;
      _publishVideoState();
      _notify();
    }
  }

  void _onAudioState(ja.PlayerState state) {
    if (_mode != PlaybackMode.audio) return;
    _publishAudioState();
    if (state.processingState == ja.ProcessingState.completed) {
      _onCompleted();
    }
    _notify();
  }

  void _onAudioPosition(Duration position) {
    if (_mode != PlaybackMode.audio) return;
    _checkSponsor(position);
    _updateProgress(position, _audio.duration ?? Duration.zero);
  }

  void _onCompleted() {
    if (_completionHandled || _loading) return;
    _completionHandled = true;
    _saveNow();
    if (_autoplay && _queue.isNotEmpty) unawaited(next());
  }

  void _checkSponsor(Duration position) {
    if (_segments.isEmpty || _loading) return;
    final segment = _skipper.segmentToSkip(position, _segments);
    if (segment == null) return;
    unawaited(seek(Duration(milliseconds: (segment.end * 1000).round())));
    _showSponsorNotice('Skip ${sponsorCategoryLabel(segment.category)}');
  }

  void _updateProgress(Duration position, Duration duration) {
    final next = PlaybackProgress(
      Duration(seconds: position.inSeconds),
      isLive || duration.isNegative ? Duration.zero : duration,
    );
    if (progress.value != next) progress.value = next;
    if (DateTime.now().difference(_lastPositionSave) >= _positionSaveInterval) {
      _saveNow();
    }
  }

  void _publishVideoState() {
    final value = _video?.value;
    if (value == null) return;
    _handler.publish(
      playing: value.isPlaying,
      processing: !value.isInitialized
          ? AudioProcessingState.loading
          : value.isBuffering
              ? AudioProcessingState.buffering
              : value.isCompleted
                  ? AudioProcessingState.completed
                  : AudioProcessingState.ready,
      position: value.position,
      buffered: value.buffered.isEmpty ? Duration.zero : value.buffered.last.end,
      canSeek: canSeek,
    );
  }

  void _publishAudioState() {
    final state = _audio.playerState;
    _handler.publish(
      playing: state.playing,
      processing: switch (state.processingState) {
        ja.ProcessingState.idle => AudioProcessingState.idle,
        ja.ProcessingState.loading => AudioProcessingState.loading,
        ja.ProcessingState.buffering => AudioProcessingState.buffering,
        ja.ProcessingState.ready => AudioProcessingState.ready,
        ja.ProcessingState.completed => AudioProcessingState.completed,
      },
      position: _audio.position,
      buffered: _audio.bufferedPosition,
      canSeek: canSeek,
    );
  }

  // -------------------------------------------------------------- commands

  @override
  Future<void> play() async {
    _completionHandled = false;
    switch (_mode) {
      case PlaybackMode.video:
        await _video?.play();
      case PlaybackMode.audio:
        unawaited(_audio.play());
      case PlaybackMode.idle:
        // Play with nothing loaded (steering wheel after a restart, or after a failure):
        // retry the current video, or resume the last one.
        if (_loading) return;
        final current = _current;
        final last = _settings.lastVideo;
        if (current != null) {
          await select(current, startAt: progress.value.position);
        } else if (last != null) {
          await select(last, startAt: _settings.lastPosition);
        }
    }
  }

  @override
  Future<void> pause() async {
    switch (_mode) {
      case PlaybackMode.video:
        await _video?.pause();
      case PlaybackMode.audio:
        await _audio.pause();
      case PlaybackMode.idle:
        return;
    }
    _saveNow();
  }

  Future<void> togglePlay() => isPlaying ? pause() : play();

  @override
  Future<void> stop() async {
    await pause();
    // idle makes audio_service drop the foreground service and the notification.
    _handler.publish(
      playing: false,
      processing: AudioProcessingState.idle,
      position: position,
      canSeek: false,
    );
  }

  @override
  Future<void> next() async {
    final video = _neighbor(1);
    if (video != null) await select(video);
  }

  @override
  Future<void> previous() async {
    final video = _neighbor(-1);
    if (video != null) await select(video);
  }

  CarVideo? _neighbor(int step) {
    if (_queue.isEmpty) return null;
    final current = _current;
    final index =
        current == null ? -1 : _queue.indexWhere((v) => v.id == current.id);
    if (index < 0) return _queue.first;
    return _queue[(index + step) % _queue.length];
  }

  @override
  Future<void> seek(Duration target) async {
    if (!canSeek) return;
    final duration = progress.value.duration;
    var to = target.isNegative ? Duration.zero : target;
    if (to > duration) to = duration;
    if (to < duration) _completionHandled = false;
    // Move the seek bar now; the player catches up.
    _updateProgress(to, duration);
    switch (_mode) {
      case PlaybackMode.video:
        await _video?.seekTo(to);
        _publishVideoState();
      case PlaybackMode.audio:
        await _audio.seek(to);
        _publishAudioState();
      case PlaybackMode.idle:
        return;
    }
  }

  @override
  Future<void> seekRelative(Duration offset) => seek(position + offset);

  // -------------------------------------------------------------- settings

  Future<void> setQuality(StreamQuality quality) async {
    if (quality == _quality) return;
    _quality = quality;
    if (!quality.isAudioOnly) _lastVideoQuality = quality;
    unawaited(_settings.setQuality(quality.label));
    final current = _current;
    if (current == null) {
      _notify();
      return;
    }
    _say('Kualitas: ${quality.label}');
    await select(current, startAt: isLive ? null : position);
  }

  /// Switches between Audio Only and the last video quality.
  Future<void> setAudioOnly(bool on) =>
      setQuality(on ? StreamQuality.audioOnly : _lastVideoQuality);

  void toggleAutoplay() {
    _autoplay = !_autoplay;
    unawaited(_settings.setAutoplay(_autoplay));
    _notify();
  }

  /// Retries the current video from where it stopped.
  Future<void> retry() async {
    final current = _current;
    if (current != null) {
      await select(current, startAt: progress.value.position);
    }
  }

  /// Takes the feed's longer list when it still contains the current video, so autoplay
  /// continues into newly loaded pages instead of wrapping around.
  void updateQueue(List<CarVideo> videos) {
    final current = _current;
    if (current == null || !videos.any((v) => v.id == current.id)) return;
    _queue = List.unmodifiable(videos);
  }

  // --------------------------------------------------------------- helpers

  void _say(String message) {
    if (!_messages.isClosed) _messages.add(message);
  }

  void _showSponsorNotice(String text) {
    sponsorNotice.value = text;
    _sponsorNoticeTimer?.cancel();
    _sponsorNoticeTimer = Timer(_sponsorNoticeDuration, () {
      if (!_disposed) sponsorNotice.value = null;
    });
  }

  void _saveNow() {
    if (_current == null || isLive) return;
    _lastPositionSave = DateTime.now();
    unawaited(_settings.setLastPosition(position));
  }

  bool _isStale(int generation) => _disposed || generation != _generation;

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _saveNow();
    _disposed = true;
    _generation++;
    for (final subscription in _subscriptions) {
      subscription.cancel();
    }
    _sponsorNoticeTimer?.cancel();
    unawaited(_releasePlayers(_detachPlayers()));
    if (identical(_handler.commands, this)) _handler.commands = null;
    _messages.close();
    progress.dispose();
    sponsorNotice.dispose();
    super.dispose();
  }
}
