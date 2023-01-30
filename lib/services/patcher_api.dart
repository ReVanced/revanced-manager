import 'dart:io';

import 'package:app_installer/app_installer.dart';
import 'package:collection/collection.dart';
import 'package:cr_file_saver/file_saver.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:share_extend/share_extend.dart';

@lazySingleton
class PatcherAPI {
  static const patcherChannel =
      MethodChannel('app.revanced.manager.flutter/patcher');
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final RootAPI _rootAPI = RootAPI();
  late Directory _dataDir;
  late Directory _tmpDir;
  late File _keyStoreFile;
  List<Patch> _patches = [];
  Map filteredPatches = <String, List<Patch>>{};
  File? _outFile;

  Future<void> initialize() async {
    await _loadPatches();
    final Directory appCache = await getTemporaryDirectory();
    _dataDir = await getExternalStorageDirectory() ?? appCache;
    _tmpDir = Directory('${appCache.path}/patcher');
    _keyStoreFile = File('${_dataDir.path}/revanced-manager.keystore');
    cleanPatcher();
  }

  void cleanPatcher() {
    if (_tmpDir.existsSync()) {
      _tmpDir.deleteSync(recursive: true);
    }
  }

  Future<void> _loadPatches() async {
    try {
      if (_patches.isEmpty) {
        _patches = await _managerAPI.getPatches();
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      _patches = List.empty();
    }
  }

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps(
    bool showUniversalPatches,
  ) async {
    final List<ApplicationWithIcon> filteredApps = [];
    final bool allAppsIncluded =
        _patches.any((patch) => patch.compatiblePackages.isEmpty) &&
            showUniversalPatches;
    if (allAppsIncluded) {
      final allPackages = await DeviceApps.getInstalledApplications(
        includeAppIcons: true,
        onlyAppsWithLaunchIntent: true,
      );
      for (final pkg in allPackages) {
        if (!filteredApps.any((app) => app.packageName == pkg.packageName)) {
          final appInfo = await DeviceApps.getApp(
            pkg.packageName,
            true,
          ) as ApplicationWithIcon?;
          if (appInfo != null) {
            filteredApps.add(appInfo);
          }
        }
      }
    }
    for (final Patch patch in _patches) {
      for (final Package package in patch.compatiblePackages) {
        try {
          if (!filteredApps.any((app) => app.packageName == package.name)) {
            final ApplicationWithIcon? app = await DeviceApps.getApp(
              package.name,
              true,
            ) as ApplicationWithIcon?;
            if (app != null) {
              filteredApps.add(app);
            }
          }
        } on Exception catch (e, s) {
          await Sentry.captureException(e, stackTrace: s);
          continue;
        }
      }
    }
    return filteredApps;
  }

  List<Patch> getFilteredPatches(String packageName) {
    if (!filteredPatches.keys.contains(packageName)) {
      final List<Patch> patches = _patches
          .where(
            (patch) =>
                patch.compatiblePackages.isEmpty ||
                !patch.name.contains('settings') &&
                    patch.compatiblePackages
                        .any((pack) => pack.name == packageName),
          )
          .toList();
      filteredPatches[packageName] = patches;
    }
    return filteredPatches[packageName];
  }

  Future<List<Patch>> getAppliedPatches(List<String> appliedPatches) async {
    return _patches
        .where((patch) => appliedPatches.contains(patch.name))
        .toList();
  }

  bool dependencyNeedsIntegrations(String name) {
    return name.contains('integrations') ||
        _patches.any(
          (patch) =>
              patch.name == name &&
              (patch.dependencies.any(
                (dep) => dependencyNeedsIntegrations(dep),
              )),
        );
  }

  Future<bool> needsIntegrations(List<Patch> selectedPatches) async {
    return selectedPatches.any(
      (patch) => patch.dependencies.any(
        (dep) => dependencyNeedsIntegrations(dep),
      ),
    );
  }

  Future<bool> needsResourcePatching(List<Patch> selectedPatches) async {
    return selectedPatches.any(
      (patch) => patch.dependencies.any(
        (dep) => dep.contains('resource-'),
      ),
    );
  }

  Future<bool> needsSettingsPatch(List<Patch> selectedPatches) async {
    return selectedPatches.any(
      (patch) => patch.dependencies.any(
        (dep) => dep.contains('settings'),
      ),
    );
  }

  Future<String> getOriginalFilePath(
    String packageName,
    String originalFilePath,
  ) async {
    try {
      final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
      if (hasRootPermissions) {
        originalFilePath = await _rootAPI.getOriginalFilePath(
          packageName,
          originalFilePath,
        );
      }
      return originalFilePath;
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return originalFilePath;
    }
  }

  Future<void> runPatcher(
    String packageName,
    String originalFilePath,
    List<Patch> selectedPatches,
  ) async {
    final bool mergeIntegrations = await needsIntegrations(selectedPatches);
    final bool includeSettings = await needsSettingsPatch(selectedPatches);
    if (includeSettings) {
      try {
        final Patch? settingsPatch = _patches.firstWhereOrNull(
          (patch) =>
              patch.name.contains('settings') &&
              patch.compatiblePackages.any((pack) => pack.name == packageName),
        );
        if (settingsPatch != null) {
          selectedPatches.add(settingsPatch);
        }
      } on Exception catch (e, s) {
        await Sentry.captureException(e, stackTrace: s);
        // ignore
      }
    }
    final File? patchBundleFile = await _managerAPI.downloadPatches();
    File? integrationsFile;
    if (mergeIntegrations) {
      integrationsFile = await _managerAPI.downloadIntegrations();
    }
    if (patchBundleFile != null) {
      _dataDir.createSync();
      _tmpDir.createSync();
      final Directory workDir = _tmpDir.createTempSync('tmp-');
      final File inputFile = File('${workDir.path}/base.apk');
      final File patchedFile = File('${workDir.path}/patched.apk');
      _outFile = File('${workDir.path}/out.apk');
      final Directory cacheDir = Directory('${workDir.path}/cache');
      cacheDir.createSync();
      try {
        await patcherChannel.invokeMethod(
          'runPatcher',
          {
            'patchBundleFilePath': patchBundleFile.path,
            'originalFilePath': await getOriginalFilePath(
              packageName,
              originalFilePath,
            ),
            'inputFilePath': inputFile.path,
            'patchedFilePath': patchedFile.path,
            'outFilePath': _outFile!.path,
            'integrationsPath': mergeIntegrations ? integrationsFile!.path : '',
            'selectedPatches': selectedPatches.map((p) => p.name).toList(),
            'cacheDirPath': cacheDir.path,
            'mergeIntegrations': mergeIntegrations,
            'keyStoreFilePath': _keyStoreFile.path,
          },
        );
      } on Exception catch (e, s) {
        if (kDebugMode) {
          print(e);
        }
        throw await Sentry.captureException(e, stackTrace: s);
      }
    }
  }

  Future<bool> installPatchedFile(PatchedApplication patchedApp) async {
    if (_outFile != null) {
      try {
        if (patchedApp.isRooted) {
          final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
          if (hasRootPermissions) {
            return _rootAPI.installApp(
              patchedApp.packageName,
              patchedApp.apkFilePath,
              _outFile!.path,
            );
          }
        } else {
          await AppInstaller.installApk(_outFile!.path);
          return await DeviceApps.isAppInstalled(patchedApp.packageName);
        }
      } on Exception catch (e, s) {
        await Sentry.captureException(e, stackTrace: s);
        return false;
      }
    }
    return false;
  }

  void exportPatchedFile(String appName, String version) {
    try {
      if (_outFile != null) {
        final String newName = _getFileName(appName, version);
        CRFileSaver.saveFileWithDialog(SaveFileDialogParams(
            sourceFilePath: _outFile!.path, destinationFileName: newName,),);
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  void sharePatchedFile(String appName, String version) {
    try {
      if (_outFile != null) {
        final String newName = _getFileName(appName, version);
        final int lastSeparator = _outFile!.path.lastIndexOf('/');
        final String newPath =
            _outFile!.path.substring(0, lastSeparator + 1) + newName;
        final File shareFile = _outFile!.copySync(newPath);
        ShareExtend.share(shareFile.path, 'file');
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  String _getFileName(String appName, String version) {
    final String prefix = appName.toLowerCase().replaceAll(' ', '-');
    final String newName = '$prefix-revanced_v$version.apk';
    return newName;
  }

  Future<void> sharePatcherLog(String logs) async {
    final Directory appCache = await getTemporaryDirectory();
    final Directory logDir = Directory('${appCache.path}/logs');
    logDir.createSync();
    final String dateTime = DateTime.now()
        .toIso8601String()
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('T', '')
        .replaceAll('.', '');
    final File log =
        File('${logDir.path}/revanced-manager_patcher_$dateTime.log');
    log.writeAsStringSync(logs);
    ShareExtend.share(log.path, 'file');
  }

  String getRecommendedVersion(String packageName) {
    final Map<String, int> versions = {};
    for (final Patch patch in _patches) {
      final Package? package = patch.compatiblePackages.firstWhereOrNull(
        (pack) => pack.name == packageName,
      );
      if (package != null) {
        for (final String version in package.versions) {
          versions.update(
            version,
            (value) => versions[version]! + 1,
            ifAbsent: () => 1,
          );
        }
      }
    }
    if (versions.isNotEmpty) {
      final entries = versions.entries.toList()
        ..sort((a, b) => a.value.compareTo(b.value));
      versions
        ..clear()
        ..addEntries(entries);
      versions.removeWhere((key, value) => value != versions.values.last);
      return (versions.keys.toList()..sort()).last;
    }
    return '';
  }
}
