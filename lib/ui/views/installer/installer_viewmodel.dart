import 'package:device_apps/device_apps.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

class InstallerViewModel extends BaseViewModel {
  double? progress = 0.0;
  String logs = '';
  bool isPatching = false;
  bool isInstalled = false;

  Future<void> initialize() async {
    try {
      await FlutterBackground.initialize(
        androidConfig: const FlutterBackgroundAndroidConfig(
          notificationTitle: 'Patching',
          notificationText: 'ReVanced Manager is patching',
          notificationImportance: AndroidNotificationImportance.Default,
          notificationIcon: AndroidResource(
            name: 'ic_notification',
            defType: 'drawable',
          ),
        ),
      );
      await FlutterBackground.enableBackgroundExecution();
    } finally {
      await locator<PatcherAPI>().handlePlatformChannelMethods();
      await runPatcher();
    }
  }

  void updateProgress(double value) {
    progress = value;
    isInstalled = false;
    isPatching = progress == 1.0 ? false : true;
    if (progress == 0.0) {
      logs = '';
    }
    notifyListeners();
  }

  void updateLog(String message) {
    if (message.isNotEmpty && !message.startsWith('Merging L')) {
      if (logs.isNotEmpty) {
        logs += '\n';
      }
      logs += message;
      notifyListeners();
    }
  }

  Future<void> runPatcher() async {
    updateProgress(0.0);
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    List<Patch> selectedPatches =
        locator<PatchesSelectorViewModel>().selectedPatches;
    if (selectedApp != null && selectedPatches.isNotEmpty) {
      String apkFilePath = selectedApp.apkFilePath;
      try {
        updateLog('Initializing installer');
        if (selectedApp.isRooted && !selectedApp.isFromStorage) {
          updateLog('Checking if an old patched version exists');
          bool oldExists =
              await locator<PatcherAPI>().checkOldPatch(selectedApp);
          if (oldExists) {
            updateLog('Deleting old patched version');
            await locator<PatcherAPI>().deleteOldPatch(selectedApp);
          }
        }
        updateLog('Creating working directory');
        bool mergeIntegrations = false;
        bool resourcePatching = false;
        if (selectedApp.packageName == 'com.google.android.youtube') {
          mergeIntegrations = true;
          resourcePatching = true;
        } else if (selectedApp.packageName ==
            'com.google.android.apps.youtube.music') {
          resourcePatching = true;
        }
        await locator<PatcherAPI>().initPatcher(mergeIntegrations);
        await locator<PatcherAPI>().runPatcher(
          apkFilePath,
          selectedPatches,
          mergeIntegrations,
          resourcePatching,
        );
      } on Exception {
        updateLog('An error occurred! Aborting');
      }
    } else {
      updateLog('No app or patches selected! Aborting');
    }
    try {
      await FlutterBackground.disableBackgroundExecution();
    } finally {
      isPatching = false;
    }
  }

  void installResult() async {
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    if (selectedApp != null) {
      updateLog(selectedApp.isRooted
          ? 'Installing patched file using root method'
          : 'Installing patched file using nonroot method');
      isInstalled = await locator<PatcherAPI>().installPatchedFile(selectedApp);
      if (isInstalled) {
        updateLog('Done');
        await saveApp(selectedApp);
      } else {
        updateLog('An error occurred! Aborting');
      }
    }
  }

  void shareResult() {
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    if (selectedApp != null) {
      locator<PatcherAPI>().sharePatchedFile(
        selectedApp.name,
        selectedApp.version,
      );
    }
  }

  Future<void> cleanWorkplace() async {
    locator<PatcherAPI>().cleanPatcher();
    locator<AppSelectorViewModel>().selectedApp = null;
    locator<PatchesSelectorViewModel>().selectedPatches.clear();
    locator<PatcherViewModel>().notifyListeners();
  }

  void openApp() {
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    if (selectedApp != null) {
      DeviceApps.openApp(selectedApp.packageName);
    }
  }

  Future<void> saveApp(PatchedApplication selectedApp) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String> patchedApps = prefs.getStringList('patchedApps') ?? [];
    String app = selectedApp.toJson().toString();
    if (!patchedApps.contains(app)) {
      patchedApps.add(app);
      prefs.setStringList('patchedApps', patchedApps);
    }
  }
}
