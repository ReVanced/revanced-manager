// ignore_for_file: use_build_context_synchronously
import 'dart:async';
import 'dart:io';

import 'package:app_installer/app_installer.dart';
import 'package:cross_connectivity/cross_connectivity.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/homeView/update_confirmation_dialog.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class HomeViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final GithubAPI _githubAPI = locator<GithubAPI>();
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
  final Toast _toast = locator<Toast>();
  final flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();
  DateTime? _lastUpdate;
  bool showUpdatableApps = false;
  List<PatchedApplication> patchedInstalledApps = [];
  List<PatchedApplication> patchedUpdatableApps = [];
  String _managerVersion = '';

  Future<void> initialize(BuildContext context) async {
    _managerVersion = await AboutInfo.getInfo().then(
      (value) => value.keys.contains('version') ? value['version']! : '',
    );
    _managerVersion = await _managerAPI.getCurrentManagerVersion();
    await flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('ic_notification'),
      ),
      onDidReceiveNotificationResponse: (response) async {
        if (response.id == 0) {
          _toast.showBottom('homeView.installingMessage');
          final File? managerApk = await _managerAPI.downloadManager();
          if (managerApk != null) {
            await AppInstaller.installApk(managerApk.path);
          } else {
            _toast.showBottom('homeView.errorDownloadMessage');
          }
        }
      },
    );
    flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestPermission();
    final bool isConnected = await Connectivity().checkConnection();
    if (!isConnected) {
      _toast.showBottom('homeView.noConnection');
    }
    final NotificationAppLaunchDetails? notificationAppLaunchDetails =
        await flutterLocalNotificationsPlugin.getNotificationAppLaunchDetails();
    if (notificationAppLaunchDetails?.didNotificationLaunchApp ?? false) {
      _toast.showBottom('homeView.installingMessage');
      final File? managerApk = await _managerAPI.downloadManager();
      if (managerApk != null) {
        await AppInstaller.installApk(managerApk.path);
      } else {
        _toast.showBottom('homeView.errorDownloadMessage');
      }
    }
    _getPatchedApps();
    _managerAPI.reAssessSavedApps().then((_) => _getPatchedApps());
  }

  void navigateToAppInfo(PatchedApplication app) {
    _navigationService.navigateTo(
      Routes.appInfoView,
      arguments: AppInfoViewArguments(app: app),
    );
  }

  void toggleUpdatableApps(bool value) {
    showUpdatableApps = value;
    notifyListeners();
  }

  Future<void> navigateToPatcher(PatchedApplication app) async {
    locator<PatcherViewModel>().selectedApp = app;
    locator<PatcherViewModel>().selectedPatches =
        await _patcherAPI.getAppliedPatches(app.appliedPatches);
    locator<PatcherViewModel>().notifyListeners();
    locator<NavigationViewModel>().setIndex(1);
  }

  void _getPatchedApps() {
    patchedInstalledApps = _managerAPI.getPatchedApps().toList();
    patchedUpdatableApps = _managerAPI
        .getPatchedApps()
        .where((app) => app.hasUpdates == true)
        .toList();
    notifyListeners();
  }

  Future<bool> hasManagerUpdates() async {
    final String? latestVersion = await _managerAPI.getLatestManagerVersion();
    final String currentVersion = await _managerAPI.getCurrentManagerVersion();
    if (latestVersion != null) {
      try {
        final int latestVersionInt =
            int.parse(latestVersion.replaceAll(RegExp('[^0-9]'), ''));
        final int currentVersionInt =
            int.parse(currentVersion.replaceAll(RegExp('[^0-9]'), ''));
        return latestVersionInt > currentVersionInt;
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
        return false;
      }
    }
    return false;
  }

  Future<bool> hasPatchesUpdates() async {
    final String? latestVersion = await _managerAPI.getLatestPatchesVersion();
    final String? currentVersion = await _managerAPI.getCurrentPatchesVersion();
    if (latestVersion != null) {
      try {
        final int latestVersionInt =
            int.parse(latestVersion.replaceAll(RegExp('[^0-9]'), ''));
        final int currentVersionInt =
            int.parse(currentVersion!.replaceAll(RegExp('[^0-9]'), ''));
        return latestVersionInt > currentVersionInt;
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
        return false;
      }
    }
    return false;
  }

  Future<File?> downloadManager() async {
    try {
      final response = await _revancedAPI.downloadManager();
      final bytes = await response!.readAsBytes();
      final tempDir = await getTemporaryDirectory();
      final tempFile = File('${tempDir.path}/revanced-manager.apk');
      await tempFile.writeAsBytes(bytes);
      return tempFile;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<void> updateManager(BuildContext context) async {
    try {
      _toast.showBottom('homeView.downloadingMessage');
      showDialog(
        context: context,
        builder: (context) => SimpleDialog(
          contentPadding: const EdgeInsets.all(16.0),
          title: I18nText(
            'homeView.downloadingMessage',
            child: Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
                color: Theme.of(context).colorScheme.secondary,
              ),
            ),
          ),
          children: [
            Column(
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.new_releases_outlined,
                      color: Theme.of(context).colorScheme.secondary,
                    ),
                    const SizedBox(width: 8.0),
                    Text(
                      'v$_managerVersion',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w500,
                        color: Theme.of(context).colorScheme.secondary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16.0),
                StreamBuilder<double>(
                  initialData: 0.0,
                  stream: _revancedAPI.managerUpdateProgress.stream,
                  builder: (context, snapshot) {
                    return LinearProgressIndicator(
                      value: snapshot.data! * 0.01,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        Theme.of(context).colorScheme.secondary,
                      ),
                    );
                  },
                ),
                const SizedBox(height: 16.0),
                Align(
                  alignment: Alignment.centerRight,
                  child: CustomMaterialButton(
                    label: I18nText('cancelButton'),
                    onPressed: () {
                      _revancedAPI.disposeManagerUpdateProgress();
                      Navigator.of(context).pop();
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      );
      final File? managerApk = await downloadManager();
      if (managerApk != null) {
        // await flutterLocalNotificationsPlugin.zonedSchedule(
        //   0,
        //   FlutterI18n.translate(
        //     context,
        //     'homeView.notificationTitle',
        //   ),
        //   FlutterI18n.translate(
        //     context,
        //     'homeView.notificationText',
        //   ),
        //   tz.TZDateTime.now(tz.local).add(const Duration(seconds: 5)),
        //   const NotificationDetails(
        //     android: AndroidNotificationDetails(
        //       'revanced_manager_channel',
        //       'ReVanced Manager Channel',
        //       importance: Importance.max,
        //       priority: Priority.high,
        //       ticker: 'ticker',
        //     ),
        //   ),
        //   androidAllowWhileIdle: true,
        //   uiLocalNotificationDateInterpretation:
        //       UILocalNotificationDateInterpretation.absoluteTime,
        // );
        _toast.showBottom('homeView.installingMessage');
        await AppInstaller.installApk(managerApk.path);
      } else {
        _toast.showBottom('homeView.errorDownloadMessage');
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom('homeView.errorInstallMessage');
    }
  }

  void updatesAreDisabled() {
    _toast.showBottom('homeView.updatesDisabled');
  }

  Future<void> showUpdateConfirmationDialog(
    BuildContext parentContext,
  ) {
    return showModalBottomSheet(
      context: parentContext,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24.0)),
      ),
      builder: (context) => const UpdateConfirmationDialog(),
    );
  }

  Future<Map<String, dynamic>?> getLatestManagerRelease() {
    return _githubAPI.getLatestRelease(_managerAPI.defaultManagerRepo);
  }

  Future<String?> getLatestPatcherReleaseTime() {
    return _managerAPI.getLatestPatcherReleaseTime();
  }

  Future<String?> getLatestManagerReleaseTime() {
    return _managerAPI.getLatestManagerReleaseTime();
  }

  Future<void> forceRefresh(BuildContext context) async {
    await Future.delayed(const Duration(seconds: 1));
    if (_lastUpdate == null ||
        _lastUpdate!.difference(DateTime.now()).inSeconds > 2) {
      _managerAPI.clearAllData();
    }
    _toast.showBottom('homeView.refreshSuccess');
    initialize(context);
  }
}
