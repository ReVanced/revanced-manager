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
    bool needsResourcePatching = await _patcherAPI.needsResourcePatching(
      selectedPatches,
    );
    if (needsResourcePatching && selectedApp != null) {
      bool isSplit = await _managerAPI.isSplitApk(selectedApp!);
      return !isSplit;
    }
    return true;
  }

  Future<void> showPatchConfirmationDialog(BuildContext context) async {
    bool isValid = await isValidPatchConfig();
    if (isValid) {
      navigateToInstaller();
    } else {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('patcherView.patchDialogTitle'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText('patcherView.patchDialogText'),
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
            )
          ],
        ),
      );
    }
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
