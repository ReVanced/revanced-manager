import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/widgets/patch_text_button.dart';

class ApplicationItem extends StatelessWidget {
  final String asset;
  final String name;
  final String releaseDate;
  final Function()? onPressed;

  const ApplicationItem({
    Key? key,
    required this.asset,
    required this.name,
    required this.releaseDate,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final isSVG = asset.endsWith(".svg");
    return ListTile(
      horizontalTitleGap: 12.0,
      leading: isSVG
          ? SvgPicture.asset(
              asset,
              height: 26,
              width: 26,
            )
          : Image.asset(
              asset,
              height: 39,
              width: 39,
            ),
      title: Text(
        name,
        style: GoogleFonts.roboto(
          color: const Color(0xff7792BA),
        ),
      ),
      subtitle: Text(
        releaseDate,
        style: robotoTextStyle,
      ),
      trailing: PatchTextButton(
        text: "Patch",
        onPressed: onPressed,
      ),
    );
  }
}
