import 'package:flutter/material.dart';

class CustomSliverAppBar extends StatelessWidget {
  final Widget title;
  final List<Widget>? actions;
  final PreferredSizeWidget? bottom;

  const CustomSliverAppBar({
    Key? key,
    required this.title,
    this.actions,
    this.bottom,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SliverAppBar(
      pinned: true,
      snap: false,
      floating: false,
      expandedHeight: 100.0,
      automaticallyImplyLeading: false,
      backgroundColor: MaterialStateColor.resolveWith(
        (states) => states.contains(MaterialState.scrolledUnder)
            ? Theme.of(context).colorScheme.surface
            : Theme.of(context).canvasColor,
      ),
      flexibleSpace: FlexibleSpaceBar(
        titlePadding: const EdgeInsets.only(bottom: 16.0, left: 20.0),
        title: title,
      ),
      actions: actions,
      bottom: bottom,
    );
  }
}
