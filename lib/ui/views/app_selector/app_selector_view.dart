import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
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
  String query = '';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<AppSelectorViewModel>.reactive(
      disposeViewModel: false,
      onModelReady: (model) => model.initialise(),
      viewModelBuilder: () => locator<AppSelectorViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.apps.isNotEmpty
                ? Column(
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
                      const SizedBox(height: 12),
                      query.isEmpty || query.length < 2
                          ? _getAllResults(model)
                          : _getFilteredResults(model)
                    ],
                  )
                : query.isEmpty || query.length < 2
                    ? const Center(
                        child: CircularProgressIndicator(
                          color: Color(0xff7792BA),
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
          model.apps.sort((a, b) => a.name!.compareTo(b.name!));
          return InkWell(
            onTap: () {
              model.selectApp(model.apps[index]);
              Navigator.of(context).pop();
            },
            child: InstalledAppItem(
              name: model.apps[index].name!,
              pkgName: model.apps[index].packageName!,
              icon: model.apps[index].icon!,
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
          model.apps.sort((a, b) => a.name!.compareTo(b.name!));
          if (model.apps[index].name!.toLowerCase().contains(
                query.toLowerCase(),
              )) {
            return InkWell(
              onTap: () {
                model.selectApp(model.apps[index]);
                Navigator.of(context).pop();
              },
              child: InstalledAppItem(
                name: model.apps[index].name!,
                pkgName: model.apps[index].packageName!,
                icon: model.apps[index].icon!,
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
