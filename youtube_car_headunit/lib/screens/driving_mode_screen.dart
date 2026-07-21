import 'dart:async';

import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';

class DrivingModeScreen extends StatefulWidget {
  final CarVideo video;
  final bool isPlaying;
  final bool isAudioOnly;
  final VideoPlayerController? videoController;
  final AudioPlayer? audioPlayer;
  final VoidCallback onNext;
  final VoidCallback onPrevious;
  final VoidCallback onTogglePlay;
  final VoidCallback onClose;
  final ValueChanged<Duration> onSeek;

  const DrivingModeScreen({
    super.key,
    required this.video,
    required this.isPlaying,
    required this.isAudioOnly,
    required this.onNext,
    required this.onPrevious,
    required this.onTogglePlay,
    required this.onClose,
    required this.onSeek,
    this.videoController,
    this.audioPlayer,
  });

  @override
  State<DrivingModeScreen> createState() => _DrivingModeScreenState();
}

class _DrivingModeScreenState extends State<DrivingModeScreen> {
  StreamSubscription<Duration>? _audioPosSub;
  int _lastUiSecond = -1;

  Duration get _position {
    if (widget.isAudioOnly) {
      return widget.audioPlayer?.position ?? Duration.zero;
    }
    return widget.videoController?.value.position ?? Duration.zero;
  }

  Duration get _duration {
    if (widget.isAudioOnly) {
      return widget.audioPlayer?.duration ?? widget.video.duration;
    }
    final d = widget.videoController?.value.duration;
    if (d != null && d > Duration.zero) return d;
    return widget.video.duration;
  }

  @override
  void initState() {
    super.initState();
    _attachListeners();
  }

  @override
  void didUpdateWidget(DrivingModeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.videoController != widget.videoController ||
        oldWidget.audioPlayer != widget.audioPlayer ||
        oldWidget.isAudioOnly != widget.isAudioOnly ||
        oldWidget.video.id != widget.video.id) {
      _detachListeners(oldWidget);
      _lastUiSecond = -1;
      _attachListeners();
    }
  }

  void _attachListeners() {
    if (widget.isAudioOnly) {
      _audioPosSub = widget.audioPlayer?.positionStream.listen((pos) {
        if (!mounted) return;
        final sec = pos.inSeconds;
        if (sec == _lastUiSecond) return;
        _lastUiSecond = sec;
        setState(() {});
      });
    } else {
      widget.videoController?.addListener(_onVideoTick);
    }
  }

  void _detachListeners(DrivingModeScreen from) {
    from.videoController?.removeListener(_onVideoTick);
    _audioPosSub?.cancel();
    _audioPosSub = null;
  }

  void _onVideoTick() {
    if (!mounted) return;
    final sec = widget.videoController?.value.position.inSeconds ?? 0;
    if (sec == _lastUiSecond) return;
    _lastUiSecond = sec;
    setState(() {});
  }

  @override
  void dispose() {
    widget.videoController?.removeListener(_onVideoTick);
    _audioPosSub?.cancel();
    super.dispose();
  }

  String _formatTime(Duration d) {
    final m = d.inMinutes;
    final s = d.inSeconds.remainder(60);
    return '$m:${s < 10 ? '0' : ''}$s';
  }

  @override
  Widget build(BuildContext context) {
    final position = _position;
    final duration = _duration;
    final maxSec = duration.inSeconds > 0 ? duration.inSeconds.toDouble() : 1.0;
    final posSec = position.inSeconds.toDouble().clamp(0.0, maxSec);

    return Scaffold(
      backgroundColor: CarTheme.bgPrimary,
      body: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onHorizontalDragEnd: (details) {
          final velocity = details.primaryVelocity;
          if (velocity == null) return;
          if (velocity < -200) {
            widget.onNext();
            _toast(context, 'Lagu berikutnya');
          } else if (velocity > 200) {
            widget.onPrevious();
            _toast(context, 'Lagu sebelumnya');
          }
        },
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            child: Column(
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: CarTheme.accentRed,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: const Text(
                        'MODE SETIR',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    const Spacer(),
                    FilledButton.tonalIcon(
                      onPressed: widget.onClose,
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(48, 48),
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                      ),
                      icon: const Icon(LucideIcons.minimize2, size: 18),
                      label: const Text(
                        'Keluar',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                const Text(
                  'Geser kiri / kanan untuk ganti lagu',
                  style: TextStyle(
                    fontSize: 12,
                    color: CarTheme.textMuted,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Expanded(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Flexible(
                        flex: 4,
                        child: AspectRatio(
                          aspectRatio: 1,
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(20),
                            child: Image.network(
                              widget.video.thumbnail,
                              fit: BoxFit.cover,
                              cacheWidth: 360,
                              filterQuality: FilterQuality.low,
                              errorBuilder: (_, __, ___) => const ColoredBox(
                                color: CarTheme.bgCard,
                                child: Icon(
                                  LucideIcons.music,
                                  size: 48,
                                  color: CarTheme.textMuted,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 24),
                      Expanded(
                        flex: 6,
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.video.title,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 22,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                                height: 1.2,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              widget.video.uploader,
                              style: const TextStyle(
                                fontSize: 16,
                                color: CarTheme.textMuted,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                // Progress bar — large thumb for driving
                Row(
                  children: [
                    Text(
                      _formatTime(position),
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: CarTheme.textMuted,
                      ),
                    ),
                    Expanded(
                      child: SliderTheme(
                        data: SliderTheme.of(context).copyWith(
                          trackHeight: 6,
                          thumbShape: const RoundSliderThumbShape(
                            enabledThumbRadius: 12,
                          ),
                          overlayShape: const RoundSliderOverlayShape(
                            overlayRadius: 20,
                          ),
                        ),
                        child: Slider(
                          value: posSec,
                          max: maxSec,
                          activeColor: CarTheme.accentRed,
                          inactiveColor: Colors.white24,
                          onChanged: (val) {
                            widget.onSeek(Duration(seconds: val.toInt()));
                          },
                        ),
                      ),
                    ),
                    Text(
                      _formatTime(duration),
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: CarTheme.textMuted,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      iconSize: 48,
                      constraints: const BoxConstraints(
                        minWidth: 72,
                        minHeight: 72,
                      ),
                      icon:
                          const Icon(LucideIcons.skipBack, color: Colors.white),
                      onPressed: widget.onPrevious,
                    ),
                    const SizedBox(width: 24),
                    IconButton.filled(
                      style: IconButton.styleFrom(
                        backgroundColor: CarTheme.accentRed,
                        minimumSize: const Size(80, 80),
                      ),
                      icon: Icon(
                        widget.isPlaying ? LucideIcons.pause : LucideIcons.play,
                        color: Colors.white,
                        size: 40,
                      ),
                      onPressed: widget.onTogglePlay,
                    ),
                    const SizedBox(width: 24),
                    IconButton(
                      iconSize: 48,
                      constraints: const BoxConstraints(
                        minWidth: 72,
                        minHeight: 72,
                      ),
                      icon: const Icon(
                        LucideIcons.skipForward,
                        color: Colors.white,
                      ),
                      onPressed: widget.onNext,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _toast(BuildContext context, String message) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(milliseconds: 700),
      ),
    );
  }
}
