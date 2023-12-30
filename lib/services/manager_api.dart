import 'dart:convert';
import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:injectable/injectable.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class ManagerAPI {
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
  final GithubAPI _githubAPI = locator<GithubAPI>();
  final RootAPI _rootAPI = RootAPI();
  final String patcherRepo = 'revanced-patcher';
  final String cliRepo = 'revanced-cli';
  late SharedPreferences _prefs;
  List<Patch> patches = [];
  List<Option> modifiedOptions = [];
  List<Option> options = [];
  Patch? selectedPatch;
  BuildContext? ctx;
  bool isRooted = false;
  bool suggestedAppVersionSelected = true;
  bool isDynamicThemeAvailable = false;
  String storedPatchesFile = '/selected-patches.json';
  String keystoreFile =
      '/sdcard/Android/data/app.revanced.manager.flutter/files/revanced-manager.keystore';
  String defaultKeystorePassword = 's3cur3p@ssw0rd';
  String defaultApiUrl = 'https://api.revanced.app/';
  String defaultRepoUrl = 'https://api.github.com';
  String defaultPatcherRepo = 'ReVanced/revanced-patcher';
  String defaultPatchesRepo = 'ReVanced/revanced-patches';
  String defaultIntegrationsRepo = 'ReVanced/revanced-integrations';
  String defaultCliRepo = 'ReVanced/revanced-cli';
  String defaultManagerRepo = 'ReVanced/revanced-manager';
  String? patchesVersion = '';
  String? integrationsVersion = '';

  bool isDefaultPatchesRepo() {
    return getPatchesRepo().toLowerCase() == 'revanced/revanced-patches';
  }

  bool isDefaultIntegrationsRepo() {
    return getIntegrationsRepo().toLowerCase() ==
        'revanced/revanced-integrations';
  }

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    isRooted = await _rootAPI.isRooted();
    isDynamicThemeAvailable =
        (await getSdkVersion()) >= 31; // ANDROID_12_SDK_VERSION = 31
    storedPatchesFile =
        (await getApplicationDocumentsDirectory()).path + storedPatchesFile;
  }

  Future<int> getSdkVersion() async {
    final AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt;
  }

  String getApiUrl() {
    return _prefs.getString('apiUrl') ?? defaultApiUrl;
  }

  Future<void> setApiUrl(String url) async {
    if (url.isEmpty || url == ' ') {
      url = defaultApiUrl;
    }
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

  String getPatchesDownloadURL() {
    return _prefs.getString('patchesDownloadURL') ?? '';
  }

  Future<void> setPatchesDownloadURL(String value) async {
    await _prefs.setString('patchesDownloadURL', value);
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

  bool getPatchesConsent() {
    return _prefs.getBool('patchesConsent') ?? false;
  }

  Future<void> setPatchesConsent(bool consent) async {
    await _prefs.setBool('patchesConsent', consent);
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

  bool isChangingToggleModified() {
    return _prefs.getBool('isChangingToggleModified') ?? false;
  }

  void setChangingToggleModified(bool value) {
    _prefs.setBool('isChangingToggleModified', value);
  }

  Future<void> setPatchesAutoUpdate(bool value) async {
    await _prefs.setBool('patchesAutoUpdate', value);
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

  String getIntegrationsDownloadURL() {
    return _prefs.getString('integrationsDownloadURL') ?? '';
  }

  Future<void> setIntegrationsDownloadURL(String value) async {
    await _prefs.setString('integrationsDownloadURL', value);
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
    if (patches.isNotEmpty) {
      return patches;
    }
    final File? patchBundleFile = await downloadPatches();
    final Directory appCache = await getTemporaryDirectory();
    Directory('${appCache.path}/cache').createSync();
    final Directory workDir =
        Directory('${appCache.path}/cache').createTempSync('tmp-');
    final Directory cacheDir = Directory('${workDir.path}/cache');
    cacheDir.createSync();
    if (patchBundleFile != null) {
      try {
        final String patchesJson = await PatcherAPI.patcherChannel.invokeMethod(
          'getPatches',
          {
            'patchBundleFilePath': patchBundleFile.path,
            'cacheDirPath': cacheDir.path,
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
    try {
      final String repoName = getPatchesRepo();
      final String currentVersion = await getCurrentPatchesVersion();
      final String url = getPatchesDownloadURL();
      return await _githubAPI.getPatchesReleaseFile(
        '.jar',
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

  Future<File?> downloadIntegrations() async {
    try {
      final String repoName = getIntegrationsRepo();
      final String currentVersion = await getCurrentIntegrationsVersion();
      final String url = getIntegrationsDownloadURL();
      return await _githubAPI.getPatchesReleaseFile(
        '.apk',
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
    return await _revancedAPI.getLatestReleaseFile(
      '.apk',
      defaultManagerRepo,
    );
  }

  Future<String?> getLatestPatchesReleaseTime() async {
    if (isDefaultPatchesRepo()) {
      return await _revancedAPI.getLatestReleaseTime(
        '.json',
        defaultPatchesRepo,
      );
    } else {
      final release =
          await _githubAPI.getLatestPatchesRelease(getPatchesRepo());
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

  Future<String?> getLatestIntegrationsVersion() async {
    if (isDefaultIntegrationsRepo()) {
      return await _revancedAPI.getLatestReleaseVersion(
        '.apk',
        defaultIntegrationsRepo,
      );
    } else {
      final release = await _githubAPI.getLatestRelease(getIntegrationsRepo());
      if (release != null) {
        return release['tag_name'];
      } else {
        return null;
      }
    }
  }

  Future<String?> getLatestPatchesVersion() async {
    if (isDefaultPatchesRepo()) {
      return await _revancedAPI.getLatestReleaseVersion(
        '.json',
        defaultPatchesRepo,
      );
    } else {
      final release =
          await _githubAPI.getLatestPatchesRelease(getPatchesRepo());
      if (release != null) {
        return release['tag_name'];
      } else {
        return null;
      }
    }
  }

  Future<String> getCurrentManagerVersion() async {
    final PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
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

  Future<String> getCurrentIntegrationsVersion() async {
    integrationsVersion = _prefs.getString('integrationsVersion') ?? '0.0.0';
    if (integrationsVersion == '0.0.0' || isPatchesAutoUpdate()) {
      final String newIntegrationsVersion =
          await getLatestIntegrationsVersion() ?? '0.0.0';
      if (integrationsVersion != newIntegrationsVersion &&
          newIntegrationsVersion != '0.0.0') {
        await setCurrentIntegrationsVersion(newIntegrationsVersion);
      }
    }
    return integrationsVersion!;
  }

  Future<void> setCurrentIntegrationsVersion(String version) async {
    await _prefs.setString('integrationsVersion', version);
    await setIntegrationsDownloadURL('');
    await downloadIntegrations();
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
      builder: (context) => WillPopScope(
        onWillPop: () async => false,
        child: AlertDialog(
          title: I18nText('warning'),
          content: ValueListenableBuilder(
            valueListenable: noShow,
            builder: (context, value, child) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  I18nText(
                    'patchItem.patchesChangeWarningDialogText',
                    child: const Text(
                      '',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  CheckboxListTile(
                    value: value,
                    contentPadding: EdgeInsets.zero,
                    title: I18nText(
                      'noShowAgain',
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
              child: I18nText('okButton'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> rePatchedSavedApps() async {
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
