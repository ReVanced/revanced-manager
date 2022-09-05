import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
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
      header: CustomCard(
        child: Row(
          children: <Widget>[
            SizedBox(
              width: 60,
              child: Image.memory(icon, height: 39, width: 39),
            ),
            const SizedBox(width: 4),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  name,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Text(format(patchDate, locale: 'en_short')),
              ],
            ),
            const Spacer(),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: <Widget>[
                CustomMaterialButton(
                  label: isUpdatableApp
                      ? I18nText('applicationItem.patchButton')
                      : I18nText('applicationItem.infoButton'),
                  onPressed: onPressed,
                ),
              ],
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
              child: const Text(
                '',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
            const SizedBox(height: 4),
            Text('\u2022 ${changelog.join('\n\u2022 ')}'),
          ],
        ),
      ),
    );
  }
}
