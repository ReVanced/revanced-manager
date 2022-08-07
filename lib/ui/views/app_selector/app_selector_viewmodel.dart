import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:stacked/stacked.dart';

class AppSelectorViewModel extends BaseViewModel {
  List<AppInfo> apps = [];
  String query = '';

  void initialization() {
    getApps();
  }

  void getApps() async {
    apps = await InstalledApps.getInstalledApps(false, true);
  }
}
