import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:package_info_plus/package_info_plus.dart';

class AboutInfo {
  static Future<Map<String, dynamic>> getInfo() async {
    final packageInfo = await PackageInfo.fromPlatform();
    final info = await DeviceInfoPlugin().androidInfo;
    const buildFlavor =
        kReleaseMode ? 'release' : (kProfileMode ? 'profile' : 'debug');

    return {
      'version': packageInfo.version,
      'flavor': buildFlavor,
      'model': info.model,
      'androidVersion': info.version.release,
      'supportedArch': info.supportedAbis,
    };
  }
}
