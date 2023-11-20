import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
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
    return CustomCard(
      onTap: onPressed,
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
          const SizedBox(height: 8),
          if (locator<PatcherViewModel>().selectedApp == null)
            I18nText('appSelectorCard.widgetSubtitle')
          else
            Row(
              children: <Widget>[
                SizedBox(
                  height: 18.0,
                  child: ClipOval(
                    child: Image.memory(
                      locator<PatcherViewModel>().selectedApp == null
                          ? Uint8List(0)
                          : locator<PatcherViewModel>().selectedApp!.icon,
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    locator<PatcherViewModel>().getAppSelectionString(),
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
              ],
            ),
          if (locator<PatcherViewModel>().selectedApp == null)
            Container()
          else
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 4),
                Text(
                  locator<PatcherViewModel>().getCurrentVersionString(context),
                ),
                Row(
                  children: [
                    GestureDetector(
                      onTap: () {
                        locator<PatcherViewModel>()
                            .searchSuggestedVersionOnWeb();
                      },
                      child: Container(
                        decoration: BoxDecoration(
                          color:
                              Theme.of(context).colorScheme.secondaryContainer,
                          borderRadius:
                              const BorderRadius.all(Radius.circular(7)),
                        ),
                        padding: const EdgeInsets.symmetric(horizontal: 3),
                        child: Wrap(
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            Text(
                              locator<PatcherViewModel>()
                                  .getSuggestedVersionString(context),
                              style: TextStyle(
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSecondaryContainer,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Icon(
                              Icons.launch,
                              size: 16,
                              color: Theme.of(context)
                                  .colorScheme
                                  .onSecondaryContainer,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
        ],
      ),
    );
  }
}
