// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:timeago/timeago.dart';

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  void setLanguage(String language) {
    notifyListeners();
  }

  void navigateToRootChecker() {
    _navigationService.navigateTo(Routes.rootCheckerView);
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
      DynamicTheme.of(context)!.setTheme(value ? 2 : 0);
    } else {
      DynamicTheme.of(context)!.setTheme(value ? 3 : 1);
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
      DynamicTheme.of(context)!.setTheme(value ? 1 : 0);
    } else {
      DynamicTheme.of(context)!.setTheme(value ? 3 : 2);
    }
    notifyListeners();
  }
}
