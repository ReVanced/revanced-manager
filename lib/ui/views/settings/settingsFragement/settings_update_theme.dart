// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class SUpdateTheme extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  bool getDynamicThemeStatus() {
    return _managerAPI.getUseDynamicTheme();
  }

  void setUseDynamicTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDynamicTheme(value);
    int currentTheme = DynamicTheme.of(context)!.themeId;
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

  void setUseDarkTheme(BuildContext context, bool value) async {
    await _managerAPI.setUseDarkTheme(value);
    int currentTheme = DynamicTheme.of(context)!.themeId;
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
