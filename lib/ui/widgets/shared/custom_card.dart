import 'package:flutter/material.dart';

class CustomCard extends StatelessWidget {
  const CustomCard({
    super.key,
    this.isFilled = true,
    required this.child,
    this.onTap,
    this.padding,
    this.backgroundColor,
  });
  final bool isFilled;
  final Widget child;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;
  final Color? backgroundColor;

  @override
  Widget build(BuildContext context) {
    return Material(
      type: isFilled ? MaterialType.card : MaterialType.transparency,
      color: isFilled
          ? backgroundColor?.withOpacity(0.4) ??
              Theme.of(context).colorScheme.secondaryContainer.withOpacity(0.4)
          : backgroundColor ?? Colors.transparent,
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
