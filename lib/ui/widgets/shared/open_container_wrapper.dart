import 'package:animations/animations.dart';
import 'package:flutter/material.dart';

class OpenContainerWrapper extends StatelessWidget {
  const OpenContainerWrapper({
    super.key,
    required this.openBuilder,
    required this.closedBuilder,
  });
  final OpenContainerBuilder openBuilder;
  final CloseContainerBuilder closedBuilder;

  @override
  Widget build(BuildContext context) {
    return OpenContainer(
      openBuilder: openBuilder,
      closedBuilder: closedBuilder,
      transitionDuration: const Duration(milliseconds: 400),
      openColor: Theme.of(context).colorScheme.primary,
      closedColor: Colors.transparent,
      closedShape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
    );
  }
}
