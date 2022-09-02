import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/application_item.dart';

class AvailableUpdatesCard extends StatelessWidget {
  AvailableUpdatesCard({Key? key}) : super(key: key);

  final List<PatchedApplication> apps =
      locator<HomeViewModel>().patchedUpdatableApps;

  @override
  Widget build(BuildContext context) {
    return apps.isEmpty
        ? Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              color: Theme.of(context).colorScheme.primary,
            ),
            padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
            child: Center(
              child: Column(
                children: <Widget>[
                  Icon(
                    Icons.update_disabled,
                    size: 40,
                    color: Theme.of(context).colorScheme.secondary,
                  ),
                  const SizedBox(height: 16),
                  I18nText(
                    'homeView.noUpdates',
                    child: Text(
                      '',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.subtitle1!.copyWith(
                          color: Theme.of(context).colorScheme.secondary),
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
