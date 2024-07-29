// ignore_for_file: use_build_context_synchronously
import 'package:device_info_plus/device_info_plus.dart';
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
  Future<void> initialize(BuildContext context) async {
    locator<Toast>().initialize(context);
    final SharedPreferences prefs = await SharedPreferences.getInstance();

    if (prefs.getBool('permissionsRequested') == null) {
      await Permission.storage.request();
      await prefs.setBool('permissionsRequested', true);
      await RootAPI().hasRootPermissions().then(
            (value) => Permission.requestInstallPackages.request().then(
                  (value) => Permission.ignoreBatteryOptimizations.request(),
                ),
          );
    }

    final dynamicTheme = DynamicTheme.of(context)!;
    if (prefs.getInt('themeMode') == null) {
      await prefs.setInt('themeMode', 0);
      await dynamicTheme.setTheme(0);
    }

    // Force disable Material You on Android 11 and below
    if (dynamicTheme.themeId.isOdd) {
      const int android12SdkVersion = 31;
      final AndroidDeviceInfo info = await DeviceInfoPlugin().androidInfo;
      if (info.version.sdkInt < android12SdkVersion) {
        await prefs.setInt('themeMode', 0);
        await prefs.setBool('useDynamicTheme', false);
        await dynamicTheme.setTheme(0);
      }
    }

    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        systemNavigationBarColor: Colors.transparent,
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
