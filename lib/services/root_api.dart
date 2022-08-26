import 'package:root/root.dart';

class RootAPI {
  final String _managerDirPath = "/data/adb/revanced_manager";
  final String _postFsDataDirPath = "/data/adb/post-fs-data.d";
  final String _serviceDDirPath = "/data/adb/service.d";

  Future<bool> isAppInstalled(String packageName) async {
    if (packageName.isNotEmpty) {
      String? res = await Root.exec(
        cmd: 'ls "$_managerDirPath/$packageName"',
      );
      if (res != null && res.isNotEmpty) {
        res = await Root.exec(
          cmd: 'ls "$_serviceDDirPath/$packageName.sh"',
        );
        return res != null && res.isNotEmpty;
      }
    }
    return false;
  }

  Future<List<String>> getInstalledApps() async {
    try {
      String? res = await Root.exec(
        cmd: 'ls "$_managerDirPath"',
      );
      if (res != null) {
        List<String> apps = res.split('\n');
        List<String> toRemove = [];
        for (String packageName in apps) {
          bool isInstalled = await isAppInstalled(packageName);
          if (!isInstalled) {
            toRemove.add(packageName);
          }
        }
        apps.removeWhere((a) => toRemove.contains(a));
        return apps;
      }
    } on Exception {
      return List.empty();
    }
    return List.empty();
  }

  Future<void> deleteApp(String packageName, String originalFilePath) async {
    await Root.exec(
      cmd: 'am force-stop "$packageName"',
    );
    await Root.exec(
      cmd: 'su -mm -c "umount -l $originalFilePath"',
    );
    await Root.exec(
      cmd: 'rm -rf "$_managerDirPath/$packageName"',
    );
    await Root.exec(
      cmd: 'rm -rf "$_serviceDDirPath/$packageName.sh"',
    );
    await Root.exec(
      cmd: 'rm -rf "$_postFsDataDirPath/$packageName.sh"',
    );
  }

  Future<bool> installApp(
    String packageName,
    String originalFilePath,
    String patchedFilePath,
  ) async {
    try {
      await Root.exec(
        cmd: 'mkdir -p "$_managerDirPath/$packageName"',
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
        'base_path=$_managerDirPath/$packageName/base.apk\n'
        'stock_path=\$(pm path $packageName | grep base | sed \'"\'"\'s/package://g\'"\'"\')\n'
        '[ ! -z \$stock_path ] && mount -o bind \$base_path \$stock_path';
    String scriptFilePath = '$_serviceDDirPath/$packageName.sh';
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
    String scriptFilePath = '$_postFsDataDirPath/$packageName.sh';
    await Root.exec(
      cmd: 'echo \'$content\' > "$scriptFilePath"',
    );
    await Root.exec(
      cmd: 'chmod 744 "$scriptFilePath"',
    );
  }

  Future<void> installApk(String packageName, String patchedFilePath) async {
    String newPatchedFilePath = '$_managerDirPath/$packageName/base.apk';
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
    String newPatchedFilePath = '$_managerDirPath/$packageName/base.apk';
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
