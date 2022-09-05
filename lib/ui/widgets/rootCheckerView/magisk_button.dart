import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:flutter_svg/flutter_svg.dart';

class MagiskButton extends StatelessWidget {
  final Function() onPressed;

  const MagiskButton({
    Key? key,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        GestureDetector(
          onTap: onPressed,
          child: CircleAvatar(
            radius: 32,
            backgroundColor: Theme.of(context).colorScheme.primary,
            child: SvgPicture.asset(
              'assets/images/magisk.svg',
              color: Theme.of(context).colorScheme.surface,
              height: 40,
              width: 40,
            ),
          ),
        ),
        const SizedBox(height: 8),
        I18nText(
          'rootCheckerView.grantPermission',
          child: const Text(
            '',
            style: TextStyle(fontSize: 15),
          ),
        ),
      ],
    );
  }
}
