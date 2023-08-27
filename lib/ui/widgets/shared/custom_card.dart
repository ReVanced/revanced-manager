import 'package:flutter/material.dart';

class CustomCard extends StatelessWidget {
  const CustomCard({
    Key? key,
    this.isFilled = true,
    required this.child,
    this.onTap,
    this.padding,
    this.backgroundColor,
  }) : super(key: key);
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
          padding: padding ?? const EdgeInsets.only(top: 20.0, bottom: 20, left: 10),
          child: child,
        ),
      ),
    );
  }
}
