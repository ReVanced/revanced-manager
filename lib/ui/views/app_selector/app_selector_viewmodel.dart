import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final List<ApplicationWithIcon> apps = [];
  bool noApps = false;

  Future<void> initialize() async {
    apps.addAll(await _patcherAPI.getFilteredInstalledApps());
    apps.sort((a, b) => a.appName.compareTo(b.appName));
    noApps = apps.isEmpty;
    notifyListeners();
  }

  void selectApp(ApplicationWithIcon application) async {
    locator<PatcherViewModel>().selectedApp = PatchedApplication(
      name: application.appName,
      packageName: application.packageName,
      version: application.versionName!,
      apkFilePath: application.apkFilePath,
      icon: application.icon,
      patchDate: DateTime.now(),
      isRooted: false,
    );
    locator<PatcherViewModel>().selectedPatches.clear();
    locator<PatcherViewModel>().notifyListeners();
  }

  Future<void> selectAppFromStorage(BuildContext context) async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['apk'],
      );
      if (result != null && result.files.single.path != null) {
        File apkFile = File(result.files.single.path!);
        ApplicationWithIcon? application =
            await DeviceApps.getAppFromStorage(apkFile.path, true)
                as ApplicationWithIcon?;
        if (application != null) {
          locator<PatcherViewModel>().selectedApp = PatchedApplication(
            name: application.appName,
            packageName: application.packageName,
            version: application.versionName!,
            apkFilePath: result.files.single.path!,
            icon: application.icon,
            patchDate: DateTime.now(),
            isRooted: false,
          );
          locator<PatcherViewModel>().selectedPatches.clear();
          locator<PatcherViewModel>().notifyListeners();
        }
      }
    } on Exception {
      Fluttertoast.showToast(
        msg: FlutterI18n.translate(
          context,
          'appSelectorView.errorMessage',
        ),
        toastLength: Toast.LENGTH_LONG,
        gravity: ToastGravity.CENTER,
      );
    }
  }

  List<ApplicationWithIcon> getFilteredApps(String query) {
    return apps
        .where((app) =>
            query.isEmpty ||
            query.length < 2 ||
            app.appName.toLowerCase().contains(query.toLowerCase()))
        .toList();
  }
}
