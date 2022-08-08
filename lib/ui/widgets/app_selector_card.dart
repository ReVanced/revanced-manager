import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/patcher_api.dart';

class AppSelectorCard extends StatelessWidget {
  final Function()? onPressed;
  AppSelectorCard({
    Key? key,
    this.onPressed,
  }) : super(key: key);

  final PatcherService patcherService = locator<PatcherService>();

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
              'appSelectorCard.widgetTitle',
              child: Text(
                '',
                style: GoogleFonts.roboto(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 10),
            patcherService.getSelectedApp().isNotEmpty
                ? Text(
                    patcherService.getSelectedApp(),
                    style: robotoTextStyle,
                  )
                : I18nText(
                    'appSelectorCard.widgetSubtitle',
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
