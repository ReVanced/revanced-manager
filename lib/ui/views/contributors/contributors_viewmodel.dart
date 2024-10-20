import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  List<dynamic> patcherContributors = [];
  List<dynamic> patchesContributors = [];
  List<dynamic> cliContributors = [];
  List<dynamic> managerContributors = [];

  String repoName(String repo) => repo.split('/').last;

  Future<void> getContributors() async {
    final Map<String, List<dynamic>> contributors =
        await _managerAPI.getContributors();
    patcherContributors = contributors[repoName(_managerAPI.defaultPatcherRepo)] ?? [];
    patchesContributors = contributors[repoName(_managerAPI.defaultPatchesRepo)] ?? [];
    cliContributors = contributors[repoName(_managerAPI.defaultCliRepo)] ?? [];
    managerContributors = contributors[repoName(_managerAPI.defaultManagerRepo)] ?? [];
    notifyListeners();
  }
}
