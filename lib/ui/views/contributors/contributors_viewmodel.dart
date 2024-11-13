import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  Map<String, List<dynamic>> contributors = {};

  Future<void> getContributors() async {
    contributors = await _managerAPI.getContributors();
    notifyListeners();
  }
}
