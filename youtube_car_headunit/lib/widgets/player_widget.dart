import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';

import '../controllers/playback_controller.dart';
import '../services/youtube_service.dart';
import '../theme/car_theme.dart';
import 'now_playing.dart';
import 'seek_bar.dart';

/// Inline player of the YouTube menu: picture, title, seek bar and controls.
class PlayerWidget extends StatelessWidget {
  const PlayerWidget({
    super.key,
    required this.playback,
    required this.onOpenMusic,
    required this.onOpenFullscreen,
  });

  final PlaybackController playback;
  final VoidCallback onOpenMusic;
  final VoidCallback onOpenFullscreen;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: playback,
      builder: (context, _) {
        final video = playback.current;
        if (video == null) return const SizedBox.shrink();
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: RepaintBoundary(
                child: _Picture(
                  playback: playback,
                  onOpenFullscreen: onOpenFullscreen,
                ),
              ),
            ),
            const SizedBox(height: 10),
            Text(
              video.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: CarTheme.body.copyWith(fontWeight: FontWeight.w600),
            ),
            SeekBar(
              progress: playback.progress,
              onSeek: playback.seek,
              enabled: playback.canSeek,
              isLive: playback.isLive,
            ),
            // One row so the controls stay on screen in the ~300 dp tall column.
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _SmallButton(
                  label: playback.servedLabel,
                  tooltip: 'Kualitas video',
                  onTap: () => _showQualitySheet(context),
                ),
                ControlButton(
                  icon: LucideIcons.skipBack,
                  size: 48,
                  onPressed: playback.previous,
                  tooltip: 'Sebelumnya',
                ),
                ControlButton(
                  icon: playback.isPlaying ? LucideIcons.pause : LucideIcons.play,
                  size: 56,
                  primary: true,
                  loading: playback.isLoading,
                  onPressed: playback.togglePlay,
                  tooltip: playback.isPlaying ? 'Jeda' : 'Putar',
                ),
                ControlButton(
                  icon: LucideIcons.skipForward,
                  size: 48,
                  onPressed: playback.next,
                  tooltip: 'Berikutnya',
                ),
                _SmallButton(
                  icon: LucideIcons.music2,
                  tooltip: 'Layar Musik',
                  onTap: onOpenMusic,
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  void _showQualitySheet(BuildContext context) {
    const options = [
      (StreamQuality.p360, '360p', 'Hemat & anti lag', LucideIcons.zap),
      (StreamQuality.p480, '480p', 'Standar', LucideIcons.tv),
      (StreamQuality.p720, '720p', 'HD, lebih berat', LucideIcons.sparkles),
      (StreamQuality.audioOnly, 'Audio', 'Hemat kuota', LucideIcons.headphones),
    ];
    // Options sit side by side: a landscape head unit has far more width than height, and
    // a vertical list overflowed the sheet on a 6" phone.
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      constraints: const BoxConstraints(maxWidth: 720),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(CarTheme.radiusLarge),
        ),
      ),
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    const Expanded(
                      child: Text('Kualitas video', style: CarTheme.heading),
                    ),
                    IconButton(
                      icon: const Icon(LucideIcons.x),
                      color: CarTheme.textMuted,
                      onPressed: () => Navigator.pop(sheetContext),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    for (final (quality, title, subtitle, icon) in options)
                      Expanded(
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 5),
                          child: _QualityTile(
                            title: title,
                            subtitle: subtitle,
                            icon: icon,
                            selected: playback.quality == quality,
                            onTap: () {
                              Navigator.pop(sheetContext);
                              playback.setQuality(quality);
                            },
                          ),
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _QualityTile extends StatelessWidget {
  const _QualityTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected ? CarTheme.accent : CarTheme.text;
    return Material(
      color: selected ? CarTheme.accentSoft : CarTheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(CarTheme.radius),
        side: BorderSide(
          color: selected ? CarTheme.accent : CarTheme.border,
          width: selected ? 2 : 1,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(CarTheme.radius),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
          child: Column(
            children: [
              Icon(icon, color: color, size: 24),
              const SizedBox(height: 8),
              Text(title, style: CarTheme.heading.copyWith(color: color)),
              const SizedBox(height: 2),
              Text(
                subtitle,
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: CarTheme.caption.copyWith(fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SmallButton extends StatelessWidget {
  const _SmallButton({
    required this.tooltip,
    required this.onTap,
    this.icon,
    this.label,
  });

  final String tooltip;
  final VoidCallback onTap;
  final IconData? icon;
  final String? label;

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltip,
      child: Material(
        color: CarTheme.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(CarTheme.radiusSmall),
          side: const BorderSide(color: CarTheme.border),
        ),
        child: InkWell(
          onTap: onTap,
          customBorder: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(CarTheme.radiusSmall),
          ),
          child: SizedBox(
            width: 52,
            height: 44,
            child: Center(
              child: icon != null
                  ? Icon(icon, size: 20, color: CarTheme.textMuted)
                  : FittedBox(
                      fit: BoxFit.scaleDown,
                      child: Text(
                        label ?? '',
                        style: CarTheme.label.copyWith(color: CarTheme.text),
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Picture extends StatelessWidget {
  const _Picture({required this.playback, required this.onOpenFullscreen});

  final PlaybackController playback;
  final VoidCallback onOpenFullscreen;

  @override
  Widget build(BuildContext context) {
    final video = playback.current!;
    final controller = playback.videoController;
    return ClipRRect(
      borderRadius: BorderRadius.circular(CarTheme.radiusLarge),
      child: ColoredBox(
        color: Colors.black,
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (playback.hasVideo && controller != null)
              Center(
                child: AspectRatio(
                  aspectRatio: controller.value.aspectRatio,
                  child: VideoPlayer(controller),
                ),
              )
            else
              Artwork(url: video.thumbnail, radius: 0, cacheWidth: 480),
            if (playback.isAudioOnly && !playback.isLoading)
              const ColoredBox(
                color: Color(0x99000000),
                child: Center(
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(LucideIcons.headphones, color: CarTheme.text),
                      SizedBox(width: 8),
                      Text('Audio saja', style: CarTheme.body),
                    ],
                  ),
                ),
              ),
            if (playback.isLoading)
              const ColoredBox(
                color: Color(0x99000000),
                child: Center(
                  child: SpinKitThreeBounce(color: CarTheme.accent, size: 20),
                ),
              ),
            if (playback.error != null && !playback.isLoading)
              ColoredBox(
                color: const Color(0xE6000000),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        playback.error!,
                        textAlign: TextAlign.center,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: CarTheme.body,
                      ),
                      const SizedBox(height: 10),
                      FilledButton.icon(
                        onPressed: playback.retry,
                        icon: const Icon(LucideIcons.rotateCcw, size: 18),
                        label: const Text('Ulangi'),
                      ),
                    ],
                  ),
                ),
              ),
            if (playback.isLive)
              const Positioned(
                top: 10,
                left: 10,
                child: Tag('LIVE', color: CarTheme.live),
              ),
            Positioned(
              top: 10,
              right: 10,
              child: ValueListenableBuilder<String?>(
                valueListenable: playback.sponsorNotice,
                builder: (context, notice, _) {
                  if (notice == null) return const SizedBox.shrink();
                  return Tag(
                    notice,
                    color: CarTheme.positive,
                    textColor: CarTheme.bg,
                  );
                },
              ),
            ),
            if (playback.hasVideo && !playback.isLoading)
              Positioned(
                right: 6,
                bottom: 6,
                child: Material(
                  color: const Color(0x99000000),
                  shape: const CircleBorder(),
                  child: IconButton(
                    onPressed: onOpenFullscreen,
                    tooltip: 'Layar penuh',
                    icon: const Icon(
                      LucideIcons.maximize,
                      color: CarTheme.text,
                      size: 20,
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
