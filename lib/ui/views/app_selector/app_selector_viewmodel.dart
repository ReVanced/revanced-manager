import 'dart:io';

import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_file_dialog/flutter_file_dialog.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();
  final List<ApplicationWithIcon> apps = [];
  List<String> allApps = [];
  bool noApps = false;
  bool isRooted = false;
  int patchesCount(String packageName) {
    return _patcherAPI.getFilteredPatches(packageName).length;
  }

  List<Patch> patches = [];

  Future<void> initialize() async {
    patches = await _managerAPI.getPatches();
    isRooted = _managerAPI.isRooted;

    apps.addAll(
      await _patcherAPI
          .getFilteredInstalledApps(_managerAPI.areUniversalPatchesEnabled()),
    );
    apps.sort(
      (a, b) => _patcherAPI
          .getFilteredPatches(b.packageName)
          .length
          .compareTo(_patcherAPI.getFilteredPatches(a.packageName).length),
    );
    getAllApps();
    notifyListeners();
  }

  List<String> getAllApps() {
    allApps = patches
        .expand((e) => e.compatiblePackages.map((p) => p.name))
        .toSet()
        .where((name) => !apps.any((app) => app.packageName == name))
        .toList();
    noApps = allApps.isEmpty && apps.isEmpty;
    return allApps;
  }

  String getSuggestedVersion(String packageName) {
    return _patcherAPI.getSuggestedVersion(packageName);
  }

  Future<bool> checkSplitApk(String packageName) async {
    final app = await DeviceApps.getApp(packageName);
    if (app != null) {
      return app.isSplit;
    }
    return true;
  }

  Future<void> searchSuggestedVersionOnWeb({
    required String packageName,
  }) async {
    final String suggestedVersion = getSuggestedVersion(packageName);
    final String architecture = await AboutInfo.getInfo().then((info) {
      return info['supportedArch'][0];
    });

    if (suggestedVersion.isNotEmpty) {
      await openDefaultBrowser('$packageName apk version $suggestedVersion $architecture');
    } else {
      await openDefaultBrowser('$packageName apk $architecture');
    }
  }

  Future<void> openDefaultBrowser(String query) async {
    if (Platform.isAndroid) {
      try {
        const platform = MethodChannel('app.revanced.manager.flutter/browser');
        await platform.invokeMethod('openBrowser', {'query': query});
      } catch (e) {
        if (kDebugMode) {
          print(e);
        }
      }
    } else {
      throw 'Platform not supported';
    }
  }

  Future<void> selectApp(
    BuildContext context,
    ApplicationWithIcon application, [
    bool isFromStorage = false,
  ]) async {
    final String suggestedVersion =
        getSuggestedVersion(application.packageName);
    if (application.versionName != suggestedVersion &&
        suggestedVersion.isNotEmpty) {
      _managerAPI.suggestedAppVersionSelected = false;
      if (_managerAPI.isRequireSuggestedAppVersionEnabled() &&
          context.mounted) {
        return showRequireSuggestedAppVersionDialog(
          context,
          application.versionName!,
          suggestedVersion,
        );
      }
    } else {
      _managerAPI.suggestedAppVersionSelected = true;
    }
    locator<PatcherViewModel>().selectedApp = PatchedApplication(
      name: application.appName,
      packageName: application.packageName,
      version: application.versionName!,
      apkFilePath: application.apkFilePath,
      icon: application.icon,
      patchDate: DateTime.now(),
      isFromStorage: isFromStorage,
    );
    await locator<PatcherViewModel>().loadLastSelectedPatches();
    if (context.mounted) {
      Navigator.pop(context);
    }
  }

  Future<void> canSelectInstalled(
    BuildContext context,
    String packageName,
  ) async {
    final app =
        await DeviceApps.getApp(packageName, true) as ApplicationWithIcon?;
    if (app != null) {
      final bool isSplitApk = await checkSplitApk(packageName);
      if (isRooted || !isSplitApk) {
        if (context.mounted) {
          await selectApp(context, app);
        }
        final List<Option> requiredNullOptions = getNullRequiredOptions(
          locator<PatcherViewModel>().selectedPatches,
          packageName,
        );
        if (requiredNullOptions.isNotEmpty) {
          locator<PatcherViewModel>().showRequiredOptionDialog();
        }
      } else {
        if (context.mounted) {
          return showSelectFromStorageDialog(context);
        }
      }
    }
  }

  Future showRequireSuggestedAppVersionDialog(
    BuildContext context,
    String selectedVersion,
    String suggestedVersion,
  ) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.warning),
        content: Text(
          t.appSelectorView.requireSuggestedAppVersionDialogText(
            suggested: suggestedVersion,
            selected: selectedVersion,
          ),
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w500,
          ),
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  Future showSelectFromStorageDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (innerContext) => SimpleDialog(
        alignment: Alignment.center,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
        children: [
          const SizedBox(height: 10),
          Icon(
            Icons.block,
            size: 28,
            color: Theme.of(innerContext).colorScheme.primary,
          ),
          const SizedBox(height: 20),
          Text(
            t.appSelectorView.featureNotAvailable,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w600,
              wordSpacing: 1.5,
            ),
          ),
          const SizedBox(height: 20),
          Text(
            t.appSelectorView.featureNotAvailableText,
            style: const TextStyle(
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 30),
          FilledButton(
            onPressed: () async {
              Navigator.pop(innerContext);
              await selectAppFromStorage(context);
            },
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.sd_card),
                const SizedBox(width: 10),
                Text(t.appSelectorView.selectFromStorageButton),
              ],
            ),
          ),
          const SizedBox(height: 10),
          TextButton(
            onPressed: () {
              Navigator.pop(innerContext);
            },
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(width: 10),
                Text(t.cancelButton),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> selectAppFromStorage(BuildContext context) async {
    try {
      final String? result = await FlutterFileDialog.pickFile(
        params: const OpenFileDialogParams(
          mimeTypesFilter: ['application/vnd.android.package-archive'],
        ),
      );
      if (result != null) {
        final File apkFile = File(result);
        final List<String> pathSplit = result.split('/');
        pathSplit.removeLast();
        final Directory filePickerCacheDir = Directory(pathSplit.join('/'));
        final Iterable<File> deletableFiles =
            (await filePickerCacheDir.list().toList()).whereType<File>();
        for (final file in deletableFiles) {
          if (file.path != apkFile.path && file.path.endsWith('.apk')) {
            file.delete();
          }
        }
        final ApplicationWithIcon? application =
            await DeviceApps.getAppFromStorage(
          apkFile.path,
          true,
        ) as ApplicationWithIcon?;
        if (application != null && context.mounted) {
          await selectApp(context, application, true);
        }
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom(t.appSelectorView.errorMessage);
    }
  }

  List<ApplicationWithIcon> getFilteredApps(String query) {
    return apps
        .where(
          (app) =>
              query.isEmpty ||
              query.length < 2 ||
              app.appName.toLowerCase().contains(query.toLowerCase()) ||
              app.packageName.toLowerCase().contains(query.toLowerCase()),
        )
        .toList();
  }

  List<String> getFilteredAppsNames(String query) {
    return allApps
        .where(
          (app) =>
              query.isEmpty ||
              query.length < 2 ||
              app.toLowerCase().contains(query.toLowerCase()),
        )
        .toList();
  }

  void showDownloadToast() =>
      _toast.showBottom(t.appSelectorView.downloadToast);
}
