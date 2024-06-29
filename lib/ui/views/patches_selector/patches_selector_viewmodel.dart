import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_item.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final NavigationService _navigationService = locator<NavigationService>();
  final List<Patch> patches = [];
  final List<Patch> currentSelection = [];
  final List<Patch> selectedPatches =
      locator<PatcherViewModel>().selectedPatches;
  PatchedApplication? selectedApp = locator<PatcherViewModel>().selectedApp;
  String? patchesVersion = '';

  bool isDefaultPatchesRepo() {
    return _managerAPI.getPatchesRepo() == 'revanced/revanced-patches';
  }

  Future<void> initialize() async {
    getPatchesVersion().whenComplete(() => notifyListeners());
    patches.addAll(
      _patcherAPI.getFilteredPatches(
        selectedApp!.packageName,
      ),
    );
    final List<Option> requiredNullOptions =
        getNullRequiredOptions(patches, selectedApp!.packageName);
    patches.sort((a, b) {
      if (b.options.any((option) => requiredNullOptions.contains(option)) &&
          a.options.isEmpty) {
        return 1;
      } else {
        return a.name.compareTo(b.name);
      }
    });
    currentSelection.clear();
    currentSelection.addAll(selectedPatches);

    notifyListeners();
  }

  bool isSelected(Patch patch) {
    return selectedPatches.any(
      (element) => element.name == patch.name,
    );
  }

  void navigateToPatchOptions(List<Option> setOptions, Patch patch) {
    _managerAPI.options = setOptions;
    _managerAPI.selectedPatch = patch;
    _navigationService.navigateToPatchOptionsView();
  }

  bool areRequiredOptionsNull(BuildContext context) {
    final List<String> patchesWithNullRequiredOptions = [];
    final List<Option> requiredNullOptions =
        getNullRequiredOptions(selectedPatches, selectedApp!.packageName);
    if (requiredNullOptions.isNotEmpty) {
      for (final patch in selectedPatches) {
        for (final patchOption in patch.options) {
          if (requiredNullOptions.contains(patchOption)) {
            patchesWithNullRequiredOptions.add(patch.name);
            break;
          }
        }
      }
      showSetRequiredOption(context, patchesWithNullRequiredOptions);
      return true;
    }
    return false;
  }

  Future<void> showSetRequiredOption(
    BuildContext context,
    List<String> patches,
  ) async {
    return showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.notice),
        content: Text(
          t.patchesSelectorView.setRequiredOption(
            patches: patches.map((patch) => '• $patch').join('\n'),
          ),
        ),
        actions: <Widget>[
          FilledButton(
            onPressed: () => {
              Navigator.of(context).pop(),
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  void selectPatch(Patch patch, bool isSelected, BuildContext context) {
    if (_managerAPI.isPatchesChangeEnabled()) {
      if (isSelected && !selectedPatches.contains(patch)) {
        selectedPatches.add(patch);
      } else {
        selectedPatches.remove(patch);
      }
      notifyListeners();
    } else {
      showPatchesChangeDialog(context);
    }
  }

  Future<void> showPatchesChangeDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.warning),
        content: Text(
          t.patchItem.patchesChangeWarningDialogText,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w500,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.okButton),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(context)
                ..pop()
                ..pop();
            },
            child: Text(t.patchItem.patchesChangeWarningDialogButton),
          ),
        ],
      ),
    );
  }

  void selectDefaultPatches() {
    selectedPatches.clear();
    if (locator<PatcherViewModel>().selectedApp?.packageName != null) {
      selectedPatches.addAll(
        _patcherAPI
            .getFilteredPatches(
              locator<PatcherViewModel>().selectedApp!.packageName,
            )
            .where(
              (element) =>
                  !element.excluded &&
                  (!_managerAPI.isVersionCompatibilityCheckEnabled() ||
                      isPatchSupported(element)),
            ),
      );
    }
    notifyListeners();
  }

  void clearPatches() {
    selectedPatches.clear();
    notifyListeners();
  }

  void selectPatches() {
    locator<PatcherViewModel>().selectedPatches = selectedPatches;
    saveSelectedPatches();
    locator<PatcherViewModel>().notifyListeners();
  }

  void resetSelection() {
    selectedPatches.clear();
    selectedPatches.addAll(currentSelection);
    notifyListeners();
  }

  Future<void> getPatchesVersion() async {
    patchesVersion = await _managerAPI.getCurrentPatchesVersion();
  }

  List<Patch> getQueriedPatches(String query) {
    final List<Patch> patch = patches
        .where(
          (patch) =>
              query.isEmpty ||
              query.length < 2 ||
              patch.name.toLowerCase().contains(query.toLowerCase()) ||
              patch.name
                  .replaceAll(RegExp(r'[^\w\s]+'), '')
                  .toLowerCase()
                  .contains(query.toLowerCase()),
        )
        .toList();
    if (_managerAPI.areUniversalPatchesEnabled()) {
      return patch;
    } else {
      return patch
          .where((patch) => patch.compatiblePackages.isNotEmpty)
          .toList();
    }
  }

  Widget getPatchItem(BuildContext context, Patch patch) {
    return PatchItem(
      name: patch.name,
      simpleName: patch.getSimpleName(),
      description: patch.description ?? '',
      packageVersion: getAppInfo().version,
      supportedPackageVersions: getSupportedVersions(patch),
      isUnsupported: !isPatchSupported(patch),
      isChangeEnabled: _managerAPI.isPatchesChangeEnabled(),
      hasUnsupportedPatchOption: hasUnsupportedRequiredOption(
        patch.options,
        patch,
      ),
      options: patch.options,
      isSelected: isSelected(patch),
      navigateToOptions: (options) => navigateToPatchOptions(
        options,
        patch,
      ),
      onChanged: (value) => selectPatch(
        patch,
        value,
        context,
      ),
    );
  }

  Widget getPatchCategory(BuildContext context, String category) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        vertical: 10.0,
      ),
      child: Container(
        padding: const EdgeInsets.only(
          top: 10.0,
          bottom: 10.0,
          left: 5.0,
        ),
        child: Text(
          category,
          style: TextStyle(
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
      ),
    );
  }

  PatchedApplication getAppInfo() {
    return locator<PatcherViewModel>().selectedApp!;
  }

  bool isPatchNew(Patch patch) {
    return locator<PatcherViewModel>().isPatchNew(patch);
  }

  List<String> getSupportedVersions(Patch patch) {
    final PatchedApplication app = locator<PatcherViewModel>().selectedApp!;
    final Package? package = patch.compatiblePackages.firstWhereOrNull(
      (pack) => pack.name == app.packageName,
    );
    if (package != null) {
      return package.versions;
    } else {
      return List.empty();
    }
  }

  void onMenuSelection(value, BuildContext context) {
    switch (value) {
      case 0:
        loadSelectedPatches(context);
        break;
    }
  }

  Future<void> saveSelectedPatches() async {
    final List<String> selectedPatches =
        this.selectedPatches.map((patch) => patch.name).toList();
    await _managerAPI.setSelectedPatches(
      locator<PatcherViewModel>().selectedApp!.packageName,
      selectedPatches,
    );
  }

  Future<void> loadSelectedPatches(BuildContext context) async {
    if (_managerAPI.isPatchesChangeEnabled()) {
      final List<String>? appliedPatches = _managerAPI
          .getPatchedApps()
          .firstWhereOrNull(
            (app) => app.packageName == selectedApp!.packageName,
          )
          ?.appliedPatches;
      final List<String> selectedPatches = appliedPatches ??
          await _managerAPI.getSelectedPatches(
            selectedApp!.packageName,
          );
      if (selectedPatches.isNotEmpty) {
        this.selectedPatches.clear();
        this.selectedPatches.addAll(
              patches.where((patch) => selectedPatches.contains(patch.name)),
            );
        if (_managerAPI.isVersionCompatibilityCheckEnabled()) {
          this.selectedPatches.removeWhere((patch) => !isPatchSupported(patch));
        }
      } else {
        locator<Toast>().showBottom(t.patchesSelectorView.noSavedPatches);
      }
      notifyListeners();
    } else {
      showPatchesChangeDialog(context);
    }
  }
}
