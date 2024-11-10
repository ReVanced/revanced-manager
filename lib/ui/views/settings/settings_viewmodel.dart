import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_file_dialog/flutter_file_dialog.dart';
import 'package:logcat/logcat.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_language.dart';
import 'package:share_plus/share_plus.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final PatchesSelectorViewModel _patchesSelectorViewModel =
      PatchesSelectorViewModel();
  final PatcherViewModel _patcherViewModel = locator<PatcherViewModel>();
  final Toast _toast = locator<Toast>();

  final SUpdateLanguage sUpdateLanguage = SUpdateLanguage();

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }

  bool isPatchesAutoUpdate() {
    return _managerAPI.isPatchesAutoUpdate();
  }

  void setPatchesAutoUpdate(bool value) {
    _managerAPI.setPatchesAutoUpdate(value);
    notifyListeners();
  }

  bool showUpdateDialog() {
    return _managerAPI.showUpdateDialog();
  }

  void setShowUpdateDialog(bool value) {
    _managerAPI.setShowUpdateDialog(value);
    notifyListeners();
  }

  bool isPatchesChangeEnabled() {
    return _managerAPI.isPatchesChangeEnabled();
  }

  void useAlternativeSources(bool value) {
    _managerAPI.useAlternativeSources(value);
    _managerAPI.setCurrentPatchesVersion('0.0.0');
    _managerAPI.setLastUsedPatchesVersion();
    notifyListeners();
  }

  bool isUsingAlternativeSources() {
    return _managerAPI.isUsingAlternativeSources();
  }

  Future<void> showPatchesChangeEnableDialog(
    bool value,
    BuildContext context,
  ) async {
    if (value) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.warning),
          content: Text(
            t.settingsView.enablePatchesSelectionWarningText,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                _managerAPI.setChangingToggleModified(true);
                _managerAPI.setPatchesChangeEnabled(true);
                Navigator.of(context).pop();
              },
              child: Text(t.yesButton),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text(t.noButton),
            ),
          ],
        ),
      );
    } else {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.warning),
          content: Text(
            t.settingsView.disablePatchesSelectionWarningText,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text(t.noButton),
            ),
            FilledButton(
              onPressed: () {
                _managerAPI.setChangingToggleModified(true);
                _patchesSelectorViewModel.selectDefaultPatches();
                _managerAPI.setPatchesChangeEnabled(false);
                Navigator.of(context).pop();
              },
              child: Text(t.yesButton),
            ),
          ],
        ),
      );
    }
  }

  bool areUniversalPatchesEnabled() {
    return _managerAPI.areUniversalPatchesEnabled();
  }

  void showUniversalPatches(bool value) {
    _managerAPI.enableUniversalPatchesStatus(value);
    notifyListeners();
  }

  bool isLastPatchedAppEnabled() {
    return _managerAPI.isLastPatchedAppEnabled();
  }

  void useLastPatchedApp(bool value) {
    _managerAPI.enableLastPatchedAppStatus(value);
    if (!value) {
      _managerAPI.deleteLastPatchedApp();
    }
    notifyListeners();
  }

  bool isVersionCompatibilityCheckEnabled() {
    return _managerAPI.isVersionCompatibilityCheckEnabled();
  }

  void useVersionCompatibilityCheck(bool value) {
    _managerAPI.enableVersionCompatibilityCheckStatus(value);
    notifyListeners();
  }

  bool isRequireSuggestedAppVersionEnabled() {
    return _managerAPI.isRequireSuggestedAppVersionEnabled();
  }

  Future<void>? showRequireSuggestedAppVersionDialog(
    BuildContext context,
    bool value,
  ) {
    if (!value) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t.warning),
          content: Text(
            t.settingsView.requireSuggestedAppVersionDialogText,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                _managerAPI.enableRequireSuggestedAppVersionStatus(false);
                Navigator.of(context).pop();
              },
              child: Text(t.yesButton),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text(t.noButton),
            ),
          ],
        ),
      );
    } else {
      _managerAPI.enableRequireSuggestedAppVersionStatus(true);

      if (!_managerAPI.suggestedAppVersionSelected) {
        _patcherViewModel.selectedApp = null;
      }

      return null;
    }
  }

  void deleteKeystore() {
    _managerAPI.deleteKeystore();
    _toast.showBottom(t.settingsView.regeneratedKeystore);
    notifyListeners();
  }

  void deleteTempDir() {
    _managerAPI.deleteTempFolder();
    _toast.showBottom(t.settingsView.deletedTempDir);
    notifyListeners();
  }

  Future<void> exportSettings() async {
    try {
      final String settings = _managerAPI.exportSettings();
      final Directory tempDir = await getTemporaryDirectory();
      final String filePath = '${tempDir.path}/manager_settings.json';
      final File file = File(filePath);
      await file.writeAsString(settings);
      final String? result = await FlutterFileDialog.saveFile(
        params: SaveFileDialogParams(
          sourceFilePath: file.path,
          fileName: 'manager_settings.json',
          mimeTypesFilter: ['application/json'],
        ),
      );
      if (result != null) {
        _toast.showBottom(t.settingsView.exportedSettings);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> importSettings() async {
    try {
      final String? result = await FlutterFileDialog.pickFile(
        params: const OpenFileDialogParams(
          fileExtensionsFilter: ['json'],
        ),
      );
      if (result != null) {
        final File inFile = File(result);
        final String settings = inFile.readAsStringSync();
        inFile.delete();
        _managerAPI.importSettings(settings);
        _toast.showBottom(t.settingsView.importedSettings);
        _toast.showBottom(t.settingsView.restartAppForChanges);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom(t.settingsView.jsonSelectorErrorMessage);
    }
  }

  Future<void> exportPatches() async {
    try {
      final File outFile = File(_managerAPI.storedPatchesFile);
      if (outFile.existsSync()) {
        final String dateTime =
            DateTime.now().toString().replaceAll(' ', '_').split('.').first;
        final status = await FlutterFileDialog.saveFile(
          params: SaveFileDialogParams(
            sourceFilePath: outFile.path,
            fileName: 'selected_patches_$dateTime.json',
          ),
        );
        if (status != null) {
          _toast.showBottom(t.settingsView.exportedPatches);
        }
      } else {
        _toast.showBottom(t.settingsView.noExportFileFound);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> importPatches(BuildContext context) async {
    if (isPatchesChangeEnabled()) {
      try {
        final String? result = await FlutterFileDialog.pickFile(
          params: const OpenFileDialogParams(
            fileExtensionsFilter: ['json'],
          ),
        );
        if (result != null) {
          final File inFile = File(result);
          inFile.copySync(_managerAPI.storedPatchesFile);
          inFile.delete();
          if (_patcherViewModel.selectedApp != null) {
            _patcherViewModel.loadLastSelectedPatches();
          }
          _toast.showBottom(t.settingsView.importedPatches);
        }
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
        _toast.showBottom(t.settingsView.jsonSelectorErrorMessage);
      }
    } else {
      _managerAPI.showPatchesChangeWarningDialog(context);
    }
  }

  Future<void> exportKeystore() async {
    try {
      final File outFile = File(_managerAPI.keystoreFile);
      if (outFile.existsSync()) {
        final String dateTime =
            DateTime.now().toString().replaceAll(' ', '_').split('.').first;
        final status = await FlutterFileDialog.saveFile(
          params: SaveFileDialogParams(
            sourceFilePath: outFile.path,
            fileName: 'keystore_$dateTime.keystore',
          ),
        );
        if (status != null) {
          _toast.showBottom(t.settingsView.exportedKeystore);
        }
      } else {
        _toast.showBottom(t.settingsView.noKeystoreExportFileFound);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> importKeystore() async {
    try {
      final String? result = await FlutterFileDialog.pickFile();
      if (result != null) {
        final File inFile = File(result);
        inFile.copySync(_managerAPI.keystoreFile);

        _toast.showBottom(t.settingsView.importedKeystore);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom(t.settingsView.keystoreSelectorErrorMessage);
    }
  }

  void resetAllOptions() {
    _managerAPI.resetAllOptions();
    _toast.showBottom(t.settingsView.resetStoredOptions);
  }

  void resetSelectedPatches() {
    _managerAPI.resetLastSelectedPatches();
    _toast.showBottom(t.settingsView.resetStoredPatches);
  }

  Future<void> deleteLogs() async {
    final Directory appCacheDir = await getTemporaryDirectory();
    final Directory logsDir = Directory('${appCacheDir.path}/logs');
    if (logsDir.existsSync()) {
      logsDir.deleteSync(recursive: true);
    }
    _toast.showBottom(t.settingsView.deletedLogs);
  }

  Future<void> exportLogcatLogs() async {
    final Directory appCache = await getTemporaryDirectory();
    final Directory logDir = Directory('${appCache.path}/logs');
    logDir.createSync();
    final String dateTime = DateTime.now()
        .toIso8601String()
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('T', '')
        .replaceAll('.', '');
    final File logcat =
        File('${logDir.path}/revanced-manager_logcat_$dateTime.log');
    final String logs = await Logcat.execute();
    logcat.writeAsStringSync(logs);
    await Share.shareXFiles([XFile(logcat.path)]);
  }
}
