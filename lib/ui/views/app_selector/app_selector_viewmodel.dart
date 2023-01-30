import 'dart:io';

import 'package:device_apps/device_apps.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();
  final List<ApplicationWithIcon> apps = [];
  bool noApps = false;
  int patchesCount(String packageName) {
    return _patcherAPI.getFilteredPatches(packageName).length;
  }

  Future<void> initialize() async {
    apps.addAll(
      await _patcherAPI
          .getFilteredInstalledApps(_managerAPI.areUniversalPatchesEnabled()),
    );
    apps.sort(
      (a, b) => _patcherAPI
          .getFilteredPatches(b.packageName)
          .length
          .compareTo(_patcherAPI.getFilteredPatches(a.packageName).length),
    );
    noApps = apps.isEmpty;
    notifyListeners();
  }

  Future<void> selectApp(ApplicationWithIcon application) async {
    locator<PatcherViewModel>().selectedApp = PatchedApplication(
      name: application.appName,
      packageName: application.packageName,
      originalPackageName: application.packageName,
      version: application.versionName!,
      apkFilePath: application.apkFilePath,
      icon: application.icon,
      patchDate: DateTime.now(),
    );
    locator<PatcherViewModel>().loadLastSelectedPatches();
  }

  Future<void> selectAppFromStorage(BuildContext context) async {
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['apk'],
      );
      if (result != null && result.files.single.path != null) {
        final File apkFile = File(result.files.single.path!);
        final List<String> pathSplit = result.files.single.path!.split('/');
        pathSplit.removeLast();
        final Directory filePickerCacheDir = Directory(pathSplit.join('/'));
        final Iterable<File> deletableFiles =
            (await filePickerCacheDir.list().toList()).whereType<File>();
        for (final file in deletableFiles) {
          if (file.path != apkFile.path && file.path.endsWith('.apk')) {
            file.delete();
          }
        }
        final ApplicationWithIcon? application =
            await DeviceApps.getAppFromStorage(
          apkFile.path,
          true,
        ) as ApplicationWithIcon?;
        if (application != null) {
          locator<PatcherViewModel>().selectedApp = PatchedApplication(
            name: application.appName,
            packageName: application.packageName,
            originalPackageName: application.packageName,
            version: application.versionName!,
            apkFilePath: result.files.single.path!,
            icon: application.icon,
            patchDate: DateTime.now(),
            isFromStorage: true,
          );
          locator<PatcherViewModel>().loadLastSelectedPatches();
        }
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      _toast.showBottom('appSelectorView.errorMessage');
    }
  }

  List<ApplicationWithIcon> getFilteredApps(String query) {
    return apps
        .where(
          (app) =>
              query.isEmpty ||
              query.length < 2 ||
              app.appName.toLowerCase().contains(query.toLowerCase()),
        )
        .toList();
  }
}
