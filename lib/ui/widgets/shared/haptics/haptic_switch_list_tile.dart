import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HapticSwitchListTile extends StatelessWidget {
  const HapticSwitchListTile({
    super.key,
    required this.value,
    required this.onChanged,
    this.title,
    this.subtitle,
    this.contentPadding,
  });
  final bool value;
  final Function(bool)? onChanged;
  final Widget? title;
  final Widget? subtitle;
  final EdgeInsetsGeometry? contentPadding;

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: contentPadding ?? EdgeInsets.zero,
      title: title,
      subtitle: subtitle,
      value: value,
      onChanged: (value) => {
        if (value) {
          HapticFeedback.mediumImpact(),
        } else {
          HapticFeedback.lightImpact(),
        },
        if (onChanged != null) onChanged!(value),
      },
    );
  }
}
