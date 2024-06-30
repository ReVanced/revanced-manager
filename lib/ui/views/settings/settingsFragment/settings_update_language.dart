// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:language_code/language_code.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

final _settingViewModel = SettingsViewModel();
final _navigationService = NavigationService();

class SUpdateLanguage extends BaseViewModel {
  final Toast _toast = locator<Toast>();
  late SharedPreferences _prefs;
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    _prefs.getString('language');
    notifyListeners();
  }

  Future<void> updateLocale(String locale) async {
    LocaleSettings.setLocaleRaw(locale);
    _managerAPI.setLocale(locale);
    Future.delayed(
      const Duration(milliseconds: 120),
      () => _toast.showBottom(t.settingsView.languageUpdated),
    );
  }

  Future<void> showLanguagesDialog(BuildContext parentContext) {
    final ValueNotifier<AppLocale> selectedLanguageCode = ValueNotifier(
      LocaleSettings.currentLocale,
    );
    LanguageCodes getLanguageCode(Locale locale) {
      return LanguageCodes.fromLocale(
        locale,
        orElse: () => LanguageCodes.fromCode(locale.languageCode),
      );
    }

    final currentlyUsedLanguage =
        getLanguageCode(LocaleSettings.currentLocale.flutterLocale);
    // initLang();

    // Return a dialog with list for each language supported by the application.
    // the dialog will display the english and native name of each languages,
    // the current language will be highlighted by selected radio button.
    return showDialog(
      context: parentContext,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.languageLabel),
        icon: const Icon(Icons.language),
        contentPadding: EdgeInsets.zero,
        content: ValueListenableBuilder(
          valueListenable: selectedLanguageCode,
          builder: (context, value, child) {
            return SingleChildScrollView(
              child: ListBody(
                children: [
                  RadioListTile(
                    title: Text(currentlyUsedLanguage.englishName),
                    subtitle: Text(
                      '${currentlyUsedLanguage.nativeName}\n'
                      '(${LocaleSettings.currentLocale.languageTag})',
                    ),
                    value: LocaleSettings.currentLocale ==
                        selectedLanguageCode.value,
                    groupValue: true,
                    onChanged: (value) {
                      selectedLanguageCode.value = LocaleSettings.currentLocale;
                    },
                  ),
                  ...AppLocale.values
                      .where(
                    (locale) => locale != LocaleSettings.currentLocale,
                  )
                      .map((locale) {
                    final languageCode = getLanguageCode(locale.flutterLocale);
                    return RadioListTile(
                      title: Text(languageCode.englishName),
                      subtitle: Text(
                        '${languageCode.nativeName}\n'
                        '(${locale.languageTag})',
                      ),
                      value: locale == selectedLanguageCode.value,
                      groupValue: true,
                      onChanged: (value) {
                        selectedLanguageCode.value = locale;
                      },
                    );
                  }),
                ],
              ),
            );
          },
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
            },
            child: Text(t.cancelButton),
          ),
          TextButton(
            onPressed: () async {
              updateLocale(selectedLanguageCode.value.languageTag);
              await _navigationService.navigateToNavigationView();
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }
}

class SUpdateLanguageUI extends StatelessWidget {
  const SUpdateLanguageUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsTileDialog(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: t.settingsView.languageLabel,
      subtitle:
          LanguageCodes.fromLocale(LocaleSettings.currentLocale.flutterLocale)
              .nativeName,
      onTap: () =>
          _settingViewModel.sUpdateLanguage.showLanguagesDialog(context),
    );
  }
}
