// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  PatchedApplication? selectedApp;
  List<Patch> selectedPatches = [];
  List<String> removedPatches = [];

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
    final bool needsResourcePatching = await _patcherAPI.needsResourcePatching(
      selectedPatches,
    );
    if (needsResourcePatching && selectedApp != null) {
      final bool isSplit = await _managerAPI.isSplitApk(selectedApp!);
      return !isSplit;
    }
    return true;
  }

  Future<void> showPatchConfirmationDialog(BuildContext context) async {
    final bool isValid = await isValidPatchConfig();
    if (context.mounted) {
      if (isValid) {
        showArmv7WarningDialog(context);
      } else {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText('warning'),
            backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
            content: I18nText('patcherView.splitApkWarningDialogText'),
            actions: <Widget>[
              CustomMaterialButton(
                label: I18nText('noButton'),
                onPressed: () => Navigator.of(context).pop(),
              ),
              CustomMaterialButton(
                label: I18nText('yesButton'),
                isFilled: false,
                onPressed: () {
                  Navigator.of(context).pop();
                  showArmv7WarningDialog(context);
                },
              ),
            ],
          ),
        );
      }
    }
  }

  Future<void> showRemovedPatchesDialog(BuildContext context) async {
    if (removedPatches.isNotEmpty) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('notice'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText(
            'patcherView.removedPatchesWarningDialogText',
            translationParams: {'patches': removedPatches.join('\n')},
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
                Navigator.of(context).pop();
                navigateToInstaller();
              },
            ),
          ],
        ),
      );
    } else {
      showArmv7WarningDialog(context);
    }
  }

  Future<void> showArmv7WarningDialog(BuildContext context) async {
    final bool armv7 = await AboutInfo.getInfo().then((info) {
      final List<String> archs = info['supportedArch'];
      final supportedAbis = ['arm64-v8a', 'x86', 'x86_64'];
      return !archs.any((arch) => supportedAbis.contains(arch));
    });
    if (context.mounted && armv7) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('warning'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText('patcherView.armv7WarningDialogText'),
          actions: <Widget>[
            CustomMaterialButton(
              label: I18nText('noButton'),
              onPressed: () => Navigator.of(context).pop(),
            ),
            CustomMaterialButton(
              label: I18nText('yesButton'),
              isFilled: false,
              onPressed: () {
                Navigator.of(context).pop();
                navigateToInstaller();
              },
            ),
          ],
        ),
      );
    } else {
      navigateToInstaller();
    }
  }

  String getAppSelectionString() {
    String text = '${selectedApp!.name} (${selectedApp!.packageName})';
    if (text.length > 32) {
      text = '${text.substring(0, 32)}...)';
    }
    return text;
  }

  String getSuggestedVersionString(BuildContext context) {
    String suggestedVersion =
        _patcherAPI.getSuggestedVersion(selectedApp!.packageName);
    if (suggestedVersion.isEmpty) {
      suggestedVersion = FlutterI18n.translate(
        context,
        'appSelectorCard.allVersions',
      );
    } else {
      suggestedVersion = 'v$suggestedVersion';
    }
    return '${FlutterI18n.translate(
      context,
      'appSelectorCard.currentVersion',
    )}: v${selectedApp!.version}\n${FlutterI18n.translate(
      context,
      'appSelectorCard.suggestedVersion',
    )}: $suggestedVersion';
  }

  Future<void> loadLastSelectedPatches() async {
    this.selectedPatches.clear();
    removedPatches.clear();
    final List<String> selectedPatches =
        await _managerAPI.getSelectedPatches(selectedApp!.originalPackageName);
    final List<Patch> patches =
        _patcherAPI.getFilteredPatches(selectedApp!.originalPackageName);
    this
        .selectedPatches
        .addAll(patches.where((patch) => selectedPatches.contains(patch.name)));
    if (!_managerAPI.isPatchesChangeEnabled()) {
      this.selectedPatches.clear();
      this.selectedPatches.addAll(patches.where((patch) => !patch.excluded));
    }
    if (!_managerAPI.areExperimentalPatchesEnabled()) {
      this.selectedPatches.removeWhere((patch) => !isPatchSupported(patch));
    }
    if (!_managerAPI.areUniversalPatchesEnabled()) {
      this
          .selectedPatches
          .removeWhere((patch) => patch.compatiblePackages.isEmpty);
    }
    final usedPatches = _managerAPI.getUsedPatches(selectedApp!.originalPackageName);
    for (final patch in usedPatches){
      if (!patches.any((p) => p.name == patch.name)){
        removedPatches.add('\u2022 ${patch.name}');
      }
    }
    notifyListeners();
  }
}
