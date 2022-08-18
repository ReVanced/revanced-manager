import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class SettingsViewModel extends BaseViewModel {
  final NavigationService _navigationService = locator<NavigationService>();

  void setLanguage(String language) {
    notifyListeners();
  }

  void navigateToContributors() {
    _navigationService.navigateTo(Routes.contributorsView);
  }
}
