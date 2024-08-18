import 'package:flutter/material.dart';

class CustomSliverAppBar extends StatelessWidget {
  const CustomSliverAppBar({
    super.key,
    required this.title,
    this.actions,
    this.bottom,
    this.isMainView = false,
    this.onBackButtonPressed,
  });
  final Widget title;
  final List<Widget>? actions;
  final PreferredSizeWidget? bottom;
  final bool isMainView;
  final Function()? onBackButtonPressed;

  @override
  Widget build(BuildContext context) {
    return SliverAppBar(
      pinned: true,
      expandedHeight: 100.0,
      automaticallyImplyLeading: !isMainView,
      flexibleSpace: FlexibleSpaceBar(
        titlePadding: EdgeInsets.only(
          bottom: bottom != null ? 16.0 : 14.0,
          left: isMainView ? 20.0 : 55.0,
        ),
        title: title,
      ),
      leading: isMainView
          ? null
          : IconButton(
              icon: Icon(
                Icons.arrow_back,
                color: Theme.of(context).textTheme.titleLarge!.color,
              ),
              onPressed:
                  onBackButtonPressed ?? () => Navigator.of(context).pop(),
            ),
      backgroundColor: WidgetStateColor.resolveWith(
        (states) => states.contains(WidgetState.scrolledUnder)
            ? Theme.of(context).colorScheme.surface
            : Theme.of(context).canvasColor,
      ),
      actions: actions,
      bottom: bottom,
    );
  }
}
