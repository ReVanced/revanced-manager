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
    notifyListeners();
  }

  bool isSelected(int index) {
    return selectedPatches.any(
      (element) => element.name == patches[index].name,
    );
  }

  void selectPatch(int index, bool isSelected) {
    Patch patch = patches.firstWhere((p) => p.name == patches[index].name);
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
}
