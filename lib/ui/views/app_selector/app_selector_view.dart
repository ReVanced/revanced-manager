import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
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
          label: I18nText('appSelectorView.storageButton'),
          icon: const Icon(Icons.sd_storage),
          onPressed: () {
            model.selectAppFromStorage(context);
            Navigator.of(context).pop();
          },
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          backgroundColor: Theme.of(context).colorScheme.secondary,
          foregroundColor: Theme.of(context).colorScheme.surface,
        ),
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.noApps
                ? Center(
                    child: I18nText('appSelectorCard.noAppsLabel'),
                  )
                : model.apps.isEmpty
                    ? Center(
                        child: CircularProgressIndicator(
                          color: Theme.of(context).colorScheme.secondary,
                        ),
                      )
                    : Column(
                        children: [
                          SearchBar(
                            showSelectIcon: false,
                            fillColor: isDark
                                ? const Color(0xff1B222B)
                                : Colors.grey[200],
                            hintText: FlutterI18n.translate(
                              context,
                              'appSelectorView.searchBarHint',
                            ),
                            hintTextColor:
                                Theme.of(context).colorScheme.tertiary,
                            onQueryChanged: (searchQuery) {
                              setState(() {
                                _query = searchQuery;
                              });
                            },
                          ),
                          const SizedBox(height: 12),
                          Expanded(
                            child: ListView(
                              padding: const EdgeInsets.only(bottom: 80),
                              children: model
                                  .getFilteredApps(_query)
                                  .map((app) => InkWell(
                                        onTap: () {
                                          model.selectApp(app);
                                          Navigator.of(context).pop();
                                        },
                                        child: InstalledAppItem(
                                          name: app.appName,
                                          pkgName: app.packageName,
                                          icon: app.icon,
                                        ),
                                      ))
                                  .toList(),
                            ),
                          ),
                        ],
                      ),
          ),
        ),
      ),
    );
  }
}
