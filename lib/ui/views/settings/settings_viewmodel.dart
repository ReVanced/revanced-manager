import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:timeago/timeago.dart';

class SettingsViewModel extends BaseViewModel {
  bool isRooted = false;

  Future<void> initialize() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    isRooted = prefs.getBool('isRooted') ?? false;
    notifyListeners();
  }

  final NavigationService _navigationService = locator<NavigationService>();

  void setLanguage(String language) {
    notifyListeners();
  }

  void navigateToRootChecker() {
    _navigationService.navigateTo(Routes.rootCheckerView);
  }

  Future<void> updateLanguage(BuildContext context, String? value) async {
    if (value != null) {
      await FlutterI18n.refresh(context, Locale(value));
      setLocaleMessages(value, EnMessages());
    }
  }
}
