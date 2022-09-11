import 'dart:convert';
import 'dart:io';
// ignore: depend_on_referenced_packages
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:dio_http_cache_lts/dio_http_cache_lts.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:revanced_manager/models/patch.dart';

class GithubAPI {
  final String apiUrl = 'https://api.github.com';
  final Dio _dio = Dio();
  final DioCacheManager _dioCacheManager = DioCacheManager(
    CacheConfig(
      defaultMaxAge: const Duration(hours: 1),
      defaultMaxStale: const Duration(days: 7),
    ),
  );
  final Map<String, String> repoAppPath = {
    'com.google.android.youtube': 'youtube',
    'com.google.android.apps.youtube.music': 'music',
    'com.twitter.android': 'twitter',
    'com.reddit.frontpage': 'reddit',
    'com.zhiliaoapp.musically': 'tiktok',
    'de.dwd.warnapp': 'warnwetter',
    'com.garzotto.pflotsh.ecmwf_a': 'ecmwf',
  };

  void initialize() {
    _dio.interceptors.add(_dioCacheManager.interceptor);
  }

  Future<void> clearAllCache() async {
    await _dioCacheManager.clearAll();
  }

  Future<Map<String, dynamic>?> _getLatestRelease(String repoName) async {
    try {
      var response = await _dio.get(
        '$apiUrl/repos/$repoName/releases/latest',
        options: buildCacheOptions(const Duration(hours: 1)),
      );
      if (response.headers.value(DIO_CACHE_HEADER_KEY_DATA_SOURCE) != null) {
        print('1 - From cache');
      } else {
        print('1 - From net');
      }
      return response.data;
    } on Exception {
      // ignore
    }
    return null;
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
        '$apiUrl/repos/$repoName/commits',
        queryParameters: {
          'path': path,
          'per_page': 3,
          'since': since.toIso8601String(),
        },
        options: buildCacheOptions(const Duration(hours: 1)),
      );
      if (response.headers.value(DIO_CACHE_HEADER_KEY_DATA_SOURCE) != null) {
        print('2 - From cache');
      } else {
        print('2 - From net');
      }
      List<dynamic> commits = response.data;
      return commits
          .map((commit) =>
              (commit['commit']['message'] as String).split('\n')[0])
          .toList();
    } on Exception {
      // ignore
    }
    return List.empty();
  }

  Future<File?> getLatestReleaseFile(String extension, String repoName) async {
    try {
      Map<String, dynamic>? release = await _getLatestRelease(repoName);
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
    } on Exception {
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
    } on Exception {
      // ignore
    }
    return patches;
  }
}
