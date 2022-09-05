import 'package:device_apps/device_apps.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:stacked/stacked.dart';

class InstallerViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final PatchedApplication _app = locator<PatcherViewModel>().selectedApp!;
  final List<Patch> _patches = locator<PatcherViewModel>().selectedPatches;
  static const _installerChannel = MethodChannel(
    'app.revanced.manager/installer',
  );
  final ScrollController scrollController = ScrollController();
  double? progress = 0.0;
  String logs = '';
  String headerLogs = '';
  bool isPatching = true;
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
        case 'update':
          if (call.arguments != null) {
            Map<dynamic, dynamic> arguments = call.arguments;
            double progress = arguments['progress'];
            String header = arguments['header'];
            String log = arguments['log'];
            update(progress, header, log);
          }
          break;
      }
    });
  }

  void update(double value, String header, String log) {
    if (value > 0) {
      progress = value;
    }
    isPatching = progress == 1.0 ? false : true;
    if (progress == 0.0) {
      logs = '';
      isInstalled = false;
    }
    if (header.isNotEmpty) {
      headerLogs = header;
    }
    if (log.isNotEmpty && !log.startsWith('Merging L')) {
      if (logs.isNotEmpty) {
        logs += '\n';
      }
      logs += log;
      Future.delayed(const Duration(milliseconds: 500)).then((value) {
        scrollController.animateTo(
          scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.fastOutSlowIn,
        );
      });
    }
    notifyListeners();
  }

  Future<void> runPatcher() async {
    update(0.0, 'Initializing...', 'Initializing installer');
    if (_patches.isNotEmpty) {
      String apkFilePath = _app.apkFilePath;
      try {
        if (_app.isRooted) {
          update(0.0, '', 'Checking if an old patched version exists');
          bool oldExists = await _patcherAPI.checkOldPatch(_app);
          if (oldExists) {
            update(0.0, '', 'Deleting old patched version');
            await _patcherAPI.deleteOldPatch(_app);
          }
        }
        update(0.0, '', 'Creating working directory');
        await _patcherAPI.runPatcher(_app.packageName, apkFilePath, _patches);
      } on Exception {
        update(1.0, 'Aborting...', 'An error occurred! Aborting');
      }
    } else {
      update(1.0, 'Aborting...', 'No app or patches selected! Aborting');
    }
    try {
      await FlutterBackground.disableBackgroundExecution();
    } finally {
      isPatching = false;
    }
  }

  void installResult(bool installAsRoot) async {
    _app.isRooted = installAsRoot;
    update(
      1.0,
      'Installing...',
      _app.isRooted
          ? 'Installing patched file using root method'
          : 'Installing patched file using nonroot method',
    );
    isInstalled = await _patcherAPI.installPatchedFile(_app);
    if (isInstalled) {
      update(1.0, 'Installed!', 'Installed!');
      _app.patchDate = DateTime.now();
      _app.appliedPatches = _patches.map((p) => p.name).toList();
      await _managerAPI.savePatchedApp(_app);
    } else {
      update(1.0, 'Aborting...', 'An error occurred! Aborting');
    }
  }

  void shareResult() {
    _patcherAPI.sharePatchedFile(_app.name, _app.version);
  }

  void shareLog() {
    _patcherAPI.shareLog(logs);
  }

  Future<void> cleanPatcher() async {
    _patcherAPI.cleanPatcher();
    locator<PatcherViewModel>().selectedApp = null;
    locator<PatcherViewModel>().selectedPatches.clear();
    locator<PatcherViewModel>().notifyListeners();
  }

  void openApp() {
    DeviceApps.openApp(_app.packageName);
  }

  void onMenuSelection(int value) {
    switch (value) {
      case 0:
        shareResult();
        break;
      case 1:
        shareLog();
        break;
    }
  }
}
