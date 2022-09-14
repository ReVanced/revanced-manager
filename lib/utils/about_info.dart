import 'package:package_info_plus/package_info_plus.dart';
import 'package:device_info_plus/device_info_plus.dart';

class AboutInfo {
  static Future<Map<String, dynamic>> getInfo() async {
    final packageInfo = await PackageInfo.fromPlatform();
    final info = await DeviceInfoPlugin().androidInfo;
    return {
      'version': packageInfo.version,
      'model': info.model,
      'androidVersion': info.version.release,
      'arch': info.supportedAbis.first
    };
  }
}
