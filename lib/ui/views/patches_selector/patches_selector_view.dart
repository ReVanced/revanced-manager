import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_item.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_options_fields.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorView extends StatefulWidget {
  const PatchesSelectorView({Key? key}) : super(key: key);

  @override
  State<PatchesSelectorView> createState() => _PatchesSelectorViewState();
}

class _PatchesSelectorViewState extends State<PatchesSelectorView> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    ExpandableController expController = ExpandableController();
    return ViewModelBuilder<PatchesSelectorViewModel>.reactive(
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => PatchesSelectorViewModel(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: Visibility(
          visible: model.patches.isNotEmpty,
          child: FloatingActionButton.extended(
            label: I18nText('patchesSelectorView.doneButton'),
            icon: const Icon(Icons.check),
            onPressed: () {
              model.selectPatches();
              Navigator.of(context).pop();
            },
          ),
        ),
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 4.0, horizontal: 12.0),
            child: model.patches.isEmpty
                ? Center(
                    child: CircularProgressIndicator(
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  )
                : Column(
                    children: <Widget>[
                      SearchBar(
                        showSelectIcon: true,
                        hintText: FlutterI18n.translate(
                          context,
                          'patchesSelectorView.searchBarHint',
                        ),
                        onQueryChanged: (searchQuery) {
                          setState(() {
                            _query = searchQuery;
                          });
                        },
                        onSelectAll: (value) => model.selectAllPatches(value),
                      ),
                      const SizedBox(height: 12),
                      // TODO: IMPROVE THIS BAD CODE
                      Expanded(
                        child: ListView(
                          padding: const EdgeInsets.only(bottom: 80),
                          children: model
                              .getQueriedPatches(_query)
                              .map((patch) => patch.name
                                      .contains("custom-branding")
                                  ? ExpandablePanel(
                                      controller: expController,
                                      theme: const ExpandableThemeData(
                                        hasIcon: false,
                                        tapBodyToExpand: true,
                                        tapBodyToCollapse: true,
                                        tapHeaderToExpand: true,
                                      ),
                                      header: Column(
                                        children: [
                                          GestureDetector(
                                            onLongPress: () =>
                                                expController.toggle(),
                                            child: PatchItem(
                                              name: patch.name,
                                              simpleName: patch.getSimpleName(),
                                              description: patch.description,
                                              version: patch.version,
                                              packageVersion:
                                                  model.getAppVersion(),
                                              supportedPackageVersions: model
                                                  .getSupportedVersions(patch),
                                              isUnsupported: !model
                                                  .isPatchSupported(patch),
                                              isSelected:
                                                  model.isSelected(patch),
                                              onChanged: (value) => model
                                                  .selectPatch(patch, value),
                                              child: const Padding(
                                                padding: EdgeInsets.symmetric(
                                                  vertical: 8.0,
                                                ),
                                                child: Text(
                                                    'Long press for additional options.'),
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                      expanded: Padding(
                                        padding: const EdgeInsets.symmetric(
                                          vertical: 10.0,
                                          horizontal: 10,
                                        ),
                                        child: Container(
                                          padding: const EdgeInsets.symmetric(
                                            vertical: 8,
                                            horizontal: 8,
                                          ),
                                          decoration: BoxDecoration(
                                            color: Theme.of(context)
                                                .colorScheme
                                                .tertiary
                                                .withOpacity(0.1),
                                            borderRadius:
                                                BorderRadius.circular(12),
                                          ),
                                          child: Column(
                                            children: [
                                              Text(
                                                "Patch options",
                                                style: GoogleFonts.inter(
                                                  fontSize: 18,
                                                  fontWeight: FontWeight.w600,
                                                ),
                                              ),
                                              const OptionsTextField(
                                                  hint: "App name"),
                                              const OptionsFilePicker(
                                                optionName: "Choose a logo",
                                              ),
                                            ],
                                          ),
                                        ),
                                      ),
                                      collapsed: Container(),
                                    )
                                  : PatchItem(
                                      name: patch.name,
                                      simpleName: patch.getSimpleName(),
                                      version: patch.version,
                                      description: patch.description,
                                      packageVersion: model.getAppVersion(),
                                      supportedPackageVersions:
                                          model.getSupportedVersions(patch),
                                      isUnsupported:
                                          !model.isPatchSupported(patch),
                                      isSelected: model.isSelected(patch),
                                      onChanged: (value) =>
                                          model.selectPatch(patch, value),
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
