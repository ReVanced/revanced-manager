import 'package:flutter/material.dart' hide SearchBar;
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_popup_menu.dart';
import 'package:revanced_manager/ui/widgets/shared/search_bar.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorView extends StatefulWidget {
  const PatchesSelectorView({Key? key}) : super(key: key);

  @override
  State<PatchesSelectorView> createState() => _PatchesSelectorViewState();
}

class _PatchesSelectorViewState extends State<PatchesSelectorView> {
  String _query = '';
  final _managerAPI = locator<ManagerAPI>();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!_managerAPI.isPatchesChangeEnabled() &&
          _managerAPI.showPatchesChangeWarning()) {
        _managerAPI.showPatchesChangeWarningDialog(context);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatchesSelectorViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(),
      viewModelBuilder: () => PatchesSelectorViewModel(),
      builder: (context, model, child) => Scaffold(
        resizeToAvoidBottomInset: false,
        floatingActionButton: Visibility(
          visible: model.patches.isNotEmpty,
          child: FloatingActionButton.extended(
            label: Row(
              children: <Widget>[
                I18nText('patchesSelectorView.doneButton'),
                Text(' (${model.selectedPatches.length})'),
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
              title: I18nText(
                'patchesSelectorView.viewTitle',
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
              actions: [
                FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Container(
                    margin: const EdgeInsets.only(top: 12, bottom: 12),
                    padding:
                    const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
                    decoration: BoxDecoration(
                      color: Theme.of(context)
                          .colorScheme
                          .tertiary
                          .withOpacity(0.5),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      model.patchesVersion!,
                      style: TextStyle(
                        color: Theme.of(context).textTheme.titleLarge!.color,
                      ),
                    ),
                  ),
                ),
                CustomPopupMenu(
                  onSelected: (value) =>
                  {model.onMenuSelection(value, context)},
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
                preferredSize: const Size.fromHeight(66.0),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    vertical: 8.0,
                    horizontal: 12.0,
                  ),
                  child: SearchBar(
                    hintText: FlutterI18n.translate(
                      context,
                      'patchesSelectorView.searchBarHint',
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
                      padding:
                          const EdgeInsets.symmetric(horizontal: 12.0).copyWith(
                        bottom: MediaQuery.viewPaddingOf(context).bottom + 8.0,
                      ),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              ActionChip(
                                label: I18nText('patchesSelectorView.default'),
                                tooltip: FlutterI18n.translate(
                                  context,
                                  'patchesSelectorView.defaultTooltip',
                                ),
                                onPressed: () {
                                  if (_managerAPI.isPatchesChangeEnabled()) {
                                    model.selectDefaultPatches();
                                  } else {
                                    model.showPatchesChangeDialog(context);
                                  }
                                },
                              ),
                              const SizedBox(width: 8),
                              ActionChip(
                                label: I18nText('patchesSelectorView.none'),
                                tooltip: FlutterI18n.translate(
                                  context,
                                  'patchesSelectorView.noneTooltip',
                                ),
                                onPressed: () {
                                  if (_managerAPI.isPatchesChangeEnabled()) {
                                    model.clearPatches();
                                  } else {
                                    model.showPatchesChangeDialog(context);
                                  }
                                },
                              ),
                            ],
                          ),
                          ...model.getQueriedPatches(_query).map(
                            (patch) {
                              if (patch.compatiblePackages.isNotEmpty) {
                                return PatchItem(
                                  name: patch.name,
                                  simpleName: patch.getSimpleName(),
                                  description: patch.description,
                                  packageVersion: model.getAppInfo().version,
                                  supportedPackageVersions:
                                      model.getSupportedVersions(patch),
                                  isUnsupported: !isPatchSupported(patch),
                                  isChangeEnabled:
                                      _managerAPI.isPatchesChangeEnabled(),
                                  isNew: model.isPatchNew(
                                    patch,
                                    model.getAppInfo().packageName,
                                  ),
                                  isSelected: model.isSelected(patch),
                                  onChanged: (value) =>
                                      model.selectPatch(patch, value, context),
                                );
                              } else {
                                return Container();
                              }
                            },
                          ),
                          if (_managerAPI.areUniversalPatchesEnabled())
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Padding(
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 10.0,
                                  ),
                                  child: Container(
                                    padding: const EdgeInsets.only(
                                      top: 10.0,
                                      bottom: 10.0,
                                      left: 5.0,
                                    ),
                                    child: I18nText(
                                      'patchesSelectorView.universalPatches',
                                      child: Text(
                                        '',
                                        style: TextStyle(
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                                ...model.getQueriedPatches(_query).map((patch) {
                                  if (patch.compatiblePackages.isEmpty) {
                                    return PatchItem(
                                      name: patch.name,
                                      simpleName: patch.getSimpleName(),
                                      description: patch.description,
                                      packageVersion:
                                          model.getAppInfo().version,
                                      supportedPackageVersions:
                                          model.getSupportedVersions(patch),
                                      isUnsupported: !isPatchSupported(patch),
                                      isChangeEnabled:
                                          _managerAPI.isPatchesChangeEnabled(),
                                      isNew: false,
                                      isSelected: model.isSelected(patch),
                                      onChanged: (value) => model.selectPatch(
                                        patch,
                                        value,
                                        context,
                                      ),
                                    );
                                  } else {
                                    return Container();
                                  }
                                }),
                              ],
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
