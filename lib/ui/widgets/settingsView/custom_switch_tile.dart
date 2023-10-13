import 'package:flutter/material.dart';

class CustomSwitchTile extends StatelessWidget {
  const CustomSwitchTile({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onTap,
    this.padding,
  }) : super(key: key);
  final Widget title;
  final Widget subtitle;
  final bool value;
  final Function(bool) onTap;
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: padding ?? EdgeInsets.zero,
      title: title,
      subtitle: subtitle,
      onTap: () => onTap(!value),
      trailing: Switch(
        value: value,
        onChanged: onTap,
      ),
    );
  }
}
