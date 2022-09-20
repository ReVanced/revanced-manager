import 'package:flutter/material.dart';

class CustomSliverAppBar extends StatelessWidget {
  final Widget title;
  final List<Widget>? actions;
  final PreferredSizeWidget? bottom;
  final bool isMainView;
  final Function()? onBackButtonPressed;

  const CustomSliverAppBar({
    Key? key,
    required this.title,
    this.actions,
    this.bottom,
    this.isMainView = false,
    this.onBackButtonPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SliverAppBar(
      pinned: true,
      snap: false,
      floating: false,
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
                color: Theme.of(context).textTheme.headline6!.color,
              ),
              onPressed:
                  onBackButtonPressed ?? () => Navigator.of(context).pop(),
            ),
      backgroundColor: MaterialStateColor.resolveWith(
        (states) => states.contains(MaterialState.scrolledUnder)
            ? Theme.of(context).colorScheme.surface
            : Theme.of(context).canvasColor,
      ),
      actions: actions,
      bottom: bottom,
    );
  }
}
