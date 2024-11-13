import 'dart:io';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/download_manager.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:synchronized/synchronized.dart';

@lazySingleton
class GithubAPI {
  late final Dio _dio;
  late final ManagerAPI _managerAPI = locator<ManagerAPI>();
  late final DownloadManager _downloadManager = locator<DownloadManager>();
  final Map<String, Lock> _lockMap = {};

  Future<void> initialize(String repoUrl) async {
    _dio = _downloadManager.initDio(repoUrl);
  }

  Future<void> clearAllCache() async {
    await _downloadManager.clearAllCache();
  }

  Future<Response> _dioGetSynchronously(String path) async {
    // Create a new Lock for each path
    if (!_lockMap.containsKey(path)) {
      _lockMap[path] = Lock();
    }
    return _lockMap[path]!.synchronized(() async {
      return await _dio.get(path);
    });
  }

  Future<Map<String, dynamic>?> getLatestRelease(
    String repoName,
  ) async {
    try {
      final response = await _dioGetSynchronously(
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

  Future<String?> getChangelogs(bool isPatches) async {
    final String repoName = isPatches
        ? _managerAPI.getPatchesRepo()
        : _managerAPI.defaultManagerRepo;
    try {
      final response = await _dioGetSynchronously(
        '/repos/$repoName/releases?per_page=50',
      );
      final buffer = StringBuffer();
      final String version = isPatches
          ? _managerAPI.getLastUsedPatchesVersion()
          : await _managerAPI.getCurrentManagerVersion();
      int releases = 0;
      for (final release in response.data) {
        if (release['tag_name'] == version) {
          if (buffer.isEmpty) {
            buffer.writeln(release['body']);
            releases++;
          }
          break;
        }
        if (release['prerelease']) {
          continue;
        }
        buffer.writeln(release['body']);
        releases++;
        if (isPatches && releases == 10) {
          break;
        }
      }
      return buffer.toString();
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
      final response = await _dioGetSynchronously(
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
          _managerAPI.setPatchesDownloadURL(downloadUrl);
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
