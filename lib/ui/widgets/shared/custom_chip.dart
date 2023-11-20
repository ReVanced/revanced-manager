import 'package:flutter/material.dart';

class CustomChip extends StatelessWidget {
  const CustomChip({
    super.key,
    required this.label,
    this.isSelected = false,
    this.onSelected,
  });
  final Widget label;
  final bool isSelected;
  final Function(bool)? onSelected;

  @override
  Widget build(BuildContext context) {
    return RawChip(
      showCheckmark: false,
      label: label,
      selected: isSelected,
      labelStyle: Theme.of(context).textTheme.titleSmall!.copyWith(
            color: isSelected
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.secondary,
            fontWeight: FontWeight.w500,
          ),
      backgroundColor: Colors.transparent,
      selectedColor: Theme.of(context).colorScheme.secondaryContainer,
      padding: const EdgeInsets.all(10),
      onSelected: onSelected,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: isSelected
            ? BorderSide.none
            : BorderSide(
                width: 0.2,
                color: Theme.of(context).colorScheme.secondary,
              ),
      ),
    );
  }
}
