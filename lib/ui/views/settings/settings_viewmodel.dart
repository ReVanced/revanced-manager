import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class SettingsViewModel extends BaseViewModel {
  bool isRooted = false;

  Future<void> initialize() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    isRooted = prefs.getBool('isRooted') ?? false;
    notifyListeners();
  }

  final NavigationService _navigationService = locator<NavigationService>();

  void setLanguage(String language) {
    notifyListeners();
  }

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }

  void navigateToRootChecker() {
    _navigationService.navigateTo(Routes.rootCheckerView);
  }
}
