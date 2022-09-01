import 'package:json_annotation/json_annotation.dart';
import 'package:revanced_manager/utils/string.dart';

part 'patch.g.dart';

@JsonSerializable()
class Patch {
  final String name;
  final String description;
  final String version;
  final bool excluded;
  final List<String> dependencies;
  final List<Package> compatiblePackages;

  Patch({
    required this.name,
    required this.description,
    required this.version,
    required this.excluded,
    required this.dependencies,
    required this.compatiblePackages,
  });

  factory Patch.fromJson(Map<String, dynamic> json) => _$PatchFromJson(json);

  Map<String, dynamic> toJson() => _$PatchToJson(this);

  String getSimpleName() {
    return name
        .replaceAll('-', ' ')
        .split('-')
        .join(' ')
        .toTitleCase()
        .replaceFirst('Microg', 'MicroG');
  }
}

@JsonSerializable()
class Package {
  final String name;
  final List<String> versions;

  Package({
    required this.name,
    required this.versions,
  });

  factory Package.fromJson(Map<String, dynamic> json) =>
      _$PackageFromJson(json);

  Map toJson() => _$PackageToJson(this);
}
