import 'package:collection/collection.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final List<Patch> patches = [];
  final List<Patch> selectedPatches =
      locator<PatcherViewModel>().selectedPatches;
  String? patchesVersion = '';
  bool isDefaultPatchesRepo() {
    return _managerAPI.getPatchesRepo() == 'revanced/revanced-patches';
  }

  Future<void> initialize() async {
    getPatchesVersion().whenComplete(() => notifyListeners());
    patches.addAll(
      _patcherAPI.getFilteredPatches(
        locator<PatcherViewModel>().selectedApp!.originalPackageName,
      ),
    );
    patches.sort((a, b) {
      if (a.compatiblePackages.isEmpty == b.compatiblePackages.isEmpty) {
        return a.name.compareTo(b.name);
      } else {
        return a.compatiblePackages.isEmpty ? 1 : -1;
      }
    });
    notifyListeners();
  }

  bool isSelected(Patch patch) {
    return selectedPatches.any(
      (element) => element.name == patch.name,
    );
  }

  void selectPatch(Patch patch, bool isSelected) {
    if (isSelected && !selectedPatches.contains(patch)) {
      selectedPatches.add(patch);
    } else {
      selectedPatches.remove(patch);
    }
    notifyListeners();
  }

  void selectDefaultPatches() {
    selectedPatches.clear();

    if (_managerAPI.areExperimentalPatchesEnabled() == false) {
      selectedPatches.addAll(
        patches.where(
          (element) => element.excluded == false && isPatchSupported(element),
        ),
      );
    }

    if (_managerAPI.areExperimentalPatchesEnabled()) {
      selectedPatches
          .addAll(patches.where((element) => element.excluded == false));
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

  String getAppVersion() {
    return locator<PatcherViewModel>().selectedApp!.version;
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

  void onMenuSelection(value) {
    switch (value) {
      case 0:
        loadSelectedPatches();
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

  Future<void> loadSelectedPatches() async {
    final List<String> selectedPatches = await _managerAPI.getSelectedPatches(
      locator<PatcherViewModel>().selectedApp!.originalPackageName,
    );
    if (selectedPatches.isNotEmpty) {
      this.selectedPatches.clear();
      this.selectedPatches.addAll(
            patches.where((patch) => selectedPatches.contains(patch.name)),
          );
    } else {
      locator<Toast>().showBottom('patchesSelectorView.noSavedPatches');
    }
    notifyListeners();
  }
}
