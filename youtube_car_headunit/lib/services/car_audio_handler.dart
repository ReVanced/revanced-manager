import 'package:audio_service/audio_service.dart';
import 'package:just_audio/just_audio.dart';

import '../models/car_video.dart';
import 'youtube_service.dart';

/// Commands arriving from the media session: notification, lockscreen, steering-wheel and
/// Bluetooth buttons. PlaybackController implements them, because it owns the players.
abstract interface class MediaCommands {
  Future<void> play();

  Future<void> pause();

  Future<void> stop();

  Future<void> next();

  Future<void> previous();

  Future<void> seek(Duration position);

  Future<void> seekRelative(Duration offset);
}

/// Bridge between Android's media session (audio_service) and [MediaCommands].
///
/// It owns the just_audio player used for Audio Only because audio_service keeps this
/// object alive for the whole session. Video plays through video_player, so the
/// controller pushes state here with [publish]; the session always mirrors whichever
/// player is active. Only one decoder runs at a time (important on a Galaxy A6+).
class CarAudioHandler extends BaseAudioHandler {
  /// Headers are applied natively by ExoPlayer on Android; just_audio's default Dart proxy
  /// would add a localhost hop and would not cover HLS segment requests anyway.
  final AudioPlayer player = AudioPlayer(
    userAgent: kStreamUserAgent,
    useProxyForRequestHeaders: false,
  );

  MediaCommands? commands;

  @override
  Future<void> play() async => commands?.play();

  @override
  Future<void> pause() async => commands?.pause();

  @override
  Future<void> stop() async => commands?.stop();

  @override
  Future<void> skipToNext() async => commands?.next();

  @override
  Future<void> skipToPrevious() async => commands?.previous();

  @override
  Future<void> seek(Duration position) async => commands?.seek(position);

  @override
  Future<void> fastForward() async =>
      commands?.seekRelative(const Duration(seconds: 10));

  @override
  Future<void> rewind() async =>
      commands?.seekRelative(const Duration(seconds: -10));

  /// Shows [video] in the notification and lockscreen.
  void setNowPlaying(CarVideo video, {Duration? duration}) {
    final length = duration ?? video.duration;
    mediaItem.add(
      MediaItem(
        id: video.id,
        title: video.title,
        artist: video.uploader,
        album: 'YouTube Car',
        artUri: Uri.tryParse(video.thumbnail),
        duration: video.isLive || length <= Duration.zero ? null : length,
      ),
    );
  }

  /// Publishes playback state. audio_service extrapolates the position while playing, so
  /// this is only needed on state changes and seeks, not every tick.
  void publish({
    required bool playing,
    required AudioProcessingState processing,
    required Duration position,
    Duration buffered = Duration.zero,
    bool canSeek = true,
  }) {
    playbackState.add(
      playbackState.value.copyWith(
        controls: [
          MediaControl.skipToPrevious,
          if (playing) MediaControl.pause else MediaControl.play,
          MediaControl.skipToNext,
          MediaControl.stop,
        ],
        systemActions: {
          if (canSeek) ...const {
            MediaAction.seek,
            MediaAction.fastForward,
            MediaAction.rewind,
          },
        },
        androidCompactActionIndices: const [0, 1, 2],
        processingState: processing,
        playing: playing,
        updatePosition: position,
        bufferedPosition: buffered,
        speed: 1.0,
      ),
    );
  }
}
