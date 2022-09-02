import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/constants.dart';

class SettingsTileDialog extends StatelessWidget {
  final String title;
  final String subtitle;
  final List<Widget> children;

  const SettingsTileDialog({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.children,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: I18nText(
        title,
        child: Text(
          '',
          style: kSettingItemTextStyle,
        ),
      ),
      subtitle: I18nText(subtitle),
      onTap: () => showDialog(
        context: context,
        builder: (context) => SimpleDialog(
          title: I18nText(title),
          backgroundColor: Theme.of(context).colorScheme.surface,
          children: children,
        ),
      ),
    );
  }
}
