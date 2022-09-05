import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';
import 'package:root/root.dart';
import 'package:stacked_services/stacked_services.dart';

class RootCheckerViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  bool isRooted = false;

  void initialize() {
    isRooted = _managerAPI.isRooted() ?? false;
  }

  Future<void> navigateAsRoot() async {
    bool? res = await Root.isRooted();
    isRooted = res != null && res == true;
    if (isRooted) {
      await navigateToHome();
    } else {
      notifyListeners();
    }
  }

  Future<void> navigateAsNonRoot() async {
    isRooted = false;
    await navigateToHome();
  }

  Future<void> navigateToHome() async {
    _managerAPI.setIsRooted(isRooted);
    _navigationService.navigateTo(Routes.navigationView);
  }
}
