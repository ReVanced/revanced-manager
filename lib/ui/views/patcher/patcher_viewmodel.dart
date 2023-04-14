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
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatcherAPI _patcherAPI = locator<PatcherAPI>();
  final Toast _toast = locator<Toast>();
  PatchedApplication? selectedApp;
  List<Patch> selectedPatches = [];
  late bool hasMicroGPatch = selectedPatches.any(
    (patch) => patch.name.endsWith('microg-support'),
  );

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

    hasMicroGPatch = selectedPatches.any(
      (patch) => patch.name.endsWith('microg-support'),
    );

    if (context.mounted) {
      if (isValid) {
        askForInstallationMethod(context);
      } else {
        return showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: I18nText('warning'),
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
                  askForInstallationMethod(context);
                },
              )
            ],
          ),
        );
      }
    }
  }

  Future<dynamic> askForInstallationMethod(BuildContext context) {
    if (selectedApp!.packageName.contains('youtube')) {
      final width = MediaQuery.of(context).size.width;
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: I18nText('selectInstallMethod'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: I18nText(
            !hasMicroGPatch
                ? FlutterI18n.translate(
                    context,
                    'selectInstallMethodHint',
                    translationParams: {
                      'installMethod': 'root',
                      'option': 'Select',
                      'otherInstallMethod': 'non-root',
                    },
                  )
                : FlutterI18n.translate(
                    context,
                    'selectInstallMethodHint',
                    translationParams: {
                      'installMethod': 'non-root',
                      'option': 'Unselect',
                      'otherInstallMethod': 'root',
                    },
                  ),
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () {
                if (hasMicroGPatch) {
                  _toast.show(
                    'installerView.installErrorDialogText1',
                  );
                  hasMicroGPatch = true;
                } else {
                  Navigator.of(context).pop();
                  navigateToInstaller();
                }
              },
              style: ButtonStyle(
                minimumSize: MaterialStateProperty.all<Size>(Size(width, 48)),
                foregroundColor: MaterialStateProperty.all<Color>(
                  Theme.of(context).colorScheme.primary,
                ),
                backgroundColor: MaterialStateProperty.all<Color>(
                  Theme.of(context).colorScheme.primary.withOpacity(0.1),
                ),
                shape: MaterialStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.0),
                  ),
                ),
              ),
              child: I18nText('rootLabel'),
            ),
            const SizedBox(height: 8),
            TextButton(
              style: ButtonStyle(
                minimumSize: MaterialStateProperty.all<Size>(Size(width, 48)),
                foregroundColor: MaterialStateProperty.all<Color>(
                  Theme.of(context).colorScheme.primary,
                ),
                backgroundColor: MaterialStateProperty.all<Color>(
                  Theme.of(context).colorScheme.primary.withOpacity(0.1),
                ),
                shape: MaterialStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.0),
                  ),
                ),
              ),
              child: I18nText('nonRootLabel'),
              onPressed: () {
                if (!hasMicroGPatch) {
                  _toast.show(
                    'installerView.installErrorDialogText2',
                  );
                  hasMicroGPatch = false;
                } else {
                  Navigator.of(context).pop();
                  navigateToInstaller();
                }
              },
            )
          ],
        ),
      );
    } else {
      navigateToInstaller();
      return Future.value();
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
