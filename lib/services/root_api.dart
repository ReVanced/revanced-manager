import 'dart:io';

import 'package:injectable/injectable.dart';
import 'package:root/root.dart';

@lazySingleton
class RootAPI {
  final String managerDirPath = "/data/adb/revanced_manager";
  final String postFsDataDirPath = "/data/adb/post-fs-data.d";
  final String serviceDDirPath = "/data/adb/service.d";

  bool deleteApp(String packageName) {
    try {
      File('$managerDirPath/$packageName.apk').deleteSync();
      File('$serviceDDirPath/$packageName.sh').deleteSync();
      File('$postFsDataDirPath/$packageName.sh').deleteSync();
      return true;
    } on Exception {
      return false;
    }
  }

  Future<bool> installApp(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    try {
      Directory managerDir = Directory(managerDirPath);
      managerDir.createSync();
      String newPatchedFilePath = '$managerDirPath/$packageName.apk';
      installServiceDScript(
        packageName,
        originalFilePath,
        newPatchedFilePath,
      );
      installPostFsDataScript(
        packageName,
        originalFilePath,
        newPatchedFilePath,
      );
      File(patchedFilePath).renameSync(newPatchedFilePath);
      await Root.exec(
        cmd: 'chmod 644 $newPatchedFilePath',
      );
      await Root.exec(
        cmd: 'chown system:system $newPatchedFilePath',
      );
      await Root.exec(
        cmd: 'chcon u:object_r:apk_data_file:s0 $newPatchedFilePath',
      );
      return true;
    } on Exception {
      return false;
    }
  }

  Future<void> installServiceDScript(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    String content = '#!/system/bin/sh\n'
        'while [ "\$(getprop sys.boot_completed | tr -d \'\r\')" != "1" ]; do sleep 1; done\n'
        'sleep 1\n'
        'chcon u:object_r:apk_data_file:s0 $patchedFilePath\n'
        'mount -o bind $patchedFilePath $originalFilePath';
    File scriptFile = File('$serviceDDirPath/$packageName.sh');
    await scriptFile.writeAsString(content);
    await Root.exec(cmd: 'chmod 744 ${scriptFile.path}');
  }

  Future<void> installPostFsDataScript(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    String content = '#!/system/bin/sh\n'
        'while read line; do echo \$line | grep $originalFilePath | '
        'awk \'{print \$2}\' | xargs umount -l; done< /proc/mounts';
    File scriptFile = File('$postFsDataDirPath/$packageName.sh');
    await scriptFile.writeAsString(content);
    await Root.exec(cmd: 'chmod 744 ${scriptFile.path}');
  }
}
