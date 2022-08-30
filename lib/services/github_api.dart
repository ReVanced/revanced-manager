import 'dart:io';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:github/github.dart';
import 'package:timeago/timeago.dart';

class GithubAPI {
  final GitHub _github = GitHub();

  final Map<String, String> repoAppPath = {
    'com.google.android.youtube': 'youtube',
    'com.google.android.apps.youtube.music': 'music',
    'com.twitter.android': 'twitter',
    'com.reddit.frontpage': 'reddit',
    'com.zhiliaoapp.musically': 'tiktok',
    'de.dwd.warnapp': 'warnwetter',
    'com.garzotto.pflotsh.ecmwf_a': 'ecmwf',
  };

  Future<String?> latestReleaseVersion(String org, String repoName) async {
    try {
      var latestRelease = await _github.repositories.getLatestRelease(
        RepositorySlug(org, repoName),
      );
      return latestRelease.tagName;
    } on Exception {
      return null;
    }
  }

  Future<File?> latestReleaseFile(
    String extension,
    String org,
    String repoName,
  ) async {
    try {
      var latestRelease = await _github.repositories.getLatestRelease(
        RepositorySlug(org, repoName),
      );
      String? url = latestRelease.assets
          ?.firstWhere((asset) =>
              asset.name != null &&
              asset.name!.endsWith(extension) &&
              !asset.name!.contains('-sources') &&
              !asset.name!.contains('-javadoc'))
          .browserDownloadUrl;
      if (url != null) {
        return await DefaultCacheManager().getSingleFile(url);
      }
    } on Exception {
      return null;
    }
    return null;
  }

  Future<String> latestCommitTime(String org, String repoName) async {
    try {
      var repo = await _github.repositories.getRepository(
        RepositorySlug(org, repoName),
      );
      return repo.pushedAt != null
          ? format(repo.pushedAt!, locale: 'en_short')
          : '';
    } on Exception {
      return '';
    }
  }

  Future<List<Contributor>> getContributors(String org, String repoName) async {
    return await (_github.repositories.listContributors(
      RepositorySlug(org, repoName),
    )).toList();
  }

  Future<List<RepositoryCommit>> getCommits(
    String packageName,
    String org,
    String repoName,
  ) async {
    String path =
        'src/main/kotlin/app/revanced/patches/${repoAppPath[packageName]}';
    return await (PaginationHelper(_github)
        .objects<Map<String, dynamic>, RepositoryCommit>(
      'GET',
      '/repos/$org/$repoName/commits',
      (i) => RepositoryCommit.fromJson(i),
      params: <String, dynamic>{'path': path},
    )).toList();
  }
}
