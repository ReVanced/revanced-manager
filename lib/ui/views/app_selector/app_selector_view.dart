import 'package:flutter/material.dart' hide SearchBar;
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/app_skeleton_loader.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/not_installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_floating_action_button_extended.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
import 'package:stacked/stacked.dart' hide SkeletonLoader;

class AppSelectorView extends StatefulWidget {
  const AppSelectorView({super.key});

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
        floatingActionButton: HapticFloatingActionButtonExtended(
          label: Text(t.appSelectorView.storageButton),
          icon: const Icon(Icons.sd_storage),
          onPressed: () {
            model.selectAppFromStorage(context);
          },
        ),
        body: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              floating: true,
              title: Text(
                t.appSelectorView.viewTitle,
              ),
              titleTextStyle: TextStyle(
                fontSize: 22.0,
                color: Theme.of(context).textTheme.titleLarge!.color,
              ),
              leading: IconButton(
                icon: Icon(
                  Icons.arrow_back,
                  color: Theme.of(context).textTheme.titleLarge!.color,
                ),
                onPressed: () => Navigator.of(context).pop(),
              ),
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(66.0),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    vertical: 8.0,
                    horizontal: 12.0,
                  ),
                  child: SearchBar(
                    hintText: t.appSelectorView.searchBarHint,
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
                      child: Text(
                        t.appSelectorCard.noAppsLabel,
                        style: TextStyle(
                          color: Theme.of(context).textTheme.titleLarge!.color,
                        ),
                      ),
                    )
                  : model.allApps.isEmpty && model.apps.isEmpty
                      ? const AppSkeletonLoader()
                      : Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12.0)
                              .copyWith(
                            bottom:
                                MediaQuery.viewPaddingOf(context).bottom + 8.0,
                          ),
                          child: Column(
                            children: [
                              ...model.getFilteredApps(_query).map(
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
                                      installedVersion: app.versionName!,
                                      onTap: () => model.canSelectInstalled(
                                        context,
                                        app.packageName,
                                      ),
                                      onLinkTap: () =>
                                          model.searchSuggestedVersionOnWeb(
                                        packageName: app.packageName,
                                      ),
                                    ),
                                  ),
                              ...model.getFilteredAppsNames(_query).map(
                                    (app) => NotInstalledAppItem(
                                      name: app,
                                      patchesCount: model.patchesCount(app),
                                      suggestedVersion:
                                          model.getSuggestedVersion(app),
                                      onTap: () {
                                        model.showDownloadToast();
                                      },
                                      onLinkTap: () =>
                                          model.searchSuggestedVersionOnWeb(
                                        packageName: app,
                                      ),
                                    ),
                                  ),
                              const SizedBox(height: 70.0),
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
