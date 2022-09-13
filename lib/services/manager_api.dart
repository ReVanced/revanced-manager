import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:shared_preferences/shared_preferences.dart';

@lazySingleton
class ManagerAPI {
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
  final GithubAPI _githubAPI = locator<GithubAPI>();
  final RootAPI _rootAPI = RootAPI();
  final String patcherRepo = 'revanced-patcher';
  final String cliRepo = 'revanced-cli';
  late SharedPreferences _prefs;
  String defaultPatcherRepo = 'revanced/revanced-patcher';
  String defaultPatchesRepo = 'revanced/revanced-patches';
  String defaultIntegrationsRepo = 'revanced/revanced-integrations';
  String defaultCliRepo = 'revanced/revanced-cli';
  String defaultManagerRepo = 'revanced/revanced-manager';

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
  }

  String getPatchesRepo() {
    return _prefs.getString('patchesRepo') ?? defaultPatchesRepo;
  }

  Future<void> setPatchesRepo(String value) async {
    if (value.isEmpty || value.startsWith('/') || value.endsWith('/')) {
      value = defaultPatchesRepo;
    }
    await _prefs.setString('patchesRepo', value);
  }

  String getIntegrationsRepo() {
    return _prefs.getString('integrationsRepo') ?? defaultIntegrationsRepo;
  }

  Future<void> setIntegrationsRepo(String value) async {
    if (value.isEmpty || value.startsWith('/') || value.endsWith('/')) {
      value = defaultIntegrationsRepo;
    }
    await _prefs.setString('integrationsRepo', value);
  }

  bool getUseDynamicTheme() {
    return _prefs.getBool('useDynamicTheme') ?? false;
  }

  Future<void> setUseDynamicTheme(bool value) async {
    await _prefs.setBool('useDynamicTheme', value);
  }

  bool getUseDarkTheme() {
    return _prefs.getBool('useDarkTheme') ?? false;
  }

  Future<void> setUseDarkTheme(bool value) async {
    await _prefs.setBool('useDarkTheme', value);
  }

  List<PatchedApplication> getPatchedApps() {
    List<String> apps = _prefs.getStringList('patchedApps') ?? [];
    return apps.map((a) => PatchedApplication.fromJson(jsonDecode(a))).toList();
  }

  Future<void> setPatchedApps(List<PatchedApplication> patchedApps) async {
    if (patchedApps.length > 1) {
      patchedApps.sort((a, b) => a.name.compareTo(b.name));
    }
    await _prefs.setStringList('patchedApps',
        patchedApps.map((a) => json.encode(a.toJson())).toList());
  }

  Future<void> savePatchedApp(PatchedApplication app) async {
    List<PatchedApplication> patchedApps = getPatchedApps();
    patchedApps.removeWhere((a) => a.packageName == app.packageName);
    ApplicationWithIcon? installed =
        await DeviceApps.getApp(app.packageName, true) as ApplicationWithIcon?;
    if (installed != null) {
      app.name = installed.appName;
      app.version = installed.versionName!;
      app.icon = installed.icon;
    }
    patchedApps.add(app);
    await setPatchedApps(patchedApps);
  }

  Future<void> deletePatchedApp(PatchedApplication app) async {
    List<PatchedApplication> patchedApps = getPatchedApps();
    patchedApps.removeWhere((a) => a.packageName == app.packageName);
    await setPatchedApps(patchedApps);
  }

  void clearAllData() {
    _revancedAPI.clearAllCache();
    _githubAPI.clearAllCache();
  }

  Future<Map<String, List<dynamic>>> getContributors() async {
    return await _revancedAPI.getContributors();
  }

  Future<List<Patch>> getPatches() async {
    if (getPatchesRepo() == defaultPatchesRepo) {
      return await _revancedAPI.getPatches();
    } else {
      return await _githubAPI.getPatches(getPatchesRepo());
    }
  }

  Future<File?> downloadPatches() async {
    String repoName = getPatchesRepo();
    if (repoName == defaultPatchesRepo) {
      return await _revancedAPI.getLatestReleaseFile(
        '.jar',
        defaultPatchesRepo,
      );
    } else {
      return await _githubAPI.getLatestReleaseFile('.jar', repoName);
    }
  }

  Future<File?> downloadIntegrations() async {
    String repoName = getIntegrationsRepo();
    if (repoName == defaultIntegrationsRepo) {
      return await _revancedAPI.getLatestReleaseFile(
        '.apk',
        defaultIntegrationsRepo,
      );
    } else {
      return await _githubAPI.getLatestReleaseFile('.apk', repoName);
    }
  }

  Future<File?> downloadManager() async {
    return await _revancedAPI.getLatestReleaseFile('.apk', defaultManagerRepo);
  }

  Future<String?> getLatestPatcherReleaseTime() async {
    return await _revancedAPI.getLatestReleaseTime('.gz', defaultPatcherRepo);
  }

  Future<String?> getLatestManagerReleaseTime() async {
    return await _revancedAPI.getLatestReleaseTime('.apk', defaultManagerRepo);
  }

  Future<String?> getLatestManagerVersion() async {
    return await _revancedAPI.getLatestReleaseVersion(
      '.apk',
      defaultManagerRepo,
    );
  }

  Future<String> getCurrentManagerVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  Future<void> reAssessSavedApps() async {
    List<PatchedApplication> patchedApps = getPatchedApps();
    List<PatchedApplication> toRemove = [];
    for (PatchedApplication app in patchedApps) {
      bool isRemove = await isAppUninstalled(app);
      if (isRemove) {
        toRemove.add(app);
      } else {
        app.hasUpdates = await hasAppUpdates(app.packageName, app.patchDate);
        app.changelog = await getAppChangelog(app.packageName, app.patchDate);
        if (!app.hasUpdates) {
          String? currentInstalledVersion =
              (await DeviceApps.getApp(app.packageName))?.versionName;
          if (currentInstalledVersion != null) {
            String currentSavedVersion = app.version;
            int currentInstalledVersionInt = int.parse(
                currentInstalledVersion.replaceAll(RegExp('[^0-9]'), ''));
            int currentSavedVersionInt =
                int.parse(currentSavedVersion.replaceAll(RegExp('[^0-9]'), ''));
            if (currentInstalledVersionInt > currentSavedVersionInt) {
              app.hasUpdates = true;
            }
          }
        }
      }
    }
    patchedApps.removeWhere((a) => toRemove.contains(a));
    await setPatchedApps(patchedApps);
  }

  Future<bool> isAppUninstalled(PatchedApplication app) async {
    bool existsRoot = false;
    if (app.isRooted) {
      bool hasRootPermissions = await _rootAPI.hasRootPermissions();
      if (hasRootPermissions) {
        existsRoot = await _rootAPI.isAppInstalled(app.packageName);
      }
    }
    bool existsNonRoot = await DeviceApps.isAppInstalled(app.packageName);
    return !existsRoot && !existsNonRoot;
  }

  Future<bool> hasAppUpdates(String packageName, DateTime patchDate) async {
    List<String> commits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
      patchDate,
    );
    return commits.isNotEmpty;
  }

  Future<List<String>> getAppChangelog(
      String packageName, DateTime patchDate) async {
    List<String> newCommits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
      patchDate,
    );
    if (newCommits.isEmpty) {
      newCommits = await _githubAPI.getCommits(
        packageName,
        getPatchesRepo(),
        DateTime(2022, 3, 20, 21, 06, 01),
      );
    }
    return newCommits;
  }
}
