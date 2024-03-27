import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/application_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

//ignore: must_be_immutable
class InstalledAppsCard extends StatelessWidget {
  InstalledAppsCard({super.key});

  List<PatchedApplication> apps = locator<HomeViewModel>().patchedInstalledApps;
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  List<PatchedApplication> patchedApps = [];

  Future _getApps() async {
    if (apps.isNotEmpty) {
      patchedApps = [...apps];
      for (final element in apps) {
        await DeviceApps.getApp(element.packageName).then((value) {
          if (element.version != value?.versionName) {
            patchedApps.remove(element);
          }
        });
      }
      if (apps.length != patchedApps.length) {
        await _managerAPI.setPatchedApps(patchedApps);
        apps.clear();
        apps = [...patchedApps];
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: _getApps(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.done) {
          return apps.isEmpty
              ? CustomCard(
                  child: Center(
                    child: Column(
                      children: <Widget>[
                        Icon(
                          size: 40,
                          Icons.file_download_off,
                          color: Theme.of(context).colorScheme.secondary,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          t.homeView.noInstallations,
                          textAlign: TextAlign.center,
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium!
                              .copyWith(
                                color: Theme.of(context).colorScheme.secondary,
                              ),
                        ),
                      ],
                    ),
                  ),
                )
              : ListView(
                  shrinkWrap: true,
                  padding: EdgeInsets.zero,
                  physics: const NeverScrollableScrollPhysics(),
                  children: apps
                      .map(
                        (app) => ApplicationItem(
                          icon: app.icon,
                          name: app.name,
                          patchDate: app.patchDate,
                          onPressed: () =>
                              locator<HomeViewModel>().navigateToAppInfo(app, false),
                        ),
                      )
                      .toList(),
                );
        } else {
          return const Center(child: CircularProgressIndicator());
        }
      },
    );
  }
}
