import 'dart:io';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/github_api.dart';

class ManagerAPI {
  final GithubAPI _githubAPI = GithubAPI();

  Future<File?> downloadPatches() async {
    return await _githubAPI.latestReleaseFile(ghOrg, patchesRepo);
  }

  Future<File?> downloadIntegrations() async {
    return await _githubAPI.latestReleaseFile(ghOrg, integrationsRepo);
  }

  Future<File?> downloadManager() async {
    return await _githubAPI.latestReleaseFile(
      'Aunali321',
      'revanced-manager-flutter',
    );
  }

  Future<String?> getLatestPatchesVersion() async {
    return await _githubAPI.latestReleaseVersion(ghOrg, patchesRepo);
  }

  Future<String?> getLatestManagerVersion() async {
    return await _githubAPI.latestReleaseVersion(
      'Aunali321',
      'revanced-manager-flutter',
    );
  }

  Future<String> getCurrentManagerVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  Future<bool> hasAppUpdates(String packageName) async {
    // TODO: get status based on last update time on the folder of this app?
    return true;
  }

  Future<String> getAppChangelog(String packageName) async {
    // TODO: get changelog based on last commits on the folder of this app?
    return 'to be implemented';
  }
}
