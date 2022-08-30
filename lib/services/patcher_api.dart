import 'dart:convert';
import 'dart:io';
import 'package:app_installer/app_installer.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:share_extend/share_extend.dart';

@lazySingleton
class PatcherAPI {
  static const patcherChannel = MethodChannel(
    'app.revanced.manager/patcher',
  );
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final RootAPI _rootAPI = RootAPI();
  late Directory _tmpDir;
  List<Patch> _patches = [];
  File? _outFile;

  Future<void> initialize() async {
    await _loadPatches();
    Directory appCache = await getTemporaryDirectory();
    _tmpDir = Directory('${appCache.path}/patcher');
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
        File? patchJsonFile = await _managerAPI.downloadPatches('.json');
        if (patchJsonFile != null) {
          List<dynamic> list = json.decode(patchJsonFile.readAsStringSync());
          _patches = list.map((patch) => Patch.fromJson(patch)).toList();
        }
      }
    } on Exception {
      _patches = List.empty();
    }
  }

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps() async {
    List<ApplicationWithIcon> filteredApps = [];
    await _loadPatches();
    for (Patch patch in _patches) {
      for (Package package in patch.compatiblePackages) {
        try {
          if (!filteredApps.any((app) => app.packageName == package.name)) {
            ApplicationWithIcon? app =
                await DeviceApps.getApp(package.name, true)
                    as ApplicationWithIcon?;
            if (app != null) {
              filteredApps.add(app);
            }
          }
        } catch (e) {
          continue;
        }
      }
    }
    return filteredApps;
  }

  Future<List<Patch>> getFilteredPatches(String packageName) async {
    await _loadPatches();
    return _patches
        .where((patch) =>
            !patch.name.contains('settings') &&
            patch.compatiblePackages.any((pack) => pack.name == packageName))
        .toList();
  }

  Future<List<Patch>> getAppliedPatches(List<String> appliedPatches) async {
    await _loadPatches();
    return _patches
        .where((patch) => appliedPatches.contains(patch.name))
        .toList();
  }

  Future<void> runPatcher(
    String packageName,
    String originalFilePath,
    List<Patch> selectedPatches,
  ) async {
    bool mergeIntegrations = selectedPatches.any(
      (patch) => patch.dependencies.contains('integrations'),
    );
    bool resourcePatching = selectedPatches.any(
      (patch) => patch.dependencies.any((dep) => dep.contains('resource-')),
    );
    bool includeSettings = selectedPatches.any(
      (patch) => patch.dependencies.contains('settings'),
    );
    if (includeSettings) {
      try {
        Patch settingsPatch = _patches.firstWhere(
          (patch) =>
              patch.name.contains('settings') &&
              patch.compatiblePackages.any((pack) => pack.name == packageName),
        );
        selectedPatches.add(settingsPatch);
      } catch (e) {
        // ignore
      }
    }
    File? patchBundleFile = await _managerAPI.downloadPatches('.jar');
    File? integrationsFile;
    if (mergeIntegrations) {
      integrationsFile = await _managerAPI.downloadIntegrations('.apk');
    }
    if (patchBundleFile != null) {
      _tmpDir.createSync();
      Directory workDir = _tmpDir.createTempSync('tmp-');
      File inputFile = File('${workDir.path}/base.apk');
      File patchedFile = File('${workDir.path}/patched.apk');
      _outFile = File('${workDir.path}/out.apk');
      Directory cacheDir = Directory('${workDir.path}/cache');
      cacheDir.createSync();
      await patcherChannel.invokeMethod(
        'runPatcher',
        {
          'patchBundleFilePath': patchBundleFile.path,
          'originalFilePath': originalFilePath,
          'inputFilePath': inputFile.path,
          'patchedFilePath': patchedFile.path,
          'outFilePath': _outFile!.path,
          'integrationsPath': mergeIntegrations ? integrationsFile!.path : '',
          'selectedPatches': selectedPatches.map((p) => p.name).toList(),
          'cacheDirPath': cacheDir.path,
          'mergeIntegrations': mergeIntegrations,
          'resourcePatching': resourcePatching,
        },
      );
    }
  }

  Future<bool> installPatchedFile(PatchedApplication patchedApp) async {
    if (_outFile != null) {
      try {
        if (patchedApp.isRooted && !patchedApp.isFromStorage) {
          return _rootAPI.installApp(
            patchedApp.packageName,
            patchedApp.apkFilePath,
            _outFile!.path,
          );
        } else {
          await AppInstaller.installApk(_outFile!.path);
          return await DeviceApps.isAppInstalled(patchedApp.packageName);
        }
      } on Exception {
        return false;
      }
    }
    return false;
  }

  bool sharePatchedFile(String appName, String version) {
    if (_outFile != null) {
      String prefix = appName.toLowerCase().replaceAll(' ', '-');
      File share = _outFile!.renameSync('$prefix-revanced_v$version.apk');
      ShareExtend.share(share.path, 'file');
      return true;
    } else {
      return false;
    }
  }

  Future<bool> checkOldPatch(PatchedApplication patchedApp) async {
    if (patchedApp.isRooted) {
      return await _rootAPI.isAppInstalled(patchedApp.packageName);
    }
    return false;
  }

  Future<void> deleteOldPatch(PatchedApplication patchedApp) async {
    if (patchedApp.isRooted) {
      await _rootAPI.deleteApp(patchedApp.packageName, patchedApp.apkFilePath);
    }
  }
}
