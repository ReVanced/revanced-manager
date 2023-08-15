import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final List<Patch> patches = [];
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
        selectedApp!.originalPackageName,
      ),
    );
    patches.sort((a, b) {
      if (isPatchNew(a, selectedApp!.packageName) ==
          isPatchNew(b, selectedApp!.packageName)) {
        return a.name.compareTo(b.name);
      } else {
        return isPatchNew(b, selectedApp!.packageName) ? 1 : -1;
      }
    });
    notifyListeners();
  }

  bool isSelected(Patch patch) {
    return selectedPatches.any(
      (element) => element.name == patch.name,
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
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        title: I18nText('warning'),
        content: I18nText(
          'patchItem.patchesChangeWarningDialogText',
          child: const Text(
            '',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        actions: [
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('okButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('patchItem.patchesChangeWarningDialogButton'),
            onPressed: () {
              Navigator.of(context)
                ..pop()
                ..pop();
            },
          ),
        ],
      ),
    );
  }

  void selectDefaultPatches() {
    selectedPatches.clear();
    if (locator<PatcherViewModel>().selectedApp?.originalPackageName != null) {
      selectedPatches.addAll(
        _patcherAPI
            .getFilteredPatches(
              locator<PatcherViewModel>().selectedApp!.originalPackageName,
            )
            .where(
              (element) =>
                  !element.excluded &&
                  (_managerAPI.areExperimentalPatchesEnabled() ||
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
              patch.getSimpleName().toLowerCase().contains(query.toLowerCase()),
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

  PatchedApplication getAppInfo() {
    return locator<PatcherViewModel>().selectedApp!;
  }

  bool isPatchNew(Patch patch, String packageName) {
    final List<Patch> savedPatches = _managerAPI.getSavedPatches(packageName);
    if (savedPatches.isEmpty) {
      return false;
    } else {
      return !savedPatches
          .any((p) => p.getSimpleName() == patch.getSimpleName());
    }
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
      locator<PatcherViewModel>().selectedApp!.originalPackageName,
      selectedPatches,
    );
  }

  Future<void> loadSelectedPatches(BuildContext context) async {
    if (_managerAPI.isPatchesChangeEnabled()) {
      final List<String> selectedPatches = await _managerAPI.getSelectedPatches(
        locator<PatcherViewModel>().selectedApp!.originalPackageName,
      );
      if (selectedPatches.isNotEmpty) {
        this.selectedPatches.clear();
        this.selectedPatches.addAll(
              patches.where((patch) => selectedPatches.contains(patch.name)),
            );
        if (!_managerAPI.areExperimentalPatchesEnabled()) {
          this.selectedPatches.removeWhere((patch) => !isPatchSupported(patch));
        }
      } else {
        locator<Toast>().showBottom('patchesSelectorView.noSavedPatches');
      }
      notifyListeners();
    } else {
      showPatchesChangeDialog(context);
    }
  }
}
