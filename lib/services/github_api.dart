import 'package:github/github.dart';
import 'package:injectable/injectable.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class GithubAPI {
  var github = GitHub();

  Future<String?> latestRelease(String org, repoName) async {
    var latestRelease = await github.repositories.getLatestRelease(
      RepositorySlug(org, repoName),
    );
    var dlurl = latestRelease.assets
        ?.firstWhere((asset) =>
            asset.name != null &&
            (asset.name!.endsWith('.dex') || asset.name!.endsWith('.apk')) &&
            !asset.name!.contains('-sources') &&
            !asset.name!.contains('-javadoc'))
        .browserDownloadUrl;
    return dlurl;
  }

  Future<String> latestCommitTime(String org, repoName) async {
    var repo = await github.repositories.getRepository(
      RepositorySlug(org, repoName),
    );
    return format(repo.pushedAt!);
  }
}
