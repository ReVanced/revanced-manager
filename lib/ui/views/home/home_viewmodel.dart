// ignore_for_file: use_build_context_synchronously
import 'dart:async';
import 'dart:io';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
  List<PatchedApplication> patchedInstalledApps = [];
  String? _latestManagerVersion = '';
  File? downloadedApk;

  Future<void> initialize(BuildContext context) async {
    _managerAPI.rePatchedSavedApps().then((_) => _getPatchedApps());

    if (!_managerAPI.getPatchesConsent()) {
      await showPatchesConsent(context);
    }

    _latestManagerVersion = await _managerAPI.getLatestManagerVersion();

    await _patcherAPI.initialize();

    await flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings('ic_notification'),
      ),
      onDidReceiveNotificationResponse: (response) async {
        if (response.id == 0) {
          _toast.showBottom('homeView.installingMessage');
          final File? managerApk = await _managerAPI.downloadManager();
          if (managerApk != null) {
            await _patcherAPI.installApk(context, managerApk.path);
          } else {
            _toast.showBottom('homeView.errorDownloadMessage');
          }
        }
      },
    );
    flutterLocalNotificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();

    final bool isConnected =
        await Connectivity().checkConnectivity() != ConnectivityResult.none;
    if (!isConnected) {
      _toast.showBottom('homeView.noConnection');
    }

    final NotificationAppLaunchDetails? notificationAppLaunchDetails =
        await flutterLocalNotificationsPlugin.getNotificationAppLaunchDetails();
    if (notificationAppLaunchDetails?.didNotificationLaunchApp ?? false) {
      _toast.showBottom('homeView.installingMessage');
      final File? managerApk = await _managerAPI.downloadManager();
      if (managerApk != null) {
        await _patcherAPI.installApk(context, managerApk.path);
      } else {
        _toast.showBottom('homeView.errorDownloadMessage');
      }
    }
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
    notifyListeners();
  }

  Future<bool> hasManagerUpdates() async {
    String currentVersion = await _managerAPI.getCurrentManagerVersion();

    // add v to current version
    if (!currentVersion.startsWith('v')) {
      currentVersion = 'v$currentVersion';
    }

    _latestManagerVersion =
        await _managerAPI.getLatestManagerVersion() ?? currentVersion;

    if (_latestManagerVersion != currentVersion) {
      return true;
    }
    return false;
  }

  Future<bool> hasPatchesUpdates() async {
    final String? latestVersion = await _managerAPI.getLatestPatchesVersion();
    final String currentVersion = await _managerAPI.getCurrentPatchesVersion();
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

  Future<void> showPatchesConsent(BuildContext context) async {
    final ValueNotifier<bool> autoUpdate = ValueNotifier(true);
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Download ReVanced Patches?'),
        content: ValueListenableBuilder(
          valueListenable: autoUpdate,
          builder: (context, value, child) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                I18nText(
                  'homeView.patchesConsentDialogText',
                  child: Text(
                    '',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                      color: Theme.of(context).colorScheme.secondary,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: I18nText(
                    'homeView.patchesConsentDialogText2',
                    translationParams: {
                      'url': _managerAPI.defaultApiUrl.split('/')[2],
                    },
                    child: Text(
                      '',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ),
                ),
                CheckboxListTile(
                  value: value,
                  contentPadding: EdgeInsets.zero,
                  title: I18nText(
                    'homeView.patchesConsentDialogText3',
                  ),
                  subtitle: I18nText(
                    'homeView.patchesConsentDialogText3Sub',
                  ),
                  onChanged: (selected) {
                    autoUpdate.value = selected!;
                  },
                ),
              ],
            );
          },
        ),
        actions: [
          TextButton(
            onPressed: () async {
              await _managerAPI.setPatchesConsent(false);
              SystemNavigator.pop();
            },
            child: I18nText('quitButton'),
          ),
          FilledButton(
            onPressed: () async {
              await _managerAPI.setPatchesConsent(true);
              await _managerAPI.setPatchesAutoUpdate(autoUpdate.value);
              Navigator.of(context).pop();
            },
            child: I18nText('okButton'),
          ),
        ],
      ),
    );
  }

  Future<void> updatePatches(BuildContext context) async {
    _toast.showBottom('homeView.downloadingMessage');
    final String patchesVersion =
        await _managerAPI.getLatestPatchesVersion() ?? '0.0.0';
    final String integrationsVersion =
        await _managerAPI.getLatestIntegrationsVersion() ?? '0.0.0';
    if (patchesVersion != '0.0.0' && integrationsVersion != '0.0.0') {
      await _managerAPI.setCurrentPatchesVersion(patchesVersion);
      await _managerAPI.setCurrentIntegrationsVersion(integrationsVersion);
      _toast.showBottom('homeView.downloadedMessage');
      forceRefresh(context);
    } else {
      _toast.showBottom('homeView.errorDownloadMessage');
    }
  }

  Future<void> updateManager(BuildContext context) async {
    final ValueNotifier<bool> downloaded = ValueNotifier(false);
    try {
      _toast.showBottom('homeView.downloadingMessage');
      showDialog(
        context: context,
        builder: (context) => ValueListenableBuilder(
          valueListenable: downloaded,
          builder: (context, value, child) {
            return SimpleDialog(
              backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
              contentPadding: const EdgeInsets.all(16.0),
              title: I18nText(
                !value
                    ? 'homeView.downloadingMessage'
                    : 'homeView.downloadedMessage',
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
                          '$_latestManagerVersion',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).colorScheme.secondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16.0),
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
                              child: I18nText('cancelButton'),
                            ),
                          ),
                        ],
                      ),
                    if (value)
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          I18nText(
                            'homeView.installUpdate',
                            child: Text(
                              '',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w500,
                                color: Theme.of(context).colorScheme.secondary,
                              ),
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
                                  child: I18nText('cancelButton'),
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
                                  child: I18nText('updateButton'),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                  ],
                ),
              ],
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
        _toast.showBottom('homeView.installingMessage');
        await _patcherAPI.installApk(context, managerApk.path);
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
    bool isPatches,
  ) {
    return showModalBottomSheet(
      context: parentContext,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24.0)),
      ),
      builder: (context) => UpdateConfirmationDialog(
        isPatches: isPatches,
      ),
    );
  }

  Future<Map<String, dynamic>?> getLatestManagerRelease() {
    return _githubAPI.getLatestManagerRelease(_managerAPI.defaultManagerRepo);
  }

  Future<Map<String, dynamic>?> getLatestPatchesRelease() {
    return _githubAPI.getLatestPatchesRelease(_managerAPI.defaultPatchesRepo);
  }

  Future<String?> getLatestPatchesReleaseTime() {
    return _managerAPI.getLatestPatchesReleaseTime();
  }

  Future<String?> getLatestManagerReleaseTime() {
    return _managerAPI.getLatestManagerReleaseTime();
  }

  Future<void> forceRefresh(BuildContext context) async {
    _managerAPI.clearAllData();
    _toast.showBottom('homeView.refreshSuccess');
    initialize(context);
  }
}
