import 'dart:async';
import 'dart:developer';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_cache_interceptor/dio_cache_interceptor.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class RevancedAPI {
  late Dio _dio = Dio();

  final _cacheOptions = CacheOptions(
    store: MemCacheStore(),
    maxStale: const Duration(days: 1),
    priority: CachePriority.high,
  );

  Future<void> initialize(String apiUrl) async {
    try {
      _dio = Dio(
        BaseOptions(
          baseUrl: apiUrl,
        ),
      );

      _dio.interceptors.add(DioCacheInterceptor(options: _cacheOptions));
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> clearAllCache() async {
    try {
      await _cacheOptions.store!.clean();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<Map<String, List<dynamic>>> getContributors() async {
    final Map<String, List<dynamic>> contributors = {};
    try {
      final response = await _dio.get('/contributors');
      final List<dynamic> repositories = response.data['repositories'];
      for (final Map<String, dynamic> repo in repositories) {
        final String name = repo['name'];
        contributors[name] = repo['contributors'];
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return {};
    }
    return contributors;
  }

  Future<List<Patch>> getPatches() async {
    try {
      final response = await _dio.get('/patches');
      final List<dynamic> patches = response.data;
      return patches.map((patch) => Patch.fromJson(patch)).toList();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return List.empty();
    }
  }

  Future<Map<String, dynamic>?> _getLatestRelease(
    String extension,
    String repoName,
  ) async {
    try {
      final response = await _dio.get('/tools');
      final List<dynamic> tools = response.data['tools'];
      return tools.firstWhereOrNull(
        (t) =>
            t['repository'] == repoName &&
            (t['name'] as String).endsWith(extension),
      );
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
  }

  Future<String?> getLatestReleaseVersion(
    String extension,
    String repoName,
  ) async {
    try {
      final Map<String, dynamic>? release = await _getLatestRelease(
        extension,
        repoName,
      );
      if (release != null) {
        return release['version'];
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
    return null;
  }

  Future<File?> getLatestReleaseFile(
    String extension,
    String repoName,
  ) async {
    try {
      final Map<String, dynamic>? release = await _getLatestRelease(
        extension,
        repoName,
      );
      if (release != null) {
        final String url = release['browser_download_url'];
        return await DefaultCacheManager().getSingleFile(url);
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
    return null;
  }

  StreamController<double> managerUpdateProgress = StreamController<double>();

  void updateManagerDownloadProgress(int progress) {
    managerUpdateProgress.add(progress.toDouble());
  }

  Stream<double> getManagerUpdateProgress() {
    return managerUpdateProgress.stream;
  }

  void disposeManagerUpdateProgress() {
    managerUpdateProgress.close();
  }

  Future<File?> downloadManager() async {
    final Map<String, dynamic>? release = await _getLatestRelease(
      '.apk',
      'revanced/revanced-manager',
    );
    File? outputFile;
    await for (final result in DefaultCacheManager().getFileStream(
      release!['browser_download_url'] as String,
      withProgress: true,
    )) {
      if (result is DownloadProgress) {
        final totalSize = result.totalSize ?? 10000000;
        final progress = (result.downloaded / totalSize * 100).round();

        updateManagerDownloadProgress(progress);
      } else if (result is FileInfo) {
        // The download is complete; convert the FileInfo object to a File object
        outputFile = File(result.file.path);
      }
    }
    return outputFile;
  }

  Future<String?> getLatestReleaseTime(
    String extension,
    String repoName,
  ) async {
    try {
      final Map<String, dynamic>? release = await _getLatestRelease(
        extension,
        repoName,
      );
      if (release != null) {
        final DateTime timestamp =
            DateTime.parse(release['timestamp'] as String);
        return format(timestamp, locale: 'en_short');
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return null;
    }
    return null;
  }
}
