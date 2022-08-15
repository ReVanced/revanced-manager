import 'dart:convert';

import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class HomeViewModel extends BaseViewModel {
  Future downloadPatches() => locator<ManagerAPI>().downloadPatches();
  Future downloadIntegrations() => locator<ManagerAPI>().downloadIntegrations();

  Future<List<PatchedApplication>> getPatchedApps() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String> patchedApps = prefs.getStringList('patchedApps') ?? [];
    return patchedApps
        .map((app) => PatchedApplication.fromJson(json.decode(app)))
        .toList();
  }
}
