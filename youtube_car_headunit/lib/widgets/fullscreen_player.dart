import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';

class FullscreenPlayer extends StatefulWidget {
  final CarVideo video;
  final VideoPlayerController? controller;
  final bool isVideoLoading;
  final VoidCallback onNext;
  final VoidCallback onPrevious;
  final VoidCallback onCloseFullscreen;

  const FullscreenPlayer({
    super.key,
    required this.video,
    required this.controller,
    required this.isVideoLoading,
    required this.onNext,
    required this.onPrevious,
    required this.onCloseFullscreen,
  });

  @override
  State<FullscreenPlayer> createState() => _FullscreenPlayerState();
}

class _FullscreenPlayerState extends State<FullscreenPlayer> {
  bool _showControls = true;
  Timer? _hideControlsTimer;

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    widget.controller?.addListener(_onControllerUpdate);
    _startHideTimer();
  }

  @override
  void didUpdateWidget(FullscreenPlayer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller?.removeListener(_onControllerUpdate);
      widget.controller?.addListener(_onControllerUpdate);
      _startHideTimer();
    }
  }

  void _onControllerUpdate() {
    if (mounted) setState(() {});
  }

  void _startHideTimer() {
    _hideControlsTimer?.cancel();
    _hideControlsTimer = Timer(const Duration(seconds: 3), () {
      if (mounted && (widget.controller?.value.isPlaying ?? false)) {
        setState(() {
          _showControls = false;
        });
      }
    });
  }

  void _toggleControls() {
    setState(() {
      _showControls = !_showControls;
    });
    if (_showControls) {
      _startHideTimer();
    } else {
      _hideControlsTimer?.cancel();
    }
  }

  @override
  void dispose() {
    _hideControlsTimer?.cancel();
    widget.controller?.removeListener(_onControllerUpdate);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  String _formatTime(Duration d) {
    final m = d.inMinutes;
    final s = d.inSeconds.remainder(60);
    return '$m:${s < 10 ? '0' : ''}$s';
  }

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    final isInitialized = controller != null && controller.value.isInitialized;
    final isPlaying = controller?.value.isPlaying ?? false;
    final position = controller?.value.position ?? Duration.zero;
    final duration = controller?.value.duration ?? Duration.zero;

    return Scaffold(
      backgroundColor: Colors.black,
      body: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: _toggleControls,
        child: Stack(
          alignment: Alignment.center,
          children: [
            // 100% Fullscreen Video Render
            if (isInitialized && !widget.isVideoLoading)
              Center(
                child: AspectRatio(
                  aspectRatio: controller.value.aspectRatio,
                  child: VideoPlayer(controller),
                ),
              )
            else
              Image.network(
                widget.video.thumbnail,
                width: double.infinity,
                height: double.infinity,
                fit: BoxFit.cover,
                cacheWidth: 800,
              ),

            // Loading Overlay when switching videos in Fullscreen
            if (widget.isVideoLoading || !isInitialized)
              Container(
                color: Colors.black.withValues(alpha: 0.8),
                child: const Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      SpinKitPulse(color: CarTheme.accentRed, size: 56),
                      SizedBox(height: 12),
                      Text(
                        'Memuat Stream Ad-Free...',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.white70,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

            // Controls Overlay (Smooth Animated Fade & Auto-Hide)
            AnimatedOpacity(
              opacity: _showControls ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 300),
              child: IgnorePointer(
                ignoring: !_showControls,
                child: Container(
                  color: Colors.black54,
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Top Header Bar
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              widget.video.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ),
                          IconButton(
                            iconSize: 28,
                            icon: const Icon(LucideIcons.minimize, color: Colors.white),
                            onPressed: widget.onCloseFullscreen,
                          ),
                        ],
                      ),

                      // Middle Controls
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          IconButton(
                            iconSize: 44,
                            icon: const Icon(LucideIcons.skipBack, color: Colors.white),
                            onPressed: () {
                              _startHideTimer();
                              widget.onPrevious();
                            },
                          ),
                          const SizedBox(width: 32),
                          CircleAvatar(
                            radius: 34,
                            backgroundColor: CarTheme.accentRed,
                            child: IconButton(
                              iconSize: 36,
                              icon: widget.isVideoLoading
                                  ? const SpinKitThreeBounce(color: Colors.white, size: 20)
                                  : Icon(isPlaying ? LucideIcons.pause : LucideIcons.play, color: Colors.white),
                              onPressed: () {
                                _startHideTimer();
                                setState(() {
                                  if (isPlaying) {
                                    controller?.pause();
                                  } else {
                                    controller?.play();
                                  }
                                });
                              },
                            ),
                          ),
                          const SizedBox(width: 32),
                          IconButton(
                            iconSize: 44,
                            icon: const Icon(LucideIcons.skipForward, color: Colors.white),
                            onPressed: () {
                              _startHideTimer();
                              widget.onNext();
                            },
                          ),
                        ],
                      ),

                      // Bottom Progress Slider
                      Row(
                        children: [
                          Text(
                            _formatTime(position),
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                          ),
                          Expanded(
                            child: SliderTheme(
                              data: SliderTheme.of(context).copyWith(
                                activeTrackColor: CarTheme.accentRed,
                                thumbColor: CarTheme.accentRed,
                                trackHeight: 5,
                                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                                overlayShape: const RoundSliderOverlayShape(overlayRadius: 16),
                              ),
                              child: Slider(
                                value: position.inSeconds.toDouble().clamp(0.0, duration.inSeconds.toDouble() > 0 ? duration.inSeconds.toDouble() : 1.0),
                                max: duration.inSeconds.toDouble() > 0 ? duration.inSeconds.toDouble() : 1.0,
                                onChangeStart: (_) => _hideControlsTimer?.cancel(),
                                onChangeEnd: (_) => _startHideTimer(),
                                onChanged: (val) {
                                  controller?.seekTo(Duration(seconds: val.toInt()));
                                },
                              ),
                            ),
                          ),
                          Text(
                            _formatTime(duration),
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
