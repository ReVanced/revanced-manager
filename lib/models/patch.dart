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
    _migrateV16ToV17(json);

    return _$PatchFromJson(json);
  }

  static void _migrateV16ToV17(Map<String, dynamic> json) {
    if (json['options'] == null) {
      json['options'] = [];
    }
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
    required this.values,
    required this.required,
    required this.valueType,
  });

  factory Option.fromJson(Map<String, dynamic> json) {
    _migrateV17ToV19(json);

    return _$OptionFromJson(json);
  }

  static void _migrateV17ToV19(Map<String, dynamic> json) {
    if (json['valueType'] == null) {
      final type = json['optionClassType'];
      if (type is String) {
        json['valueType'] = type
            .replaceAll('PatchOption', '')
            .replaceAll('List', 'Array');

        json['optionClassType'] = null;
      }
    }
  }

  final String key;
  final String title;
  final String description;
  final dynamic value;
  final Map<String, dynamic>? values;
  final bool required;
  final String valueType;

  Map toJson() => _$OptionToJson(this);
}
