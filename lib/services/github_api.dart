import 'dart:io';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:github/github.dart';
import 'package:timeago/timeago.dart';

class GithubAPI {
  final GitHub _github = GitHub();

  Future<String?> latestReleaseVersion(String org, repoName) async {
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
    repoName,
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

  Future<String> latestCommitTime(String org, repoName) async {
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

  Future<List<Contributor>> getContributors(String org, repoName) async {
    return await (_github.repositories.listContributors(
      RepositorySlug(org, repoName),
    )).toList();
  }

  Future<List<RepositoryCommit>> getCommits(String org, repoName) async {
    return await (_github.repositories.listCommits(
      RepositorySlug(org, repoName),
    )).toList();
  }
}
