import 'package:flutter/material.dart';

class DashboardChip extends StatelessWidget {
  final Widget label;
  final bool isSelected;
  final Function(bool)? onSelected;

  const DashboardChip({
    Key? key,
    required this.label,
    required this.isSelected,
    this.onSelected,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return RawChip(
      showCheckmark: false,
      label: label,
      selected: isSelected,
      labelStyle: Theme.of(context).textTheme.subtitle2!.copyWith(
            color: isSelected
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.secondary,
            fontWeight: FontWeight.w500,
          ),
      backgroundColor: Colors.transparent,
      selectedColor: Theme.of(context).colorScheme.secondaryContainer,
      padding: const EdgeInsets.all(8),
      onSelected: onSelected,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: isSelected
            ? BorderSide(
              width: 0.2,
              color: Theme.of(context).colorScheme.secondaryContainer,
            )
            : BorderSide(
                width: 0.2,
                color: Theme.of(context).colorScheme.secondary,
              ),
      ),
    );
  }
}
