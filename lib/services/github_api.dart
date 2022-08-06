import 'package:github/github.dart';
import 'package:injectable/injectable.dart';

@lazySingleton
class GithubAPI {
  var github = GitHub();

  Future<String?> latestRelease(String org, repoName) async {
    var latestRelease = await github.repositories
        .getLatestRelease(RepositorySlug(org, repoName));
    var dlurl = latestRelease.assets
        ?.firstWhere((asset) =>
            asset.name != null &&
            (asset.name!.endsWith('.dex') || asset.name!.endsWith('.apk')) &&
            !asset.name!.contains('-sources') &&
            !asset.name!.contains('-javadoc'))
        .browserDownloadUrl;
    return dlurl;
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
      return "$days days";
    } else if (hours > 1 && hours < 24) {
      var hours = (commitTime.inHours).abs().toString();
      return "$hours hours";
    } else {
      var minutes = (commitTime.inMinutes).abs().toString();
      return "$minutes mins";
    }
  }

  Future contributors(String org, repoName) async {
    var contributors =
        github.repositories.listContributors(RepositorySlug(org, repoName));
    contributors.forEach((contributor) {});
    return contributors;
  }
}
