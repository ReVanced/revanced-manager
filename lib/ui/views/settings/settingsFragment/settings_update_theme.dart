// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
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

  I18nText getThemeModeName() {
    final int = getThemeMode();
    switch (int) {
      case 0:
        return I18nText('settingsView.systemThemeLabel');
      case 1:
        return I18nText('settingsView.lightThemeLabel');
      case 2:
        return I18nText('settingsView.darkThemeLabel');
      default:
        return I18nText('settingsView.systemThemeLabel');
    }
  }

  Future<void> showThemeDialog(BuildContext context) async {
    int? currentTheme = getThemeMode();

    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.themeModeLabel'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        actions: <Widget>[
          Column(
            children: <Widget>[
              RadioListTile(
                title: I18nText('settingsView.systemThemeLabel'),
                value: 0,
                groupValue: currentTheme,
                onChanged: (value) => {
                  currentTheme = value,
                  setThemeMode(context, value!),
                  Navigator.of(context).pop(),
                },
              ),
              RadioListTile(
                title: I18nText('settingsView.lightThemeLabel'),
                value: 1,
                groupValue: currentTheme,
                onChanged: (value) => {
                  currentTheme = value,
                  setThemeMode(context, value!),
                  Navigator.of(context).pop(),
                },
              ),
              RadioListTile(
                title: I18nText('settingsView.darkThemeLabel'),
                value: 2,
                groupValue: currentTheme,
                onChanged: (value) => {
                  currentTheme = value,
                  setThemeMode(context, value!),
                  Navigator.of(context).pop(),
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

final sUpdateTheme = SUpdateTheme();
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
          trailing: CustomMaterialButton(
            label: sUpdateTheme.getThemeModeName(),
            onPressed: () => { sUpdateTheme.showThemeDialog(context) },
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
