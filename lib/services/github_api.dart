import 'dart:convert';
import 'dart:io';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_http_cache_lts/dio_http_cache_lts.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:sentry_dio/sentry_dio.dart';

@lazySingleton
class GithubAPI {
  late Dio _dio = Dio();
  final DioCacheManager _dioCacheManager = DioCacheManager(CacheConfig());
  final Options _cacheOptions = buildCacheOptions(
    const Duration(hours: 6),
    maxStale: const Duration(days: 1),
  );
  final Map<String, String> repoAppPath = {
    'com.google.android.youtube': 'youtube',
    'com.google.android.apps.youtube.music': 'music',
    'com.twitter.android': 'twitter',
    'com.reddit.frontpage': 'reddit',
    'com.zhiliaoapp.musically': 'tiktok',
    'de.dwd.warnapp': 'warnwetter',
    'com.garzotto.pflotsh.ecmwf_a': 'ecmwf',
    'com.spotify.music': 'spotify',
  };

  void initialize(String repoUrl) async {
    try {
      _dio = Dio(BaseOptions(
        baseUrl: repoUrl,
      ));

      _dio.interceptors.add(_dioCacheManager.interceptor);
      _dio.addSentry(
        captureFailedRequests: true,
      );
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
    }
  }

  Future<void> clearAllCache() async {
    try {
      await _dioCacheManager.clearAll();
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
    }
  }

  Future<Map<String, dynamic>?> getLatestRelease(String repoName) async {
    try {
      var response = await _dio.get(
        '/repos/$repoName/releases',
        options: _cacheOptions,
      );
      return response.data[0];
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return null;
    }
  }

  Future<List<String>> getCommits(
    String packageName,
    String repoName,
    DateTime since,
  ) async {
    String path =
        'src/main/kotlin/app/revanced/patches/${repoAppPath[packageName]}';
    try {
      var response = await _dio.get(
        '/repos/$repoName/commits',
        queryParameters: {
          'path': path,
          'since': since.toIso8601String(),
        },
        options: _cacheOptions,
      );
      List<dynamic> commits = response.data;
      return commits
          .map(
            (commit) => (commit['commit']['message']).split('\n')[0] +
                ' - ' +
                commit['commit']['author']['name'] +
                '\n' as String,
          )
          .toList();
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return List.empty();
    }
  }

  Future<File?> getLatestReleaseFile(String extension, String repoName) async {
    try {
      Map<String, dynamic>? release = await getLatestRelease(repoName);
      if (release != null) {
        Map<String, dynamic>? asset =
            (release['assets'] as List<dynamic>).firstWhereOrNull(
          (asset) => (asset['name'] as String).endsWith(extension),
        );
        if (asset != null) {
          return await DefaultCacheManager().getSingleFile(
            asset['browser_download_url'],
          );
        }
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return null;
    }
    return null;
  }

  Future<List<Patch>> getPatches(String repoName) async {
    List<Patch> patches = [];
    try {
      File? f = await getLatestReleaseFile('.json', repoName);
      if (f != null) {
        List<dynamic> list = jsonDecode(f.readAsStringSync());
        patches = list.map((patch) => Patch.fromJson(patch)).toList();
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return List.empty();
    }
    return patches;
  }

  Future<String> getLastestReleaseVersion(String repoName) async {
    try {
      Map<String, dynamic>? release = await getLatestRelease(repoName);
      if (release != null) {
        return release['tag_name'];
      } else {
        return 'Unknown';
      }
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return '';
    }
  }
}
