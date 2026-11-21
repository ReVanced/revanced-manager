import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../controllers/playback_controller.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';
import 'seek_bar.dart';

/// Video thumbnail with a quiet placeholder, decoded at display size.
class Artwork extends StatelessWidget {
  const Artwork({
    super.key,
    required this.url,
    this.radius = CarTheme.radius,
    this.cacheWidth = 320,
    this.fallbackUrl,
  });

  final String url;
  final double radius;
  final int cacheWidth;

  /// Tried when [url] fails, e.g. a missing high-resolution thumbnail.
  final String? fallbackUrl;

  @override
  Widget build(BuildContext context) {
    const placeholder = ColoredBox(
      color: CarTheme.surfaceHigh,
      child: Center(
        child: Icon(LucideIcons.music2, color: CarTheme.textFaint, size: 28),
      ),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: url.isEmpty
          ? placeholder
          : Image.network(
              url,
              fit: BoxFit.cover,
              cacheWidth: cacheWidth,
              filterQuality: FilterQuality.low,
              // Quiet placeholder until the first frame, so large artwork never leaves
              // an empty hole while it downloads.
              frameBuilder: (context, child, frame, wasSynchronouslyLoaded) =>
                  frame == null && !wasSynchronouslyLoaded ? placeholder : child,
              errorBuilder: (_, __, ___) => fallbackUrl == null
                  ? placeholder
                  : Image.network(
                      fallbackUrl!,
                      fit: BoxFit.cover,
                      cacheWidth: cacheWidth,
                      filterQuality: FilterQuality.low,
                      errorBuilder: (_, __, ___) => placeholder,
                    ),
            ),
    );
  }
}

/// Dashboard card: what plays, seek bar and big controls, or a way to start something.
class NowPlayingCard extends StatelessWidget {
  const NowPlayingCard({
    super.key,
    required this.playback,
    required this.onOpen,
    this.resumeVideo,
    this.onResume,
    this.onBrowse,
  });

  final PlaybackController playback;

  /// Opens the full Musik screen.
  final VoidCallback onOpen;

  /// Last video of a previous session, offered when nothing plays.
  final CarVideo? resumeVideo;
  final VoidCallback? onResume;
  final VoidCallback? onBrowse;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: playback,
      builder: (context, _) {
        final video = playback.current;
        if (video == null) return _empty();
        return SurfaceCard(
          onTap: onOpen,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox.square(
                    dimension: 76,
                    child: Artwork(url: video.thumbnail),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          video.title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: CarTheme.heading,
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Flexible(
                              child: Text(
                                video.uploader,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: CarTheme.caption,
                              ),
                            ),
                            const SizedBox(width: 8),
                            if (playback.isLive)
                              const Tag('LIVE', color: CarTheme.live)
                            else
                              Tag(playback.servedLabel),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const Spacer(),
              if (playback.upNext(1) case [final next])
                UpNextRow(video: next, onTap: playback.next),
              const Spacer(),
              SeekBar(
                progress: playback.progress,
                onSeek: playback.seek,
                enabled: playback.canSeek,
                isLive: playback.isLive,
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ControlButton(
                    icon: LucideIcons.skipBack,
                    size: 56,
                    onPressed: playback.previous,
                    tooltip: 'Sebelumnya',
                  ),
                  const SizedBox(width: 20),
                  ControlButton(
                    icon: playback.isPlaying
                        ? LucideIcons.pause
                        : LucideIcons.play,
                    size: 68,
                    primary: true,
                    loading: playback.isLoading,
                    onPressed: playback.togglePlay,
                    tooltip: playback.isPlaying ? 'Jeda' : 'Putar',
                  ),
                  const SizedBox(width: 20),
                  ControlButton(
                    icon: LucideIcons.skipForward,
                    size: 56,
                    onPressed: playback.next,
                    tooltip: 'Berikutnya',
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _empty() {
    final resume = resumeVideo;
    return SurfaceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Belum ada yang diputar', style: CarTheme.heading),
          const SizedBox(height: 6),
          const Text(
            'Pilih musik atau video dari menu YouTube.',
            style: CarTheme.caption,
          ),
          const Spacer(),
          if (resume != null && onResume != null) ...[
            Material(
              color: CarTheme.surfaceHigh,
              borderRadius: BorderRadius.circular(CarTheme.radius),
              child: InkWell(
                onTap: onResume,
                borderRadius: BorderRadius.circular(CarTheme.radius),
                child: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Row(
                    children: [
                      SizedBox(
                        width: 72,
                        height: 44,
                        child: Artwork(
                          url: resume.thumbnail,
                          radius: CarTheme.radiusSmall,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Lanjutkan', style: CarTheme.label),
                            Text(
                              resume.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: CarTheme.body,
                            ),
                          ],
                        ),
                      ),
                      const Icon(LucideIcons.play, color: CarTheme.accent),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
          ],
          if (onBrowse != null)
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: onBrowse,
                icon: const Icon(LucideIcons.youtube, size: 20),
                label: const Text('Buka YouTube'),
              ),
            ),
        ],
      ),
    );
  }
}

/// Compact now-playing pill floating over the map.
class NowPlayingMini extends StatelessWidget {
  const NowPlayingMini({
    super.key,
    required this.playback,
    required this.onOpen,
  });

  final PlaybackController playback;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: playback,
      builder: (context, _) {
        final video = playback.current;
        if (video == null) return const SizedBox.shrink();
        return SizedBox(
          width: 340,
          child: SurfaceCard(
            color: CarTheme.surfaceHigh,
            padding: const EdgeInsets.all(8),
            onTap: onOpen,
            child: Row(
              children: [
                SizedBox.square(
                  dimension: 52,
                  child: Artwork(
                    url: video.thumbnail,
                    radius: CarTheme.radiusSmall,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        video.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: CarTheme.body,
                      ),
                      Text(
                        video.uploader,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: CarTheme.caption,
                      ),
                    ],
                  ),
                ),
                ControlButton(
                  icon: playback.isPlaying ? LucideIcons.pause : LucideIcons.play,
                  size: 52,
                  primary: true,
                  loading: playback.isLoading,
                  onPressed: playback.togglePlay,
                ),
                ControlButton(
                  icon: LucideIcons.skipForward,
                  size: 52,
                  onPressed: playback.next,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// "Berikutnya" line: small thumbnail and title of the next video; tap to skip to it.
class UpNextRow extends StatelessWidget {
  const UpNextRow({
    super.key,
    required this.video,
    required this.onTap,
    this.showLabel = true,
  });

  final CarVideo video;
  final VoidCallback onTap;
  final bool showLabel;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(CarTheme.radius),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            SizedBox(
              width: 64,
              height: 36,
              child: Artwork(url: video.thumbnail, radius: 8, cacheWidth: 160),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (showLabel)
                    const Text('Berikutnya', style: CarTheme.label),
                  Text(
                    video.title,
                    maxLines: showLabel ? 1 : 2,
                    overflow: TextOverflow.ellipsis,
                    style: CarTheme.body,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
