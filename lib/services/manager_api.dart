import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_checkbox_list_tile.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class ManagerAPI {
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
  final GithubAPI _githubAPI = locator<GithubAPI>();
  final Toast _toast = locator<Toast>();
  final RootAPI _rootAPI = RootAPI();
  final String patcherRepo = 'revanced-patcher';
  final String cliRepo = 'revanced-cli';
  late SharedPreferences _prefs;
  Map<String, List>? contributors;
  List<Patch> patches = [];
  List<Option> options = [];
  Patch? selectedPatch;
  BuildContext? ctx;
  bool isRooted = false;
  bool releaseBuild = false;
  bool suggestedAppVersionSelected = true;
  bool isDynamicThemeAvailable = false;
  bool isScopedStorageAvailable = false;
  int sdkVersion = 0;
  String storedPatchesFile = '/selected-patches.json';
  String keystoreFile =
      '/sdcard/Android/data/app.revanced.manager.flutter/files/revanced-manager.keystore';
  String defaultKeystorePassword = 's3cur3p@ssw0rd';
  String defaultApiUrl = 'https://api.revanced.app/v4';
  String defaultRepoUrl = 'https://api.github.com';
  String defaultPatcherRepo = 'revanced/revanced-patcher';
  String defaultPatchesRepo = 'revanced/revanced-patches';
  String defaultCliRepo = 'revanced/revanced-cli';
  String defaultManagerRepo = 'revanced/revanced-manager';
  String? patchesVersion = '';

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    isRooted = await _rootAPI.isRooted();
    if (sdkVersion == 0) {
      sdkVersion = await getSdkVersion();
    }
    isDynamicThemeAvailable = sdkVersion >= 31; // ANDROID_12_SDK_VERSION = 31
    isScopedStorageAvailable = sdkVersion >= 30; // ANDROID_11_SDK_VERSION = 30
    storedPatchesFile =
        (await getApplicationDocumentsDirectory()).path + storedPatchesFile;
    if (kReleaseMode) {
      releaseBuild = !(await getCurrentManagerVersion()).contains('-dev');
    }

    final hasMigratedToNewMigrationSystem = _prefs.getBool('migratedToNewApiPrefSystem') ?? false;
    if (!hasMigratedToNewMigrationSystem) {
      final apiUrl = getApiUrl().toLowerCase();

      final isReleases = apiUrl.contains('releases.revanced.app');
      final isDomain = apiUrl.endsWith('api.revanced.app');
      final isV2 = apiUrl.contains('api.revanced.app/v2');
      final isV3 = apiUrl.contains('api.revanced.app/v3');

      if (isReleases || isDomain || isV2 || isV3) {
        await resetApiUrl();
        // At this point, the preference is removed.
        // Now, no more migration is needed because:
        // If the user touches the API URL,
        // it will be remembered forever as intended.
        // On the other hand, if the user resets it or sets it to the default,
        // the URL will be updated whenever the app is updated.
        _prefs.setBool('migratedToNewApiPrefSystem', true);
      }
    }

    final bool hasMigratedToAlternativeSource =
        _prefs.getBool('migratedToAlternativeSource') ?? false;
    if (!hasMigratedToAlternativeSource) {
      final String patchesRepo = getPatchesRepo();
      final bool usingAlternativeSources =
          patchesRepo.toLowerCase() != defaultPatchesRepo;
      _prefs.setBool('useAlternativeSources', usingAlternativeSources);
      _prefs.setBool('migratedToAlternativeSource', true);
    }
  }

  Future<int> getSdkVersion() async {
    final AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt;
  }

  String getApiUrl() {
    return _prefs.getString('apiUrl') ?? defaultApiUrl;
  }

  Future<void> resetApiUrl() async {
    await _prefs.remove('apiUrl');
    await _revancedAPI.clearAllCache();
    _toast.showBottom(t.settingsView.restartAppForChanges);
  }

  Future<void> setApiUrl(String url) async {
    url = url.toLowerCase();

    if (url == defaultApiUrl) {
      return;
    }

    if (!url.startsWith('http')) {
      url = 'https://$url';
    }

    await _prefs.setString('apiUrl', url);
    await _revancedAPI.clearAllCache();
    _toast.showBottom(t.settingsView.restartAppForChanges);
  }

  String getRepoUrl() {
    return defaultRepoUrl;
  }

  String getPatchesDownloadURL() {
    return _prefs.getString('patchesDownloadURL') ?? '';
  }

  Future<void> setPatchesDownloadURL(String value) async {
    await _prefs.setString('patchesDownloadURL', value);
  }

  String getPatchesRepo() {
    if (!isUsingAlternativeSources()) {
      return defaultPatchesRepo;
    }
    return _prefs.getString('patchesRepo') ?? defaultPatchesRepo;
  }

  Future<void> setPatchesRepo(String value) async {
    if (value.isEmpty || value.startsWith('/') || value.endsWith('/')) {
      value = defaultPatchesRepo;
    }
    await _prefs.setString('patchesRepo', value);
  }

  bool getDownloadConsent() {
    return _prefs.getBool('downloadConsent') ?? false;
  }

  void setDownloadConsent(bool consent) {
    _prefs.setBool('downloadConsent', consent);
  }

  bool isPatchesAutoUpdate() {
    return _prefs.getBool('patchesAutoUpdate') ?? false;
  }

  bool isPatchesChangeEnabled() {
    return _prefs.getBool('patchesChangeEnabled') ?? false;
  }

  void setPatchesChangeEnabled(bool value) {
    _prefs.setBool('patchesChangeEnabled', value);
  }

  bool showPatchesChangeWarning() {
    return _prefs.getBool('showPatchesChangeWarning') ?? true;
  }

  void setPatchesChangeWarning(bool value) {
    _prefs.setBool('showPatchesChangeWarning', !value);
  }

  bool showUpdateDialog() {
    return _prefs.getBool('showUpdateDialog') ?? true;
  }

  void setShowUpdateDialog(bool value) {
    _prefs.setBool('showUpdateDialog', value);
  }

  bool isChangingToggleModified() {
    return _prefs.getBool('isChangingToggleModified') ?? false;
  }

  void setChangingToggleModified(bool value) {
    _prefs.setBool('isChangingToggleModified', value);
  }

  void setPatchesAutoUpdate(bool value) {
    _prefs.setBool('patchesAutoUpdate', value);
  }

  List<Patch> getSavedPatches(String packageName) {
    final List<String> patchesJson =
        _prefs.getStringList('savedPatches-$packageName') ?? [];
    final List<Patch> patches = patchesJson.map((String patchJson) {
      return Patch.fromJson(jsonDecode(patchJson));
    }).toList();
    return patches;
  }

  Future<void> savePatches(List<Patch> patches, String packageName) async {
    final List<String> patchesJson = patches.map((Patch patch) {
      return jsonEncode(patch.toJson());
    }).toList();
    await _prefs.setStringList('savedPatches-$packageName', patchesJson);
  }

  List<Patch> getUsedPatches(String packageName) {
    final List<String> patchesJson =
        _prefs.getStringList('usedPatches-$packageName') ?? [];
    final List<Patch> patches = patchesJson.map((String patchJson) {
      return Patch.fromJson(jsonDecode(patchJson));
    }).toList();
    return patches;
  }

  Future<void> setUsedPatches(List<Patch> patches, String packageName) async {
    final List<String> patchesJson = patches.map((Patch patch) {
      return jsonEncode(patch.toJson());
    }).toList();
    await _prefs.setStringList('usedPatches-$packageName', patchesJson);
  }

  void useAlternativeSources(bool value) {
    _prefs.setBool('useAlternativeSources', value);
    _toast.showBottom(t.settingsView.restartAppForChanges);
  }

  bool isUsingAlternativeSources() {
    return _prefs.getBool('useAlternativeSources') ?? false;
  }

  Option? getPatchOption(String packageName, String patchName, String key) {
    final String? optionJson =
        _prefs.getString('patchOption-$packageName-$patchName-$key');
    if (optionJson != null) {
      final Option option = Option.fromJson(jsonDecode(optionJson));
      return option;
    } else {
      return null;
    }
  }

  void setPatchOption(Option option, String patchName, String packageName) {
    final String optionJson = jsonEncode(option.toJson());
    _prefs.setString(
      'patchOption-$packageName-$patchName-${option.key}',
      optionJson,
    );
  }

  void clearPatchOption(String packageName, String patchName, String key) {
    _prefs.remove('patchOption-$packageName-$patchName-$key');
  }

  bool getUseDynamicTheme() {
    return _prefs.getBool('useDynamicTheme') ?? false;
  }

  Future<void> setUseDynamicTheme(bool value) async {
    await _prefs.setBool('useDynamicTheme', value);
  }

  int getThemeMode() {
    return _prefs.getInt('themeMode') ?? 2;
  }

  Future<void> setThemeMode(int value) async {
    await _prefs.setInt('themeMode', value);
  }

  bool areUniversalPatchesEnabled() {
    return _prefs.getBool('universalPatchesEnabled') ?? false;
  }

  Future<void> enableUniversalPatchesStatus(bool value) async {
    await _prefs.setBool('universalPatchesEnabled', value);
  }

  bool isVersionCompatibilityCheckEnabled() {
    return _prefs.getBool('versionCompatibilityCheckEnabled') ?? true;
  }

  Future<void> enableVersionCompatibilityCheckStatus(bool value) async {
    await _prefs.setBool('versionCompatibilityCheckEnabled', value);
  }

  bool isRequireSuggestedAppVersionEnabled() {
    return _prefs.getBool('requireSuggestedAppVersionEnabled') ?? true;
  }

  Future<void> enableRequireSuggestedAppVersionStatus(bool value) async {
    await _prefs.setBool('requireSuggestedAppVersionEnabled', value);
  }

  bool isLastPatchedAppEnabled() {
    return _prefs.getBool('lastPatchedAppEnabled') ?? true;
  }

  Future<void> enableLastPatchedAppStatus(bool value) async {
    await _prefs.setBool('lastPatchedAppEnabled', value);
  }

  Future<void> setKeystorePassword(String password) async {
    await _prefs.setString('keystorePassword', password);
  }

  String getKeystorePassword() {
    return _prefs.getString('keystorePassword') ?? defaultKeystorePassword;
  }

  String getLocale() {
    return _prefs.getString('locale') ?? 'en';
  }

  Future<void> setLocale(String value) async {
    await _prefs.setString('locale', value);
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

  PatchedApplication? getLastPatchedApp() {
    final String? app = _prefs.getString('lastPatchedApp');
    return app != null ? PatchedApplication.fromJson(jsonDecode(app)) : null;
  }

  Future<void> deleteLastPatchedApp() async {
    final PatchedApplication? app = getLastPatchedApp();
    if (app != null) {
      final File file = File(app.patchedFilePath);
      await file.delete();
      await _prefs.remove('lastPatchedApp');
    }
  }

  Future<void> setLastPatchedApp(
    PatchedApplication app,
    File outFile,
  ) async {
    deleteLastPatchedApp();
    final Directory appCache = await getApplicationSupportDirectory();
    app.patchedFilePath =
        outFile.copySync('${appCache.path}/lastPatchedApp.apk').path;
    app.fileSize = outFile.lengthSync();
    await _prefs.setString(
      'lastPatchedApp',
      json.encode(app.toJson()),
    );
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
    return contributors ??= await _revancedAPI.getContributors();
  }

  Future<List<Patch>> getPatches() async {
    if (patches.isNotEmpty) {
      return patches;
    }
    final File? patchBundleFile = await downloadPatches();
    if (patchBundleFile != null) {
      try {
        final String patchesJson = await PatcherAPI.patcherChannel.invokeMethod(
          'getPatches',
          {
            'patchBundleFilePath': patchBundleFile.path,
          },
        );
        final List<dynamic> patchesJsonList = jsonDecode(patchesJson);
        patches = patchesJsonList
            .map((patchJson) => Patch.fromJson(patchJson))
            .toList();
        return patches;
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    }

    return List.empty();
  }

  Future<File?> downloadPatches() async {
    if (!isUsingAlternativeSources()) {
      return await _revancedAPI.getLatestReleaseFile('patches');
    }

    try {
      final String repoName = getPatchesRepo();
      final String currentVersion = await getCurrentPatchesVersion();
      final String url = getPatchesDownloadURL();
      return await _githubAPI.getReleaseFile(
        '.rvp',
        repoName,
        currentVersion,
        url,
      );
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<File?> downloadManager() async {
    return await _revancedAPI.getLatestReleaseFile('manager');
  }

  Future<String?> getLatestPatchesReleaseTime() async {
    if (!isUsingAlternativeSources()) {
      return await _revancedAPI.getLatestReleaseTime('patches');
    } else {
      final release = await _githubAPI.getLatestRelease(getPatchesRepo());
      if (release != null) {
        final DateTime timestamp =
            DateTime.parse(release['created_at'] as String);
        return format(timestamp, locale: 'en_short');
      } else {
        return null;
      }
    }
  }

  Future<String?> getLatestManagerReleaseTime() async {
    return await _revancedAPI.getLatestReleaseTime(
      'manager',
    );
  }

  Future<String?> getLatestManagerVersion() async {
    return await _revancedAPI.getLatestReleaseVersion(
      'manager',
    );
  }

  Future<String?> getLatestPatchesVersion() async {
    if (!isUsingAlternativeSources()) {
      return await _revancedAPI.getLatestReleaseVersion(
        'patches',
      );
    } else {
      final release = await _githubAPI.getLatestRelease(getPatchesRepo());
      if (release != null) {
        return release['tag_name'];
      } else {
        return null;
      }
    }
  }

  String getLastUsedPatchesVersion() {
    final String lastPatchesVersions =
        _prefs.getString('lastUsedPatchesVersion') ?? '{}';
    final Map<String, dynamic> lastPatchesVersionMap =
        jsonDecode(lastPatchesVersions);
    final String repo = getPatchesRepo();
    return lastPatchesVersionMap[repo] ?? '0.0.0';
  }

  void setLastUsedPatchesVersion({String? version}) {
    final String lastPatchesVersions =
        _prefs.getString('lastUsedPatchesVersion') ?? '{}';
    final Map<String, dynamic> lastPatchesVersionMap =
        jsonDecode(lastPatchesVersions);
    final repo = getPatchesRepo();
    final String lastPatchesVersion =
        version ?? lastPatchesVersionMap[repo] ?? '0.0.0';
    lastPatchesVersionMap[repo] = lastPatchesVersion;
    _prefs.setString(
      'lastUsedPatchesVersion',
      jsonEncode(lastPatchesVersionMap),
    );
  }

  Future<String> getCurrentManagerVersion() async {
    final PackageInfo packageInfo = await PackageInfo.fromPlatform();
    String version = packageInfo.version;
    if (!version.startsWith('v')) {
      version = 'v$version';
    }
    return version;
  }

  Future<String> getCurrentPatchesVersion() async {
    patchesVersion = _prefs.getString('patchesVersion') ?? '0.0.0';
    if (patchesVersion == '0.0.0' || isPatchesAutoUpdate()) {
      final String newPatchesVersion =
          await getLatestPatchesVersion() ?? '0.0.0';
      if (patchesVersion != newPatchesVersion && newPatchesVersion != '0.0.0') {
        await setCurrentPatchesVersion(newPatchesVersion);
      }
    }
    return patchesVersion!;
  }

  Future<void> setCurrentPatchesVersion(String version) async {
    await _prefs.setString('patchesVersion', version);
    await setPatchesDownloadURL('');
    await downloadPatches();
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

  Future<List<PatchedApplication>> getMountedApps() async {
    final List<PatchedApplication> mountedApps = [];
    final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
    if (hasRootPermissions) {
      final List<String> installedApps = await _rootAPI.getInstalledApps();
      for (final String packageName in installedApps) {
        final ApplicationWithIcon? application = await DeviceApps.getApp(
          packageName,
          true,
        ) as ApplicationWithIcon?;
        if (application != null) {
          mountedApps.add(
            PatchedApplication(
              name: application.appName,
              packageName: application.packageName,
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

    return mountedApps;
  }

  Future<void> showPatchesChangeWarningDialog(BuildContext context) {
    final ValueNotifier<bool> noShow =
        ValueNotifier(!showPatchesChangeWarning());
    return showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) => PopScope(
        canPop: false,
        child: AlertDialog(
          title: Text(t.warning),
          content: ValueListenableBuilder(
            valueListenable: noShow,
            builder: (context, value, child) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    t.patchItem.patchesChangeWarningDialogText,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 8),
                  HapticCheckboxListTile(
                    value: value,
                    contentPadding: EdgeInsets.zero,
                    title: Text(
                      t.noShowAgain,
                    ),
                    onChanged: (selected) {
                      noShow.value = selected!;
                    },
                  ),
                ],
              );
            },
          ),
          actions: [
            FilledButton(
              onPressed: () {
                setPatchesChangeWarning(noShow.value);
                Navigator.of(context).pop();
              },
              child: Text(t.okButton),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> reAssessPatchedApps() async {
    final List<PatchedApplication> patchedApps = getPatchedApps();

    // Remove apps that are not installed anymore.
    final List<PatchedApplication> toRemove =
        await getAppsToRemove(patchedApps);
    patchedApps.removeWhere((a) => toRemove.contains(a));

    // Determine all apps that are installed by mounting.
    final List<PatchedApplication> mountedApps = await getMountedApps();
    mountedApps.removeWhere(
      (app) => patchedApps
          .any((patchedApp) => patchedApp.packageName == app.packageName),
    );
    patchedApps.addAll(mountedApps);

    await setPatchedApps(patchedApps);

    // Delete the saved app if the file is not found.
    final PatchedApplication? lastPatchedApp = getLastPatchedApp();
    if (lastPatchedApp != null) {
      final File file = File(lastPatchedApp.patchedFilePath);
      if (!file.existsSync()) {
        deleteLastPatchedApp();
        _prefs.remove('lastPatchedApp');
      }
    }
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
    if (isVersionCompatibilityCheckEnabled() == true) {
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

  String exportSettings() {
    final Map<String, dynamic> settings = _prefs
        .getKeys()
        .fold<Map<String, dynamic>>({}, (Map<String, dynamic> map, String key) {
      map[key] = _prefs.get(key);
      return map;
    });
    return jsonEncode(settings);
  }

  Future<void> importSettings(String settings) async {
    final Map<String, dynamic> settingsMap = jsonDecode(settings);
    settingsMap.forEach((key, value) {
      if (value is bool) {
        _prefs.setBool(key, value);
      } else if (value is int) {
        _prefs.setInt(key, value);
      } else if (value is double) {
        _prefs.setDouble(key, value);
      } else if (value is String) {
        _prefs.setString(key, value);
      } else if (value is List<dynamic>) {
        _prefs.setStringList(
          key,
          value.map((a) => json.encode(a.toJson())).toList(),
        );
      }
    });
  }

  void resetAllOptions() {
    _prefs.getKeys().where((key) => key.startsWith('patchOption-')).forEach(
      (key) {
        _prefs.remove(key);
      },
    );
  }

  Future<void> resetLastSelectedPatches() async {
    final File selectedPatchesFile = File(storedPatchesFile);
    if (selectedPatchesFile.existsSync()) {
      selectedPatchesFile.deleteSync();
    }
  }
}
