import 'dart:convert';
import 'dart:io';

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class ExportSettingsViewModel extends BaseViewModel {
  final _channel = const MethodChannel('app.revanced.manager.flutter/settings');
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  void init(BuildContext context) {
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

  Future<void> accept() async {
    final externalDir = await getExternalStorageDirectory();

    final Map<String, dynamic> data = {};

    data['themeMode'] = _managerAPI.getThemeMode();
    data['useDynamicTheme'] = _managerAPI.getUseDynamicTheme();

    data['apiUrl'] = _managerAPI.getApiUrl();
    data['patchesRepo'] = _managerAPI.getPatchesRepo();
    data['integrationsRepo'] = _managerAPI.getIntegrationsRepo();

    data['patchesAutoUpdate'] = _managerAPI.isPatchesAutoUpdate();
    data['patchesChangeEnabled'] = _managerAPI.isPatchesChangeEnabled();
    data['universalPatchesEnabled'] = _managerAPI.areUniversalPatchesEnabled();
    data['experimentalPatchesEnabled'] = _managerAPI.areExperimentalPatchesEnabled();

    data['keystorePassword'] = _managerAPI.getKeystorePassword();

    // Load keystore
    if (externalDir != null) {
      final keystoreFile = File('${externalDir.path}/revanced-manager.keystore');
      if (keystoreFile.existsSync()) {
        final keystoreBytes = keystoreFile.readAsBytesSync();
        data['keystore'] = base64Encode(keystoreBytes);
      }
    }

    // Load patches
    final patchFile = File(_managerAPI.storedPatchesFile);
    if (patchFile.existsSync()) {
      data['patches'] = patchFile.readAsStringSync();
    }

    _channel.invokeMethod('accept', {'data': jsonEncode(data)});
  }

  void deny() {
    _channel.invokeMethod('deny');
  }
}
