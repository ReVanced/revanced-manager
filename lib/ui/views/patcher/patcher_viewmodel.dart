// ignore_for_file: use_build_context_synchronously

import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  Set<String> savedPatchNames = {};
  PatchedApplication? selectedApp;
  BuildContext? ctx;
  List<Patch> selectedPatches = [];
  List<String> removedPatches = [];
  List<String> newPatches = [];

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

  bool showRemovedPatchesDialog(BuildContext context) {
    if (removedPatches.isNotEmpty) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.notice),
          content: SingleChildScrollView(
            child: Text(
              t.patcherView.removedPatchesWarningDialogText(
                patches: removedPatches.join('\n'),
                newPatches: newPatches.isNotEmpty
                    ? t.patcherView.addedPatchesDialogText(
                        addedPatches: newPatches.join('\n'),
                      )
                    : '',
              ),
            ),
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
                Navigator.of(context).pop();
                showIncompatibleArchWarningDialog(context);
              },
              child: Text(t.yesButton),
            ),
          ],
        ),
      );
      return false;
    }
    return true;
  }

  bool checkRequiredPatchOption(BuildContext context) {
    if (getNullRequiredOptions(selectedPatches, selectedApp!.packageName)
        .isNotEmpty) {
      showRequiredOptionDialog(context);
      return false;
    }
    return true;
  }

  void showRequiredOptionDialog([context]) {
    showDialog(
      context: context ?? ctx,
      builder: (context) => AlertDialog(
        title: Text(t.notice),
        content: Text(t.patcherView.requiredOptionDialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => {
              Navigator.of(context).pop(),
            },
            child: Text(t.cancelButton),
          ),
          FilledButton(
            onPressed: () => {
              Navigator.pop(context),
              navigateToPatchesSelector(),
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  Future<void> showIncompatibleArchWarningDialog(BuildContext context) async {
    final bool notSupported = await AboutInfo.getInfo().then((info) {
      final List<String> archs = info['supportedArch'];
      final supportedAbis = ['arm64-v8a', 'x86', 'x86_64', 'armeabi-v7a'];
      return !archs.any((arch) => supportedAbis.contains(arch));
    });
    if (context.mounted && notSupported) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.warning),
          content: Text(t.patcherView.incompatibleArchWarningDialogText),
          actions: <Widget>[
            FilledButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text(t.noButton),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                navigateToInstaller();
              },
              child: Text(t.yesButton),
            ),
          ],
        ),
      );
    } else {
      navigateToInstaller();
    }
  }

  String getAppSelectionString() {
    return '${selectedApp!.name} ${selectedApp!.version}';
  }

  Future<void> queryVersion(String suggestedVersion) async {
    await openDefaultBrowser(
      '${selectedApp!.packageName} apk version $suggestedVersion',
    );
  }

  String getSuggestedVersionString(BuildContext context) {
    return _patcherAPI.getSuggestedVersion(selectedApp!.packageName);
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

  bool isPatchNew(Patch patch) {
    if (savedPatchNames.isEmpty) {
      savedPatchNames = _managerAPI
          .getSavedPatches(selectedApp!.packageName)
          .map((p) => p.name)
          .toSet();
    }
    if (savedPatchNames.isEmpty) {
      return false;
    }
    return !savedPatchNames.contains(patch.name);
  }

  Future<void> loadLastSelectedPatches() async {
    this.selectedPatches.clear();
    removedPatches.clear();
    newPatches.clear();
    final List<String> selectedPatches =
        await _managerAPI.getSelectedPatches(selectedApp!.packageName);
    final List<Patch> patches =
        _patcherAPI.getFilteredPatches(selectedApp!.packageName);
    this
        .selectedPatches
        .addAll(patches.where((patch) => selectedPatches.contains(patch.name)));
    if (!_managerAPI.isPatchesChangeEnabled()) {
      this.selectedPatches.clear();
      this.selectedPatches.addAll(patches.where((patch) => !patch.excluded));
    }
    if (_managerAPI.isVersionCompatibilityCheckEnabled()) {
      this.selectedPatches.removeWhere((patch) => !isPatchSupported(patch));
    }
    if (!_managerAPI.areUniversalPatchesEnabled()) {
      this
          .selectedPatches
          .removeWhere((patch) => patch.compatiblePackages.isEmpty);
    }
    this.selectedPatches.addAll(
          patches.where(
            (patch) =>
                isPatchNew(patch) &&
                !patch.excluded &&
                !this.selectedPatches.contains(patch),
          ),
        );
    final usedPatches = _managerAPI.getUsedPatches(selectedApp!.packageName);
    for (final patch in usedPatches) {
      if (!patches.any((p) => p.name == patch.name)) {
        removedPatches.add('• ${patch.name}');
        for (final option in patch.options) {
          _managerAPI.clearPatchOption(
            selectedApp!.packageName,
            patch.name,
            option.key,
          );
        }
      }
    }
    for (final patch in patches) {
      if (isPatchNew(patch)) {
        newPatches.add('• ${patch.name}');
      }
    }
    notifyListeners();
  }
}
