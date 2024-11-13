// ignore_for_file: use_build_context_synchronously
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:screenshot_callback/screenshot_callback.dart';
import 'package:stacked/stacked.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

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
  final Key logCustomScrollKey = UniqueKey();
  final ScrollController scrollController = ScrollController();
  final ScreenshotCallback screenshotCallback = ScreenshotCallback();
  double? progress = 0.0;
  String logs = '';
  String headerLogs = '';
  bool isRooted = false;
  bool isPatching = true;
  bool isInstalling = false;
  bool isInstalled = false;
  bool hasErrors = false;
  bool isCanceled = false;
  bool cancel = false;
  bool showPopupScreenshotWarning = true;

  bool showAutoScrollButton = false;
  bool _isAutoScrollEnabled = true;
  bool _isAutoScrolling = false;

  double get getCurrentScrollPercentage {
    final maxScrollExtent = scrollController.position.maxScrollExtent;
    final currentPosition = scrollController.position.pixels;

    return currentPosition / maxScrollExtent;
  }

  bool handleAutoScrollNotification(ScrollNotification event) {
    if (_isAutoScrollEnabled && event is ScrollStartNotification) {
      _isAutoScrollEnabled = _isAutoScrolling;
      showAutoScrollButton = false;
      notifyListeners();

      return true;
    }

    if (event is ScrollEndNotification) {
      const anchorThreshold = 0.987;

      _isAutoScrollEnabled =
          _isAutoScrolling || getCurrentScrollPercentage >= anchorThreshold;

      showAutoScrollButton = !_isAutoScrollEnabled && !_isAutoScrolling;
      notifyListeners();

      return true;
    }

    return false;
  }

  void scrollToBottom() {
    _isAutoScrolling = true;

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final maxScrollExtent = scrollController.position.maxScrollExtent;

      await scrollController.animateTo(
        maxScrollExtent,
        duration: const Duration(milliseconds: 100),
        curve: Curves.fastOutSlowIn,
      );

      _isAutoScrolling = false;
    });
  }

  Future<void> initialize(BuildContext context) async {
    isRooted = await _rootAPI.isRooted();
    if (await Permission.ignoreBatteryOptimizations.isGranted) {
      try {
        FlutterBackground.initialize(
          androidConfig: FlutterBackgroundAndroidConfig(
            notificationTitle: t.installerView.notificationTitle,
            notificationText: t.installerView.notificationText,
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
    await WakelockPlus.enable();
    await handlePlatformChannelMethods();
    await runPatcher(context);
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
    } else if (value == .85) {
      isPatching = false;
      hasErrors = false;
      await _managerAPI.savePatches(
        _patcherAPI.getFilteredPatches(_app.packageName),
        _app.packageName,
      );
      await _managerAPI.setUsedPatches(_patches, _app.packageName);
      _managerAPI.setLastUsedPatchesVersion(
        version: _managerAPI.patchesVersion,
      );
    } else if (value == -100.0) {
      isPatching = false;
      hasErrors = true;
      progress = 0.0;
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
      if (_isAutoScrollEnabled) {
        scrollToBottom();
      }
    }
    notifyListeners();
  }

  Future<void> runPatcher(BuildContext context) async {
    try {
      await _patcherAPI.runPatcher(
        _app.packageName,
        _app.apkFilePath,
        _patches,
        _app.isFromStorage,
      );
      _app.appliedPatches = _patches.map((p) => p.name).toList();
      if (_managerAPI.isLastPatchedAppEnabled()) {
        await _managerAPI.setLastPatchedApp(_app, _patcherAPI.outFile!);
      } else {
        _app.patchedFilePath = _patcherAPI.outFile!.path;
      }
      final homeViewModel = locator<HomeViewModel>();
      _managerAPI
          .reAssessPatchedApps()
          .then((_) => homeViewModel.getPatchedApps());
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
      await WakelockPlus.disable();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  void _trimLogs(List<String> logLines, String keyword, String? newString) {
    final lineCount = logLines.where((line) => line.endsWith(keyword)).length;
    final index = logLines.indexWhere((line) => line.endsWith(keyword));
    if (newString != null && lineCount > 0) {
      logLines.insert(
        index,
        newString.replaceAll('{lineCount}', lineCount.toString()),
      );
    }
    logLines.removeWhere((lines) => lines.endsWith(keyword));
  }

  dynamic _getPatchOptionValue(String patchName, Option option) {
    final Option? savedOption =
        _managerAPI.getPatchOption(_app.packageName, patchName, option.key);
    if (savedOption != null) {
      return savedOption.value;
    } else {
      return option.value;
    }
  }

  String _formatPatches(List<Patch> patches, String noneString) {
    return patches.isEmpty
        ? noneString
        : patches.map((p) {
            final optionsChanged = p.options
                .where((o) => _getPatchOptionValue(p.name, o) != o.value)
                .toList();
            return p.name +
                (optionsChanged.isEmpty
                    ? ''
                    : ' [${optionsChanged.map((o) => '${o.title}: ${_getPatchOptionValue(p.name, o)}').join(", ")}]');
          }).join(', ');
  }

  String _getSuggestedVersion(String packageName) {
    String suggestedVersion = _patcherAPI.getSuggestedVersion(_app.packageName);
    if (suggestedVersion.isEmpty) {
      suggestedVersion = 'Any';
    }
    return suggestedVersion;
  }

  Future<void> copyLogs() async {
    final info = await AboutInfo.getInfo();

    // Trim out extra lines
    final logsTrimmed = logs.split('\n');
    _trimLogs(logsTrimmed, 'succeeded', 'Applied {lineCount} patches');
    _trimLogs(logsTrimmed, '.dex', 'Compiled {lineCount} dex files');

    // Get patches added / removed
    final defaultPatches = _patcherAPI
        .getFilteredPatches(_app.packageName)
        .where((p) => !p.excluded)
        .toList();
    final appliedPatchesNames = _patches.map((p) => p.name).toList();

    final patchesAdded = _patches.where((p) => p.excluded).toList();
    final patchesRemoved = defaultPatches
        .where((p) => !appliedPatchesNames.contains(p.name))
        .map((p) => p.name)
        .toList();
    final patchesOptionsChanged = defaultPatches
        .where(
          (p) =>
              appliedPatchesNames.contains(p.name) &&
              p.options.any((o) => _getPatchOptionValue(p.name, o) != o.value),
        )
        .toList();

    // Add Info
    final formattedLogs = [
      '- Device Info',
      'ReVanced Manager: ${info['version']}',
      'Model: ${info['model']}',
      'Android version: ${info['androidVersion']}',
      'Supported architectures: ${info['supportedArch'].join(", ")}',
      'Root permissions: ${isRooted ? 'Yes' : 'No'}', //

      '\n- Patch Info',
      'App: ${_app.packageName} v${_app.version} (Suggested: ${_getSuggestedVersion(_app.packageName)})',
      'Patches version: ${_managerAPI.patchesVersion}',
      'Patches added: ${_formatPatches(patchesAdded, 'Default')}',
      'Patches removed: ${patchesRemoved.isEmpty ? 'None' : patchesRemoved.join(', ')}',
      'Default patch options changed: ${_formatPatches(patchesOptionsChanged, 'None')}', //

      '\n- Settings',
      'Allow changing patch selection: ${_managerAPI.isPatchesChangeEnabled()}',
      'Version compatibility check: ${_managerAPI.isVersionCompatibilityCheckEnabled()}',
      'Show universal patches: ${_managerAPI.areUniversalPatchesEnabled()}',
      'Patches source: ${_managerAPI.getPatchesRepo()}',

      '\n- Logs',
      logsTrimmed.join('\n'),
    ];

    Clipboard.setData(ClipboardData(text: formattedLogs.join('\n')));
    _toast.showBottom(t.installerView.copiedToClipboard);
  }

  Future<void> screenshotDetected(BuildContext context) async {
    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          t.warning,
        ),
        icon: const Icon(Icons.warning),
        content: SingleChildScrollView(
          child: Text(t.installerView.screenshotDetected),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
            },
            child: Text(t.noButton),
          ),
          FilledButton(
            onPressed: () {
              copyLogs();
              showPopupScreenshotWarning = true;
              Navigator.of(context).pop();
            },
            child: Text(t.yesButton),
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
        builder: (innerContext) => AlertDialog(
          title: Text(
            t.installerView.installType,
          ),
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
                      child: Text(
                        t.installerView.installTypeDescription,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                          color: Theme.of(context).colorScheme.secondary,
                        ),
                      ),
                    ),
                    RadioListTile(
                      title: Text(t.installerView.installNonRootType),
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 16),
                      value: 0,
                      groupValue: value,
                      onChanged: (selected) {
                        installType.value = selected!;
                      },
                    ),
                    RadioListTile(
                      title: Text(t.installerView.installRootType),
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 16),
                      value: 1,
                      groupValue: value,
                      onChanged: (selected) {
                        installType.value = selected!;
                      },
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Text(
                        t.installerView.warning,
                        style: TextStyle(
                          fontWeight: FontWeight.w500,
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(innerContext).pop();
              },
              child: Text(t.cancelButton),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(innerContext).pop();
                installResult(context, installType.value == 1);
              },
              child: Text(t.installerView.installButton),
            ),
          ],
        ),
      );
    } else {
      await showDialog(
        context: context,
        barrierDismissible: false,
        builder: (innerContext) => AlertDialog(
          title: Text(t.warning),
          contentPadding: const EdgeInsets.all(16),
          content: Text(t.installerView.warning),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(innerContext).pop();
              },
              child: Text(t.cancelButton),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(innerContext).pop();
                installResult(context, false);
              },
              child: Text(t.installerView.installButton),
            ),
          ],
        ),
      );
    }
  }

  Future<void> stopPatcher() async {
    try {
      isCanceled = true;
      update(0.5, 'Canceling...', 'Canceling patching process');
      await _patcherAPI.stopPatcher();
      await WakelockPlus.disable();
      update(-100.0, 'Canceled...', 'Press back to exit');
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> installResult(BuildContext context, bool installAsRoot) async {
    isInstalling = true;
    try {
      _app.isRooted = installAsRoot;
      if (headerLogs != 'Installing...') {
        update(
          .85,
          'Installing...',
          _app.isRooted ? 'Mounting patched app' : 'Installing patched app',
        );
      }
      final int response = await _patcherAPI.installPatchedFile(context, _app);
      if (response == 0) {
        isInstalled = true;
        _app.isFromStorage = false;
        _app.patchDate = DateTime.now();

        // In case a patch changed the app name or package name,
        // update the app info.
        final app =
            await DeviceApps.getAppFromStorage(_patcherAPI.outFile!.path);
        if (app != null) {
          _app.name = app.appName;
          _app.packageName = app.packageName;
        }
        await _managerAPI.savePatchedApp(_app);

        _managerAPI
            .reAssessPatchedApps()
            .then((_) => locator<HomeViewModel>().getPatchedApps());

        update(1.0, 'Installed', 'Installed');
      } else if (response == 3) {
        update(
          .85,
          'Installation canceled',
          'Installation canceled',
        );
      } else if (response == 10) {
        installResult(context, installAsRoot);
      } else {
        update(
          .85,
          'Installation failed',
          'Installation failed',
        );
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
    isInstalling = false;
  }

  void exportResult() {
    try {
      _patcherAPI.exportPatchedFile(_app);
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

  Future<void> onPopAttempt(BuildContext context) async {
    if (!cancel) {
      cancel = true;
      _toast.showBottom(t.installerView.pressBackAgain);
    } else if (!isCanceled) {
      await stopPatcher();
    } else {
      _toast.showBottom(t.installerView.noExit);
    }
  }

  void onPop() {
    if (!cancel) {
      cleanPatcher();
    } else {
      _patcherAPI.cleanPatcher();
    }
    ScreenshotCallback().dispose();
  }
}
