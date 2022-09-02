import 'package:flutter/material.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch.dart';

class CustomSwitchTile extends StatelessWidget {
  final Widget title;
  final Widget subtitle;
  final bool value;
  final Function(bool) onTap;

  const CustomSwitchTile({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: title,
      subtitle: subtitle,
      trailing: CustomSwitch(
        value: value,
        onChanged: onTap,
      ),
    );
  }
}
