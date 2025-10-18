import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/detected_plugin.dart';
import 'package:revanced_manager/services/plugin_detector.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:stacked/stacked.dart';

class SettingsManagePluginsViewModel extends BaseViewModel {
  final PluginDetector _pluginDetector = locator<PluginDetector>();
  final Toast _toast = locator<Toast>();

  List<DetectedPlugin> _detectedPlugins = [];
  bool _isLoading = false;

  List<DetectedPlugin> get detectedPlugins => _detectedPlugins;
  bool get isLoading => _isLoading;

  List<DetectedPlugin> get uninstallablePlugins => 
      _detectedPlugins.where((plugin) => plugin.isUninstallable).toList();
  Future<void> initialize() async {
    await detectPlugins();
  }
  Future<void> detectPlugins() async {
    _isLoading = true;
    notifyListeners();

    try {
      _detectedPlugins = await _pluginDetector.detectPlugins();
    } catch (e) {
      _toast.showBottom(t.settingsView.pluginDetectionFailed);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> uninstallPlugin(DetectedPlugin plugin, BuildContext context) async {
    final bool? confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.uninstallPluginDialogTitle(appName: plugin.appName)),
        content: Text(t.settingsView.uninstallPluginDialogText),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(t.cancelButton),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(t.settingsView.uninstallButton),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      setBusy(true);
      final bool success = await DeviceApps.uninstallApp(plugin.packageName);
      
      if (success) {
        _detectedPlugins.removeWhere((p) => p.packageName == plugin.packageName);
        _toast.showBottom(t.settingsView.uninstallPluginSuccess(appName: plugin.appName));
        notifyListeners();
      } else {
        _toast.showBottom(t.settingsView.uninstallPluginError(appName: plugin.appName));
      }
    } catch (e) {
      try {
        await _uninstallWithIntent(plugin.packageName);
        _detectedPlugins.removeWhere((p) => p.packageName == plugin.packageName);
        _toast.showBottom(t.settingsView.uninstallPluginSuccess(appName: plugin.appName));
        notifyListeners();
      } catch (intentError) {
        _toast.showBottom(t.settingsView.uninstallPluginError(appName: plugin.appName));
      }
    } finally {
      setBusy(false);
    }
  }

  Future<void> _uninstallWithIntent(String packageName) async {
    const MethodChannel channel = MethodChannel('app.revanced.manager.flutter/patcher');
    
    try {
      await channel.invokeMethod('uninstallApp', {
        'packageName': packageName,
      });
    } on PlatformException catch (e) {
      rethrow;
    }
  }

  IconData getCategoryIcon(PluginCategory category) {
    switch (category) {
      case PluginCategory.revanced:
        return Icons.auto_awesome;
      case PluginCategory.vanced:
        return Icons.play_circle_fill;
      case PluginCategory.microg:
        return Icons.security;
      case PluginCategory.integrations:
        return Icons.integration_instructions;
      case PluginCategory.downloader:
        return Icons.download;
      case PluginCategory.other:
        return Icons.extension;
    }
  }

  Color getCategoryColor(PluginCategory category, BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    
    switch (category) {
      case PluginCategory.revanced:
        return colorScheme.primary;
      case PluginCategory.vanced:
        return colorScheme.secondary;
      case PluginCategory.microg:
        return colorScheme.tertiary;
      case PluginCategory.integrations:
        return colorScheme.primaryContainer;
      case PluginCategory.downloader:
        return colorScheme.secondaryContainer;
      case PluginCategory.other:
        return colorScheme.outline;
    }
  }
}