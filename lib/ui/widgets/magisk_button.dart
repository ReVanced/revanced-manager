import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';

class MagiskButton extends StatelessWidget {
  final Function()? onPressed;
  const MagiskButton({
    Key? key,
    this.onPressed,
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
            backgroundColor: isDark
                ? Theme.of(context).colorScheme.secondary
                : const Color(0xffCBDFFC),
            child: SvgPicture.asset(
              'assets/images/magisk.svg',
              color: isDark ? Colors.white70 : Colors.grey[900],
              height: 50,
              width: 50,
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
