import 'dart:typed_data';
import 'package:json_annotation/json_annotation.dart';

part 'patched_application.g.dart';

@JsonSerializable()
class PatchedApplication {
  final String name;
  final String packageName;
  final String version;
  final String apkFilePath;
  @JsonKey(
    fromJson: bytesFromString,
    toJson: bytesToString,
  )
  final Uint8List icon;
  final DateTime patchDate;
  final bool isRooted;
  final bool isFromStorage;
  final List<String> appliedPatches;

  PatchedApplication({
    required this.name,
    required this.packageName,
    required this.version,
    required this.apkFilePath,
    required this.icon,
    required this.patchDate,
    required this.isRooted,
    required this.isFromStorage,
    this.appliedPatches = const <String>[],
  });

  factory PatchedApplication.fromJson(Map<String, dynamic> json) =>
      _$PatchedApplicationFromJson(json);

  Map toJson() => _$PatchedApplicationToJson(this);

  static Uint8List bytesFromString(String pictureUrl) =>
      Uint8List.fromList(pictureUrl.codeUnits);

  static String bytesToString(Uint8List bytes) => String.fromCharCodes(bytes);
}
