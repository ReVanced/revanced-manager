import 'dart:io';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/download_manager.dart';
import 'package:revanced_manager/services/manager_api.dart';

@lazySingleton
class GithubAPI {
  late final Dio _dio;
  late final ManagerAPI _managerAPI = locator<ManagerAPI>();
  late final DownloadManager _downloadManager = locator<DownloadManager>();

  Future<void> initialize(String repoUrl) async {
    _dio = _downloadManager.initDio(repoUrl);
  }

  Future<void> clearAllCache() async {
    await _downloadManager.clearAllCache();
  }

  Future<Map<String, dynamic>?> getLatestRelease(
    String repoName,
  ) async {
    try {
      final response = await _dio.get(
        '/repos/$repoName/releases/latest',
      );
      return response.data;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<Map<String, dynamic>?> getLatestReleaseWithPreReleases(
    String repoName,
  ) async {
    try {
      final response = await _dio.get('/repos/$repoName/releases?per_page=10');
      final List<dynamic> releases = response.data;

      /*
      * Loop through all releases (including pre-releases) and return the latest
      */
      Map<String, dynamic>? latestRelease;
      DateTime latestReleaseDate = DateTime.fromMillisecondsSinceEpoch(0);
      for (final release in releases) {
        final DateTime releaseDate = DateTime.parse(release['published_at']);
        if (releaseDate.isAfter(latestReleaseDate)) {
          latestReleaseDate = releaseDate;
          latestRelease = release;
        }
      }
      return latestRelease;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<Map<String, dynamic>?> getLatestManagerRelease(
    String repoName,
  ) async {
    try {
      final response = await _dio.get(
        '/repos/$repoName/releases',
      );
      final Map<String, dynamic> releases = response.data[0];
      int updates = 0;
      final String currentVersion =
          await _managerAPI.getCurrentManagerVersion();
      while (response.data[updates]['tag_name'] != currentVersion) {
        updates++;
      }
      for (int i = 1; i < updates; i++) {
        if (response.data[i]['prerelease']) {
          continue;
        }
        releases.update(
          'body',
          (value) =>
              value +
              '\n' +
              '# ' +
              response.data[i]['tag_name'] +
              '\n' +
              response.data[i]['body'],
        );
      }
      return releases;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<File?> getReleaseFile(
    String extension,
    String repoName,
    String version,
    String url,
  ) async {
    try {
      if (url.isNotEmpty) {
        return await _downloadManager.getSingleFile(
          url,
        );
      }
      final response = await _dio.get(
        '/repos/$repoName/releases/tags/$version',
      );
      final Map<String, dynamic>? release = response.data;
      if (release != null) {
        final Map<String, dynamic>? asset =
            (release['assets'] as List<dynamic>).firstWhereOrNull(
          (asset) => (asset['name'] as String).endsWith(extension),
        );
        if (asset != null) {
          final String downloadUrl = asset['browser_download_url'];
          if (extension == '.apk') {
            _managerAPI.setIntegrationsDownloadURL(downloadUrl);
          } else {
            _managerAPI.setPatchesDownloadURL(downloadUrl);
          }
          return await _downloadManager.getSingleFile(
            downloadUrl,
          );
        }
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
    return null;
  }
}
