import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../controllers/location_controller.dart';
import '../controllers/navigation_controller.dart';
import '../controllers/playback_controller.dart';
import '../models/car_video.dart';
import '../models/navigation.dart';
import '../theme/car_theme.dart';
import '../widgets/car_map.dart';
import '../widgets/navigation/maneuver_banner.dart';
import '../widgets/now_playing.dart';

/// Dashboard: a glanceable map on the left, what plays and quick actions on the right.
class HomeSection extends StatelessWidget {
  const HomeSection({
    super.key,
    required this.playback,
    required this.location,
    required this.navigation,
    required this.active,
    required this.tilesEnabled,
    required this.resumeVideo,
    required this.onResume,
    required this.onOpenMaps,
    required this.onOpenMusic,
    required this.onOpenYouTube,
    required this.onTrending,
    required this.onLive,
    required this.onSearch,
    required this.onNavigate,
  });

  final PlaybackController playback;
  final LocationController location;
  final NavigationController navigation;
  final bool active;
  final bool tilesEnabled;
  final CarVideo? resumeVideo;
  final VoidCallback onResume;
  final VoidCallback onOpenMaps;
  final VoidCallback onOpenMusic;
  final VoidCallback onOpenYouTube;
  final VoidCallback onTrending;
  final VoidCallback onLive;
  final VoidCallback onSearch;

  /// Opens the destination search in the Maps menu.
  final VoidCallback onNavigate;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            flex: 11,
            child: SurfaceCard(
              clip: true,
              padding: EdgeInsets.zero,
              // Rebuilt on navigation state changes only; the banner and the map's
              // route layer follow the GPS fixes themselves.
              child: ListenableBuilder(
                listenable: navigation,
                builder: (context, _) {
                  final guiding = navigation.state == NavState.navigating ||
                      navigation.state == NavState.arrived;
                  return Stack(
                    children: [
                      Positioned.fill(
                        child: CarMap(
                          location: location,
                          navigation: navigation,
                          active: active,
                          interactive: false,
                          showControls: false,
                          tilesEnabled: tilesEnabled,
                          overlayInsets: guiding
                              ? const EdgeInsets.only(top: 64)
                              : EdgeInsets.zero,
                          onTap: onOpenMaps,
                        ),
                      ),
                      if (guiding)
                        Positioned(
                          top: 10,
                          left: 10,
                          right: 10,
                          child: GestureDetector(
                            onTap: onOpenMaps,
                            child: ManeuverBanner(
                              navigation: navigation,
                              compact: true,
                            ),
                          ),
                        )
                      else
                        Positioned(
                          right: 14,
                          bottom: 14,
                          child: FilledButton.icon(
                            onPressed: onNavigate,
                            icon: const Icon(LucideIcons.navigation, size: 20),
                            label: const Text('Navigasi'),
                          ),
                        ),
                    ],
                  );
                },
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 9,
            child: Column(
              children: [
                Expanded(
                  child: NowPlayingCard(
                    playback: playback,
                    onOpen: onOpenMusic,
                    resumeVideo: resumeVideo,
                    onResume: onResume,
                    onBrowse: onOpenYouTube,
                  ),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  height: 72,
                  child: Row(
                    children: [
                      _QuickAction(
                        icon: LucideIcons.zap,
                        label: 'Trending',
                        onTap: onTrending,
                      ),
                      const SizedBox(width: 10),
                      _QuickAction(
                        icon: LucideIcons.radio,
                        label: 'Live',
                        onTap: onLive,
                      ),
                      const SizedBox(width: 10),
                      _QuickAction(
                        icon: LucideIcons.search,
                        label: 'Cari',
                        onTap: onSearch,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: SurfaceCard(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 22, color: CarTheme.accent),
            const SizedBox(height: 4),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: CarTheme.body.copyWith(fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}
