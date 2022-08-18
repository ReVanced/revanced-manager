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

  void selectPatch(String name, bool isSelected) {
    Patch patch = patches.firstWhere((p) => p.name == name);
    if (isSelected && !selectedPatches.contains(patch)) {
      selectedPatches.add(patch);
    } else {
      selectedPatches.remove(patch);
    }
    notifyListeners();
  }

  void selectPatches() {
    locator<PatcherViewModel>().selectedPatches = selectedPatches;
    locator<PatcherViewModel>().notifyListeners();
  }
}
