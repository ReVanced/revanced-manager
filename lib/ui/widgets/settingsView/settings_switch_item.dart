import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch.dart';

class SettingsSwitchItem extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool value;
  final Function(bool) onTap;

  const SettingsSwitchItem({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: I18nText(
        title,
        child: Text(
          '',
          style: kSettingItemTextStyle,
        ),
      ),
      subtitle: I18nText(subtitle),
      trailing: CustomSwitch(
        value: value,
        onChanged: onTap,
      ),
    );
  }
}
