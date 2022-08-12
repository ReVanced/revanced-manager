import 'package:flutter/material.dart';
import 'package:revanced_manager/constants.dart';

class PatchTextButton extends StatelessWidget {
  final String text;
  final Function()? onPressed;
  final Color borderColor;
  final Color backgroundColor;
  const PatchTextButton({
    Key? key,
    required this.text,
    this.onPressed,
    this.borderColor = const Color(0xff7792BA),
    this.backgroundColor = Colors.transparent,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      style: Theme.of(context).textButtonTheme.style?.copyWith(
            backgroundColor: MaterialStateProperty.all<Color?>(backgroundColor),
            side: MaterialStateProperty.all<BorderSide>(
              BorderSide(
                color: borderColor,
                width: 1,
              ),
            ),
          ),
      child: Text(
        text,
        style: interTextStyle.copyWith(
            color: backgroundColor == Colors.transparent
                ? const Color.fromRGBO(119, 146, 186, 1)
                : Colors.white),
      ),
    );
  }
}
