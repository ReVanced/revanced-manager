import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/application_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class AvailableUpdatesCard extends StatelessWidget {
  AvailableUpdatesCard({Key? key}) : super(key: key);

  final List<PatchedApplication> apps =
      locator<HomeViewModel>().patchedUpdatableApps;

  @override
  Widget build(BuildContext context) {
    return apps.isEmpty
        ? CustomCard(
            child: Center(
              child: Column(
                children: <Widget>[
                  const Icon(Icons.update_disabled, size: 40),
                  const SizedBox(height: 16),
                  I18nText(
                    'homeView.noUpdates',
                    child: Text(
                      '',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.subtitle1!,
                    ),
                  )
                ],
              ),
            ),
          )
        : ListView(
            shrinkWrap: true,
            padding: EdgeInsets.zero,
            physics: const NeverScrollableScrollPhysics(),
            children: apps
                .map((app) => ApplicationItem(
                      icon: app.icon,
                      name: app.name,
                      patchDate: app.patchDate,
                      changelog: app.changelog,
                      isUpdatableApp: true,
                      onPressed: () =>
                          locator<HomeViewModel>().navigateToPatcher(
                        app,
                      ),
                    ))
                .toList(),
          );
  }
}
