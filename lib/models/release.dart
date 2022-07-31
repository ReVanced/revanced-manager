import 'package:revanced_manager_flutter/models/release_asset.dart';

class Release {
  String? tagName;
  String? publishedAt;
  bool? isPrerelease;
  List<ReleaseAsset>? assets;
  String? body;
}
