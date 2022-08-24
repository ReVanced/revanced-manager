import 'dart:convert';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

class InstallerViewModel extends BaseViewModel {
  final ScrollController scrollController = ScrollController();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final PatchedApplication? _app = locator<PatcherViewModel>().selectedApp;
  final List<Patch> _patches = locator<PatcherViewModel>().selectedPatches;
  static const _installerChannel = MethodChannel(
    'app.revanced.manager/installer',
  );
  double? progress = 0.0;
  String logs = '';
  String headerLogs = '';
  bool isPatching = false;
  bool isInstalled = false;

  Future<void> initialize(BuildContext context) async {
    try {
      await FlutterBackground.initialize(
        androidConfig: FlutterBackgroundAndroidConfig(
          notificationTitle: FlutterI18n.translate(
            context,
            'installerView.notificationTitle',
          ),
          notificationText: FlutterI18n.translate(
            context,
            'installerView.notificationText',
          ),
          notificationImportance: AndroidNotificationImportance.Default,
          notificationIcon: const AndroidResource(
            name: 'ic_notification',
            defType: 'drawable',
          ),
        ),
      );
      await FlutterBackground.enableBackgroundExecution();
    } finally {
      await handlePlatformChannelMethods();
      await runPatcher();
    }
  }

  Future<dynamic> handlePlatformChannelMethods() async {
    _installerChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'updateProgress':
          if (call.arguments != null) {
            updateProgress(call.arguments);
          }
          break;
        case 'updateLog':
          if (call.arguments != null) {
            updateLog(call.arguments);
          }
          break;
      }
    });
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
      Future.delayed(const Duration(milliseconds: 500)).then((value) {
        scrollController.animateTo(
          scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.fastOutSlowIn,
        );
      });
      notifyListeners();
    }
  }

  Future<void> runPatcher() async {
    updateProgress(0.0);
    if (_app != null && _patches.isNotEmpty) {
      String apkFilePath = _app!.apkFilePath;
      try {
        updateLog('Initializing installer');
        headerLogs = 'Initializing';
        if (_app!.isRooted && !_app!.isFromStorage) {
          updateLog('Checking if an old patched version exists');
          bool oldExists = await _patcherAPI.checkOldPatch(_app!);
          if (oldExists) {
            updateLog('Deleting old patched version');
            await _patcherAPI.deleteOldPatch(_app!);
          }
        }
        updateLog('Creating working directory');
        bool mergeIntegrations = false;
        bool resourcePatching = false;
        if (_app!.packageName == 'com.google.android.youtube') {
          mergeIntegrations = true;
          resourcePatching = true;
        } else if (_app!.packageName ==
            'com.google.android.apps.youtube.music') {
          resourcePatching = true;
        }
        await _patcherAPI.mergeIntegrations(mergeIntegrations);
        headerLogs = 'Merging integrations';
        await _patcherAPI.runPatcher(
          apkFilePath,
          _patches,
          mergeIntegrations,
          resourcePatching,
        );
      } on Exception {
        updateLog('An error occurred! Aborting');
        headerLogs = 'Aborting...';
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
    if (_app != null) {
      updateLog(_app!.isRooted
          ? 'Installing patched file using root method'
          : 'Installing patched file using nonroot method');
      headerLogs = 'Installing...';
      isInstalled = await _patcherAPI.installPatchedFile(_app!);
      if (isInstalled) {
        updateLog('Done');
        _app!.patchDate = DateTime.now();
        _app!.appliedPatches.addAll(_patches.map((p) => p.name).toList());
        await saveApp();
      } else {
        updateLog('An error occurred! Aborting');
        headerLogs = 'Aborting...';
      }
    }
  }

  void shareResult() {
    if (_app != null) {
      _patcherAPI.sharePatchedFile(_app!.name, _app!.version);
    }
  }

  Future<void> cleanPatcher() async {
    _patcherAPI.cleanPatcher();
    locator<PatcherViewModel>().selectedApp = null;
    locator<PatcherViewModel>().selectedPatches.clear();
    locator<PatcherViewModel>().notifyListeners();
  }

  void openApp() {
    if (_app != null) {
      DeviceApps.openApp(_app!.packageName);
    }
  }

  Future<void> saveApp() async {
    if (_app != null) {
      SharedPreferences prefs = await SharedPreferences.getInstance();
      List<String> patchedApps = prefs.getStringList('patchedApps') ?? [];
      String appStr = json.encode(_app!.toJson());
      patchedApps.removeWhere(
          (a) => json.decode(a)['packageName'] == _app!.packageName);
      patchedApps.add(appStr);
      prefs.setStringList('patchedApps', patchedApps);
    }
  }
}
