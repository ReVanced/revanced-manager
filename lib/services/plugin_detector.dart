import 'package:device_apps/device_apps.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/detected_plugin.dart';

@lazySingleton
class PluginDetector {
  static const List<String> _knownPluginPackages = [
    'app.revanced.manager.flutter',
    'app.revanced.manager',
    'com.google.android.youtube.music.revanced',
    'com.google.android.youtube.revanced',
    'com.vanced.android.youtube',
    'com.vanced.manager',
    'com.microg.android.gms',
    'com.mgoogle.android.gms',
    'app.revanced.integrations',
    'revanced.youtube.music',
    'revanced.youtube',
  ];

  static const List<String> _downloaderKeywords = [
    'youtube',
    'downloader',
    'ytdl',
    'tubemate',
    'snaptube',
    'vidmate',
    'newpipe',
    'vanced',
    'revanced',
    'microg',
    'integrations',
  ];

  Future<List<DetectedPlugin>> detectPlugins() async {
    try {
      final List<Application> allApps = await DeviceApps.getInstalledApplications(
        includeSystemApps: true,
        includeAppIcons: true,
        onlyAppsWithLaunchIntent: false,
      );

      final List<DetectedPlugin> detectedPlugins = [];

      for (final Application app in allApps) {
        if (_isPlugin(app)) {
          final ApplicationWithIcon? appWithIcon = app is ApplicationWithIcon 
              ? app 
              : await DeviceApps.getApp(app.packageName, true) as ApplicationWithIcon?;
          
          detectedPlugins.add(
            DetectedPlugin(
              packageName: app.packageName,
              appName: app.appName,
              versionName: app.versionName ?? 'Unknown',
              icon: appWithIcon?.icon,
              isSystemApp: app.systemApp,
              category: _categorizePlugin(app.packageName, app.appName),
              hasLauncherIcon: await _hasLauncherIcon(app.packageName),
            ),
          );
        }
      }

      detectedPlugins.sort((a, b) {
        final categoryCompare = a.category.index.compareTo(b.category.index);
        if (categoryCompare != 0) {
          return categoryCompare;
        }
        return a.appName.compareTo(b.appName);
      });

      return detectedPlugins;
    } catch (e) {
      return [];
    }
  }

  bool _isPlugin(Application app) {
    final String packageName = app.packageName.toLowerCase();
    final String appName = app.appName.toLowerCase();

    if (_knownPluginPackages.any((known) => packageName.contains(known.toLowerCase()))) {
      return true;
    }

    if (_downloaderKeywords.any((keyword) => 
        packageName.contains(keyword) || appName.contains(keyword))) {
      return true;
    }

    if (!app.systemApp && _mightBePlugin(packageName, appName)) {
      return true;
    }

    return false;
  }

  PluginCategory _categorizePlugin(String packageName, String appName) {
    final String lowerPackage = packageName.toLowerCase();
    final String lowerName = appName.toLowerCase();

    if (lowerPackage.contains('revanced') || lowerName.contains('revanced')) {
      return PluginCategory.revanced;
    }
    
    if (lowerPackage.contains('vanced') || lowerName.contains('vanced')) {
      return PluginCategory.vanced;
    }
    
    if (lowerPackage.contains('microg') || lowerName.contains('microg')) {
      return PluginCategory.microg;
    }
    
    if (lowerPackage.contains('integrations') || lowerName.contains('integrations')) {
      return PluginCategory.integrations;
    }

    if (_downloaderKeywords.any((keyword) => 
        lowerPackage.contains(keyword) || lowerName.contains(keyword))) {
      return PluginCategory.downloader;
    }

    return PluginCategory.other;
  }

  bool _mightBePlugin(String packageName, String appName) {
    final patterns = [
      RegExp(r'\.plugin\.', caseSensitive: false),
      RegExp(r'\.addon\.', caseSensitive: false),
      RegExp(r'\.extension\.', caseSensitive: false),
      RegExp(r'\.mod\.', caseSensitive: false),
    ];

    return patterns.any((pattern) => 
        pattern.hasMatch(packageName) || pattern.hasMatch(appName));
  }

  Future<bool> _hasLauncherIcon(String packageName) async {
    try {
      final app = await DeviceApps.getApp(packageName);
      return app != null && await DeviceApps.isAppInstalled(packageName);
    } catch (e) {
      return false;
    }
  }
}