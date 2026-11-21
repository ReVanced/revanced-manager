import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';

import '../controllers/playback_controller.dart';
import '../theme/car_theme.dart';
import 'seek_bar.dart';

/// Immersive video. Tap shows the controls for a few seconds; Back or the large close
/// button leaves.
class FullscreenPlayer extends StatefulWidget {
  const FullscreenPlayer({
    super.key,
    required this.playback,
    required this.onClose,
  });

  final PlaybackController playback;
  final VoidCallback onClose;

  @override
  State<FullscreenPlayer> createState() => _FullscreenPlayerState();
}

class _FullscreenPlayerState extends State<FullscreenPlayer> {
  static const _controlsVisibleFor = Duration(seconds: 5);

  bool _showControls = true;
  Timer? _hideTimer;

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    _startHideTimer();
  }

  void _startHideTimer() {
    _hideTimer?.cancel();
    _hideTimer = Timer(_controlsVisibleFor, () {
      if (mounted && widget.playback.isPlaying) {
        setState(() => _showControls = false);
      }
    });
  }

  void _toggleControls() {
    setState(() => _showControls = !_showControls);
    if (_showControls) {
      _startHideTimer();
    } else {
      _hideTimer?.cancel();
    }
  }

  /// Runs [action] and keeps the controls visible a while longer.
  void _withControls(VoidCallback action) {
    action();
    _startHideTimer();
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    applyCarSystemUi();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final playback = widget.playback;
    return ColoredBox(
      color: Colors.black,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: _toggleControls,
        child: ListenableBuilder(
          listenable: playback,
          builder: (context, _) {
            final video = playback.current;
            final controller = playback.videoController;
            if (video == null) return const SizedBox.shrink();
            return Stack(
              alignment: Alignment.center,
              children: [
                if (playback.hasVideo && controller != null)
                  Center(
                    child: RepaintBoundary(
                      child: AspectRatio(
                        aspectRatio: controller.value.aspectRatio,
                        child: VideoPlayer(controller),
                      ),
                    ),
                  ),
                if (playback.isLoading || !playback.hasVideo)
                  const SpinKitThreeBounce(color: CarTheme.accent, size: 24),
                AnimatedOpacity(
                  opacity: _showControls ? 1.0 : 0.0,
                  duration: const Duration(milliseconds: 180),
                  child: IgnorePointer(
                    ignoring: !_showControls,
                    child: ColoredBox(
                      color: const Color(0x8C000000),
                      child: SafeArea(
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(16, 12, 24, 12),
                          child: Column(
                            children: [
                              Row(
                                children: [
                                  Material(
                                    color: CarTheme.surfaceHigh,
                                    shape: const StadiumBorder(),
                                    child: InkWell(
                                      onTap: widget.onClose,
                                      customBorder: const StadiumBorder(),
                                      child: const Padding(
                                        padding: EdgeInsets.symmetric(
                                          horizontal: 18,
                                          vertical: 14,
                                        ),
                                        child: Row(
                                          mainAxisSize: MainAxisSize.min,
                                          children: [
                                            Icon(LucideIcons.minimize, size: 20),
                                            SizedBox(width: 8),
                                            Text('Tutup', style: CarTheme.body),
                                          ],
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: Text(
                                      video.title,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: CarTheme.heading,
                                    ),
                                  ),
                                ],
                              ),
                              const Spacer(),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  ControlButton(
                                    icon: LucideIcons.skipBack,
                                    size: 64,
                                    onPressed: () =>
                                        _withControls(playback.previous),
                                  ),
                                  const SizedBox(width: 32),
                                  ControlButton(
                                    icon: playback.isPlaying
                                        ? LucideIcons.pause
                                        : LucideIcons.play,
                                    size: 80,
                                    primary: true,
                                    onPressed: () =>
                                        _withControls(playback.togglePlay),
                                  ),
                                  const SizedBox(width: 32),
                                  ControlButton(
                                    icon: LucideIcons.skipForward,
                                    size: 64,
                                    onPressed: () =>
                                        _withControls(playback.next),
                                  ),
                                ],
                              ),
                              const Spacer(),
                              SeekBar(
                                progress: playback.progress,
                                onSeek: playback.seek,
                                enabled: playback.canSeek,
                                isLive: playback.isLive,
                                large: true,
                                textColor: CarTheme.text,
                                onDragStart: () => _hideTimer?.cancel(),
                                onDragEnd: _startHideTimer,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
