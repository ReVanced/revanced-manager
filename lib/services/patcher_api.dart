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
import 'package:revanced_manager/utils/string.dart';
import 'package:share_extend/share_extend.dart';

@lazySingleton
class PatcherAPI {
  static const patcherChannel = MethodChannel(
    'app.revanced.manager/patcher',
  );
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final RootAPI _rootAPI = RootAPI();
  Directory? _tmpDir;
  Directory? _workDir;
  Directory? _cacheDir;
  File? _jarPatchBundleFile;
  File? _integrations;
  File? _inputFile;
  File? _patchedFile;
  File? _outFile;

  Future<void> initPatcher() async {
    Directory appCache = await getTemporaryDirectory();
    _tmpDir = Directory('${appCache.path}/patcher');
    _tmpDir!.createSync();
    _workDir = _tmpDir!.createTempSync('tmp-');
    _inputFile = File('${_workDir!.path}/base.apk');
    _patchedFile = File('${_workDir!.path}/patched.apk');
    _outFile = File('${_workDir!.path}/out.apk');
    _cacheDir = Directory('${_workDir!.path}/cache');
    _cacheDir!.createSync();
  }

  Future<bool> loadPatches() async {
    if (_tmpDir == null) {
      await initPatcher();
    }
    if (_jarPatchBundleFile == null) {
      _jarPatchBundleFile = await _managerAPI.downloadPatches('.jar');
      if (_jarPatchBundleFile != null) {
        try {
          await patcherChannel.invokeMethod<bool>(
            'loadPatches',
            {
              'jarPatchBundlePath': _jarPatchBundleFile!.path,
              'cacheDirPath': _cacheDir!.path,
            },
          );
        } on Exception {
          return false;
        }
      }
    }
    return _jarPatchBundleFile != null;
  }

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps() async {
    List<ApplicationWithIcon> filteredPackages = [];
    bool isLoaded = await loadPatches();
    if (isLoaded) {
      try {
        List<String>? patchesPackages = await patcherChannel
            .invokeListMethod<String>('getCompatiblePackages');
        if (patchesPackages != null) {
          for (String package in patchesPackages) {
            try {
              ApplicationWithIcon? app = await DeviceApps.getApp(package, true)
                  as ApplicationWithIcon?;
              if (app != null) {
                filteredPackages.add(app);
              }
            } catch (e) {
              continue;
            }
          }
        }
      } on Exception {
        return List.empty();
      }
    }
    return filteredPackages;
  }

  Future<List<Patch>> getFilteredPatches(
    PatchedApplication? selectedApp,
  ) async {
    List<Patch> filteredPatches = [];
    if (selectedApp != null) {
      bool isLoaded = await loadPatches();
      if (isLoaded) {
        try {
          var patches =
              await patcherChannel.invokeListMethod<Map<dynamic, dynamic>>(
            'getFilteredPatches',
            {
              'targetPackage': selectedApp.packageName,
              'targetVersion': selectedApp.version,
              'ignoreVersion': true,
            },
          );
          if (patches != null) {
            for (var patch in patches) {
              if (!filteredPatches
                  .any((element) => element.name == patch['name'])) {
                filteredPatches.add(
                  Patch(
                    name: patch['name'],
                    simpleName: (patch['name'] as String)
                        .replaceAll('-', ' ')
                        .split('-')
                        .join(' ')
                        .toTitleCase(),
                    version: patch['version'] ?? '?.?.?',
                    description: patch['description'] ?? 'N/A',
                    include: patch['include'] ?? true,
                  ),
                );
              }
            }
          }
        } on Exception {
          return List.empty();
        }
      }
    }
    return filteredPatches;
  }

  Future<List<Patch>> getAppliedPatches(
    PatchedApplication? selectedApp,
  ) async {
    List<Patch> appliedPatches = [];
    if (selectedApp != null) {
      bool isLoaded = await loadPatches();
      if (isLoaded) {
        try {
          var patches =
              await patcherChannel.invokeListMethod<Map<dynamic, dynamic>>(
            'getFilteredPatches',
            {
              'targetPackage': selectedApp.packageName,
              'targetVersion': selectedApp.version,
              'ignoreVersion': true,
            },
          );
          if (patches != null) {
            for (var patch in patches) {
              if (selectedApp.appliedPatches.contains(patch['name'])) {
                appliedPatches.add(
                  Patch(
                    name: patch['name'],
                    simpleName: (patch['name'] as String)
                        .replaceAll('-', ' ')
                        .split('-')
                        .join(' ')
                        .toTitleCase(),
                    version: patch['version'] ?? '?.?.?',
                    description: patch['description'] ?? 'N/A',
                    include: patch['include'] ?? true,
                  ),
                );
              }
            }
          }
        } on Exception {
          return List.empty();
        }
      }
    }
    return appliedPatches;
  }

  Future<void> mergeIntegrations(bool mergeIntegrations) async {
    if (mergeIntegrations) {
      _integrations = await _managerAPI.downloadIntegrations('.apk');
    } else {
      _integrations = null;
    }
  }

  Future<void> runPatcher(
    String originalFilePath,
    List<Patch> selectedPatches,
    bool mergeIntegrations,
    bool resourcePatching,
  ) async {
    await patcherChannel.invokeMethod(
      'runPatcher',
      {
        'originalFilePath': originalFilePath,
        'inputFilePath': _inputFile!.path,
        'patchedFilePath': _patchedFile!.path,
        'outFilePath': _outFile!.path,
        'integrationsPath': _integrations != null ? _integrations!.path : '',
        'selectedPatches': selectedPatches.map((p) => p.name).toList(),
        'cacheDirPath': _cacheDir!.path,
        'mergeIntegrations': mergeIntegrations,
        'resourcePatching': resourcePatching,
      },
    );
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

  void cleanPatcher() {
    if (_tmpDir != null) {
      _tmpDir!.deleteSync(recursive: true);
      _tmpDir = null;
    }
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
