import 'package:flutter/material.dart';

class CustomCard extends StatelessWidget {
  final bool isFilled;
  final Widget child;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;

  const CustomCard({
    Key? key,
    this.isFilled = true,
    required this.child,
    this.onTap,
    this.padding,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(
      type: isFilled ? MaterialType.card : MaterialType.transparency,
      color: isFilled
          ? Theme.of(context).colorScheme.secondaryContainer.withOpacity(0.4)
          : Colors.transparent,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: padding ?? const EdgeInsets.all(20.0),
          child: child,
        ),
      ),
    );
  }
}
