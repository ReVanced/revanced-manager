import 'package:flutter/material.dart';

class CustomCard extends StatelessWidget {
  final Widget child;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;

  const CustomCard({
    Key? key,
    required this.child,
    this.onTap,
    this.padding,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.secondaryContainer,
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
