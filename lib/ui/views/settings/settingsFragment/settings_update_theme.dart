// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:stacked/stacked.dart';

final _settingViewModel = SettingsViewModel();

// ignore: constant_identifier_names
const int ANDROID_12_SDK_VERSION = 31;

class SUpdateTheme extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  bool getDynamicThemeStatus() {
    return _managerAPI.getUseDynamicTheme();
  }

  Future<void> setUseDynamicTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDynamicTheme(value);
    final int currentTheme = (DynamicTheme.of(context)!.themeId ~/ 2) * 2;
    await DynamicTheme.of(context)!.setTheme(currentTheme + (value ? 1 : 0));
    notifyListeners();
  }

  int getThemeMode() {
    return _managerAPI.getThemeMode();
  }

  Future<void> setThemeMode(BuildContext context, int value) async {
    await _managerAPI.setThemeMode(value);
    final bool isDynamicTheme = DynamicTheme.of(context)!.themeId.isEven;
    await DynamicTheme.of(context)!.setTheme(value * 2 + (isDynamicTheme ? 0 : 1));
    final bool isLight = value != 2 && (value == 1 || DynamicTheme.of(context)!.theme.brightness == Brightness.light);
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        systemNavigationBarIconBrightness:
        isLight ? Brightness.dark : Brightness.light,
      ),
    );
    notifyListeners();
  }
}

class SUpdateThemeUI extends StatelessWidget {
  const SUpdateThemeUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.appearanceSectionTitle',
      children: <Widget>[
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.themeModeLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.themeModeHint'),
          trailing: DropdownButton<int>(
            value: SUpdateTheme().getThemeMode(),
            onChanged: (value) => SUpdateTheme().setThemeMode(context, value!),
            items: <DropdownMenuItem<int>>[
              DropdownMenuItem<int>(
                value: 0,
                child: I18nText('settingsView.systemThemeLabel'),
              ),
              DropdownMenuItem<int>(
                value: 1,
                child: I18nText('settingsView.lightThemeLabel'),
              ),
              DropdownMenuItem<int>(
                value: 2,
                child: I18nText('settingsView.darkThemeLabel'),
              ),
            ],
          ),
        ),
        FutureBuilder<int>(
          future: _settingViewModel.getSdkVersion(),
          builder: (context, snapshot) => Visibility(
            visible:
                snapshot.hasData && snapshot.data! >= ANDROID_12_SDK_VERSION,
            child: SwitchListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
              title: I18nText(
                'settingsView.dynamicThemeLabel',
                child: const Text(
                  '',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              subtitle: I18nText('settingsView.dynamicThemeHint'),
              value: _settingViewModel.sUpdateTheme.getDynamicThemeStatus(),
              onChanged: (value) => {
                _settingViewModel.sUpdateTheme.setUseDynamicTheme(
                  context,
                  value,
                ),
              },
            ),
          ),
        ),
      ],
    );
  }
}
