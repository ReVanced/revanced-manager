import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorViewModel extends BaseViewModel {
  PatcherService patcherService = PatcherService();
  List<Patch>? patches = [];
  AppInfo? appInfo;

  Future<void> getApp() async {
    AppInfo app = await InstalledApps.getAppInfo("com.google.android.youtube");
    appInfo = app;
  }
  Future<List<Patch>?>? getPatches() async {
    getApp();
    PatcherService patcherService = PatcherService();
    patcherService.loadPatches();
    return patcherService.getFilteredPatches(appInfo);
  }

}