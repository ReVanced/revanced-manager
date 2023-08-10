import 'package:flutter/material.dart';

class GradientProgressIndicator extends StatefulWidget {
  const GradientProgressIndicator({required this.progress, super.key});
  final double? progress;

  @override
  State<GradientProgressIndicator> createState() =>
      _GradientProgressIndicatorState();
}

class _GradientProgressIndicatorState extends State<GradientProgressIndicator> {
  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 500),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [
              Theme.of(context).colorScheme.primary,
              Theme.of(context).colorScheme.secondary,
            ],
          ),
        ),
        height: 5,
        width: MediaQuery.sizeOf(context).width * widget.progress!,
      ),
    );
  }
}
