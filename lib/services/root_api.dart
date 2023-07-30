import 'package:flutter/foundation.dart';
import 'package:root/root.dart';

class RootAPI {
  // TODO(ponces): remove in the future, keep it for now during migration.
  final String _revancedOldDirPath = '/data/local/tmp/revanced-manager';
  final String _revancedDirPath = '/data/adb/revanced';
  final String _postFsDataDirPath = '/data/adb/post-fs-data.d';
  final String _serviceDDirPath = '/data/adb/service.d';

  Future<bool> isRooted() async {
    try {
      final bool? isRooted = await Root.isRootAvailable();
      return isRooted != null && isRooted;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return false;
    }
  }

  Future<bool> hasRootPermissions() async {
    try {
      bool? isRooted = await Root.isRootAvailable();
      if (isRooted != null && isRooted) {
        isRooted = await Root.isRooted();
        return isRooted != null && isRooted;
      }
      return false;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return false;
    }
  }

  Future<void> setPermissions(
    String permissions,
    ownerGroup,
    seLinux,
    String filePath,
  ) async {
    try {
      if (permissions.isNotEmpty) {
        await Root.exec(
          cmd: 'chmod $permissions "$filePath"',
        );
      }
      if (ownerGroup.isNotEmpty) {
        await Root.exec(
          cmd: 'chown $ownerGroup "$filePath"',
        );
      }
      if (seLinux.isNotEmpty) {
        await Root.exec(
          cmd: 'chcon $seLinux "$filePath"',
        );
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
  }

  Future<bool> isAppInstalled(String packageName) async {
    if (packageName.isNotEmpty) {
      return fileExists('$_serviceDDirPath/$packageName.sh');
    }
    return false;
  }

  Future<List<String>> getInstalledApps() async {
    final List<String> apps = List.empty(growable: true);
    try {
      String? res = await Root.exec(
        cmd: 'ls "$_revancedDirPath"',
      );
      if (res != null) {
        final List<String> list = res.split('\n');
        list.removeWhere((pack) => pack.isEmpty);
        apps.addAll(list.map((pack) => pack.trim()).toList());
      }
      // TODO(ponces): remove in the future, keep it for now during migration.
      res = await Root.exec(
        cmd: 'ls "$_revancedOldDirPath"',
      );
      if (res != null) {
        final List<String> list = res.split('\n');
        list.removeWhere((pack) => pack.isEmpty);
        apps.addAll(list.map((pack) => pack.trim()).toList());
      }
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
    }
    return apps;
  }

  Future<void> deleteApp(String packageName, String originalFilePath) async {
    await Root.exec(
      cmd: 'am force-stop "$packageName"',
    );
    await Root.exec(
      cmd: 'su -mm -c "umount -l $originalFilePath"',
    );
    // TODO(ponces): remove in the future, keep it for now during migration.
    await Root.exec(
      cmd: 'rm -rf "$_revancedOldDirPath/$packageName"',
    );
    await Root.exec(
      cmd: 'rm -rf "$_revancedDirPath/$packageName"',
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
      await deleteApp(packageName, originalFilePath);
      await Root.exec(
        cmd: 'mkdir -p "$_revancedDirPath/$packageName"',
      );
      await setPermissions(
        '0755',
        'shell:shell',
        '',
        '$_revancedDirPath/$packageName',
      );
      await saveOriginalFilePath(packageName, originalFilePath);
      await installServiceDScript(packageName);
      await installPostFsDataScript(packageName);
      await installApk(packageName, patchedFilePath);
      await mountApk(packageName, originalFilePath);
      return true;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return false;
    }
  }

  Future<void> installServiceDScript(String packageName) async {
    final String content = '#!/system/bin/sh\n'
        'while [ "\$(getprop sys.boot_completed | tr -d \'"\'"\'\\\\r\'"\'"\')" != "1" ]; do sleep 3; done\n'
        'base_path=$_revancedDirPath/$packageName/base.apk\n'
        'stock_path=\$(pm path $packageName | grep base | sed \'"\'"\'s/package://g\'"\'"\')\n'
        r'[ ! -z $stock_path ] && mount -o bind $base_path $stock_path';
    final String scriptFilePath = '$_serviceDDirPath/$packageName.sh';
    await Root.exec(
      cmd: 'echo \'$content\' > "$scriptFilePath"',
    );
    await setPermissions('0744', '', '', scriptFilePath);
  }

  Future<void> installPostFsDataScript(String packageName) async {
    final String content = '#!/system/bin/sh\n'
        'stock_path=\$(pm path $packageName | grep base | sed \'"\'"\'s/package://g\'"\'"\')\n'
        r'[ ! -z $stock_path ] && umount -l $stock_path';
    final String scriptFilePath = '$_postFsDataDirPath/$packageName.sh';
    await Root.exec(
      cmd: 'echo \'$content\' > "$scriptFilePath"',
    );
    await setPermissions('0744', '', '', scriptFilePath);
  }

  Future<void> installApk(String packageName, String patchedFilePath) async {
    final String newPatchedFilePath = '$_revancedDirPath/$packageName/base.apk';
    await Root.exec(
      cmd: 'cp "$patchedFilePath" "$newPatchedFilePath"',
    );
    await setPermissions(
      '0644',
      'system:system',
      'u:object_r:apk_data_file:s0',
      newPatchedFilePath,
    );
  }

  Future<void> mountApk(String packageName, String originalFilePath) async {
    final String newPatchedFilePath = '$_revancedDirPath/$packageName/base.apk';
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

  Future<bool> isMounted(String packageName) async {
    final String? res = await Root.exec(
      cmd: 'cat /proc/mounts | grep $packageName',
    );
    return res != null && res.isNotEmpty;
  }

  Future<void> saveOriginalFilePath(
    String packageName,
    String originalFilePath,
  ) async {
    final String originalRootPath =
        '$_revancedDirPath/$packageName/original.apk';
    await Root.exec(
      cmd: 'mkdir -p "$_revancedDirPath/$packageName"',
    );
    await setPermissions(
      '0755',
      'shell:shell',
      '',
      '$_revancedDirPath/$packageName',
    );
    await Root.exec(
      cmd: 'cp "$originalFilePath" "$originalRootPath"',
    );
    await setPermissions(
      '0644',
      'shell:shell',
      'u:object_r:apk_data_file:s0',
      originalFilePath,
    );
  }

  Future<bool> fileExists(String path) async {
    try {
      final String? res = await Root.exec(
        cmd: 'ls $path',
      );
      return res != null && res.isNotEmpty;
    } on Exception catch (e) {
      if (kDebugMode) {
        print(e);
      }
      return false;
    }
  }
}
