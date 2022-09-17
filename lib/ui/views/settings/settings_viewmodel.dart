// ignore_for_file: use_build_context_synchronously
import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:logcat/logcat.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_text_field.dart';
import 'package:share_extend/share_extend.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:timeago/timeago.dart';

// ignore: constant_identifier_names
const int ANDROID_12_SDK_VERSION = 31;

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final TextEditingController _patSourceController = TextEditingController();
  final TextEditingController _intSourceController = TextEditingController();

  void setLanguage(String language) {
    notifyListeners();
  }

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }

  Future<void> updateLanguage(BuildContext context, String? value) async {
    if (value != null) {
      await FlutterI18n.refresh(context, Locale(value));
      setLocaleMessages(value, EnMessages());
    }
  }

  bool getDynamicThemeStatus() {
    return _managerAPI.getUseDynamicTheme();
  }

  void setUseDynamicTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDynamicTheme(value);
    int currentTheme = DynamicTheme.of(context)!.themeId;
    if (currentTheme.isEven) {
      await DynamicTheme.of(context)!.setTheme(value ? 2 : 0);
    } else {
      await DynamicTheme.of(context)!.setTheme(value ? 3 : 1);
    }

    notifyListeners();
  }

  bool getDarkThemeStatus() {
    return _managerAPI.getUseDarkTheme();
  }

  void setUseDarkTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDarkTheme(value);
    int currentTheme = DynamicTheme.of(context)!.themeId;
    if (currentTheme < 2) {
      await DynamicTheme.of(context)!.setTheme(value ? 1 : 0);
    } else {
      await DynamicTheme.of(context)!.setTheme(value ? 3 : 2);
    }
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        systemNavigationBarIconBrightness:
            value ? Brightness.light : Brightness.dark,
      ),
    );
    notifyListeners();
  }

  Future<void> showLanguagesDialog(BuildContext context) {
    return showDialog(
      context: context,
      builder: (context) => SimpleDialog(
        title: I18nText('settingsView.languageLabel'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        children: <Widget>[
          RadioListTile<String>(
            title: I18nText('settingsView.englishOption'),
            value: 'en',
            groupValue: 'en',
            onChanged: (value) {
              updateLanguage(context, value);
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
    );
  }

  Future<void> showSourcesDialog(BuildContext context) async {
    String patchesRepo = _managerAPI.getPatchesRepo();
    String integrationsRepo = _managerAPI.getIntegrationsRepo();
    _patSourceController.text = patchesRepo;
    _intSourceController.text = integrationsRepo;
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
                leadingIcon: const Icon(Icons.extension_outlined),
                inputController: _patSourceController,
                label: I18nText('settingsView.sourcesPatchesLabel'),
                hint: patchesRepo,
                onChanged: (value) => notifyListeners(),
              ),
              const SizedBox(height: 20),
              CustomTextField(
                leadingIcon: const Icon(Icons.merge_outlined),
                inputController: _intSourceController,
                label: I18nText('settingsView.sourcesIntegrationsLabel'),
                hint: integrationsRepo,
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
              _patSourceController.clear();
              _intSourceController.clear();
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              _managerAPI.setPatchesRepo(_patSourceController.text);
              _managerAPI.setIntegrationsRepo(_intSourceController.text);
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
            label: I18nText('cancelButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
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

  Future<int> getSdkVersion() async {
    AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
    return info.version.sdkInt ?? -1;
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
