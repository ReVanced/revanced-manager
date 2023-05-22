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
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
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
                  showArmv7WarningDialog(context);
                },
              )
            ],
          ),
        );
      }
    }
  }

  Future<void> showArmv7WarningDialog(BuildContext context) async {
    final bool armv7 = await AboutInfo.getInfo().then((info) {
      final List<String> archs = info['arch'];
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
            )
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
        'appSelectorCard.anyVersion',
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
    final List<String> selectedPatches =
        await _managerAPI.getSelectedPatches(selectedApp!.originalPackageName);
    final List<Patch> patches =
        _patcherAPI.getFilteredPatches(selectedApp!.originalPackageName);
    this
        .selectedPatches
        .addAll(patches.where((patch) => selectedPatches.contains(patch.name)));
    notifyListeners();
  }
}
