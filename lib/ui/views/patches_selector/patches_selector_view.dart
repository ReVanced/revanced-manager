import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_chip.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_popup_menu.dart';
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
    return ViewModelBuilder<PatchesSelectorViewModel>.reactive(
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => PatchesSelectorViewModel(),
      builder: (context, model, child) => Scaffold(
        resizeToAvoidBottomInset: false,
        floatingActionButton: Visibility(
          visible: model.patches.isNotEmpty,
          child: FloatingActionButton.extended(
            label: Row(
              children: <Widget>[
                I18nText('patchesSelectorView.doneButton'),
                Text(' (${model.selectedPatches.length})')
              ],
            ),
            icon: const Icon(Icons.check),
            onPressed: () {
              model.selectPatches();
              Navigator.of(context).pop();
            },
          ),
        ),
        body: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              floating: true,
              snap: false,
              title: I18nText(
                'patchesSelectorView.viewTitle',
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
              actions: [
                Container(
                  height: 2,
                  margin: const EdgeInsets.only(top: 12, bottom: 12),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
                  decoration: BoxDecoration(
                    color:
                        Theme.of(context).colorScheme.tertiary.withOpacity(0.5),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    model.patchesVersion!,
                    style: TextStyle(
                      color: Theme.of(context).textTheme.headline6!.color,
                    ),
                  ),
                ),
                CustomPopupMenu(
                  onSelected: (value) => {model.onMenuSelection(value)},
                  children: {
                    0: I18nText(
                      'patchesSelectorView.loadPatchesSelection',
                      child: const Text(
                        '',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  },
                ),
              ],
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(64.0),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    vertical: 8.0,
                    horizontal: 12.0,
                  ),
                  child: SearchBar(
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
                    onSelectAll: (value) {
                      if (value) {
                        model.selectAllPatcherWarning(context);
                      }
                      model.selectAllPatches(value);
                    },
                  ),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: model.patches.isEmpty
                  ? Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Center(
                        child: I18nText(
                          'patchesSelectorView.noPatchesFound',
                          child: Text(
                            '',
                            style: Theme.of(context).textTheme.bodyMedium,
                          ),
                        ),
                      ),
                    )
                  : Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 12.0)
                          .copyWith(bottom: 80),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              CustomChip(
                                label:
                                    I18nText('patchesSelectorView.recommended'),
                                onSelected: (value) {
                                  model.selectRecommendedPatches();
                                },
                              ),
                              const SizedBox(width: 8),
                              CustomChip(
                                label: I18nText('patchesSelectorView.all'),
                                onSelected: (value) {
                                  if (value) {
                                    model.selectAllPatcherWarning(context);
                                  }
                                  model.selectAllPatches(true);
                                },
                              ),
                              const SizedBox(width: 8),
                              CustomChip(
                                label: I18nText('patchesSelectorView.none'),
                                onSelected: (value) {
                                  model.clearPatches();
                                },
                              ),
                            ],
                          ),
                          ...model
                              .getQueriedPatches(_query)
                              .map(
                                (patch) => PatchItem(
                                  name: patch.name,
                                  simpleName: patch.getSimpleName(),
                                  version: patch.version,
                                  description: patch.description,
                                  packageVersion: model.getAppVersion(),
                                  supportedPackageVersions:
                                      model.getSupportedVersions(patch),
                                  isUnsupported: !model.isPatchSupported(patch),
                                  isSelected: model.isSelected(patch),
                                  onChanged: (value) =>
                                      model.selectPatch(patch, value),
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
