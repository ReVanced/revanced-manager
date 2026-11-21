import 'dart:async';

import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../theme/car_theme.dart';
import '../utils/format.dart';

enum HeadunitSection {
  home('Home', LucideIcons.home),
  maps('Maps', LucideIcons.map),
  youtube('YouTube', LucideIcons.youtube),
  music('Musik', LucideIcons.music2);

  const HeadunitSection(this.label, this.icon);

  final String label;
  final IconData icon;
}

/// Left menu with the clock on top, the way factory head units place it.
class NavRail extends StatelessWidget {
  const NavRail({
    super.key,
    required this.section,
    required this.onSelect,
    this.isPlaying = false,
    this.turnIndicator,
  });

  final HeadunitSection section;
  final ValueChanged<HeadunitSection> onSelect;

  /// Shows a small dot on Musik while something plays.
  final bool isPlaying;

  /// Next-turn tile shown under the clock while navigating, in place of the date, so
  /// guidance stays visible in every menu. The rail has no height left for both on a
  /// 411 dp tall screen.
  final Widget? turnIndicator;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        color: CarTheme.surface,
        border: Border(right: BorderSide(color: CarTheme.border)),
      ),
      child: SizedBox(
        width: 108,
        child: Column(
          children: [
            const SizedBox(height: 14),
            _Clock(showDate: turnIndicator == null),
            if (turnIndicator case final indicator?) ...[
              const SizedBox(height: 8),
              indicator,
            ],
            const Spacer(),
            for (final item in HeadunitSection.values) ...[
              _RailItem(
                section: item,
                selected: item == section,
                badge: item == HeadunitSection.music && isPlaying,
                onTap: () => onSelect(item),
              ),
              const SizedBox(height: 6),
            ],
            const Spacer(),
          ],
        ),
      ),
    );
  }
}

class _RailItem extends StatelessWidget {
  const _RailItem({
    required this.section,
    required this.selected,
    required this.badge,
    required this.onTap,
  });

  final HeadunitSection section;
  final bool selected;
  final bool badge;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected ? CarTheme.accent : CarTheme.textMuted;
    return Semantics(
      selected: selected,
      button: true,
      label: section.label,
      child: Material(
        color: selected ? CarTheme.accentSoft : Colors.transparent,
        borderRadius: BorderRadius.circular(CarTheme.radius),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(CarTheme.radius),
          child: SizedBox(
            width: 84,
            height: 68,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Icon(section.icon, size: 26, color: color),
                    if (badge)
                      const Positioned(
                        right: -4,
                        top: -2,
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: CarTheme.accent,
                            shape: BoxShape.circle,
                          ),
                          child: SizedBox.square(dimension: 8),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 6),
                ExcludeSemantics(
                  child: Text(
                    section.label,
                    style: CarTheme.label.copyWith(
                      color: selected ? CarTheme.accent : CarTheme.textMuted,
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

/// Minute-accurate clock and date; ticks on the minute boundary instead of every second.
class _Clock extends StatefulWidget {
  const _Clock({this.showDate = true});

  final bool showDate;

  @override
  State<_Clock> createState() => _ClockState();
}

class _ClockState extends State<_Clock> {
  DateTime _now = DateTime.now();
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _schedule();
  }

  void _schedule() {
    final now = DateTime.now();
    final nextMinute = DateTime(now.year, now.month, now.day, now.hour, now.minute)
        .add(const Duration(minutes: 1));
    _timer = Timer(nextMinute.difference(now), () {
      if (!mounted) return;
      setState(() => _now = DateTime.now());
      _schedule();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Scale down instead of wrapping when a large system font would not fit the rail.
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Column(
        children: [
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(formatClock(_now), style: CarTheme.clock),
          ),
          if (widget.showDate) ...[
            const SizedBox(height: 4),
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(formatShortDate(_now), style: CarTheme.caption),
            ),
          ],
        ],
      ),
    );
  }
}
