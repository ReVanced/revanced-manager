import 'package:github/github.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final GithubAPI _githubAPI = GithubAPI();
  List<Contributor> patchesContributors = [];
  List<Contributor> integrationsContributors = [];
  List<Contributor> patcherContributors = [];
  List<Contributor> cliContributors = [];
  List<Contributor> managerContributors = [];

  Future<void> getContributors() async {
    patchesContributors = await _githubAPI.getContributors(
      ghOrg,
      patchesRepo,
    );
    integrationsContributors = await _githubAPI.getContributors(
      ghOrg,
      integrationsRepo,
    );
    patcherContributors = await _githubAPI.getContributors(
      ghOrg,
      patcherRepo,
    );
    cliContributors = await _githubAPI.getContributors(
      ghOrg,
      cliRepo,
    );
    managerContributors = await _githubAPI.getContributors(
      ghOrg,
      managerRepo,
    );
    notifyListeners();
  }
}
