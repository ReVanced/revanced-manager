import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/installed_app_item.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
import 'package:revanced_manager/ui/widgets/appSelectorView/app_skeleton_loader.dart';
import 'package:stacked/stacked.dart' hide SkeletonLoader;
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
              snap: false,
              title: I18nText(
                'appSelectorView.viewTitle',
                child: Text(
                  '',
                  style: TextStyle(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
              leading: IconButton(
                icon: Icon(
                  Icons.arrow_back,
                  color: Theme.of(context).textTheme.headline6!.color,
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
                    showSelectIcon: false,
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
                      child: I18nText('appSelectorCard.noAppsLabel'),
                    )
                  : model.apps.isEmpty
                      ? const AppSkeletonLoader()
                      : Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12.0)
                              .copyWith(bottom: 80),
                          child: Column(
                            children: model
                                .getFilteredApps(_query)
                                .map((app) => InstalledAppItem(
                                      name: app.appName,
                                      pkgName: app.packageName,
                                      icon: app.icon,
                                      onTap: () {
                                        model.selectApp(app);
                                        Navigator.of(context).pop();
                                      },
                                    ))
                                .toList(),
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }
}
