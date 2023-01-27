import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';

class SettingsTileDialog extends StatelessWidget {
  const SettingsTileDialog({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.padding,
  }) : super(key: key);
  final String title;
  final String subtitle;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: padding ?? EdgeInsets.zero,
      title: I18nText(
        title,
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText(subtitle),
      onTap: onTap,
    );
  }
}
