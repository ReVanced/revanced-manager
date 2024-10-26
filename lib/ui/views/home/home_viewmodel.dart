// ignore_for_file: use_build_context_synchronously
import 'dart:async';
import 'dart:io';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/homeView/update_confirmation_sheet.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_checkbox_list_tile.dart';
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
  bool showUpdatableApps = false;
  PatchedApplication? lastPatchedApp;
  bool releaseBuild = false;
  List<PatchedApplication> patchedInstalledApps = [];
  String _currentManagerVersion = '';
  String _currentPatchesVersion = '';
  String? latestManagerVersion;
  String? latestPatchesVersion;
  File? downloadedApk;

  Future<void> initialize(BuildContext context) async {
    _managerAPI.reAssessPatchedApps().then((_) => getPatchedApps());
    _currentManagerVersion = await _managerAPI.getCurrentManagerVersion();
    if (!_managerAPI.getDownloadConsent()) {
      await showDownloadConsent(context);
      await forceRefresh(context);
      return;
    }
    _currentPatchesVersion = await _managerAPI.getCurrentPatchesVersion();
    if (_managerAPI.showUpdateDialog() && await hasManagerUpdates()) {
      showUpdateDialog(context, false);
    }
    if (!_managerAPI.isPatchesAutoUpdate() &&
        _managerAPI.showUpdateDialog() &&
        await hasPatchesUpdates()) {
      showUpdateDialog(context, true);
    }

    await _patcherAPI.initialize();

    await flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('ic_notification'),
      ),
      onDidReceiveNotificationResponse: (response) async {
        if (response.id == 0) {
          _toast.showBottom(t.homeView.installingMessage);
          final File? managerApk = await _managerAPI.downloadManager();
          if (managerApk != null) {
            await _patcherAPI.installApk(context, managerApk.path);
          } else {
            _toast.showBottom(t.homeView.errorDownloadMessage);
          }
        }
      },
    );
    flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    final bool isConnected = !(await Connectivity().checkConnectivity())
        .contains(ConnectivityResult.none);
    if (!isConnected) {
      _toast.showBottom(t.homeView.noConnection);
    }

    final NotificationAppLaunchDetails? notificationAppLaunchDetails =
        await flutterLocalNotificationsPlugin.getNotificationAppLaunchDetails();
    if (notificationAppLaunchDetails?.didNotificationLaunchApp ?? false) {
      _toast.showBottom(t.homeView.installingMessage);
      final File? managerApk = await _managerAPI.downloadManager();
      if (managerApk != null) {
        await _patcherAPI.installApk(context, managerApk.path);
      } else {
        _toast.showBottom(t.homeView.errorDownloadMessage);
      }
    }
  }

  void navigateToAppInfo(PatchedApplication app, bool isLastPatchedApp) {
    _navigationService.navigateTo(
      Routes.appInfoView,
      arguments:
          AppInfoViewArguments(app: app, isLastPatchedApp: isLastPatchedApp),
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

  void getPatchedApps() {
    lastPatchedApp = _managerAPI.getLastPatchedApp();
    patchedInstalledApps = _managerAPI.getPatchedApps().toList();
    notifyListeners();
  }

  bool isLastPatchedAppEnabled() {
    return _managerAPI.isLastPatchedAppEnabled();
  }

  Future<bool> hasManagerUpdates() async {
    if (!_managerAPI.releaseBuild) {
      return false;
    }
    latestManagerVersion =
        await _managerAPI.getLatestManagerVersion() ?? _currentManagerVersion;

    if (latestManagerVersion != _currentManagerVersion) {
      return true;
    }
    return false;
  }

  Future<bool> hasPatchesUpdates() async {
    latestPatchesVersion = await _managerAPI.getLatestPatchesVersion();
    if (latestPatchesVersion != null) {
      try {
        final int latestVersionInt =
            int.parse(latestPatchesVersion!.replaceAll(RegExp('[^0-9]'), ''));
        final int currentVersionInt =
            int.parse(_currentPatchesVersion.replaceAll(RegExp('[^0-9]'), ''));
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

  Future<void> showDownloadConsent(BuildContext context) async {
    final ValueNotifier<bool> autoUpdate = ValueNotifier(true);
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => PopScope(
        canPop: false,
        child: AlertDialog(
          title: Text(t.homeView.downloadConsentDialogTitle),
          content: ValueListenableBuilder(
            valueListenable: autoUpdate,
            builder: (context, value, child) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    t.homeView.downloadConsentDialogText,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                      color: Theme.of(context).colorScheme.secondary,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Text(
                      t.homeView.downloadConsentDialogText2(
                        url: _managerAPI.defaultApiUrl.split('/')[2],
                      ),
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
          actions: [
            TextButton(
              onPressed: () async {
                _managerAPI.setDownloadConsent(false);
                SystemNavigator.pop();
              },
              child: Text(t.quitButton),
            ),
            FilledButton(
              onPressed: () async {
                _managerAPI.setDownloadConsent(true);
                _managerAPI.setPatchesAutoUpdate(autoUpdate.value);
                Navigator.of(context).pop();
              },
              child: Text(t.okButton),
            ),
          ],
        ),
      ),
    );
  }

  void showUpdateDialog(BuildContext context, bool isPatches) {
    final ValueNotifier<bool> noShow =
        ValueNotifier(!_managerAPI.showUpdateDialog());
    showDialog(
      context: context,
      builder: (innerContext) => AlertDialog(
        title: Text(t.homeView.updateDialogTitle),
        content: ValueListenableBuilder(
          valueListenable: noShow,
          builder: (context, value, child) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  t.homeView.updateDialogText(
                    file: isPatches ? 'ReVanced Patches' : 'ReVanced Manager',
                    version: isPatches
                        ? _currentPatchesVersion
                        : _currentManagerVersion,
                  ),
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                    color: Theme.of(context).colorScheme.secondary,
                  ),
                ),
                const SizedBox(height: 10),
                HapticCheckboxListTile(
                  value: value,
                  contentPadding: EdgeInsets.zero,
                  title: Text(t.noShowAgain),
                  subtitle: Text(t.homeView.changeLaterSubtitle),
                  onChanged: (selected) {
                    noShow.value = selected!;
                  },
                ),
              ],
            );
          },
        ),
        actions: [
          TextButton(
            onPressed: () async {
              _managerAPI.setShowUpdateDialog(!noShow.value);
              Navigator.pop(innerContext);
            },
            child: Text(t.dismissButton), // Decide later
          ),
          FilledButton(
            onPressed: () async {
              _managerAPI.setShowUpdateDialog(!noShow.value);
              Navigator.pop(innerContext);
              await showUpdateConfirmationDialog(context, isPatches);
            },
            child: Text(t.showUpdateButton),
          ),
        ],
      ),
    );
  }

  Future<void> updatePatches(BuildContext context) async {
    _toast.showBottom(t.homeView.downloadingMessage);
    final String patchesVersion =
        await _managerAPI.getLatestPatchesVersion() ?? '0.0.0';
    if (patchesVersion != '0.0.0') {
      await _managerAPI.setCurrentPatchesVersion(patchesVersion);
      _toast.showBottom(t.homeView.downloadedMessage);
      forceRefresh(context);
    } else {
      _toast.showBottom(t.homeView.errorDownloadMessage);
    }
  }

  Future<void> updateManager(BuildContext context) async {
    final ValueNotifier<bool> downloaded = ValueNotifier(false);
    try {
      _toast.showBottom(t.homeView.downloadingMessage);
      showDialog(
        context: context,
        builder: (context) => ValueListenableBuilder(
          valueListenable: downloaded,
          builder: (context, value, child) {
            return AlertDialog(
              title: Text(
                !value
                    ? t.homeView.downloadingMessage
                    : t.homeView.downloadedMessage,
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (!value)
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
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
                          child: FilledButton(
                            onPressed: () {
                              _revancedAPI.disposeManagerUpdateProgress();
                              Navigator.of(context).pop();
                            },
                            child: Text(t.cancelButton),
                          ),
                        ),
                      ],
                    ),
                  if (value)
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          t.homeView.installUpdate,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).colorScheme.secondary,
                          ),
                        ),
                        const SizedBox(height: 16.0),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: () {
                                  Navigator.of(context).pop();
                                },
                                child: Text(t.cancelButton),
                              ),
                            ),
                            const SizedBox(width: 8.0),
                            Align(
                              alignment: Alignment.centerRight,
                              child: FilledButton(
                                onPressed: () async {
                                  await _patcherAPI.installApk(
                                    context,
                                    downloadedApk!.path,
                                  );
                                },
                                child: Text(t.updateButton),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                ],
              ),
            );
          },
        ),
      );
      final File? managerApk = await downloadManager();
      if (managerApk != null) {
        downloaded.value = true;
        downloadedApk = managerApk;
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
        _toast.showBottom(t.homeView.installingMessage);
        await _patcherAPI.installApk(context, managerApk.path);
      } else {
        _toast.showBottom(t.homeView.errorDownloadMessage);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom(t.homeView.errorInstallMessage);
    }
  }

  Future<void> showUpdateConfirmationDialog(
    BuildContext parentContext,
    bool isPatches, [
    bool changelog = false,
  ]) {
    return showModalBottomSheet(
      context: parentContext,
      useSafeArea: true,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24.0)),
      ),
      builder: (context) => UpdateConfirmationSheet(
        isPatches: isPatches,
        changelog: changelog,
      ),
    );
  }

  Future<String?> getChangelogs(bool isPatches) {
    return _githubAPI.getChangelogs(isPatches);
  }

  Future<String?> getLatestPatchesReleaseTime() {
    return _managerAPI.getLatestPatchesReleaseTime();
  }

  Future<String?> getLatestManagerReleaseTime() {
    return _managerAPI.getLatestManagerReleaseTime();
  }

  Future<void> forceRefresh(BuildContext context) async {
    await _managerAPI.clearAllData();
    await initialize(context);
    _toast.showBottom(t.homeView.refreshSuccess);
  }
}
