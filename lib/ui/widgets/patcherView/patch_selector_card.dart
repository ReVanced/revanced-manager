import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

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
      child: CustomCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            I18nText(
              locator<PatcherViewModel>().selectedPatches.isEmpty
                  ? 'patchSelectorCard.widgetTitle'
                  : 'patchSelectorCard.widgetTitleSelected',
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
                ? I18nText('patchSelectorCard.widgetSubtitle')
                : locator<PatcherViewModel>().selectedPatches.isEmpty
                    ? I18nText('patchSelectorCard.widgetEmptySubtitle')
                    : Text(_getPatchesSelection()),
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
