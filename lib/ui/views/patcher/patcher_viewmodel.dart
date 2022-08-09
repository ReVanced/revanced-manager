import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class PatcherViewModel extends BaseViewModel {
  final _navigationService = locator<NavigationService>();
  bool dimPatchCard = true;
  bool showFabButton = false;

  void navigateToAppSelector() {
    _navigationService.navigateTo(Routes.appSelectorView);
  }

  void navigateToPatchesSelector() {
    _navigationService.navigateTo(Routes.patchesSelectorView);
  }
}
