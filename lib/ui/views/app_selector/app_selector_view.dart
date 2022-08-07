import 'package:flutter/material.dart';
import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:revanced_manager/ui/widgets/installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/search_bar.dart';
import 'package:stacked/stacked.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';

class AppSelectorView extends StatefulWidget {
  const AppSelectorView({Key? key}) : super(key: key);

  @override
  State<AppSelectorView> createState() => _AppSelectorViewState();
}

class _AppSelectorViewState extends State<AppSelectorView> {
  List<AppInfo> apps = [];
  String query = '';

  void getApps() async {
    apps = await InstalledApps.getInstalledApps(false, true);
    setState(() {});
  }

  @override
  void initState() {
    getApps();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder.reactive(
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: Column(
              children: [
                SearchBar(
                  onQueryChanged: (searchQuery) {
                    setState(() {
                      query = searchQuery;
                    });
                  },
                ),
                if (query.isEmpty || query.length < 2)
                  apps.isEmpty
                      ? const Center(
                          child: CircularProgressIndicator(),
                        )
                      : Expanded(
                          child: ListView.builder(
                            itemCount: apps.length,
                            itemBuilder: (context, index) {
                              //sort alphabetically
                              apps.sort((a, b) => a.name!.compareTo(b.name!));
                              return InstalledAppItem(
                                name: apps[index].name!,
                                pkgName: apps[index].packageName!,
                                icon: apps[index].icon!,
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
                              apps.sort((a, b) => a.name!.compareTo(b.name!));
                              if (apps[index].name!.toLowerCase().contains(
                                    query.toLowerCase(),
                                  )) {
                                return InstalledAppItem(
                                  name: apps[index].name!,
                                  pkgName: apps[index].packageName!,
                                  icon: apps[index].icon!,
                                );
                              } else {
                                return const SizedBox();
                              }
                            },
                          ),
                        ),
              ],
            ),
          ),
        ),
      ),
      viewModelBuilder: () => AppSelectorViewModel(),
    );
  }
}
