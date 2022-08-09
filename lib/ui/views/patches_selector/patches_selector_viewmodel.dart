import 'package:installed_apps/app_info.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  final PatcherService patcherService = locator<PatcherService>();
  List<Patch>? patches = [];
  List<Patch> selectedPatches = [];

  Future<void> initialise() async {
    await getPatches();
    notifyListeners();
  }

  Future<void> getPatches() async {
    AppInfo? appInfo = locator<AppSelectorViewModel>().selectedApp;
    patches = await patcherService.getFilteredPatches(appInfo);
  }

  void selectPatches(List<Patch> patches) {}
}
