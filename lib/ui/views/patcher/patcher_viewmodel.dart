import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class PatcherViewModel extends BaseViewModel {
  final _naviagtionService = locator<NavigationService>();

  void navigateToAppSelector() {
    _naviagtionService.navigateTo(Routes.appSelectorView);
  }
}
