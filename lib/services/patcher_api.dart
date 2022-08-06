import 'dart:io';
import 'package:flutter/services.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/utils/string.dart';

class PatcherService {
  File? _patchBundleFile;
  final List<AppInfo> _filteredPackages = [];
  final Map<String, List<Patch>> _filteredPatches = <String, List<Patch>>{};
  final GithubAPI githubAPI = GithubAPI();
  static const platform = MethodChannel('app.revanced/patcher');
  static final PatcherService _instance = PatcherService.internal();
  factory PatcherService() => _instance;
  PatcherService.internal();

  Future<void> loadPatches() async {
    if (_patchBundleFile == null) {
      String? dexFileUrl =
          await githubAPI.latestRelease('revanced', 'revanced-patches');
      if (dexFileUrl != null) {
        _patchBundleFile =
            await DefaultCacheManager().getSingleFile(dexFileUrl);
        try {
          await platform.invokeMethod(
            'loadPatches',
            {
              'pathBundlesPaths': <String>[_patchBundleFile!.absolute.path],
            },
          );
        } on PlatformException {
          _patchBundleFile = null;
        }
      }
    }
  }

  Future<List<AppInfo>> getFilteredInstalledApps() async {
    if (_patchBundleFile != null && _filteredPackages.isEmpty) {
      List<AppInfo> all = await InstalledApps.getInstalledApps(false, true);
      try {
        List<String>? patchesPackages =
            await platform.invokeListMethod<String>('getCompatiblePackages');
        if (patchesPackages != null) {
          for (AppInfo app in all) {
            if (patchesPackages.contains(app.packageName)) {
              _filteredPackages.add(app);
            }
          }
        }
      } on Exception {
        return List.empty();
      }
    }
    return _filteredPackages;
  }

  Future<List<Patch>?> getFilteredPatches(AppInfo? targetApp) async {
    if (_patchBundleFile != null && targetApp != null) {
      if (_filteredPatches[targetApp.packageName] == null ||
          _filteredPatches[targetApp.packageName]!.isEmpty) {
        _filteredPatches[targetApp.packageName!] = [];
        try {
          var patches = await platform.invokeListMethod<Map<dynamic, dynamic>>(
            'getFilteredPatches',
            {
              'targetPackage': targetApp.packageName,
              'targetVersion': targetApp.versionName,
              'ignoreVersion': true,
            },
          );
          if (patches != null) {
            for (var patch in patches) {
              _filteredPatches[targetApp.packageName]!.add(
                Patch(
                  name: patch['name'],
                  simpleName: (patch['name'] as String)
                      .replaceAll('-', ' ')
                      .split('-')
                      .join(' ')
                      .toTitleCase(),
                  version: patch['version'] ?? 'unknown',
                  description: patch['description'] ?? 'unknown',
                ),
              );
            }
          }
        } on Exception {
          return List.empty();
        }
      }
    } else {
      return List.empty();
    }
    return _filteredPatches[targetApp.packageName];
  }
}
