import 'package:installed_apps/app_info.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patch_item.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherAPI patcherAPI = locator<PatcherAPI>();
  List<Patch>? patches = [];
  List<Patch> selectedPatches = [];

  Future<void> initialise() async {
    await getPatches();
    notifyListeners();
  }

  Future<void> getPatches() async {
    AppInfo? appInfo = locator<AppSelectorViewModel>().selectedApp;
    patches = await patcherAPI.getFilteredPatches(appInfo);
  }

  void selectPatches(List<PatchItem> patchItems) {
    selectedPatches.clear();
    if (patches != null) {
      for (PatchItem patch in patchItems) {
        if (patch.isSelected) {
          selectedPatches.add(
            patches!.firstWhere((element) => element.name == patch.name),
          );
        }
      }
    }
    locator<PatcherViewModel>().hideFabButton =
        selectedPatches.isEmpty ? true : false;
    locator<PatcherViewModel>().notifyListeners();
  }
}
