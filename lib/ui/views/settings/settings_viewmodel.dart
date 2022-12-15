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
      File outFile = File(_managerAPI.storedPatchesFile);
      if (outFile.existsSync()) {
        String dateTime =
            DateTime.now().toString().replaceAll(' ', '_').split('.').first;
        String tempFilePath =
            '${outFile.path.substring(0, outFile.path.lastIndexOf('/') + 1)}selected_patches_$dateTime.json';
        outFile.copySync(tempFilePath);
        await CRFileSaver.saveFileWithDialog(SaveFileDialogParams(
            sourceFilePath: tempFilePath, destinationFileName: ''));
        File(tempFilePath).delete();
        locator<Toast>().showBottom('settingsView.exportedPatches');
      } else {
        locator<Toast>().showBottom('settingsView.noExportFileFound');
      }
    } on Exception catch (e, s) {
      Sentry.captureException(e, stackTrace: s);
    }
  }

  Future<void> importPatches() async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      if (result != null && result.files.single.path != null) {
        File inFile = File(result.files.single.path!);
        final File storedPatchesFile = File(_managerAPI.storedPatchesFile);
        if (!storedPatchesFile.existsSync()) {
          storedPatchesFile.createSync(recursive: true);
        }
        inFile.copySync(storedPatchesFile.path);
        inFile.delete();
        if (locator<PatcherViewModel>().selectedApp != null) {
          locator<PatcherViewModel>().loadLastSelectedPatches();
        }
        locator<Toast>().showBottom('settingsView.importedPatches');
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      locator<Toast>().showBottom('settingsView.jsonSelectorErrorMessage');
    }
  }

  void resetSelectedPatches() {
    _managerAPI.resetLastSelectedPatches();
    _toast.showBottom('settingsView.resetStoredPatches');
  }

  Future<int> getSdkVersion() async {
    AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt ?? -1;
  }

  Future<void> deleteLogs() async {
    Directory appCacheDir = await getTemporaryDirectory();
    Directory logsDir = Directory('${appCacheDir.path}/logs');
    if (logsDir.existsSync()) {
      logsDir.deleteSync(recursive: true);
    }
    _toast.showBottom('settingsView.deletedLogs');
  }

  Future<void> exportLogcatLogs() async {
    Directory appCache = await getTemporaryDirectory();
    Directory logDir = Directory('${appCache.path}/logs');
    logDir.createSync();
    String dateTime = DateTime.now()
        .toIso8601String()
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('T', '')
        .replaceAll('.', '');
    File logcat = File('${logDir.path}/revanced-manager_logcat_$dateTime.log');
    String logs = await Logcat.execute();
    logcat.writeAsStringSync(logs);
    ShareExtend.share(logcat.path, 'file');
  }
}
