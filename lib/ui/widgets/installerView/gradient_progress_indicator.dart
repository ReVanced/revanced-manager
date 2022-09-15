import 'package:flutter/material.dart';

class GradientProgressIndicator extends StatefulWidget {
  final double? progress;
  const GradientProgressIndicator({required this.progress, super.key});

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
        width: MediaQuery.of(context).size.width * widget.progress!,
      ),
    );
  }
}
