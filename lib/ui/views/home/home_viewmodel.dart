import 'dart:convert';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class HomeViewModel extends BaseViewModel {
  final _navigationService = locator<NavigationService>();
  final GithubAPI githubAPI = GithubAPI();
  final PatcherAPI patcherAPI = locator<PatcherAPI>();
  bool showUpdatableApps = true;

  Future<void> initialize() async {
    await patcherAPI.loadPatches();
  }

  void toggleUpdatableApps(bool value) {
    showUpdatableApps = value;
    notifyListeners();
  }

  Future downloadPatches() => locator<ManagerAPI>().downloadPatches();
  Future downloadIntegrations() => locator<ManagerAPI>().downloadIntegrations();

  void navigateToInstaller(PatchedApplication app) async {
    locator<AppSelectorViewModel>().selectedApp = app;
    locator<PatchesSelectorViewModel>().selectedPatches =
        await patcherAPI.getAppliedPatches(app);
    _navigationService.navigateTo(Routes.installerView);
  }

  Future<List<PatchedApplication>> getPatchedApps(bool isUpdatable) async {
    List<PatchedApplication> list = [];
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String> patchedApps = prefs.getStringList('patchedApps') ?? [];
    for (String str in patchedApps) {
      PatchedApplication app = PatchedApplication.fromJson(json.decode(str));
      bool hasUpdates = await githubAPI.hasUpdates(
        app,
        'revanced',
        'revanced-patches',
      );
      if (hasUpdates == isUpdatable) {
        list.add(app);
      }
    }
    return list;
  }
}
