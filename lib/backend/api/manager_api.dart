import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart' as p;
import 'package:revanced_manager_flutter/constants.dart';
import 'github_api.dart';

// use path_provider to get the path of the storage directory

class ManagerAPI {
  Dio dio = Dio();
  GithubAPI githubAPI = GithubAPI();

  Future<String?> getPath() async {
    final path = await p.getApplicationSupportDirectory();
    final workDir = Directory('${path.path}/revanced').createSync();
    final workDirPath = "${path.path}/revanced";
    return workDirPath;
  }

  Future<File?> downloadAssets(String repo) async {
    try {
      final workDir = await getPath();
      final dlUrl = await githubAPI.latestRelease(ghOrg, repo);
      final name = dlUrl
          ?.split('/')
          .lastWhere((element) => element.contains('revanced'));
      print(name);
      final assetFile = File('$workDir/$name');
      final response = await dio.get(
        dlUrl!,
        options: Options(
          responseType: ResponseType.bytes,
          followRedirects: true,
          receiveTimeout: 0,
        ),
        onReceiveProgress: (count, total) {
          print('$count/$total');
        },
      );
      final raf = assetFile.openSync(mode: FileMode.write);
      raf.writeFromSync(response.data);
      raf.closeSync();
      return assetFile;
    } catch (e) {
      print(e);
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
