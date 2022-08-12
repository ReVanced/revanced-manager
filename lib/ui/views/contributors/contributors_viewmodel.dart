import 'package:github/github.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:stacked/stacked.dart';

class ContributorsViewModel extends BaseViewModel {
  final GithubAPI githubAPI = GithubAPI();
  List<Contributor> patchesContributors = [];
  List<Contributor> integrationsContributors = [];
  List<Contributor> patcherContributors = [];
  List<Contributor> cliContributors = [];
  List<Contributor> managerContributors = [];

  Future<List<Contributor>> getContributors() async {
    patchesContributors =
        await githubAPI.getContributors('revanced', 'revanced-patches');
    integrationsContributors =
        await githubAPI.getContributors('revanced', 'revanced-integrations');
    patcherContributors =
        await githubAPI.getContributors('revanced', 'revanced-patcher');
    cliContributors =
        await githubAPI.getContributors('revanced', 'revanced-cli');
    managerContributors =
        await githubAPI.getContributors('revanced', 'revanced-manager');

    return [];
  }
}
