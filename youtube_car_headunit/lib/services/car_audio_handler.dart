import 'package:audio_service/audio_service.dart';
import 'package:just_audio/just_audio.dart';
import '../models/car_video.dart';

/// Handles background / audio-only playback and media notification state.
///
/// Important for mid-range devices (e.g. Galaxy A6+): when video is playing
/// via [VideoPlayerController], do **not** also decode audio here — that
/// doubles CPU, RAM, and network use. Use [updateMetadataOnly] for video mode
/// and [playAudioOnly] when quality is "Audio Only".
class CarAudioHandler extends BaseAudioHandler with QueueHandler, SeekHandler {
  final AudioPlayer _player = AudioPlayer();

  static const Map<String, String> defaultHeaders = {
    'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
            '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  };

  CarAudioHandler() {
    _player.playerStateStream.listen((state) {
      final playing = state.playing;
      final processingState = state.processingState;

      playbackState.add(
        playbackState.value.copyWith(
          controls: [
            MediaControl.skipToPrevious,
            if (playing) MediaControl.pause else MediaControl.play,
            MediaControl.skipToNext,
            MediaControl.stop,
          ],
          systemActions: const {
            MediaAction.seek,
            MediaAction.seekForward,
            MediaAction.seekBackward,
          },
          androidCompactActionIndices: const [0, 1, 2],
          processingState: const {
            ProcessingState.idle: AudioProcessingState.idle,
            ProcessingState.loading: AudioProcessingState.loading,
            ProcessingState.buffering: AudioProcessingState.buffering,
            ProcessingState.ready: AudioProcessingState.ready,
            ProcessingState.completed: AudioProcessingState.completed,
          }[processingState]!,
          playing: playing,
          updatePosition: _player.position,
          bufferedPosition: _player.bufferedPosition,
          speed: _player.speed,
        ),
      );
    });
  }

  AudioPlayer get player => _player;

  MediaItem _toMediaItem(CarVideo video) {
    return MediaItem(
      id: video.id,
      title: video.title,
      artist: video.uploader,
      album: 'YouTube Car Edition',
      artUri: Uri.tryParse(video.thumbnail),
      duration: video.duration,
    );
  }

  /// Updates lock-screen / notification metadata without starting a second decoder.
  void updateMetadataOnly(CarVideo video) {
    mediaItem.add(_toMediaItem(video));
  }

  /// Plays audio-only stream (no VideoPlayer). Used for "Audio Only" quality.
  Future<void> playAudioOnly(CarVideo video) async {
    mediaItem.add(_toMediaItem(video));

    final url = video.audioStreamUrl;
    if (url == null) return;

    try {
      await _player.stop();
      await _player.setUrl(url, headers: defaultHeaders);
      await _player.play();
    } catch (_) {
      // Fallback gracefully on stream errors
    }
  }

  @override
  Future<void> play() => _player.play();

  @override
  Future<void> pause() => _player.pause();

  @override
  Future<void> seek(Duration position) => _player.seek(position);

  @override
  Future<void> stop() => _player.stop();

  Future<void> dispose() async {
    await _player.dispose();
  }
}
