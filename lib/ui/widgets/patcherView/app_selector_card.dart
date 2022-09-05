import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class AppSelectorCard extends StatelessWidget {
  final Function() onPressed;

  const AppSelectorCard({
    Key? key,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: CustomCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            I18nText(
              locator<PatcherViewModel>().selectedApp == null
                  ? 'appSelectorCard.widgetTitle'
                  : 'appSelectorCard.widgetTitleSelected',
              child: const Text(
                '',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 10),
            locator<PatcherViewModel>().selectedApp == null
                ? I18nText('appSelectorCard.widgetSubtitle')
                : Row(
                    children: <Widget>[
                      SizedBox(
                        height: 16.0,
                        child: ClipOval(
                          child: Image.memory(
                            locator<PatcherViewModel>().selectedApp == null
                                ? Uint8List(0)
                                : locator<PatcherViewModel>().selectedApp!.icon,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text(_getAppSelection()),
                    ],
                  ),
          ],
        ),
      ),
    );
  }

  String _getAppSelection() {
    String name = locator<PatcherViewModel>().selectedApp!.name;
    String version = locator<PatcherViewModel>().selectedApp!.version;
    return '$name (v$version)';
  }
}
