import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  List<dynamic> patcherContributors = [];
  List<dynamic> patchesContributors = [];
  List<dynamic> integrationsContributors = [];
  List<dynamic> cliContributors = [];
  List<dynamic> managerContributors = [];

  Future<void> getContributors() async {
    Map<String, List<dynamic>> contributors =
        await _managerAPI.getContributors();
    patcherContributors = contributors[_managerAPI.defaultPatcherRepo] ?? [];
    patchesContributors = contributors[_managerAPI.getPatchesRepo()] ?? [];
    integrationsContributors =
        contributors[_managerAPI.getIntegrationsRepo()] ?? [];
    cliContributors = contributors[_managerAPI.defaultCliRepo] ?? [];
    managerContributors = contributors[_managerAPI.defaultManagerRepo] ?? [];
    notifyListeners();
  }
}
