import 'dart:io';
import 'package:device_apps/device_apps.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI patcherAPI = locator<PatcherAPI>();
  bool isRooted = false;
  bool isFromStorage = false;
  List<ApplicationWithIcon> apps = [];
  PatchedApplication? selectedApp;

  Future<void> initialize() async {
    await getApps();
    SharedPreferences prefs = await SharedPreferences.getInstance();
    isRooted = prefs.getBool('isRooted') ?? false;
    notifyListeners();
  }

  Future<void> getApps() async {
    apps = await patcherAPI.getFilteredInstalledApps();
  }

  void selectApp(ApplicationWithIcon application) async {
    isFromStorage = false;
    PatchedApplication app = PatchedApplication(
      name: application.appName,
      packageName: application.packageName,
      version: application.versionName!,
      apkFilePath: application.apkFilePath,
      icon: application.icon,
      patchDate: DateTime.now(),
      isRooted: isRooted,
      isFromStorage: isFromStorage,
      appliedPatches: [],
    );
    locator<AppSelectorViewModel>().selectedApp = app;
    locator<PatchesSelectorViewModel>().selectedPatches.clear();
    locator<PatchesSelectorViewModel>().notifyListeners();
    locator<PatcherViewModel>().notifyListeners();
  }

  Future<void> selectAppFromStorage(BuildContext context) async {
    isFromStorage = true;
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
          PatchedApplication app = PatchedApplication(
            name: application.appName,
            packageName: application.packageName,
            version: application.versionName!,
            apkFilePath: result.files.single.path!,
            icon: application.icon,
            patchDate: DateTime.now(),
            isRooted: isRooted,
            isFromStorage: isFromStorage,
            appliedPatches: [],
          );
          locator<AppSelectorViewModel>().selectedApp = app;
          locator<PatchesSelectorViewModel>().selectedPatches.clear();
          locator<PatchesSelectorViewModel>().notifyListeners();
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
}
