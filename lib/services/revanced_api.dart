import 'dart:developer';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_cache_interceptor/dio_cache_interceptor.dart';
import 'package:dio_cache_interceptor_hive_store/dio_cache_interceptor_hive_store.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:native_dio_adapter/native_dio_adapter.dart';
import 'package:path_provider/path_provider.dart' as path_provider;
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/utils/check_for_gms.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class RevancedAPI {
  late Dio _dio = Dio();

  Future<CacheOptions> getCacheOptions() async {
    final cacheDir = await path_provider.getTemporaryDirectory();
    return CacheOptions(
      store: HiveCacheStore(cacheDir.path),
      policy: CachePolicy.forceCache,
      hitCacheOnErrorExcept: [],
      maxStale: const Duration(days: 1),
      keyBuilder: (request) =>
          CacheOptions.defaultCacheKeyBuilder(request),
    );
  }

  Future<void> initialize(String apiUrl) async {
    try {
      final cacheOptions = await getCacheOptions();
      final bool isGMSInstalled = await checkForGMS();

      if (!isGMSInstalled) {
        _dio = Dio(
          BaseOptions(
            baseUrl: apiUrl,
          ),
        );
        log('ReVanced API: Using default engine');
      } else {
        _dio = Dio(
          BaseOptions(
            baseUrl: apiUrl,
          ),
        )..httpClientAdapter = NativeAdapter();
        log('ReVanced API: Using CronetEngine');
      }
      _dio.interceptors.add(
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
      );
      _dio.interceptors.add(DioCacheInterceptor(options: cacheOptions));
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
