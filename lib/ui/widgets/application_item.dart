import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/widgets/patch_text_button.dart';
import 'package:timeago/timeago.dart';

class ApplicationItem extends StatelessWidget {
  final Uint8List icon;
  final String name;
  final DateTime patchDate;
  final Function()? onPressed;

  const ApplicationItem({
    Key? key,
    required this.icon,
    required this.name,
    required this.patchDate,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      horizontalTitleGap: 12.0,
      leading: Image.memory(icon),
      title: Text(
        name,
        style: GoogleFonts.roboto(
          color: Theme.of(context).colorScheme.secondary,
          fontWeight: FontWeight.w600,
        ),
      ),
      subtitle: Text(
        format(patchDate),
        style: robotoTextStyle,
      ),
      trailing: PatchTextButton(
        text: FlutterI18n.translate(
          context,
          'applicationItem.patchButton',
        ),
        onPressed: onPressed,
      ),
    );
  }
}
