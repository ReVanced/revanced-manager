// ignore_for_file: use_build_context_synchronously
import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/home/home_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class NavigationViewModel extends IndexTrackingViewModel {
  void initialize(BuildContext context) async {
    locator<Toast>().initialize(context);
    SharedPreferences prefs = await SharedPreferences.getInstance();
    if (prefs.getBool('permissionsRequested') == null) {
      await prefs.setBool('permissionsRequested', true);
      RootAPI().hasRootPermissions().then(
            (value) => Permission.requestInstallPackages.request().then(
                  (value) => Permission.ignoreBatteryOptimizations.request(),
                ),
          );
    }
    if (prefs.getBool('useDarkTheme') == null) {
      bool isDark =
          MediaQuery.of(context).platformBrightness != Brightness.light;
      await prefs.setBool('useDarkTheme', isDark);
      await DynamicTheme.of(context)!.setTheme(isDark ? 1 : 0);
    }
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        systemNavigationBarColor: Colors.transparent,
        systemNavigationBarIconBrightness:
            DynamicTheme.of(context)!.theme.brightness == Brightness.light
                ? Brightness.dark
                : Brightness.light,
      ),
    );
  }

  Widget getViewForIndex(int index) {
    switch (index) {
      case 0:
        return const HomeView();
      case 1:
        return const PatcherView();
      case 2:
        return const SettingsView();
      default:
        return const HomeView();
    }
  }
}
