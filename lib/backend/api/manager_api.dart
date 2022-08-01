import 'dart:io';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart' as p;

// use path_provider to get the path of the storage directory

void getPath() async {
  final path = await p.getExternalStorageDirectory();
  print(path);
}
