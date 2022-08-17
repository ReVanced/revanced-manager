import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';

class AppSelectorCard extends StatelessWidget {
  final Function()? onPressed;
  final Color? color;

  AppSelectorCard({
    Key? key,
    this.onPressed,
    this.color = const Color(0xff1B222B),
  }) : super(key: key);

  final PatcherAPI patcherAPI = locator<PatcherAPI>();

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: Container(
        width: double.infinity,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: color,
        ),
        padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 10),
            I18nText(
              locator<AppSelectorViewModel>().selectedApp == null
                  ? 'appSelectorCard.widgetTitle'
                  : 'appSelectorCard.widgetTitleSelected',
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
                    'appSelectorCard.widgetSubtitle',
                    child: Text(
                      '',
                      style: robotoTextStyle,
                    ),
                  )
                : Row(
                    children: [
                      SizedBox(
                        height: 16.0,
                        child: ClipOval(
                          child: Image.memory(
                            locator<AppSelectorViewModel>().selectedApp == null
                                ? Uint8List(0)
                                : locator<AppSelectorViewModel>()
                                    .selectedApp!
                                    .icon,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        _getAppSelection(),
                        style: robotoTextStyle,
                      ),
                    ],
                  ),
          ],
        ),
      ),
    );
  }

  String _getAppSelection() {
    String name = locator<AppSelectorViewModel>().selectedApp!.name;
    String version = locator<AppSelectorViewModel>().selectedApp!.version;
    return '$name (v$version)';
  }
}
