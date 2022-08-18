import 'dart:convert';
import 'dart:io';
import 'package:app_installer/app_installer.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class HomeViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = ManagerAPI();
  final GithubAPI _githubAPI = GithubAPI();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool showUpdatableApps = true;

  Future<void> initialize() async {
    await _patcherAPI.loadPatches();
    await flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('ic_notification'),
      ),
      onSelectNotification: (p) => DeviceApps.openApp('app.revanced.manager'),
    );
  }

  void toggleUpdatableApps(bool value) {
    showUpdatableApps = value;
    notifyListeners();
  }

  void navigateToInstaller(PatchedApplication app) async {
    locator<PatcherViewModel>().selectedApp = app;
    locator<PatcherViewModel>().selectedPatches =
        await _patcherAPI.getAppliedPatches(app);
    _navigationService.navigateTo(Routes.installerView);
  }

  Future<List<PatchedApplication>> getPatchedApps(bool isUpdatable) async {
    List<PatchedApplication> list = [];
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String> patchedApps = prefs.getStringList('patchedApps') ?? [];
    for (String str in patchedApps) {
      PatchedApplication app = PatchedApplication.fromJson(json.decode(str));
      bool hasUpdates = await _githubAPI.hasUpdates(app, ghOrg, patchesRepo);
      if (hasUpdates == isUpdatable) {
        list.add(app);
      }
    }
    return list;
  }

  void updateManager(BuildContext context) async {
    File? managerApk = await _managerAPI.downloadManager();
    if (managerApk != null) {
      flutterLocalNotificationsPlugin.show(
        0,
        // ignore: use_build_context_synchronously
        FlutterI18n.translate(
          context,
          'homeView.notificationTitle',
        ),
        // ignore: use_build_context_synchronously
        FlutterI18n.translate(
          context,
          'homeView.notificationText',
        ),
        const NotificationDetails(
          android: AndroidNotificationDetails(
            'revanced_manager_channel',
            'ReVanced Manager Channel',
          ),
        ),
      );
      AppInstaller.installApk(managerApk.path);
    }
  }
}
