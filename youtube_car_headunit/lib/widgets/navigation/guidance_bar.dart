import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../controllers/navigation_controller.dart';
import '../../models/navigation.dart';
import '../../theme/car_theme.dart';
import '../../utils/format.dart';
import 'nav_format.dart';

/// Arrival time, remaining time and distance, the voice toggle and "Akhiri".
class GuidanceBar extends StatelessWidget {
  const GuidanceBar({
    super.key,
    required this.navigation,
    this.muted,
    this.onToggleMute,
  });

  final NavigationController navigation;

  /// Voice state; the toggle is hidden without it.
  final ValueListenable<bool>? muted;
  final VoidCallback? onToggleMute;

  @override
  Widget build(BuildContext context) {
    final muted = this.muted;
    return SurfaceCard(
      color: CarTheme.surfaceHigh,
      padding: const EdgeInsets.fromLTRB(16, 10, 10, 10),
      child: Row(
        children: [
          Expanded(
            child: ValueListenableBuilder<NavProgress?>(
              valueListenable: navigation.progress,
              builder: (context, progress, _) {
                final route = navigation.route;
                final remaining = progress?.timeRemaining ?? route?.duration;
                final distance = progress?.distanceRemaining ?? route?.distance;
                final eta = progress?.eta ??
                    (remaining == null ? null : DateTime.now().add(remaining));
                return Row(
                  children: [
                    // Fixed slot so a wide clock (large system font) shrinks instead of
                    // pushing the buttons off the card.
                    SizedBox(
                      width: 80,
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        alignment: Alignment.centerLeft,
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              eta == null ? '--:--' : formatClock(eta),
                              style: CarTheme.title.copyWith(
                                fontSize: 26,
                                height: 1.1,
                                fontFeatures: const [FontFeature.tabularFigures()],
                              ),
                            ),
                            const Text('Tiba', style: CarTheme.label),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            remaining == null ? '--' : formatTravelTime(remaining),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: CarTheme.heading,
                          ),
                          Text(
                            distance == null ? '--' : formatDistance(distance),
                            maxLines: 1,
                            style: CarTheme.caption,
                          ),
                        ],
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          if (muted != null)
            ValueListenableBuilder<bool>(
              valueListenable: muted,
              builder: (context, isMuted, _) => ControlButton(
                icon: isMuted ? LucideIcons.volumeX : LucideIcons.volume2,
                size: 52,
                tooltip: isMuted ? 'Nyalakan suara' : 'Bisukan suara',
                onPressed: onToggleMute,
              ),
            ),
          const SizedBox(width: 6),
          FilledButton(
            onPressed: navigation.stop,
            style: FilledButton.styleFrom(
              backgroundColor: CarTheme.surface,
              foregroundColor: CarTheme.text,
              minimumSize: const Size(84, 52),
              padding: const EdgeInsets.symmetric(horizontal: 14),
              side: const BorderSide(color: CarTheme.border),
            ),
            child: const Text('Akhiri'),
          ),
        ],
      ),
    );
  }
}
