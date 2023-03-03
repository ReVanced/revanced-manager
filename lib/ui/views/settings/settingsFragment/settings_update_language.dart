// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/main.dart';
import 'package:revanced_manager/services/crowdin_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:timeago/timeago.dart' as timeago;

final _settingViewModel = SettingsViewModel();

class SUpdateLanguage extends BaseViewModel {
  final CrowdinAPI _crowdinAPI = locator<CrowdinAPI>();
  final Toast _toast = locator<Toast>();
  late SharedPreferences _prefs;
  String selectedLanguage = 'English';
  String selectedLanguageLocale = prefs.getString('language') ?? 'en_US';
  List languages = [];

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    selectedLanguageLocale =
        _prefs.getString('language') ?? selectedLanguageLocale;
    notifyListeners();
  }

  Future<void> updateLanguage(BuildContext context, String? value) async {
    if (value != null) {
      selectedLanguageLocale = value;
      _prefs = await SharedPreferences.getInstance();
      await _prefs.setString('language', value);
      await FlutterI18n.refresh(context, Locale(value));
      timeago.setLocaleMessages(value, timeago.EnMessages());
      locator<NavigationViewModel>().notifyListeners();
      notifyListeners();
    }
  }

  Future<void> initLang() async {
    languages = await _crowdinAPI.getLanguages();
    languages.sort((a, b) => a['name'].compareTo(b['name']));
    notifyListeners();
  }

  Future<void> showLanguagesDialog(BuildContext parentContext) {
    initLang();
    return showDialog(
      context: parentContext,
      builder: (context) => SimpleDialog(
        title: I18nText('settingsView.languageLabel'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        children: [
          SizedBox(
            height: 500,
            child: ListView.builder(
              itemCount: languages.length,
              itemBuilder: (context, index) {
                return RadioListTile<String>(
                  title: Text(languages[index]['name']),
                  subtitle: Text(languages[index]['locale']),
                  value: languages[index]['locale'],
                  groupValue: selectedLanguageLocale,
                  onChanged: (value) {
                    selectedLanguage = languages[index]['name'];
                    _toast.showBottom('settingsView.restartAppForChanges');
                    updateLanguage(context, value);
                    Navigator.pop(context);
                  },
                );
              },
            ),
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
      title: 'settingsView.languageLabel',
      subtitle: _settingViewModel.sUpdateLanguage.selectedLanguage,
      onTap: () =>
          _settingViewModel.sUpdateLanguage.showLanguagesDialog(context),
    );
  }
}
