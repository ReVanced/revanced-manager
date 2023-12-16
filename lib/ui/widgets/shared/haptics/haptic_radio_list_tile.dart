import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HapticRadioListTile extends StatelessWidget {
  const HapticRadioListTile({
    super.key,
    required this.title,
    required this.value,
    required this.groupValue,
    this.subtitle,
    this.onChanged,
    this.contentPadding,
  });
  final Widget title;
  final Widget? subtitle;
  final int value;
  final Function(int?)? onChanged;
  final int groupValue;
  final EdgeInsetsGeometry? contentPadding;

  @override
  Widget build(BuildContext context) {
    return RadioListTile(
      contentPadding: contentPadding ?? EdgeInsets.zero,
      title: title,
      subtitle: subtitle,
      value: value,
      groupValue: groupValue,
      onChanged: (val) => {
        if (val == value) {
          HapticFeedback.lightImpact(),
        },

        if (onChanged != null) onChanged!(val),
      },
    );
  }
}
