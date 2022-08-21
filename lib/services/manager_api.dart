import 'dart:io';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/github_api.dart';

class ManagerAPI {
  final GithubAPI _githubAPI = GithubAPI();

  Future<File?> downloadPatches(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, ghOrg, patchesRepo);
  }

  Future<File?> downloadIntegrations(String extension) async {
    return await _githubAPI.latestReleaseFile(
      extension,
      ghOrg,
      integrationsRepo,
    );
  }

  Future<File?> downloadManager(String extension) async {
    return await _githubAPI.latestReleaseFile(extension, ghOrg, managerRepo);
  }

  Future<String?> getLatestPatchesVersion() async {
    return await _githubAPI.latestReleaseVersion(ghOrg, patchesRepo);
  }

  Future<String?> getLatestManagerVersion() async {
    return await _githubAPI.latestReleaseVersion(ghOrg, managerRepo);
  }

  Future<String> getCurrentManagerVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  Future<bool> hasAppUpdates(String packageName) async {
    // TODO: get status based on last update time on the folder of this app?
    return false;
  }

  Future<String> getAppChangelog(String packageName) async {
    // TODO: get changelog based on last commits on the folder of this app?
    return 'To be implemented';
  }
}
