import 'package:github/github.dart';

class GithubAPI {
  var github = GitHub();

  Future latestRelease(String org, repoName) async {
    var latestRelease = await github.repositories
        .getLatestRelease(RepositorySlug(org, repoName));
    var dlurl = latestRelease.assets?.first.browserDownloadUrl;
    print(dlurl);
    return latestRelease;
  }
}

void main(List<String> args) {
  GithubAPI().latestRelease('revanced', 'revanced-patches');
}
