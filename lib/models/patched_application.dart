import 'package:revanced_manager/models/patch.dart';

class PatchedApplication {
  final String name;
  final String packageName;
  final String version;
  final String apkFilePath;
  final bool isRooted;
  final bool isFromStorage;
  final List<Patch> appliedPatches;

  PatchedApplication({
    required this.name,
    required this.packageName,
    required this.version,
    required this.apkFilePath,
    required this.isRooted,
    required this.isFromStorage,
    this.appliedPatches = const <Patch>[],
  });
}
