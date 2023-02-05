import 'dart:io';
import 'package:cr_file_saver/file_saver.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:logcat/logcat.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_update_language.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_update_theme.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:share_extend/share_extend.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();

  final SUpdateLanguage sUpdateLanguage = SUpdateLanguage();
  final SUpdateTheme sUpdateTheme = SUpdateTheme();

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }

  bool isSentryEnabled() {
    return _managerAPI.isSentryEnabled();
  }

  void useSentry(bool value) {
    _managerAPI.setSentryStatus(value);
    _toast.showBottom('settingsView.restartAppForChanges');
    notifyListeners();
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
    _toast.showBottom('settingsView.deletedKeystore');
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
        await CRFileSaver.saveFileWithDialog(SaveFileDialogParams(
            sourceFilePath: outFile.path, destinationFileName: 'selected_patches_$dateTime.json',),);
        _toast.showBottom('settingsView.exportedPatches');
      } else {
        _toast.showBottom('settingsView.noExportFileFound');
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  Future<void> importPatches() async {
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      if (result != null && result.files.single.path != null) {
        final File inFile = File(result.files.single.path!);
        inFile.copySync(_managerAPI.storedPatchesFile);
        inFile.delete();
        if (locator<PatcherViewModel>().selectedApp != null) {
          locator<PatcherViewModel>().loadLastSelectedPatches();
        }
        _toast.showBottom('settingsView.importedPatches');
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      _toast.showBottom('settingsView.jsonSelectorErrorMessage');
    }
  }

  void resetSelectedPatches() {
    _managerAPI.resetLastSelectedPatches();
    _toast.showBottom('settingsView.resetStoredPatches');
  }

  Future<int> getSdkVersion() async {
    final AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt ?? -1;
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
