import 'package:github/github.dart';

class GithubAPI {
  var github = GitHub();

  Future latestRelease(String org, repoName) async {
    var latestRelease = await github.repositories
        .getLatestRelease(RepositorySlug(org, repoName));
    var dlurl = latestRelease.assets
        ?.firstWhere((element) =>
            element.browserDownloadUrl!.contains(".jar") ||
            element.browserDownloadUrl!.contains(".apk"))
        .browserDownloadUrl;
    print(dlurl);
    return latestRelease;
  }

  Future latestCommitTime(String org, repoName) async {
    var repo =
        await github.repositories.getRepository(RepositorySlug(org, repoName));

    var commitTime = repo.pushedAt?.difference(
      DateTime.now().toLocal(),
    );

    final hours = commitTime!.inHours.abs();

    if (hours > 24) {
      var days = (commitTime.inDays).abs().toString();
      print("$days days");
      return "$days days";
    } else if (hours > 1 && hours < 24) {
      var hours = (commitTime.inHours).abs().toString();
      print("$hours hours");
      return "$hours hours";
    } else {
      var minutes = (commitTime.inMinutes).abs().toString();
      print("$minutes minutes");
      return "$minutes mins";
    }
  }

  Future contributors(String org, repoName) async {
    var contributors =
        github.repositories.listContributors(RepositorySlug(org, repoName));
    contributors.forEach((contributor) {
      print(contributor.login);
      print(contributor.avatarUrl);
    });
    return contributors;
  }
}

void main(List<String> args) {
  GithubAPI githubAPI = GithubAPI();
  // githubAPI.latestRelease('revanced', 'revanced-patches');
  // githubAPI.latestCommitTime("revanced", "revanced-patches");
  // githubAPI.contributors("revanced", "revanced-manager");
}
