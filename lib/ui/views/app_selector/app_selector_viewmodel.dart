import 'package:device_apps/device_apps.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  List<Application> apps = [];
  String query = '';

  void initialization() {
    getApps();
  }

  void getApps() async {
    apps = await DeviceApps.getInstalledApplications();
  }
}
