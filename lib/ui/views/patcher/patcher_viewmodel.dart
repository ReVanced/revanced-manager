import 'package:device_apps/device_apps.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  PatchedApplication? selectedApp;
  List<Patch> selectedPatches = [];

  void navigateToAppSelector() {
    _navigationService.navigateTo(Routes.appSelectorView);
  }

  void navigateToPatchesSelector() {
    _navigationService.navigateTo(Routes.patchesSelectorView);
  }

  void navigateToInstaller() {
    _navigationService.navigateTo(Routes.installerView);
  }

  bool showPatchButton() {
    return selectedPatches.isNotEmpty;
  }

  bool dimPatchesCard() {
    return selectedApp == null;
  }

  Future<bool> isValidPatchConfig() async {
    bool needsResourcePatching =
        await _patcherAPI.needsResourcePatching(selectedPatches);
    if (needsResourcePatching && selectedApp != null) {
      Application? app = await DeviceApps.getApp(selectedApp!.packageName);
      if (app != null && app.isSplit) {
        return false;
      }
    }
    return true;
  }

  Future<bool> isCompatibleApk() async {
    int minSdk = await _patcherAPI.getMinSdkApk(selectedApp!.apkFilePath);
    AndroidDeviceInfo androidInfo = await DeviceInfoPlugin().androidInfo;
    final deviceSdk = androidInfo.version.sdkInt ?? -2;
    return deviceSdk >= minSdk;
  }

  Future<void> showWarningDialogs(BuildContext context) async {
    bool isValid = await isValidPatchConfig();
    bool isCompatible = await isCompatibleApk();

    // Needs to check for mounted but this is a stateless widget, so we can't.
    // Fixed in a newer build of flutter: https://github.com/flutter/flutter/pull/111619
    // So I'm leaving it as is for the time being since it works.

    if (!isValid) {
      bool userContinued = await showInvalidPatchConfigDialog(context);
      if (!userContinued) return;
    }

    if (!isCompatible) {
      bool userContinued = await showIncompatibleApkDialog(context);
      if (!userContinued) return;
    }

    navigateToInstaller();
  }

  Future<bool> showInvalidPatchConfigDialog(BuildContext context) async {
    return await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('patcherView.patchDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('patcherView.patchDialogText'),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () => Navigator.of(context).pop(false),
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              Navigator.of(context).pop(true);
            },
          )
        ],
      ),
    );
  }

  Future<bool> showIncompatibleApkDialog(BuildContext context) async {
    return await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('patcherView.patchDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('patcherView.patchDialogCompatibleText'),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () => Navigator.of(context).pop(false),
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              Navigator.of(context).pop(true);
            },
          )
        ],
      ),
    );
  }

  String getAppSelectionString() {
    String text = '${selectedApp!.name} (${selectedApp!.packageName})';
    if (text.length > 32) {
      text = '${text.substring(0, 32)}...)';
    }
    return text;
  }

  String getRecommendedVersionString(BuildContext context) {
    String recommendedVersion =
        _patcherAPI.getRecommendedVersion(selectedApp!.packageName);
    if (recommendedVersion.isEmpty) {
      recommendedVersion = FlutterI18n.translate(
        context,
        'appSelectorCard.anyVersion',
      );
    } else {
      recommendedVersion = 'v$recommendedVersion';
    }
    return '${FlutterI18n.translate(
      context,
      'appSelectorCard.currentVersion',
    )}: v${selectedApp!.version}\n${FlutterI18n.translate(
      context,
      'appSelectorCard.recommendedVersion',
    )}: $recommendedVersion';
  }
}
