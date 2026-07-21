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
  final String selectedQuality;
  final ValueChanged<String> onQualityChanged;
  final bool isAutoplayEnabled;
  final VoidCallback onToggleAutoplay;
  final VoidCallback onNext;
  final VoidCallback onPrevious;
  final VoidCallback onToggleDrivingMode;
  final VoidCallback onToggleFullscreen;
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
    this.sponsorSegments = const [],
  });

  @override
  State<PlayerWidget> createState() => _PlayerWidgetState();
}

class _PlayerWidgetState extends State<PlayerWidget> {
  String? _sponsorNotice;

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
    }
  }

  void _onControllerTick() {
    if (!mounted) return;
    _checkSponsorSegments();
    setState(() {}); // Real-time per-second playback position & slider update!
  }

  void _checkSponsorSegments() {
    final controller = widget.controller;
    if (controller == null || !controller.value.isInitialized) return;
    final currentPos = controller.value.position.inSeconds.toDouble();

    for (var seg in widget.sponsorSegments) {
      if (currentPos >= seg.start && currentPos < seg.end) {
        controller.seekTo(Duration(seconds: seg.end.toInt()));
        if (mounted) {
          setState(() {
            _sponsorNotice = 'Auto-Skip Sponsor (${seg.category})';
          });
          Future.delayed(const Duration(seconds: 3), () {
            if (mounted) {
              setState(() {
                _sponsorNotice = null;
              });
            }
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
          {'label': '360p (Default - Hemat Kuota & Anti Lag)', 'value': '360p', 'icon': LucideIcons.zap},
          {'label': '480p (Standard Car Display)', 'value': '480p', 'icon': LucideIcons.tv},
          {'label': '720p (HD Quality)', 'value': '720p', 'icon': LucideIcons.sparkles},
          {'label': '1080p (Full HD)', 'value': '1080p', 'icon': LucideIcons.film},
          {'label': '🎵 Audio Only (Hemat Kuota 95%)', 'value': 'Audio Only', 'icon': LucideIcons.music},
        ];

        return Container(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(LucideIcons.sliders, color: CarTheme.accentRed, size: 20),
                  const SizedBox(width: 8),
                  const Text('Pilih Kualitas Video', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                  const Spacer(),
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
                  leading: Icon(q['icon'] as IconData, color: isSelected ? CarTheme.accentRed : CarTheme.textMuted),
                  title: Text(
                    q['label'] as String,
                    style: TextStyle(
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      color: isSelected ? CarTheme.accentRed : Colors.white,
                      fontSize: 13,
                    ),
                  ),
                  trailing: isSelected ? const Icon(LucideIcons.check, color: CarTheme.accentRed) : null,
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

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    final isInitialized = controller != null && controller.value.isInitialized;
    final isPlaying = controller?.value.isPlaying ?? false;
    final position = controller?.value.position ?? Duration.zero;
    final duration = controller?.value.duration ?? Duration.zero;

    return SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Responsive Video Container
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Container(
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: Colors.white12),
              ),
              clipBehavior: Clip.antiAlias,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  if (isInitialized && !widget.isVideoLoading)
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
                    ),

                  // Loading Overlay when switching videos
                  if (widget.isVideoLoading || !isInitialized)
                    Container(
                      color: Colors.black.withValues(alpha: 0.75),
                      child: const Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            SpinKitPulse(color: CarTheme.accentRed, size: 36),
                            SizedBox(height: 6),
                            Text(
                              'Memuat Stream Ad-Free...',
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

                  // Fullscreen Button Overlay
                  if (isInitialized && !widget.isVideoLoading)
                    Positioned(
                      bottom: 8,
                      right: 8,
                      child: GestureDetector(
                        onTap: widget.onToggleFullscreen,
                        child: Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.75),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Icon(LucideIcons.maximize, color: Colors.white, size: 18),
                        ),
                      ),
                    ),

                  // Toast Notification for SponsorBlock
                  if (_sponsorNotice != null)
                    Positioned(
                      top: 8,
                      right: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: CarTheme.accentGreen,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(LucideIcons.zap, color: Colors.black, size: 14),
                            const SizedBox(width: 4),
                            Text(
                              _sponsorNotice!,
                              style: const TextStyle(
                                color: Colors.black,
                                fontWeight: FontWeight.bold,
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 6),

          // Controls Panel
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            decoration: BoxDecoration(
              color: CarTheme.bgCard,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: Colors.white12),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Title & Badges
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SingleChildScrollView(
                            scrollDirection: Axis.horizontal,
                            child: Row(
                              children: [
                                // Quality Selector Badge (Clickable)
                                InkWell(
                                  onTap: () => _showQualityModal(context),
                                  borderRadius: BorderRadius.circular(4),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: Colors.white12,
                                      borderRadius: BorderRadius.circular(4),
                                      border: Border.all(color: Colors.white24),
                                    ),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(LucideIcons.sliders, size: 10, color: Colors.white70),
                                        const SizedBox(width: 3),
                                        Text(
                                          widget.selectedQuality,
                                          style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 6),
                                // Autoplay Toggle Badge
                                InkWell(
                                  onTap: widget.onToggleAutoplay,
                                  borderRadius: BorderRadius.circular(4),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: widget.isAutoplayEnabled ? CarTheme.accentRed.withValues(alpha: 0.2) : Colors.white12,
                                      borderRadius: BorderRadius.circular(4),
                                      border: Border.all(color: widget.isAutoplayEnabled ? CarTheme.accentRed : Colors.white24),
                                    ),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        Icon(LucideIcons.repeat, size: 10, color: widget.isAutoplayEnabled ? CarTheme.accentRed : Colors.white70),
                                        const SizedBox(width: 3),
                                        Text(
                                          widget.isAutoplayEnabled ? 'AUTOPLAY ON' : 'AUTOPLAY OFF',
                                          style: TextStyle(
                                            fontSize: 9,
                                            fontWeight: FontWeight.bold,
                                            color: widget.isAutoplayEnabled ? CarTheme.accentRed : Colors.white70,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            widget.video.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: CarTheme.textMain),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    InkWell(
                      onTap: widget.onToggleDrivingMode,
                      borderRadius: BorderRadius.circular(10),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: CarTheme.accentRed,
                          borderRadius: BorderRadius.circular(10),
                          boxShadow: const [BoxShadow(color: CarTheme.accentRedGlow, blurRadius: 8)],
                        ),
                        child: const Row(
                          children: [
                            Icon(LucideIcons.tv, size: 14, color: Colors.white),
                            SizedBox(width: 4),
                            Text('Setir', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Colors.white)),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),

                // Responsive Progress Bar (Enlarged Touch Target)
                Row(
                  children: [
                    Text(_formatTime(position), style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: CarTheme.textMuted)),
                    Expanded(
                      child: SliderTheme(
                        data: SliderTheme.of(context).copyWith(
                          trackHeight: 5,
                          thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 8),
                          overlayShape: const RoundSliderOverlayShape(overlayRadius: 14),
                        ),
                        child: Slider(
                          value: position.inSeconds.toDouble().clamp(0.0, (duration.inSeconds.toDouble() > 0 ? duration.inSeconds.toDouble() : 1.0)),
                          max: duration.inSeconds.toDouble() > 0 ? duration.inSeconds.toDouble() : 1.0,
                          activeColor: CarTheme.accentRed,
                          inactiveColor: Colors.white24,
                          onChanged: (val) {
                            controller?.seekTo(Duration(seconds: val.toInt()));
                          },
                        ),
                      ),
                    ),
                    Text(_formatTime(duration), style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: CarTheme.textMuted)),
                  ],
                ),

                // Ergonomic Controls Buttons Row
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    IconButton(
                      iconSize: 24,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
                      icon: const Icon(LucideIcons.skipBack, color: Colors.white),
                      onPressed: widget.onPrevious,
                    ),
                    InkWell(
                      onTap: () {
                        final current = controller?.value.position ?? Duration.zero;
                        controller?.seekTo(current - const Duration(seconds: 10));
                      },
                      borderRadius: BorderRadius.circular(10),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.white12,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Text('-10s', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.white)),
                      ),
                    ),
                    CircleAvatar(
                      radius: 20,
                      backgroundColor: CarTheme.accentRed,
                      child: IconButton(
                        iconSize: 22,
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
                        icon: widget.isVideoLoading
                            ? const SpinKitThreeBounce(color: Colors.white, size: 14)
                            : Icon(isPlaying ? LucideIcons.pause : LucideIcons.play, color: Colors.white),
                        onPressed: () {
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
                    InkWell(
                      onTap: () {
                        final current = controller?.value.position ?? Duration.zero;
                        controller?.seekTo(current + const Duration(seconds: 10));
                      },
                      borderRadius: BorderRadius.circular(10),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.white12,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Text('+10s', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.white)),
                      ),
                    ),
                    IconButton(
                      iconSize: 24,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(minWidth: 36, minHeight: 36),
                      icon: const Icon(LucideIcons.skipForward, color: Colors.white),
                      onPressed: widget.onNext,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
