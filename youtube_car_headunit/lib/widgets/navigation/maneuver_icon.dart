import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../models/navigation.dart';

/// Arrow for a maneuver, drawn from Lucide's outline icons.
///
/// Indonesia drives on the left: roundabouts turn clockwise and a U-turn goes to the
/// right, so those two shapes are mirrored from the right-hand-traffic icons.
class ManeuverIcon extends StatelessWidget {
  const ManeuverIcon(this.step, {super.key, this.size = 40, this.color});

  final RouteStep step;
  final double size;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final color = this.color ?? IconTheme.of(context).color;
    final (data, turn) = _shape(step);
    final icon = Icon(data, size: size, color: color);
    return switch (turn) {
      _Turn.none => icon,
      // Lucide's undo-2 is a U lying on its side; turned upright and mirrored it is a
      // U-turn to the right.
      _Turn.uturn => Transform.flip(
          flipX: true,
          child: Transform.rotate(angle: -math.pi / 2, child: icon),
        ),
    };
  }

  static (IconData, _Turn) _shape(RouteStep step) {
    switch (step.type) {
      case ManeuverType.arrive:
        return (LucideIcons.flag, _Turn.none);
      case ManeuverType.roundabout:
      case ManeuverType.rotary:
      case ManeuverType.roundaboutTurn:
      case ManeuverType.exitRoundabout:
      case ManeuverType.exitRotary:
        return (LucideIcons.rotateCw, _Turn.none);
      default:
        break;
    }
    return switch (step.modifier) {
      ManeuverModifier.uturn => (LucideIcons.undo2, _Turn.uturn),
      ManeuverModifier.sharpRight || ManeuverModifier.right => (
          LucideIcons.cornerUpRight,
          _Turn.none
        ),
      ManeuverModifier.slightRight => (LucideIcons.arrowUpRight, _Turn.none),
      ManeuverModifier.slightLeft => (LucideIcons.arrowUpLeft, _Turn.none),
      ManeuverModifier.sharpLeft || ManeuverModifier.left => (
          LucideIcons.cornerUpLeft,
          _Turn.none
        ),
      ManeuverModifier.straight || ManeuverModifier.none => (
          LucideIcons.arrowUp,
          _Turn.none
        ),
    };
  }
}

enum _Turn { none, uturn }
