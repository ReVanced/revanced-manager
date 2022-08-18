import 'dart:io';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/services/github_api.dart';

class ManagerAPI {
  final GithubAPI _githubAPI = GithubAPI();

  Future<File?> downloadPatches() async {
    return await _githubAPI.latestRelease(ghOrg, patchesRepo);
  }

  Future<File?> downloadIntegrations() async {
    return await _githubAPI.latestRelease(ghOrg, integrationsRepo);
  }

  Future<File?> downloadManager() async {
    return await _githubAPI.latestRelease(ghOrg, managerRepo);
  }
}
