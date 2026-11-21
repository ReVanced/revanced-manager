import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../controllers/playback_controller.dart';
import '../theme/car_theme.dart';
import '../utils/format.dart';

/// Position slider with elapsed and total time.
///
/// Dragging only moves the thumb; the seek happens once on release, because every seek
/// restarts buffering on a network stream. A zero duration (live, or not known yet)
/// disables it.
class SeekBar extends StatefulWidget {
  const SeekBar({
    super.key,
    required this.progress,
    required this.onSeek,
    this.enabled = true,
    this.isLive = false,
    this.large = false,
    this.textColor = CarTheme.textMuted,
    this.onDragStart,
    this.onDragEnd,
  });

  final ValueListenable<PlaybackProgress> progress;
  final ValueChanged<Duration> onSeek;
  final bool enabled;
  final bool isLive;

  /// Bigger track and thumb for the Musik screen.
  final bool large;
  final Color textColor;
  final VoidCallback? onDragStart;
  final VoidCallback? onDragEnd;

  @override
  State<SeekBar> createState() => _SeekBarState();
}

class _SeekBarState extends State<SeekBar> {
  double? _dragSeconds;

  @override
  Widget build(BuildContext context) {
    final textStyle = CarTheme.time.copyWith(
      fontSize: widget.large ? 14 : 12,
      color: widget.textColor,
    );
    return ValueListenableBuilder<PlaybackProgress>(
      valueListenable: widget.progress,
      builder: (context, progress, _) {
        final hasDuration = progress.duration > Duration.zero;
        final canSeek = widget.enabled && !widget.isLive && hasDuration;
        final maxSeconds =
            hasDuration ? progress.duration.inSeconds.toDouble() : 1.0;
        final seconds = (_dragSeconds ?? progress.position.inSeconds.toDouble())
            .clamp(0.0, maxSeconds);
        return Row(
          children: [
            Text(
              formatDuration(Duration(seconds: seconds.round())),
              style: textStyle,
            ),
            Expanded(
              child: SliderTheme(
                data: SliderTheme.of(context).copyWith(
                  trackHeight: widget.large ? 6 : 4,
                  thumbShape: RoundSliderThumbShape(
                    enabledThumbRadius: widget.large ? 12 : 8,
                  ),
                  overlayShape: RoundSliderOverlayShape(
                    overlayRadius: widget.large ? 20 : 16,
                  ),
                ),
                child: Slider(
                  value: hasDuration ? seconds : 0,
                  max: maxSeconds,
                  onChangeStart: canSeek
                      ? (value) {
                          widget.onDragStart?.call();
                          setState(() => _dragSeconds = value);
                        }
                      : null,
                  onChanged: canSeek
                      ? (value) => setState(() => _dragSeconds = value)
                      : null,
                  onChangeEnd: canSeek
                      ? (value) {
                          setState(() => _dragSeconds = null);
                          widget.onSeek(Duration(seconds: value.round()));
                          widget.onDragEnd?.call();
                        }
                      : null,
                ),
              ),
            ),
            Text(
              widget.isLive
                  ? 'LIVE'
                  : hasDuration
                      ? formatDuration(progress.duration)
                      : '--:--',
              style: textStyle,
            ),
          ],
        );
      },
    );
  }
}
