import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_fonts/google_fonts.dart';

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
      children: [
        GestureDetector(
          onTap: onPressed,
          child: CircleAvatar(
            radius: 32,
            backgroundColor: Theme.of(context).colorScheme.secondary,
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
          child: Text(
            '',
            style: GoogleFonts.poppins(
              fontSize: 15,
            ),
          ),
        ),
      ],
    );
  }
}
