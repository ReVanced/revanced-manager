import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class PatcherViewModel extends BaseViewModel {
  final _navigationService = locator<NavigationService>();

  void navigateToAppSelector() {
    _navigationService.navigateTo(Routes.appSelectorView);
  }

  void navigateToPatchesSelector() {
    _navigationService.navigateTo(Routes.patchesSelectorView);
  }

  void navigateToInstaller() {
    _navigationService.navigateTo(Routes.installerView);
  }

  bool showFabButton() {
    return locator<PatchesSelectorViewModel>().selectedPatches.isNotEmpty;
  }

  bool dimPatchesCard() {
    return locator<AppSelectorViewModel>().selectedApp == null;
  }
}
