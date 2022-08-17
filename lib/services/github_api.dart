import 'package:github/github.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class GithubAPI {
  var github = GitHub();

  Future<String?> latestRelease(String org, repoName) async {
    try {
      var latestRelease = await github.repositories.getLatestRelease(
        RepositorySlug(org, repoName),
      );
      return latestRelease.assets
          ?.firstWhere((asset) =>
              asset.name != null &&
              (asset.name!.endsWith('.dex') || asset.name!.endsWith('.apk')) &&
              !asset.name!.contains('-sources') &&
              !asset.name!.contains('-javadoc'))
          .browserDownloadUrl;
    } on Exception {
      return '';
    }
  }

  Future<String> latestCommitTime(String org, repoName) async {
    try {
      var repo = await github.repositories.getRepository(
        RepositorySlug(org, repoName),
      );
      return repo.pushedAt != null
          ? format(repo.pushedAt!, locale: 'en_short')
          : '';
    } on Exception {
      return '';
    }
  }

  Future<List<Contributor>> getContributors(String org, repoName) async {
    try {
      var contributors = github.repositories.listContributors(
        RepositorySlug(org, repoName),
      );
      return contributors.toList();
    } on Exception {
      return List.empty();
    }
  }

  Future<bool> hasUpdates(PatchedApplication app, String org, repoName) async {
    // TODO: get status based on last update time on the folder of this app?
    return true;
  }

  Future<String> getChangelog(
      PatchedApplication app, String org, repoName) async {
    // TODO: get changelog based on last commits on the folder of this app?
    return 'fix: incorrect fingerprint version';
  }
}
