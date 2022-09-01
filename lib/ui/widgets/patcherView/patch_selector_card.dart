import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';

class PatchSelectorCard extends StatelessWidget {
  final Function() onPressed;

  const PatchSelectorCard({
    Key? key,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: Container(
        width: double.infinity,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: Theme.of(context).colorScheme.primary,
        ),
        padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            I18nText(
              locator<PatcherViewModel>().selectedPatches.isEmpty
                  ? 'patchSelectorCard.widgetTitle'
                  : 'patchSelectorCard.widgetTitleSelected',
              child: Text(
                '',
                style: GoogleFonts.roboto(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 10),
            locator<PatcherViewModel>().selectedApp == null
                ? I18nText(
                    'patchSelectorCard.widgetSubtitle',
                    child: Text(
                      '',
                      style: kRobotoTextStyle,
                    ),
                  )
                : locator<PatcherViewModel>().selectedPatches.isEmpty
                    ? I18nText(
                        'patchSelectorCard.widgetEmptySubtitle',
                        child: Text(
                          '',
                          style: kRobotoTextStyle,
                        ),
                      )
                    : Text(
                        _getPatchesSelection(),
                        style: kRobotoTextStyle,
                      ),
          ],
        ),
      ),
    );
  }

  String _getPatchesSelection() {
    String text = '';
    for (Patch p in locator<PatcherViewModel>().selectedPatches) {
      text += '${p.getSimpleName()} (v${p.version})\n';
    }
    return text.substring(0, text.length - 1);
  }
}
