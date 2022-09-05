import 'package:flutter/material.dart';

class CustomSliverAppBar extends StatelessWidget {
  final Widget title;
  final PreferredSizeWidget? bottom;

  const CustomSliverAppBar({
    Key? key,
    required this.title,
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
        titlePadding: const EdgeInsets.symmetric(
          vertical: 23.0,
          horizontal: 20.0,
        ),
        title: title,
      ),
      bottom: bottom,
    );
  }
}
