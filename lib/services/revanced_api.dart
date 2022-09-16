import 'dart:io';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_http_cache_lts/dio_http_cache_lts.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:timeago/timeago.dart';
import 'package:shared_preferences/shared_preferences.dart';

@lazySingleton
class RevancedAPI {
  String defaultApiUrl = 'https://revanced-releases-api.afterst0rm.xyz';
  String apiUrl = 'https://revanced-releases-api.afterst0rm.xyz';
  final Dio _dio = Dio();
  final DioCacheManager _dioCacheManager = DioCacheManager(CacheConfig());
  late SharedPreferences _prefs;
  final Options _cacheOptions = buildCacheOptions(
    const Duration(days: 1),
    maxStale: const Duration(days: 7),
  );

  Future<void> initialize() async {
    _dio.interceptors.add(_dioCacheManager.interceptor);
    _prefs = await SharedPreferences.getInstance();
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

  String getApiUrl() {
    return _prefs.getString('apiUrl') ?? defaultApiUrl;
  }

  Future<void> setApiUrl(String url) async {
    if (url.isEmpty || url == ' ') {
      url = defaultApiUrl;
    }
    await _prefs.setString('apiUrl', url);
  }
}
