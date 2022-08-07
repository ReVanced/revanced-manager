import 'dart:io';
import 'package:dio/dio.dart';
import 'package:injectable/injectable.dart';
import 'package:path_provider/path_provider.dart' as p;
import 'package:revanced_manager/constants.dart';
import 'github_api.dart';

// use path_provider to get the path of the storage directory
@lazySingleton
class ManagerAPI {
  Dio dio = Dio();
  GithubAPI githubAPI = GithubAPI();

  Future<String?> getPath() async {
    final path = await p.getApplicationSupportDirectory();
    Directory('${path.path}/revanced').createSync();
    final workDirPath = '${path.path}/revanced';
    return workDirPath;
  }

  Future<File?> downloadAssets(String repo) async {
    try {
      final workDir = await getPath();
      final dlUrl = await githubAPI.latestRelease(ghOrg, repo);
      final name = dlUrl
          ?.split('/')
          .lastWhere((element) => element.contains('revanced'));
      final assetFile = File('$workDir/$name');
      final response = await dio.get(
        dlUrl!,
        options: Options(
          responseType: ResponseType.bytes,
          followRedirects: true,
          receiveTimeout: 0,
        ),
      );
      final raf = assetFile.openSync(mode: FileMode.write);
      raf.writeFromSync(response.data);
      raf.closeSync();
      return assetFile;
    } catch (e) {
      return null;
    }
  }

  Future<void> downloadPatches() async {
    await downloadAssets(patchesRepo);
  }

  Future<void> downloadIntegrations() async {
    await downloadAssets(integrationsRepo);
  }
}
