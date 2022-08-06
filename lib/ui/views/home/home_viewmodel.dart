import 'package:revanced_manager_flutter/app/app.locator.dart';
import 'package:revanced_manager_flutter/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class HomeViewModel extends BaseViewModel {
  Future downloadPatches() => locator<ManagerAPI>().downloadPatches();
  Future downloadIntegrations() => locator<ManagerAPI>().downloadIntegrations();
}
