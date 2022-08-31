import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final List<Patch> patches = [];
  final List<Patch> selectedPatches =
      locator<PatcherViewModel>().selectedPatches;

  Future<void> initialize() async {
    patches.addAll(await _patcherAPI.getFilteredPatches(
      locator<PatcherViewModel>().selectedApp!.packageName,
    ));
    patches.sort((a, b) => a.name.compareTo(b.name));
    if (selectedPatches.isEmpty) {
      for (Patch patch in patches) {
        if (!patch.excluded && isPatchSupported(patch)) {
          selectedPatches.add(patch);
        }
      }
    }
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

  void selectAllPatches(bool isSelected) {
    selectedPatches.clear();
    if (isSelected) {
      selectedPatches.addAll(patches);
    }
    notifyListeners();
  }

  void selectPatches() {
    locator<PatcherViewModel>().selectedPatches = selectedPatches;
    locator<PatcherViewModel>().notifyListeners();
  }

  List<Patch> getQueriedPatches(String query) {
    return patches
        .where((patch) =>
            query.isEmpty ||
            query.length < 2 ||
            patch.name.toLowerCase().contains(
                  query.toLowerCase(),
                ))
        .toList();
  }

  String getAppVersion() {
    return locator<PatcherViewModel>().selectedApp!.version;
  }

  List<String> getSupportedVersions(Patch patch) {
    PatchedApplication app = locator<PatcherViewModel>().selectedApp!;
    return patch.compatiblePackages
        .firstWhere((pack) => pack.name == app.packageName)
        .versions;
  }

  bool isPatchSupported(Patch patch) {
    PatchedApplication app = locator<PatcherViewModel>().selectedApp!;
    return patch.compatiblePackages.any((pack) =>
        pack.name == app.packageName &&
        (pack.versions.isEmpty || pack.versions.contains(app.version)));
  }
}
