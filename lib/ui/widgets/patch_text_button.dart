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
      style: TextButton.styleFrom(
        side: BorderSide(
          color: borderColor,
          width: 1,
        ),
        primary: Colors.white,
        backgroundColor: backgroundColor,
        padding: const EdgeInsets.symmetric(
          vertical: 10,
          horizontal: 24,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
      ),
      child: Text(
        text,
        style: interTextStyle,
      ),
    );
  }
}
