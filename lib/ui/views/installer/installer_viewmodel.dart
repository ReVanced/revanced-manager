// ignore_for_file: use_build_context_synchronously
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
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
import 'package:revanced_manager/utils/about_info.dart';
import 'package:screenshot_callback/screenshot_callback.dart';
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
  final ScreenshotCallback screenshotCallback = ScreenshotCallback();
  double? progress = 0.0;
  String logs = '';
  String headerLogs = '';
  bool isRooted = false;
  bool isPatching = true;
  bool isInstalled = false;
  bool hasErrors = false;
  bool isCanceled = false;
  bool cancel = false;
  bool showPopupScreenshotWarning = true;

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
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        } // ignore
      }
    }
    screenshotCallback.addListener(() {
      if (showPopupScreenshotWarning) {
        showPopupScreenshotWarning = false;
        screenshotDetected(context);
      }
    });
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

  Future<void> update(double value, String header, String log) async {
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
      await _managerAPI.savePatches(
        _patcherAPI.getFilteredPatches(_app.packageName),
        _app.packageName,
      );
      await _managerAPI.setUsedPatches(_patches, _app.packageName);
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
      await _patcherAPI.runPatcher(
        _app.packageName,
        _app.apkFilePath,
        _patches,
      );
    } on Exception catch (e) {
      update(
        -100.0,
        'Failed...',
        'Something went wrong:\n$e',
      );
      if (kDebugMode) {
        print(e);
      }
    }

    // Necessary to reset the state of patches so that they
    // can be reloaded again.
   _managerAPI.patches.clear();
    await _patcherAPI.loadPatches();

    try {
      if (FlutterBackground.isBackgroundExecutionEnabled) {
        try {
          FlutterBackground.disableBackgroundExecution();
        } on Exception catch (e) {
          if (kDebugMode) {
            print(e);
          } // ignore
        }
      }
      await Wakelock.disable();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> copyLogs() async {
    final info = await AboutInfo.getInfo();

    final formattedLogs = [
      '```',
      '~ Device Info',
      'ReVanced Manager: ${info['version']}',
      'Build: ${info['flavor']}',
      'Model: ${info['model']}',
      'Android version: ${info['androidVersion']}',
      'Supported architectures: ${info['supportedArch'].join(", ")}',

      '\n~ Patch Info',
      'App: ${_app.packageName} v${_app.version}',
      'Patches version: ${_managerAPI.patchesVersion}',
      'Patches: ${_patches.map((p) => p.name).toList().join(", ")}',

      '\n~ Settings',
      'Allow changing patch selection: ${_managerAPI.isPatchesChangeEnabled()}',
      'Show universal patches: ${_managerAPI.areUniversalPatchesEnabled()}',
      'Version compatibility check: ${_managerAPI.isVersionCompatibilityCheckEnabled()}',
      'Patches source: ${_managerAPI.getPatchesRepo()}',
      'Integration source: ${_managerAPI.getIntegrationsRepo()}',

      '\n~ Logs',
      logs,
      '```',
    ];

    Clipboard.setData(ClipboardData(text: formattedLogs.join('\n')));
    _toast.showBottom('installerView.copiedToClipboard');
  }

  Future<void> screenshotDetected(BuildContext context) async {
    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText(
          'warning',
        ),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        icon: const Icon(Icons.warning),
        content: SingleChildScrollView(
          child: I18nText('installerView.screenshotDetected'),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              copyLogs();
              showPopupScreenshotWarning = true;
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
    );
  }

  Future<void> installTypeDialog(BuildContext context) async {
    final ValueNotifier<int> installType = ValueNotifier(0);
    if (isRooted) {
      await showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => AlertDialog(
          title: I18nText(
            'installerView.installType',
          ),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          icon: const Icon(Icons.file_download_outlined),
          contentPadding: const EdgeInsets.symmetric(vertical: 16),
          content: SingleChildScrollView(
            child: ValueListenableBuilder(
              valueListenable: installType,
              builder: (context, value, child) {
                return Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 20,
                        vertical: 10,
                      ),
                      child: I18nText(
                        'installerView.installTypeDescription',
                        child: Text(
                          '',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).colorScheme.secondary,
                          ),
                        ),
                      ),
                    ),
                    RadioListTile(
                      title: I18nText('installerView.installNonRootType'),
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 16),
                      value: 0,
                      groupValue: value,
                      onChanged: (selected) {
                        installType.value = selected!;
                      },
                    ),
                    RadioListTile(
                      title: I18nText('installerView.installRootType'),
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 16),
                      value: 1,
                      groupValue: value,
                      onChanged: (selected) {
                        installType.value = selected!;
                      },
                    ),
                  ],
                );
              },
            ),
          ),
          actions: [
            CustomMaterialButton(
              label: I18nText('cancelButton'),
              isFilled: false,
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            CustomMaterialButton(
              label: I18nText('installerView.installButton'),
              onPressed: () {
                Navigator.of(context).pop();
                installResult(context, installType.value == 1);
              },
            ),
          ],
        ),
      );
    } else {
      installResult(context, false);
    }
  }

  Future<void> stopPatcher() async {
    try {
      isCanceled = true;
      update(0.5, 'Canceling...', 'Canceling patching process');
      await _patcherAPI.stopPatcher();
      update(-100.0, 'Canceled...', 'Press back to exit');
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> installResult(BuildContext context, bool installAsRoot) async {
    try {
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
        _app.isFromStorage = false;
        _app.patchDate = DateTime.now();
        _app.appliedPatches = _patches.map((p) => p.name).toList();

        // In case a patch changed the app name or package name,
        // update the app info.
        final app =
            await DeviceApps.getAppFromStorage(_patcherAPI.outFile!.path);
        if (app != null) {
          _app.name = app.appName;
          _app.packageName = app.packageName;
        }

        await _managerAPI.savePatchedApp(_app);

        update(1.0, 'Installed!', 'Installed!');
      } else {
        // TODO(aabed): Show error message.
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  void exportResult() {
    try {
      _patcherAPI.exportPatchedFile(_app.name, _app.version);
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> cleanPatcher() async {
    try {
      _patcherAPI.cleanPatcher();
      locator<PatcherViewModel>().selectedApp = null;
      locator<PatcherViewModel>().selectedPatches.clear();
      locator<PatcherViewModel>().notifyListeners();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  void openApp() {
    DeviceApps.openApp(_app.packageName);
  }

  void onButtonPressed(int value) {
    switch (value) {
      case 0:
        exportResult();
        break;
      case 1:
        copyLogs();
        break;
    }
  }

  Future<bool> onWillPop(BuildContext context) async {
    if (isPatching) {
      if (!cancel) {
        cancel = true;
        _toast.showBottom('installerView.pressBackAgain');
      } else if (!isCanceled) {
        await stopPatcher();
      } else {
        _toast.showBottom('installerView.noExit');
      }
      return false;
    }
    if (!cancel) {
      cleanPatcher();
    } else {
      _patcherAPI.cleanPatcher();
    }
    screenshotCallback.dispose();
    Navigator.of(context).pop();
    return true;
  }
}
