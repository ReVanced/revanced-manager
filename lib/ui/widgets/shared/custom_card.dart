import 'package:flutter/material.dart';

class CustomCard extends StatelessWidget {
  final bool isFilled;
  final Widget child;

  const CustomCard({
    Key? key,
    this.isFilled = true,
    required this.child,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: isFilled
            ? Theme.of(context).colorScheme.secondaryContainer
            : Colors.transparent,
        border: isFilled
            ? null
            : Border.all(
                width: 1,
                color: Theme.of(context).colorScheme.secondary,
              ),
      ),
      padding: const EdgeInsets.all(20),
      child: child,
    );
  }
}
