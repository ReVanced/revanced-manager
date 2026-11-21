import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';

import '../controllers/playback_controller.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';
import '../widgets/now_playing.dart';
import '../widgets/seek_bar.dart';

/// Big-control now-playing screen for use while driving: artwork (or the video), title,
/// seek bar, large transport buttons, Audio Only and autoplay switches, what comes next.
/// Swipe left or right anywhere to skip.
class MusicSection extends StatelessWidget {
  const MusicSection({
    super.key,
    required this.playback,
    required this.onOpenYouTube,
  });

  final PlaybackController playback;
  final VoidCallback onOpenYouTube;

  static const _skip = Duration(seconds: 10);

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: playback,
      builder: (context, _) {
        final video = playback.current;
        if (video == null) return _empty();
        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onHorizontalDragEnd: (details) {
            final velocity = details.primaryVelocity ?? 0;
            if (velocity < -300) playback.next();
            if (velocity > 300) playback.previous();
          },
          child: LayoutBuilder(
            builder: (context, constraints) {
              final wide = constraints.maxWidth >= 880;
              final artSide = (constraints.maxHeight - 40)
                  .clamp(120.0, constraints.maxWidth * (wide ? 0.32 : 0.36));
              return Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
                child: Row(
                  children: [
                    SizedBox.square(
                      dimension: artSide,
                      child: _Picture(playback: playback, video: video),
                    ),
                    const SizedBox(width: 28),
                    Expanded(child: _details(video, showNext: !wide)),
                    if (wide) ...[
                      const SizedBox(width: 20),
                      SizedBox(width: 260, child: _UpNextList(playback: playback)),
                    ],
                  ],
                ),
              );
            },
          ),
        );
      },
    );
  }

  Widget _details(CarVideo video, {required bool showNext}) {
    final next = playback.upNext(1);
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            if (playback.isLive)
              const Tag('LIVE', color: CarTheme.live)
            else
              Tag(playback.servedLabel),
          ],
        ),
        const SizedBox(height: 10),
        Text(
          video.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: CarTheme.title,
        ),
        const SizedBox(height: 4),
        Text(
          video.uploader,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: CarTheme.caption.copyWith(fontSize: 15),
        ),
        const SizedBox(height: 12),
        SeekBar(
          progress: playback.progress,
          onSeek: playback.seek,
          enabled: playback.canSeek,
          isLive: playback.isLive,
          large: true,
        ),
        const SizedBox(height: 4),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            ControlButton(
              icon: LucideIcons.rotateCcw,
              size: 52,
              onPressed:
                  playback.canSeek ? () => playback.seekRelative(-_skip) : null,
              tooltip: 'Mundur 10 detik',
            ),
            ControlButton(
              icon: LucideIcons.skipBack,
              size: 64,
              onPressed: playback.previous,
              tooltip: 'Sebelumnya',
            ),
            ControlButton(
              icon: playback.isPlaying ? LucideIcons.pause : LucideIcons.play,
              size: 80,
              primary: true,
              loading: playback.isLoading,
              onPressed: playback.togglePlay,
              tooltip: playback.isPlaying ? 'Jeda' : 'Putar',
            ),
            ControlButton(
              icon: LucideIcons.skipForward,
              size: 64,
              onPressed: playback.next,
              tooltip: 'Berikutnya',
            ),
            ControlButton(
              icon: LucideIcons.rotateCw,
              size: 52,
              onPressed:
                  playback.canSeek ? () => playback.seekRelative(_skip) : null,
              tooltip: 'Maju 10 detik',
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            _Toggle(
              icon: LucideIcons.headphones,
              label: 'Audio saja',
              value: playback.quality.isAudioOnly,
              onChanged: playback.setAudioOnly,
            ),
            const SizedBox(width: 10),
            _Toggle(
              icon: LucideIcons.repeat,
              label: 'Autoplay',
              value: playback.autoplay,
              onChanged: (_) => playback.toggleAutoplay(),
            ),
          ],
        ),
        if (showNext && next.isNotEmpty) ...[
          const SizedBox(height: 12),
          UpNextRow(video: next.first, onTap: playback.next),
        ],
      ],
    );
  }

  Widget _empty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(LucideIcons.music2, size: 40, color: CarTheme.textFaint),
          const SizedBox(height: 12),
          const Text('Belum ada yang diputar', style: CarTheme.heading),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: onOpenYouTube,
            icon: const Icon(LucideIcons.youtube, size: 20),
            label: const Text('Buka YouTube'),
          ),
        ],
      ),
    );
  }
}

class _Picture extends StatelessWidget {
  const _Picture({required this.playback, required this.video});

  final PlaybackController playback;
  final CarVideo video;

  @override
  Widget build(BuildContext context) {
    final controller = playback.videoController;
    if (!playback.hasVideo || controller == null) {
      return Artwork(
        url: video.largeThumbnail,
        fallbackUrl: video.thumbnail,
        radius: CarTheme.radiusLarge,
        cacheWidth: 720,
      );
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(CarTheme.radiusLarge),
      child: ColoredBox(
        color: Colors.black,
        child: FittedBox(
          fit: BoxFit.cover,
          clipBehavior: Clip.hardEdge,
          child: SizedBox(
            width: controller.value.size.width,
            height: controller.value.size.height,
            child: VideoPlayer(controller),
          ),
        ),
      ),
    );
  }
}

class _Toggle extends StatelessWidget {
  const _Toggle({
    required this.icon,
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final IconData icon;
  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final color = value ? CarTheme.accent : CarTheme.textMuted;
    return Material(
      color: value ? CarTheme.accentSoft : CarTheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(CarTheme.radius),
        side: BorderSide(color: value ? CarTheme.accent : CarTheme.border),
      ),
      child: InkWell(
        onTap: () => onChanged(!value),
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(CarTheme.radius),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 18, color: color),
              const SizedBox(width: 8),
              Text(label, style: CarTheme.body.copyWith(color: color)),
            ],
          ),
        ),
      ),
    );
  }
}

class _UpNextList extends StatelessWidget {
  const _UpNextList({required this.playback});

  final PlaybackController playback;

  @override
  Widget build(BuildContext context) {
    final items = playback.upNext(6);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Berikutnya', style: CarTheme.heading),
        const SizedBox(height: 10),
        if (items.isEmpty)
          const Text('Antrian kosong', style: CarTheme.caption)
        else
          Expanded(
            child: ListView.separated(
              itemCount: items.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) => UpNextRow(
                video: items[index],
                showLabel: false,
                onTap: () => playback.select(items[index]),
              ),
            ),
          ),
      ],
    );
  }
}
