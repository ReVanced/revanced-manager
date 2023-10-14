import 'package:json_annotation/json_annotation.dart';

part 'patch.g.dart';

@JsonSerializable()
class Patch {
  Patch({
    required this.name,
    required this.description,
    required this.excluded,
    required this.compatiblePackages,
    required this.options,
  });

  factory Patch.fromJson(Map<String, dynamic> json) {
    // See: https://github.com/ReVanced/revanced-manager/issues/1364#issuecomment-1760414618
    if (json['options'] == null) {
      json['options'] = [];
    }

    return _$PatchFromJson(json);
  }

  final String name;
  final String? description;
  final bool excluded;
  final List<Package> compatiblePackages;
  final List<Option> options;

  Map<String, dynamic> toJson() => _$PatchToJson(this);

  String getSimpleName() {
    return name;
  }
}

@JsonSerializable()
class Package {
  Package({
    required this.name,
    required this.versions,
  });

  factory Package.fromJson(Map<String, dynamic> json) =>
      _$PackageFromJson(json);

  final String name;
  final List<String> versions;

  Map toJson() => _$PackageToJson(this);
}

@JsonSerializable()
class Option {
  Option({
    required this.key,
    required this.title,
    required this.description,
    required this.value,
    required this.required,
    required this.optionClassType,
  });

  factory Option.fromJson(Map<String, dynamic> json) => _$OptionFromJson(json);

  final String key;
  final String title;
  final String description;
  dynamic value;
  final bool required;
  final String optionClassType;

  Map toJson() => _$OptionToJson(this);
}
