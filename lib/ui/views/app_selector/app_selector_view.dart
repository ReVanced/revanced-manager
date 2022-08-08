import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
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
  final PatcherService patcherService = locator<PatcherService>();
  String query = '';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<AppSelectorViewModel>.reactive(
      onModelReady: (model) => model.initialise(),
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: Column(
              children: [
                SearchBar(
                  hintText: FlutterI18n.translate(
                    context,
                    'appSelectorView.searchBarHint',
                  ),
                  onQueryChanged: (searchQuery) {
                    setState(() {
                      query = searchQuery;
                    });
                  },
                ),
                if (query.isEmpty || query.length < 2)
                  model.apps.isEmpty
                      ? const Center(
                          child: CircularProgressIndicator(),
                        )
                      : Expanded(
                          child: ListView.builder(
                            itemCount: model.apps.length,
                            itemBuilder: (context, index) {
                              //sort alphabetically
                              model.apps
                                  .sort((a, b) => a.name!.compareTo(b.name!));
                              return InkWell(
                                onTap: () {
                                  patcherService.setSelectedApp(
                                      model.apps[index].packageName!);
                                  Navigator.of(context).pop();
                                  locator<PatcherViewModel>().notifyListeners();
                                },
                                child: InstalledAppItem(
                                  name: model.apps[index].name!,
                                  pkgName: model.apps[index].packageName!,
                                  icon: model.apps[index].icon!,
                                ),
                              );
                            },
                          ),
                        ),
                if (query.isNotEmpty)
                  model.apps.isEmpty
                      ? Center(
                          child: I18nText('appSelectorCard.noAppsLabel'),
                        )
                      : Expanded(
                          child: ListView.builder(
                            itemCount: model.apps.length,
                            itemBuilder: (context, index) {
                              model.apps
                                  .sort((a, b) => a.name!.compareTo(b.name!));
                              if (model.apps[index].name!
                                  .toLowerCase()
                                  .contains(
                                    query.toLowerCase(),
                                  )) {
                                return InstalledAppItem(
                                  name: model.apps[index].name!,
                                  pkgName: model.apps[index].packageName!,
                                  icon: model.apps[index].icon!,
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
