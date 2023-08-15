import 'dart:io';
import 'package:cr_file_saver/file_saver.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:logcat/logcat.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_language.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_theme.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:share_extend/share_extend.dart';
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
  final SUpdateTheme sUpdateTheme = SUpdateTheme();

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

  bool isPatchesChangeEnabled() {
    return _managerAPI.isPatchesChangeEnabled();
  }

  Future<void> showPatchesChangeEnableDialog(
    bool value,
    BuildContext context,
  ) async {
    if (value) {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          title: I18nText('warning'),
          content: I18nText(
            'settingsView.enablePatchesSelectionWarningText',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          actions: [
            CustomMaterialButton(
              isFilled: false,
              label: I18nText('noButton'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            CustomMaterialButton(
              label: I18nText('yesButton'),
              onPressed: () {
                _managerAPI.setChangingToggleModified(true);
                _managerAPI.setPatchesChangeEnabled(true);
                Navigator.of(context).pop();
              },
            ),
          ],
        ),
      );
    } else {
      return showDialog(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          title: I18nText('warning'),
          content: I18nText(
            'settingsView.disablePatchesSelectionWarningText',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          actions: [
            CustomMaterialButton(
              isFilled: false,
              label: I18nText('noButton'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            CustomMaterialButton(
              label: I18nText('yesButton'),
              onPressed: () {
                _managerAPI.setChangingToggleModified(true);
                _patchesSelectorViewModel.selectDefaultPatches();
                _managerAPI.setPatchesChangeEnabled(false);
                Navigator.of(context).pop();
              },
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

  bool areExperimentalPatchesEnabled() {
    return _managerAPI.areExperimentalPatchesEnabled();
  }

  void useExperimentalPatches(bool value) {
    _managerAPI.enableExperimentalPatchesStatus(value);
    notifyListeners();
  }

  void deleteKeystore() {
    _managerAPI.deleteKeystore();
    _toast.showBottom('settingsView.regeneratedKeystore');
    notifyListeners();
  }

  void deleteTempDir() {
    _managerAPI.deleteTempFolder();
    _toast.showBottom('settingsView.deletedTempDir');
    notifyListeners();
  }

  Future<void> exportPatches() async {
    try {
      final File outFile = File(_managerAPI.storedPatchesFile);
      if (outFile.existsSync()) {
        final String dateTime =
            DateTime.now().toString().replaceAll(' ', '_').split('.').first;
        await CRFileSaver.saveFileWithDialog(
          SaveFileDialogParams(
            sourceFilePath: outFile.path,
            destinationFileName: 'selected_patches_$dateTime.json',
          ),
        );
        _toast.showBottom('settingsView.exportedPatches');
      } else {
        _toast.showBottom('settingsView.noExportFileFound');
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
        final FilePickerResult? result = await FilePicker.platform.pickFiles(
          type: FileType.custom,
          allowedExtensions: ['json'],
        );
        if (result != null && result.files.single.path != null) {
          final File inFile = File(result.files.single.path!);
          inFile.copySync(_managerAPI.storedPatchesFile);
          inFile.delete();
          if (_patcherViewModel.selectedApp != null) {
            _patcherViewModel.loadLastSelectedPatches();
          }
          _toast.showBottom('settingsView.importedPatches');
        }
      } on Exception catch (e) {
        if (kDebugMode) {
          print(e);
        }
        _toast.showBottom('settingsView.jsonSelectorErrorMessage');
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
        await CRFileSaver.saveFileWithDialog(
          SaveFileDialogParams(
            sourceFilePath: outFile.path,
            destinationFileName: 'keystore_$dateTime.keystore',
          ),
        );
        _toast.showBottom('settingsView.exportedKeystore');
      } else {
        _toast.showBottom('settingsView.noKeystoreExportFileFound');
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> importKeystore() async {
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles();
      if (result != null && result.files.single.path != null) {
        final File inFile = File(result.files.single.path!);
        inFile.copySync(_managerAPI.keystoreFile);

        _toast.showBottom('settingsView.importedKeystore');
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      _toast.showBottom('settingsView.keystoreSelectorErrorMessage');
    }
  }

  void resetSelectedPatches() {
    _managerAPI.resetLastSelectedPatches();
    _toast.showBottom('settingsView.resetStoredPatches');
  }

  Future<int> getSdkVersion() async {
    final AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt;
  }

  Future<void> deleteLogs() async {
    final Directory appCacheDir = await getTemporaryDirectory();
    final Directory logsDir = Directory('${appCacheDir.path}/logs');
    if (logsDir.existsSync()) {
      logsDir.deleteSync(recursive: true);
    }
    _toast.showBottom('settingsView.deletedLogs');
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
    ShareExtend.share(logcat.path, 'file');
  }
}
