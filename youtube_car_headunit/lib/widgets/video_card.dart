import 'package:flutter/material.dart';

import '../models/car_video.dart';
import '../theme/car_theme.dart';
import '../utils/format.dart';
import 'now_playing.dart';

/// One video in the grid: thumbnail with duration or LIVE, title and channel.
class VideoCard extends StatelessWidget {
  final CarVideo video;
  final VoidCallback onTap;
  final bool isSelected;

  const VideoCard({
    super.key,
    required this.video,
    required this.onTap,
    this.isSelected = false,
  });

  /// Search results carry no duration for streams that are live right now.
  bool get _isLive => video.isLive || video.duration == Duration.zero;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(CarTheme.radius),
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AspectRatio(
              aspectRatio: 16 / 9,
              child: DecoratedBox(
                position: DecorationPosition.foreground,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(CarTheme.radius),
                  border: isSelected
                      ? Border.all(color: CarTheme.accent, width: 3)
                      : null,
                ),
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    // Decode at display size: a large win in RAM and CPU on the A6+.
                    Artwork(url: video.thumbnail, cacheWidth: 360),
                    Positioned(
                      right: 6,
                      bottom: 6,
                      child: _isLive
                          ? const Tag('LIVE', color: CarTheme.live)
                          : Tag(
                              formatDuration(video.duration),
                              color: const Color(0xCC000000),
                            ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              video.title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: CarTheme.body.copyWith(
                fontSize: 14,
                height: 1.25,
                color: isSelected ? CarTheme.accent : CarTheme.text,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              video.uploader,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: CarTheme.caption.copyWith(fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}
