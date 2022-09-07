import 'package:github/github.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final GithubAPI _githubAPI = GithubAPI();
  List<Contributor> patcherContributors = [];
  List<Contributor> patchesContributors = [];
  List<Contributor> integrationsContributors = [];
  List<Contributor> cliContributors = [];
  List<Contributor> managerContributors = [];

  Future<void> getContributors() async {
    patcherContributors = await _githubAPI.getContributors(
      _managerAPI.getPatcherRepo(),
    );
    patchesContributors = await _githubAPI.getContributors(
      _managerAPI.getPatchesRepo(),
    );
    integrationsContributors = await _githubAPI.getContributors(
      _managerAPI.getIntegrationsRepo(),
    );
    cliContributors = await _githubAPI.getContributors(
      _managerAPI.getCliRepo(),
    );
    managerContributors = await _githubAPI.getContributors(
      _managerAPI.getManagerRepo(),
    );
    notifyListeners();
  }
}
