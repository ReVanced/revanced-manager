import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:intl/intl.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/utils/string.dart';
import 'package:stacked/stacked.dart';

class AppInfoViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final RootAPI _rootAPI = RootAPI();

  void uninstallApp(PatchedApplication app) {
    if (app.isRooted) {
      _rootAPI.deleteApp(app.packageName, app.apkFilePath);
      _managerAPI.deletePatchedApp(app);
    } else {
      DeviceApps.uninstallApp(app.packageName);
      _managerAPI.deletePatchedApp(app);
    }
  }

  void navigateToPatcher(PatchedApplication app) async {
    locator<PatcherViewModel>().selectedApp = app;
    locator<PatcherViewModel>().selectedPatches =
        await _patcherAPI.getAppliedPatches(app.appliedPatches);
    locator<PatcherViewModel>().notifyListeners();
    locator<NavigationViewModel>().setIndex(1);
  }

  Future<void> showUninstallAlertDialog(
    BuildContext context,
    PatchedApplication app,
  ) async {
    bool hasRootPermissions = await _rootAPI.hasRootPermissions();
    if (app.isRooted && !hasRootPermissions) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('appInfoView.alertDialogTitle'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText('appInfoView.errorDialogText'),
          actions: [
            CustomMaterialButton(
              label: I18nText('okButton'),
              onPressed: () => Navigator.of(context).pop(),
            )
          ],
        ),
      );
    } else {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('appInfoView.alertDialogTitle'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText('appInfoView.alertDialogText'),
          actions: [
            CustomMaterialButton(
              isFilled: false,
              label: I18nText('cancelButton'),
              onPressed: () => Navigator.of(context).pop(),
            ),
            CustomMaterialButton(
              label: I18nText('okButton'),
              onPressed: () {
                uninstallApp(app);
                locator<NavigationViewModel>().notifyListeners();
                Navigator.of(context).pop();
                Navigator.of(context).pop();
              },
            )
          ],
        ),
      );
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
        actions: [
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
