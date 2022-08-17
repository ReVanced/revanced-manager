import 'dart:convert';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class HomeViewModel extends BaseViewModel {
  final GithubAPI githubAPI = GithubAPI();
  bool showUpdatableApps = true;

  void toggleUpdatableApps(bool value) {
    showUpdatableApps = value;
    notifyListeners();
  }

  Future downloadPatches() => locator<ManagerAPI>().downloadPatches();
  Future downloadIntegrations() => locator<ManagerAPI>().downloadIntegrations();

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
