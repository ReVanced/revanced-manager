// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch_tile.dart';
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
    final int currentTheme = DynamicTheme.of(context)!.themeId;
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

  Future<void> setUseDarkTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDarkTheme(value);
    final int currentTheme = DynamicTheme.of(context)!.themeId;
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
}

class SUpdateThemeUI extends StatelessWidget {
  const SUpdateThemeUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.appearanceSectionTitle',
      children: <Widget>[
        CustomSwitchTile(
          padding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.darkThemeLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.darkThemeHint'),
          value: SUpdateTheme().getDarkThemeStatus(),
          onTap: (value) => SUpdateTheme().setUseDarkTheme(
            context,
            value,
          ),
        ),
        FutureBuilder<int>(
          future: _settingViewModel.getSdkVersion(),
          builder: (context, snapshot) => Visibility(
            visible:
                snapshot.hasData && snapshot.data! >= ANDROID_12_SDK_VERSION,
            child: CustomSwitchTile(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
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
              onTap: (value) =>
                  _settingViewModel.sUpdateTheme.setUseDynamicTheme(
                context,
                value,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
