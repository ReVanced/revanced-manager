// ignore_for_file: use_build_context_synchronously
import 'dart:io';
import 'package:cr_file_saver/file_saver.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:logcat/logcat.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_update_language.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_update_theme.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_text_field.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:share_extend/share_extend.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

// ignore: constant_identifier_names
const int ANDROID_12_SDK_VERSION = 31;

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();
  final TextEditingController _orgPatSourceController = TextEditingController();
  final TextEditingController _patSourceController = TextEditingController();
  final TextEditingController _orgIntSourceController = TextEditingController();
  final TextEditingController _intSourceController = TextEditingController();
  final TextEditingController _apiUrlController = TextEditingController();

  final SUpdateLanguage sUpdateLanguage = SUpdateLanguage();
  final SUpdateTheme sUpdateTheme = SUpdateTheme();

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }

  Future<void> showSourcesDialog(BuildContext context) async {
    String patchesRepo = _managerAPI.getPatchesRepo();
    String integrationsRepo = _managerAPI.getIntegrationsRepo();
    _orgPatSourceController.text = patchesRepo.split('/')[0];
    _patSourceController.text = patchesRepo.split('/')[1];
    _orgIntSourceController.text = integrationsRepo.split('/')[0];
    _intSourceController.text = integrationsRepo.split('/')[1];
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            I18nText('settingsView.sourcesLabel'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showResetConfirmationDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            )
          ],
        ),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              CustomTextField(
                leadingIcon: Icon(
                  Icons.extension_outlined,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                inputController: _orgPatSourceController,
                label: I18nText('settingsView.orgPatchesLabel'),
                hint: patchesRepo.split('/')[0],
                onChanged: (value) => notifyListeners(),
              ),
              const SizedBox(height: 8),
              CustomTextField(
                leadingIcon: const Icon(
                  Icons.extension_outlined,
                  color: Colors.transparent,
                ),
                inputController: _patSourceController,
                label: I18nText('settingsView.sourcesPatchesLabel'),
                hint: patchesRepo.split('/')[1],
                onChanged: (value) => notifyListeners(),
              ),
              const SizedBox(height: 20),
              CustomTextField(
                leadingIcon: Icon(
                  Icons.merge_outlined,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                inputController: _orgIntSourceController,
                label: I18nText('settingsView.orgIntegrationsLabel'),
                hint: integrationsRepo.split('/')[0],
                onChanged: (value) => notifyListeners(),
              ),
              const SizedBox(height: 8),
              CustomTextField(
                leadingIcon: const Icon(
                  Icons.merge_outlined,
                  color: Colors.transparent,
                ),
                inputController: _intSourceController,
                label: I18nText('settingsView.sourcesIntegrationsLabel'),
                hint: integrationsRepo.split('/')[1],
                onChanged: (value) => notifyListeners(),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('cancelButton'),
            onPressed: () {
              _orgPatSourceController.clear();
              _patSourceController.clear();
              _orgIntSourceController.clear();
              _intSourceController.clear();
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              _managerAPI.setPatchesRepo(
                '${_orgPatSourceController.text}/${_patSourceController.text}',
              );
              _managerAPI.setIntegrationsRepo(
                '${_orgIntSourceController.text}/${_intSourceController.text}',
              );
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }

  Future<void> showApiUrlDialog(BuildContext context) async {
    String apiUrl = _managerAPI.getApiUrl();
    _apiUrlController.text = apiUrl.replaceAll('https://', '');
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            I18nText('settingsView.apiURLLabel'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showApiUrlResetDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            )
          ],
        ),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              CustomTextField(
                leadingIcon: Icon(
                  Icons.api_outlined,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                inputController: _apiUrlController,
                label: I18nText('settingsView.selectApiURL'),
                hint: apiUrl.split('/')[0],
                onChanged: (value) => notifyListeners(),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('cancelButton'),
            onPressed: () {
              _apiUrlController.clear();
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              String apiUrl = _apiUrlController.text;
              if (!apiUrl.startsWith('https')) {
                apiUrl = 'https://$apiUrl';
              }
              _managerAPI.setApiUrl(apiUrl);
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }

  Future<void> showResetConfirmationDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.sourcesResetDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('settingsView.sourcesResetDialogText'),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              _managerAPI.setPatchesRepo('');
              _managerAPI.setIntegrationsRepo('');
              Navigator.of(context).pop();
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }

  Future<void> showApiUrlResetDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.sourcesResetDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('settingsView.apiURLResetDialogText'),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              _managerAPI.setApiUrl('');
              Navigator.of(context).pop();
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }

  // bool isSentryEnabled() {
  //   return _managerAPI.isSentryEnabled();
  // }

  // void useSentry(bool value) {
  //   _managerAPI.setSentryStatus(value);
  //   _toast.showBottom('settingsView.restartAppForChanges');
  //   notifyListeners();
  // }

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
