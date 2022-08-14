import 'package:fluttertoast/fluttertoast.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/ui/views/home/home_view.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:root/root.dart';
import 'package:stacked_services/stacked_services.dart';

class RootCheckerViewModel extends BaseViewModel {
  final _navigationService = locator<NavigationService>();
  bool? isRooted = false;

  Future<void> initialize() async {
    await checkRoot();
    notifyListeners();
  }

  Future<void> checkRoot() async {
    isRooted = await Root.isRooted();
    notifyListeners();
  }

  Future<void> getMagiskPermissions() async {
    if (isRooted == true) {
      Fluttertoast.showToast(msg: 'Magisk permission already granted!');
    }
    await Root.exec(cmd: "adb shell su -c exit");
    notifyListeners();
  }

  Future<void> navigateToHome() async {
    final prefs = await SharedPreferences.getInstance();
    prefs.setBool('showHome', true);
    _navigationService.navigateTo(Routes.homeView);
    notifyListeners();
  }
}
