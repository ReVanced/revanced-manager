import 'package:json_annotation/json_annotation.dart';

part 'tool.g.dart';

@JsonSerializable()
class Tool {
  final String repository;
  final String version;
  final String timestamp;
  final String name;
  final String? size;
  @JsonKey(name: 'browser_download_url')
  final String browserDownloadUrl;
  @JsonKey(name: 'content_type')
  final String contentType;

  const Tool({
    required this.repository,
    required this.version,
    required this.timestamp,
    required this.name,
    required this.size,
    required this.browserDownloadUrl,
    required this.contentType,
  });

  factory Tool.fromJson(Map<String, dynamic> json) => _$ToolFromJson(json);

  Map<String, dynamic> toJson() => _$ToolToJson(this);
}
