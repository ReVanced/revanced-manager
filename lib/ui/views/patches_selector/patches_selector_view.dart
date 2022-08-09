import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patch_item.dart';
import 'package:revanced_manager/ui/widgets/search_bar.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorView extends StatefulWidget {
  const PatchesSelectorView({Key? key}) : super(key: key);

  @override
  State<PatchesSelectorView> createState() => _PatchesSelectorViewState();
}

class _PatchesSelectorViewState extends State<PatchesSelectorView> {
  String query = '';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatchesSelectorViewModel>.reactive(
      disposeViewModel: false,
      onModelReady: (model) => model.initialise(),
      viewModelBuilder: () => locator<PatchesSelectorViewModel>(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: FloatingActionButton.extended(
          onPressed: () => {},
          label: I18nText('patchesSelectorView.fabButton'),
          icon: const Icon(Icons.check),
          backgroundColor: const Color(0xff7792BA),
          foregroundColor: Colors.white,
        ),
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.patches != null && model.patches!.isNotEmpty
                ? Column(
                    children: [
                      SearchBar(
                        hintText: FlutterI18n.translate(
                          context,
                          'patchesSelectorView.searchBarHint',
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
    return Expanded(
      child: ListView.builder(
        itemCount: model.patches!.length,
        itemBuilder: (context, index) {
          model.patches!.sort((a, b) => a.simpleName.compareTo(b.simpleName));
          return PatchItem(
            name: model.patches![index].simpleName,
            version: model.patches![index].version,
            description: model.patches![index].description,
            isSelected: false,
          );
        },
      ),
    );
  }

  Widget _getFilteredResults(PatchesSelectorViewModel model) {
    return Expanded(
      child: ListView.builder(
        itemCount: model.patches!.length,
        itemBuilder: (context, index) {
          model.patches!.sort((a, b) => a.simpleName.compareTo(b.simpleName));
          if (model.patches![index].simpleName.toLowerCase().contains(
                query.toLowerCase(),
              )) {
            return PatchItem(
              name: model.patches![index].simpleName,
              version: model.patches![index].version,
              description: model.patches![index].description,
              isSelected: false,
            );
          } else {
            return const SizedBox();
          }
        },
      ),
    );
  }
}
