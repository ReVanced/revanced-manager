import 'package:flutter/material.dart';

class SettingsTileDialog extends StatelessWidget {
  const SettingsTileDialog({
    super.key,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.padding,
  });
  final String title;
  final String subtitle;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: padding ?? EdgeInsets.zero,
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(subtitle),
      onTap: onTap,
    );
  }
}
