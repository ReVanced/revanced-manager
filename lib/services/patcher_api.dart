import 'dart:io';

import 'package:collection/collection.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_file_dialog/flutter_file_dialog.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:share_plus/share_plus.dart';

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
  File? outFile;

  Future<void> initialize() async {
    await loadPatches();
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
    return _patches.where((patch) => patch.compatiblePackages.isEmpty).toList();
  }

  Future<void> loadPatches() async {
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
        _universalPatches.isNotEmpty && showUniversalPatches;
    if (allAppsIncluded) {
      final appList = await DeviceApps.getInstalledApplications(
        includeAppIcons: true,
        onlyAppsWithLaunchIntent: true,
      );

      for (final app in appList) {
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

  Future<void> runPatcher(
    String packageName,
    String apkFilePath,
    List<Patch> selectedPatches,
  ) async {
    final File? integrationsFile = await _managerAPI.downloadIntegrations();
    final Map<String, Map<String, dynamic>> options = {};
    for (final patch in selectedPatches) {
      if (patch.options.isNotEmpty) {
        final Map<String, dynamic> patchOptions = {};
        for (final option in patch.options) {
          final patchOption =
              _managerAPI.getPatchOption(packageName, patch.name, option.key);
          if (patchOption != null) {
            patchOptions[patchOption.key] = patchOption.value;
          }
        }
        options[patch.name] = patchOptions;
      }
    }

    if (integrationsFile != null) {
      _dataDir.createSync();
      _tmpDir.createSync();
      final Directory workDir = _tmpDir.createTempSync('tmp-');
      final File inputFile = File('${workDir.path}/base.apk');
      final File patchedFile = File('${workDir.path}/patched.apk');
      outFile = File('${workDir.path}/out.apk');
      final Directory cacheDir = Directory('${workDir.path}/cache');
      cacheDir.createSync();
      final String originalFilePath = apkFilePath;

      try {
        await patcherChannel.invokeMethod(
          'runPatcher',
          {
            'originalFilePath': originalFilePath,
            'inputFilePath': inputFile.path,
            'patchedFilePath': patchedFile.path,
            'outFilePath': outFile!.path,
            'integrationsPath': integrationsFile.path,
            'selectedPatches': selectedPatches.map((p) => p.name).toList(),
            'options': options,
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

  Future<int> installPatchedFile(
    BuildContext context,
    PatchedApplication patchedApp,
  ) async {
    if (outFile != null) {
      _managerAPI.ctx = context;
      try {
        if (patchedApp.isRooted) {
          final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
          final packageVersion = await DeviceApps.getApp(patchedApp.packageName)
              .then((app) => app?.versionName);
          if (!hasRootPermissions) {
            installErrorDialog(10);
          } else if (packageVersion == null) {
            installErrorDialog(1);
          } else if (packageVersion == patchedApp.version) {
            return await _rootAPI.installApp(
              patchedApp.packageName,
              patchedApp.apkFilePath,
              outFile!.path,
            )
                ? 0
                : 1;
          } else {
            installErrorDialog(0);
          }
        } else {
          if (await _rootAPI.hasRootPermissions()) {
            await _rootAPI.unmount(patchedApp.packageName);
          }
          if (context.mounted) {
            return await installApk(
              context,
              outFile!.path,
            );
          }
        }
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    }
    return 1;
  }

  Future<int> installApk(
    BuildContext context,
    String apkPath,
  ) async {
    try {
      final status = await patcherChannel.invokeMethod('installApk', {
        'apkPath': apkPath,
      });
      final int statusCode = status['status'];
      final String message = status['message'];
      final bool hasExtra =
          message.contains('INSTALL_FAILED_VERIFICATION_FAILURE') ||
              message.contains('INSTALL_FAILED_VERSION_DOWNGRADE');
      if (statusCode == 0 || (statusCode == 3 && !hasExtra)) {
        return statusCode;
      } else {
        _managerAPI.ctx = context;
        return await installErrorDialog(
          statusCode,
          status,
          hasExtra,
        );
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return 3;
    }
  }

  Future<int> installErrorDialog(
    int statusCode, [
    status,
    bool hasExtra = false,
  ]) async {
    final String statusValue = InstallStatus.byCode(hasExtra ? double.parse('$statusCode.1') : statusCode);
    bool cleanInstall = false;
    await showDialog(
      context: _managerAPI.ctx!,
      builder: (context) => AlertDialog(
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        title: I18nText('installErrorDialog.$statusValue'),
        content: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            I18nText(
              'installErrorDialog.${statusValue}_description',
              translationParams: statusCode == 2
                  ? {
                      'packageName': status['otherPackageName'],
                    }
                  : null,
            ),
          ],
        ),
        actions: (status == null)
            ? <Widget>[
                CustomMaterialButton(
                  label: I18nText('okButton'),
                  onPressed: () async {
                    Navigator.pop(context);
                  },
                ),
              ]
            : <Widget>[
                CustomMaterialButton(
                  isFilled: !(statusCode == 4 || statusCode == 5),
                  label: I18nText('cancelButton'),
                  onPressed: () {
                    Navigator.pop(context);
                  },
                ),
                if (statusCode == 4 || statusCode == 5)
                  CustomMaterialButton(
                    label: I18nText('okButton'),
                    onPressed: () async {
                      final int response = await patcherChannel.invokeMethod(
                        'uninstallApp',
                        {'packageName': status['packageName']},
                      );
                      if (response == 0 && context.mounted) {
                        cleanInstall = true;
                        Navigator.pop(context);
                      }
                    },
                  ),
              ],
      ),
    );
    return cleanInstall ? 10 : 1;
  }

  void exportPatchedFile(String appName, String version) {
    try {
      if (outFile != null) {
        final String newName = _getFileName(appName, version);
        FlutterFileDialog.saveFile(
        params: SaveFileDialogParams(
            sourceFilePath: outFile!.path,
            fileName: newName,
            mimeTypesFilter: ['application/vnd.android.package-archive'],
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
      if (outFile != null) {
        final String newName = _getFileName(appName, version);
        final int lastSeparator = outFile!.path.lastIndexOf('/');
        final String newPath =
            outFile!.path.substring(0, lastSeparator + 1) + newName;
        final File shareFile = outFile!.copySync(newPath);
        Share.shareXFiles([XFile(shareFile.path)]);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  String _getFileName(String appName, String version) {
    final String patchVersion = _managerAPI.patchesVersion!;
    final String prefix = appName.toLowerCase().replaceAll(' ', '-');
    final String newName = '$prefix-revanced_v$version-patches_v$patchVersion.apk';
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
    final String fileName = 'revanced-manager_patcher_$dateTime.txt';
    final File log = File('${logDir.path}/$fileName');
    log.writeAsStringSync(logs);
    FlutterFileDialog.saveFile(
      params: SaveFileDialogParams(
        sourceFilePath: log.path,
        fileName: fileName,
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

enum InstallStatus {
  statusFailureTimeout(8),
  statusFailureStorage(6),
  statusFailureInvalid(4),
  statusFailureIncompatible(7),
  statusFailureConflict(5),
  statusFailureBlocked(2),
  installFailedVerificationFailure(3.1),
  installFailedVersionDowngrade(4.1),

  mountVersionMismatch(0),
  mountMissingInstallation(1),
  mountNoRoot(10);

  const InstallStatus(this.statusCode);
  final double statusCode;

  static String byCode(num code) {
    return InstallStatus.values
        .firstWhere((flag) => flag.statusCode == code)
        .status;
  }
}

extension InstallStatusExtension on InstallStatus {
  String get status {
    switch (this) {
      case InstallStatus.statusFailureTimeout:
        return 'status_failure_timeout';
      case InstallStatus.statusFailureStorage:
        return 'status_failure_storage';
      case InstallStatus.statusFailureInvalid:
        return 'status_failure_invalid';
      case InstallStatus.statusFailureIncompatible:
        return 'status_failure_incompatible';
      case InstallStatus.statusFailureConflict:
        return 'status_failure_conflict';
      case InstallStatus.statusFailureBlocked:
        return 'status_failure_blocked';
      case InstallStatus.installFailedVerificationFailure:
        return 'install_failed_verification_failure';
      case InstallStatus.installFailedVersionDowngrade:
        return 'install_failed_version_downgrade';
      case InstallStatus.mountVersionMismatch:
        return 'mount_version_mismatch';
      case InstallStatus.mountMissingInstallation:
        return 'mount_missing_installation';
      case InstallStatus.mountNoRoot:
        return 'mount_no_root';
      default:
        return 'unknownStatus';
    }
  }
}
