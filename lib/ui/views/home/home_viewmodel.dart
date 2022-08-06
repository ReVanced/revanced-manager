import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class HomeViewModel extends BaseViewModel {
  Future downloadPatches() => locator<ManagerAPI>().downloadPatches();
  Future downloadIntegrations() => locator<ManagerAPI>().downloadIntegrations();
}
