import 'dart:io';

import 'package:collection/collection.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_file_dialog/flutter_file_dialog.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:share_plus/share_plus.dart';

@lazySingleton
class PatcherAPI {
  static const patcherChannel = MethodChannel('app.revanced.manager.flutter/patcher');
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final RootAPI _rootAPI = RootAPI();
  late Directory _dataDir;
  late Directory _tmpDir;
  late File _keyStoreFile;
  List<Patch> _patches = [];
  List<Patch> _universalPatches = [];
  Set<String> _compatiblePackages = {};
  Map filteredPatches = <String, List<Patch>>{};
  File? outFile;

  Future<void> initialize() async {
    await loadPatches();
    final Directory appCache = await getApplicationSupportDirectory();
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

  Set<String> getCompatiblePackages() {
    final Set<String> compatiblePackages = {};
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
        _universalPatches = getUniversalPatches();
        _compatiblePackages = getCompatiblePackages();
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }

      _patches = List.empty();
    }
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
    bool isFromStorage,
  ) async {
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

    _dataDir.createSync();
    _tmpDir.createSync();
    final Directory workDir = await _tmpDir.createTemp('tmp-');

    final File inApkFile = File('${workDir.path}/in.apk');
    await File(apkFilePath).copy(inApkFile.path);

    if (isFromStorage) {
      // The selected apk was copied to cacheDir by the file picker, so it's not needed anymore.
      // rename() can't be used here, as Android system also counts the size of files moved out from cacheDir
      // as part of the app's cache size.
      File(apkFilePath).delete();
    }

    outFile = File('${workDir.path}/out.apk');

    final Directory tmpDir =
        Directory('${workDir.path}/revanced-temporary-files');

    try {
      await patcherChannel.invokeMethod(
        'runPatcher',
        {
          'inFilePath': inApkFile.path,
          'outFilePath': outFile!.path,
          'selectedPatches': selectedPatches.map((p) => p.name).toList(),
          'options': options,
          'tmpDirPath': tmpDir.path,
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
    if (patchedApp.patchedFilePath != '') {
      _managerAPI.ctx = context;
      try {
        if (patchedApp.isRooted) {
          final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
          final packageVersion = await DeviceApps.getApp(patchedApp.packageName)
              .then((app) => app?.versionName);
          if (!hasRootPermissions) {
            installErrorDialog(1);
          } else if (packageVersion == null) {
            installErrorDialog(1.2);
          } else if (packageVersion == patchedApp.version) {
            return await _rootAPI.install(
              patchedApp.packageName,
              patchedApp.apkFilePath,
              patchedApp.patchedFilePath,
            )
                ? 0
                : 1;
          } else {
            installErrorDialog(1.1);
          }
        } else {
          if (await _rootAPI.hasRootPermissions()) {
            await _rootAPI.uninstall(patchedApp.packageName);
          }
          if (context.mounted) {
            return await installApk(
              context,
              patchedApp.patchedFilePath,
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
    num statusCode, [
    status,
    bool hasExtra = false,
  ]) async {
    final String statusValue = InstallStatus.byCode(
      hasExtra ? double.parse('$statusCode.1') : statusCode,
    );
    bool cleanInstall = false;
    final bool isFixable = statusCode == 4 || statusCode == 5;

    var description = t['installErrorDialog.${statusValue}_description'];
    if (statusCode == 2) {
      description = description(
        packageName: statusCode == 2
            ? {
                'packageName': status['otherPackageName'],
              }
            : null,
      );
    }

    await showDialog(
      context: _managerAPI.ctx!,
      builder: (context) => AlertDialog(
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        title: Text(t['installErrorDialog.$statusValue']),
        content: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(description),
          ],
        ),
        actions: (status == null)
            ? <Widget>[
                FilledButton(
                  onPressed: () async {
                    Navigator.pop(context);
                  },
                  child: Text(t.okButton),
                ),
              ]
            : <Widget>[
                if (!isFixable)
                  FilledButton(
                    onPressed: () {
                      Navigator.pop(context);
                    },
                    child: Text(t.cancelButton),
                  )
                else
                  TextButton(
                    onPressed: () {
                      Navigator.pop(context);
                    },
                    child: Text(t.cancelButton),
                  ),
                if (isFixable)
                  FilledButton(
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
                    child: Text(t.okButton),
                  ),
              ],
      ),
    );
    return cleanInstall ? 10 : 1;
  }

  void exportPatchedFile(PatchedApplication app) {
    try {
      if (outFile != null) {
        final String newName = _getFileName(app.name, app.version);
        FlutterFileDialog.saveFile(
          params: SaveFileDialogParams(
            sourceFilePath: app.patchedFilePath,
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

  void sharePatchedFile(PatchedApplication app) {
    try {
      if (outFile != null) {
        final String newName = _getFileName(app.name, app.version);
        final int lastSeparator = app.patchedFilePath.lastIndexOf('/');
        final String newPath =
            app.patchedFilePath.substring(0, lastSeparator + 1) + newName;
        final File shareFile = File(app.patchedFilePath).copySync(newPath);
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
    final String newName =
        '$prefix-revanced_v$version-patches_$patchVersion.apk';
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
  mountNoRoot(1),
  mountVersionMismatch(1.1),
  mountMissingInstallation(1.2),
  statusFailureBlocked(2),
  installFailedVerificationFailure(3.1),
  statusFailureInvalid(4),
  installFailedVersionDowngrade(4.1),
  statusFailureConflict(5),
  statusFailureStorage(6),
  statusFailureIncompatible(7),
  statusFailureTimeout(8);

  const InstallStatus(this.statusCode);

  final double statusCode;

  static String byCode(num code) {
    try {
      return InstallStatus.values
          .firstWhere((flag) => flag.statusCode == code)
          .status;
    } catch (e) {
      return 'status_unknown';
    }
  }
}

extension InstallStatusExtension on InstallStatus {
  String get status {
    switch (this) {
      case InstallStatus.mountNoRoot:
        return 'mount_no_root';
      case InstallStatus.mountVersionMismatch:
        return 'mount_version_mismatch';
      case InstallStatus.mountMissingInstallation:
        return 'mount_missing_installation';
      case InstallStatus.statusFailureBlocked:
        return 'status_failure_blocked';
      case InstallStatus.installFailedVerificationFailure:
        return 'install_failed_verification_failure';
      case InstallStatus.statusFailureInvalid:
        return 'status_failure_invalid';
      case InstallStatus.installFailedVersionDowngrade:
        return 'install_failed_version_downgrade';
      case InstallStatus.statusFailureConflict:
        return 'status_failure_conflict';
      case InstallStatus.statusFailureStorage:
        return 'status_failure_storage';
      case InstallStatus.statusFailureIncompatible:
        return 'status_failure_incompatible';
      case InstallStatus.statusFailureTimeout:
        return 'status_failure_timeout';
    }
  }
}
