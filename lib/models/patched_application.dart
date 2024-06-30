import 'dart:convert';
import 'dart:typed_data';
import 'package:json_annotation/json_annotation.dart';

part 'patched_application.g.dart';

@JsonSerializable()
class PatchedApplication {
  PatchedApplication({
    required this.name,
    required this.packageName,
    required this.version,
    required this.apkFilePath,
    required this.icon,
    required this.patchDate,
    this.isRooted = false,
    this.isFromStorage = false,
    this.appliedPatches = const [],
    this.patchedFilePath = '',
    this.fileSize = 0,
  });

  factory PatchedApplication.fromJson(Map<String, dynamic> json) =>
      _$PatchedApplicationFromJson(json);
  String name;
  String packageName;
  String version;
  final String apkFilePath;
  @JsonKey(
    fromJson: decodeBase64,
    toJson: encodeBase64,
  )
  Uint8List icon;
  DateTime patchDate;
  bool isRooted;
  bool isFromStorage;
  List<String> appliedPatches;
  String patchedFilePath;
  int fileSize;

  Map<String, dynamic> toJson() => _$PatchedApplicationToJson(this);

  static Uint8List decodeBase64(String icon) => base64.decode(icon);

  static String encodeBase64(Uint8List bytes) => base64.encode(bytes);
}
