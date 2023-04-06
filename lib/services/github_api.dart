import 'dart:convert';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_cache_interceptor/dio_cache_interceptor.dart';
import 'package:dio_cache_interceptor_hive_store/dio_cache_interceptor_hive_store.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart';
import 'package:revanced_manager/models/patch.dart';

@lazySingleton
class GithubAPI {
  late Dio _dio = Dio();

  Future<CacheOptions> getCacheOptions() async {
    final cacheDir = await getTemporaryDirectory();
    return CacheOptions(
      store: HiveCacheStore(cacheDir.path),
      policy: CachePolicy.forceCache,
      hitCacheOnErrorExcept: [],
      maxStale: const Duration(days: 1),
      keyBuilder: (request) =>
          CacheOptions.defaultCacheKeyBuilder(request),
    );
  }

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

  Future<void> initialize(String repoUrl) async {
    try {
      final cacheOptions = await getCacheOptions();
      _dio = Dio(
        BaseOptions(
          baseUrl: repoUrl,
        ),
      )
        ..interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) async {
              final key = CacheOptions.defaultCacheKeyBuilder(options);
              final cache = await cacheOptions.store?.get(key);
              if (cache != null) {
                return handler.resolve(cache.toResponse(options));
              }
              return handler.next(options);
            },
          ),
        )
        ..interceptors.add(DioCacheInterceptor(options: cacheOptions));
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<void> clearAllCache() async {
    try {
      await getCacheOptions().then((cacheOptions) async {
        await cacheOptions.store?.clean();
      });
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
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

  Future<List<String>> getCommits(
    String packageName,
    String repoName,
    DateTime since,
  ) async {
    final String path =
        'src/main/kotlin/app/revanced/patches/${repoAppPath[packageName]}';
    try {
      final response = await _dio.get(
        '/repos/$repoName/commits',
        queryParameters: {
          'path': path,
          'since': since.toIso8601String(),
        },
      );
      final List<dynamic> commits = response.data;
      return commits
          .map(
            (commit) => (commit['commit']['message']).split('\n')[0] +
                ' - ' +
                commit['commit']['author']['name'] +
                '\n' as String,
          )
          .toList();
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
    return [];
  }

  Future<File?> getLatestReleaseFile(
    String extension,
    String repoName,
  ) async {
    try {
      final Map<String, dynamic>? release =
          await getLatestRelease(repoName);
      if (release != null) {
        final Map<String, dynamic>? asset =
            (release['assets'] as List<dynamic>).firstWhereOrNull(
          (asset) => (asset['name'] as String).endsWith(extension),
        );
        if (asset != null) {
          return await DefaultCacheManager().getSingleFile(
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

  Future<List<Patch>> getPatches(String repoName) async {
    List<Patch> patches = [];
    try {
      final File? f = await getLatestReleaseFile('.json', repoName);
      if (f != null) {
        final List<dynamic> list = jsonDecode(f.readAsStringSync());
        patches = list.map((patch) => Patch.fromJson(patch)).toList();
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }

    return patches;
  }

  Future<String> getLastestReleaseVersion(String repoName) async {
    try {
      final Map<String, dynamic>? release =
          await getLatestRelease(repoName);
      if (release != null) {
        return release['tag_name'];
      } else {
        return 'Unknown';
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }

      return 'Unknown';
    }
  }
}
