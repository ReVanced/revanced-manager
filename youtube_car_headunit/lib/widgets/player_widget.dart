import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';

class PlayerWidget extends StatefulWidget {
  final CarVideo video;
  final VideoPlayerController? controller;
  final bool isVideoLoading;
  final bool isAudioOnly;
  final String selectedQuality;
  final ValueChanged<String> onQualityChanged;
  final bool isAutoplayEnabled;
  final VoidCallback onToggleAutoplay;
  final VoidCallback onNext;
  final VoidCallback onPrevious;
  final VoidCallback onToggleDrivingMode;
  final VoidCallback onToggleFullscreen;
  final VoidCallback? onTogglePlay;
  final bool isAudioPlaying;
  final List<SponsorSegment> sponsorSegments;

  const PlayerWidget({
    super.key,
    required this.video,
    required this.controller,
    required this.isVideoLoading,
    required this.selectedQuality,
    required this.onQualityChanged,
    required this.isAutoplayEnabled,
    required this.onToggleAutoplay,
    required this.onNext,
    required this.onPrevious,
    required this.onToggleDrivingMode,
    required this.onToggleFullscreen,
    this.isAudioOnly = false,
    this.onTogglePlay,
    this.isAudioPlaying = false,
    this.sponsorSegments = const [],
  });

  @override
  State<PlayerWidget> createState() => _PlayerWidgetState();
}

class _PlayerWidgetState extends State<PlayerWidget> {
  String? _sponsorNotice;
  int _lastUiSecond = -1;
  bool? _lastPlaying;
  String? _lastSkippedCategory;

  @override
  void initState() {
    super.initState();
    widget.controller?.addListener(_onControllerTick);
  }

  @override
  void didUpdateWidget(PlayerWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller?.removeListener(_onControllerTick);
      widget.controller?.addListener(_onControllerTick);
      _lastUiSecond = -1;
      _lastPlaying = null;
      _lastSkippedCategory = null;
    }
  }

  /// Rebuild at most once per second (or on play/pause) — critical on A6+.
  void _onControllerTick() {
    if (!mounted) return;
    final controller = widget.controller;
    if (controller == null || !controller.value.isInitialized) return;

    _checkSponsorSegments();

    final second = controller.value.position.inSeconds;
    final playing = controller.value.isPlaying;
    if (second == _lastUiSecond && playing == _lastPlaying) return;

    _lastUiSecond = second;
    _lastPlaying = playing;
    setState(() {});
  }

  void _checkSponsorSegments() {
    final controller = widget.controller;
    if (controller == null || !controller.value.isInitialized) return;
    final currentPos = controller.value.position.inSeconds.toDouble();

    for (final seg in widget.sponsorSegments) {
      if (currentPos >= seg.start && currentPos < seg.end) {
        if (_lastSkippedCategory == '${seg.category}_${seg.start}') return;
        _lastSkippedCategory = '${seg.category}_${seg.start}';
        controller.seekTo(Duration(seconds: seg.end.toInt()));
        if (mounted) {
          setState(() {
            _sponsorNotice = 'Skip Sponsor (${seg.category})';
          });
          Future.delayed(const Duration(seconds: 2), () {
            if (mounted) setState(() => _sponsorNotice = null);
          });
        }
        break;
      }
    }
  }

  @override
  void dispose() {
    widget.controller?.removeListener(_onControllerTick);
    super.dispose();
  }

  String _formatTime(Duration d) {
    final m = d.inMinutes;
    final s = d.inSeconds.remainder(60);
    return '$m:${s < 10 ? '0' : ''}$s';
  }

  void _showQualityModal(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: CarTheme.bgSurface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        final qualities = [
          {
            'label': '360p — Hemat & Anti Lag (disarankan A6+)',
            'value': '360p',
            'icon': LucideIcons.zap,
          },
          {
            'label': '480p — Standard',
            'value': '480p',
            'icon': LucideIcons.tv,
          },
          {
            'label': '720p — HD (lebih berat)',
            'value': '720p',
            'icon': LucideIcons.sparkles,
          },
          {
            'label': 'Audio Only — Hemat kuota ~95%',
            'value': 'Audio Only',
            'icon': LucideIcons.music,
          },
        ];

        return Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(LucideIcons.sliders,
                      color: CarTheme.accentRed, size: 20),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Kualitas Video',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(LucideIcons.x, color: CarTheme.textMuted),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              const Divider(color: Colors.white12),
              ...qualities.map((q) {
                final isSelected = widget.selectedQuality == q['value'];
                return ListTile(
                  dense: true,
                  leading: Icon(
                    q['icon'] as IconData,
                    color: isSelected ? CarTheme.accentRed : CarTheme.textMuted,
                  ),
                  title: Text(
                    q['label'] as String,
                    style: TextStyle(
                      fontWeight:
                          isSelected ? FontWeight.bold : FontWeight.normal,
                      color: isSelected ? CarTheme.accentRed : Colors.white,
                      fontSize: 13,
                    ),
                  ),
                  trailing: isSelected
                      ? const Icon(LucideIcons.check, color: CarTheme.accentRed)
                      : null,
                  onTap: () {
                    Navigator.pop(context);
                    widget.onQualityChanged(q['value'] as String);
                  },
                );
              }),
            ],
          ),
        );
      },
    );
  }

  void _togglePlay() {
    if (widget.isAudioOnly) {
      widget.onTogglePlay?.call();
      return;
    }
    final controller = widget.controller;
    if (controller == null) return;
    if (controller.value.isPlaying) {
      controller.pause();
    } else {
      controller.play();
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    final isInitialized = controller != null && controller.value.isInitialized;
    final isPlaying = widget.isAudioOnly
        ? widget.isAudioPlaying
        : (controller?.value.isPlaying ?? false);
    final position = controller?.value.position ?? Duration.zero;
    final duration = controller?.value.duration ??
        (widget.isAudioOnly ? widget.video.duration : Duration.zero);

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        AspectRatio(
          aspectRatio: 16 / 9,
          child: RepaintBoundary(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.white12),
              ),
              clipBehavior: Clip.antiAlias,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  if (widget.isAudioOnly)
                    ColoredBox(
                      color: CarTheme.bgCard,
                      child: Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              LucideIcons.music,
                              size: 40,
                              color: CarTheme.accentRed.withValues(alpha: 0.8),
                            ),
                            const SizedBox(height: 8),
                            const Text(
                              'Mode Audio Only',
                              style: TextStyle(
                                color: Colors.white70,
                                fontWeight: FontWeight.bold,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                      ),
                    )
                  else if (isInitialized && !widget.isVideoLoading)
                    AspectRatio(
                      aspectRatio: controller.value.aspectRatio,
                      child: VideoPlayer(controller),
                    )
                  else
                    Image.network(
                      widget.video.thumbnail,
                      fit: BoxFit.cover,
                      width: double.infinity,
                      height: double.infinity,
                      cacheWidth: 480,
                      filterQuality: FilterQuality.low,
                    ),
                  if (widget.isVideoLoading && !widget.isAudioOnly)
                    Container(
                      color: Colors.black54,
                      child: const Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            SpinKitPulse(color: CarTheme.accentRed, size: 32),
                            SizedBox(height: 6),
                            Text(
                              'Memuat...',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.white70,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  if (isInitialized &&
                      !widget.isVideoLoading &&
                      !widget.isAudioOnly)
                    Positioned(
                      bottom: 8,
                      right: 8,
                      child: IconButton(
                        onPressed: widget.onToggleFullscreen,
                        style: IconButton.styleFrom(
                          backgroundColor: Colors.black54,
                          minimumSize: const Size(40, 40),
                        ),
                        icon: const Icon(
                          LucideIcons.maximize,
                          color: Colors.white,
                          size: 18,
                        ),
                      ),
                    ),
                  if (_sponsorNotice != null)
                    Positioned(
                      top: 8,
                      right: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 5,
                        ),
                        decoration: BoxDecoration(
                          color: CarTheme.accentGreen,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          _sponsorNotice!,
                          style: const TextStyle(
                            color: Colors.black,
                            fontWeight: FontWeight.bold,
                            fontSize: 11,
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 6),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
          decoration: BoxDecoration(
            color: CarTheme.bgCard,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.white12),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            _Badge(
                              icon: LucideIcons.sliders,
                              label: widget.selectedQuality,
                              onTap: () => _showQualityModal(context),
                            ),
                            const SizedBox(width: 6),
                            _Badge(
                              icon: LucideIcons.repeat,
                              label: widget.isAutoplayEnabled
                                  ? 'AUTO ON'
                                  : 'AUTO OFF',
                              active: widget.isAutoplayEnabled,
                              onTap: widget.onToggleAutoplay,
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          widget.video.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                            color: CarTheme.textMain,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 6),
                  FilledButton.icon(
                    onPressed: widget.onToggleDrivingMode,
                    style: FilledButton.styleFrom(
                      backgroundColor: CarTheme.accentRed,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      minimumSize: const Size(48, 44),
                    ),
                    icon: const Icon(LucideIcons.tv, size: 16),
                    label: const Text(
                      'Setir',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
              ),
              if (!widget.isAudioOnly)
                Row(
                  children: [
                    Text(
                      _formatTime(position),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: CarTheme.textMuted,
                      ),
                    ),
                    Expanded(
                      child: SliderTheme(
                        data: SliderTheme.of(context).copyWith(
                          trackHeight: 4,
                          thumbShape: const RoundSliderThumbShape(
                            enabledThumbRadius: 8,
                          ),
                          overlayShape: const RoundSliderOverlayShape(
                            overlayRadius: 16,
                          ),
                        ),
                        child: Slider(
                          value: position.inSeconds.toDouble().clamp(
                                0.0,
                                duration.inSeconds > 0
                                    ? duration.inSeconds.toDouble()
                                    : 1.0,
                              ),
                          max: duration.inSeconds > 0
                              ? duration.inSeconds.toDouble()
                              : 1.0,
                          activeColor: CarTheme.accentRed,
                          inactiveColor: Colors.white24,
                          onChanged: (val) {
                            controller?.seekTo(Duration(seconds: val.toInt()));
                          },
                        ),
                      ),
                    ),
                    Text(
                      _formatTime(duration),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: CarTheme.textMuted,
                      ),
                    ),
                  ],
                ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  IconButton(
                    iconSize: 26,
                    constraints:
                        const BoxConstraints(minWidth: 44, minHeight: 44),
                    icon: const Icon(LucideIcons.skipBack, color: Colors.white),
                    onPressed: widget.onPrevious,
                  ),
                  if (!widget.isAudioOnly)
                    TextButton(
                      onPressed: () {
                        final current =
                            controller?.value.position ?? Duration.zero;
                        controller?.seekTo(
                          current - const Duration(seconds: 10),
                        );
                      },
                      child: const Text(
                        '-10s',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  IconButton.filled(
                    style: IconButton.styleFrom(
                      backgroundColor: CarTheme.accentRed,
                      minimumSize: const Size(48, 48),
                    ),
                    icon: widget.isVideoLoading
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : Icon(
                            isPlaying ? LucideIcons.pause : LucideIcons.play,
                            color: Colors.white,
                            size: 22,
                          ),
                    onPressed: _togglePlay,
                  ),
                  if (!widget.isAudioOnly)
                    TextButton(
                      onPressed: () {
                        final current =
                            controller?.value.position ?? Duration.zero;
                        controller?.seekTo(
                          current + const Duration(seconds: 10),
                        );
                      },
                      child: const Text(
                        '+10s',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  IconButton(
                    iconSize: 26,
                    constraints:
                        const BoxConstraints(minWidth: 44, minHeight: 44),
                    icon: const Icon(LucideIcons.skipForward,
                        color: Colors.white),
                    onPressed: widget.onNext,
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Badge extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool active;

  const _Badge({
    required this.icon,
    required this.label,
    required this.onTap,
    this.active = false,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(4),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
        decoration: BoxDecoration(
          color: active
              ? CarTheme.accentRed.withValues(alpha: 0.2)
              : Colors.white12,
          borderRadius: BorderRadius.circular(4),
          border: Border.all(
            color: active ? CarTheme.accentRed : Colors.white24,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 10,
              color: active ? CarTheme.accentRed : Colors.white70,
            ),
            const SizedBox(width: 3),
            Text(
              label,
              style: TextStyle(
                fontSize: 9,
                fontWeight: FontWeight.bold,
                color: active ? CarTheme.accentRed : Colors.white70,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
