// Check for google mobile services on device

import 'package:device_apps/device_apps.dart';

Future<bool> checkForGMS() async {
  bool isGMSInstalled = true;
  isGMSInstalled = await DeviceApps.isAppInstalled('com.google.android.gms') ||
      await DeviceApps.isAppInstalled('com.android.vending');
  return isGMSInstalled;
}
