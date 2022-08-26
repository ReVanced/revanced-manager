import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
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

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
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

  void saveApp(
    ApplicationWithIcon application,
    bool isRooted,
    bool isFromStorage,
  ) {
    savePatchedApp(
      PatchedApplication(
        name: application.appName,
        packageName: application.packageName,
        version: application.versionName!,
        apkFilePath: application.apkFilePath,
        icon: application.icon,
        patchDate: DateTime.now(),
        isRooted: isRooted,
        isFromStorage: isFromStorage,
        appliedPatches: [],
      ),
    );
  }

  Future<void> reAssessSavedApps() async {
    List<PatchedApplication> patchedApps = getPatchedApps();
    bool isRoot = isRooted() ?? false;
    List<PatchedApplication> toRemove = [];
    for (PatchedApplication app in patchedApps) {
      bool existsRoot = false;
      if (isRoot) {
        existsRoot = await _rootAPI.isAppInstalled(app.packageName);
      }
      bool existsNonRoot = await DeviceApps.isAppInstalled(app.packageName);
      if (!existsRoot && !existsNonRoot) {
        toRemove.add(app);
      } else if (existsNonRoot) {
        ApplicationWithIcon? application =
            await DeviceApps.getApp(app.packageName, true)
                as ApplicationWithIcon?;
        if (application != null) {
          int savedVersionInt =
              int.parse(app.version.replaceAll(RegExp('[^0-9]'), ''));
          int currentVersionInt = int.parse(
              application.versionName!.replaceAll(RegExp('[^0-9]'), ''));
          if (savedVersionInt < currentVersionInt) {
            toRemove.add(app);
          }
        }
      }
    }
    patchedApps.removeWhere((a) => toRemove.contains(a));
    setPatchedApps(patchedApps);
    List<String> apps = await _rootAPI.getInstalledApps();
    for (String packageName in apps) {
      if (!patchedApps.any((a) => a.packageName == packageName)) {
        ApplicationWithIcon? application =
            await DeviceApps.getApp(packageName, true) as ApplicationWithIcon?;
        if (application != null) {
          saveApp(application, true, false);
        }
      }
    }
  }

  Future<bool> hasAppUpdates(String packageName) async {
    // TODO: get status based on last update time on the folder of this app?
    return false;
  }

  Future<List<String>> getAppChangelog(
    String packageName,
    DateTime lastUpdated,
  ) async {
    return (await _githubAPI.getCommits(ghOrg, patchesRepo))
        .where((c) =>
            c.commit != null &&
            c.commit!.message != null &&
            !c.commit!.message!.startsWith('chore') &&
            c.commit!.author != null &&
            c.commit!.author!.date != null)
        .map((c) => '  - ${c.commit!.message!}')
        .toList()
        .sublist(0, 3);
  }
}
