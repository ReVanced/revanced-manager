import 'package:flutter/material.dart';

class CustomPopupMenu extends StatelessWidget {
  final Function(dynamic) onSelected;
  final Map<int, Widget> children;

  const CustomPopupMenu({
    Key? key,
    required this.onSelected,
    required this.children,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Theme(
      data: Theme.of(context).copyWith(useMaterial3: false),
      child: PopupMenuButton<int>(
        icon: Icon(
          Icons.more_vert,
          color: Theme.of(context).colorScheme.secondary,
        ),
        onSelected: onSelected,
        itemBuilder: (context) => children.entries
            .map(
              (entry) => PopupMenuItem<int>(
                padding: const EdgeInsets.all(16.0).copyWith(right: 20),
                value: entry.key,
                child: entry.value,
              ),
            )
            .toList(),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
        color: Theme.of(context).colorScheme.secondaryContainer,
        position: PopupMenuPosition.under,
      ),
    );
  }
}
