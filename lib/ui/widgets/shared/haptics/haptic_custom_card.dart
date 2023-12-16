import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class HapticCustomCard extends StatelessWidget {
  const HapticCustomCard({
    super.key,
    this.isFilled = true,
    required this.child,
    this.onTap,
    this.padding,
    this.backgroundColor,
  });
  final bool isFilled;
  final Widget child;
  final Function()? onTap;
  final EdgeInsetsGeometry? padding;
  final Color? backgroundColor;

  @override
  Widget build(BuildContext context) {
    return CustomCard(
      isFilled: isFilled,
      onTap: () => {
        HapticFeedback.selectionClick(),
        if (onTap != null) onTap!(),
      },
      padding: padding,
      backgroundColor: backgroundColor,
      child: child,
    );
  }
}
