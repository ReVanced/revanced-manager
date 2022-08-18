import 'dart:io';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:github/github.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:timeago/timeago.dart';

class GithubAPI {
  final GitHub _github = GitHub();

  Future<File?> latestRelease(String org, repoName) async {
    try {
      var latestRelease = await _github.repositories.getLatestRelease(
        RepositorySlug(org, repoName),
      );
      String? url = latestRelease.assets
          ?.firstWhere((asset) =>
              asset.name != null &&
              (asset.name!.endsWith('.dex') || asset.name!.endsWith('.apk')) &&
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
    try {
      var contributors = _github.repositories.listContributors(
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
