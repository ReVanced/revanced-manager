import 'package:injectable/injectable.dart';
import 'package:root/root.dart';

@lazySingleton
class RootAPI {
  final String managerDirPath = "/data/adb/revanced_manager";
  final String postFsDataDirPath = "/data/adb/post-fs-data.d";
  final String serviceDDirPath = "/data/adb/service.d";

  Future<bool> checkApp(String packageName) async {
    try {
      String? res = await Root.exec(
        cmd: 'ls -la "$managerDirPath/$packageName"',
      );
      return res != null && res.isNotEmpty;
    } on Exception {
      return false;
    }
  }

  Future<void> deleteApp(String packageName, String originalFilePath) async {
    await Root.exec(
      cmd: 'am force-stop "$packageName"',
    );
    await Root.exec(
      cmd: 'su -mm -c "umount -l $originalFilePath"',
    );
    await Root.exec(
      cmd: 'rm -rf "$managerDirPath/$packageName"',
    );
    await Root.exec(
      cmd: 'rm -rf "$serviceDDirPath/$packageName.sh"',
    );
    await Root.exec(
      cmd: 'rm -rf "$postFsDataDirPath/$packageName.sh"',
    );
  }

  Future<bool> installApp(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    try {
      await Root.exec(
        cmd: 'mkdir -p "$managerDirPath/$packageName"',
      );
      installServiceDScript(packageName);
      installPostFsDataScript(packageName);
      installApk(packageName, patchedFilePath);
      mountApk(packageName, originalFilePath, patchedFilePath);
      return true;
    } on Exception {
      return false;
    }
  }

  Future<void> installServiceDScript(String packageName) async {
    String content = '#!/system/bin/sh\n'
        'while [ "\$(getprop sys.boot_completed | tr -d \'"\'"\'\\\\r\'"\'"\')" != "1" ]; do sleep 1; done\n'
        'base_path=$managerDirPath/$packageName/base.apk\n'
        'stock_path=\$(pm path $packageName | grep base | sed \'"\'"\'s/package://g\'"\'"\')\n'
        '[ ! -z \$stock_path ] && mount -o bind \$base_path \$stock_path';
    String scriptFilePath = '$serviceDDirPath/$packageName.sh';
    await Root.exec(
      cmd: 'echo \'$content\' > "$scriptFilePath"',
    );
    await Root.exec(
      cmd: 'chmod 744 "$scriptFilePath"',
    );
  }

  Future<void> installPostFsDataScript(String packageName) async {
    String content = '#!/system/bin/sh\n'
        'stock_path=\$(pm path $packageName | grep base | sed \'"\'"\'s/package://g\'"\'"\')\n'
        '[ ! -z \$stock_path ] && umount -l \$stock_path';
    String scriptFilePath = '$postFsDataDirPath/$packageName.sh';
    await Root.exec(
      cmd: 'echo \'$content\' > "$scriptFilePath"',
    );
    await Root.exec(
      cmd: 'chmod 744 "$scriptFilePath"',
    );
  }

  Future<void> installApk(String packageName, String patchedFilePath) async {
    String newPatchedFilePath = '$managerDirPath/$packageName/base.apk';
    await Root.exec(
      cmd: 'cp "$patchedFilePath" "$newPatchedFilePath"',
    );
    await Root.exec(
      cmd: 'chmod 644 "$newPatchedFilePath"',
    );
    await Root.exec(
      cmd: 'chown system:system "$newPatchedFilePath"',
    );
    await Root.exec(
      cmd: 'chcon u:object_r:apk_data_file:s0 "$newPatchedFilePath"',
    );
  }

  Future<void> mountApk(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    String newPatchedFilePath = '$managerDirPath/$packageName/base.apk';
    await Root.exec(
      cmd: 'am force-stop "$packageName"',
    );
    await Root.exec(
      cmd: 'su -mm -c "umount -l $originalFilePath"',
    );
    await Root.exec(
      cmd: 'su -mm -c "mount -o bind $newPatchedFilePath $originalFilePath"',
    );
  }
}
