import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patch_item.dart';
import 'package:revanced_manager/ui/widgets/search_bar.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_themes/stacked_themes.dart';

class PatchesSelectorView extends StatefulWidget {
  const PatchesSelectorView({Key? key}) : super(key: key);

  @override
  State<PatchesSelectorView> createState() => _PatchesSelectorViewState();
}

class _PatchesSelectorViewState extends State<PatchesSelectorView> {
  final List<PatchItem> patches = [];
  String query = '';

  @override
  Widget build(BuildContext context) {
    bool isDark = getThemeManager(context).isDarkMode;
    return ViewModelBuilder<PatchesSelectorViewModel>.reactive(
      disposeViewModel: false,
      onModelReady: (model) => model.initialise(),
      viewModelBuilder: () => locator<PatchesSelectorViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.patches != null && model.patches!.isNotEmpty
                ? Column(
                    children: [
                      SearchBar(
                        fillColor:
                            isDark ? Colors.blueGrey[700] : Colors.grey[400],
                        hintText: FlutterI18n.translate(
                          context,
                          'patchesSelectorView.searchBarHint',
                        ),
                        hintTextColor: isDark ? Colors.white : Colors.grey[800],
                        onQueryChanged: (searchQuery) {
                          setState(() {
                            query = searchQuery;
                          });
                        },
                      ),
                      const SizedBox(height: 12),
                      query.isEmpty || query.length < 2
                          ? _getAllResults(model)
                          : _getFilteredResults(model),
                      MaterialButton(
                        textColor: Colors.white,
                        color: const Color(0x957792BA),
                        minWidth: double.infinity,
                        padding: const EdgeInsets.symmetric(
                          vertical: 12,
                          horizontal: 8,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        onPressed: () {
                          model.selectPatches(patches);
                          Navigator.of(context).pop();
                        },
                        child: I18nText('patchesSelectorView.fabButton'),
                      ),
                    ],
                  )
                : const Center(
                    child: CircularProgressIndicator(
                      color: Color(0xff7792BA),
                    ),
                  ),
          ),
        ),
      ),
    );
  }

  Widget _getAllResults(PatchesSelectorViewModel model) {
    patches.clear();
    return Expanded(
      child: ListView.builder(
        itemCount: model.patches!.length,
        itemBuilder: (context, index) {
          model.patches!.sort((a, b) => a.simpleName.compareTo(b.simpleName));
          PatchItem item = PatchItem(
            name: model.patches![index].name,
            simpleName: model.patches![index].simpleName,
            version: model.patches![index].version,
            description: model.patches![index].description,
            isSelected: model.selectedPatches.any(
              (element) => element.name == model.patches![index].name,
            ),
          );
          patches.add(item);
          return item;
        },
      ),
    );
  }

  Widget _getFilteredResults(PatchesSelectorViewModel model) {
    patches.clear();
    return Expanded(
      child: ListView.builder(
        itemCount: model.patches!.length,
        itemBuilder: (context, index) {
          model.patches!.sort((a, b) => a.simpleName.compareTo(b.simpleName));
          if (model.patches![index].simpleName.toLowerCase().contains(
                query.toLowerCase(),
              )) {
            PatchItem item = PatchItem(
              name: model.patches![index].name,
              simpleName: model.patches![index].simpleName,
              version: model.patches![index].version,
              description: model.patches![index].description,
              isSelected: model.selectedPatches.any(
                (element) => element.name == model.patches![index].name,
              ),
            );
            patches.add(item);
            return item;
          } else {
            return const SizedBox();
          }
        },
      ),
    );
  }
}
