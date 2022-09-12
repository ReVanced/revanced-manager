import 'dart:io';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_http_cache_lts/dio_http_cache_lts.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:timeago/timeago.dart';

@lazySingleton
class RevancedAPI {
  final String apiUrl = 'https://revanced-releases-api.afterst0rm.xyz';
  final Dio _dio = Dio();
  final DioCacheManager _dioCacheManager = DioCacheManager(CacheConfig());
  final Options _cacheOptions = buildCacheOptions(
    const Duration(hours: 1),
    maxStale: const Duration(days: 7),
  );

  void initialize() {
    _dio.interceptors.add(_dioCacheManager.interceptor);
  }

  Future<void> clearAllCache() async {
    await _dioCacheManager.clearAll();
  }

  Future<Map<String, List<dynamic>>> getContributors() async {
    Map<String, List<dynamic>> contributors = {};
    try {
      var response = await _dio.get(
        '$apiUrl/contributors',
        options: _cacheOptions,
      );
      List<dynamic> repositories = response.data['repositories'];
      for (Map<String, dynamic> repo in repositories) {
        String name = repo['name'];
        contributors[name] = repo['contributors'];
      }
    } on Exception {
      return {};
    }
    return contributors;
  }

  Future<List<Patch>> getPatches() async {
    try {
      var response = await _dio.get('$apiUrl/patches', options: _cacheOptions);
      List<dynamic> patches = response.data;
      return patches.map((patch) => Patch.fromJson(patch)).toList();
    } on Exception {
      return List.empty();
    }
  }

  Future<Map<String, dynamic>?> _getLatestRelease(
    String extension,
    String repoName,
  ) async {
    try {
      var response = await _dio.get('$apiUrl/tools', options: _cacheOptions);
      List<dynamic> tools = response.data['tools'];
      return tools.firstWhereOrNull(
        (t) =>
            t['repository'] == repoName &&
            (t['name'] as String).endsWith(extension),
      );
    } on Exception {
      return null;
    }
  }

  Future<String?> getLatestReleaseVersion(
      String extension, String repoName) async {
    try {
      Map<String, dynamic>? release =
          await _getLatestRelease(extension, repoName);
      if (release != null) {
        return release['version'];
      }
    } on Exception {
      return null;
    }
    return null;
  }

  Future<File?> getLatestReleaseFile(String extension, String repoName) async {
    try {
      Map<String, dynamic>? release = await _getLatestRelease(
        extension,
        repoName,
      );
      if (release != null) {
        String url = release['browser_download_url'];
        return await DefaultCacheManager().getSingleFile(url);
      }
    } on Exception {
      return null;
    }
    return null;
  }

  Future<String?> getLatestReleaseTime(
    String extension,
    String repoName,
  ) async {
    try {
      Map<String, dynamic>? release = await _getLatestRelease(
        extension,
        repoName,
      );
      if (release != null) {
        DateTime timestamp = DateTime.parse(release['timestamp'] as String);
        return format(timestamp, locale: 'en_short');
      }
    } on Exception {
      return null;
    }
    return null;
  }
}
