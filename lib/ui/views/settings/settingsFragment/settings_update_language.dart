// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:language_code/language_code.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
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
    // initLang();

    // Return a dialog with list for each language supported by the application.
    // the dialog will display the country, english name, native name of each languages,
    // the current language will be highlighted with a bold font weight.
    return showDialog(
      context: parentContext,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.languageLabel),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: ListBody(
            children: AppLocale.values.map(
              (locale) {
                return ListTile(
                  title: Text(
                    (() {
                      try {
                        return LanguageCodes.fromCode(locale.languageCode)
                            .englishName;
                      } catch (e) {
                        // This act as an fallback if the language is not supported by the package
                        // Do not try to make this nicer or debug it; trust me, I've tried.
                        return locale.languageCode;
                      }
                    })(),
                    style: TextStyle(
                      fontWeight: locale.languageCode ==
                              LocaleSettings.currentLocale.languageCode
                          ? FontWeight.bold
                          : FontWeight.normal,
                    ),
                  ),
                  subtitle: Text(
                    (() {
                      try {
                        return LanguageCodes.fromCode(locale.languageCode)
                            .nativeName;
                      } catch (e) {
                        return '????';
                      }
                    })(),
                  ),
                  trailing: Text(
                    '${locale.countryCode}'
                    '-'
                    '${locale.languageCode}',
                  ),
                  onTap: () {
                    updateLocale(locale.languageCode.replaceAll('-', '_'));
                    Navigator.pop(context);
                  },
                );
              },
            ).toList(),
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            label: Text(t.cancelButton),
            onPressed: () {
              Navigator.of(context).pop();
            },
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
      subtitle: LocaleSettings.currentLocale.name,
      onTap: () =>
          _settingViewModel.sUpdateLanguage.showLanguagesDialog(context),
    );
  }
}
