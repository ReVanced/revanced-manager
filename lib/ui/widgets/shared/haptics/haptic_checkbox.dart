import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HapticCheckbox extends StatelessWidget {
  const HapticCheckbox({
    super.key,
    required this.value,
    required this.onChanged,
    this.activeColor,
    this.checkColor,
    this.side,
  });
  final bool value;
  final Function(bool?)? onChanged;
  final Color? activeColor;
  final Color? checkColor;
  final BorderSide? side;

  @override
  Widget build(BuildContext context) {
    return Checkbox(
      value: value,
      onChanged: (value) => {
        HapticFeedback.selectionClick(),
        if (onChanged != null) onChanged!(value),
      },
      activeColor: activeColor,
      checkColor: checkColor,
      side: side,
    );
  }
}
