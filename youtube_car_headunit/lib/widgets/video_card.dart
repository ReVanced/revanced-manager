import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';

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

  String _formatDuration(Duration d) {
    if (d == Duration.zero) return 'LIVE';
    final minutes = d.inMinutes;
    final seconds = d.inSeconds.remainder(60);
    return '$minutes:${seconds < 10 ? '0' : ''}$seconds';
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: CarTheme.bgSurface,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: isSelected ? CarTheme.accentRed : Colors.white12,
              width: isSelected ? 2 : 1,
            ),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(11),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AspectRatio(
                  aspectRatio: 16 / 9,
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.network(
                        video.thumbnail,
                        fit: BoxFit.cover,
                        // Decode at display size — huge win on A6+ RAM/CPU
                        cacheWidth: 320,
                        cacheHeight: 180,
                        filterQuality: FilterQuality.low,
                        errorBuilder: (_, __, ___) => const ColoredBox(
                          color: CarTheme.bgCard,
                          child: Center(
                            child: Icon(
                              LucideIcons.video,
                              color: CarTheme.textMuted,
                              size: 28,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        bottom: 6,
                        right: 6,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.black87,
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            _formatDuration(video.duration),
                            style: const TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                      if (isSelected)
                        const Positioned(
                          top: 6,
                          left: 6,
                          child: Icon(
                            LucideIcons.playCircle,
                            color: CarTheme.accentRed,
                            size: 22,
                          ),
                        ),
                    ],
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(8, 5, 8, 5),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          video.title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            height: 1.15,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          video.uploader,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 10,
                            color: CarTheme.textMuted,
                            fontWeight: FontWeight.w500,
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
      ),
    );
  }
}
