import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/app_skeleton_loader.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/not_installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
import 'package:stacked/stacked.dart' hide SkeletonLoader;

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
      onViewModelReady: (model) => model.initialize(),
      viewModelBuilder: () => AppSelectorViewModel(),
      builder: (context, model, child) => Scaffold(
        resizeToAvoidBottomInset: false,
        floatingActionButton: FloatingActionButton.extended(
          label: I18nText('appSelectorView.storageButton'),
          icon: const Icon(Icons.sd_storage),
          onPressed: () {
            model.selectAppFromStorage(context);
            Navigator.of(context).pop();
          },
        ),
        body: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              floating: true,
              title: I18nText(
                'appSelectorView.viewTitle',
                child: Text(
                  '',
                  style: TextStyle(
                    color: Theme.of(context).textTheme.titleLarge!.color,
                  ),
                ),
              ),
              leading: IconButton(
                icon: Icon(
                  Icons.arrow_back,
                  color: Theme.of(context).textTheme.titleLarge!.color,
                ),
                onPressed: () => Navigator.of(context).pop(),
              ),
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(64.0),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    vertical: 8.0,
                    horizontal: 12.0,
                  ),
                  child: SearchBar(
                    hintText: FlutterI18n.translate(
                      context,
                      'appSelectorView.searchBarHint',
                    ),
                    onQueryChanged: (searchQuery) {
                      setState(() {
                        _query = searchQuery;
                      });
                    },
                  ),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: model.noApps
                  ? Center(
                      child: I18nText(
                        'appSelectorView.noAppsLabel',
                        child: Text(
                          '',
                          style: TextStyle(
                            color:
                                Theme.of(context).textTheme.titleLarge!.color,
                          ),
                        ),
                      ),
                    )
                  : model.apps.isEmpty
                      ? const AppSkeletonLoader()
                      : Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12.0)
                              .copyWith(bottom: 80),
                          child: Column(
                            children: [
                              ...model
                                  .getFilteredApps(_query)
                                  .map(
                                    (app) => InstalledAppItem(
                                      name: app.appName,
                                      pkgName: app.packageName,
                                      icon: app.icon,
                                      patchesCount:
                                          model.patchesCount(app.packageName),
                                      suggestedVersion:
                                          model.getSuggestedVersion(
                                        app.packageName,
                                      ),
                                      onTap: () {
                                        model.isRooted
                                            ? model.selectApp(app).then(
                                                  (_) => Navigator.of(context)
                                                      .pop(),
                                                )
                                            : model.showSelectFromStorageDialog(
                                                context,
                                              );
                                      },
                                    ),
                                  )
                                  .toList(),
                              ...model
                                  .getFilteredAppsNames(_query)
                                  .map(
                                    (app) => NotInstalledAppItem(
                                      name: app,
                                      patchesCount: model.patchesCount(app),
                                      suggestedVersion:
                                          model.getSuggestedVersion(app),
                                      onTap: () {
                                        model.showDownloadToast();
                                      },
                                    ),
                                  )
                                  .toList(),
                            ],
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }
}
