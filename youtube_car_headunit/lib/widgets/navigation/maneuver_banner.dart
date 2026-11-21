import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../controllers/navigation_controller.dart';
import '../../models/navigation.dart';
import '../../theme/car_theme.dart';
import 'maneuver_icon.dart';
import 'nav_format.dart';

/// The next maneuver while navigating: arrow, distance, instruction, and the one after.
///
/// Rebuilds on every GPS fix through [NavigationController.progress]; the section around
/// it only rebuilds when the navigation state changes.
class ManeuverBanner extends StatelessWidget {
  const ManeuverBanner({super.key, required this.navigation, this.compact = false});

  final NavigationController navigation;

  /// One row for the Home map card instead of the full card of the Maps menu.
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: navigation,
      builder: (context, _) {
        if (navigation.state == NavState.arrived) {
          return compact
              ? const _CompactRow(
                  icon: Icon(LucideIcons.flag, size: 26, color: CarTheme.accent),
                  title: 'Tiba',
                  subtitle: 'Anda telah tiba di tujuan',
                )
              : _ArrivalCard(navigation: navigation);
        }
        if (navigation.rerouting) {
          return compact
              ? const _CompactRow(
                  icon: _Spinner(size: 22),
                  title: 'Rute',
                  subtitle: 'Menghitung ulang rute...',
                )
              : const _StatusCard(
                  icon: _Spinner(size: 28),
                  text: 'Menghitung ulang rute...',
                );
        }
        return ValueListenableBuilder<NavProgress?>(
          valueListenable: navigation.progress,
          builder: (context, progress, _) {
            if (progress == null) {
              return compact
                  ? const _CompactRow(
                      icon: Icon(LucideIcons.navigation2,
                          size: 26, color: CarTheme.accent),
                      title: '--',
                      subtitle: 'Menunggu posisi GPS...',
                    )
                  : const _StatusCard(
                      icon: Icon(LucideIcons.navigation2,
                          size: 28, color: CarTheme.accent),
                      text: 'Menunggu posisi GPS...',
                    );
            }
            return compact
                ? _CompactRow(
                    icon: ManeuverIcon(
                      progress.nextStep,
                      size: 26,
                      color: CarTheme.accent,
                    ),
                    title: formatDistance(progress.distanceToManeuver),
                    subtitle: progress.nextStep.instruction,
                  )
                : _FullBanner(progress: progress);
          },
        );
      },
    );
  }
}

class _FullBanner extends StatelessWidget {
  const _FullBanner({required this.progress});

  final NavProgress progress;

  @override
  Widget build(BuildContext context) {
    final next = progress.nextStep;
    final after = progress.stepAfter;
    return SurfaceCard(
      color: CarTheme.surfaceHigh,
      padding: const EdgeInsets.all(14),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              DecoratedBox(
                decoration: BoxDecoration(
                  color: CarTheme.accent,
                  borderRadius: BorderRadius.circular(CarTheme.radius),
                ),
                child: SizedBox.square(
                  dimension: 64,
                  child: Center(
                    child: ManeuverIcon(next, size: 40, color: CarTheme.onAccent),
                  ),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      formatDistance(progress.distanceToManeuver),
                      maxLines: 1,
                      style: CarTheme.title.copyWith(
                        fontSize: 30,
                        height: 1.1,
                        fontFeatures: const [FontFeature.tabularFigures()],
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      next.instruction,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: CarTheme.heading,
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (after != null) ...[
            const SizedBox(height: 10),
            const Divider(height: 1),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('Lalu', style: CarTheme.label),
                const SizedBox(width: 10),
                ManeuverIcon(after, size: 20, color: CarTheme.textMuted),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    after.instruction,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: CarTheme.caption,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.icon, required this.text});

  final Widget icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      color: CarTheme.surfaceHigh,
      padding: const EdgeInsets.all(14),
      child: SizedBox(
        height: 64,
        child: Row(
          children: [
            SizedBox.square(dimension: 64, child: Center(child: icon)),
            const SizedBox(width: 14),
            Expanded(
              child: Text(
                text,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: CarTheme.heading,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ArrivalCard extends StatelessWidget {
  const _ArrivalCard({required this.navigation});

  final NavigationController navigation;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      color: CarTheme.surfaceHigh,
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          const SizedBox.square(
            dimension: 64,
            child: Center(
              child: Icon(LucideIcons.flag, size: 36, color: CarTheme.accent),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Tiba di tujuan', style: CarTheme.heading),
                if (navigation.destination case final place?)
                  Text(
                    place.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: CarTheme.caption,
                  ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          FilledButton(onPressed: navigation.stop, child: const Text('Selesai')),
        ],
      ),
    );
  }
}

class _CompactRow extends StatelessWidget {
  const _CompactRow({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  final Widget icon;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: CarTheme.surfaceHigh,
        borderRadius: BorderRadius.circular(CarTheme.radius),
        border: Border.all(color: CarTheme.border),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Row(
          children: [
            SizedBox.square(dimension: 30, child: Center(child: icon)),
            const SizedBox(width: 10),
            Text(
              title,
              style: CarTheme.heading.copyWith(
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: CarTheme.caption,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Next turn in the menu rail, so guidance stays in view in every menu.
class NextTurnTile extends StatelessWidget {
  const NextTurnTile({super.key, required this.navigation, required this.onTap});

  final NavigationController navigation;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Petunjuk arah',
      child: Material(
        color: CarTheme.surfaceHigh,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(CarTheme.radius),
          side: const BorderSide(color: CarTheme.border),
        ),
        child: InkWell(
          onTap: onTap,
          customBorder: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(CarTheme.radius),
          ),
          child: SizedBox(
            width: 84,
            height: 52,
            child: ExcludeSemantics(
              child: ListenableBuilder(
                listenable: navigation,
                builder: (context, _) {
                  if (navigation.state == NavState.arrived) {
                    return const _TileContent(
                      icon: Icon(LucideIcons.flag, size: 22, color: CarTheme.accent),
                      text: 'Tiba',
                    );
                  }
                  if (navigation.rerouting) {
                    return const _TileContent(icon: _Spinner(size: 18), text: 'Rute');
                  }
                  return ValueListenableBuilder<NavProgress?>(
                    valueListenable: navigation.progress,
                    builder: (context, progress, _) => progress == null
                        ? const _TileContent(
                            icon: Icon(LucideIcons.navigation2,
                                size: 22, color: CarTheme.accent),
                            text: '--',
                          )
                        : _TileContent(
                            icon: ManeuverIcon(
                              progress.nextStep,
                              size: 22,
                              color: CarTheme.accent,
                            ),
                            text: formatDistance(progress.distanceToManeuver),
                          ),
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TileContent extends StatelessWidget {
  const _TileContent({required this.icon, required this.text});

  final Widget icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          SizedBox.square(dimension: 24, child: Center(child: icon)),
          const SizedBox(width: 4),
          Flexible(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                text,
                style: CarTheme.body.copyWith(
                  fontWeight: FontWeight.w600,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Spinner extends StatelessWidget {
  const _Spinner({required this.size});

  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: size,
      child: const CircularProgressIndicator(strokeWidth: 2.5),
    );
  }
}
