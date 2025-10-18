import 'dart:typed_data';

enum PluginCategory {
  revanced,
  vanced,
  microg,
  integrations,
  downloader,
  other,
}

extension PluginCategoryExtension on PluginCategory {
  String get displayName {
    switch (this) {
      case PluginCategory.revanced:
        return 'ReVanced';
      case PluginCategory.vanced:
        return 'Vanced';
      case PluginCategory.microg:
        return 'MicroG';
      case PluginCategory.integrations:
        return 'Integrations';
      case PluginCategory.downloader:
        return 'Downloader';
      case PluginCategory.other:
        return 'Other';
    }
  }

  String get description {
    switch (this) {
      case PluginCategory.revanced:
        return 'ReVanced related apps and components';
      case PluginCategory.vanced:
        return 'YouTube Vanced related apps';
      case PluginCategory.microg:
        return 'MicroG services for Google Play compatibility';
      case PluginCategory.integrations:
        return 'Integration components for patches';
      case PluginCategory.downloader:
        return 'Media downloader applications';
      case PluginCategory.other:
        return 'Other detected plugins or components';
    }
  }
}

class DetectedPlugin {
  final String packageName;
  final String appName;
  final String versionName;
  final Uint8List? icon;
  final bool isSystemApp;
  final PluginCategory category;
  final bool hasLauncherIcon;

  const DetectedPlugin({
    required this.packageName,
    required this.appName,
    required this.versionName,
    this.icon,
    required this.isSystemApp,
    required this.category,
    required this.hasLauncherIcon,
  });

  bool get isUninstallable {
    if (isSystemApp) return false;
    if (packageName.contains('app.revanced.manager.flutter')) return false;
    if (packageName.contains('com.android.') || packageName.contains('android.')) return false;
    if (packageName.contains('com.microg.android.gms') || packageName.contains('com.vanced.android.youtube')) {
      return false;
    }
    return true;
  }

  String get detectionReason {
    if (!hasLauncherIcon) {
      return 'Hidden from launcher';
    }
    return 'Detected as ${category.displayName.toLowerCase()} component';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is DetectedPlugin &&
          runtimeType == other.runtimeType &&
          packageName == other.packageName;

  @override
  int get hashCode => packageName.hashCode;

  @override
  String toString() {
    return 'DetectedPlugin{packageName: $packageName, appName: $appName, category: $category}';
  }
}