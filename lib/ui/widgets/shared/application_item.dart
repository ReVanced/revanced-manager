import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/widgets/shared/patch_text_button.dart';
import 'package:expandable/expandable.dart';
import 'package:timeago/timeago.dart';

class ApplicationItem extends StatelessWidget {
  final Uint8List icon;
  final String name;
  final DateTime patchDate;
  final List<String> changelog;
  final bool isUpdatableApp;
  final Function() onPressed;

  const ApplicationItem({
    Key? key,
    required this.icon,
    required this.name,
    required this.patchDate,
    required this.changelog,
    required this.isUpdatableApp,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ExpandablePanel(
      theme: const ExpandableThemeData(
        hasIcon: false,
        animationDuration: Duration(milliseconds: 450),
      ),
      header: Container(
        height: 60,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: Theme.of(context).colorScheme.primary,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 12.0),
        child: Row(
          children: <Widget>[
            SizedBox(
              width: 60,
              child: Image.memory(
                icon,
                height: 39,
                width: 39,
              ),
            ),
            const SizedBox(width: 4),
            SizedBox(
              width: MediaQuery.of(context).size.width - 250,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    name,
                    style: GoogleFonts.roboto(
                      color: Theme.of(context).colorScheme.secondary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  Text(
                    format(patchDate, locale: 'en_short'),
                    style: kRobotoTextStyle.copyWith(
                      color: Theme.of(context).colorScheme.tertiary,
                    ),
                  ),
                ],
              ),
            ),
            const Spacer(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8.0),
              child: PatchTextButton(
                text: isUpdatableApp
                    ? 'applicationItem.patchButton'
                    : 'applicationItem.openButton',
                onPressed: onPressed,
                borderColor: isDark
                    ? const Color(0xff4D5054)
                    : const Color.fromRGBO(119, 146, 168, 1),
              ),
            ),
          ],
        ),
      ),
      collapsed: const Text(''),
      expanded: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            I18nText(
              'applicationItem.changelogLabel',
              child: Text(
                '',
                style: kRobotoTextStyle.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '\u2022 ${changelog.join('\n\u2022 ')}',
              style: kRobotoTextStyle,
            ),
          ],
        ),
      ),
    );
  }
}
