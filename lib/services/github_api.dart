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
        '/repos/$repoName/releases',
      );
      return response.data[0];
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<Map<String, dynamic>?> getPatchesRelease(
    String repoName,
    String version,
  ) async {
    try {
      final response = await _dio.get(
        '/repos/$repoName/releases/tags/$version',
      );
      return response.data;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<Map<String, dynamic>?> getLatestPatchesRelease(
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
      while (response.data[updates]['tag_name'] != 'v$currentVersion') {
        updates++;
      }
      for (int i = 1; i < updates; i++) {
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

  Future<File?> getLatestReleaseFile(
    String extension,
    String repoName,
  ) async {
    try {
      final Map<String, dynamic>? release = await getLatestRelease(repoName);
      if (release != null) {
        final Map<String, dynamic>? asset =
            (release['assets'] as List<dynamic>).firstWhereOrNull(
          (asset) => (asset['name'] as String).endsWith(extension),
        );
        if (asset != null) {
          return await _downloadManager.getSingleFile(
            asset['browser_download_url'],
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

  Future<File?> getPatchesReleaseFile(
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
      final Map<String, dynamic>? release =
          await getPatchesRelease(repoName, version);
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
