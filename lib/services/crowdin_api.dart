import 'package:dio/dio.dart';
import 'package:dio_http_cache_lts/dio_http_cache_lts.dart';
import 'package:injectable/injectable.dart' hide Environment;
import 'package:revanced_manager/utils/environment.dart';
import 'package:sentry_dio/sentry_dio.dart';
import 'package:sentry_flutter/sentry_flutter.dart';

@lazySingleton
class CrowdinAPI {
  late Dio _dio = Dio();
  final DioCacheManager _dioCacheManager = DioCacheManager(CacheConfig());
  final apiKey = Environment.crowdinKEY;

  Future<void> initialize() async {
    try {
      _dio = Dio(
        BaseOptions(
          baseUrl: 'https://api.crowdin.com/api/v2',
        ),
      );

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

  Future<List> getLanguages() async {
    try {
      final response = await _dio.get(
        '/projects',
        options: buildCacheOptions(
          const Duration(hours: 6),
          maxStale: const Duration(days: 1),
          options: Options(
            headers: {
              'Authorization': 'Bearer $apiKey',
            },
            contentType: 'application/json',
          ),
        ),
      );
      final List targetLanguages =
          await response.data['data'][0]['data']['targetLanguages'];

      return targetLanguages;
    } on Exception catch (e, s) {
      await Sentry.captureException(e, stackTrace: s);
      return [];
    }
  }
}
