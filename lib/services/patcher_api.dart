import 'dart:io';
import 'package:app_installer/app_installer.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/utils/string.dart';
import 'package:share_extend/share_extend.dart';

@lazySingleton
class PatcherAPI {
  static const patcherChannel = MethodChannel(
    'app.revanced.manager/patcher',
  );
  static const installerChannel = MethodChannel(
    'app.revanced.manager/installer',
  );
  final GithubAPI githubAPI = GithubAPI();
  final RootAPI rootAPI = RootAPI();
  final List<ApplicationWithIcon> _filteredPackages = [];
  final Map<String, List<Patch>> _filteredPatches = <String, List<Patch>>{};
  Directory? _tmpDir;
  Directory? _workDir;
  Directory? _cacheDir;
  File? _patchBundleFile;
  File? _integrations;
  File? _inputFile;
  File? _patchedFile;
  File? _outFile;

  Future<dynamic> handlePlatformChannelMethods() async {
    installerChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'updateProgress':
          if (call.arguments != null) {
            locator<InstallerViewModel>().updateProgress(call.arguments);
          }
          break;
        case 'updateLog':
          if (call.arguments != null) {
            locator<InstallerViewModel>().updateLog(call.arguments);
          }
          break;
      }
    });
  }

  Future<bool?> loadPatches() async {
    if (_patchBundleFile == null) {
      String? dexFileUrl =
          await githubAPI.latestRelease('revanced', 'revanced-patches');
      if (dexFileUrl != null && dexFileUrl.isNotEmpty) {
        try {
          _patchBundleFile =
              await DefaultCacheManager().getSingleFile(dexFileUrl);
          return await patcherChannel.invokeMethod<bool>(
            'loadPatches',
            {
              'pathBundlesPaths': <String>[_patchBundleFile!.absolute.path],
            },
          );
        } on Exception {
          _patchBundleFile = null;
          return false;
        }
      }
      return false;
    }
    return true;
  }

  Future<List<ApplicationWithIcon>> getFilteredInstalledApps() async {
    if (_patchBundleFile != null && _filteredPackages.isEmpty) {
      try {
        List<String>? patchesPackages = await patcherChannel
            .invokeListMethod<String>('getCompatiblePackages');
        if (patchesPackages != null) {
          for (String package in patchesPackages) {
            try {
              ApplicationWithIcon? app = await DeviceApps.getApp(package, true)
                  as ApplicationWithIcon?;
              if (app != null) {
                _filteredPackages.add(app);
              }
            } catch (e) {
              continue;
            }
          }
        }
      } on Exception {
        _filteredPackages.clear();
        return List.empty();
      }
    }
    return _filteredPackages;
  }

  Future<List<Patch>?> getFilteredPatches(
    PatchedApplication? selectedApp,
  ) async {
    if (_patchBundleFile != null && selectedApp != null) {
      if (_filteredPatches[selectedApp.packageName] == null ||
          _filteredPatches[selectedApp.packageName]!.isEmpty) {
        _filteredPatches[selectedApp.packageName] = [];
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
              if (!_filteredPatches[selectedApp.packageName]!
                  .any((element) => element.name == patch['name'])) {
                _filteredPatches[selectedApp.packageName]!.add(
                  Patch(
                    name: patch['name'],
                    simpleName: (patch['name'] as String)
                        .replaceAll('-', ' ')
                        .split('-')
                        .join(' ')
                        .toTitleCase(),
                    version: patch['version'] ?? '?.?.?',
                    description: patch['description'] ?? 'N/A',
                  ),
                );
              }
            }
          }
        } on Exception {
          _filteredPatches[selectedApp.packageName]!.clear();
          return List.empty();
        }
      }
    } else {
      return List.empty();
    }
    return _filteredPatches[selectedApp.packageName];
  }

  Future<File?> downloadIntegrations() async {
    String? apkFileUrl =
        await githubAPI.latestRelease('revanced', 'revanced-integrations');
    if (apkFileUrl != null && apkFileUrl.isNotEmpty) {
      return await DefaultCacheManager().getSingleFile(apkFileUrl);
    }
    return null;
  }

  Future<void> initPatcher(bool mergeIntegrations) async {
    if (mergeIntegrations) {
      _integrations = await downloadIntegrations();
    } else {
      _integrations = File('');
    }
    _tmpDir = await getTemporaryDirectory();
    _workDir = _tmpDir!.createTempSync('tmp-');
    _inputFile = File('${_workDir!.path}/base.apk');
    _patchedFile = File('${_workDir!.path}/patched.apk');
    _outFile = File('${_workDir!.path}/out.apk');
    _cacheDir = Directory('${_workDir!.path}/cache');
    _cacheDir!.createSync();
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
        'integrationsPath': _integrations!.path,
        'selectedPatches': selectedPatches.map((e) => e.name).toList(),
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
          return rootAPI.installApp(
            patchedApp.packageName,
            patchedApp.apkFilePath,
            _outFile!.path,
          );
        } else {
          await AppInstaller.installApk(_outFile!.path);
          return true;
        }
      } on Exception {
        return false;
      }
    }
    return false;
  }

  void cleanPatcher() {
    if (_workDir != null) {
      _workDir!.deleteSync(recursive: true);
    }
  }

  bool sharePatchedFile(String appName, String version) {
    if (_outFile != null) {
      String path = _tmpDir!.path;
      String prefix = appName.toLowerCase().replaceAll(' ', '-');
      String sharePath = '$path/$prefix-revanced_v$version.apk';
      File share = _outFile!.copySync(sharePath);
      ShareExtend.share(share.path, 'file');
      return true;
    } else {
      return false;
    }
  }

  Future<bool> checkOldPatch(PatchedApplication patchedApp) async {
    if (patchedApp.isRooted) {
      return await rootAPI.checkApp(patchedApp.packageName);
    }
    return false;
  }

  Future<void> deleteOldPatch(PatchedApplication patchedApp) async {
    if (patchedApp.isRooted) {
      await rootAPI.deleteApp(patchedApp.packageName, patchedApp.apkFilePath);
    }
  }
}
