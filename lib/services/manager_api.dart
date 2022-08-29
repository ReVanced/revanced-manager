import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:github/github.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:shared_preferences/shared_preferences.dart';

@lazySingleton
class ManagerAPI {
  final GithubAPI _githubAPI = GithubAPI();
  final RootAPI _rootAPI = RootAPI();
  late SharedPreferences _prefs;
  late List<RepositoryCommit> _commits = [];

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    _commits = (await _githubAPI.getCommits(ghOrg, patchesRepo)).toList();
  }

  Future<File?> downloadPatches(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, ghOrg, patchesRepo);
  }

  Future<File?> downloadIntegrations(String extension) async {
    return await _githubAPI.latestReleaseFile(
      extension,
      ghOrg,
      integrationsRepo,
    );
  }

  Future<File?> downloadManager(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, ghOrg, managerRepo);
  }

  Future<String?> getLatestPatchesVersion() async {
    return await _githubAPI.latestReleaseVersion(ghOrg, patchesRepo);
  }

  Future<String?> getLatestManagerVersion() async {
    return await _githubAPI.latestReleaseVersion(ghOrg, managerRepo);
  }

  Future<String> getCurrentManagerVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  bool? isRooted() {
    return _prefs.getBool('isRooted');
  }

  List<PatchedApplication> getPatchedApps() {
    List<String> apps = _prefs.getStringList('patchedApps') ?? [];
    return apps
        .map((a) => PatchedApplication.fromJson(json.decode(a)))
        .toList();
  }

  void setPatchedApps(List<PatchedApplication> patchedApps) {
    _prefs.setStringList('patchedApps',
        patchedApps.map((a) => json.encode(a.toJson())).toList());
  }

  void savePatchedApp(PatchedApplication app) {
    List<PatchedApplication> patchedApps = getPatchedApps();
    patchedApps.removeWhere((a) => a.packageName == app.packageName);
    patchedApps.add(app);
    setPatchedApps(patchedApps);
  }

  Future<void> reAssessSavedApps() async {
    bool isRoot = isRooted() ?? false;
    List<PatchedApplication> patchedApps = getPatchedApps();
    List<PatchedApplication> toRemove = [];
    for (PatchedApplication app in patchedApps) {
      bool isRemove = await isAppUninstalled(app, isRoot);
      if (isRemove) {
        toRemove.add(app);
      } else {
        List<String> newChangelog = getAppChangelog(
          app.packageName,
          app.patchDate,
        );
        if (newChangelog.isNotEmpty) {
          app.changelog = newChangelog;
          app.hasUpdates = true;
        } else {
          app.hasUpdates = false;
        }
      }
    }
    patchedApps.removeWhere((a) => toRemove.contains(a));
    setPatchedApps(patchedApps);
  }

  Future<bool> isAppUninstalled(PatchedApplication app, bool isRoot) async {
    bool existsRoot = false;
    if (isRoot) {
      existsRoot = await _rootAPI.isAppInstalled(app.packageName);
    }
    bool existsNonRoot = await DeviceApps.isAppInstalled(app.packageName);
    return !existsRoot && !existsNonRoot;
  }

  List<String> getAppChangelog(String packageName, DateTime patchedDate) {
    List<String> newCommits = _commits
        .where((c) =>
            c.commit != null &&
            c.commit!.message != null &&
            c.commit!.author != null &&
            c.commit!.author!.date != null &&
            c.commit!.author!.date!.isAfter(patchedDate))
        .map((c) => c.commit!.message!)
        .toList();
    if (newCommits.isNotEmpty) {
      int firstChore = newCommits.indexWhere((c) => c.startsWith('chore'));
      int secondChore =
          newCommits.indexWhere((c) => c.startsWith('chore'), firstChore + 1);
      if (firstChore >= 0 && secondChore > firstChore) {
        return newCommits.sublist(firstChore + 1, secondChore);
      }
    }
    return List.empty();
  }
}
