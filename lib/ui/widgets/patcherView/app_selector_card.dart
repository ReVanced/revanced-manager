import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class AppSelectorCard extends StatelessWidget {
  const AppSelectorCard({
    super.key,
    required this.onPressed,
  });

  final Function() onPressed;

  @override
  Widget build(BuildContext context) {
    final vm = locator<PatcherViewModel>();


    String? suggestedVersion;
    if (vm.selectedApp != null) {
      suggestedVersion = vm.getSuggestedVersionString(context);
    }

    return CustomCard(
      onTap: onPressed,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            vm.selectedApp == null
                ? t.appSelectorCard.widgetTitle
                : t.appSelectorCard.widgetTitleSelected,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 8),
          if (vm.selectedApp == null)
            Text(t.appSelectorCard.widgetSubtitle)
          else
            Row(
              children: <Widget>[
                SizedBox(
                  height: 18.0,
                  child: ClipOval(
                    child: Image.memory(
                      vm.selectedApp == null
                          ? Uint8List(0)
                          : vm.selectedApp!.icon,
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    vm.getAppSelectionString(),
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
          if (vm.selectedApp == null)
            Container()
          else
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 4),
                Text(
                  vm.selectedApp!.packageName,
                ),
                if (suggestedVersion!.isNotEmpty &&
                    suggestedVersion != vm.selectedApp!.version) ...[
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Material(
                        color: Theme.of(context).colorScheme.secondaryContainer,
                        borderRadius:
                            const BorderRadius.all(Radius.circular(8)),
                        child: InkWell(
                          onTap: () {
                            vm.queryVersion(suggestedVersion!);
                          },
                          borderRadius:
                              const BorderRadius.all(Radius.circular(8)),
                          child: Container(
                            padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  t.suggested(
                                    version: suggestedVersion,
                                  ),
                                ),
                                const SizedBox(width: 4),
                                Icon(
                                  Icons.search,
                                  size: 16,
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onSecondaryContainer,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
        ],
      ),
    );
  }
}
