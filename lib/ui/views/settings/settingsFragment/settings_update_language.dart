// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:language_code/language_code.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

final _settingViewModel = SettingsViewModel();

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
    final ValueNotifier<String> selectedLanguageCode = ValueNotifier(
      '${LocaleSettings.currentLocale.languageCode}-${LocaleSettings.currentLocale.countryCode}',
    );
    LanguageCodes getLanguageCode(locale) {
      return LanguageCodes.fromCode(
        '${locale.languageCode}_${locale.countryCode}',
        orElse: () => LanguageCodes.fromCode(locale.languageCode),
      );
    }

    final currentlyUsedLanguage = getLanguageCode(LocaleSettings.currentLocale);
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
                    // TODO(Someone): There must've been a better way to do this.
                    title: Text(currentlyUsedLanguage.englishName),
                    subtitle: Text(
                      '${currentlyUsedLanguage.nativeName} (${LocaleSettings.currentLocale.languageCode}${LocaleSettings.currentLocale.countryCode != null ? '-${LocaleSettings.currentLocale.countryCode}' : ''})',
                    ),
                    value:
                        '${LocaleSettings.currentLocale.languageCode}-${LocaleSettings.currentLocale.countryCode}' ==
                            selectedLanguageCode.value,
                    groupValue: true,
                    onChanged: (value) {
                      selectedLanguageCode.value =
                          '${LocaleSettings.currentLocale.languageCode}-${LocaleSettings.currentLocale.countryCode}';
                    },
                  ),
                  ...AppLocale.values
                      .where(
                    (locale) =>
                        locale.languageCode != currentlyUsedLanguage.code,
                  )
                      .map((locale) {
                    final languageCode = getLanguageCode(locale);
                    return RadioListTile(
                      title: Text(languageCode.englishName),
                      subtitle: Text(
                        '${languageCode.nativeName} (${locale.languageCode}${locale.countryCode != null ? '-${locale.countryCode}' : ''})',
                      ),
                      value: '${locale.languageCode}-${locale.countryCode}' ==
                          selectedLanguageCode.value,
                      groupValue: true,
                      onChanged: (value) {
                        selectedLanguageCode.value =
                            '${locale.languageCode}-${locale.countryCode}';
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
            onPressed: () {
              // TODO(nullcube): Translation will not update until we refresh the page.
              updateLocale(selectedLanguageCode.value);
              Navigator.of(context).pop();
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
          LanguageCodes.fromCode(LocaleSettings.currentLocale.languageCode)
              .nativeName,
      onTap: () =>
          _settingViewModel.sUpdateLanguage.showLanguagesDialog(context),
    );
  }
}
