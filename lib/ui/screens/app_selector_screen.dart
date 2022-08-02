import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager_flutter/ui/widgets/installed_app_item.dart';
import 'package:revanced_manager_flutter/ui/widgets/search_bar.dart';

class AppSelectorScreen extends StatefulWidget {
  const AppSelectorScreen({Key? key}) : super(key: key);

  @override
  State<AppSelectorScreen> createState() => _AppSelectorScreenState();
}

class _AppSelectorScreenState extends State<AppSelectorScreen> {
  List<Application> apps = [];
  String query = 'yout';

  void getApps() async {
    apps = await DeviceApps.getInstalledApplications();
    setState(() {});
  }

  @override
  void initState() {
    getApps();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 4.0, horizontal: 8.0),
          child: Column(
            children: [
              SearchBar(),
              if (query.isEmpty || query.length < 2)
                apps.isEmpty
                    ? const Center(
                        child: CircularProgressIndicator(),
                      )
                    : Expanded(
                        child: ListView.builder(
                          itemCount: apps.length,
                          itemBuilder: (context, index) {
                            return InstalledAppItem(
                              name: apps[index].appName,
                              pkgName: apps[index].packageName,
                              isSelected: false,
                            );
                          },
                        ),
                      ),
              if (query.isNotEmpty)
                apps.isEmpty
                    ? const Center(
                        child: Text('No apps found'),
                      )
                    : Expanded(
                        child: ListView.builder(
                          itemCount: apps.length,
                          itemBuilder: (context, index) {
                            if (apps[index].appName.toLowerCase().contains(
                                  query.toLowerCase(),
                                )) {
                              return InstalledAppItem(
                                name: apps[index].appName,
                                pkgName: apps[index].packageName,
                                isSelected: false,
                              );
                            } else {
                              return SizedBox();
                            }
                          },
                        ),
                      ),
            ],
          ),
        ),
      ),
    );
  }
}
