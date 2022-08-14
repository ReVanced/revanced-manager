import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
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
    if (isRooted == true) {
      navigateToHome();
    }
    notifyListeners();
  }

  Future<bool> getMagiskPermissions() async {
    try {
      await Root.exec(cmd: 'cat /proc/version');
    } on Exception {
      return false;
    }
    return true;
  }

  Future<void> navigateToHome() async {
    final prefs = await SharedPreferences.getInstance();
    prefs.setBool('isRooted', isRooted!);
    _navigationService.navigateTo(Routes.navigation);
    notifyListeners();
  }
}
