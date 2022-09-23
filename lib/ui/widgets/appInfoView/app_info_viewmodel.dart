// ignore_for_file: use_build_context_synchronously
import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:intl/intl.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/utils/string.dart';
import 'package:stacked/stacked.dart';

class AppInfoViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final RootAPI _rootAPI = RootAPI();

  Future<void> uninstallApp(
    BuildContext context,
    PatchedApplication app,
    bool onlyUnpatch,
  ) async {
    bool isUninstalled = true;
    if (app.isRooted) {
      bool hasRootPermissions = await _rootAPI.hasRootPermissions();
      if (hasRootPermissions) {
        await _rootAPI.deleteApp(app.packageName, app.apkFilePath);
        if (!onlyUnpatch) {
          await DeviceApps.uninstallApp(app.packageName);
        }
      }
    } else {
      isUninstalled = await DeviceApps.uninstallApp(app.packageName);
    }
    if (isUninstalled) {
      await _managerAPI.deletePatchedApp(app);
      locator<HomeViewModel>().initialize(context);
    }
  }

  void navigateToPatcher(PatchedApplication app) async {
    locator<PatcherViewModel>().selectedApp = app;
    locator<PatcherViewModel>().selectedPatches =
        await _patcherAPI.getAppliedPatches(app.appliedPatches);
    locator<PatcherViewModel>().notifyListeners();
    locator<NavigationViewModel>().setIndex(1);
  }

  Future<void> showUninstallDialog(
    BuildContext context,
    PatchedApplication app,
    bool onlyUnpatch,
  ) async {
    bool hasRootPermissions = await _rootAPI.hasRootPermissions();
    if (app.isRooted && !hasRootPermissions) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('appInfoView.rootDialogTitle'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText('appInfoView.rootDialogText'),
          actions: <Widget>[
            CustomMaterialButton(
              label: I18nText('okButton'),
              onPressed: () => Navigator.of(context).pop(),
            )
          ],
        ),
      );
    } else {
      if (onlyUnpatch) {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText(
              'appInfoView.unpatchButton',
            ),
            backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
            content: I18nText(
              'appInfoView.unpatchDialogText',
            ),
            actions: <Widget>[
              CustomMaterialButton(
                isFilled: false,
                label: I18nText('noButton'),
                onPressed: () => Navigator.of(context).pop(),
              ),
              CustomMaterialButton(
                label: I18nText('yesButton'),
                onPressed: () {
                  uninstallApp(context, app, onlyUnpatch);
                  Navigator.of(context).pop();
                  Navigator.of(context).pop();
                },
              )
            ],
          ),
        );
      } else {
        uninstallApp(context, app, onlyUnpatch);
        Navigator.of(context).pop();
      }
    }
  }

  String getPrettyDate(BuildContext context, DateTime dateTime) {
    return DateFormat.yMMMMd(Localizations.localeOf(context).languageCode)
        .format(dateTime);
  }

  String getPrettyTime(BuildContext context, DateTime dateTime) {
    return DateFormat.jm(Localizations.localeOf(context).languageCode)
        .format(dateTime);
  }

  Future<void> showAppliedPatchesDialog(
    BuildContext context,
    PatchedApplication app,
  ) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('appInfoView.appliedPatchesLabel'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: Text(getAppliedPatchesString(app.appliedPatches)),
        actions: <Widget>[
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () => Navigator.of(context).pop(),
          )
        ],
      ),
    );
  }

  String getAppliedPatchesString(List<String> appliedPatches) {
    List<String> names = appliedPatches
        .map((p) => p
            .replaceAll('-', ' ')
            .split('-')
            .join(' ')
            .toTitleCase()
            .replaceFirst('Microg', 'MicroG'))
        .toList();
    return '\u2022 ${names.join('\n\u2022 ')}';
  }

  void openApp(PatchedApplication app) {
    DeviceApps.openApp(app.packageName);
  }
}
