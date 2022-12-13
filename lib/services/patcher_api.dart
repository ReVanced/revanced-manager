import 'dart:io';
import 'package:app_installer/app_installer.dart';
import 'package:collection/collection.dart';
import 'package:device_apps/device_apps.dart';
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
import 'package:cr_file_saver/file_saver.dart';

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
  File? _outFile;

  Future<void> initialize() async {
    await _loadPatches();
    Directory appCache = await getTemporaryDirectory();
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

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps() async {
    List<ApplicationWithIcon> filteredApps = [];
    bool? allAppsIncluded =
        _patches.any((patch) => patch.compatiblePackages.isEmpty);
    if (allAppsIncluded) {
      var allPackages = await DeviceApps.getInstalledApplications(
        includeAppIcons: true,
        onlyAppsWithLaunchIntent: true,
      );
      allPackages.forEach((pkg) async {
        if (!filteredApps.any((app) => app.packageName == pkg.packageName)) {
          var appInfo = await DeviceApps.getApp(
            pkg.packageName,
            true,
          ) as ApplicationWithIcon?;
          if (appInfo != null) {
            filteredApps.add(appInfo);
          }
        }
      });
    }
    for (Patch patch in _patches) {
      for (Package package in patch.compatiblePackages) {
        try {
          if (!filteredApps.any((app) => app.packageName == package.name)) {
            ApplicationWithIcon? app = await DeviceApps.getApp(
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
    List<Patch> filteredPatches = [];
    _patches.forEach((patch) {
      if (patch.compatiblePackages.isEmpty) {
        filteredPatches.add(patch);
      } else {
        if (!patch.name.contains('settings') &&
            patch.compatiblePackages.any((pack) => pack.name == packageName)) {
          filteredPatches.add(patch);
        }
      }
    });
    return filteredPatches;
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
      bool hasRootPermissions = await _rootAPI.hasRootPermissions();
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
    bool mergeIntegrations = await needsIntegrations(selectedPatches);
    bool includeSettings = await needsSettingsPatch(selectedPatches);
    if (includeSettings) {
      try {
        Patch? settingsPatch = _patches.firstWhereOrNull(
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
    File? patchBundleFile = await _managerAPI.downloadPatches();
    File? integrationsFile;
    if (mergeIntegrations) {
      integrationsFile = await _managerAPI.downloadIntegrations();
    }
    if (patchBundleFile != null) {
      _dataDir.createSync();
      _tmpDir.createSync();
      Directory workDir = _tmpDir.createTempSync('tmp-');
      File inputFile = File('${workDir.path}/base.apk');
      File patchedFile = File('${workDir.path}/patched.apk');
      _outFile = File('${workDir.path}/out.apk');
      Directory cacheDir = Directory('${workDir.path}/cache');
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
        print(e);
        throw await Sentry.captureException(e, stackTrace: s);
      }
    }
  }

  Future<bool> installPatchedFile(PatchedApplication patchedApp) async {
    if (_outFile != null) {
      try {
        if (patchedApp.isRooted) {
          bool hasRootPermissions = await _rootAPI.hasRootPermissions();
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
        String newName = _getFileName(appName, version);

        // This is temporary workaround to populate initial file name
        // ref: https://github.com/Cleveroad/cr_file_saver/issues/7
        int lastSeparator = _outFile!.path.lastIndexOf('/');
        String newSourcePath =
            _outFile!.path.substring(0, lastSeparator + 1) + newName;
        _outFile!.copySync(newSourcePath);

        CRFileSaver.saveFileWithDialog(SaveFileDialogParams(
            sourceFilePath: newSourcePath, destinationFileName: newName));
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  void sharePatchedFile(String appName, String version) {
    try {
      if (_outFile != null) {
        String newName = _getFileName(appName, version);
        int lastSeparator = _outFile!.path.lastIndexOf('/');
        String newPath =
            _outFile!.path.substring(0, lastSeparator + 1) + newName;
        File shareFile = _outFile!.copySync(newPath);
        ShareExtend.share(shareFile.path, 'file');
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  String _getFileName(String appName, String version) {
    String prefix = appName.toLowerCase().replaceAll(' ', '-');
    String newName = '$prefix-revanced_v$version.apk';
    return newName;
  }

  Future<void> sharePatcherLog(String logs) async {
    Directory appCache = await getTemporaryDirectory();
    Directory logDir = Directory('${appCache.path}/logs');
    logDir.createSync();
    String dateTime = DateTime.now()
        .toIso8601String()
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('T', '')
        .replaceAll('.', '');
    File log = File('${logDir.path}/revanced-manager_patcher_$dateTime.log');
    log.writeAsStringSync(logs);
    ShareExtend.share(log.path, 'file');
  }

  String getRecommendedVersion(String packageName) {
    Map<String, int> versions = {};
    for (Patch patch in _patches) {
      Package? package = patch.compatiblePackages.firstWhereOrNull(
        (pack) => pack.name == packageName,
      );
      if (package != null) {
        for (String version in package.versions) {
          versions.update(
            version,
            (value) => versions[version]! + 1,
            ifAbsent: () => 1,
          );
        }
      }
    }
    if (versions.isNotEmpty) {
      var entries = versions.entries.toList()
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
