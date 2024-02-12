import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class PatchSelectorCard extends StatelessWidget {
  const PatchSelectorCard({
    super.key,
    required this.onPressed,
  });
  final Function() onPressed;

  @override
  Widget build(BuildContext context) {
    return CustomCard(
      onTap: onPressed,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                locator<PatcherViewModel>().selectedPatches.isEmpty
                    ? t.patchSelectorCard.widgetTitle
                    : t.patchSelectorCard.widgetTitleSelected,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Text(
                locator<PatcherViewModel>().selectedPatches.isEmpty
                    ? ''
                    : ' (${locator<PatcherViewModel>().selectedPatches.length})',
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          if (locator<PatcherViewModel>().selectedApp == null)
            Text(t.patchSelectorCard.widgetSubtitle)
          else
            locator<PatcherViewModel>().selectedPatches.isEmpty
                ? Text(t.patchSelectorCard.widgetEmptySubtitle)
                : Text(_getPatchesSelection()),
        ],
      ),
    );
  }

  String _getPatchesSelection() {
    String text = '';
    final List<Patch> selectedPatches =
        locator<PatcherViewModel>().selectedPatches;
    selectedPatches.sort((a, b) => a.name.compareTo(b.name));
    for (final Patch p in selectedPatches) {
      text += '•  ${p.getSimpleName()}\n';
    }
    return text.substring(0, text.length - 1);
  }
}
