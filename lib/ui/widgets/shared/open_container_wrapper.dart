import 'package:animations/animations.dart';
import 'package:flutter/material.dart';

class OpenContainerWrapper extends StatelessWidget {
  final OpenContainerBuilder openBuilder;
  final CloseContainerBuilder closedBuilder;

  const OpenContainerWrapper({
    Key? key,
    required this.openBuilder,
    required this.closedBuilder,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return OpenContainer(
      openBuilder: openBuilder,
      closedBuilder: closedBuilder,
      transitionType: ContainerTransitionType.fade,
      transitionDuration: const Duration(milliseconds: 400),
      openColor: Theme.of(context).colorScheme.primary,
      closedColor: Colors.transparent,
      closedShape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
    );
  }
}
