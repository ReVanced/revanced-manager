import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/theme.dart';
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
  String _query = '';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<AppSelectorViewModel>.reactive(
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => AppSelectorViewModel(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: FloatingActionButton.extended(
          onPressed: () {
            model.selectAppFromStorage(context);
            Navigator.of(context).pop();
          },
          label: I18nText('appSelectorView.fabButton'),
          icon: const Icon(Icons.sd_storage),
          backgroundColor: Theme.of(context).colorScheme.secondary,
          foregroundColor: Colors.white,
        ),
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.apps.isNotEmpty
                ? Column(
                    children: [
                      SearchBar(
                        showSelectIcon: false,
                        fillColor:
                            isDark ? const Color(0xff1B222B) : Colors.grey[200],
                        hintText: FlutterI18n.translate(
                          context,
                          'appSelectorView.searchBarHint',
                        ),
                        hintTextColor: Theme.of(context).colorScheme.tertiary,
                        onQueryChanged: (searchQuery) {
                          setState(() {
                            _query = searchQuery;
                          });
                        },
                      ),
                      const SizedBox(height: 12),
                      _query.isEmpty || _query.length < 2
                          ? _getAllResults(model)
                          : _getFilteredResults(model)
                    ],
                  )
                : _query.isEmpty || _query.length < 2
                    ? Center(
                        child: CircularProgressIndicator(
                          color: Theme.of(context).colorScheme.secondary,
                        ),
                      )
                    : Center(
                        child: I18nText('appSelectorCard.noAppsLabel'),
                      ),
          ),
        ),
      ),
    );
  }

  Widget _getAllResults(AppSelectorViewModel model) {
    return Expanded(
      child: ListView.builder(
        itemCount: model.apps.length,
        itemBuilder: (context, index) {
          model.apps.sort((a, b) => a.appName.compareTo(b.appName));
          return InkWell(
            onTap: () {
              model.selectApp(model.apps[index]);
              Navigator.of(context).pop();
            },
            child: InstalledAppItem(
              name: model.apps[index].appName,
              pkgName: model.apps[index].packageName,
              icon: model.apps[index].icon,
            ),
          );
        },
      ),
    );
  }

  Widget _getFilteredResults(AppSelectorViewModel model) {
    return Expanded(
      child: ListView.builder(
        itemCount: model.apps.length,
        itemBuilder: (context, index) {
          model.apps.sort((a, b) => a.appName.compareTo(b.appName));
          if (model.apps[index].appName.toLowerCase().contains(
                _query.toLowerCase(),
              )) {
            return InkWell(
              onTap: () {
                model.selectApp(model.apps[index]);
                Navigator.of(context).pop();
              },
              child: InstalledAppItem(
                name: model.apps[index].appName,
                pkgName: model.apps[index].packageName,
                icon: model.apps[index].icon,
              ),
            );
          } else {
            return const SizedBox();
          }
        },
      ),
    );
  }
}
