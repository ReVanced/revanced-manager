// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
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
    // initLang();
    return showDialog(
      context: parentContext,
      builder: (context) => SimpleDialog(
        title: Text(t.settingsView.languageLabel),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        children: AppLocale.values.map((locale) {
          return ListTile(
            title: Text(locale.languageCode),
            subtitle: Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: Text(locale.languageTag),
            ),
            onTap: () {
              updateLocale(locale.languageCode.replaceAll('-', '_'));
              Navigator.pop(context);
            },
          );
        }).toList(),
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
