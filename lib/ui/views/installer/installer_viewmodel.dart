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
  double? progress = 0.2;
  String logs = '';
  bool isPatching = false;
  bool isInstalled = false;
  bool showButtons = false;

  Future<void> initialize() async {
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
    await locator<PatcherAPI>().handlePlatformChannelMethods();
    runPatcher();
  }

  void addLog(String message) {
    if (message.isNotEmpty && !message.startsWith('Merging L')) {
      if (logs.isNotEmpty) {
        logs += '\n';
      }
      logs += message;
      notifyListeners();
    }
  }

  void updateProgress(double value) {
    progress = value;
    isInstalled = false;
    isPatching = progress == 1.0 ? false : true;
    showButtons = progress == 1.0 ? true : false;
    if (progress == 0.0) {
      logs = '';
    }
    notifyListeners();
  }

  Future<void> runPatcher() async {
    updateProgress(0.0);
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    if (selectedApp != null) {
      String apkFilePath = selectedApp.apkFilePath;
      List<Patch> selectedPatches =
          locator<PatchesSelectorViewModel>().selectedPatches;
      if (selectedPatches.isNotEmpty) {
        addLog('Initializing installer');
        if (selectedApp.isRooted && !selectedApp.isFromStorage) {
          addLog('Checking if an old patched version exists');
          bool oldExists =
              await locator<PatcherAPI>().checkOldPatch(selectedApp);
          if (oldExists) {
            addLog('Deleting old patched version');
            await locator<PatcherAPI>().deleteOldPatch(selectedApp);
          }
        }
        addLog('Creating working directory');
        bool? isSuccess = await locator<PatcherAPI>().initPatcher();
        if (isSuccess != null && isSuccess) {
          updateProgress(0.1);
          addLog('Copying original apk');
          isSuccess = await locator<PatcherAPI>().copyInputFile(apkFilePath);
          if (isSuccess != null && isSuccess) {
            updateProgress(0.2);
            addLog('Creating patcher');
            bool resourcePatching = false;
            if (selectedApp.packageName == 'com.google.android.youtube' ||
                selectedApp.packageName ==
                    'com.google.android.apps.youtube.music') {
              resourcePatching = true;
            }
            isSuccess = await locator<PatcherAPI>().createPatcher(
              resourcePatching,
            );
            if (isSuccess != null && isSuccess) {
              if (selectedApp.packageName == 'com.google.android.youtube') {
                updateProgress(0.3);
                addLog('Merging integrations');
                isSuccess = await locator<PatcherAPI>().mergeIntegrations();
              }
              if (isSuccess != null && isSuccess) {
                updateProgress(0.5);
                isSuccess =
                    await locator<PatcherAPI>().applyPatches(selectedPatches);
                if (isSuccess != null && isSuccess) {
                  updateProgress(0.7);
                  addLog('Repacking patched apk');
                  isSuccess = await locator<PatcherAPI>().repackPatchedFile();
                  if (isSuccess != null && isSuccess) {
                    updateProgress(0.9);
                    addLog('Signing patched apk');
                    isSuccess = await locator<PatcherAPI>().signPatchedFile();
                    if (isSuccess != null && isSuccess) {
                      showButtons = true;
                      updateProgress(1.0);
                      addLog('Finished');
                    }
                  }
                }
              }
            }
          }
        }
        if (isSuccess == null || !isSuccess) {
          addLog('An error occurred! Aborting');
        }
      } else {
        addLog('No patches selected! Aborting');
      }
    } else {
      addLog('No app selected! Aborting');
    }
    await FlutterBackground.disableBackgroundExecution();
    isPatching = false;
  }

  void installResult() async {
    PatchedApplication? selectedApp =
        locator<AppSelectorViewModel>().selectedApp;
    if (selectedApp != null) {
      addLog(selectedApp.isRooted
          ? 'Installing patched file using root method'
          : 'Installing patched file using nonroot method');
      isInstalled = await locator<PatcherAPI>().installPatchedFile(selectedApp);
      if (isInstalled) {
        addLog('Done');
        await saveApp(selectedApp);
      } else {
        addLog('An error occurred! Aborting');
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
