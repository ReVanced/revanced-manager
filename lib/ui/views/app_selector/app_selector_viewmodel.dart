import 'package:installed_apps/app_info.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI patcherAPI = locator<PatcherAPI>();
  List<AppInfo> apps = [];
  AppInfo? selectedApp;

  Future<void> initialise() async {
    await getApps();
    notifyListeners();
  }

  Future<void> getApps() async {
    await patcherAPI.loadPatches();
    apps = await patcherAPI.getFilteredInstalledApps();
  }

  void selectApp(AppInfo appInfo) {
    locator<AppSelectorViewModel>().selectedApp = appInfo;
    locator<PatcherViewModel>().notifyListeners();
  }
}
