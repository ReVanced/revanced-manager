import 'package:flutter/material.dart';
import 'package:revanced_manager/theme.dart';

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
            ? isDark
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).navigationBarTheme.backgroundColor!
            : Theme.of(context).scaffoldBackgroundColor,
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
