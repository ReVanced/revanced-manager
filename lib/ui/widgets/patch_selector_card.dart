import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';

class PatchSelectorCard extends StatelessWidget {
  final Function()? onPressed;
  const PatchSelectorCard({
    Key? key,
    this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: Container(
        width: double.infinity,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: const Color(0xff1B222B),
        ),
        padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            I18nText(
              'patchSelectorCard.widgetTitle',
              child: Text(
                '',
                style: GoogleFonts.roboto(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 10),
            locator<AppSelectorViewModel>().selectedApp == null
                ? I18nText(
                    'patchSelectorCard.widgetFirstSubtitle',
                    child: Text(
                      '',
                      style: robotoTextStyle,
                    ),
                  )
                : locator<PatchesSelectorViewModel>().selectedPatches.isEmpty
                    ? I18nText(
                        'patchSelectorCard.widgetSecondSubtitle',
                        child: Text(
                          '',
                          style: robotoTextStyle,
                        ),
                      )
                    : I18nText(
                        'patchSelectorCard.widgetThirdSubtitle',
                        translationParams: {
                          'selected': locator<PatchesSelectorViewModel>()
                              .selectedPatches
                              .length
                              .toString()
                        },
                        child: Text(
                          '',
                          style: robotoTextStyle,
                        ),
                      ),
          ],
        ),
      ),
    );
  }
}
