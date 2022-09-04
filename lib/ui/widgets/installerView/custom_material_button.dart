import 'package:flutter/material.dart';

class CustomMaterialButton extends StatelessWidget {
  final Widget label;
  final bool isFilled;
  final bool isExpanded;
  final Function()? onPressed;

  const CustomMaterialButton({
    Key? key,
    required this.label,
    this.isFilled = true,
    this.isExpanded = false,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: ButtonStyle(
        padding: MaterialStateProperty.all(
          isExpanded
              ? const EdgeInsets.symmetric(horizontal: 24, vertical: 12)
              : const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        ),
        shape: MaterialStateProperty.all(const StadiumBorder()),
        backgroundColor: MaterialStateProperty.all(
          isFilled ? Theme.of(context).colorScheme.primary : Colors.transparent,
        ),
        foregroundColor: MaterialStateProperty.all(
          isFilled
              ? Theme.of(context).colorScheme.surface
              : Theme.of(context).colorScheme.primary,
        ),
      ),
      onPressed: onPressed,
      child: label,
    );
  }
}
