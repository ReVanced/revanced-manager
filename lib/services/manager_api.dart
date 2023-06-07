import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:shared_preferences/shared_preferences.dart';

@lazySingleton
class ManagerAPI {
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
  final GithubAPI _githubAPI = locator<GithubAPI>();
  final RootAPI _rootAPI = RootAPI();
  final String patcherRepo = 'revanced-patcher';
  final String cliRepo = 'revanced-cli';
  late SharedPreferences _prefs;
  bool isRooted = false;
  String storedPatchesFile = '/selected-patches.json';
  String keystoreFile =
      '/sdcard/Android/data/app.revanced.manager.flutter/files/revanced-manager.keystore';
  String defaultKeystorePassword = 's3cur3p@ssw0rd';
  String defaultApiUrl = 'https://releases.revanced.app/';
  String defaultRepoUrl = 'https://api.github.com';
  String defaultPatcherRepo = 'revanced/revanced-patcher';
  String defaultPatchesRepo = 'revanced/revanced-patches';
  String defaultIntegrationsRepo = 'revanced/revanced-integrations';
  String defaultCliRepo = 'revanced/revanced-cli';
  String defaultManagerRepo = 'revanced/revanced-manager';
  String? patchesVersion = '';
  bool isDefaultPatchesRepo() {
    return getPatchesRepo() == 'revanced/revanced-patches';
  }

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    isRooted = await _rootAPI.isRooted();
    storedPatchesFile =
        (await getApplicationDocumentsDirectory()).path + storedPatchesFile;
  }

  String getApiUrl() {
    return _prefs.getString('apiUrl') ?? defaultApiUrl;
  }

  Future<void> setApiUrl(String url) async {
    if (url.isEmpty || url == ' ') {
      url = defaultApiUrl;
    }
    await _revancedAPI.initialize(url);
    await _revancedAPI.clearAllCache();
    await _prefs.setString('apiUrl', url);
  }

  String getRepoUrl() {
    return _prefs.getString('repoUrl') ?? defaultRepoUrl;
  }

  Future<void> setRepoUrl(String url) async {
    if (url.isEmpty || url == ' ') {
      url = defaultRepoUrl;
    }
    await _prefs.setString('repoUrl', url);
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

  bool areUniversalPatchesEnabled() {
    return _prefs.getBool('universalPatchesEnabled') ?? false;
  }

  Future<void> enableUniversalPatchesStatus(bool value) async {
    await _prefs.setBool('universalPatchesEnabled', value);
  }

  bool areExperimentalPatchesEnabled() {
    return _prefs.getBool('experimentalPatchesEnabled') ?? false;
  }

  Future<void> enableExperimentalPatchesStatus(bool value) async {
    await _prefs.setBool('experimentalPatchesEnabled', value);
  }

  Future<void> setKeystorePassword(String password) async {
    await _prefs.setString('keystorePassword', password);
  }

  String getKeystorePassword() {
    return _prefs.getString('keystorePassword') ?? defaultKeystorePassword;
  }

  Future<void> deleteTempFolder() async {
    final Directory dir = Directory('/data/local/tmp/revanced-manager');
    if (await dir.exists()) {
      await dir.delete(recursive: true);
    }
  }

  Future<void> deleteKeystore() async {
    final File keystore = File(
      keystoreFile,
    );
    if (await keystore.exists()) {
      await keystore.delete();
    }
  }

  List<PatchedApplication> getPatchedApps() {
    final List<String> apps = _prefs.getStringList('patchedApps') ?? [];
    return apps.map((a) => PatchedApplication.fromJson(jsonDecode(a))).toList();
  }

  Future<void> setPatchedApps(
    List<PatchedApplication> patchedApps,
  ) async {
    if (patchedApps.length > 1) {
      patchedApps.sort((a, b) => a.name.compareTo(b.name));
    }
    await _prefs.setStringList(
      'patchedApps',
      patchedApps.map((a) => json.encode(a.toJson())).toList(),
    );
  }

  Future<void> savePatchedApp(PatchedApplication app) async {
    final List<PatchedApplication> patchedApps = getPatchedApps();
    patchedApps.removeWhere((a) => a.packageName == app.packageName);
    final ApplicationWithIcon? installed = await DeviceApps.getApp(
      app.packageName,
      true,
    ) as ApplicationWithIcon?;
    if (installed != null) {
      app.name = installed.appName;
      app.version = installed.versionName!;
      app.icon = installed.icon;
    }
    patchedApps.add(app);
    await setPatchedApps(patchedApps);
  }

  Future<void> deletePatchedApp(PatchedApplication app) async {
    final List<PatchedApplication> patchedApps = getPatchedApps();
    patchedApps.removeWhere((a) => a.packageName == app.packageName);
    await setPatchedApps(patchedApps);
  }

  Future<void> clearAllData() async {
    try {
      _revancedAPI.clearAllCache();
      _githubAPI.clearAllCache();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<Map<String, List<dynamic>>> getContributors() async {
    return await _revancedAPI.getContributors();
  }

  Future<List<Patch>> getPatches() async {
    try {
      final String repoName = getPatchesRepo();
      if (repoName == defaultPatchesRepo) {
        return await _revancedAPI.getPatches();
      } else {
        return await _githubAPI.getPatches(repoName);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return [];
    }
  }

  Future<File?> downloadPatches() async {
    try {
      final String repoName = getPatchesRepo();
      if (repoName == defaultPatchesRepo) {
        return await _revancedAPI.getLatestReleaseFile(
          '.jar',
          defaultPatchesRepo,
        );
      } else {
        return await _githubAPI.getLatestReleaseFile('.jar', repoName);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<File?> downloadIntegrations() async {
    try {
      final String repoName = getIntegrationsRepo();
      if (repoName == defaultIntegrationsRepo) {
        return await _revancedAPI.getLatestReleaseFile(
          '.apk',
          defaultIntegrationsRepo,
        );
      } else {
        return await _githubAPI.getLatestReleaseFile('.apk', repoName);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<File?> downloadManager() async {
    return await _revancedAPI.getLatestReleaseFile(
      '.apk',
      defaultManagerRepo,
    );
  }

  Future<String?> getLatestPatcherReleaseTime() async {
    return await _revancedAPI.getLatestReleaseTime(
      '.gz',
      defaultPatcherRepo,
    );
  }

  Future<String?> getLatestManagerReleaseTime() async {
    return await _revancedAPI.getLatestReleaseTime(
      '.apk',
      defaultManagerRepo,
    );
  }

  Future<String?> getLatestManagerVersion() async {
    return await _revancedAPI.getLatestReleaseVersion(
      '.apk',
      defaultManagerRepo,
    );
  }

  Future<String?> getLatestPatchesVersion() async {
    return await _revancedAPI.getLatestReleaseVersion(
      '.json',
      defaultPatchesRepo,
    );
  }

  Future<String> getCurrentManagerVersion() async {
    final PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  Future<String?> getCurrentPatchesVersion() async {
    if (isDefaultPatchesRepo()) {
      patchesVersion = await getLatestPatchesVersion();
      // print('Patches version: $patchesVersion');
    } else {
      // fetch from github
      patchesVersion =
          await _githubAPI.getLastestReleaseVersion(getPatchesRepo());
    }
    return patchesVersion ?? '0.0.0';
  }

  Future<List<PatchedApplication>> getAppsToRemove(
    List<PatchedApplication> patchedApps,
  ) async {
    final List<PatchedApplication> toRemove = [];
    for (final PatchedApplication app in patchedApps) {
      final bool isRemove = await isAppUninstalled(app);
      if (isRemove) {
        toRemove.add(app);
      }
    }
    return toRemove;
  }

  Future<List<PatchedApplication>> getUnsavedApps(
    List<PatchedApplication> patchedApps,
  ) async {
    final List<PatchedApplication> unsavedApps = [];
    final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
    if (hasRootPermissions) {
      final List<String> installedApps = await _rootAPI.getInstalledApps();
      for (final String packageName in installedApps) {
        if (!patchedApps.any((app) => app.packageName == packageName)) {
          final ApplicationWithIcon? application = await DeviceApps.getApp(
            packageName,
            true,
          ) as ApplicationWithIcon?;
          if (application != null) {
            unsavedApps.add(
              PatchedApplication(
                name: application.appName,
                packageName: application.packageName,
                originalPackageName: application.packageName,
                version: application.versionName!,
                apkFilePath: application.apkFilePath,
                icon: application.icon,
                patchDate: DateTime.now(),
                isRooted: true,
              ),
            );
          }
        }
      }
    }
    final List<Application> userApps =
        await DeviceApps.getInstalledApplications();
    for (final Application app in userApps) {
      if (app.packageName.startsWith('app.revanced') &&
          !app.packageName.startsWith('app.revanced.manager.') &&
          !patchedApps.any((uapp) => uapp.packageName == app.packageName)) {
        final ApplicationWithIcon? application = await DeviceApps.getApp(
          app.packageName,
          true,
        ) as ApplicationWithIcon?;
        if (application != null) {
          unsavedApps.add(
            PatchedApplication(
              name: application.appName,
              packageName: application.packageName,
              originalPackageName: application.packageName,
              version: application.versionName!,
              apkFilePath: application.apkFilePath,
              icon: application.icon,
              patchDate: DateTime.now(),
            ),
          );
        }
      }
    }
    return unsavedApps;
  }

  Future<void> reAssessSavedApps() async {
    final List<PatchedApplication> patchedApps = getPatchedApps();
    final List<PatchedApplication> unsavedApps =
        await getUnsavedApps(patchedApps);
    patchedApps.addAll(unsavedApps);
    final List<PatchedApplication> toRemove =
        await getAppsToRemove(patchedApps);
    patchedApps.removeWhere((a) => toRemove.contains(a));
    for (final PatchedApplication app in patchedApps) {
      app.hasUpdates =
          await hasAppUpdates(app.originalPackageName, app.patchDate);
      app.changelog =
          await getAppChangelog(app.originalPackageName, app.patchDate);
      if (!app.hasUpdates) {
        final String? currentInstalledVersion =
            (await DeviceApps.getApp(app.packageName))?.versionName;
        if (currentInstalledVersion != null) {
          final String currentSavedVersion = app.version;
          final int currentInstalledVersionInt = int.parse(
            currentInstalledVersion.replaceAll(RegExp('[^0-9]'), ''),
          );
          final int currentSavedVersionInt = int.parse(
            currentSavedVersion.replaceAll(RegExp('[^0-9]'), ''),
          );
          if (currentInstalledVersionInt > currentSavedVersionInt) {
            app.hasUpdates = true;
          }
        }
      }
    }
    await setPatchedApps(patchedApps);
  }

  Future<bool> isAppUninstalled(PatchedApplication app) async {
    bool existsRoot = false;
    final bool existsNonRoot = await DeviceApps.isAppInstalled(app.packageName);
    if (app.isRooted) {
      final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
      if (hasRootPermissions) {
        existsRoot = await _rootAPI.isAppInstalled(app.packageName);
      }
      return !existsRoot || !existsNonRoot;
    }
    return !existsNonRoot;
  }

  Future<bool> hasAppUpdates(
    String packageName,
    DateTime patchDate,
  ) async {
    final List<String> commits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
      patchDate,
    );
    return commits.isNotEmpty;
  }

  Future<List<String>> getAppChangelog(
    String packageName,
    DateTime patchDate,
  ) async {
    List<String> newCommits = await _githubAPI.getCommits(
      packageName,
      getPatchesRepo(),
      patchDate,
    );
    if (newCommits.isEmpty) {
      newCommits = await _githubAPI.getCommits(
        packageName,
        getPatchesRepo(),
        patchDate,
      );
    }
    return newCommits;
  }

  Future<bool> isSplitApk(PatchedApplication patchedApp) async {
    Application? app;
    if (patchedApp.isFromStorage) {
      app = await DeviceApps.getAppFromStorage(patchedApp.apkFilePath);
    } else {
      app = await DeviceApps.getApp(patchedApp.packageName);
    }
    return app != null && app.isSplit;
  }

  Future<void> setSelectedPatches(
    String app,
    List<String> patches,
  ) async {
    final File selectedPatchesFile = File(storedPatchesFile);
    final Map<String, dynamic> patchesMap = await readSelectedPatchesFile();
    if (patches.isEmpty) {
      patchesMap.remove(app);
    } else {
      patchesMap[app] = patches;
    }
    selectedPatchesFile.writeAsString(jsonEncode(patchesMap));
  }

  // get default patches for app
  Future<List<String>> getDefaultPatches() async {
    final List<Patch> patches = await getPatches();
    final List<String> defaultPatches = [];
    if (areExperimentalPatchesEnabled() == false) {
      defaultPatches.addAll(
        patches
            .where(
              (element) =>
                  element.excluded == false && isPatchSupported(element),
            )
            .map((p) => p.name),
      );
    } else {
      defaultPatches.addAll(
        patches
            .where((element) => isPatchSupported(element))
            .map((p) => p.name),
      );
    }
    return defaultPatches;
  }

  Future<List<String>> getSelectedPatches(String app) async {
    final Map<String, dynamic> patchesMap = await readSelectedPatchesFile();
    final List<String> defaultPatches = await getDefaultPatches();
    return List.from(patchesMap.putIfAbsent(app, () => defaultPatches));
  }

  Future<Map<String, dynamic>> readSelectedPatchesFile() async {
    final File selectedPatchesFile = File(storedPatchesFile);
    if (!selectedPatchesFile.existsSync()) {
      return {};
    }
    final String string = selectedPatchesFile.readAsStringSync();
    if (string.trim().isEmpty) {
      return {};
    }
    return jsonDecode(string);
  }

  Future<void> resetLastSelectedPatches() async {
    final File selectedPatchesFile = File(storedPatchesFile);
    selectedPatchesFile.deleteSync();
  }
}
