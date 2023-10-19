// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/main.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:timeago/timeago.dart' as timeago;

final _settingViewModel = SettingsViewModel();

class SUpdateLanguage extends BaseViewModel {
  final Toast _toast = locator<Toast>();
  late SharedPreferences _prefs;
  String selectedLanguage = prefs.getString('language') ?? 'English';
  String selectedLanguageLocale = prefs.getString('languageCode') ?? 'en_US';
  List languages = [
    {'name': 'English', 'locale': 'en_US'},
    {'name': 'Français', 'locale': 'fr_FR'},
    {'name': 'Deutsch', 'locale': 'de_DE'},
    {'name': 'Español', 'locale': 'es_ES'},
    {'name': 'Italiano', 'locale': 'it_IT'},
    {'name': 'Português', 'locale': 'pt_PT'},
    {'name': 'Polski', 'locale': 'pl_PL'},
    {'name': 'Nederlands', 'locale': 'nl_NL'},
    {'name': 'Русский', 'locale': 'ru_RU'},
    {'name': '日本語', 'locale': 'ja_JP'},
    {'name': '한국어', 'locale': 'ko_KR'},
    {'name': '中文', 'locale': 'zh_CN'},
    {'name': '中文（台灣）', 'locale': 'zh_TW'},
  ];

  Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
    selectedLanguageLocale =
        _prefs.getString('languageCode') ?? selectedLanguageLocale;
    notifyListeners();
  }

  Future<void> updateLanguage(BuildContext context, String? value) async {
    if (value != null) {
      final languageName =
          languages.firstWhere((element) => element['locale'] == value)['name'];
      selectedLanguageLocale = value;
      selectedLanguage = languageName;

      _prefs = await SharedPreferences.getInstance();
      await _prefs.setString('language', languageName);
      await _prefs.setString('languageCode', value);
      await FlutterI18n.refresh(context, Locale(value));
      timeago.setLocaleMessages(value, timeago.EnMessages());
      locator<NavigationViewModel>().notifyListeners();
      notifyListeners();
    }
  }

  Future<void> initLang() async {
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
          for (var language in languages)
            SimpleDialogOption(
              child: Text(language['name']),
              onPressed: () async {
                await updateLanguage(context, language['locale']);
                _toast.showBottom(
                  FlutterI18n.translate(
                    context,
                    'settingsView.languageChanged',
                    translationParams: {'language': language['name']},
                  ),
                );
                Navigator.pop(context);
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
    return ViewModelBuilder<SUpdateLanguage>.reactive(
      viewModelBuilder: () => SUpdateLanguage(),
      onViewModelReady: (model) => model.initialize(),
      builder: (context, model, child) => SettingsTileDialog(
        padding: const EdgeInsets.symmetric(horizontal: 20.0),
        title: 'settingsView.languageLabel',
        subtitle: model.selectedLanguage,
        onTap: () => model.showLanguagesDialog(context),
      ),
    );
  }
}
