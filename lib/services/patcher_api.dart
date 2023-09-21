import 'dart:io';

import 'package:collection/collection.dart';
import 'package:cr_file_saver/file_saver.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:install_plugin/install_plugin.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/root_api.dart';
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
  List<Patch> _universalPatches = [];
  List<String> _compatiblePackages = [];
  Map filteredPatches = <String, List<Patch>>{};
  File? _outFile;

  Future<void> initialize() async {
    await _loadPatches();
    await _managerAPI.downloadIntegrations();
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

  List<String> getCompatiblePackages() {
    final List<String> compatiblePackages = [];
    for (final Patch patch in _patches) {
      for (final Package package in patch.compatiblePackages) {
        if (!compatiblePackages.contains(package.name)) {
          compatiblePackages.add(package.name);
        }
      }
    }
    return compatiblePackages;
  }

  List<Patch> getUniversalPatches() {
    return _patches
        .where((patch) => patch.compatiblePackages.isEmpty)
        .toList();
  }

  Future<void> _loadPatches() async {
    try {
      if (_patches.isEmpty) {
        _patches = await _managerAPI.getPatches();
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _patches = List.empty();
    }

    _compatiblePackages = getCompatiblePackages();
    _universalPatches = getUniversalPatches();
  }

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps(
    bool showUniversalPatches,
  ) async {
    final List<ApplicationWithIcon> filteredApps = [];
    final bool allAppsIncluded =
        _universalPatches.isNotEmpty &&
            showUniversalPatches;
    if (allAppsIncluded) {
      final appList = await DeviceApps.getInstalledApplications(
        includeAppIcons: true,
        onlyAppsWithLaunchIntent: true,
      );

      for(final app in appList) {
        filteredApps.add(app as ApplicationWithIcon);
      }
    }
    for (final packageName in _compatiblePackages) {
      try {
        if (!filteredApps.any((app) => app.packageName == packageName)) {
          final ApplicationWithIcon? app = await DeviceApps.getApp(
            packageName,
            true,
          ) as ApplicationWithIcon?;
          if (app != null) {
            filteredApps.add(app);
          }
        }
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    }
    return filteredApps;
  }

  List<Patch> getFilteredPatches(String packageName) {
    if (!_compatiblePackages.contains(packageName)) {
      return _universalPatches;
    }

    final List<Patch> patches = _patches
        .where(
          (patch) =>
              patch.compatiblePackages.isEmpty ||
              !patch.name.contains('settings') &&
                  patch.compatiblePackages
                      .any((pack) => pack.name == packageName),
        )
        .toList();
    if (!_managerAPI.areUniversalPatchesEnabled()) {
      filteredPatches[packageName] = patches
          .where((patch) => patch.compatiblePackages.isNotEmpty)
          .toList();
    } else {
      filteredPatches[packageName] = patches;
    }
    return filteredPatches[packageName];
  }

  Future<List<Patch>> getAppliedPatches(
    List<String> appliedPatches,
  ) async {
    return _patches
        .where((patch) => appliedPatches.contains(patch.name))
        .toList();
  }

  Future<bool> needsResourcePatching(
    List<Patch> selectedPatches,
  ) async {
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

  Future<void> runPatcher(
    String packageName,
    String apkFilePath,
    List<Patch> selectedPatches,
  ) async {
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
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    }
    final File? patchBundleFile = await _managerAPI.downloadPatches();
    final File? integrationsFile = await _managerAPI.downloadIntegrations();
    if (patchBundleFile != null) {
      _dataDir.createSync();
      _tmpDir.createSync();
      final Directory workDir = _tmpDir.createTempSync('tmp-');
      final File inputFile = File('${workDir.path}/base.apk');
      final File patchedFile = File('${workDir.path}/patched.apk');
      _outFile = File('${workDir.path}/out.apk');
      final Directory cacheDir = Directory('${workDir.path}/cache');
      cacheDir.createSync();
      final String originalFilePath = apkFilePath;
      try {
        await patcherChannel.invokeMethod(
          'runPatcher',
          {
            'patchBundleFilePath': patchBundleFile.path,
            'originalFilePath': originalFilePath,
            'inputFilePath': inputFile.path,
            'patchedFilePath': patchedFile.path,
            'outFilePath': _outFile!.path,
            'integrationsPath': integrationsFile!.path,
            'selectedPatches': selectedPatches.map((p) => p.name).toList(),
            'cacheDirPath': cacheDir.path,
            'keyStoreFilePath': _keyStoreFile.path,
            'keystorePassword': _managerAPI.getKeystorePassword(),
          },
        );
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    }
  }

  Future<void> stopPatcher() async {
    try {
      await patcherChannel.invokeMethod('stopPatcher');
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
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
          final install = await InstallPlugin.installApk(_outFile!.path);
          return install['isSuccess'];
        }
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
        return false;
      }
    }
    return false;
  }

  void exportPatchedFile(String appName, String version) {
    try {
      if (_outFile != null) {
        final String newName = _getFileName(appName, version);
        CRFileSaver.saveFileWithDialog(
          SaveFileDialogParams(
            sourceFilePath: _outFile!.path,
            destinationFileName: newName,
          ),
        );
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
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
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  String _getFileName(String appName, String version) {
    final String prefix = appName.toLowerCase().replaceAll(' ', '-');
    final String newName = '$prefix-revanced_v$version.apk';
    return newName;
  }

  Future<void> exportPatcherLog(String logs) async {
    final Directory appCache = await getTemporaryDirectory();
    final Directory logDir = Directory('${appCache.path}/logs');
    logDir.createSync();
    final String dateTime = DateTime.now()
        .toIso8601String()
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('T', '')
        .replaceAll('.', '');
    final String fileName = 'revanced-manager_patcher_$dateTime.log';
    final File log = File('${logDir.path}/$fileName');
    log.writeAsStringSync(logs);
    CRFileSaver.saveFileWithDialog(
      SaveFileDialogParams(
        sourceFilePath: log.path,
        destinationFileName: fileName,
      ),
    );
  }

  String getSuggestedVersion(String packageName) {
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
