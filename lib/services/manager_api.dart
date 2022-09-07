import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:github/github.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:shared_preferences/shared_preferences.dart';

@lazySingleton
class ManagerAPI {
  final GithubAPI _githubAPI = GithubAPI();
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

  String getPatcherRepo() {
    return defaultPatcherRepo;
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

  String getCliRepo() {
    return defaultCliRepo;
  }

  String getManagerRepo() {
    return _prefs.getString('managerRepo') ?? defaultManagerRepo;
  }

  Future<void> setManagerRepo(String value) async {
    if (value.isEmpty || value.startsWith('/') || value.endsWith('/')) {
      value = defaultManagerRepo;
    }
    await _prefs.setString('managerRepo', value);
  }

  Future<File?> downloadPatches(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, getPatchesRepo());
  }

  Future<File?> downloadIntegrations(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, getIntegrationsRepo());
  }

  Future<File?> downloadManager(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, getManagerRepo());
  }

  Future<String?> getLatestPatchesVersion() async {
    return await _githubAPI.latestReleaseVersion(getPatchesRepo());
  }

  Future<String?> getLatestManagerVersion() async {
    return await _githubAPI.latestReleaseVersion(getManagerRepo());
  }

  Future<String> getCurrentManagerVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
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
    return apps
        .map((a) => PatchedApplication.fromJson(json.decode(a)))
        .toList();
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
      existsRoot = await _rootAPI.isAppInstalled(app.packageName);
    }
    bool existsNonRoot = await DeviceApps.isAppInstalled(app.packageName);
    return !existsRoot && !existsNonRoot;
  }

  Future<bool> hasAppUpdates(String packageName, DateTime patchDate) async {
    List<RepositoryCommit> commits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
    );
    return commits.any((c) =>
        c.commit != null &&
        c.commit!.author != null &&
        c.commit!.author!.date != null &&
        c.commit!.author!.date!.isAfter(patchDate));
  }

  Future<List<String>> getAppChangelog(
    String packageName,
    DateTime patchDate,
  ) async {
    List<RepositoryCommit> commits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
    );
    List<String> newCommits = commits
        .where((c) =>
            c.commit != null &&
            c.commit!.author != null &&
            c.commit!.author!.date != null &&
            c.commit!.author!.date!.isAfter(patchDate) &&
            c.commit!.message != null)
        .map((c) => c.commit!.message!)
        .toList();
    if (newCommits.isEmpty) {
      newCommits = commits
          .where((c) => c.commit != null && c.commit!.message != null)
          .take(3)
          .map((c) => c.commit!.message!)
          .toList();
    }
    return newCommits;
  }
}
