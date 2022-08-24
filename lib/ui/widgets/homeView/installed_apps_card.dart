import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/application_item.dart';

class InstalledAppsCard extends StatelessWidget {
  InstalledAppsCard({
    Key? key,
  }) : super(key: key);

  final ManagerAPI _managerAPI = ManagerAPI();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: [
        FutureBuilder<List<PatchedApplication>>(
          future: locator<HomeViewModel>().getPatchedApps(false),
          builder: (context, snapshot) =>
              snapshot.hasData && snapshot.data!.isNotEmpty
                  ? ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      padding: EdgeInsets.zero,
                      itemCount: snapshot.data!.length,
                      itemBuilder: (context, index) => FutureBuilder<String>(
                        future: _managerAPI.getAppChangelog(
                          snapshot.data![index].packageName,
                        ),
                        initialData: '',
                        builder: (context, snapshot2) => ApplicationItem(
                          icon: snapshot.data![index].icon,
                          name: snapshot.data![index].name,
                          patchDate: snapshot.data![index].patchDate,
                          changelog: snapshot2.data!,
                          isUpdatableApp: false,
                          onPressed: () => DeviceApps.openApp(
                            snapshot.data![index].packageName,
                          ),
                        ),
                      ),
                    )
                  : Container(),
        ),
      ],
    );
  }
}
