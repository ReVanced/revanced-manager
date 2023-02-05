// ignore_for_file: use_build_context_synchronously
import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:stacked/stacked.dart';
import 'package:wakelock/wakelock.dart';

class InstallerViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final RootAPI _rootAPI = RootAPI();
  final Toast _toast = locator<Toast>();
  final PatchedApplication _app = locator<PatcherViewModel>().selectedApp!;
  final List<Patch> _patches = locator<PatcherViewModel>().selectedPatches;
  static const _installerChannel = MethodChannel(
    'app.revanced.manager.flutter/installer',
  );
  final ScrollController scrollController = ScrollController();
  double? progress = 0.0;
  String logs = '';
  String headerLogs = '';
  bool isRooted = false;
  bool isPatching = true;
  bool isInstalled = false;
  bool hasErrors = false;

  Future<void> initialize(BuildContext context) async {
    isRooted = await _rootAPI.isRooted();
    if (await Permission.ignoreBatteryOptimizations.isGranted) {
      try {
        FlutterBackground.initialize(
          androidConfig: FlutterBackgroundAndroidConfig(
            notificationTitle: FlutterI18n.translate(
              context,
              'installerView.notificationTitle',
            ),
            notificationText: FlutterI18n.translate(
              context,
              'installerView.notificationText',
            ),
            notificationIcon: const AndroidResource(
              name: 'ic_notification',
            ),
          ),
        ).then((value) => FlutterBackground.enableBackgroundExecution());
      } on Exception catch (e, s) {
        await Sentry.captureException(e, stackTrace: s);
        // ignore
      }
    }
    await Wakelock.enable();
    await handlePlatformChannelMethods();
    await runPatcher();
  }

  Future<dynamic> handlePlatformChannelMethods() async {
    _installerChannel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'update':
          if (call.arguments != null) {
            final Map<dynamic, dynamic> arguments = call.arguments;
            final double progress = arguments['progress'];
            final String header = arguments['header'];
            final String log = arguments['log'];
            update(progress, header, log);
          }
          break;
      }
    });
  }

  void update(double value, String header, String log) {
    if (value >= 0.0) {
      progress = value;
    }
    if (value == 0.0) {
      logs = '';
      isPatching = true;
      isInstalled = false;
      hasErrors = false;
    } else if (value == 1.0) {
      isPatching = false;
      hasErrors = false;
    } else if (value == -100.0) {
      isPatching = false;
      hasErrors = true;
    }
    if (header.isNotEmpty) {
      headerLogs = header;
    }
    if (log.isNotEmpty && !log.startsWith('Merging L')) {
      if (logs.isNotEmpty) {
        logs += '\n';
      }
      logs += log;
      if (logs[logs.length - 1] == '\n') {
        logs = logs.substring(0, logs.length - 1);
      }
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
    try {
      update(0.0, 'Initializing...', 'Initializing installer');
      if (_patches.isNotEmpty) {
        try {
          update(0.1, '', 'Creating working directory');
          await _patcherAPI.runPatcher(
            _app.packageName,
            _app.apkFilePath,
            _patches,
          );
        } on Exception catch (e, s) {
          update(
            -100.0,
            'Aborting...',
            'An error occurred! Aborting\nError:\n$e',
          );
          await Sentry.captureException(e, stackTrace: s);
          throw await Sentry.captureException(e, stackTrace: s);
        }
      } else {
        update(-100.0, 'Aborting...', 'No app or patches selected! Aborting');
      }
      if (FlutterBackground.isBackgroundExecutionEnabled) {
        try {
          FlutterBackground.disableBackgroundExecution();
        } on Exception catch (e, s) {
          await Sentry.captureException(e, stackTrace: s);
          // ignore
        }
      }
      await Wakelock.disable();
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
    }
  }

  Future<void> installResult(BuildContext context, bool installAsRoot) async {
    try {
      _app.isRooted = installAsRoot;
      final bool hasMicroG =
          _patches.any((p) => p.name.endsWith('microg-support'));
      final bool rootMicroG = installAsRoot && hasMicroG;
      final bool rootFromStorage = installAsRoot && _app.isFromStorage;
      final bool ytWithoutRootMicroG =
          !installAsRoot && !hasMicroG && _app.packageName.contains('youtube');
      if (rootMicroG || rootFromStorage || ytWithoutRootMicroG) {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText('installerView.installErrorDialogTitle'),
            backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
            content: I18nText(
              rootMicroG
                  ? 'installerView.installErrorDialogText1'
                  : rootFromStorage
                      ? 'installerView.installErrorDialogText3'
                      : 'installerView.installErrorDialogText2',
            ),
            actions: <Widget>[
              CustomMaterialButton(
                label: I18nText('okButton'),
                onPressed: () => Navigator.of(context).pop(),
              )
            ],
          ),
        );
      } else {
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
          _app.isFromStorage = false;
          _app.patchDate = DateTime.now();
          _app.appliedPatches = _patches.map((p) => p.name).toList();
          if (hasMicroG) {
            _app.name += ' ReVanced';
            _app.packageName = _app.packageName.replaceFirst(
              'com.google.',
              'app.revanced.',
            );
          }
          await _managerAPI.savePatchedApp(_app);
        }
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
    }
  }

  void exportResult() {
    try {
      _patcherAPI.exportPatchedFile(_app.name, _app.version);
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  void shareResult() {
    try {
      _patcherAPI.sharePatchedFile(_app.name, _app.version);
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  void shareLog() {
    _patcherAPI.sharePatcherLog(logs);
  }

  Future<void> cleanPatcher() async {
    try {
      _patcherAPI.cleanPatcher();
      locator<PatcherViewModel>().selectedApp = null;
      locator<PatcherViewModel>().selectedPatches.clear();
      locator<PatcherViewModel>().notifyListeners();
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
    }
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
        exportResult();
        break;
      case 2:
        shareLog();
        break;
    }
  }

  Future<bool> onWillPop(BuildContext context) async {
    if (isPatching) {
      _toast.showBottom('installerView.noExit');
      return false;
    }
    cleanPatcher();
    Navigator.of(context).pop();
    return true;
  }
}
