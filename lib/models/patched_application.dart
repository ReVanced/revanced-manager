import 'dart:convert';
import 'dart:typed_data';
import 'package:json_annotation/json_annotation.dart';

part 'patched_application.g.dart';

@JsonSerializable()
class PatchedApplication {
  String name;
  final String packageName;
  String version;
  final String apkFilePath;
  @JsonKey(
    fromJson: decodeBase64,
    toJson: encodeBase64,
  )
  Uint8List icon;
  DateTime patchDate;
  final bool isRooted;
  bool hasUpdates;
  List<String> appliedPatches;
  List<String> changelog;

  PatchedApplication({
    required this.name,
    required this.packageName,
    required this.version,
    required this.apkFilePath,
    required this.icon,
    required this.patchDate,
    this.isRooted = false,
    this.hasUpdates = false,
    this.appliedPatches = const [],
    this.changelog = const [],
  });

  factory PatchedApplication.fromJson(Map<String, dynamic> json) =>
      _$PatchedApplicationFromJson(json);

  Map<String, dynamic> toJson() => _$PatchedApplicationToJson(this);

  static Uint8List decodeBase64(String icon) => base64.decode(icon);

  static String encodeBase64(Uint8List bytes) => base64.encode(bytes);
}
