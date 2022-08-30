import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/theme.dart';

class CustomMaterialButton extends StatelessWidget {
  final String text;
  final bool isFilled;
  final bool isExpanded;
  final Function()? onPressed;

  const CustomMaterialButton({
    Key? key,
    required this.text,
    this.isFilled = true,
    this.isExpanded = false,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: ButtonStyle(
        padding: MaterialStateProperty.all(
          isExpanded
              ? const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                )
              : const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 12,
                ),
        ),
        shape: MaterialStateProperty.all(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(100),
            side: BorderSide(
              width: 1,
              color: Theme.of(context).colorScheme.secondary,
            ),
          ),
        ),
        side: MaterialStateProperty.all(
          BorderSide(
            color: isFilled
                ? Colors.transparent
                : Theme.of(context).iconTheme.color!.withOpacity(0.4),
            width: 1,
          ),
        ),
        backgroundColor: MaterialStateProperty.all(
          isFilled
              ? Theme.of(context).colorScheme.secondary
              : isDark
                  ? Theme.of(context).colorScheme.background
                  : Colors.white,
        ),
        foregroundColor: MaterialStateProperty.all(
          isFilled
              ? Theme.of(context).colorScheme.background
              : Theme.of(context).colorScheme.secondary,
        ),
      ),
      onPressed: onPressed,
      child: I18nText(text),
    );
  }
}
