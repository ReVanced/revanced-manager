// ignore_for_file: use_build_context_synchronously
import 'dart:math';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:intl/intl.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:stacked/stacked.dart';

class AppInfoViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final RootAPI _rootAPI = RootAPI();
  final Toast _toast = locator<Toast>();

  Future<void> installApp(
    BuildContext context,
    PatchedApplication app,
  ) async {
    app.isRooted = await _managerAPI.installTypeDialog(context);
    final int statusCode = await _patcherAPI.installPatchedFile(context, app);
    if (statusCode == 0) {
      locator<HomeViewModel>().initialize(context);
    }
  }

  Future<void> exportApp(
    PatchedApplication app,
  ) async {
    _patcherAPI.exportPatchedFile(app);
  }

  Future<void> uninstallApp(
    BuildContext context,
    PatchedApplication app,
    bool onlyUnpatch,
  ) async {
    var isUninstalled = onlyUnpatch;

    if (!onlyUnpatch) {
      // TODO(Someone): Wait for the app to uninstall successfully.
      isUninstalled = await DeviceApps.uninstallApp(app.packageName);
    }

    if (isUninstalled && app.isRooted &&  await _rootAPI.hasRootPermissions()) {
      await _rootAPI.uninstall(app.packageName);
    }

    if (isUninstalled) {
      await _managerAPI.deletePatchedApp(app);
      locator<HomeViewModel>().initialize(context);
    }
  }

  Future<void> navigateToPatcher(PatchedApplication app) async {
    locator<PatcherViewModel>().selectedApp = app;
    locator<PatcherViewModel>().selectedPatches =
        await _patcherAPI.getAppliedPatches(app.appliedPatches);
    locator<PatcherViewModel>().notifyListeners();
    locator<NavigationViewModel>().setIndex(1);
  }

  void updateNotImplemented(BuildContext context) {
    _toast.showBottom('appInfoView.updateNotImplemented');
  }

  Future<void> showUninstallDialog(
    BuildContext context,
    PatchedApplication app,
    bool onlyUnpatch,
  ) async {
    final bool hasRootPermissions = await _rootAPI.hasRootPermissions();
    if (app.isRooted && !hasRootPermissions) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('appInfoView.rootDialogTitle'),
          content: I18nText('appInfoView.rootDialogText'),
          actions: <Widget>[
            FilledButton(
              onPressed: () => Navigator.of(context).pop(),
              child: I18nText('okButton'),
            ),
          ],
        ),
      );
    } else {
      if (onlyUnpatch) {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText(
              'appInfoView.unmountButton',
            ),
            content: I18nText(
              'appInfoView.unmountDialogText',
            ),
            actions: <Widget>[
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: I18nText('noButton'),
              ),
              FilledButton(
                onPressed: () {
                  uninstallApp(context, app, true);
                  Navigator.of(context).pop();
                  Navigator.of(context).pop();
                },
                child: I18nText('yesButton'),
              ),
            ],
          ),
        );
      } else {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText(
              'appInfoView.uninstallButton',
            ),
            content: I18nText(
              'appInfoView.uninstallDialogText',
            ),
            actions: <Widget>[
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: I18nText('noButton'),
              ),
              FilledButton(
                onPressed: () {
                  uninstallApp(context, app, false);
                  Navigator.of(context).pop();
                  Navigator.of(context).pop();
                },
                child: I18nText('yesButton'),
              ),
            ],
          ),
        );
      }
    }
  }

  Future<void> showDeleteDialog(
    BuildContext context,
    PatchedApplication app,
  ) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('appInfoView.removeAppDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('appInfoView.removeAppDialogText'),
        actions: <Widget>[
          OutlinedButton(
            child: I18nText('cancelButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          FilledButton(
            child: I18nText('okButton'),
            onPressed: () => {
              _managerAPI.deleteLastPatchedApp(),
              Navigator.of(context)..pop()..pop(),
              locator<HomeViewModel>().initialize(context),
            },
          ),
        ],
      ),
    );
  }

  String getPrettyDate(BuildContext context, DateTime dateTime) {
    return DateFormat.yMMMMd(Localizations.localeOf(context).languageCode)
        .format(dateTime);
  }

  String getPrettyTime(BuildContext context, DateTime dateTime) {
    return DateFormat.jm(Localizations.localeOf(context).languageCode)
        .format(dateTime);
  }

  String getFileSizeString(int bytes) {
    const suffixes = ['B', 'KB', 'MB', 'GB', 'TB'];
    final i = (log(bytes) / log(1024)).floor();
    return '${(bytes / pow(1024, i)).toStringAsFixed(2)} ${suffixes[i]}';
  }

  Future<void> showAppliedPatchesDialog(
    BuildContext context,
    PatchedApplication app,
  ) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('appInfoView.appliedPatchesLabel'),
        content: SingleChildScrollView(
          child: Text(getAppliedPatchesString(app.appliedPatches)),
        ),
        actions: <Widget>[
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: I18nText('okButton'),
          ),
        ],
      ),
    );
  }

  String getAppliedPatchesString(List<String> appliedPatches) {
    return '• ${appliedPatches.join('\n• ')}';
  }

  void openApp(PatchedApplication app) {
    DeviceApps.openApp(app.packageName);
  }
}
