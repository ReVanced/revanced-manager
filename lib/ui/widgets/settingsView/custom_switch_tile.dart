import 'package:flutter/material.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch.dart';

class CustomSwitchTile extends StatelessWidget {
  final Widget title;
  final Widget subtitle;
  final bool value;
  final Function(bool) onTap;
  final EdgeInsetsGeometry? padding;

  const CustomSwitchTile({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onTap,
    this.padding,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: padding ?? EdgeInsets.zero,
      title: title,
      subtitle: subtitle,
      onTap: () => onTap(!value),
      trailing: CustomSwitch(
        value: value,
        onChanged: onTap,
      ),
    );
  }
}
