import 'package:github/github.dart';
import 'package:injectable/injectable.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class GithubAPI {
  var github = GitHub();

  Future<String?> latestRelease(String org, repoName) async {
    String? dlurl = '';
    try {
      var latestRelease = await github.repositories.getLatestRelease(
        RepositorySlug(org, repoName),
      );
      dlurl = latestRelease.assets
          ?.firstWhere((asset) =>
              asset.name != null &&
              (asset.name!.endsWith('.dex') || asset.name!.endsWith('.apk')) &&
              !asset.name!.contains('-sources') &&
              !asset.name!.contains('-javadoc'))
          .browserDownloadUrl;
    } on Exception {
      dlurl = '';
    }
    return dlurl;
  }

  Future<String> latestCommitTime(String org, repoName) async {
    String pushedAt = '';
    try {
      var repo = await github.repositories.getRepository(
        RepositorySlug(org, repoName),
      );
      pushedAt = repo.pushedAt != null ? format(repo.pushedAt!) : '';
    } on Exception {
      pushedAt = '';
    }
    return pushedAt;
  }

  Future<List<Contributor>> getContributors(String org, repoName) async {
    try {
      var contributors = await github.repositories.listContributors(
        RepositorySlug(org, repoName),
      );
      return contributors.toList();
    } on Exception {
      print(Exception);
      return [];
    }
  }
}
