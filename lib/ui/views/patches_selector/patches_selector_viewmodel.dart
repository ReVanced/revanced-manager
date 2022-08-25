import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
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
      locator<PatcherViewModel>().selectedApp,
    ));
    patches.sort((a, b) => a.simpleName.compareTo(b.simpleName));
    for (Patch p in patches) {
      if (p.include) {
        selectedPatches.add(p);
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
            patch.simpleName.toLowerCase().contains(
                  query.toLowerCase(),
                ))
        .toList();
  }
}
