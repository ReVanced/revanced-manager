import 'package:dio/dio.dart';
import 'package:dio_cache_interceptor/dio_cache_interceptor.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_cache_manager/file.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';

@lazySingleton
class DownloadManager {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  late final String _userAgent;

  final _cacheOptions = CacheOptions(
    store: MemCacheStore(),
    maxStale: const Duration(days: 1),
    priority: CachePriority.high,
  );

  Future<void> initialize() async {
    _userAgent =
        'ReVanced-Manager/${await _managerAPI.getCurrentManagerVersion()}';
  }

  Dio initDio(String url) {
    var dio = Dio();
    try {
      dio = Dio(
        BaseOptions(
          baseUrl: url,
          headers: {
            'User-Agent': _userAgent,
          },
        ),
      );
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }

    dio.interceptors.add(DioCacheInterceptor(options: _cacheOptions));
    return dio;
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

  Future<File> getSingleFile(String url) async {
    return DefaultCacheManager().getSingleFile(
      url,
      headers: {
        'User-Agent': _userAgent,
      },
    );
  }

  Stream<FileResponse> getFileStream(String url) {
    return DefaultCacheManager().getFileStream(
      url,
      withProgress: true,
      headers: {
        'User-Agent': _userAgent,
      },
    );
  }
}
