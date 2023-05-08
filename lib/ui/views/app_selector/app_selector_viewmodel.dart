import 'dart:io';

import 'package:device_apps/device_apps.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final RevancedAPI _revancedAPI = locator<RevancedAPI>();
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
    patches = await _revancedAPI.getPatches();
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
    noApps = apps.isEmpty;
    getAllApps();

    notifyListeners();
  }

  List<String> getAllApps() {
    allApps = patches
        .expand((e) => e.compatiblePackages.map((p) => p.name))
        .toSet()
        .where((name) => !apps.any((app) => app.packageName == name))
        .toList();

    return allApps;
  }

  String getSuggestedVersion(String packageName) {
    return _patcherAPI.getSuggestedVersion(packageName);
  }

  Future<void> selectApp(ApplicationWithIcon application) async {
    locator<PatcherViewModel>().selectedApp = PatchedApplication(
      name: application.appName,
      packageName: application.packageName,
      originalPackageName: application.packageName,
      version: application.versionName!,
      apkFilePath: application.apkFilePath,
      icon: application.icon,
      patchDate: DateTime.now(),
    );
    locator<PatcherViewModel>().loadLastSelectedPatches();
  }

  Future showSelectFromStorageDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => SimpleDialog(
        alignment: Alignment.center,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
        children: [
          const SizedBox(height: 10),
          Icon(
            Icons.block,
            size: 28,
            color: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(height: 20),
          I18nText(
            'appSelectorView.featureNotAvailable',
            child: const Text(
              '',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
                wordSpacing: 1.5,
              ),
            ),
          ),
          const SizedBox(height: 20),
          I18nText(
            'appSelectorView.featureNotAvailableText',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 14,
              ),
            ),
          ),
          const SizedBox(height: 30),
          CustomMaterialButton(
            onPressed: () => selectAppFromStorage(context).then(
              (_) {
                Navigator.pop(context);
                Navigator.pop(context);
              },
            ),
            label: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.sd_card),
                const SizedBox(width: 10),
                I18nText('appSelectorView.selectFromStorageButton'),
              ],
            ),
          ),
          const SizedBox(height: 10),
          CustomMaterialButton(
            isFilled: false,
            onPressed: () {
              Navigator.pop(context);
            },
            label: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(width: 10),
                I18nText('cancelButton'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> selectAppFromStorage(BuildContext context) async {
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['apk'],
      );
      if (result != null && result.files.single.path != null) {
        final File apkFile = File(result.files.single.path!);
        final List<String> pathSplit = result.files.single.path!.split('/');
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
        if (application != null) {
          locator<PatcherViewModel>().selectedApp = PatchedApplication(
            name: application.appName,
            packageName: application.packageName,
            originalPackageName: application.packageName,
            version: application.versionName!,
            apkFilePath: result.files.single.path!,
            icon: application.icon,
            patchDate: DateTime.now(),
            isFromStorage: true,
          );
          locator<PatcherViewModel>().loadLastSelectedPatches();
        }
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom('appSelectorView.errorMessage');
    }
  }

  List<ApplicationWithIcon> getFilteredApps(String query) {
    return apps
        .where(
          (app) =>
              query.isEmpty ||
              query.length < 2 ||
              app.appName.toLowerCase().contains(query.toLowerCase()),
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
      _toast.showBottom('appSelectorView.downloadToast');
}
